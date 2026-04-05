package net.sam.samrequiemmod.possession.skeleton;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class SkeletonHorseRidingHandler {

    private SkeletonHorseRidingHandler() {}

    public static boolean isSkeletonType(PlayerEntity player) {
        return SkeletonPossessionController.isAnySkeletonPossessing(player)
                || WitherSkeletonPossessionController.isWitherSkeletonPossessing(player);
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(entity instanceof SkeletonHorseEntity horse)) return ActionResult.PASS;
            if (!isSkeletonType(player)) return ActionResult.PASS;
            if (player.getVehicle() == horse) return ActionResult.SUCCESS;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(horse, true, false);
            return ActionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!isSkeletonType(player)) return ActionResult.PASS;
            if (!(entity instanceof SkeletonHorseEntity)) return ActionResult.PASS;
            if (player.getVehicle() == entity) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }

    public static void tick(ServerPlayerEntity player) {
    }
}
