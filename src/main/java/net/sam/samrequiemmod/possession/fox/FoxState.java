package net.sam.samrequiemmod.possession.fox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FoxState {

    private static final Map<UUID, String> SERVER_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> CLIENT_VARIANTS = new ConcurrentHashMap<>();

    private FoxState() {}

    public static void setServerVariant(UUID uuid, String variantId) {
        SERVER_VARIANTS.put(uuid, variantId);
    }

    public static String getServerVariant(UUID uuid) {
        return SERVER_VARIANTS.getOrDefault(uuid, "red");
    }

    public static void setClientVariant(UUID uuid, String variantId) {
        CLIENT_VARIANTS.put(uuid, variantId);
    }

    public static String getClientVariant(UUID uuid) {
        return CLIENT_VARIANTS.getOrDefault(uuid, "red");
    }

    public static void clear(UUID uuid) {
        SERVER_VARIANTS.remove(uuid);
        CLIENT_VARIANTS.remove(uuid);
    }
}
