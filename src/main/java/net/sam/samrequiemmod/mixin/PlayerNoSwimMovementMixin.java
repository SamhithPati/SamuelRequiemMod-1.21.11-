package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.NoSwimPossessionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerNoSwimMovementMixin {

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void samrequiemmod$applyNoSwimWaterPhysics(CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        if (!NoSwimPossessionHelper.shouldForceSink(player)) return;
        NoSwimPossessionHelper.applyUnderwaterMovement(player);
    }
}
