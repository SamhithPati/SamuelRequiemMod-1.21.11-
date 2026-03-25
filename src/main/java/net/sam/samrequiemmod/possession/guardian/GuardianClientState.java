package net.sam.samrequiemmod.possession.guardian;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuardianClientState {

    private static final Map<UUID, BeamState> BEAMS = new ConcurrentHashMap<>();

    private GuardianClientState() {}

    public static void setBeam(UUID playerUuid, UUID targetUuid, int warmupTicks) {
        if (targetUuid == null) {
            BEAMS.remove(playerUuid);
            return;
        }
        BEAMS.put(playerUuid, new BeamState(targetUuid, warmupTicks, System.currentTimeMillis()));
    }

    public static UUID getTargetUuid(UUID playerUuid) {
        BeamState state = BEAMS.get(playerUuid);
        if (state == null) return null;
        if (getElapsedTicks(playerUuid) > state.warmupTicks + 5) {
            BEAMS.remove(playerUuid);
            return null;
        }
        return state.targetUuid;
    }

    public static int getElapsedTicks(UUID playerUuid) {
        BeamState state = BEAMS.get(playerUuid);
        if (state == null) return 0;
        long elapsedMs = System.currentTimeMillis() - state.startedAtMs;
        return (int) Math.max(0L, elapsedMs / 50L);
    }

    private record BeamState(UUID targetUuid, int warmupTicks, long startedAtMs) {}
}
