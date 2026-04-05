package net.sam.samrequiemmod.possession.piglin;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.WaterShakeNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Baby piglin possession controller.
 * Same as adult piglin but:
 * - Slightly faster (handled by profile)
 * - Cannot melee attack mobs
 * - Baby sounds (higher pitch)
 * - Converts to baby zombified piglin in the overworld
 */
public final class BabyPiglinPossessionController {

    private BabyPiglinPossessionController() {}

    private static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    public static boolean isBabyPiglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PIGLIN
                && BabyPiglinState.isServerBaby(player);
    }

    public static void register() {

        // ── Attack: baby piglins CANNOT melee — block all attacks ─────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isBabyPiglinPossessing(sp)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            // Baby piglins can't attack — consume the event to prevent vanilla damage
            return ActionResult.SUCCESS;
        });

        // ── Hurt: sound, rally, provoke ───────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isBabyPiglinPossessing(player)) return true;

            Entity attacker = source.getAttacker();
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_PIGLIN_HURT, 1.5f);

            if (attacker == null || PiglinPossessionController.isPiglinAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                PiglinPossessionController.rallyNearbyPiglins(player, livingAttacker);
            }
            return true;
        });

        // ── Death sound ───────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isBabyPiglinPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.5f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isBabyPiglinPossessing(player)) return;

        PiglinPossessionController.lockHunger(player);
        PiglinPossessionController.preventNaturalHealing(player);
        handleOverworldConversion(player);

        // Baby piglin ambient sound (higher pitch)
        if (player.age % 120 == 0 && player.getRandom().nextFloat() < 0.35f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.5f);
        }

        // Re-rally allies every 20 ticks
        if (player.age % 20 == 0) persistRally(player);
    }

    private static void handleOverworldConversion(ServerPlayerEntity player) {
        boolean inOverworld = player.getEntityWorld().getRegistryKey() == World.OVERWORLD;
        if (!inOverworld) {
            int prev = OverworldConversionTracker.getTicks(player.getUuid());
            OverworldConversionTracker.reset(player.getUuid());
            if (prev > 0) WaterShakeNetworking.broadcast(player, false);
            return;
        }

        OverworldConversionTracker.tick(player.getUuid());
        int ticks = OverworldConversionTracker.getTicks(player.getUuid());

        if (ticks == 1) WaterShakeNetworking.broadcast(player, true);

        if (ticks >= 400) {
            OverworldConversionTracker.reset(player.getUuid());
            WaterShakeNetworking.broadcast(player, false);

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED,
                    SoundCategory.HOSTILE, 1.0f, 1.5f);

            float health = player.getHealth();
            PossessionManager.clearPossession(player);
            // Set baby state BEFORE start possession
            BabyZombifiedPiglinState.setServerBaby(player.getUuid(), true);
            PossessionManager.startPossession(player, EntityType.ZOMBIFIED_PIGLIN, health);
            BabyZombifiedPiglinNetworking.broadcast(player, true);
        }
    }

    private static void persistRally(ServerPlayerEntity player) {
        UUID attackerUuid = LAST_ATTACKER.get(player.getUuid());
        if (attackerUuid == null) return;
        Entity e = player.getEntityWorld().getEntity(attackerUuid);
        if (!(e instanceof LivingEntity attacker) || !attacker.isAlive()) {
            LAST_ATTACKER.remove(player.getUuid());
            return;
        }
        var box = player.getBoundingBox().expand(40.0);
        for (MobEntity ally : player.getEntityWorld()
                .getEntitiesByClass(MobEntity.class, box, m -> PiglinPossessionController.isPiglinAlly(m) && m.isAlive())) {
            if (ally instanceof net.minecraft.entity.mob.PiglinEntity piglin && piglin.isBaby()) continue;
            if (ally.getTarget() == null || !ally.getTarget().isAlive()) {
                if (ally instanceof net.minecraft.entity.mob.AbstractPiglinEntity abstractPiglin) {
                    abstractPiglin.getBrain().remember(
                            net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET, attacker);
                    abstractPiglin.getBrain().remember(
                            net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT, attacker.getUuid(),
                            600);
                }
                ally.setTarget(attacker);
            }
        }
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        LAST_ATTACKER.remove(player.getUuid());
        OverworldConversionTracker.reset(player.getUuid());
        WaterShakeNetworking.broadcast(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_ATTACKER.remove(uuid);
        OverworldConversionTracker.reset(uuid);
    }
}






