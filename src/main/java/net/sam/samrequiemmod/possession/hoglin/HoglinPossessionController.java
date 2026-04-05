package net.sam.samrequiemmod.possession.hoglin;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Hoglin;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.UUID;

public final class HoglinPossessionController {

    private static final int ATTACK_ANIMATION_TICKS = 10;

    private HoglinPossessionController() {}

    public static boolean isHoglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.HOGLIN;
    }

    public static boolean isZoglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOGLIN;
    }

    public static boolean isBabyHoglinPossessing(PlayerEntity player) {
        return isHoglinPossessing(player) && BabyHoglinState.isServerBaby(player);
    }

    public static boolean isAdultHoglinPossessing(PlayerEntity player) {
        return isHoglinPossessing(player) && !BabyHoglinState.isServerBaby(player);
    }

    public static boolean isBabyZoglinPossessing(PlayerEntity player) {
        return isZoglinPossessing(player) && BabyHoglinState.isServerBaby(player);
    }

    public static boolean isAdultZoglinPossessing(PlayerEntity player) {
        return isZoglinPossessing(player) && !BabyHoglinState.isServerBaby(player);
    }

    public static boolean isAnyHoglinTypePossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.HOGLIN || type == EntityType.ZOGLIN;
    }

    public static boolean isHoglinAlly(Entity entity) {
        return entity instanceof HoglinEntity;
    }

    public static boolean isZoglinAlly(Entity entity) {
        return entity instanceof ZoglinEntity;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isAnyHoglinTypePossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            float damage = getAttackDamage(sp);
            boolean damaged = target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
            if (damaged) {
                Hoglin.knockback(sp, target);
                playAttackFeedback(sp);
                HoglinAttackNetworking.broadcastAttack(sp, ATTACK_ANIMATION_TICKS);
            }

            if (entity instanceof MobEntity mob && !isPassiveToPlayer(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnyHoglinTypePossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (isAnyZoglinPossessing(player)) {
                if (source.equals(player.getDamageSources().magic()) && player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
                if (isZoglinFireDamage(player, source)) {
                    return false;
                }
            }

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getHurtSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));

            Entity attacker = source.getAttacker();
            if (attacker instanceof MobEntity mob && !isPassiveToPlayer(attacker)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }

            if (attacker instanceof LivingEntity livingAttacker && isAnyHoglinPossessing(player)) {
                rallyNearbyHoglins(player, livingAttacker);
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnyHoglinTypePossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getDeathSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnyHoglinTypePossessing(player)) return;

        lockHunger(player);
        handleAmbientSound(player);
        aggroIronGolems(player);

        if (isAnyHoglinPossessing(player)) {
            handleOverworldConversion(player);
        } else {
            HoglinConversionTracker.reset(player.getUuid());
            preventSwimming(player);
            preventDrowning(player);
            handlePoisonImmunity(player);
            handleHarmingHeals(player);
        }
    }

    private static void handleOverworldConversion(ServerPlayerEntity player) {
        boolean inOverworld = player.getEntityWorld().getRegistryKey() == World.OVERWORLD;
        if (!inOverworld) {
            HoglinConversionTracker.reset(player.getUuid());
            return;
        }

        HoglinConversionTracker.tick(player.getUuid());
        if (HoglinConversionTracker.getTicks(player.getUuid()) < 400) return;

        boolean wasBaby = BabyHoglinState.isServerBaby(player);
        float health = player.getHealth();

        HoglinConversionTracker.reset(player.getUuid());
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, SoundCategory.HOSTILE, 1.0f, getPitch(player));

        PossessionManager.clearPossession(player);
        if (wasBaby) {
            BabyHoglinState.setServerBaby(player.getUuid(), true);
        }
        PossessionManager.startPossession(player, EntityType.ZOGLIN, health);
        if (wasBaby) {
            BabyHoglinNetworking.broadcast(player, true);
        }
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.35f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAmbientSound(player), SoundCategory.HOSTILE, 1.0f, getPitch(player));
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        List<IronGolemEntity> golems = player.getEntityWorld().getEntitiesByClass(
                IronGolemEntity.class, box, golem -> golem.isAlive());
        for (IronGolemEntity golem : golems) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static void rallyNearbyHoglins(ServerPlayerEntity player, LivingEntity threat) {
        Box box = player.getBoundingBox().expand(30.0);
        for (HoglinEntity hoglin : player.getEntityWorld().getEntitiesByClass(
                HoglinEntity.class, box, entity -> entity.isAlive())) {
            if (hoglin.isBaby()) continue;
            if (hoglin.getUuid().equals(player.getUuid())) continue;
            hoglin.getBrain().remember(MemoryModuleType.ATTACK_TARGET, threat);
            hoglin.getBrain().remember(MemoryModuleType.ANGRY_AT, threat.getUuid(), 600L);
            hoglin.getBrain().forget(MemoryModuleType.AVOID_TARGET);
            hoglin.setTarget(threat);
        }
    }

    private static void playAttackFeedback(ServerPlayerEntity player) {
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAttackSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void preventSwimming(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.NoSwimPossessionHelper.disableSwimmingPose(player);
    }

    private static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) {
            player.setAir(player.getMaxAir());
        }
    }

    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) {
            player.removeStatusEffect(StatusEffects.POISON);
        }
    }

    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    private static boolean isZoglinFireDamage(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource source) {
        return source.equals(player.getDamageSources().onFire())
                || source.equals(player.getDamageSources().inFire())
                || source.equals(player.getDamageSources().lava())
                || source.equals(player.getDamageSources().hotFloor())
                || source.getSource() instanceof net.minecraft.entity.projectile.SmallFireballEntity;
    }

    private static float getAttackDamage(ServerPlayerEntity player) {
        Difficulty difficulty = player.getEntityWorld().getDifficulty();
        if (isAnyBabyPossessing(player)) {
            return switch (difficulty) {
                case HARD -> 0.75f;
                case EASY, NORMAL -> 0.5f;
                default -> 0.5f;
            };
        }
        if (isAnyZoglinPossessing(player)) {
            return switch (difficulty) {
                case EASY -> 5.0f;
                case NORMAL -> 9.0f;
                case HARD -> 13.5f;
                default -> 9.0f;
            };
        }
        return switch (difficulty) {
            case EASY -> 5.0f;
            case NORMAL -> 8.0f;
            case HARD -> 12.0f;
            default -> 8.0f;
        };
    }

    private static boolean isPassiveToPlayer(Entity entity) {
        return isHoglinAlly(entity) || isZoglinAlly(entity);
    }

    private static boolean isAnyBabyPossessing(PlayerEntity player) {
        return isAnyHoglinTypePossessing(player) && BabyHoglinState.isServerBaby(player);
    }

    private static boolean isAnyHoglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.HOGLIN;
    }

    private static boolean isAnyZoglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOGLIN;
    }

    private static SoundEvent getAmbientSound(PlayerEntity player) {
        return isAnyZoglinPossessing(player)
                ? SoundEvents.ENTITY_ZOGLIN_AMBIENT
                : SoundEvents.ENTITY_HOGLIN_AMBIENT;
    }

    private static SoundEvent getAttackSound(PlayerEntity player) {
        return isAnyZoglinPossessing(player)
                ? SoundEvents.ENTITY_ZOGLIN_ATTACK
                : SoundEvents.ENTITY_HOGLIN_ATTACK;
    }

    private static SoundEvent getHurtSound(PlayerEntity player) {
        return isAnyZoglinPossessing(player)
                ? SoundEvents.ENTITY_ZOGLIN_HURT
                : SoundEvents.ENTITY_HOGLIN_HURT;
    }

    private static SoundEvent getDeathSound(PlayerEntity player) {
        return isAnyZoglinPossessing(player)
                ? SoundEvents.ENTITY_ZOGLIN_DEATH
                : SoundEvents.ENTITY_HOGLIN_DEATH;
    }

    private static float getPitch(PlayerEntity player) {
        return isAnyBabyPossessing(player) ? 1.35f : 1.0f;
    }

    public static boolean isHoglinFood(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.isOf(Items.WARPED_FUNGUS) || stack.isOf(Items.CRIMSON_FUNGUS));
    }

    public static float getHoglinFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.WARPED_FUNGUS) || stack.isOf(Items.CRIMSON_FUNGUS)) return 4.0f;
        return 0.0f;
    }

    public static boolean isZoglinFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && getZoglinFoodHealing(stack) > 0.0f;
    }

    public static float getZoglinFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.ROTTEN_FLESH)) return 3.0f;
        if (stack.isOf(Items.BEEF)) return 4.0f;
        if (stack.isOf(Items.CHICKEN)) return 3.0f;
        if (stack.isOf(Items.MUTTON)) return 4.0f;
        if (stack.isOf(Items.PORKCHOP)) return 4.0f;
        if (stack.isOf(Items.RABBIT)) return 3.0f;
        return 0.0f;
    }

    public static boolean blocksHoglinFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isHoglinFood(stack);
    }

    public static boolean blocksZoglinFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isZoglinFood(stack);
    }

    public static String getHoglinFoodErrorMessage() {
        return "§cAs a hoglin, you can only heal from warped fungus and crimson fungus.";
    }

    public static String getZoglinFoodErrorMessage() {
        return "§cAs a zoglin, you can only heal from raw meat and rotten flesh.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        HoglinConversionTracker.reset(uuid);
        BabyHoglinState.setServerBaby(uuid, false);
    }
}






