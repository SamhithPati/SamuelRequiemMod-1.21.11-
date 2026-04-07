package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.sam.samrequiemmod.possession.aquatic.NautilusPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwimUpwardMixin {

    @Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$blockNautilusSwimUpward(TagKey<Fluid> fluidTag, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!NautilusPossessionController.isAnyNautilusPossessing(serverPlayer)) return;
        ci.cancel();
    }
}
