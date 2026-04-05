package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.PossessionProfile;
import net.sam.samrequiemmod.possession.PossessionProfileResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerEntityEyeHeightMixin {

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overrideStandingEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return;
        }

        PossessionProfile profile = PossessionProfileResolver.get(player);
        if (profile != null) {
            cir.setReturnValue(profile.getEyeHeight());
        }
    }

    @Inject(method = "getEyeY", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overrideEyeY(CallbackInfoReturnable<Double> cir) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return;
        }

        PossessionProfile profile = PossessionProfileResolver.get(player);
        if (profile != null) {
            cir.setReturnValue(player.getY() + profile.getEyeHeight());
        }
    }
}
