package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.beast.BeastPossessionController;
import net.sam.samrequiemmod.possession.beast.BeastState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AxolotlPlayDeadMovementMixin {

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$freezeAxolotlPlayDead(Vec3d input, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!BeastPossessionController.isAxolotlPossessing(player)) return;
        if (!BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age)) return;

        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        ci.cancel();
    }
}






