package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public final class RavagerRidingHandler {

    private RavagerRidingHandler() {}

    public static boolean isIllagerPossessed(PlayerEntity player) {
        return PillagerPossessionController.isPillagerPossessing(player)
                || VindicatorPossessionController.isVindicatorPossessing(player)
                || EvokerPossessionController.isEvokerPossessing(player)
                || RavagerPossessionController.isRavagerPossessing(player);
    }

    public static void register() {
        // Block attacking the ridden ravager
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!isIllagerPossessed(player)) return ActionResult.PASS;
            if (!(entity instanceof RavagerEntity)) return ActionResult.PASS;
            if (player.getVehicle() == entity) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }
}






