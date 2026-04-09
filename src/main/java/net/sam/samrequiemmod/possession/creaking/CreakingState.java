package net.sam.samrequiemmod.possession.creaking;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CreakingState {

    private static final Map<UUID, BlockPos> SERVER_HEART_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_INVULNERABLE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_INVULNERABLE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_CRUMBLING_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CLIENT_CRUMBLING_UNTIL = new ConcurrentHashMap<>();

    private CreakingState() {
    }

    public static void setServerHeartPos(UUID uuid, BlockPos pos) {
        if (pos == null) {
            SERVER_HEART_POSITIONS.remove(uuid);
        } else {
            SERVER_HEART_POSITIONS.put(uuid, pos.toImmutable());
        }
    }

    public static BlockPos getServerHeartPos(UUID uuid) {
        return SERVER_HEART_POSITIONS.get(uuid);
    }

    public static boolean hasServerHeart(UUID uuid) {
        return SERVER_HEART_POSITIONS.containsKey(uuid);
    }

    public static void setServerInvulnerableUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) {
            SERVER_INVULNERABLE_UNTIL.remove(uuid);
        } else {
            SERVER_INVULNERABLE_UNTIL.put(uuid, untilTick);
        }
    }

    public static boolean isServerInvulnerable(UUID uuid, long currentTick) {
        long until = SERVER_INVULNERABLE_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            SERVER_INVULNERABLE_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static long getServerInvulnerableUntil(UUID uuid) {
        return SERVER_INVULNERABLE_UNTIL.getOrDefault(uuid, 0L);
    }

    public static void setClientInvulnerableUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) {
            CLIENT_INVULNERABLE_UNTIL.remove(uuid);
        } else {
            CLIENT_INVULNERABLE_UNTIL.put(uuid, untilTick);
        }
    }

    public static boolean isClientInvulnerable(UUID uuid, long currentTick) {
        long until = CLIENT_INVULNERABLE_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            CLIENT_INVULNERABLE_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void setServerCrumblingUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) {
            SERVER_CRUMBLING_UNTIL.remove(uuid);
        } else {
            SERVER_CRUMBLING_UNTIL.put(uuid, untilTick);
        }
    }

    public static long getServerCrumblingUntil(UUID uuid) {
        return SERVER_CRUMBLING_UNTIL.getOrDefault(uuid, 0L);
    }

    public static boolean isServerCrumbling(UUID uuid, long currentTick) {
        long until = SERVER_CRUMBLING_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            SERVER_CRUMBLING_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void setClientCrumblingUntil(UUID uuid, long untilTick) {
        if (untilTick <= 0L) {
            CLIENT_CRUMBLING_UNTIL.remove(uuid);
        } else {
            CLIENT_CRUMBLING_UNTIL.put(uuid, untilTick);
        }
    }

    public static boolean isClientCrumbling(UUID uuid, long currentTick) {
        long until = CLIENT_CRUMBLING_UNTIL.getOrDefault(uuid, 0L);
        if (until <= currentTick) {
            CLIENT_CRUMBLING_UNTIL.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clear(UUID uuid) {
        SERVER_HEART_POSITIONS.remove(uuid);
        SERVER_INVULNERABLE_UNTIL.remove(uuid);
        CLIENT_INVULNERABLE_UNTIL.remove(uuid);
        SERVER_CRUMBLING_UNTIL.remove(uuid);
        CLIENT_CRUMBLING_UNTIL.remove(uuid);
    }
}
