package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.NoSwimPossessionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NoSwimStepHeightMixin {

    @Inject(method = "getStepHeight", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$raiseNoSwimUnderwaterStepHeight(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!NoSwimPossessionHelper.shouldForceSink(player)) return;
        if (!player.isTouchingWater()) return;

        cir.setReturnValue(1.1f);
    }
}
