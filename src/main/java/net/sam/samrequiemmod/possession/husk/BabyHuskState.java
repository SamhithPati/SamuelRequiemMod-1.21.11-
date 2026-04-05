package net.sam.samrequiemmod.possession.husk;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BabyHuskState {

    private static final Set<UUID> BABY_HUSK_PLAYERS        = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_BABY_HUSK_PLAYERS = ConcurrentHashMap.newKeySet();

    private BabyHuskState() {}

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) BABY_HUSK_PLAYERS.add(uuid);
        else      BABY_HUSK_PLAYERS.remove(uuid);
    }

    public static boolean isServerBaby(PlayerEntity player) {
        return BABY_HUSK_PLAYERS.contains(player.getUuid());
    }

    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT_BABY_HUSK_PLAYERS.add(uuid);
        else      CLIENT_BABY_HUSK_PLAYERS.remove(uuid);
    }

    public static boolean isClientBaby(UUID uuid) {
        return CLIENT_BABY_HUSK_PLAYERS.contains(uuid);
    }

    /** Works on both server and client — used by the dimensions mixin. */
    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return BABY_HUSK_PLAYERS.contains(uuid) || CLIENT_BABY_HUSK_PLAYERS.contains(uuid);
    }
}





