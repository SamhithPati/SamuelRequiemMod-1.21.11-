package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.sam.samrequiemmod.item.ModItems;

public final class RelicUnpossessHandler {

    private RelicUnpossessHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (world.isClient) {
                return TypedActionResult.pass(stack);
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            if (hand != Hand.MAIN_HAND) {
                return TypedActionResult.pass(stack);
            }

            if (player.isSneaking()) {
                return TypedActionResult.pass(stack);
            }

            if (!stack.isOf(ModItems.POSSESSION_RELIC)) {
                return TypedActionResult.pass(stack);
            }

            if (!player.isSneaking()) {
                return TypedActionResult.pass(stack);
            }

            if (!PossessionManager.isPossessing(serverPlayer)) {
                serverPlayer.sendMessage(Text.literal("§eYou are not currently possessing anything."), true);
                return TypedActionResult.fail(stack);
            }

            PossessionManager.clearPossession(serverPlayer);

            serverPlayer.sendMessage(Text.literal("§aYou returned to your normal body."), true);
            return TypedActionResult.success(stack);
        });
    }
}