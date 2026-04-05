package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.WolfVariant;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WolfEntity.class)
public interface WolfEntityVariantInvoker {

    @Invoker("setVariant")
    void samrequiemmod$setVariant(RegistryEntry<WolfVariant> variant);
}
