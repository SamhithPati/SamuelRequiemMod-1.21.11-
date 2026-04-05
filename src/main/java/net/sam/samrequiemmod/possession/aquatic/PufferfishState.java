package net.sam.samrequiemmod.possession.aquatic;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state for pufferfish possession — tracks which players
 * are currently in the puffed-up state so the renderer can display
 * the inflated pufferfish model.
 */
public final class PufferfishState {

    private PufferfishState() {}

    private static final Set<UUID> PUFFED_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void setPuffed(UUID playerUuid, boolean puffed) {
        if (puffed) PUFFED_PLAYERS.add(playerUuid);
        else PUFFED_PLAYERS.remove(playerUuid);
    }

    public static boolean isPuffed(UUID playerUuid) {
        return PUFFED_PLAYERS.contains(playerUuid);
    }
}






