package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.PossessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PossessedPlayerArmorDurabilityMixin {

    @Inject(method = "damageArmor", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$skipArmorDurabilityLoss(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && PossessionManager.isPossessing(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "damageEquipment", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$skipEquipmentDurabilityLoss(
            DamageSource source,
            float amount,
            EquipmentSlot[] slots,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof ServerPlayerEntity player && PossessionManager.isPossessing(player)) {
            ci.cancel();
        }
    }
}
