package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ZombieEntity.class)
public interface ZombieEntityInWaterAccessor {
    /** Exposes the static CONVERTING_IN_WATER TrackedData key. */
    @Accessor("CONVERTING_IN_WATER")
    static TrackedData<Boolean> getConvertingInWaterKey() {
        throw new AssertionError();
    }
}