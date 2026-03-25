package net.sam.samrequiemmod.possession.aquatic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.UUID;

/**
 * Squid possession controller.
 * <p>
 * - 5 hearts of health
 * - Same swim speed as fish (Dolphin's Grace in water)
 * - Drowns out of water
 * - Swimming animation
 * - Hurt/death sounds
 * - Extretes ink when taking damage
 * - Cannot melee hit mobs
 * - Hostile mobs don't attack
 * - Guardians and Elder Guardians DO attack
 * - Speed greatly reduced on land
 */
public final class SquidPossessionController {

    private SquidPossessionController() {}

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isSquidPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SQUID;
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // Block all melee attacks
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isSquidPossessing(sp)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // Hurt sounds + ink squirt on damage
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isSquidPossessing(player)) return true;

            // Play squid hurt sound
            player.getWorld().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SQUID_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Squirt ink particles (like vanilla squid when damaged)
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SQUID_INK,
                        player.getX(), player.getBodyY(0.5), player.getZ(),
                        30, // count
                        0.3, 0.3, 0.3, // spread
                        0.1 // speed
                );
            }

            return true;
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isSquidPossessing(player)) return;

        lockHunger(player);
        handleBreathing(player);
        handleWaterSwimSpeed(player);
        handleAmbientSound(player);
    }

    // ── Hunger lock ────────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Breathing: underwater = fine, on land = drowning ───────────────────────
    // Vanilla automatically refills air when the player is above water, so
    // we apply direct drowning damage instead of air manipulation.

    private static final java.util.Map<java.util.UUID, Integer> OUT_OF_WATER_TICKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static void handleBreathing(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            OUT_OF_WATER_TICKS.remove(player.getUuid());
            player.setAir(player.getMaxAir());
        } else {
            int ticks = OUT_OF_WATER_TICKS.getOrDefault(player.getUuid(), 0) + 1;
            OUT_OF_WATER_TICKS.put(player.getUuid(), ticks);

            // 300 ticks = 15 seconds before drowning starts
            if (ticks > 300 && ticks % 20 == 0) {
                player.damage(player.getDamageSources().drown(), 2.0f);
            }

            // Visually drain the air bar
            int airRemaining = Math.max(0, player.getMaxAir() - ticks);
            player.setAir(airRemaining);
        }
    }

    // ── Water speed boost ──────────────────────────────────────────────────────

    private static void handleWaterSwimSpeed(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            if (!player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
        }
    }

    // ── Ambient sounds ────────────────────────────────────────────────────────

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 200 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;

        player.getWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SQUID_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
        OUT_OF_WATER_TICKS.remove(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        OUT_OF_WATER_TICKS.remove(uuid);
    }
}
