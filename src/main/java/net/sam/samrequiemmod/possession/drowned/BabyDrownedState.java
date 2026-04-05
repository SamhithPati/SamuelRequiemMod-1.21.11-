package net.sam.samrequiemmod.possession.drowned;

import net.minecraft.entity.player.PlayerEntity;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BabyDrownedState {
    private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT = ConcurrentHashMap.newKeySet();
    private BabyDrownedState() {}

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) SERVER.add(uuid); else SERVER.remove(uuid);
    }
    public static boolean isServerBaby(PlayerEntity player) {
        return SERVER.contains(player.getUuid());
    }
    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT.add(uuid); else CLIENT.remove(uuid);
    }
    public static boolean isClientBaby(UUID uuid) {
        return CLIENT.contains(uuid);
    }
    /** Works on both sides — used by dimensions mixin. */
    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return SERVER.contains(uuid) || CLIENT.contains(uuid);
    }
}





