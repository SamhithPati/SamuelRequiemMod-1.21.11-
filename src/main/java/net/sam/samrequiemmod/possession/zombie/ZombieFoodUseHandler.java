package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
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
            if (world.isClient()) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            boolean isZombie     = ZombiePossessionController.isZombiePossessing(serverPlayer);
            boolean isBabyZombie = BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer);
            boolean isHusk       = HuskPossessionController.isHuskPossessing(serverPlayer);
            boolean isBabyHusk   = net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer);
            boolean isDrowned    = net.sam.samrequiemmod.possession.drowned.DrownedPossessionController.isDrownedPossessing(serverPlayer);
            boolean isBabyDrowned = net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer);
            boolean isZombieVillager = net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(serverPlayer);
            boolean isBabyZombieVillager = net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(serverPlayer);
            boolean isVillager = net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(serverPlayer);
            boolean isPillager   = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(serverPlayer);
            boolean isVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(serverPlayer);
            boolean isEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(serverPlayer);
            boolean isRavager = net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(serverPlayer);
            boolean isWitch = net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(serverPlayer);
            boolean isSpider = net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(serverPlayer);
            boolean isHoglin = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isHoglinPossessing(serverPlayer);
            boolean isZoglin = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(serverPlayer);
            boolean isGuardian = net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(serverPlayer);
            boolean isBlaze = net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(serverPlayer);
            boolean isGhast = net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(serverPlayer);
            boolean isSlime = net.sam.samrequiemmod.possession.slime.SlimePossessionController.isSlimePossessing(serverPlayer);
            boolean isMagmaCube = net.sam.samrequiemmod.possession.slime.SlimePossessionController.isMagmaCubePossessing(serverPlayer);
            boolean isWolf = net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(serverPlayer);
            boolean isFox = net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(serverPlayer);
            boolean isFeline = net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(serverPlayer);
            boolean isVex = net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexPossessing(serverPlayer);
            boolean isWarden = net.sam.samrequiemmod.possession.warden.WardenPossessionController.isWardenPossessing(serverPlayer);
            boolean isBreeze = net.sam.samrequiemmod.possession.breeze.BreezePossessionController.isBreezePossessing(serverPlayer);
            boolean isPanda = net.sam.samrequiemmod.possession.passive.PandaPossessionController.isPandaPossessing(serverPlayer);
            boolean isBeast = net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(
                    net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer));

            if (isPanda) {
                if (!net.sam.samrequiemmod.possession.passive.PandaPossessionController.isPandaFood(stack)) {
                    if (stack.get(DataComponentTypes.FOOD) != null) {
                        serverPlayer.sendMessage(
                                Text.literal(net.sam.samrequiemmod.possession.passive.PandaPossessionController.getFoodErrorMessage()), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.passive.PandaPossessionController.getPandaFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_PANDA_EAT,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.9f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            if (isWarden) {
                if (!net.sam.samrequiemmod.possession.warden.WardenPossessionController.isWardenFood(stack)) {
                    if (stack.get(DataComponentTypes.FOOD) != null) {
                        serverPlayer.sendMessage(
                                Text.literal(net.sam.samrequiemmod.possession.warden.WardenPossessionController.getFoodErrorMessage()), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.warden.WardenPossessionController.getWardenFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.9f, 0.9f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            if (isBreeze) {
                if (!net.sam.samrequiemmod.possession.breeze.BreezePossessionController.isBreezeFood(stack)) {
                    if (net.sam.samrequiemmod.possession.breeze.BreezePossessionController.blocksFoodUse(stack)) {
                        serverPlayer.sendMessage(
                                Text.literal(net.sam.samrequiemmod.possession.breeze.BreezePossessionController.getFoodErrorMessage()), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.breeze.BreezePossessionController.getBreezeFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_BREEZE_WHIRL,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            // Enderman food — chorus fruit (no FoodComponent override, instant heal on right-click)
            boolean isEnderman = net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(serverPlayer);
            if (isEnderman) {
                if (!net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanFood(stack)) {
                    net.minecraft.component.type.FoodComponent foodCheck = stack.get(DataComponentTypes.FOOD);
                    if (foodCheck != null) {
                        serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal("§cAs an enderman, you can only heal from chorus fruit."), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.getEndermanFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            // Skeleton/Wither Skeleton food — bones (no FoodComponent, instant heal on right-click)
            boolean isSkeleton = net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(serverPlayer);
            boolean isWitherSkeleton = net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(serverPlayer);
            if (isSkeleton || isWitherSkeleton) {
                boolean isSkeletonFood = isSkeleton
                        ? net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isSkeletonFood(stack)
                        : net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonFood(stack);
                if (!isSkeletonFood) {
                    net.minecraft.component.type.FoodComponent foodCheck = stack.get(DataComponentTypes.FOOD);
                    if (foodCheck != null) {
                        serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal("§cAs a skeleton, you can only heal from bones."), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = isSkeleton
                        ? net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.getSkeletonFoodHealing(stack)
                        : net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.getWitherSkeletonFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_SKELETON_AMBIENT,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.2f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            // Iron golem food — iron ingots (no FoodComponent, instant heal on right-click)
            boolean isIronGolem = net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(serverPlayer);
            if (isIronGolem) {
                if (!net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemFood(stack)) {
                    net.minecraft.component.type.FoodComponent foodCheck = stack.get(DataComponentTypes.FOOD);
                    if (foodCheck != null) {
                        serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal("§cAs an iron golem, you can only heal from iron ingots."), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.getIronGolemFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    // Play iron golem repair sound
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_IRON_GOLEM_REPAIR,
                            net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            // Creeper food — gunpowder (no FoodComponent, instant heal on right-click)
            boolean isCreeper = net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.isCreeperPossessing(serverPlayer);
            if (isCreeper) {
                if (!net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.isCreeperFood(stack)) {
                    net.minecraft.component.type.FoodComponent foodCheck = stack.get(DataComponentTypes.FOOD);
                    if (foodCheck != null) {
                        serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal("§cAs a creeper, you can only heal from gunpowder."), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.getCreeperFoodHealing(stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    // Play creeper eating/hissing sound
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_CREEPER_PRIMED,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            if (isSpider) {
                if (net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isSpiderFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.spider.SpiderPossessionController.getSpiderFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_SPIDER_AMBIENT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.2f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.spider.SpiderPossessionController.blocksFoodUse(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.spider.SpiderPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isHoglin) {
                if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isHoglinFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.getHoglinFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_HOGLIN_AMBIENT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.blocksHoglinFoodUse(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.getHoglinFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isZoglin) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.getZoglinFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isGuardian) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isGuardianFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isBlaze) {
                if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazeFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.firemob.BlazePossessionController.getBlazeFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_BLAZE_AMBIENT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.1f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.blocksFoodUse(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.firemob.BlazePossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isGhast) {
                if (net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.firemob.GhastPossessionController.getGhastFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_GHAST_AMBIENT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.firemob.GhastPossessionController.blocksFoodUse(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.firemob.GhastPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isVex) {
                if (net.sam.samrequiemmod.possession.vex.VexPossessionController.blocksFoodUse(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.vex.VexPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isSlime) {
                if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.isSlimeFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.slime.SlimePossessionController.getSlimeFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_SLIME_SQUISH,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.6f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.blocksFoodUse(stack, false)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.slime.SlimePossessionController.getFoodErrorMessage(false)), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isMagmaCube) {
                if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.isMagmaFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.slime.SlimePossessionController.getMagmaFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_MAGMA_CUBE_SQUISH,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.6f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.blocksFoodUse(stack, true)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.slime.SlimePossessionController.getFoodErrorMessage(true)), true);
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }

            if (isWolf) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.wolf.WolfPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isFox) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null && !net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxFood(stack)) {
                    return ActionResult.PASS;
                }
                if (!net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.fox.FoxPossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                if (food == null) {
                    float healAmount = net.sam.samrequiemmod.possession.fox.FoxPossessionController.getFoxFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_FOX_EAT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.7f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isFeline) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.feline.FelinePossessionController.isFelineFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal(net.sam.samrequiemmod.possession.feline.FelinePossessionController.getFoodErrorMessage()), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Passive mob food — wheat (cow/sheep), seeds (chicken), potatoes/carrots/beetroot (pig)
            // Many of these items (wheat, seeds) don't have FoodComponent so we apply
            // healing instantly on right-click and consume one item from the stack.
            boolean isPassiveMob = net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.isPassiveMobPossessing(serverPlayer);
            if (isPassiveMob) {
                net.minecraft.entity.EntityType<?> passiveType = net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer);
                if (!net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.isPassiveMobFood(passiveType, stack)) {
                    // Block eating regular food items while possessing a passive mob
                    FoodComponent foodCheck = stack.get(DataComponentTypes.FOOD);
                    if (foodCheck != null) {
                        serverPlayer.sendMessage(
                                net.minecraft.text.Text.literal(
                                        net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.getFoodErrorMessage(passiveType)), true);
                        return ActionResult.FAIL;
                    }
                    return ActionResult.PASS;
                }
                float healAmount = net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.getPassiveMobFoodHealing(passiveType, stack);
                if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                    serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                    if (!serverPlayer.isCreative()) stack.decrement(1);
                    // Play eating sound
                    serverPlayer.getEntityWorld().playSound(null,
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EAT,
                            net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f + (serverPlayer.getRandom().nextFloat() - 0.5f) * 0.4f);
                    return ActionResult.CONSUME;
                }
                // Block vanilla food processing for food items (potato, carrot have FoodComponent)
                FoodComponent foodBlockCheck = stack.get(DataComponentTypes.FOOD);
                if (foodBlockCheck != null) return ActionResult.FAIL;
                return ActionResult.PASS;
            }

            // Witch food — carrots, golden carrots, mushroom stew, potatoes, golden apples
            if (isWitch) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a witch, you can only heal from carrots, potatoes, mushroom stew, and golden apples."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Ravager food — raw and cooked meats
            if (isRavager) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a ravager, you can only heal from raw or cooked meat."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Pillager food — only intercept if the item actually has food component
            if (isPillager) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS; // not a food item, let vanilla handle
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a pillager, you can only heal from cooked meat and golden apples."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Vindicator food — same food list as pillager
            if (isVindicator) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a vindicator, you can only heal from cooked meat and golden apples."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Evoker food — same food list as pillager
            if (isEvoker) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs an evoker, you can only heal from cooked meat and golden apples."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Piglin/brute food — eating-based: porkchop, cooked porkchop, carrot, golden carrot
            // Instant: warped fungus, crimson fungus (no FoodComponent)
            boolean isPiglinType = net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinPossessing(serverPlayer)
                    || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(serverPlayer)
                    || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(serverPlayer);
            if (isPiglinType) {
                // Instant food: warped/crimson fungus (no FoodComponent)
                if (net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinInstantFood(stack)) {
                    float healAmount = net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.getPiglinInstantFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EAT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                // Eating-based food: porkchop, cooked porkchop, carrot, golden carrot
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a piglin, you can only heal from porkchops, carrots, golden carrots, and nether fungi."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Zombified piglin food — same as zombie subtypes (raw meat and rotten flesh)
            boolean isZombifiedPiglin = net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(serverPlayer);
            if (isZombifiedPiglin) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                if (!ZombiePossessionController.isZombieFood(stack)) {
                    serverPlayer.sendMessage(
                            Text.literal("§cAs a zombified piglin, you can only heal from raw meat and rotten flesh."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isVillager) {
                FoodComponent food = stack.get(DataComponentTypes.FOOD);
                if (food == null) return ActionResult.PASS;
                boolean validVillagerFood = stack.isOf(net.minecraft.item.Items.CARROT)
                        || stack.isOf(net.minecraft.item.Items.POTATO)
                        || stack.isOf(net.minecraft.item.Items.BAKED_POTATO)
                        || stack.isOf(net.minecraft.item.Items.MUSHROOM_STEW);
                if (!validVillagerFood) {
                    serverPlayer.sendMessage(
                            Text.literal("\u00A7cAs a villager, you can only heal from carrots, potatoes, baked potatoes, and mushroom stew."), true);
                    return ActionResult.FAIL;
                }
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (isBeast) {
                net.minecraft.entity.EntityType<?> beastType =
                        net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer);
                if (beastType == net.minecraft.entity.EntityType.BEE) {
                    if (!net.sam.samrequiemmod.possession.beast.BeastPossessionController.isBeeFood(stack)) {
                        if (stack.get(DataComponentTypes.FOOD) != null) {
                            serverPlayer.sendMessage(Text.literal(net.sam.samrequiemmod.possession.beast.BeastPossessionController.getFoodErrorMessage(serverPlayer)), true);
                            return ActionResult.FAIL;
                        }
                        return ActionResult.PASS;
                    }
                    float healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getBeeFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_BEE_LOOP,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.1f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }
                if (beastType == net.minecraft.entity.EntityType.PARROT) {
                    if (!net.sam.samrequiemmod.possession.beast.BeastPossessionController.isParrotFood(stack)) {
                        if (stack.get(DataComponentTypes.FOOD) != null) {
                            serverPlayer.sendMessage(Text.literal(net.sam.samrequiemmod.possession.beast.BeastPossessionController.getFoodErrorMessage(serverPlayer)), true);
                            return ActionResult.FAIL;
                        }
                        return ActionResult.PASS;
                    }
                    float healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getParrotFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        serverPlayer.getEntityWorld().playSound(null,
                                serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                net.minecraft.sound.SoundEvents.ENTITY_PARROT_EAT,
                                net.minecraft.sound.SoundCategory.PLAYERS, 0.6f, 1.0f);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }

                if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.isHorseLikePossessing(serverPlayer)
                        && net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer) == net.minecraft.entity.EntityType.SKELETON_HORSE) {
                    if (!net.sam.samrequiemmod.possession.beast.BeastPossessionController.isSkeletonHorseFood(stack)) {
                        if (stack.get(DataComponentTypes.FOOD) != null) {
                            serverPlayer.sendMessage(Text.literal(net.sam.samrequiemmod.possession.beast.BeastPossessionController.getFoodErrorMessage(serverPlayer)), true);
                            return ActionResult.FAIL;
                        }
                        return ActionResult.PASS;
                    }
                    float healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getSkeletonHorseFoodHealing(stack);
                    if (healAmount > 0.0f && serverPlayer.getHealth() < serverPlayer.getMaxHealth()) {
                        serverPlayer.setHealth(Math.min(serverPlayer.getHealth() + healAmount, serverPlayer.getMaxHealth()));
                        if (!serverPlayer.isCreative()) stack.decrement(1);
                        return ActionResult.CONSUME;
                    }
                    return ActionResult.PASS;
                }

                if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.blocksFoodUse(serverPlayer, stack)) {
                    serverPlayer.sendMessage(Text.literal(net.sam.samrequiemmod.possession.beast.BeastPossessionController.getFoodErrorMessage(serverPlayer)), true);
                    return ActionResult.FAIL;
                }
                if (stack.get(DataComponentTypes.FOOD) == null) return ActionResult.PASS;
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (!isZombie && !isBabyZombie && !isHusk && !isBabyHusk && !isDrowned && !isBabyDrowned && !isZombieVillager && !isBabyZombieVillager) return ActionResult.PASS;

            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) return ActionResult.PASS;
            if ((isZombieVillager || isBabyZombieVillager) && stack.isOf(net.minecraft.item.Items.GOLDEN_APPLE)) {
                EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
                player.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            if (!ZombiePossessionController.isZombieFood(stack)) {
                serverPlayer.sendMessage(
                        Text.literal("§cAs a zombie, you can only heal from raw meat and rotten flesh."), true);
                return ActionResult.FAIL;
            }

            // Start the eating animation — player must hold right-click for full duration
            EATING_ITEM.put(serverPlayer.getUuid(), stack.copy());
            player.setCurrentHand(hand);
            return ActionResult.CONSUME;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        boolean isZombie     = ZombiePossessionController.isZombiePossessing(player);
        boolean isBabyZombie = BabyZombiePossessionController.isBabyZombiePossessing(player);
        boolean isHusk       = HuskPossessionController.isHuskPossessing(player);
        boolean isBabyHusk   = net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(player);
        boolean isDrowned    = net.sam.samrequiemmod.possession.drowned.DrownedPossessionController.isDrownedPossessing(player);
        boolean isBabyDrowned = net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController.isBabyDrownedPossessing(player);
        boolean isZombieVillager2 = net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(player);
        boolean isBabyZombieVillager2 = net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(player);
        boolean isVillager2 = net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player);
        boolean isPillager   = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player);
        boolean isVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player);
        boolean isEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player);
        boolean isRavager = net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player);
        boolean isWitch = net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player);
        boolean isSpider = net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(player);
        boolean isZoglin = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(player);
        boolean isGuardian = net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(player);
        boolean isBlaze = net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(player);
        boolean isGhast = net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(player);
        boolean isSlime = net.sam.samrequiemmod.possession.slime.SlimePossessionController.isAnySlimePossessing(player);
        boolean isWolf = net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(player);
        boolean isFox = net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(player);
        boolean isFeline = net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(player);
        boolean isPanda = net.sam.samrequiemmod.possession.passive.PandaPossessionController.isPandaPossessing(player);
        boolean isPassiveMob2 = net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.isPassiveMobPossessing(player);
        boolean isBeast = net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(
                net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(player));
        boolean isPiglinType2 = net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinPossessing(player)
                || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(player)
                || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player);
        boolean isZombifiedPiglin2 = net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(player);
        if (!isZombie && !isBabyZombie && !isHusk && !isBabyHusk && !isDrowned && !isBabyDrowned && !isZombieVillager2 && !isBabyZombieVillager2 && !isVillager2 && !isPillager && !isVindicator && !isEvoker && !isRavager && !isWitch && !isSpider && !isZoglin && !isGuardian && !isBlaze && !isGhast && !isSlime && !isWolf && !isFox && !isFeline && !isPanda && !isPassiveMob2 && !isBeast && !isPiglinType2 && !isZombifiedPiglin2) {
            EATING_ITEM.remove(player.getUuid());
            return;
        }

        UUID uuid = player.getUuid();
        ItemStack tracked = EATING_ITEM.get(uuid);
        if (tracked == null) return;

        if (!player.isUsingItem()) {
            // Player stopped using item — for villager foods, the finish can happen
            // between ticks, so apply the heal here before cleaning up.
            if (net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player)) {
                healVillagerFood(player, tracked);
            } else if (tracked.isOf(net.minecraft.item.Items.GOLDEN_APPLE)
                    && (net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.canStartCure(player, tracked)
                    || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.canStartCure(player, tracked))) {
                net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerCureTracker.start(player);
            }
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
            // Ravager food healing (raw + cooked meats)
            if (net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.illager.RavagerPossessionController.getRavagerFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            // Witch food healing (carrots, potatoes, mushroom stew, golden apples)
            if (net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.illager.WitchPossessionController.getWitchFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.getZoglinFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.getGuardianFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.wolf.WolfPossessionController.getWolfFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player)) {
                healVillagerFood(player, tracked);
                return;
            }
            if (tracked.isOf(net.minecraft.item.Items.GOLDEN_APPLE)
                    && (net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.canStartCure(player, tracked)
                    || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.canStartCure(player, tracked))) {
                net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerCureTracker.start(player);
                return;
            }
            if (isBeast) {
                net.minecraft.entity.EntityType<?> beastType = net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(player);
                float healAmount = 0.0f;
                if (beastType == net.minecraft.entity.EntityType.HORSE || beastType == net.minecraft.entity.EntityType.MULE) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getHorseFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.ZOMBIE_HORSE) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getZombieHorseFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.SKELETON_HORSE) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getSkeletonHorseFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.POLAR_BEAR) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getPolarFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.STRIDER) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getStriderFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.AXOLOTL) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getAxolotlFoodHealing(tracked);
                } else if (beastType == net.minecraft.entity.EntityType.CAMEL) {
                    healAmount = net.sam.samrequiemmod.possession.beast.BeastPossessionController.getCamelFoodHealing(tracked);
                }
                if (healAmount > 0.0f) {
                    player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                }
                return;
            }
            if (net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.fox.FoxPossessionController.getFoxFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            if (net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(player)) {
                float healAmount = net.sam.samrequiemmod.possession.feline.FelinePossessionController.getFelineFoodHealing(tracked);
                if (healAmount > 0.0f) player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
                return;
            }
            // Piglin food healing (porkchops, carrots, golden carrots)
            if (net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinPossessing(player)
                    || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(player)
                    || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player)) {
                float piglinHeal = net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.getPiglinFoodHealing(tracked);
                if (piglinHeal > 0.0f) player.setHealth(Math.min(player.getHealth() + piglinHeal, player.getMaxHealth()));
                return;
            }
            // Zombified piglin food healing (same as zombie)
            if (net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(player)) {
                float zpHeal = ZombiePossessionController.getZombieFoodHealing(tracked);
                if (zpHeal > 0.0f) player.setHealth(Math.min(player.getHealth() + zpHeal, player.getMaxHealth()));
                return;
            }
            if (!ZombiePossessionController.isZombieFood(tracked)) return;
            float healAmount = ZombiePossessionController.getZombieFoodHealing(tracked);
            if (healAmount > 0.0f) {
                player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
            }
        }
    }

    private static void healVillagerFood(ServerPlayerEntity player, ItemStack tracked) {
        FoodComponent food = tracked.get(DataComponentTypes.FOOD);
        float healAmount = food == null ? 0.0f : Math.max(2.0f, food.nutrition());
        if (healAmount > 0.0f) {
            player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        }
    }
}








