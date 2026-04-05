package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.beast.BeastPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ShulkerPossessionMovementMixin {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$freezeShulkerPossession(Vec3d input, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!BeastPossessionController.isShulkerPossessing(player)) return;

        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        ci.cancel();
    }
}






