package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EndermanEntity.class)
public interface EndermanEntityAngryAccessor {
    /** Exposes the static ANGRY TrackedData key for rendering the open-mouth texture. */
    @Accessor("ANGRY")
    static TrackedData<Boolean> getAngryKey() {
        throw new AssertionError();
    }
}
