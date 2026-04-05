package net.sam.samrequiemmod.possession.firemob;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FireMobAttackClientState {

    private static final Map<UUID, Long> BLAZE_ACTIVE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GHAST_ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private FireMobAttackClientState() {}

    public static void triggerBlaze(UUID playerUuid, int durationTicks) {
        BLAZE_ACTIVE_UNTIL.put(playerUuid, System.currentTimeMillis() + durationTicks * 50L);
    }

    public static void triggerGhast(UUID playerUuid, int durationTicks) {
        GHAST_ACTIVE_UNTIL.put(playerUuid, System.currentTimeMillis() + durationTicks * 50L);
    }

    public static boolean isBlazeActive(UUID playerUuid) {
        return isActive(BLAZE_ACTIVE_UNTIL, playerUuid);
    }

    public static boolean isGhastShooting(UUID playerUuid) {
        return isActive(GHAST_ACTIVE_UNTIL, playerUuid);
    }

    private static boolean isActive(Map<UUID, Long> map, UUID playerUuid) {
        long until = map.getOrDefault(playerUuid, 0L);
        if (until <= System.currentTimeMillis()) {
            map.remove(playerUuid);
            return false;
        }
        return true;
    }
}






