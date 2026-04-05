package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.passive.ChickenEntity;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController;

public final class ChickenRidingHandler {

    private ChickenRidingHandler() {}

    public static boolean isBabyUndead(PlayerEntity player) {
        return BabyZombiePossessionController.isBabyZombiePossessing(player)
                || BabyHuskPossessionController.isBabyHuskPossessing(player)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(player)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(player);
    }

    public static void register() {
        // Block attacking the ridden chicken
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!isBabyUndead(player)) return ActionResult.PASS;
            if (!(entity instanceof ChickenEntity)) return ActionResult.PASS;
            if (player.getVehicle() == entity) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }

    // Tick — dismount handled in ChickenEntityMixin$RidingMixin
    public static void tick(ServerPlayerEntity player) {}
}





