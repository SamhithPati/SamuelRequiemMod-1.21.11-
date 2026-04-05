package net.sam.samrequiemmod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

public class ModEntities {

    private static final Identifier CORRUPTED_MERCHANT_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "corrupted_merchant");
    private static final RegistryKey<EntityType<?>> CORRUPTED_MERCHANT_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, CORRUPTED_MERCHANT_ID);
    private static final Identifier SOUL_BOSS_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "soul_boss");
    private static final RegistryKey<EntityType<?>> SOUL_BOSS_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, SOUL_BOSS_ID);
    public static final EntityType<CorruptedMerchantEntity> CORRUPTED_MERCHANT =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    CORRUPTED_MERCHANT_ID,
                    FabricEntityTypeBuilder.<CorruptedMerchantEntity>create(
                                    SpawnGroup.CREATURE, CorruptedMerchantEntity::new)
                            .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
                            .build(CORRUPTED_MERCHANT_KEY)
            );
    public static final EntityType<SoulBossEntity> SOUL_BOSS =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    SOUL_BOSS_ID,
                    FabricEntityTypeBuilder.<SoulBossEntity>create(
                                    SpawnGroup.MONSTER, SoulBossEntity::new)
                            .dimensions(EntityDimensions.fixed(2.8F, 3.4F))
                            .fireImmune()
                            .trackRangeBlocks(10)
                            .trackedUpdateRate(1)
                            .build(SOUL_BOSS_KEY)
            );

    public static void register() {
        SamuelRequiemMod.LOGGER.info("Registering mod entities for " + SamuelRequiemMod.MOD_ID);
    }
}





