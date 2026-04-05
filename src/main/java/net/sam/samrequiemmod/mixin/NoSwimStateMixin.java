package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.NoSwimPossessionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class NoSwimStateMixin {

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$blockSwimmingState(boolean swimming, CallbackInfo ci) {
        if (!swimming) return;
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!NoSwimPossessionHelper.shouldForceSink(player)) return;
        ci.cancel();
    }
}
