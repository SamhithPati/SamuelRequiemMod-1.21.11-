package net.sam.samrequiemmod.possession.wolf;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WolfState {

    private static final Set<UUID> SERVER_ANGRY = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_ANGRY = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> SERVER_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> CLIENT_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIENT_SHAKE_UNTIL = new ConcurrentHashMap<>();

    private WolfState() {}

    public static void setServerAngry(UUID uuid, boolean angry) {
        if (angry) SERVER_ANGRY.add(uuid); else SERVER_ANGRY.remove(uuid);
    }

    public static boolean isServerAngry(UUID uuid) {
        return SERVER_ANGRY.contains(uuid);
    }

    public static void setClientAngry(UUID uuid, boolean angry) {
        if (angry) CLIENT_ANGRY.add(uuid); else CLIENT_ANGRY.remove(uuid);
    }

    public static boolean isClientAngry(UUID uuid) {
        return CLIENT_ANGRY.contains(uuid);
    }

    public static void setServerVariant(UUID uuid, String variantId) {
        SERVER_VARIANTS.put(uuid, variantId);
    }

    public static String getServerVariant(UUID uuid) {
        return SERVER_VARIANTS.getOrDefault(uuid, "minecraft:pale");
    }

    public static void setClientVariant(UUID uuid, String variantId) {
        CLIENT_VARIANTS.put(uuid, variantId);
    }

    public static String getClientVariant(UUID uuid) {
        return CLIENT_VARIANTS.getOrDefault(uuid, "minecraft:pale");
    }

    public static void startClientShake(UUID uuid, int currentClientAge, int durationTicks) {
        CLIENT_SHAKE_UNTIL.put(uuid, currentClientAge + durationTicks);
    }

    public static boolean isClientShaking(UUID uuid, int currentClientAge) {
        Integer until = CLIENT_SHAKE_UNTIL.get(uuid);
        if (until == null) return false;
        if (currentClientAge >= until) {
            CLIENT_SHAKE_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clear(UUID uuid) {
        SERVER_ANGRY.remove(uuid);
        CLIENT_ANGRY.remove(uuid);
        SERVER_VARIANTS.remove(uuid);
        CLIENT_VARIANTS.remove(uuid);
        CLIENT_SHAKE_UNTIL.remove(uuid);
    }
}
