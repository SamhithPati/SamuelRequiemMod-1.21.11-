package net.sam.samrequiemmod.possession.hoglin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HoglinConversionTracker {

    private static final Map<UUID, Integer> OVERWORLD_TICKS = new ConcurrentHashMap<>();

    private HoglinConversionTracker() {}

    public static void tick(UUID uuid) {
        OVERWORLD_TICKS.merge(uuid, 1, Integer::sum);
    }

    public static int getTicks(UUID uuid) {
        return OVERWORLD_TICKS.getOrDefault(uuid, 0);
    }

    public static void reset(UUID uuid) {
        OVERWORLD_TICKS.remove(uuid);
    }
}
