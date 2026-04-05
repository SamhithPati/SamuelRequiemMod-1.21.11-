package net.sam.samrequiemmod.possession.drowned;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DrownedPossessionController {

    private DrownedPossessionController() {}

    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100;

    public static void register() {

        // ── Player attacks a mob ─────────────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isDrownedPossessing(serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (ZombiePossessionController.isAlwaysPassive(entity)) {
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), calculateDamage(serverPlayer));
                LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
                ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
                serverPlayer.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            if (entity instanceof MobEntity mob) ZombieTargetingState.markProvoked(mob.getUuid(), serverPlayer.getUuid());

            boolean damaged = livingTarget.damage(
                    (ServerWorld) livingTarget.getEntityWorld(),
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    calculateDamage(serverPlayer));
            if (damaged) {
                world.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_DROWNED_AMBIENT, SoundCategory.PLAYERS, 0.65f, 1.0f);
            }
            LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
            serverPlayer.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── Mob damages the drowned player ───────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isDrownedPossessing(player)) return true;

            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }
            Entity attacker = source.getAttacker();
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_DROWNED_HURT, 1.0f);

            if (attacker != null) {
                if (!ZombiePossessionController.isZombieSubtype(attacker))
                    ZombieTargetingState.markProvoked(attacker.getUuid(), player.getUuid());
                rallyNearbyZombies(player, attacker);
            }
            return true;
        });

        // ── Death ────────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && isDrownedPossessing(player)) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_DROWNED_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            if (entity instanceof MobEntity) ZombieTargetingState.clearProvoked(entity.getUuid());
        });

        // ── Villager zombification ────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof VillagerEntity villager)) return true;
            if (!(entity.getEntityWorld() instanceof ServerWorld sw)) return true;
            if (!(source.getAttacker() instanceof ServerPlayerEntity p)) return true;
            if (!isDrownedPossessing(p)) return true;
            if (amount < villager.getHealth()) return true;

            float chance = switch (sw.getDifficulty()) {
                case EASY -> 0.0f; case NORMAL -> 0.5f; case HARD -> 1.0f; default -> 0.0f;
            };
            if (chance <= 0.0f) return true;
            if (chance < 1.0f && sw.getRandom().nextFloat() >= chance) return true;

            ZombieVillagerEntity zv = EntityType.ZOMBIE_VILLAGER.create(sw, SpawnReason.CONVERSION);
            if (zv == null) return true;
            zv.refreshPositionAndAngles(villager.getX(), villager.getY(), villager.getZ(),
                    villager.getYaw(), villager.getPitch());
            zv.setVillagerData(villager.getVillagerData());
            zv.setBaby(villager.isBaby());
            zv.initialize(sw, sw.getLocalDifficulty(zv.getBlockPos()), SpawnReason.CONVERSION, null);
            zv.setBaby(villager.isBaby());
            zv.setPersistent();
            villager.discard();
            sw.spawnEntityAndPassengers(zv);
            sw.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                    SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 1.0f, 1.0f);
            return false;
        });

        // Trident pickup blocking is handled in tick() via DrownedTridentManager
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isDrownedPossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }
        preventNaturalHealing(player);
        lockHunger(player);
        handleSunlightBurn(player);
        handleAmbientSound(player);
        preventDrowning(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        repelVillagers(player);
        aggroIronGolems(player);
        aggroSnowGolems(player);
        tickArmsRaised(player);
        DrownedTridentManager.ensureTrident(player);
        DrownedTridentManager.preventTridentDupe(player);
    }

    public static boolean isDrownedPossessing(PlayerEntity player) {
        return net.minecraft.entity.EntityType.DROWNED == PossessionManager.getPossessedType(player)
                && !BabyDrownedState.isServerBaby(player);
    }

    private static float calculateDamage(ServerPlayerEntity player) {
        double atk = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        if (atk > 1.5) return (float) atk;
        return switch (player.getEntityWorld().getDifficulty()) {
            case EASY -> 2.0f; case NORMAL -> 3.0f; case HARD -> 4.5f; default -> 3.0f;
        };
    }

    private static void rallyNearbyZombies(ServerPlayerEntity player, Entity threat) {
        if (!(threat instanceof LivingEntity lt)) return;
        Box box = player.getBoundingBox().expand(40.0);
        for (ZombieEntity z : player.getEntityWorld().getEntitiesByClass(ZombieEntity.class, box, e -> e.isAlive()))
            z.setTarget(lt);
    }

    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long last = LAST_HIT_TICK.get(player.getUuid());
        if (last == null) return;
        if ((long) player.age - last >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void preventNaturalHealing(ServerPlayerEntity player) {
        // HungerManagerMixin already blocks passive healing for possessed players.
        // Do not touch timeUntilRegen here: vanilla also uses it for hurt i-frames.
    }

    private static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) player.setAir(player.getMaxAir());
    }

    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) player.removeStatusEffect(StatusEffects.POISON);
    }

    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance h = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (h == null) return;
        player.setHealth(Math.min(player.getHealth() + 6.0f * (float) Math.pow(2, h.getAmplifier()),
                player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    private static void handleSunlightBurn(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getEntityWorld().isDay()) return;
        if (player.isTouchingWaterOrRain() || player.isTouchingWater()) return;
        if (!player.getEntityWorld().isSkyVisible(BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ())))
            return;
        if (player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) player.setOnFireFor(8);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_DROWNED_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void repelVillagers(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return;
        Box box = player.getBoundingBox().expand(16.0);
        for (VillagerEntity v : player.getEntityWorld().getEntitiesByClass(VillagerEntity.class, box, e -> e.isAlive())) {
            if (v.squaredDistanceTo(player) > 64.0) continue;
            Vec3d t = net.minecraft.entity.ai.NoPenaltyTargeting.findFrom(v, 16, 7, player.getEntityPos());
            if (t == null) {
                Vec3d a = v.getEntityPos().subtract(player.getEntityPos());
                if (a.lengthSquared() < 0.001) a = new Vec3d(1, 0, 0);
                t = v.getEntityPos().add(a.normalize().multiply(10.0));
            }
            v.getNavigation().startMovingTo(t.x, t.y, t.z, 0.6);
        }
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity g : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, e -> e.isAlive()))
            if (g.squaredDistanceTo(player) <= 576.0) { g.setTarget(player); g.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player)); }
    }

    private static void aggroSnowGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(10.0);
        for (SnowGolemEntity g : player.getEntityWorld().getEntitiesByClass(SnowGolemEntity.class, box, e -> e.isAlive()))
            g.setTarget(player);
    }
}





