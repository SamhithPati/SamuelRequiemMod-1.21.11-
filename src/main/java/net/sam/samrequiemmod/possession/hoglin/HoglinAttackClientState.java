package net.sam.samrequiemmod.possession.hoglin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HoglinAttackClientState {

    private static final Map<UUID, Long> ATTACK_END_TIME_MS = new ConcurrentHashMap<>();

    private HoglinAttackClientState() {}

    public static void trigger(UUID playerUuid, int durationTicks) {
        long endTime = System.currentTimeMillis() + (durationTicks * 50L);
        ATTACK_END_TIME_MS.put(playerUuid, endTime);
    }

    public static int getRemainingTicks(UUID playerUuid) {
        long remainingMs = ATTACK_END_TIME_MS.getOrDefault(playerUuid, 0L) - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            ATTACK_END_TIME_MS.remove(playerUuid);
            return 0;
        }
        return (int) Math.ceil(remainingMs / 50.0);
    }
}
