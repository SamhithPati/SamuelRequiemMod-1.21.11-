package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class PossessionLoadoutProtection {

    private PossessionLoadoutProtection() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!isProtectedLoadoutItem(serverPlayer, stack)) return ActionResult.PASS;

            if (stack.isOf(Items.ARROW) || stack.isOf(Items.TIPPED_ARROW)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    public static boolean isProtectedLoadoutItem(ServerPlayerEntity player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        EntityType<?> type = PossessionManager.getPossessedType(player);

        if (type == EntityType.SKELETON || type == EntityType.BOGGED || type == EntityType.STRAY || type == EntityType.PARCHED) {
            return stack.isOf(Items.BOW) || stack.isOf(Items.ARROW) || stack.isOf(Items.TIPPED_ARROW);
        }

        if (type == EntityType.WITHER_SKELETON) {
            return stack.isOf(Items.STONE_SWORD);
        }

        if (type == EntityType.PILLAGER) {
            return stack.isOf(Items.CROSSBOW)
                    || stack.isOf(Items.ARROW)
                    || stack.isOf(Items.GOAT_HORN)
                    || isAnyBanner(stack)
                    || isRavagerCall(stack);
        }

        if (type == EntityType.VINDICATOR) {
            return stack.isOf(Items.IRON_AXE)
                    || stack.isOf(Items.GOAT_HORN)
                    || isAnyBanner(stack)
                    || isRavagerCall(stack);
        }

        if (type == EntityType.EVOKER) {
            return stack.isOf(Items.TOTEM_OF_UNDYING)
                    || stack.isOf(Items.GOAT_HORN)
                    || isAnyBanner(stack)
                    || isRavagerCall(stack);
        }

        if (type == EntityType.PIGLIN) {
            return stack.isOf(Items.CROSSBOW) || stack.isOf(Items.ARROW);
        }

        if (type == EntityType.ZOMBIFIED_PIGLIN) {
            return stack.isOf(Items.GOLDEN_SPEAR);
        }

        if (type == EntityType.DROWNED) {
            return stack.isOf(Items.TRIDENT);
        }

        return false;
    }

    private static boolean isAnyBanner(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.item.BannerItem;
    }

    private static boolean isRavagerCall(ItemStack stack) {
        return stack.isOf(Items.BELL)
                && stack.contains(DataComponentTypes.CUSTOM_NAME)
                && "Ravager Call".equals(stack.get(DataComponentTypes.CUSTOM_NAME).getString());
    }
}
