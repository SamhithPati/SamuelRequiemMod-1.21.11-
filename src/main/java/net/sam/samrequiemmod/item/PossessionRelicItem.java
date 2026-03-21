package net.sam.samrequiemmod.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sam.samrequiemmod.possession.PossessionManager;

public class PossessionRelicItem extends Item {

    public PossessionRelicItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(stack);
        }

        if (hand != Hand.MAIN_HAND) {
            return TypedActionResult.pass(stack);
        }

        if (!user.isSneaking()) {
            return TypedActionResult.pass(stack);
        }

        if (!PossessionManager.isPossessing(serverPlayer)) {
            serverPlayer.sendMessage(Text.literal("§eYou are not currently possessing anything."), true);
            return TypedActionResult.fail(stack);
        }

        PossessionManager.clearPossession(serverPlayer);
        serverPlayer.sendMessage(Text.literal("§aYou returned to your normal body."), true);
        return TypedActionResult.success(stack);
    }
}