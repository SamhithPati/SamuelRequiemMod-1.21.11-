package net.sam.samrequiemmod.possession.creeper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state for creeper possession rendering (charge animation, charged glow).
 */
public final class CreeperClientState {

    private CreeperClientState() {}

    /** Players currently charging — maps UUID to fuse tick count (0–29). */
    private static final Map<UUID, Integer> CHARGING_PLAYERS = new ConcurrentHashMap<>();

    /** Players struck by lightning (charged creeper glow). */
    private static final Set<UUID> CHARGED_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void setCharging(UUID uuid, boolean charging, int fuseTicks) {
        if (charging) {
            CHARGING_PLAYERS.put(uuid, fuseTicks);
        } else {
            CHARGING_PLAYERS.remove(uuid);
        }
    }

    public static boolean isCharging(UUID uuid) {
        return CHARGING_PLAYERS.containsKey(uuid);
    }

    public static int getFuseTicks(UUID uuid) {
        return CHARGING_PLAYERS.getOrDefault(uuid, 0);
    }

    public static void setCharged(UUID uuid, boolean charged) {
        if (charged) {
            CHARGED_PLAYERS.add(uuid);
        } else {
            CHARGED_PLAYERS.remove(uuid);
        }
    }

    public static boolean isCharged(UUID uuid) {
        return CHARGED_PLAYERS.contains(uuid);
    }

    public static void clearAll() {
        CHARGING_PLAYERS.clear();
        CHARGED_PLAYERS.clear();
    }
}






