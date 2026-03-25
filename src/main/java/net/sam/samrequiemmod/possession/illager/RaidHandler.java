package net.sam.samrequiemmod.possession.illager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.village.raid.Raid;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles raid mechanics and victory celebration for illager captains:
 * - Starting raids near villages via goat horn
 * - Raid witch healing for the captain
 * - Raiders targeting baby villagers
 * - Victory celebration when all villagers die
 */
public final class RaidHandler {

    private RaidHandler() {}

    /** Captain UUID -> tick when celebration ends */
    private static final Map<UUID, Long> CELEBRATION_END_TICK = new ConcurrentHashMap<>();

    /** Captain UUID -> whether villagers were nearby last check */
    private static final Map<UUID, Boolean> HAD_VILLAGERS = new ConcurrentHashMap<>();

    /** Witch UUID -> tick of last potion throw during raid (3-second cooldown) */
    private static final Map<UUID, Long> RAID_WITCH_COOLDOWN = new ConcurrentHashMap<>();

    // ── Village detection ─────────────────────────────────────────────────────

    /** Returns true if the player is near an occupied village (POI check) or near villagers. */
    public static boolean isNearVillage(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        // Primary check: vanilla's own village detection (occupied POIs like beds/workstations)
        if (world.isNearOccupiedPointOfInterest(player.getBlockPos())) {
            return true;
        }
        // Fallback: if there are villagers nearby, treat it as a village
        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class, player.getBoundingBox().expand(64),
                Entity::isAlive);
        return !villagers.isEmpty();
    }

    // ── Raid starting ─────────────────────────────────────────────────────────

    /** Start a raid directly via RaidManager. */
    public static void handleRaidStart(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        // Apply RAID_OMEN first — Raid.start() checks for this effect to set bad omen level
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RAID_OMEN, 600, 0, false, true));

        // Directly call startRaid — bypasses the 30-second RAID_OMEN countdown
        Raid raid = world.getRaidManager().startRaid(player, player.getBlockPos());

        if (raid != null) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EVENT_MOB_EFFECT_RAID_OMEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Remove RAID_OMEN after starting so it doesn't try to start another raid when it expires
        player.removeStatusEffect(StatusEffects.RAID_OMEN);
    }

    // ── Server tick (called from SamuelRequiemMod) ────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!RavagerRidingHandler.isIllagerPossessed(player)) return;
        if (!CaptainState.isCaptain(player)) return;

        UUID uuid = player.getUuid();

        // Handle active celebration
        if (CELEBRATION_END_TICK.containsKey(uuid)) {
            tickCelebration(player);
            return;
        }

        // Tick raid-specific mechanics
        tickRaidWitchHealing(player);
        tickBabyVillagerTargeting(player);
        tickVillagerDeathDetection(player);
    }

    // ── Raid witch healing ────────────────────────────────────────────────────

    private static void tickRaidWitchHealing(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return; // Check every second
        if (player.getHealth() >= player.getMaxHealth()) return;

        ServerWorld world = player.getServerWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        // Find raid witches nearby that can heal
        List<WitchEntity> witches = world.getEntitiesByClass(
                WitchEntity.class, player.getBoundingBox().expand(16),
                w -> w.isAlive());

        long now = player.age;
        for (WitchEntity witch : witches) {
            Long lastThrow = RAID_WITCH_COOLDOWN.get(witch.getUuid());
            if (lastThrow != null && now - lastThrow < 60) continue; // 3-second cooldown

            CaptainHandler.throwRegenPotion(witch, player);
            RAID_WITCH_COOLDOWN.put(witch.getUuid(), now);
            break; // Only one witch heals per tick cycle
        }
    }

    // ── Baby villager targeting ───────────────────────────────────────────────

    private static void tickBabyVillagerTargeting(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;

        ServerWorld world = player.getServerWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        // Find baby villagers
        List<VillagerEntity> babyVillagers = world.getEntitiesByClass(
                VillagerEntity.class, player.getBoundingBox().expand(48),
                v -> v.isAlive() && v.isBaby());

        if (babyVillagers.isEmpty()) return;

        VillagerEntity target = babyVillagers.get(0);

        // Assign baby villagers as targets for idle raiders
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class, player.getBoundingBox().expand(48),
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive()
                        && (m.getTarget() == null || !m.getTarget().isAlive()))) {
            raider.setTarget(target);
        }
    }

    // ── Villager death detection / celebration trigger ─────────────────────────

    private static void tickVillagerDeathDetection(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return; // Check every 2 seconds

        UUID uuid = player.getUuid();
        ServerWorld world = player.getServerWorld();

        // Only track villager deaths during an active raid
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) {
            HAD_VILLAGERS.remove(uuid);
            return;
        }

        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class, player.getBoundingBox().expand(64),
                Entity::isAlive);

        boolean hadVillagers = HAD_VILLAGERS.getOrDefault(uuid, false);
        boolean hasVillagers = !villagers.isEmpty();

        HAD_VILLAGERS.put(uuid, hasVillagers);

        if (hadVillagers && !hasVillagers) {
            // All villagers are dead! Start celebration
            startCelebration(player);
        }
    }

    // ── Celebration ───────────────────────────────────────────────────────────

    private static void startCelebration(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        CELEBRATION_END_TICK.put(uuid, (long) player.age + 300L); // 15 seconds
        // Prevent re-trigger: mark as "no villagers" so the transition doesn't fire again
        HAD_VILLAGERS.put(uuid, false);

        ServerWorld world = player.getServerWorld();

        // Clear all raider targets so they stop fighting, and set celebrating
        Box box = player.getBoundingBox().expand(48.0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class, box,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
            raider.setTarget(null);
            raider.getNavigation().stop();
            if (raider instanceof RaiderEntity raiderEntity) {
                raiderEntity.setCelebrating(true);
            }
        }

        // Send celebration sync to client for player animation
        CaptainNetworking.broadcastCelebration(player, true);

        // Play initial celebration sound
        playCelebrationSound(player);
    }

    private static void tickCelebration(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Long endTick = CELEBRATION_END_TICK.get(uuid);
        if (endTick == null) return;

        if ((long) player.age >= endTick) {
            // Celebration over
            CELEBRATION_END_TICK.remove(uuid);
            // Keep HAD_VILLAGERS as false so it doesn't re-trigger
            HAD_VILLAGERS.put(uuid, false);
            CaptainNetworking.broadcastCelebration(player, false);

            // Stop celebrating
            ServerWorld world = player.getServerWorld();
            Box box = player.getBoundingBox().expand(48.0);
            for (MobEntity raider : world.getEntitiesByClass(
                    MobEntity.class, box,
                    m -> m instanceof RaiderEntity && m.isAlive())) {
                if (raider instanceof RaiderEntity raiderEntity) {
                    raiderEntity.setCelebrating(false);
                }
            }
            return;
        }

        // Keep raiders celebrating and prevent them from attacking
        if (player.age % 20 == 0) {
            ServerWorld world = player.getServerWorld();
            Box box = player.getBoundingBox().expand(48.0);
            for (MobEntity raider : world.getEntitiesByClass(
                    MobEntity.class, box,
                    m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
                raider.setTarget(null);
                if (raider instanceof RaiderEntity raiderEntity) {
                    raiderEntity.setCelebrating(true);
                }
            }
        }

        // Play celebration sounds periodically
        if (player.age % 60 == 0) {
            playCelebrationSound(player);
            playRaiderCelebrationSounds(player);
        }
    }

    private static void playCelebrationSound(ServerPlayerEntity player) {
        EntityType<?> possType = PossessionManager.getPossessedType(player);
        net.minecraft.sound.SoundEvent sound;
        if (possType == EntityType.PILLAGER) {
            sound = SoundEvents.ENTITY_PILLAGER_CELEBRATE;
        } else if (possType == EntityType.VINDICATOR) {
            sound = SoundEvents.ENTITY_VINDICATOR_CELEBRATE;
        } else if (possType == EntityType.EVOKER) {
            sound = SoundEvents.ENTITY_EVOKER_CELEBRATE;
        } else {
            sound = SoundEvents.ENTITY_PILLAGER_CELEBRATE;
        }
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void playRaiderCelebrationSounds(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Box box = player.getBoundingBox().expand(48.0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class, box,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
            net.minecraft.sound.SoundEvent sound;
            if (raider instanceof PillagerEntity) {
                sound = SoundEvents.ENTITY_PILLAGER_CELEBRATE;
            } else if (raider instanceof VindicatorEntity) {
                sound = SoundEvents.ENTITY_VINDICATOR_CELEBRATE;
            } else if (raider instanceof EvokerEntity) {
                sound = SoundEvents.ENTITY_EVOKER_CELEBRATE;
            } else if (raider instanceof RavagerEntity) {
                sound = SoundEvents.ENTITY_RAVAGER_CELEBRATE;
            } else {
                continue;
            }
            world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                    sound, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    // ── Celebration state query ───────────────────────────────────────────────

    public static boolean isCelebrating(UUID playerUuid) {
        return CELEBRATION_END_TICK.containsKey(playerUuid);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public static void clearAll(UUID playerUuid) {
        CELEBRATION_END_TICK.remove(playerUuid);
        HAD_VILLAGERS.remove(playerUuid);
    }
}
