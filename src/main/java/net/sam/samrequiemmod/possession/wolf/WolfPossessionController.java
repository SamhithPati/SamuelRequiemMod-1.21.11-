package net.sam.samrequiemmod.possession.wolf;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
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
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WolfPossessionController {

    private static final int SHAKE_TICKS = 28;
    private static final Map<UUID, Boolean> WAS_WET = new ConcurrentHashMap<>();

    private WolfPossessionController() {}

    public static boolean isWolfPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WOLF;
    }

    public static boolean isWolfAlly(Entity entity) {
        return entity instanceof WolfEntity;
    }

    public static boolean isBabyWolfPossessing(PlayerEntity player) {
        return isWolfPossessing(player) && WolfBabyState.isBaby(player);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isWolfPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            target.damage(sp.getDamageSources().playerAttack(sp), 3.0f);
            if (entity instanceof MobEntity mob
                    && !(entity instanceof AbstractSkeletonEntity)
                    && !isWolfAlly(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            sp.swingHand(hand, true);
            sp.getWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    getAggroSound(sp), SoundCategory.PLAYERS, 1.0f, getPitch(sp));
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWolfPossessing(player)) return true;

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WOLF_HURT, SoundCategory.PLAYERS, 1.0f, getPitch(player));

            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity livingAttacker) {
                if (isWolfAlly(attacker)) return false;
                if (attacker instanceof MobEntity mob
                        && !(attacker instanceof AbstractSkeletonEntity)
                        && !isWolfAlly(attacker)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                if (attacker instanceof AbstractSkeletonEntity skeleton) {
                    clearSkeletonAggro(skeleton);
                }
                if (!isWolfAlly(attacker)) {
                    rallyNearbyWolves(player, livingAttacker);
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWolfPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WOLF_DEATH, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isWolfPossessing(player)) return;

        lockHunger(player);
        handleAmbientSound(player);
        handleSkeletonFlee(player);
        handleWaterShake(player);
    }

    public static void initializeWolfState(ServerPlayerEntity player, WolfEntity wolf) {
        WolfBabyState.setServerBaby(player.getUuid(), wolf.isBaby());
        WolfState.setServerAngry(player.getUuid(), false);
        String variant = wolf.getVariant().getKey()
                .map(RegistryKey::getValue)
                .map(Identifier::toString)
                .orElse("minecraft:pale");
        WolfState.setServerVariant(player.getUuid(), variant);
    }

    public static void syncAllState(ServerPlayerEntity player) {
        WolfNetworking.broadcastBaby(player, WolfBabyState.isServerBaby(player));
        WolfNetworking.broadcastAngry(player, WolfState.isServerAngry(player.getUuid()));
        WolfNetworking.broadcastVariant(player, WolfState.getServerVariant(player.getUuid()));
    }

    public static void handleAngryToggle(ServerPlayerEntity player) {
        if (!isWolfPossessing(player)) return;
        boolean angry = !WolfState.isServerAngry(player.getUuid());
        WolfState.setServerAngry(player.getUuid(), angry);
        WolfNetworking.broadcastAngry(player, angry);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                angry ? SoundEvents.ENTITY_WOLF_GROWL : SoundEvents.ENTITY_WOLF_AMBIENT,
                SoundCategory.PLAYERS, 1.0f, getPitch(player));
    }

    public static boolean isWolfFood(ItemStack stack) {
        return stack.isOf(Items.ROTTEN_FLESH)
                || stack.isOf(Items.BEEF)
                || stack.isOf(Items.COOKED_BEEF)
                || stack.isOf(Items.PORKCHOP)
                || stack.isOf(Items.COOKED_PORKCHOP)
                || stack.isOf(Items.MUTTON)
                || stack.isOf(Items.COOKED_MUTTON)
                || stack.isOf(Items.CHICKEN)
                || stack.isOf(Items.COOKED_CHICKEN)
                || stack.isOf(Items.RABBIT)
                || stack.isOf(Items.COOKED_RABBIT);
    }

    public static float getWolfFoodHealing(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (stack.isOf(Items.ROTTEN_FLESH)) return 4.0f;
        return food == null ? 0.0f : Math.max(2.0f, food.nutrition());
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isWolfFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a wolf, you can only heal from meat and rotten flesh.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        WolfBabyState.setServerBaby(uuid, false);
        WolfState.clear(uuid);
        WAS_WET.remove(uuid);
    }

    private static void rallyNearbyWolves(ServerPlayerEntity player, LivingEntity attacker) {
        List<WolfEntity> wolves = player.getServerWorld().getEntitiesByClass(
                WolfEntity.class, player.getBoundingBox().expand(20.0), WolfEntity::isAlive);
        for (WolfEntity wolf : wolves) {
            wolf.setTarget(attacker);
            wolf.setAngryAt(attacker.getUuid());
            wolf.setAngerTime(200);
        }
    }

    private static void handleSkeletonFlee(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(20.0);
        for (AbstractSkeletonEntity skeleton : player.getServerWorld().getEntitiesByClass(
                AbstractSkeletonEntity.class, box, AbstractSkeletonEntity::isAlive)) {
            clearSkeletonAggro(skeleton);
            double dx = skeleton.getX() - player.getX();
            double dz = skeleton.getZ() - player.getZ();
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            double fleeX = skeleton.getX() + (dx / len) * 10.0;
            double fleeZ = skeleton.getZ() + (dz / len) * 10.0;
            skeleton.getNavigation().startMovingTo(fleeX, skeleton.getY(), fleeZ, 1.25);
        }
    }

    private static void clearSkeletonAggro(AbstractSkeletonEntity skeleton) {
        skeleton.setTarget(null);
        skeleton.setAttacker(null);
        skeleton.getNavigation().stop();
        ZombieTargetingState.clearProvoked(skeleton.getUuid());
    }

    private static void handleWaterShake(ServerPlayerEntity player) {
        boolean isWet = player.isTouchingWater() || player.isWet();
        boolean wasWet = WAS_WET.getOrDefault(player.getUuid(), false);
        if (wasWet && !isWet) {
            WolfNetworking.broadcastShake(player, SHAKE_TICKS);
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WOLF_SHAKE, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        }
        WAS_WET.put(player.getUuid(), isWet);
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 120 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                WolfState.isServerAngry(player.getUuid()) ? getAggroSound(player) : SoundEvents.ENTITY_WOLF_AMBIENT,
                SoundCategory.HOSTILE, 1.0f, getPitch(player));
    }

    private static SoundEvent getAggroSound(PlayerEntity player) {
        return WolfState.isServerAngry(player.getUuid()) ? SoundEvents.ENTITY_WOLF_GROWL : SoundEvents.ENTITY_WOLF_PANT;
    }

    private static float getPitch(PlayerEntity player) {
        return isBabyWolfPossessing(player) ? 1.35f : 1.0f;
    }
}
