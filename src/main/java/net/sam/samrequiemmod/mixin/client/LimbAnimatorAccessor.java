package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {

    @Accessor("lastSpeed")
    float samrequiemmod$getLastSpeed();

    @Accessor("speed")
    float samrequiemmod$getSpeedField();

    @Accessor("animationProgress")
    float samrequiemmod$getAnimationProgress();

    @Accessor("timeScale")
    float samrequiemmod$getTimeScale();

    @Accessor("lastSpeed")
    void samrequiemmod$setLastSpeed(float value);

    @Accessor("speed")
    void samrequiemmod$setSpeedField(float value);

    @Accessor("animationProgress")
    void samrequiemmod$setAnimationProgress(float value);

    @Accessor("timeScale")
    void samrequiemmod$setTimeScale(float value);
}
