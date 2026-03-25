package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.passive.WolfEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WolfEntity.class)
public interface WolfEntityShakeAccessor {

    @Accessor("furWet")
    void setFurWet(boolean furWet);

    @Accessor("canShakeWaterOff")
    void setCanShakeWaterOff(boolean canShakeWaterOff);

    @Accessor("shakeProgress")
    void setShakeProgress(float shakeProgress);

    @Accessor("shakeProgress")
    float getShakeProgress();

    @Accessor("lastShakeProgress")
    void setLastShakeProgress(float lastShakeProgress);
}
