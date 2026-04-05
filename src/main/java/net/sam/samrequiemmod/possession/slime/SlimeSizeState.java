package net.sam.samrequiemmod.possession.slime;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlimeSizeState {

    public static final int BIG = 4;
    public static final int MEDIUM = 2;
    public static final int SMALL = 1;

    private static final Map<UUID, Integer> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CLIENT = new ConcurrentHashMap<>();

    private SlimeSizeState() {}

    public static void setServerSize(UUID uuid, int size) {
        SERVER.put(uuid, normalize(size));
    }

    public static int getServerSize(PlayerEntity player) {
        return SERVER.getOrDefault(player.getUuid(), BIG);
    }

    public static void setClientSize(UUID uuid, int size) {
        CLIENT.put(uuid, normalize(size));
    }

    public static int getClientSize(UUID uuid) {
        return CLIENT.getOrDefault(uuid, BIG);
    }

    public static int getSize(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return SERVER.getOrDefault(uuid, CLIENT.getOrDefault(uuid, BIG));
    }

    public static void clear(UUID uuid) {
        SERVER.remove(uuid);
        CLIENT.remove(uuid);
    }

    private static int normalize(int size) {
        if (size <= SMALL) return SMALL;
        if (size <= MEDIUM) return MEDIUM;
        return BIG;
    }
}






