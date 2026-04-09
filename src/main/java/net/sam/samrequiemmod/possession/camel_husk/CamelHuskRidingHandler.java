package net.sam.samrequiemmod.possession.camel_husk;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.MountedPlayerInputHelper;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;
import net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CamelHuskRidingHandler {

    private static final Map<UUID, Long> LAST_DASH = new ConcurrentHashMap<>();

    private CamelHuskRidingHandler() {
    }

    public static boolean isAllowedRider(PlayerEntity player) {
        return HuskPossessionController.isHuskPossessing(player)
                || BabyHuskPossessionController.isBabyHuskPossessing(player)
                || SkeletonPossessionController.isParchedPossessing(player);
    }

    public static boolean isCamelHusk(EntityType<?> type) {
        return type == EntityType.CAMEL_HUSK;
    }

    public static boolean tryCamelHuskDash(Entity mount, ServerPlayerEntity rider) {
        if (!isCamelHusk(mount.getType())) return false;
        if (!isAllowedRider(rider)) return false;
        if (!(mount instanceof CamelEntity camel)) return false;
        if (!mount.isOnGround()) return false;
        if (!MountedPlayerInputHelper.isJumping(rider)) return false;

        long lastDash = LAST_DASH.getOrDefault(mount.getUuid(), -100L);
        if (rider.age - lastDash < 40L) return false;
        if (camel.getJumpCooldown() > 0 || !camel.canJump()) return false;

        camel.setStanding();
        camel.startJumping(90);
        LAST_DASH.put(mount.getUuid(), (long) rider.age);
        return true;
    }
}
