package net.sam.samrequiemmod.possession.feline;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.passive.BabyPassiveMobState;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class FelinePossessionController {

    private FelinePossessionController() {}

    public static boolean isOcelotPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.OCELOT;
    }

    public static boolean isCatPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.CAT;
    }

    public static boolean isAnyFelinePossessing(PlayerEntity player) {
        return isOcelotPossessing(player) || isCatPossessing(player);
    }

    public static boolean isBabyOcelotPossessing(PlayerEntity player) {
        return isOcelotPossessing(player) && BabyPassiveMobState.isBaby(player);
    }

    public static boolean isBabyCatPossessing(PlayerEntity player) {
        return isCatPossessing(player) && BabyPassiveMobState.isBaby(player);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isAnyFelinePossessing(sp)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            if (isCatPossessing(sp)) {
                if (entity instanceof CreeperEntity creeper) {
                    clearCreeperAggro(creeper);
                }
                return ActionResult.FAIL;
            }

            target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), 2.0f);
            if (entity instanceof CreeperEntity creeper) {
                clearCreeperAggro(creeper);
            } else if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }
            sp.swingHand(hand, true);
            sp.getEntityWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    getAmbientSound(sp), SoundCategory.PLAYERS, 1.0f, getPitch(sp));
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnyFelinePossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;
            if (source.equals(player.getDamageSources().fall())) return false;

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getHurtSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));

            if (source.getAttacker() instanceof CreeperEntity creeper) {
                clearCreeperAggro(creeper);
                return true;
            }
            if (source.getAttacker() instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnyFelinePossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getDeathSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnyFelinePossessing(player)) return;
        lockHunger(player);
        handleAmbientSound(player);
        handleCreeperFlee(player);
    }

    public static void initializeCatState(ServerPlayerEntity player, CatEntity cat) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), cat.isBaby());
        String variant = cat.getVariant().getKey()
                .map(RegistryKey::getValue)
                .map(Identifier::toString)
                .orElse("minecraft:black");
        CatState.setServerVariant(player.getUuid(), variant);
    }

    public static void initializeOcelotState(ServerPlayerEntity player, OcelotEntity ocelot) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), ocelot.isBaby());
    }

    public static void syncCatState(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.broadcast(player, BabyPassiveMobState.isServerBaby(player));
        CatNetworking.broadcastVariant(player, CatState.getServerVariant(player.getUuid()));
    }

    public static void syncOcelotState(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.broadcast(player, BabyPassiveMobState.isServerBaby(player));
    }

    public static boolean isFelineFood(ItemStack stack) {
        return stack.isOf(Items.COD) || stack.isOf(Items.SALMON);
    }

    public static float getFelineFoodHealing(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food == null ? 0.0f : Math.max(2.0f, food.nutrition());
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isFelineFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "\u00A7cAs a cat, you can only heal from cod and salmon.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        CatState.clear(uuid);
    }

    private static void handleCreeperFlee(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(20.0);
        for (CreeperEntity creeper : player.getEntityWorld().getEntitiesByClass(CreeperEntity.class, box, CreeperEntity::isAlive)) {
            clearCreeperAggro(creeper);
            double dx = creeper.getX() - player.getX();
            double dz = creeper.getZ() - player.getZ();
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            creeper.getNavigation().startMovingTo(
                    creeper.getX() + (dx / len) * 10.0,
                    creeper.getY(),
                    creeper.getZ() + (dz / len) * 10.0,
                    1.25
            );
        }
    }

    private static void clearCreeperAggro(CreeperEntity creeper) {
        creeper.setTarget(null);
        creeper.setAttacker(null);
        creeper.getNavigation().stop();
        ZombieTargetingState.clearProvoked(creeper.getUuid());
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAmbientSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));
    }

    private static SoundEvent getAmbientSound(PlayerEntity player) {
        if (isCatPossessing(player)) {
            return isBabyCatPossessing(player) ? SoundEvents.ENTITY_CAT_STRAY_AMBIENT : SoundEvents.ENTITY_CAT_AMBIENT;
        }
        return SoundEvents.ENTITY_OCELOT_AMBIENT;
    }

    private static SoundEvent getHurtSound(PlayerEntity player) {
        return isCatPossessing(player) ? SoundEvents.ENTITY_CAT_HURT : SoundEvents.ENTITY_OCELOT_HURT;
    }

    private static SoundEvent getDeathSound(PlayerEntity player) {
        return isCatPossessing(player) ? SoundEvents.ENTITY_CAT_DEATH : SoundEvents.ENTITY_OCELOT_DEATH;
    }

    private static float getPitch(PlayerEntity player) {
        if (isBabyOcelotPossessing(player) || isBabyCatPossessing(player)) return 1.35f;
        return 1.0f;
    }
}






