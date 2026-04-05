package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.NoSwimPossessionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class NoSwimWaterJumpMixin {

    @Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$replaceNoSwimWaterJump(TagKey<Fluid> fluid, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!NoSwimPossessionHelper.shouldForceSink(player)) return;

        if (player instanceof ServerPlayerEntity serverPlayer
                && NoSwimPossessionHelper.hasWaterJumpSupport(player)) {
            NoSwimPossessionHelper.applyWaterJump(serverPlayer);
        }

        ci.cancel();
    }
}
