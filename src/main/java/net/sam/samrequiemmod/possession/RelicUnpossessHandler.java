package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.item.ModItems;

public final class RelicUnpossessHandler {

    private RelicUnpossessHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (world.isClient()) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (hand != Hand.MAIN_HAND || !stack.isOf(ModItems.POSSESSION_RELIC) || !player.isSneaking()) {
                return ActionResult.PASS;
            }

            if (!PossessionManager.isPossessing(serverPlayer)) {
                serverPlayer.sendMessage(Text.literal("You are not currently possessing anything."), true);
                return ActionResult.FAIL;
            }

            PossessionManager.clearPossession(serverPlayer);
            serverPlayer.sendMessage(Text.literal("You returned to your normal body."), true);
            return ActionResult.SUCCESS;
        });
    }
}







