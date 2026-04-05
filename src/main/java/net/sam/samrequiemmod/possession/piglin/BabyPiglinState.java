package net.sam.samrequiemmod.possession.piglin;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BabyPiglinState {

    private static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT = ConcurrentHashMap.newKeySet();

    private BabyPiglinState() {}

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

    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return SERVER.contains(uuid) || CLIENT.contains(uuid);
    }
}






