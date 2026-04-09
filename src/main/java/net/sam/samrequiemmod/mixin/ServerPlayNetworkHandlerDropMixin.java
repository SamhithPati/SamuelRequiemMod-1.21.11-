package net.sam.samrequiemmod.mixin;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.PossessionLoadoutProtection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerDropMixin {

    @Shadow @Final public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$blockProtectedSelectedDrop(PlayerActionC2SPacket packet, CallbackInfo ci) {
        PlayerActionC2SPacket.Action action = packet.getAction();

        if (action != PlayerActionC2SPacket.Action.DROP_ITEM
                && action != PlayerActionC2SPacket.Action.DROP_ALL_ITEMS) {
            return;
        }

        if (PossessionLoadoutProtection.isProtectedLoadoutItem(this.player, this.player.getMainHandStack())) {
            this.player.getInventory().markDirty();
            this.player.currentScreenHandler.sendContentUpdates();
            this.player.playerScreenHandler.sendContentUpdates();
            this.player.currentScreenHandler.syncState();
            this.player.playerScreenHandler.syncState();
            ci.cancel();
        }
    }
}
