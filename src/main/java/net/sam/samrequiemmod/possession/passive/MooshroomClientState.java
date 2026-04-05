package net.sam.samrequiemmod.possession.passive;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MooshroomClientState {

    private static final Map<UUID, Boolean> BROWN_MOOSHROOM_PLAYERS = new ConcurrentHashMap<>();

    private MooshroomClientState() {}

    public static void setBrownMooshroom(UUID playerUuid, boolean isBrown) {
        if (isBrown) {
            BROWN_MOOSHROOM_PLAYERS.put(playerUuid, true);
        } else {
            BROWN_MOOSHROOM_PLAYERS.remove(playerUuid);
        }
    }

    public static boolean isBrownMooshroom(UUID playerUuid) {
        return BROWN_MOOSHROOM_PLAYERS.getOrDefault(playerUuid, false);
    }

    public static void clear() {
        BROWN_MOOSHROOM_PLAYERS.clear();
    }
}






