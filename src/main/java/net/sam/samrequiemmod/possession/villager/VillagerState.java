package net.sam.samrequiemmod.possession.villager;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VillagerState {

    private static final Set<UUID> SERVER_BABY = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_BABY = ConcurrentHashMap.newKeySet();

    private VillagerState() {}

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) SERVER_BABY.add(uuid); else SERVER_BABY.remove(uuid);
    }

    public static boolean isServerBaby(PlayerEntity player) {
        return SERVER_BABY.contains(player.getUuid());
    }

    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT_BABY.add(uuid); else CLIENT_BABY.remove(uuid);
    }

    public static boolean isClientBaby(UUID uuid) {
        return CLIENT_BABY.contains(uuid);
    }

    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return SERVER_BABY.contains(uuid) || CLIENT_BABY.contains(uuid);
    }
}






