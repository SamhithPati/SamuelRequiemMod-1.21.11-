package net.sam.samrequiemmod.possession.piglin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how many ticks a piglin-possessed player has been in the overworld.
 * After 400 ticks (20 seconds) they convert to a zombified piglin.
 */
public final class OverworldConversionTracker {

    private static final Map<UUID, Integer> OVERWORLD_TICKS = new ConcurrentHashMap<>();

    private OverworldConversionTracker() {}

    public static void tick(UUID uuid) {
        OVERWORLD_TICKS.merge(uuid, 1, Integer::sum);
    }

    public static void reset(UUID uuid) {
        OVERWORLD_TICKS.remove(uuid);
    }

    public static int getTicks(UUID uuid) {
        return OVERWORLD_TICKS.getOrDefault(uuid, 0);
    }
}






