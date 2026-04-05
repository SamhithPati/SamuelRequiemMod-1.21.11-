package net.sam.samrequiemmod.possession;

import net.minecraft.server.network.ServerPlayerEntity;

public final class PossessionDimensionHelper {

    private PossessionDimensionHelper() {
    }

    // Tracks the last resolved possession profile dimensions so collision refreshes
    // whenever the effective size changes, not just for a few baby mob cases.
    private static final java.util.Map<java.util.UUID, LastProfileSnapshot> LAST_PROFILE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void refreshDimensionsIfNeeded(ServerPlayerEntity player) {
        var currentType = PossessionManager.getPossessedType(player);
        var currentProfile = PossessionProfileResolver.get(player);
        LastProfileSnapshot lastProfile = LAST_PROFILE.get(player.getUuid());

        boolean changed = !LastProfileSnapshot.matches(lastProfile, currentType, currentProfile);

        if (!changed) return;

        PossessionManager.setLastDimensionType(player, currentType);
        LastProfileSnapshot snapshot = LastProfileSnapshot.of(currentType, currentProfile);
        if (snapshot == null) {
            LAST_PROFILE.remove(player.getUuid());
        } else {
            LAST_PROFILE.put(player.getUuid(), snapshot);
        }
        player.calculateDimensions();
    }

    public static void clearPlayer(java.util.UUID uuid) {
        LAST_PROFILE.remove(uuid);
    }

    private record LastProfileSnapshot(
            net.minecraft.entity.EntityType<?> type,
            float width,
            float height,
            float eyeHeight
    ) {
        private static LastProfileSnapshot of(
                net.minecraft.entity.EntityType<?> type,
                PossessionProfile profile
        ) {
            if (type == null || profile == null) {
                return null;
            }
            return new LastProfileSnapshot(type, profile.getWidth(), profile.getHeight(), profile.getEyeHeight());
        }

        private static boolean matches(
                LastProfileSnapshot snapshot,
                net.minecraft.entity.EntityType<?> type,
                PossessionProfile profile
        ) {
            if (snapshot == null) {
                return type == null && profile == null;
            }
            if (type == null || profile == null) {
                return false;
            }
            return snapshot.type == type
                    && Float.compare(snapshot.width, profile.getWidth()) == 0
                    && Float.compare(snapshot.height, profile.getHeight()) == 0
                    && Float.compare(snapshot.eyeHeight, profile.getEyeHeight()) == 0;
        }
    }
}





