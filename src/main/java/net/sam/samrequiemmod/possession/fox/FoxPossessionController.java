package net.sam.samrequiemmod.possession.fox;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.passive.BabyPassiveMobState;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class FoxPossessionController {

    private FoxPossessionController() {}

    public static boolean isFoxPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.FOX;
    }

    public static boolean isBabyFoxPossessing(PlayerEntity player) {
        return isFoxPossessing(player) && BabyPassiveMobState.isBaby(player);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isFoxPossessing(sp)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (isBabyFoxPossessing(sp)) return ActionResult.FAIL;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            target.damage(sp.getDamageSources().playerAttack(sp), 2.0f);
            if (entity instanceof MobEntity mob && !isFoxThreat(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }
            sp.swingHand(hand, true);
            sp.getWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENTITY_FOX_BITE, SoundCategory.PLAYERS, 1.0f, getPitch(sp));
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isFoxPossessing(player)) return true;

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_FOX_HURT, SoundCategory.PLAYERS, 1.0f, getPitch(player));

            if (source.getAttacker() instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isFoxPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_FOX_DEATH, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isFoxPossessing(player)) return;
        lockHunger(player);
        handleAmbientSound(player);
        handleFoxPredators(player);
    }

    public static void initializeFoxState(ServerPlayerEntity player, FoxEntity fox) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), fox.isBaby());
        FoxState.setServerVariant(player.getUuid(), fox.getVariant().asString());
    }

    public static void syncAllState(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.broadcast(player, BabyPassiveMobState.isServerBaby(player));
        FoxNetworking.broadcastVariant(player, FoxState.getServerVariant(player.getUuid()));
    }

    public static boolean isFoxFood(ItemStack stack) {
        return stack.isOf(Items.SWEET_BERRIES)
                || stack.isOf(Items.CHICKEN)
                || stack.isOf(Items.BEEF)
                || stack.isOf(Items.PORKCHOP)
                || stack.isOf(Items.MUTTON)
                || stack.isOf(Items.RABBIT);
    }

    public static float getFoxFoodHealing(ItemStack stack) {
        if (stack.isOf(Items.SWEET_BERRIES)) return 2.0f;
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food == null ? 0.0f : Math.max(2.0f, food.nutrition());
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isFoxFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "\u00A7cAs a fox, you can only heal from raw meat and sweet berries.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        FoxState.clear(uuid);
    }

    private static void handleFoxPredators(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(20.0);
        for (WolfEntity wolf : player.getServerWorld().getEntitiesByClass(WolfEntity.class, box, WolfEntity::isAlive)) {
            wolf.setTarget(player);
            wolf.setAngryAt(player.getUuid());
            wolf.setAngerTime(200);
        }
        for (PolarBearEntity polarBear : player.getServerWorld().getEntitiesByClass(PolarBearEntity.class, box, PolarBearEntity::isAlive)) {
            polarBear.setTarget(player);
            polarBear.setAttacker(player);
        }
    }

    private static boolean isFoxThreat(Entity entity) {
        return entity instanceof WolfEntity || entity instanceof PolarBearEntity;
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_FOX_AMBIENT, SoundCategory.PLAYERS, 1.0f, getPitch(player));
    }

    private static float getPitch(PlayerEntity player) {
        return isBabyFoxPossessing(player) ? 1.35f : 1.0f;
    }
}
