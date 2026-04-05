package net.sam.samrequiemmod.possession.feline;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CatState {

    private static final Map<UUID, String> SERVER_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> CLIENT_VARIANTS = new ConcurrentHashMap<>();

    private CatState() {}

    public static void setServerVariant(UUID uuid, String variantId) {
        SERVER_VARIANTS.put(uuid, variantId);
    }

    public static String getServerVariant(UUID uuid) {
        return SERVER_VARIANTS.getOrDefault(uuid, "minecraft:black");
    }

    public static void setClientVariant(UUID uuid, String variantId) {
        CLIENT_VARIANTS.put(uuid, variantId);
    }

    public static String getClientVariant(UUID uuid) {
        return CLIENT_VARIANTS.getOrDefault(uuid, "minecraft:black");
    }

    public static void clear(UUID uuid) {
        SERVER_VARIANTS.remove(uuid);
        CLIENT_VARIANTS.remove(uuid);
    }
}






