package net.sam.samrequiemmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.sam.samrequiemmod.item.PossessionRelicItem;

public class ModItems {

public static final Item POSSESSION_RELIC = registerItem("possession_relic", new PossessionRelicItem(new Item.Settings()));
public static final Item POSSESSION_RELIC_SHARD = registerItem("possession_relic_shard", new Item(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(SamuelRequiemMod.MOD_ID, name), item);
    }

    public static void registerModItems(){
        SamuelRequiemMod.LOGGER.info("Registering mod items for " + SamuelRequiemMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(POSSESSION_RELIC);
            entries.add(POSSESSION_RELIC_SHARD);
        });
    }
}
