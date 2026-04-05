package net.sam.samrequiemmod.possession;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class NoSwimPossessionHelper {

    private static final double DEFAULT_SINK_SPEED = -0.02;
    private static final double FAST_SINK_SPEED = -0.04;
    private static final double UNDERWATER_STEP_VERTICAL_BOOST = 0.18;

    private NoSwimPossessionHelper() {
    }

    public static boolean shouldForceSink(PlayerEntity player) {
        return net.sam.samrequiemmod.possession.zombie.ZombiePossessionController.isZombiePossessing(player)
                || net.sam.samrequiemmod.possession.zombie.BabyZombiePossessionController.isBabyZombiePossessing(player)
                || net.sam.samrequiemmod.possession.husk.HuskPossessionController.isHuskPossessing(player)
                || net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(player)
                || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(player)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(player)
                || net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(player)
                || net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(player)
                || net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player)
                || net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isAnyHoglinTypePossessing(player)
                || net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(player);
    }

    public static void disableSwimmingPose(ServerPlayerEntity player) {
        if (!player.isTouchingWater()) return;
        if (player.isSwimming()) {
            player.setSwimming(false);
        }
    }

    public static void applyUnderwaterMovement(ServerPlayerEntity player) {
        if (!shouldForceSink(player)) return;

        disableSwimmingPose(player);
        if (!player.isTouchingWater()) return;

        Vec3d velocity = player.getVelocity();
        double adjustedY = velocity.y;
        double maxSinkSpeed = player.isSneaking() ? FAST_SINK_SPEED : DEFAULT_SINK_SPEED;

        if (adjustedY < maxSinkSpeed) {
            adjustedY = maxSinkSpeed;
        }

        if (adjustedY != velocity.y) {
            player.setVelocity(velocity.x, adjustedY, velocity.z);
            player.velocityDirty = true;
        }
    }

    public static void applyWaterJump(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x, Math.max(velocity.y, UNDERWATER_STEP_VERTICAL_BOOST), velocity.z);
        player.velocityDirty = true;
        player.fallDistance = 0.0f;
    }

    public static boolean hasWaterJumpSupport(PlayerEntity player) {
        return player.isOnGround() || player.horizontalCollision || player.verticalCollision;
    }
}
