package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.WolfSoundVariant;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WolfEntity.class)
public interface WolfEntitySoundVariantAccessor {
    @Invoker("getSoundVariant")
    RegistryEntry<WolfSoundVariant> samrequiemmod$getSoundVariant();
}
