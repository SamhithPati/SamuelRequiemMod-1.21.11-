package net.sam.samrequiemmod.possession.aquatic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared possession controller for Cod, Salmon, Pufferfish, and Tropical Fish.
 * <p>
 * Common behaviour:
 * - 1.5 hearts of health
 * - Faster swimming in water (Dolphin's Grace effect)
 * - Cannot drown in water
 * - Begin drowning when out of water
 * - Cannot melee hit mobs (except pufferfish special)
 * - Hostile mobs don't attack unless provoked
 * - Swimming animations in water
 * - Flop on land, greatly reduced land speed
 * <p>
 * Pufferfish-specific: left click puffs up + 1 heart damage + Poison IV for 7s
 * Tropical fish: color variant handled via TropicalFishVariantState/Networking
 */
public final class FishPossessionController {

    private FishPossessionController() {}

    /** Tracks the tick at which a pufferfish player last attacked (for puff animation). */
    private static final Map<UUID, Long> PUFFERFISH_ATTACK_TICK = new ConcurrentHashMap<>();

    /** Duration of puff animation in ticks (2 seconds). */
    private static final int PUFF_DURATION_TICKS = 40;

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isFishPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return isFishType(type);
    }

    public static boolean isFishType(EntityType<?> type) {
        return type == EntityType.COD || type == EntityType.SALMON
                || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH;
    }

    public static boolean isPuffedUp(UUID playerUuid, long currentAge) {
        Long attackTick = PUFFERFISH_ATTACK_TICK.get(playerUuid);
        if (attackTick == null) return false;
        return (currentAge - attackTick) < PUFF_DURATION_TICKS;
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // ── Left-click: block melee for all fish EXCEPT pufferfish special ──
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isFishPossessing(sp)) return ActionResult.PASS;
            // Allow possession relic usage
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;

            EntityType<?> possType = PossessionManager.getPossessedType(sp);

            // Pufferfish special attack
            if (possType == EntityType.PUFFERFISH && entity instanceof LivingEntity target) {
                // Deal 1 heart (2 HP) of damage
                target.damage(sp.getDamageSources().playerAttack(sp), 2.0f);

                // Apply Poison IV for 7 seconds (140 ticks)
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.POISON, 140, 3, false, true));

                // Track puff state
                PUFFERFISH_ATTACK_TICK.put(sp.getUuid(), (long) sp.age);
                PufferfishNetworking.broadcastPuffState(sp, true);

                // Play pufferfish sting sound
                world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ENTITY_PUFFER_FISH_STING, SoundCategory.PLAYERS, 1.0f, 1.0f);

                // Mark provoked so the mob retaliates
                if (entity instanceof MobEntity mob) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
                }

                return ActionResult.FAIL;
            }

            // All other fish: block melee completely
            return ActionResult.FAIL;
        });

        // ── Hurt sounds ────────────────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            EntityType<?> type = PossessionManager.getPossessedType(player);
            if (!isFishType(type)) return true;

            var hurtSound = getHurtSound(type);
            if (hurtSound != null) {
                player.getWorld().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        hurtSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return true;
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isFishPossessing(player)) return;

        lockHunger(player);
        handleBreathing(player);
        handleWaterSwimSpeed(player);
        handleLandFlopping(player);
        handleAmbientSound(player);
        handlePufferfishDeflate(player);
    }

    // ── Hunger lock ────────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Breathing: underwater = fine, on land = drowning ───────────────────────
    // Vanilla automatically refills air when the player is above water, so
    // we can't rely on air manipulation. Instead, apply direct drowning damage.

    /** Tracks how many ticks each fish-possessed player has been out of water. */
    private static final Map<UUID, Integer> OUT_OF_WATER_TICKS = new ConcurrentHashMap<>();

    private static void handleBreathing(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            // In water — reset and ensure full air
            OUT_OF_WATER_TICKS.remove(player.getUuid());
            player.setAir(player.getMaxAir());
        } else {
            // On land — count ticks and apply drowning damage after air runs out
            int ticks = OUT_OF_WATER_TICKS.getOrDefault(player.getUuid(), 0) + 1;
            OUT_OF_WATER_TICKS.put(player.getUuid(), ticks);

            // 300 ticks = 15 seconds (same as vanilla air supply before drowning)
            if (ticks > 300) {
                // Apply drowning damage every second (20 ticks)
                if (ticks % 20 == 0) {
                    player.damage(player.getDamageSources().drown(), 2.0f);
                }
            }

            // Visually drain the air bar to indicate impending drowning
            int airRemaining = Math.max(0, player.getMaxAir() - ticks);
            player.setAir(airRemaining);
        }
    }

    // ── Water speed boost (Dolphin's Grace effect while in water) ──────────────

    private static void handleWaterSwimSpeed(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            // Apply Dolphin's Grace for faster swimming
            if (!player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
            }
        } else {
            // Remove speed boost on land
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
        }
    }

    // ── Land flopping: random velocity bursts when moving on land ──────────────

    private static void handleLandFlopping(ServerPlayerEntity player) {
        if (player.isTouchingWater()) return;
        if (!player.isOnGround()) return;

        // Fish flop every 10-30 ticks when trying to move on land
        Vec3d vel = player.getVelocity();
        boolean tryingToMove = Math.abs(vel.x) > 0.001 || Math.abs(vel.z) > 0.001
                || player.forwardSpeed != 0 || player.sidewaysSpeed != 0;

        if (tryingToMove && player.age % 10 == 0) {
            // Small random hop and sideways movement to simulate flopping
            double hopY = 0.3 + player.getRandom().nextDouble() * 0.2;
            double sideX = (player.getRandom().nextDouble() - 0.5) * 0.3;
            double sideZ = (player.getRandom().nextDouble() - 0.5) * 0.3;
            player.setVelocity(sideX, hopY, sideZ);
            player.velocityModified = true;

            // Play flop sound
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GUARDIAN_FLOP, SoundCategory.PLAYERS, 0.5f, 1.0f);
        }
    }

    // ── Pufferfish deflation after 2 seconds ──────────────────────────────────

    private static void handlePufferfishDeflate(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type != EntityType.PUFFERFISH) return;

        Long attackTick = PUFFERFISH_ATTACK_TICK.get(player.getUuid());
        if (attackTick == null) return;

        // Exactly at puff duration end, broadcast deflate
        if ((long) player.age - attackTick == PUFF_DURATION_TICKS) {
            PufferfishNetworking.broadcastPuffState(player, false);
            PUFFERFISH_ATTACK_TICK.remove(player.getUuid());
        }
    }

    // ── Ambient sounds ────────────────────────────────────────────────────────

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 200 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;

        EntityType<?> type = PossessionManager.getPossessedType(player);
        var sound = getAmbientSound(type);
        if (sound == null) return;

        player.getWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ── Sound lookups ─────────────────────────────────────────────────────────

    private static net.minecraft.sound.SoundEvent getAmbientSound(EntityType<?> type) {
        if (type == EntityType.COD) return SoundEvents.ENTITY_COD_AMBIENT;
        if (type == EntityType.SALMON) return SoundEvents.ENTITY_SALMON_AMBIENT;
        if (type == EntityType.PUFFERFISH) return SoundEvents.ENTITY_PUFFER_FISH_AMBIENT;
        if (type == EntityType.TROPICAL_FISH) return SoundEvents.ENTITY_TROPICAL_FISH_AMBIENT;
        return null;
    }

    private static net.minecraft.sound.SoundEvent getHurtSound(EntityType<?> type) {
        if (type == EntityType.COD) return SoundEvents.ENTITY_COD_HURT;
        if (type == EntityType.SALMON) return SoundEvents.ENTITY_SALMON_HURT;
        if (type == EntityType.PUFFERFISH) return SoundEvents.ENTITY_PUFFER_FISH_HURT;
        if (type == EntityType.TROPICAL_FISH) return SoundEvents.ENTITY_TROPICAL_FISH_HURT;
        return null;
    }

    public static net.minecraft.sound.SoundEvent getDeathSound(EntityType<?> type) {
        if (type == EntityType.COD) return SoundEvents.ENTITY_COD_DEATH;
        if (type == EntityType.SALMON) return SoundEvents.ENTITY_SALMON_DEATH;
        if (type == EntityType.PUFFERFISH) return SoundEvents.ENTITY_PUFFER_FISH_DEATH;
        if (type == EntityType.TROPICAL_FISH) return SoundEvents.ENTITY_TROPICAL_FISH_DEATH;
        return null;
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        cleanup(uuid);
        // Remove dolphin's grace if still active
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
        PufferfishNetworking.broadcastPuffState(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        cleanup(uuid);
    }

    private static void cleanup(UUID uuid) {
        PUFFERFISH_ATTACK_TICK.remove(uuid);
        OUT_OF_WATER_TICKS.remove(uuid);
    }
}
