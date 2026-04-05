package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.PossessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels all heal() calls for possessed players, preventing food regen
 * and any other passive healing. heal() is defined on LivingEntity, not
 * PlayerEntity, so the mixin targets LivingEntity.
 * Our food items use setHealth() directly so explicit food healing still works.
 */
@Mixin(LivingEntity.class)
public abstract class HungerManagerMixin {

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$cancelHealWhenPossessing(float amount, CallbackInfo ci) {
        // Only cancel for player entities that are possessing a mob
        if (!((Object) this instanceof PlayerEntity player)) return;
        if (PossessionManager.isPossessing(player)) {
            // Allow healing from Regeneration and Instant Health status effects
            if (player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.REGENERATION)) return;
            if (player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.INSTANT_HEALTH)) return;
            ci.cancel();
        }
    }
}





