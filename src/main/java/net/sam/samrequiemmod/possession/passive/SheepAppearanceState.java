package net.sam.samrequiemmod.possession.passive;

import net.minecraft.util.DyeColor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SheepAppearanceState {

    private static final Map<UUID, DyeColor> SERVER_COLORS = new ConcurrentHashMap<>();
    private static final Map<UUID, DyeColor> CLIENT_COLORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SERVER_SHEARED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> CLIENT_SHEARED = new ConcurrentHashMap<>();

    private SheepAppearanceState() {}

    public static void setServerAppearance(UUID uuid, DyeColor color, boolean sheared) {
        SERVER_COLORS.put(uuid, color);
        SERVER_SHEARED.put(uuid, sheared);
    }

    public static DyeColor getServerColor(UUID uuid) {
        return SERVER_COLORS.getOrDefault(uuid, DyeColor.WHITE);
    }

    public static boolean isServerSheared(UUID uuid) {
        return SERVER_SHEARED.getOrDefault(uuid, false);
    }

    public static void setClientAppearance(UUID uuid, DyeColor color, boolean sheared) {
        CLIENT_COLORS.put(uuid, color);
        CLIENT_SHEARED.put(uuid, sheared);
    }

    public static DyeColor getClientColor(UUID uuid) {
        return CLIENT_COLORS.getOrDefault(uuid, DyeColor.WHITE);
    }

    public static boolean isClientSheared(UUID uuid) {
        return CLIENT_SHEARED.getOrDefault(uuid, false);
    }

    public static void clear(UUID uuid) {
        SERVER_COLORS.remove(uuid);
        CLIENT_COLORS.remove(uuid);
        SERVER_SHEARED.remove(uuid);
        CLIENT_SHEARED.remove(uuid);
    }

    public static void clearAllClient() {
        CLIENT_COLORS.clear();
        CLIENT_SHEARED.clear();
    }
}
