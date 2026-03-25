package net.sam.samrequiemmod.possession.enderman;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state for enderman possession — tracks which players
 * are currently in the angry/aggressive state so the renderer can
 * display the open-mouth texture and play aggressive sounds.
 */
public final class EndermanClientState {

    private EndermanClientState() {}

    private static final Set<UUID> ANGRY_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void setAngry(UUID playerUuid, boolean angry) {
        if (angry) ANGRY_PLAYERS.add(playerUuid);
        else ANGRY_PLAYERS.remove(playerUuid);
    }

    public static boolean isAngry(UUID playerUuid) {
        return ANGRY_PLAYERS.contains(playerUuid);
    }
}
