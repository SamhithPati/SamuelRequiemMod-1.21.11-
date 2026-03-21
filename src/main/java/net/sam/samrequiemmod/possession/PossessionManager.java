package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.drowned.DrownedTridentManager;
import net.sam.samrequiemmod.possession.WaterConversionTracker;
import net.sam.samrequiemmod.possession.WaterShakeNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PossessionManager {
    private static final Map<UUID, PossessionData> PLAYER_POSSESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, EntityType<?>> LAST_DIMENSION_TYPE = new ConcurrentHashMap<>();

    /** Stores the player's health and hunger level before they possessed a mob. */
    private static final Map<UUID, float[]> PRE_POSSESSION_STATE = new ConcurrentHashMap<>();

    private PossessionManager() {
    }

    private static PossessionData getOrCreate(ServerPlayerEntity player) {
        return PLAYER_POSSESSIONS.computeIfAbsent(player.getUuid(), uuid -> new PossessionData());
    }

    @Nullable
    public static PossessionData get(PlayerEntity player) {
        return PLAYER_POSSESSIONS.get(player.getUuid());
    }

    public static boolean isPossessing(PlayerEntity player) {
        PossessionData data = get(player);
        return data != null && data.isPossessing();
    }

    @Nullable
    public static EntityType<?> getPossessedType(PlayerEntity player) {
        PossessionData data = get(player);
        return data == null ? null : data.getPossessedType();
    }

    public static void startPossession(ServerPlayerEntity player, EntityType<?> type) {
        startPossession(player, type, -1f);
    }

    public static void startPossession(ServerPlayerEntity player, EntityType<?> type, float mobHealth) {
        // Save pre-possession state before anything changes
        PRE_POSSESSION_STATE.put(player.getUuid(), new float[]{
                player.getHealth(),
                (float) player.getHungerManager().getFoodLevel()
        });

        PossessionData data = getOrCreate(player);
        data.setPossessedType(type);

        // Apply stat modifiers (max health, speed, attack) immediately so
        // getMaxHealth() is correct before we set the player's health below.
        net.sam.samrequiemmod.possession.PossessionEffects.apply(player);

        PossessionNetworking.broadcastPossessionSync(player, type);

        // Set player health to match the possessed mob's current health,
        // clamped to the (now-updated) max health.
        if (mobHealth > 0) {
            final float h = mobHealth;
            player.setHealth(Math.min(h, player.getMaxHealth()));
        } else {
            // No mob health provided — top up to full
            player.setHealth(player.getMaxHealth());
        }

        // Immediately clear all unprovoked mob targets so mobs that were
        // already chasing the player stop the moment possession begins.
        clearNearbyMobTargets(player);

        SamuelRequiemMod.LOGGER.info(
                "[Possession] {} started possessing {}",
                player.getGameProfile().getName(),
                EntityType.getId(type)
        );
    }

    /**
     * Clears the target of every nearby mob that is currently chasing this player,
     * unless the mob has been provoked (it hit the player first).
     * Called on possession start so mobs stop mid-chase immediately.
     */
    private static void clearNearbyMobTargets(ServerPlayerEntity player) {
        if (player.getServerWorld() == null) return;

        net.minecraft.util.math.Box box = player.getBoundingBox().expand(48.0);
        java.util.List<net.minecraft.entity.mob.MobEntity> mobs =
                player.getServerWorld().getEntitiesByClass(
                        net.minecraft.entity.mob.MobEntity.class,
                        box,
                        mob -> mob.isAlive() && mob.getTarget() == player);

        for (net.minecraft.entity.mob.MobEntity mob : mobs) {
            // Golems are always hostile — don't clear them
            if (mob instanceof net.minecraft.entity.passive.IronGolemEntity) continue;
            if (mob instanceof net.minecraft.entity.passive.SnowGolemEntity) continue;
            // Provoked mobs (have hit the player) keep their target
            if (net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(mob.getUuid())) continue;
            mob.setTarget(null);
        }
    }

    public static void clearPossession(ServerPlayerEntity player) {
        // Check possession type BEFORE clearing data so onUnpossess gets the right type
        boolean wasPillager = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player);
        boolean wasVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player);
        boolean wasEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player);

        PossessionData data = PLAYER_POSSESSIONS.get(player.getUuid());
        if (data != null) {
            data.clear();
        }

        // Clear stat modifiers immediately so getMaxHealth() is correct
        // before we restore the player's health below.
        net.sam.samrequiemmod.possession.PossessionEffects.clear(player);

        // Restore pre-possession health and hunger
        float[] saved = PRE_POSSESSION_STATE.remove(player.getUuid());
        if (saved != null) {
            float restoredHealth = Math.min(saved[0], player.getMaxHealth());
            player.setHealth(restoredHealth);
            player.getHungerManager().setFoodLevel((int) saved[1]);
        }

        LAST_DIMENSION_TYPE.remove(player.getUuid());
        PossessionNetworking.broadcastPossessionSync(player, null);

        // Clear baby zombie state on unpossess
        BabyZombieState.setServerBaby(player.getUuid(), false);
        BabyZombieNetworking.broadcastBabyZombieSync(player, false);
        // Clear baby husk state on unpossess
        BabyHuskState.setServerBaby(player.getUuid(), false);
        BabyHuskNetworking.broadcastBabyHuskSync(player, false);
        // Clear baby drowned state and remove trident on unpossess
        BabyDrownedState.setServerBaby(player.getUuid(), false);
        BabyDrownedNetworking.broadcast(player, false);
        DrownedTridentManager.removeTrident(player);
        DrownedTridentManager.clearPlayer(player.getUuid());
        // Reset water conversion timer and shaking state on unpossess
        WaterConversionTracker.resetSubmerged(player.getUuid());
        WaterShakeNetworking.broadcast(player, false);
        // Clean up illager possession state — remove items given during possession
        if (wasPillager) {
            net.sam.samrequiemmod.possession.illager.PillagerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.PillagerPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasVindicator) {
            net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasEvoker) {
            net.sam.samrequiemmod.possession.illager.EvokerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.EvokerPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Grant immunity window FIRST — this makes MobEntityTargetMixin
        // block any re-targeting before we clear the provoked mobs below
        SamuelRequiemMod.POST_POSSESSION_IMMUNITY.put(player.getUuid(), (long) player.age + 100L);

        // Clear all mobs this player provoked during possession, and wipe their anger memory
        java.util.Set<java.util.UUID> provokedMobs =
                net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.clearPlayerProvoked(player.getUuid());
        if (!provokedMobs.isEmpty() && player.getServerWorld() != null) {
            net.minecraft.util.math.Box box = player.getBoundingBox().expand(128.0);
            for (net.minecraft.entity.mob.MobEntity mob : player.getServerWorld()
                    .getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box, m -> m.isAlive())) {
                if (!provokedMobs.contains(mob.getUuid())) continue;
                net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.clearProvoked(mob.getUuid());
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob.getAttacker() == player) mob.setAttacker(null);
                if (mob instanceof net.minecraft.entity.mob.Angerable angerable
                        && player.getUuid().equals(angerable.getAngryAt())) {
                    angerable.setAngryAt(null);
                    angerable.setAngerTime(0);
                }
                if (mob instanceof net.minecraft.entity.passive.IronGolemEntity golem)
                    golem.setAttacking(false);
                // Register this mob to keep forgetting the player for 30 seconds
                net.sam.samrequiemmod.possession.zombie.ZombieForgetPlayerState.registerForget(
                        mob.getUuid(), player.getUuid(), player.age, 600);
            }
        }

        SamuelRequiemMod.LOGGER.info(
                "[Possession] {} cleared possession",
                player.getGameProfile().getName()
        );
    }

    public static void removePlayer(ServerPlayerEntity player) {
        PLAYER_POSSESSIONS.remove(player.getUuid());
        LAST_DIMENSION_TYPE.remove(player.getUuid());
        PRE_POSSESSION_STATE.remove(player.getUuid());
    }

    @Nullable
    public static EntityType<?> getLastDimensionType(PlayerEntity player) {
        return LAST_DIMENSION_TYPE.get(player.getUuid());
    }

    public static void setLastDimensionType(PlayerEntity player, @Nullable EntityType<?> type) {
        if (type == null) {
            LAST_DIMENSION_TYPE.remove(player.getUuid());
        } else {
            LAST_DIMENSION_TYPE.put(player.getUuid(), type);
        }
    }
}