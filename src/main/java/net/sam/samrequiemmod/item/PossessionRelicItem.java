package net.sam.samrequiemmod.item;

import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sam.samrequiemmod.possession.PossessionManager;

public class PossessionRelicItem extends Item {

    public PossessionRelicItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (hand != Hand.MAIN_HAND || !user.isSneaking()) {
            return ActionResult.PASS;
        }

        if (!PossessionManager.isPossessing(serverPlayer)) {
            serverPlayer.sendMessage(Text.literal("You are not currently possessing anything."), true);
            return ActionResult.FAIL;
        }

        PossessionManager.clearPossession(serverPlayer);
        serverPlayer.sendMessage(Text.literal("You returned to your normal body."), true);
        return ActionResult.SUCCESS;
    }
}







