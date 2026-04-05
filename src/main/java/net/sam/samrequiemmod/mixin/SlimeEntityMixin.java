package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.PossessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents slimes (and magma cubes) from attacking possessed players.
 * getTarget() is on MobEntity — cast through (MobEntity)(Object)this to reach it.
 */
@Mixin(SlimeEntity.class)
public abstract class SlimeEntityMixin {

    @Inject(method = "canAttack()Z", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$ignoresPossessedPlayer(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = ((MobEntity)(Object)this).getTarget();
        if (target instanceof ServerPlayerEntity player
                && PossessionManager.isPossessing(player)
                && !net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player)) {
            cir.setReturnValue(false);
        }
    }
}





