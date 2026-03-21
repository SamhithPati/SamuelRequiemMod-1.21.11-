package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;

public final class PossessionDimensionHelper {

    private PossessionDimensionHelper() {
    }

    // Tracks the last-known baby state per player so we detect changes
    private static final java.util.Map<java.util.UUID, Boolean> LAST_BABY_STATE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static void refreshDimensionsIfNeeded(ServerPlayerEntity player) {
        EntityType<?> current = PossessionManager.getPossessedType(player);
        EntityType<?> last    = PossessionManager.getLastDimensionType(player);

        boolean currentBaby = BabyZombieState.isServerBaby(player) || BabyHuskState.isServerBaby(player)
                || BabyDrownedState.isServerBaby(player);
        Boolean lastBaby    = LAST_BABY_STATE.get(player.getUuid());

        // Recalculate if the entity type changed OR if the baby flag changed
        boolean changed = current != last
                || lastBaby == null
                || lastBaby != currentBaby;

        if (!changed) return;

        PossessionManager.setLastDimensionType(player, current);
        LAST_BABY_STATE.put(player.getUuid(), currentBaby);
        player.calculateDimensions();
    }

    public static void clearPlayer(java.util.UUID uuid) {
        LAST_BABY_STATE.remove(uuid);
    }
}