package net.sam.samrequiemmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;

public class ModItems {

public static final Item POSSESSION_RELIC = registerItem("possession_relic", settings -> new PossessionRelicItem(settings));
public static final Item POSSESSION_RELIC_SHARD = registerItem("possession_relic_shard", Item::new);

    private static Item registerItem(String name, java.util.function.Function<Item.Settings, Item> factory) {
        Identifier id = Identifier.of(SamuelRequiemMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings().registryKey(key);
        Item item = factory.apply(settings);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void registerModItems(){
        SamuelRequiemMod.LOGGER.info("Registering mod items for " + SamuelRequiemMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(POSSESSION_RELIC);
            entries.add(POSSESSION_RELIC_SHARD);
        });
    }
}






