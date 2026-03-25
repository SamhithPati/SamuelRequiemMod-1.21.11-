package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.HorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseEntity.class)
public interface HorseEntityVariantAccessor {
    @Accessor("VARIANT")
    static TrackedData<Integer> getVariantKey() {
        throw new AssertionError();
    }
}
