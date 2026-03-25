package net.sam.samrequiemmod.client;

import net.minecraft.entity.mob.IllagerEntity;

/** Implemented by IllagerEntity via mixin — allows setting illager state for animation. */
public interface IllagerStateSetter {
    void samrequiemmod$setState(IllagerEntity.State state);
}
