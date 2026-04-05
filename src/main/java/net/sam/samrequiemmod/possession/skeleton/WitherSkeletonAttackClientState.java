package net.sam.samrequiemmod.possession.skeleton;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side: tracks which players are in wither skeleton "arms out" attacking state.
 */
public final class WitherSkeletonAttackClientState {

    private static final Set<UUID> ATTACKING = ConcurrentHashMap.newKeySet();

    private WitherSkeletonAttackClientState() {}

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






