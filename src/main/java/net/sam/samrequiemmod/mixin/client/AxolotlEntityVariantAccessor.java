package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.AxolotlEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AxolotlEntity.class)
public interface AxolotlEntityVariantAccessor {
    @Accessor("VARIANT")
    static TrackedData<Integer> getVariantKey() {
        throw new AssertionError();
    }

    @Accessor("PLAYING_DEAD")
    static TrackedData<Boolean> getPlayingDeadKey() {
        throw new AssertionError();
    }
}






