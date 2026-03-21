package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.ItemStack;
import net.sam.samrequiemmod.client.CrossbowAnimationOverride;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PillagerEntityMixin implements CrossbowAnimationOverride {

    @Unique private int samrequiemmod$overrideUseTimeLeft = -1;
    @Unique private int samrequiemmod$overrideUseTimeElapsed = -1;
    @Unique private ItemStack samrequiemmod$overrideActiveItem = null;

    @Override
    public void samrequiemmod$setUseTimeOverride(int timeLeft, int timeElapsed, ItemStack activeItem) {
        this.samrequiemmod$overrideUseTimeLeft    = timeLeft;
        this.samrequiemmod$overrideUseTimeElapsed = timeElapsed;
        this.samrequiemmod$overrideActiveItem     = activeItem;
    }

    @Override
    public void samrequiemmod$clearUseTimeOverride() {
        this.samrequiemmod$overrideUseTimeLeft    = -1;
        this.samrequiemmod$overrideUseTimeElapsed = -1;
        this.samrequiemmod$overrideActiveItem     = null;
    }

    @Inject(method = "getItemUseTimeLeft", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overrideGetItemUseTimeLeft(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof PillagerEntity)) return;
        if (samrequiemmod$overrideUseTimeLeft >= 0) {
            cir.setReturnValue(samrequiemmod$overrideUseTimeLeft);
        }
    }

    @Inject(method = "getItemUseTime", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overrideGetItemUseTime(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof PillagerEntity)) return;
        if (samrequiemmod$overrideUseTimeElapsed >= 0) {
            cir.setReturnValue(samrequiemmod$overrideUseTimeElapsed);
        }
    }

    @Inject(method = "getActiveItem", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overrideGetActiveItem(CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof PillagerEntity)) return;
        if (samrequiemmod$overrideActiveItem != null) {
            cir.setReturnValue(samrequiemmod$overrideActiveItem);
        }
    }
}