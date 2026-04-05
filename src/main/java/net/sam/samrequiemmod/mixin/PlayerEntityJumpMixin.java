package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.breeze.BreezePossessionController;
import net.sam.samrequiemmod.possession.slime.SlimePossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerEntityJumpMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$boostSlimeJump(CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) return;
        if (BreezePossessionController.isBreezePossessing(serverPlayer)) {
            BreezePossessionController.handleJumpRequest(serverPlayer);
            ci.cancel();
            return;
        }
        if (SlimePossessionController.isAnySlimePossessing(serverPlayer)) {
            if (!player.isSprinting() && player.forwardSpeed <= 0.0f && Math.abs(player.sidewaysSpeed) < 0.01f) return;

            double baseJump = SlimePossessionController.getJumpVelocity(serverPlayer);
            double forwardBoost = SlimePossessionController.getJumpHorizontalBoost(serverPlayer);
            player.setVelocity(player.getVelocity().x * forwardBoost, baseJump, player.getVelocity().z * forwardBoost);
            player.velocityDirty = true;
            player.fallDistance = 0.0f;
            SlimePossessionController.playJumpSound(serverPlayer);
            return;
        }

        double jumpBoost = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getJumpBoost(serverPlayer);
        if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.tryCamelDash(serverPlayer)) {
            return;
        }
        if (jumpBoost > 0.0) {
            player.setVelocity(player.getVelocity().x, jumpBoost, player.getVelocity().z);
            player.velocityDirty = true;
            player.fallDistance = 0.0f;
        }
    }
}






