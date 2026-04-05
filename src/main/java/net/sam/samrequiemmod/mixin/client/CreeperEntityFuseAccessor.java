package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreeperEntity.class)
public interface CreeperEntityFuseAccessor {
    /** Exposes the static CHARGED TrackedData key for the charged glow layer. */
    @Accessor("CHARGED")
    static TrackedData<Boolean> getChargedKey() {
        throw new AssertionError();
    }

    /** Exposes the static FUSE_SPEED TrackedData key for the swelling animation direction. */
    @Accessor("FUSE_SPEED")
    static TrackedData<Integer> getFuseSpeedKey() {
        throw new AssertionError();
    }

    /** Gets the current fuse time (0 to fuseTime). */
    @Accessor("currentFuseTime")
    int getCurrentFuseTime();

    /** Sets the current fuse time for driving the swelling animation. */
    @Accessor("currentFuseTime")
    void setCurrentFuseTime(int value);

    /** Gets the last fuse time (previous tick, for interpolation). */
    @Accessor("lastFuseTime")
    int getLastFuseTime();

    /** Sets the last fuse time for smooth interpolation. */
    @Accessor("lastFuseTime")
    void setLastFuseTime(int value);
}






