package net.sam.samrequiemmod.possession.breeze;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BreezeClientState {

    private static final int INHALE_TICKS = 10;
    private static final int SHOOT_TICKS = 12;
    private static final int JUMP_TICKS = 14;

    private static final Map<UUID, Integer> SHOOT_START = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> JUMP_START = new ConcurrentHashMap<>();

    private BreezeClientState() {}

    public static void startShoot(UUID playerUuid, int startTick) {
        SHOOT_START.put(playerUuid, startTick);
    }

    public static void startJump(UUID playerUuid, int startTick) {
        JUMP_START.put(playerUuid, startTick);
    }

    public static boolean isInhaling(UUID playerUuid, int currentTick) {
        int start = SHOOT_START.getOrDefault(playerUuid, Integer.MIN_VALUE);
        int elapsed = currentTick - start;
        return elapsed >= 0 && elapsed < INHALE_TICKS;
    }

    public static boolean isShooting(UUID playerUuid, int currentTick) {
        int start = SHOOT_START.getOrDefault(playerUuid, Integer.MIN_VALUE);
        int elapsed = currentTick - start;
        return elapsed >= INHALE_TICKS && elapsed < INHALE_TICKS + SHOOT_TICKS;
    }

    public static boolean isJumping(UUID playerUuid, int currentTick) {
        int start = JUMP_START.getOrDefault(playerUuid, Integer.MIN_VALUE);
        int elapsed = currentTick - start;
        return elapsed >= 0 && elapsed < JUMP_TICKS;
    }

    public static void clear(UUID playerUuid) {
        SHOOT_START.remove(playerUuid);
        JUMP_START.remove(playerUuid);
    }
}
