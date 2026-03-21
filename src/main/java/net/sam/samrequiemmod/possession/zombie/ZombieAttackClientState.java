package net.sam.samrequiemmod.possession.zombie;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side: tracks which players are currently in the zombie arms-raised state.
 */
public final class ZombieAttackClientState {

    private static final Set<UUID> ATTACKING = ConcurrentHashMap.newKeySet();

    private ZombieAttackClientState() {
    }

    public static void set(UUID playerUuid, boolean attacking) {
        if (attacking) {
            ATTACKING.add(playerUuid);
        } else {
            ATTACKING.remove(playerUuid);
        }
    }

    public static boolean isAttacking(UUID playerUuid) {
        return ATTACKING.contains(playerUuid);
    }

    public static void clearAll() {
        ATTACKING.clear();
    }
}
