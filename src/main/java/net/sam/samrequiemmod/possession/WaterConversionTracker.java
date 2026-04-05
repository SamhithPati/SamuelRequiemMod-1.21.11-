package net.sam.samrequiemmod.possession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how many ticks a possessed player has been submerged in water.
 * Used for zombie→drowned and husk→zombie conversion mechanics.
 */
public final class WaterConversionTracker {

    // UUID → ticks submerged
    private static final Map<UUID, Integer> SUBMERGED_TICKS = new ConcurrentHashMap<>();

    private WaterConversionTracker() {}

    public static void tickSubmerged(UUID uuid) {
        SUBMERGED_TICKS.merge(uuid, 1, Integer::sum);
    }

    public static void resetSubmerged(UUID uuid) {
        SUBMERGED_TICKS.remove(uuid);
    }

    public static int getSubmergedTicks(UUID uuid) {
        return SUBMERGED_TICKS.getOrDefault(uuid, 0);
    }
}





