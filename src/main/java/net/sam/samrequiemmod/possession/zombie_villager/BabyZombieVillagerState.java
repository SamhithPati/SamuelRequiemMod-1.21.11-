package net.sam.samrequiemmod.possession.zombie_villager;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BabyZombieVillagerState {

    private static final Set<UUID> BABY_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_BABY_PLAYERS = ConcurrentHashMap.newKeySet();

    private BabyZombieVillagerState() {}

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) BABY_PLAYERS.add(uuid);
        else      BABY_PLAYERS.remove(uuid);
    }

    public static boolean isServerBaby(PlayerEntity player) {
        return BABY_PLAYERS.contains(player.getUuid());
    }

    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT_BABY_PLAYERS.add(uuid);
        else      CLIENT_BABY_PLAYERS.remove(uuid);
    }

    public static boolean isClientBaby(UUID uuid) {
        return CLIENT_BABY_PLAYERS.contains(uuid);
    }

    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return BABY_PLAYERS.contains(uuid) || CLIENT_BABY_PLAYERS.contains(uuid);
    }
}
