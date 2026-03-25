package net.sam.samrequiemmod.possession.beast;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BeastAttackClientState {

    private static final Map<UUID, Long> POLAR_ATTACK_START = new ConcurrentHashMap<>();
    private static final int POLAR_ATTACK_DURATION = 12;

    private BeastAttackClientState() {}

    public static void triggerPolar(UUID uuid, long playerAge) {
        POLAR_ATTACK_START.put(uuid, playerAge);
    }

    public static boolean isPolarAttacking(UUID uuid, long currentAge) {
        Long start = POLAR_ATTACK_START.get(uuid);
        if (start == null) return false;
        if (currentAge - start >= POLAR_ATTACK_DURATION) {
            POLAR_ATTACK_START.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clear(UUID uuid) {
        POLAR_ATTACK_START.remove(uuid);
    }
}
