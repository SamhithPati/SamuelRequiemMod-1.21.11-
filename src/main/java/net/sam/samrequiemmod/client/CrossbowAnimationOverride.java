package net.sam.samrequiemmod.client;

import net.minecraft.item.ItemStack;

/**
 * Implemented by PillagerEntityMixin (via @Implements).
 * Allows PossessedPlayerRenderHelper to set crossbow use-time overrides
 * on the pillager shell without directly referencing the mixin class.
 */
public interface CrossbowAnimationOverride {
    void samrequiemmod$setUseTimeOverride(int timeLeft, int timeElapsed, ItemStack activeItem);
    void samrequiemmod$clearUseTimeOverride();
}