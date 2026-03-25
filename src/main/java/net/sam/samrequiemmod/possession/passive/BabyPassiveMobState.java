package net.sam.samrequiemmod.possession.passive;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which possessed passive-mob players are babies.
 * Works for cow, pig, sheep, and chicken — the possession type
 * from PossessionManager determines which mob it is.
 */
public final class BabyPassiveMobState {

    private static final Set<UUID> SERVER_BABIES = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> CLIENT_BABIES = ConcurrentHashMap.newKeySet();

    private BabyPassiveMobState() {}

    // ── Server side ───────────────────────────────────────────────────────────

    public static void setServerBaby(UUID uuid, boolean baby) {
        if (baby) SERVER_BABIES.add(uuid);
        else      SERVER_BABIES.remove(uuid);
    }

    public static boolean isServerBaby(PlayerEntity player) {
        return SERVER_BABIES.contains(player.getUuid());
    }

    // ── Client side ───────────────────────────────────────────────────────────

    public static void setClientBaby(UUID uuid, boolean baby) {
        if (baby) CLIENT_BABIES.add(uuid);
        else      CLIENT_BABIES.remove(uuid);
    }

    public static boolean isClientBaby(UUID uuid) {
        return CLIENT_BABIES.contains(uuid);
    }

    // ── Unified ───────────────────────────────────────────────────────────────

    public static boolean isBaby(PlayerEntity player) {
        UUID uuid = player.getUuid();
        return SERVER_BABIES.contains(uuid) || CLIENT_BABIES.contains(uuid);
    }
}
