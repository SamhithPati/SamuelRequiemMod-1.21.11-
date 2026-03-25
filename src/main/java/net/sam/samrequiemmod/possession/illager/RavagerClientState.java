package net.sam.samrequiemmod.possession.illager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state for ravager possession animations.
 * Tracks when a possessed player last performed a bite or roar,
 * so the renderer can show the corresponding animation.
 */
public final class RavagerClientState {

    private RavagerClientState() {}

    /** Player UUID -> tick when bite animation started. */
    private static final Map<UUID, Integer> BITE_START_TICK = new ConcurrentHashMap<>();

    /** Player UUID -> tick when roar animation started. */
    private static final Map<UUID, Integer> ROAR_START_TICK = new ConcurrentHashMap<>();

    /** Duration of bite animation in ticks. */
    public static final int BITE_ANIM_DURATION = 10;

    /** Duration of roar animation in ticks. */
    public static final int ROAR_ANIM_DURATION = 20;

    public static void setBiting(UUID playerUuid, int startTick) {
        BITE_START_TICK.put(playerUuid, startTick);
    }

    public static void setRoaring(UUID playerUuid, int startTick) {
        ROAR_START_TICK.put(playerUuid, startTick);
    }

    /** Returns true if the bite animation is currently active. */
    public static boolean isBiting(UUID playerUuid, int currentAge) {
        Integer start = BITE_START_TICK.get(playerUuid);
        if (start == null) return false;
        return (currentAge - start) < BITE_ANIM_DURATION;
    }

    /**
     * Returns the remaining attackTick countdown (10 -> 0) matching vanilla's decrement.
     * The RavagerEntityModel reads attackTick and expects it to count down from 10.
     */
    public static int getBiteTicksRemaining(UUID playerUuid, int currentAge) {
        Integer start = BITE_START_TICK.get(playerUuid);
        if (start == null) return 0;
        int elapsed = currentAge - start;
        if (elapsed < 0 || elapsed >= BITE_ANIM_DURATION) return 0;
        return BITE_ANIM_DURATION - elapsed; // counts down from 10 to 1
    }

    /** Returns true if the roar animation is currently active. */
    public static boolean isRoaring(UUID playerUuid, int currentAge) {
        Integer start = ROAR_START_TICK.get(playerUuid);
        if (start == null) return false;
        return (currentAge - start) < ROAR_ANIM_DURATION;
    }

    /**
     * Returns the remaining roarTick countdown (20 -> 0) matching vanilla's decrement.
     * The RavagerEntityModel reads roarTick and expects it to count down from 20.
     */
    public static int getRoarTicksRemaining(UUID playerUuid, int currentAge) {
        Integer start = ROAR_START_TICK.get(playerUuid);
        if (start == null) return 0;
        int elapsed = currentAge - start;
        if (elapsed < 0 || elapsed >= ROAR_ANIM_DURATION) return 0;
        return ROAR_ANIM_DURATION - elapsed; // counts down from 20 to 1
    }

    public static void clear(UUID playerUuid) {
        BITE_START_TICK.remove(playerUuid);
        ROAR_START_TICK.remove(playerUuid);
    }
}
