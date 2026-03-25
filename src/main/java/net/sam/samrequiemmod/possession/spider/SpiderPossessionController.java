package net.sam.samrequiemmod.possession.spider;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class SpiderPossessionController {

    private SpiderPossessionController() {}

    public static boolean isSpiderPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SPIDER;
    }

    public static boolean isCaveSpiderPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.CAVE_SPIDER;
    }

    public static boolean isAnySpiderPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isAnySpiderPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            float damage = getAttackDamage(sp.getServerWorld().getDifficulty(), isCaveSpiderPossessing(sp));
            boolean damaged = target.damage(sp.getDamageSources().playerAttack(sp), damage);
            if (damaged && isCaveSpiderPossessing(sp)) {
                applyCaveSpiderPoison(target, sp.getServerWorld().getDifficulty());
            }

            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENTITY_SPIDER_HURT, SoundCategory.PLAYERS, 0.7f, 1.15f);
            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnySpiderPossessing(player)) return true;

            if (source.equals(player.getDamageSources().magic()) && player.hasStatusEffect(StatusEffects.POISON)) {
                player.removeStatusEffect(StatusEffects.POISON);
                return false;
            }

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SPIDER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnySpiderPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SPIDER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnySpiderPossessing(player)) return;

        lockHunger(player);
        handlePoisonImmunity(player);
        handleAmbientSound(player);
        handleWallClimb(player);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.35f) return;
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void handleWallClimb(ServerPlayerEntity player) {
        if (!player.horizontalCollision) return;
        if (player.forwardSpeed <= 0.0f && Math.abs(player.sidewaysSpeed) < 0.01f) return;

        double upwardVelocity = isCaveSpiderPossessing(player) ? 0.23 : 0.20;
        double vertical = Math.max(player.getVelocity().y, upwardVelocity);
        player.setVelocity(player.getVelocity().x, vertical, player.getVelocity().z);
        player.velocityModified = true;
        player.fallDistance = 0.0f;
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) {
            player.removeStatusEffect(StatusEffects.POISON);
        }
    }

    private static float getAttackDamage(Difficulty difficulty, boolean caveSpider) {
        if (!caveSpider) {
            return switch (difficulty) {
                case HARD -> 6.0f;
                case EASY, NORMAL -> 4.0f;
                default -> 4.0f;
            };
        }

        return switch (difficulty) {
            case HARD -> 6.0f;
            case EASY, NORMAL -> 4.0f;
            default -> 4.0f;
        };
    }

    private static void applyCaveSpiderPoison(LivingEntity target, Difficulty difficulty) {
        int duration = switch (difficulty) {
            case NORMAL -> 140;
            case HARD -> 300;
            default -> 0;
        };
        if (duration <= 0) return;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, 0, false, true));
    }

    public static boolean isSpiderFood(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.isOf(Items.SPIDER_EYE) || stack.isOf(Items.FERMENTED_SPIDER_EYE));
    }

    public static float getSpiderFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.SPIDER_EYE)) return 4.0f;
        if (stack.isOf(Items.FERMENTED_SPIDER_EYE)) return 6.0f;
        return 0.0f;
    }

    public static String getFoodErrorMessage() {
        return "§cAs a spider, you can only heal from spider eyes and fermented spider eyes.";
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isSpiderFood(stack);
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID playerUuid) {
        // No persistent state yet.
    }
}
