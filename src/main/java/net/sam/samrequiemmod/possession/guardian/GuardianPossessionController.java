package net.sam.samrequiemmod.possession.guardian;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuardianPossessionController {

    private static final int GUARDIAN_WARMUP = 55;
    private static final int ELDER_WARMUP = 55;
    private static final int COOLDOWN_TICKS = 40;

    private static final Map<UUID, BeamState> ACTIVE_BEAMS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_FATIGUE_PULSE = new ConcurrentHashMap<>();

    private GuardianPossessionController() {}

    public static boolean isGuardianPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.GUARDIAN;
    }

    public static boolean isElderGuardianPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ELDER_GUARDIAN;
    }

    public static boolean isAnyGuardianPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN;
    }

    public static boolean isGuardianAlly(Entity entity) {
        return entity instanceof GuardianEntity || entity instanceof ElderGuardianEntity;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnyGuardianPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getHurtSound(player), SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (source.getAttacker() instanceof LivingEntity attacker) {
                if (attacker instanceof MobEntity mob && !isGuardianAlly(attacker)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                attacker.damage(((net.minecraft.server.world.ServerWorld) attacker.getEntityWorld()), player.getDamageSources().thorns(player), getRetaliationDamage(player));
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnyGuardianPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getDeathSound(player), SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void handleAttackRequest(ServerPlayerEntity player, UUID targetUuid) {
        if (!isAnyGuardianPossessing(player)) return;
        if (targetUuid == null) return;
        if (ACTIVE_BEAMS.containsKey(player.getUuid())) return;

        long cooldownUntil = COOLDOWNS.getOrDefault(player.getUuid(), -1L);
        if ((long) player.age < cooldownUntil) return;

        Entity entity = player.getEntityWorld().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (target.squaredDistanceTo(player) > 20.0 * 20.0) return;

        int warmup = isElderGuardianPossessing(player) ? ELDER_WARMUP : GUARDIAN_WARMUP;
        ACTIVE_BEAMS.put(player.getUuid(), new BeamState(targetUuid, player.age, warmup));
        GuardianNetworking.broadcastBeam(player, targetUuid, warmup);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnyGuardianPossessing(player)) return;

        lockHunger(player);
        handleBreathing(player);
        handleWaterSwimSpeed(player);
        handleSwimSound(player);
        handleLandFlopping(player);
        handleAmbientSound(player);
        aggroIronGolems(player);
        tickBeam(player);
        tickElderPulse(player);
    }

    private static void tickBeam(ServerPlayerEntity player) {
        BeamState state = ACTIVE_BEAMS.get(player.getUuid());
        if (state == null) return;

        Entity entity = player.getEntityWorld().getEntity(state.targetUuid());
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || target.squaredDistanceTo(player) > 20.0 * 20.0) {
            ACTIVE_BEAMS.remove(player.getUuid());
            GuardianNetworking.broadcastBeam(player, null, 0);
            return;
        }

        if (player.age - state.startedTick() < state.warmupTicks()) return;

        float damage = getBeamDamage(player.getEntityWorld().getDifficulty(), isElderGuardianPossessing(player));
        target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), player.getDamageSources().magic(), damage);
        if (target instanceof MobEntity mob && !isGuardianAlly(target)) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }

        ACTIVE_BEAMS.remove(player.getUuid());
        COOLDOWNS.put(player.getUuid(), (long) player.age + COOLDOWN_TICKS);
        GuardianNetworking.broadcastBeam(player, null, 0);
    }

    private static void tickElderPulse(ServerPlayerEntity player) {
        if (!isElderGuardianPossessing(player)) return;
        long lastPulse = LAST_FATIGUE_PULSE.getOrDefault(player.getUuid(), -1200L);
        if ((long) player.age - lastPulse < 1200L) return;

        LivingEntity nearest = null;
        double nearestDist = 50.0 * 50.0;
        for (LivingEntity entity : player.getEntityWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(50.0), e -> e.isAlive() && e != player && !isGuardianAlly(e))) {
            double d = entity.squaredDistanceTo(player);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = entity;
            }
        }

        if (nearest == null) return;
        nearest.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6000, 2, false, true));
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        LAST_FATIGUE_PULSE.put(player.getUuid(), (long) player.age);
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleBreathing(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) {
            player.setAir(player.getMaxAir());
        }
    }

    private static void handleWaterSwimSpeed(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            if (!player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
            }
        } else if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
    }

    private static void handleLandFlopping(ServerPlayerEntity player) {
        if (player.isTouchingWater()) return;
        if (!player.isOnGround()) return;
        boolean tryingToMove = Math.abs(player.getVelocity().x) > 0.001 || Math.abs(player.getVelocity().z) > 0.001
                || player.forwardSpeed != 0 || player.sidewaysSpeed != 0;
        if (!tryingToMove) return;
        if (player.age % 10 != 0) return;

        double hopY = 0.35 + player.getRandom().nextDouble() * 0.15;
        double sideX = (player.getRandom().nextDouble() - 0.5) * 0.35;
        double sideZ = (player.getRandom().nextDouble() - 0.5) * 0.35;
        player.setVelocity(sideX, hopY, sideZ);
        player.velocityDirty = true;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getFlopSound(player), SoundCategory.PLAYERS, 0.6f, 1.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 100 != 0) return;
        if (player.getRandom().nextFloat() >= 0.55f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAmbientSound(player), SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void handleSwimSound(ServerPlayerEntity player) {
        if (!player.isTouchingWater()) return;
        if (player.age % 16 != 0) return;
        Vec3d velocity = player.getVelocity();
        if (velocity.horizontalLengthSquared() < 0.0025 && Math.abs(velocity.y) < 0.02) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getSwimSound(player), SoundCategory.PLAYERS, 0.45f, 1.0f);
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static float getBeamDamage(Difficulty difficulty, boolean elder) {
        if (elder) {
            return switch (difficulty) {
                case EASY -> 5.0f;
                case NORMAL -> 8.0f;
                case HARD -> 12.0f;
                default -> 8.0f;
            };
        }
        return switch (difficulty) {
            case EASY -> 4.0f;
            case NORMAL -> 6.0f;
            case HARD -> 9.0f;
            default -> 6.0f;
        };
    }

    private static float getRetaliationDamage(PlayerEntity player) {
        return isElderGuardianPossessing(player) ? 4.0f : 2.0f;
    }

    private static SoundEvent getAmbientSound(PlayerEntity player) {
        boolean inWater = player.isTouchingWater() || player.isSubmergedInWater();
        if (isElderGuardianPossessing(player)) {
            return inWater ? SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT : SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND;
        }
        return inWater ? SoundEvents.ENTITY_GUARDIAN_AMBIENT : SoundEvents.ENTITY_GUARDIAN_AMBIENT_LAND;
    }

    private static SoundEvent getHurtSound(PlayerEntity player) {
        return isElderGuardianPossessing(player)
                ? SoundEvents.ENTITY_ELDER_GUARDIAN_HURT
                : SoundEvents.ENTITY_GUARDIAN_HURT;
    }

    private static SoundEvent getDeathSound(PlayerEntity player) {
        return isElderGuardianPossessing(player)
                ? SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH
                : SoundEvents.ENTITY_GUARDIAN_DEATH;
    }

    private static SoundEvent getFlopSound(PlayerEntity player) {
        return isElderGuardianPossessing(player)
                ? SoundEvents.ENTITY_ELDER_GUARDIAN_FLOP
                : SoundEvents.ENTITY_GUARDIAN_FLOP;
    }

    private static SoundEvent getSwimSound(PlayerEntity player) {
        return isElderGuardianPossessing(player)
                ? SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT
                : SoundEvents.ENTITY_GUARDIAN_AMBIENT;
    }

    public static boolean isGuardianFood(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.isOf(Items.COD) || stack.isOf(Items.SALMON) || stack.isOf(Items.TROPICAL_FISH));
    }

    public static float getGuardianFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.COD)) return 3.0f;
        if (stack.isOf(Items.SALMON)) return 4.0f;
        if (stack.isOf(Items.TROPICAL_FISH)) return 4.0f;
        return 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isGuardianFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a guardian, you can only heal from cod, salmon, and tropical fish.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        ACTIVE_BEAMS.remove(player.getUuid());
        COOLDOWNS.remove(player.getUuid());
        LAST_FATIGUE_PULSE.remove(player.getUuid());
        GuardianNetworking.broadcastBeam(player, null, 0);
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
    }

    public static void onUnpossessUuid(UUID uuid) {
        ACTIVE_BEAMS.remove(uuid);
        COOLDOWNS.remove(uuid);
        LAST_FATIGUE_PULSE.remove(uuid);
    }

    private record BeamState(UUID targetUuid, int startedTick, int warmupTicks) {}
}






