package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.silverfish.SilverfishHideNetworking;

public final class SilverfishHideClientHandler {

    private static BlockPos activeBlockPos;

    private SilverfishHideClientHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                stopIfNeeded();
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.SILVERFISH) {
                stopIfNeeded();
                return;
            }

            if (!client.options.useKey.isPressed()) {
                stopIfNeeded();
                return;
            }

            if (!(client.crosshairTarget instanceof BlockHitResult blockHit) || client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
                stopIfNeeded();
                return;
            }

            BlockPos blockPos = blockHit.getBlockPos();
            BlockState state = client.world.getBlockState(blockPos);
            if (!isHideableBlock(state)) {
                stopIfNeeded();
                return;
            }

            if (!blockPos.equals(activeBlockPos)) {
                activeBlockPos = blockPos.toImmutable();
                ClientPlayNetworking.send(new SilverfishHideNetworking.StartHidePayload(activeBlockPos));
            }
        });
    }

    private static void stopIfNeeded() {
        if (activeBlockPos == null) return;
        activeBlockPos = null;
        ClientPlayNetworking.send(new SilverfishHideNetworking.StopHidePayload());
    }

    private static boolean isHideableBlock(BlockState state) {
        return state.isOf(Blocks.STONE)
                || state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.STONE_BRICKS)
                || state.isOf(Blocks.MOSSY_STONE_BRICKS)
                || state.isOf(Blocks.CRACKED_STONE_BRICKS)
                || state.isOf(Blocks.CHISELED_STONE_BRICKS)
                || state.isOf(Blocks.DEEPSLATE);
    }
}
