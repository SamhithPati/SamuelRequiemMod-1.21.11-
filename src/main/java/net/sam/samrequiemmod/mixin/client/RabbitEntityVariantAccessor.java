package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.RabbitEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RabbitEntity.class)
public interface RabbitEntityVariantAccessor {
    @Accessor("RABBIT_TYPE")
    static TrackedData<Integer> getVariantKey() {
        throw new AssertionError();
    }
}
