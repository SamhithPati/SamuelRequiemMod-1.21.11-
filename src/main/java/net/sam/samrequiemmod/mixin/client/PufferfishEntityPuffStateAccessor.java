package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.PufferfishEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for PufferfishEntity's PUFF_STATE tracked data key.
 * Used by the render helper to set the puff visual state
 * on the shell entity when rendering a pufferfish possession.
 */
@Mixin(PufferfishEntity.class)
public interface PufferfishEntityPuffStateAccessor {

    @Accessor("PUFF_STATE")
    static TrackedData<Integer> getPuffStateKey() {
        throw new AssertionError();
    }
}






