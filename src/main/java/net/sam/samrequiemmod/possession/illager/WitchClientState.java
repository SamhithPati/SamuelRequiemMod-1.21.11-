package net.sam.samrequiemmod.possession.illager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state for witch possession — tracks which players are
 * currently in the potion-drinking animation so the renderer can
 * call {@code WitchEntity.setDrinking(true)} on the shell entity.
 */
public final class WitchClientState {

    private WitchClientState() {}

    private static final Set<UUID> DRINKING_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void setDrinking(UUID playerUuid, boolean drinking) {
        if (drinking) DRINKING_PLAYERS.add(playerUuid);
        else DRINKING_PLAYERS.remove(playerUuid);
    }

    public static boolean isDrinking(UUID playerUuid) {
        return DRINKING_PLAYERS.contains(playerUuid);
    }
}






