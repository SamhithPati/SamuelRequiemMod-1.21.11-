package net.sam.samrequiemmod.world;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import net.sam.samrequiemmod.SamuelRequiemMod;

public final class SoulShrineMapTrades {

    public static final TagKey<net.minecraft.world.gen.structure.Structure> SOUL_SHRINE_MAP_TARGETS =
            TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(SamuelRequiemMod.MOD_ID, "soul_shrine_maps"));

    private SoulShrineMapTrades() {
    }

    public static void register() {
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CARTOGRAPHER, 3, factories -> {
            factories.add(new TradeOffers.SellMapFactory(
                    18,
                    SOUL_SHRINE_MAP_TARGETS,
                    "filled_map.samrequiemmod.soul_shrine",
                    getMapDecorationType(),
                    12,
                    5
            ));
        });
    }

    @SuppressWarnings("unchecked")
    private static RegistryEntry<net.minecraft.item.map.MapDecorationType> getMapDecorationType() {
        return (RegistryEntry<net.minecraft.item.map.MapDecorationType>) (RegistryEntry<?>) MapDecorationTypes.RED_X;
    }
}
