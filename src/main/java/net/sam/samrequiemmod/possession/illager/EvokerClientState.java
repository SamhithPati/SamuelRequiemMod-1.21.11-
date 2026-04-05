package net.sam.samrequiemmod.possession.illager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side state for evoker possession. */
public final class EvokerClientState {

    private EvokerClientState() {}

    private static final Map<UUID, UUID> TARGETS   = new ConcurrentHashMap<>();
    /** 0=none, 1=fangs, 2=vexes */
    private static final Map<UUID, Integer> CASTING = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CAST_UNTIL = new ConcurrentHashMap<>();

    public static void setTarget(UUID playerUuid, UUID targetUuid) {
        if (targetUuid == null) TARGETS.remove(playerUuid);
        else TARGETS.put(playerUuid, targetUuid);
    }

    public static UUID getTarget(UUID playerUuid) { return TARGETS.get(playerUuid); }

    /** castType 0 = stop. Duration is 2 seconds (2000 ms). */
    public static void setCasting(UUID playerUuid, int castType) {
        if (castType == 0) { CASTING.remove(playerUuid); CAST_UNTIL.remove(playerUuid); }
        else { CASTING.put(playerUuid, castType); CAST_UNTIL.put(playerUuid, System.currentTimeMillis() + 2000L); }
    }

    /** Returns current cast type, or 0 if expired. */
    public static int getCasting(UUID playerUuid) {
        Long until = CAST_UNTIL.get(playerUuid);
        if (until == null || System.currentTimeMillis() > until) {
            CASTING.remove(playerUuid); CAST_UNTIL.remove(playerUuid);
            return 0;
        }
        return CASTING.getOrDefault(playerUuid, 0);
    }
}





