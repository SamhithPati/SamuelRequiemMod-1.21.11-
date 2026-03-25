package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TropicalFishEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for TropicalFishEntity's VARIANT tracked data key.
 * Used by the render helper to set the correct color/pattern
 * on the shell entity when rendering a tropical fish possession.
 */
@Mixin(TropicalFishEntity.class)
public interface TropicalFishEntityVariantAccessor {

    @Accessor("VARIANT")
    static TrackedData<Integer> getVariantKey() {
        throw new AssertionError();
    }
}
