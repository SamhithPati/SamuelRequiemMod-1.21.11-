package net.sam.samrequiemmod.possession.iron_golem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IronGolemClientState {

    private IronGolemClientState() {}

    /** Stores the player age tick when the attack animation started. */
    private static final Map<UUID, Integer> ATTACK_START_TICK = new ConcurrentHashMap<>();

    public static void setAttacking(UUID playerUuid, int playerAge) {
        ATTACK_START_TICK.put(playerUuid, playerAge);
    }

    /**
     * Returns remaining attack ticks (10 -> 0) for the arm swing animation.
     * Vanilla IronGolemEntity sets attackTicksLeft = 10 and counts down.
     */
    public static int getAttackTicksRemaining(UUID playerUuid, int currentAge) {
        Integer start = ATTACK_START_TICK.get(playerUuid);
        if (start == null) return 0;
        int elapsed = currentAge - start;
        if (elapsed >= 10) {
            ATTACK_START_TICK.remove(playerUuid);
            return 0;
        }
        return 10 - elapsed;
    }
}






