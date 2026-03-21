package net.sam.samrequiemmod.possession.drowned;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DrownedTridentManager {

    private static final String TAG = "samrequiemmod_drowned_trident";
    private static final Set<UUID> GIVEN = ConcurrentHashMap.newKeySet();

    private DrownedTridentManager() {}

    public static void ensureTrident(ServerPlayerEntity player) {
        if (GIVEN.contains(player.getUuid())) return;
        if (hasDrownedTrident(player)) { GIVEN.add(player.getUuid()); return; }

        ItemStack trident = new ItemStack(Items.TRIDENT);

        // Unbreakable — show tooltip
        trident.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));

        // Loyalty III — returns after throwing
        var enchReg = player.getServerWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var loyalty = enchReg.getEntry(Enchantments.LOYALTY);
        loyalty.ifPresent(e -> trident.addEnchantment(e, 3));

        // Mark as our drowned trident
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean(TAG, true);
        trident.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Custom name
        trident.set(DataComponentTypes.CUSTOM_NAME,
                net.minecraft.text.Text.literal("Drowned Trident")
                        .styled(s -> s.withItalic(false)));

        player.getInventory().insertStack(trident);
        GIVEN.add(player.getUuid());
    }

    public static void removeTrident(ServerPlayerEntity player) {
        GIVEN.remove(player.getUuid());
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (isDrownedTrident(inv.getStack(i))) {
                inv.removeStack(i);
                return;
            }
        }
    }

    public static boolean isDrownedTrident(ItemStack stack) {
        if (!stack.isOf(Items.TRIDENT)) return false;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.contains(TAG);
    }

    public static boolean hasDrownedTrident(PlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++)
            if (isDrownedTrident(inv.getStack(i))) return true;
        return false;
    }

    public static void clearPlayer(UUID uuid) {
        GIVEN.remove(uuid);
    }

    /**
     * Called every tick for possessed drowned players.
     * Drops any non-drowned tridents the player may have picked up (anti-dupe).
     */
    public static void preventTridentDupe(net.minecraft.server.network.ServerPlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(Items.TRIDENT) && !isDrownedTrident(stack)) {
                inv.removeStack(i);
                // Drop the item in the world so it isn't just deleted
                player.dropItem(stack, false);
                return;
            }
        }
    }
}