package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZombieFoodUseHandler {

    private ZombieFoodUseHandler() {}

    // Tracks which item the player started eating (and from which possession type)
    // so we can heal them when the eating animation finishes.
    private static final Map<UUID, ItemStack> EATING_ITEM = new ConcurrentHashMap<>();

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return TypedActionResult.pass(stack);
            if (hand != Hand.MAIN_HAND) return TypedActionResult.pass(stack);

            boolean isZombie     = ZombiePossessionController.isZombiePossessing(serverPlayer);
            boolean isBabyZombie = BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer);
            boolean isHusk       = HuskPossessionController.isHuskPossessing(serverPlayer);
            boolean isBabyHusk   = net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer);
            boolean isDrowned    = net.sam.samrequiemmod.possession.drowned.DrownedPossessionController.isDrownedPossessing(serverPlayer);
            boolean isBabyDrowned = net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer);
            boolean isPillager   = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(serverPlayer);
            boolean isVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(serverPlayer);
            boolean isEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(serverPlayer);

            // Pillager food — only intercept if the item actually has food component
            if (isPillager) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return TypedActionResult.pass(stack); // not a food item, let vanilla handle
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a pillager, you can only heal from cooked meat and golden apples."), true);
                    return TypedActionResult.fail(stack);
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }

            // Vindicator food — same food list as pillager
            if (isVindicator) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return TypedActionResult.pass(stack);
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a vindicator, you can only heal from cooked meat and golden apples."), true);
                    return TypedActionResult.fail(stack);
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }

            // Evoker food — same food list as pillager
            if (isEvoker) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return TypedActionResult.pass(stack);
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs an evoker, you can only heal from cooked meat and golden apples."), true);
                    return TypedActionResult.fail(stack);
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }

            if (!isZombie && !isBabyZombie && !isHusk && !isBabyHusk && !isDrowned && !isBabyDrowned) return TypedActionResult.pass(stack);

            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) return TypedActionResult.pass(stack);

            if (!ZombiePossessionController.isZombieFood(stack)) {
                serverPlayer.sendMessage(
                        Text.literal("§cAs a zombie, you can only heal from raw meat and rotten flesh."), true);
                return TypedActionResult.fail(stack);
            }

            // Start the eating animation — player must hold right-click for full duration
            EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
            player.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        });
    }

    /**
     * Called every server tick. Detects when eating animation reaches completion
     * (itemUseTimeLeft == 1, about to finish) and applies healing.
     * We heal at timeLeft==1 rather than after stopping, so we can distinguish
     * a completed eat from a cancelled one (player released early).
     */
    public static void tick(ServerPlayerEntity player) {
        boolean isZombie     = ZombiePossessionController.isZombiePossessing(player);
        boolean isBabyZombie = BabyZombiePossessionController.isBabyZombiePossessing(player);
        boolean isHusk       = HuskPossessionController.isHuskPossessing(player);
        boolean isBabyHusk   = net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(player);
        boolean isDrowned    = net.sam.samrequiemmod.possession.drowned.DrownedPossessionController.isDrownedPossessing(player);
        boolean isBabyDrowned = net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController.isBabyDrownedPossessing(player);
        boolean isPillager   = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player);
        boolean isVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player);
        boolean isEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player);
        if (!isZombie && !isBabyZombie && !isHusk && !isBabyHusk && !isDrowned && !isBabyDrowned && !isPillager && !isVindicator && !isEvoker) {
            EATING_ITEM.remove(player.getUuid());
            return;
        }

        UUID uuid = player.getUuid();
        ItemStack tracked = EATING_ITEM.get(uuid);
        if (tracked == null) return;

        if (!player.isUsingItem()) {
            // Player stopped using item — either cancelled or finished.
            // Either way, clean up without healing (eatFood mixin handles the finished case).
            EATING_ITEM.remove(uuid);
            return;
        }

        // Detect the last tick of eating: timeLeft == 1 means finishing next tick
        if (player.getItemUseTimeLeft() == 1) {
            EATING_ITEM.remove(uuid);
            // Pillager food healing
            if (net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.getPillagerFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            // Vindicator food healing (same food list as pillager)
            if (net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.getPillagerFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            // Evoker food healing
            if (net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.getPillagerFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (!ZombiePossessionController.isZombieFood(tracked)) return;
            float healAmount = ZombiePossessionController.getZombieFoodHealing(tracked);
            if (healAmount > 0.0f) {
                player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
            }
        }
    }
}