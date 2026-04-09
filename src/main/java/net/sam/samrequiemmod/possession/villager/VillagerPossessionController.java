package net.sam.samrequiemmod.possession.villager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class VillagerPossessionController {

    private VillagerPossessionController() {}

    public static boolean isVillagerPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.VILLAGER;
    }

    public static boolean isBabyVillagerPossessing(PlayerEntity player) {
        return isVillagerPossessing(player) && VillagerState.isBaby(player);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isVillagerPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isVillagerPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_VILLAGER_HURT, getPitch(player));

            if (source.getAttacker() instanceof LivingEntity attacker) {
                rallyNearbyGolems(player, attacker);
                if (attacker instanceof MobEntity mob && !isVillagerAlwaysHostile(mob)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isVillagerPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_VILLAGER_DEATH, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isVillagerPossessing(player)) return;
        lockHunger(player);
        handleAmbientSound(player);
    }

    public static boolean isVillagerAlwaysHostile(Entity entity) {
        return entity instanceof IllagerEntity
                || entity instanceof RavagerEntity
                || (entity instanceof ZombieEntity && !(entity instanceof net.minecraft.entity.mob.ZombifiedPiglinEntity));
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        VillagerState.setServerBaby(uuid, false);
    }

    public static boolean tryConvertToZombieVillager(ServerPlayerEntity player, DamageSource source) {
        if (!isVillagerPossessing(player)) return false;
        Entity attacker = resolveZombieAttacker(source);
        if (!(attacker instanceof LivingEntity livingAttacker)) return false;
        if (player.getRandom().nextFloat() >= 0.5f) return false;

        boolean baby = isBabyVillagerPossessing(player);
        VillagerState.setServerBaby(player.getUuid(), false);
        VillagerNetworking.broadcastBaby(player, false);
        net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.setServerBaby(player.getUuid(), baby);
        net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.broadcast(player, baby);
        PossessionManager.switchPossessionType(player, EntityType.ZOMBIE_VILLAGER, player.getMaxHealth());
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1.0f, baby ? 1.35f : 1.0f);
        if (livingAttacker instanceof MobEntity mob) {
            mob.setTarget(null);
        }
        return true;
    }

    private static void rallyNearbyGolems(ServerPlayerEntity player, LivingEntity attacker) {
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(
                IronGolemEntity.class, player.getBoundingBox().expand(30.0), IronGolemEntity::isAlive)) {
            golem.setTarget(attacker);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(attacker));
        }
    }

    private static Entity resolveZombieAttacker(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (net.sam.samrequiemmod.possession.zombie.ZombiePossessionController.isZombieSubtype(attacker)) {
            return attacker;
        }
        Entity directSource = source.getSource();
        if (net.sam.samrequiemmod.possession.zombie.ZombiePossessionController.isZombieSubtype(directSource)) {
            return directSource;
        }
        return null;
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.NEUTRAL, 1.0f, getPitch(player));
    }

    private static float getPitch(PlayerEntity player) {
        return isBabyVillagerPossessing(player) ? 1.35f : 1.0f;
    }
}






