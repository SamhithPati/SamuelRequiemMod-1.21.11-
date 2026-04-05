package net.sam.samrequiemmod.possession.zombie;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state for zombie possession targeting logic.
 * Kept in a separate class so both the controller (server events)
 * and the MobEntityTargetMixin can access it without circular imports.
 */
public final class ZombieTargetingState {

    // Mob UUIDs that have dealt damage to the zombie player.
    private static final Set<UUID> PROVOKED_MOBS = ConcurrentHashMap.newKeySet();

    // Maps player UUID → set of mob UUIDs they provoked during this possession session.
    private static final java.util.Map<UUID, Set<UUID>> PLAYER_PROVOKED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ZombieTargetingState() {
    }

    public static void markProvoked(UUID mobUuid) {
        PROVOKED_MOBS.add(mobUuid);
    }

    /** Mark a mob as provoked AND record which player provoked it. */
    public static void markProvoked(UUID mobUuid, UUID playerUuid) {
        PROVOKED_MOBS.add(mobUuid);
        PLAYER_PROVOKED.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet()).add(mobUuid);
    }

    public static boolean isProvoked(UUID mobUuid) {
        return PROVOKED_MOBS.contains(mobUuid);
    }

    public static void clearProvoked(UUID mobUuid) {
        PROVOKED_MOBS.remove(mobUuid);
    }

    /** Returns all mob UUIDs this player provoked, and clears the record. */
    public static Set<UUID> clearPlayerProvoked(UUID playerUuid) {
        Set<UUID> mobs = PLAYER_PROVOKED.remove(playerUuid);
        return mobs != null ? mobs : java.util.Collections.emptySet();
    }
}





