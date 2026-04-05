package net.sam.samrequiemmod.possession.illager;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side state for summoned pillagers (spawned via goat horn by a captain).
 * Tracks which pillagers belong to which captain, reverse lookups, and cooldowns.
 */
public final class SummonedPillagerState {

    private SummonedPillagerState() {}

    /** Captain UUID -> set of summoned pillager UUIDs */
    private static final Map<UUID, Set<UUID>> SUMMONED_PILLAGERS = new ConcurrentHashMap<>();

    /** Pillager UUID -> Captain UUID (reverse lookup) */
    private static final Map<UUID, UUID> SUMMONED_TO_CAPTAIN = new ConcurrentHashMap<>();

    /** Captain UUID -> tick when summon cooldown expires (15 seconds = 300 ticks) */
    private static final Map<UUID, Long> SUMMON_COOLDOWN = new ConcurrentHashMap<>();

    /** Captains who have summoned at least once (used to know when to start cooldown) */
    private static final Set<UUID> HAD_SUMMONED = ConcurrentHashMap.newKeySet();

    public static void addSummoned(UUID captainUuid, UUID pillagerUuid) {
        SUMMONED_PILLAGERS.computeIfAbsent(captainUuid, k -> ConcurrentHashMap.newKeySet()).add(pillagerUuid);
        SUMMONED_TO_CAPTAIN.put(pillagerUuid, captainUuid);
        HAD_SUMMONED.add(captainUuid);
    }

    public static void removeSummoned(UUID captainUuid, UUID pillagerUuid) {
        Set<UUID> set = SUMMONED_PILLAGERS.get(captainUuid);
        if (set != null) set.remove(pillagerUuid);
        SUMMONED_TO_CAPTAIN.remove(pillagerUuid);
    }

    public static boolean isSummoned(UUID pillagerUuid) {
        return SUMMONED_TO_CAPTAIN.containsKey(pillagerUuid);
    }

    public static boolean isSummonedBy(UUID captainUuid, UUID pillagerUuid) {
        return captainUuid.equals(SUMMONED_TO_CAPTAIN.get(pillagerUuid));
    }

    public static UUID getCaptain(UUID pillagerUuid) {
        return SUMMONED_TO_CAPTAIN.get(pillagerUuid);
    }

    public static Set<UUID> getSummoned(UUID captainUuid) {
        Set<UUID> set = SUMMONED_PILLAGERS.get(captainUuid);
        return set != null ? set : Set.of();
    }

    public static boolean hasSummoned(UUID captainUuid) {
        Set<UUID> set = SUMMONED_PILLAGERS.get(captainUuid);
        return set != null && !set.isEmpty();
    }

    public static boolean hadSummoned(UUID captainUuid) {
        return HAD_SUMMONED.contains(captainUuid);
    }

    /** Despawn all summoned pillagers from the world and clear tracking. */
    public static void despawnAll(UUID captainUuid, ServerWorld world) {
        Set<UUID> set = SUMMONED_PILLAGERS.remove(captainUuid);
        if (set == null) return;
        for (UUID pillagerUuid : new HashSet<>(set)) {
            SUMMONED_TO_CAPTAIN.remove(pillagerUuid);
            Entity entity = world.getEntity(pillagerUuid);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
    }

    public static boolean isOnCooldown(UUID captainUuid, long currentTick) {
        Long expiry = SUMMON_COOLDOWN.get(captainUuid);
        if (expiry == null) return false;
        if (currentTick >= expiry) {
            SUMMON_COOLDOWN.remove(captainUuid);
            return false;
        }
        return true;
    }

    public static void startCooldown(UUID captainUuid, long currentTick) {
        SUMMON_COOLDOWN.put(captainUuid, currentTick + 300L); // 15 seconds
    }

    /** Check if all summoned pillagers are gone and start cooldown if so. */
    public static void checkAndStartCooldown(UUID captainUuid, long currentTick) {
        if (hadSummoned(captainUuid) && !hasSummoned(captainUuid)) {
            startCooldown(captainUuid, currentTick);
            HAD_SUMMONED.remove(captainUuid);
        }
    }

    /** Full cleanup for a captain (on unpossess, disconnect, etc.) */
    public static void clearAll(UUID captainUuid) {
        Set<UUID> set = SUMMONED_PILLAGERS.remove(captainUuid);
        if (set != null) {
            for (UUID p : set) SUMMONED_TO_CAPTAIN.remove(p);
        }
        SUMMON_COOLDOWN.remove(captainUuid);
        HAD_SUMMONED.remove(captainUuid);
    }
}






