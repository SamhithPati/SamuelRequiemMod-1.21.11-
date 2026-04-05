package net.sam.samrequiemmod.possession.zombie;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which possessed zombie players are baby zombies.
 * Used server-side for behaviour logic and synced to clients for rendering.
 */
public final class BabyZombieState {

    // Server-side set (populated by server events)
    private static final Set<UUID> BABY_ZOMBIE_PLAYERS = ConcurrentHashMap.newKeySet();
    // Client-side set (populated by sync packet)
    private static final Set<UUID> CLIENT_BABY_ZOMBIE_PLAYERS = ConcurrentHashMap.newKeySet();

    private BabyZombieState() {}

    // ── Server side ───────────────────────────────────────────────────────────

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) BABY_ZOMBIE_PLAYERS.add(uuid);
        else      BABY_ZOMBIE_PLAYERS.remove(uuid);
    }

    public static boolean isServerBaby(PlayerEntity player) {
        return BABY_ZOMBIE_PLAYERS.contains(player.getUuid());
    }

    // ── Client side ───────────────────────────────────────────────────────────

    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT_BABY_ZOMBIE_PLAYERS.add(uuid);
        else      CLIENT_BABY_ZOMBIE_PLAYERS.remove(uuid);
    }

    public static boolean isClientBaby(UUID uuid) {
        return CLIENT_BABY_ZOMBIE_PLAYERS.contains(uuid);
    }

    // ── Unified: works on both server and client ──────────────────────────────

    /**
     * Returns true if the player is currently a baby zombie, regardless of which
     * logical side (server or client) this is called from.
     *
     * The mixin that sets camera/hitbox dimensions runs on BOTH sides, so we must
     * check both sets. On the server, BABY_ZOMBIE_PLAYERS is populated directly.
     * On the client, CLIENT_BABY_ZOMBIE_PLAYERS is populated via sync packet.
     */
    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return BABY_ZOMBIE_PLAYERS.contains(uuid) || CLIENT_BABY_ZOMBIE_PLAYERS.contains(uuid);
    }
}





