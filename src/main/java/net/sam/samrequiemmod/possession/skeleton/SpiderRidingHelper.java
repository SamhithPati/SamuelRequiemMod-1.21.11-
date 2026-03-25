package net.sam.samrequiemmod.possession.skeleton;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Helper methods for spider riding checks, separated from the mixin class
 * to avoid "cannot be referenced directly" errors.
 */
public final class SpiderRidingHelper {

    private SpiderRidingHelper() {}

    public static boolean isSkeletonType(PlayerEntity player) {
        return SkeletonPossessionController.isAnySkeletonPossessing(player)
                || WitherSkeletonPossessionController.isWitherSkeletonPossessing(player);
    }

    public static boolean isSpiderType(Entity entity) {
        return entity instanceof SpiderEntity;
    }
}
