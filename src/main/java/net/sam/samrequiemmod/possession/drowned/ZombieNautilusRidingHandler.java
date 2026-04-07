package net.sam.samrequiemmod.possession.drowned;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class ZombieNautilusRidingHandler {

    private ZombieNautilusRidingHandler() {}

    public static boolean isDrownedType(PlayerEntity player) {
        return DrownedPossessionController.isDrownedPossessing(player)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(player);
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(entity instanceof ZombieNautilusEntity zombieNautilus)) return ActionResult.PASS;
            if (!isDrownedType(player)) return ActionResult.PASS;
            if (player.getVehicle() == zombieNautilus) return ActionResult.SUCCESS;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(zombieNautilus, true, false);
            return ActionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!isDrownedType(player)) return ActionResult.PASS;
            if (!(entity instanceof ZombieNautilusEntity)) return ActionResult.PASS;
            if (player.getVehicle() == entity) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }
}
