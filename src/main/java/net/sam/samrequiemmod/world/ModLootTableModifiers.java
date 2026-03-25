package net.sam.samrequiemmod.world;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.item.ModItems;

public final class ModLootTableModifiers {

    private ModLootTableModifiers() {}

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!"minecraft".equals(key.getValue().getNamespace())) return;
            String path = key.getValue().getPath();
            if (!path.startsWith("chests/bastion_")) return;

            LootPool.Builder pool = LootPool.builder()
                    .rolls(net.minecraft.loot.provider.number.ConstantLootNumberProvider.create(1.0f))
                    .with(ItemEntry.builder(ModItems.POSSESSION_RELIC_SHARD)
                            .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 2.0f))))
                    .conditionally(RandomChanceLootCondition.builder(0.35f));

            tableBuilder.pool(pool);
        });
    }
}
