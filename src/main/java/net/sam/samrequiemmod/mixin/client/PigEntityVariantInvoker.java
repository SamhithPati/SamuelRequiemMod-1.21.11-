package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PigVariant;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PigEntity.class)
public interface PigEntityVariantInvoker {
    @Invoker("setVariant")
    void samrequiemmod$setVariant(RegistryEntry<PigVariant> variant);
}
