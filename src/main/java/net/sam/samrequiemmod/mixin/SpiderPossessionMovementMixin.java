package net.sam.samrequiemmod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.spider.SpiderPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SpiderPossessionMovementMixin {

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$spiderClimbingOverride(CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!SpiderPossessionController.isAnySpiderPossessing(player)) return;
        if (player.horizontalCollision) {
            cir.setReturnValue(true);
        }
    }

    @Mixin(Entity.class)
    public abstract static class CobwebSlowdownMixin {
        @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$ignoreCobwebSlow(BlockState state, net.minecraft.util.math.Vec3d multiplier, CallbackInfo ci) {
            if (!((Object) this instanceof PlayerEntity player)) return;
            if (!SpiderPossessionController.isAnySpiderPossessing(player)) return;
            if (state.isOf(Blocks.COBWEB)) {
                ci.cancel();
            }
        }
    }
}
