package net.sam.samrequiemmod.possession.aquatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the tropical fish variant (color/pattern) for possessed players.
 * Server-side stores the variant from the original mob; client-side
 * receives it via networking to render the correct visual.
 */
public final class TropicalFishVariantState {

    private TropicalFishVariantState() {}

    /** Server-side: player UUID -> tropical fish variant int. */
    private static final Map<UUID, Integer> SERVER_VARIANTS = new ConcurrentHashMap<>();

    /** Client-side: player UUID -> tropical fish variant int. */
    private static final Map<UUID, Integer> CLIENT_VARIANTS = new ConcurrentHashMap<>();

    // ── Server ─────────────────────────────────────────────────────────────────

    public static void setServerVariant(UUID playerUuid, int variant) {
        SERVER_VARIANTS.put(playerUuid, variant);
    }

    public static int getServerVariant(UUID playerUuid) {
        return SERVER_VARIANTS.getOrDefault(playerUuid, 0);
    }

    public static void removeServer(UUID playerUuid) {
        SERVER_VARIANTS.remove(playerUuid);
    }

    // ── Client ─────────────────────────────────────────────────────────────────

    public static void setClientVariant(UUID playerUuid, int variant) {
        CLIENT_VARIANTS.put(playerUuid, variant);
    }

    public static int getClientVariant(UUID playerUuid) {
        return CLIENT_VARIANTS.getOrDefault(playerUuid, 0);
    }

    public static void removeClient(UUID playerUuid) {
        CLIENT_VARIANTS.remove(playerUuid);
    }
}






