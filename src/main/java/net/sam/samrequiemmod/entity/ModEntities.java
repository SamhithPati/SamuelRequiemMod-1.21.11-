package net.sam.samrequiemmod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

public class ModEntities {

    public static final EntityType<CorruptedMerchantEntity> CORRUPTED_MERCHANT =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(SamuelRequiemMod.MOD_ID, "corrupted_merchant"),
                    FabricEntityTypeBuilder.<CorruptedMerchantEntity>create(
                                    SpawnGroup.CREATURE, CorruptedMerchantEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                            .build()
            );

    public static void register() {
        SamuelRequiemMod.LOGGER.info("Registering mod entities for " + SamuelRequiemMod.MOD_ID);
    }
}