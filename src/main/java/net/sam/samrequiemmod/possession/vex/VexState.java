package net.sam.samrequiemmod.possession.vex;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VexState {

    private static final Set<UUID> SERVER_ANGRY = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_ANGRY = ConcurrentHashMap.newKeySet();

    private VexState() {}

    public static void setServerAngry(UUID uuid, boolean angry) {
        if (angry) SERVER_ANGRY.add(uuid); else SERVER_ANGRY.remove(uuid);
    }

    public static boolean isServerAngry(UUID uuid) {
        return SERVER_ANGRY.contains(uuid);
    }

    public static void setClientAngry(UUID uuid, boolean angry) {
        if (angry) CLIENT_ANGRY.add(uuid); else CLIENT_ANGRY.remove(uuid);
    }

    public static boolean isClientAngry(UUID uuid) {
        return CLIENT_ANGRY.contains(uuid);
    }

    public static void clear(UUID uuid) {
        SERVER_ANGRY.remove(uuid);
        CLIENT_ANGRY.remove(uuid);
    }
}
