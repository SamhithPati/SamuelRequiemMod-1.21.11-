package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.AnimationState;
import net.minecraft.entity.mob.BreezeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BreezeEntity.class)
public interface BreezeEntityAnimationAccessor {
    @Accessor("slidingAnimationState")
    AnimationState samrequiemmod$getSlidingAnimationState();

    @Accessor("shootingAnimationState")
    AnimationState samrequiemmod$getShootingAnimationState();

    @Accessor("inhalingAnimationState")
    AnimationState samrequiemmod$getInhalingAnimationState();
}
