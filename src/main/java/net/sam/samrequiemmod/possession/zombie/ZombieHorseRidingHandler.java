package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController;
import net.sam.samrequiemmod.possession.drowned.DrownedPossessionController;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;

public final class ZombieHorseRidingHandler {

    private ZombieHorseRidingHandler() {}

    public static boolean isZombieType(PlayerEntity player) {
        return ZombiePossessionController.isZombiePossessing(player)
                || BabyZombiePossessionController.isBabyZombiePossessing(player)
                || HuskPossessionController.isHuskPossessing(player)
                || BabyHuskPossessionController.isBabyHuskPossessing(player)
                || DrownedPossessionController.isDrownedPossessing(player)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(player)
                || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(player)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(player);
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(entity instanceof ZombieHorseEntity horse)) return ActionResult.PASS;
            if (!isZombieType(player)) return ActionResult.PASS;
            if (player.getVehicle() == horse) return ActionResult.SUCCESS;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(horse, true, false);
            return ActionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!isZombieType(player)) return ActionResult.PASS;
            if (!(entity instanceof ZombieHorseEntity)) return ActionResult.PASS;
            if (player.getVehicle() == entity) return ActionResult.FAIL;
            return ActionResult.PASS;
        });
    }

    public static void tick(ServerPlayerEntity player) {
    }
}
