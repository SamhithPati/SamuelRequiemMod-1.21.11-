package net.sam.samrequiemmod.mixin;

import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.beast.BeastPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StriderPossessionFluidMixin {

    @Inject(method = "canWalkOnFluid", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$striderWalkOnLava(FluidState state, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (!BeastPossessionController.isStriderPossessing(player)) return;
        if (state.isOf(Fluids.LAVA) || state.isOf(Fluids.FLOWING_LAVA)) {
            cir.setReturnValue(true);
        }
    }
}






