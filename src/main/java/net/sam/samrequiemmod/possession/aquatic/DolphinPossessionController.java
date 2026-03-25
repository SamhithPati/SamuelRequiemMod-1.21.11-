package net.sam.samrequiemmod.possession.aquatic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dolphin possession controller.
 * <p>
 * - 5 hearts of health
 * - Fast underwater (faster Dolphin's Grace — level 1 amplifier)
 * - Can melee hit mobs with difficulty-scaled damage:
 *     Easy=1H(2HP), Normal=1.5H(3HP), Hard=2.25H(4.5HP)
 * - Sounds: ambient, hurt, death
 * - Can't drown underwater
 * - On land: 2 minutes (2400 ticks) before taking damage
 * - Speed greatly reduced on land
 * - If hit, dolphins in 20-block radius attack that mob
 * - Hostile mobs don't attack unless provoked
 */
public final class DolphinPossessionController {

    private DolphinPossessionController() {}

    /** Tracks how many ticks each dolphin-possessed player has been on land. */
    private static final Map<UUID, Integer> LAND_TICKS = new ConcurrentHashMap<>();

    /** Last attacker entity UUID for dolphin rally behaviour. */
    private static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    /** 2 minutes in ticks. */
    private static final int LAND_GRACE_PERIOD = 2400;

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isDolphinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.DOLPHIN;
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // ── Left-click: difficulty-scaled melee damage ──────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isDolphinPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;

            float damage = switch (world.getDifficulty()) {
                case EASY -> 2.0f;       // 1 heart
                case HARD -> 4.5f;       // 2.25 hearts
                default   -> 3.0f;       // 1.5 hearts (Normal + Peaceful)
            };

            target.damage(sp.getDamageSources().playerAttack(sp), damage);

            // Play dolphin attack sound
            world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENTITY_DOLPHIN_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Mark provoked
            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            return ActionResult.FAIL;
        });

        // ── Hurt sounds + rally nearby dolphins when hit ───────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isDolphinPossessing(player)) return true;

            // Play hurt sound
            player.getWorld().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_DOLPHIN_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Rally nearby dolphins to attack the source
            if (source.getAttacker() instanceof LivingEntity attacker) {
                LAST_ATTACKER.put(player.getUuid(), attacker.getUuid());
                rallyDolphins(player, attacker);
            }

            return true;
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isDolphinPossessing(player)) return;

        lockHunger(player);
        handleBreathing(player);
        handleWaterSwimSpeed(player);
        handleLandTimer(player);
        handleAmbientSound(player);
        handleDolphinRally(player);
    }

    // ── Hunger lock ────────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Breathing: dolphins can't drown ────────────────────────────────────────

    private static void handleBreathing(ServerPlayerEntity player) {
        // Dolphins can breathe underwater indefinitely
        if (player.isSubmergedInWater() || player.isTouchingWater()) {
            player.setAir(player.getMaxAir());
        }
        // On land breathing is handled by the land timer — no vanilla drowning
        if (!player.isTouchingWater()) {
            player.setAir(player.getMaxAir()); // prevent vanilla air drain
        }
    }

    // ── Water speed boost (stronger than fish — amplifier 1) ──────────────────

    private static void handleWaterSwimSpeed(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            if (!player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                // Amplifier 2 for fast dolphin swimming (much faster than fish)
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.DOLPHINS_GRACE, 40, 2, false, false, false));
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
        }
    }

    // ── Land timer: 2 minutes grace period, then damage ──────────────────────

    private static void handleLandTimer(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            // Reset timer when in water
            LAND_TICKS.remove(player.getUuid());
            return;
        }

        int ticks = LAND_TICKS.getOrDefault(player.getUuid(), 0) + 1;
        LAND_TICKS.put(player.getUuid(), ticks);

        // After 2 minutes on land, start taking damage (1 HP per second)
        if (ticks > LAND_GRACE_PERIOD && player.age % 20 == 0) {
            player.damage(player.getDamageSources().dryOut(), 1.0f);
        }
    }

    // ── Rally nearby dolphins when attacked ──────────────────────────────────

    private static void rallyDolphins(ServerPlayerEntity player, LivingEntity attacker) {
        Box box = player.getBoundingBox().expand(20.0);
        for (DolphinEntity dolphin : player.getServerWorld()
                .getEntitiesByClass(DolphinEntity.class, box, d -> d.isAlive())) {
            if (dolphin.getTarget() == null || !dolphin.getTarget().isAlive()) {
                dolphin.setTarget(attacker);
            }
        }
    }

    // ── Persistent dolphin rally (keep directing dolphins at last attacker) ───

    private static void handleDolphinRally(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return; // check every second

        UUID attackerUuid = LAST_ATTACKER.get(player.getUuid());
        if (attackerUuid == null) return;

        // Find the attacker in the world
        Box searchBox = player.getBoundingBox().expand(20.0);
        LivingEntity attacker = null;
        for (LivingEntity le : player.getServerWorld()
                .getEntitiesByClass(LivingEntity.class, searchBox,
                        e -> e.getUuid().equals(attackerUuid) && e.isAlive())) {
            attacker = le;
            break;
        }

        if (attacker == null) {
            LAST_ATTACKER.remove(player.getUuid());
            return;
        }

        // Keep rallying dolphins
        for (DolphinEntity dolphin : player.getServerWorld()
                .getEntitiesByClass(DolphinEntity.class, searchBox, d -> d.isAlive())) {
            if (dolphin.getTarget() == null || !dolphin.getTarget().isAlive()) {
                dolphin.setTarget(attacker);
            }
        }
    }

    // ── Ambient sounds ────────────────────────────────────────────────────────

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.3f) return;

        player.getWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_DOLPHIN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        cleanup(uuid);
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
    }

    public static void onUnpossessUuid(UUID uuid) {
        cleanup(uuid);
    }

    private static void cleanup(UUID uuid) {
        LAND_TICKS.remove(uuid);
        LAST_ATTACKER.remove(uuid);
    }
}
