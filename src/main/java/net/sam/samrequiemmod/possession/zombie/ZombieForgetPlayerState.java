package net.sam.samrequiemmod.possession.zombie;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks mobs that should forget a specific player for an extended window
 * after possession ends. Used to prevent neutral mobs (especially iron golems)
 * from continuing to chase the player after they unpossess.
 *
 * Maps mob UUID → (player UUID, expiry tick).
 */
public final class ZombieForgetPlayerState {

    // mob UUID → [playerUUID, expiryTick]
    private static final Map<UUID, long[]> FORGET_MAP = new ConcurrentHashMap<>();

    private ZombieForgetPlayerState() {}

    /**
     * Register a mob to forget a specific player for the given number of ticks.
     */
    public static void registerForget(UUID mobUuid, UUID playerUuid, long currentTick, int durationTicks) {
        FORGET_MAP.put(mobUuid, new long[]{
                playerUuid.getMostSignificantBits(),
                playerUuid.getLeastSignificantBits(),
                currentTick + durationTicks
        });
    }

    public static UUID getTrackedPlayer(UUID mobUuid) {
        long[] entry = FORGET_MAP.get(mobUuid);
        if (entry == null) return null;
        return new UUID(entry[0], entry[1]);
    }

    public static long getExpiry(UUID mobUuid) {
        long[] entry = FORGET_MAP.get(mobUuid);
        return entry == null ? 0 : entry[2];
    }

    public static boolean isActive(UUID mobUuid, long currentTick) {
        long[] entry = FORGET_MAP.get(mobUuid);
        if (entry == null) return false;
        if (currentTick >= entry[2]) { FORGET_MAP.remove(mobUuid); return false; }
        return true;
    }

    public static void removeAll() {
        FORGET_MAP.clear();
    }

    public static java.util.Set<UUID> activeMobUuids() {
        return FORGET_MAP.keySet();
    }
}





