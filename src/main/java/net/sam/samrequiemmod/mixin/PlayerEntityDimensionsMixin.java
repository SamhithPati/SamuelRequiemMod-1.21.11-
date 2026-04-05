package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.sam.samrequiemmod.possession.PossessionProfile;
import net.sam.samrequiemmod.possession.PossessionProfileResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.PlayerLikeEntity.class)
public abstract class PlayerEntityDimensionsMixin {

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overridePossessionDimensions(
            EntityPose pose,
            CallbackInfoReturnable<EntityDimensions> cir
    ) {
        if (!((Object) this instanceof net.minecraft.entity.player.PlayerEntity player)) {
            return;
        }

        PossessionProfile profile = PossessionProfileResolver.get(player);
        if (profile == null) {
            return;
        }

        cir.setReturnValue(EntityDimensions
                .changing(profile.getWidth(), profile.getHeight())
                .withEyeHeight(profile.getEyeHeight()));
    }
}
