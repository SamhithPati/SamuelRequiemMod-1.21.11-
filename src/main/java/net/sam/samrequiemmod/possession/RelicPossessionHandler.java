package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;

public final class RelicPossessionHandler {

    private RelicPossessionHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (livingTarget.getType() == EntityType.COPPER_GOLEM
                    || livingTarget.getType() == net.sam.samrequiemmod.entity.ModEntities.CORRUPTED_MERCHANT) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof MobEntity)) return ActionResult.PASS;

            if (entity instanceof PlayerEntity) {
                serverPlayer.sendMessage(Text.literal("§cYou cannot possess players."), true);
                return ActionResult.FAIL;
            }

            if (!entity.isAlive()) return ActionResult.FAIL;

            if (PossessionManager.isPossessing(serverPlayer)) {
                serverPlayer.sendMessage(Text.literal("§eYou are already possessing something."), true);
                return ActionResult.FAIL;
            }

            EntityType<?> type = livingTarget.getType();
            if (type == EntityType.ENDER_DRAGON) {
                return ActionResult.FAIL;
            }

            if (type == EntityType.WITHER) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fWither"), true);
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.CREAKING && entity instanceof CreakingEntity creaking) {
                net.sam.samrequiemmod.possession.creaking.CreakingPossessionController.initializeFromCreaking(serverPlayer, creaking);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.creaking.CreakingNetworking.broadcastState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fCreaking"), true);
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.WARDEN) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fWarden"), true);
                return ActionResult.SUCCESS;
            }

            // Iron Golem: allowed mob type
            if (type == EntityType.IRON_GOLEM) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fIron Golem"), true);
                return ActionResult.SUCCESS;
            }

            // Pillager: allowed mob type
            if (type == EntityType.PILLAGER) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            // Aquatic mobs: cod, salmon, pufferfish, tropical fish, squid, dolphin
            if (type == EntityType.COD || type == EntityType.SALMON
                    || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH
                    || type == EntityType.SQUID || type == EntityType.DOLPHIN
                    || type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS) {
                // Capture tropical fish variant before discarding the entity
                if (type == EntityType.TROPICAL_FISH
                        && entity instanceof net.minecraft.entity.passive.TropicalFishEntity tropicalFish) {
                    // Get the raw variant int from the tropical fish entity
                    // Get the raw variant int from the entity's data tracker
                    int variantInt = tropicalFish.getDataTracker().get(
                            net.sam.samrequiemmod.mixin.client.TropicalFishEntityVariantAccessor.getVariantKey());
                    net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantState.setServerVariant(
                            serverPlayer.getUuid(), variantInt);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                // Broadcast tropical fish variant after possession starts
                if (type == EntityType.TROPICAL_FISH) {
                    int variant = net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantState
                            .getServerVariant(serverPlayer.getUuid());
                    net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantNetworking
                            .broadcastVariant(serverPlayer, variant);
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            // Passive mobs: cow, pig, sheep, chicken, panda, sniffer
            if (type == EntityType.COW || type == EntityType.PIG
                    || type == EntityType.SHEEP || type == EntityType.CHICKEN
                    || type == EntityType.PANDA || type == EntityType.SNIFFER) {
                boolean isBabyPassive = (entity instanceof net.minecraft.entity.passive.PassiveEntity passive)
                        && passive.isBaby();
                if (entity instanceof net.minecraft.entity.passive.SheepEntity sheep) {
                    net.sam.samrequiemmod.possession.passive.SheepAppearanceState.setServerAppearance(
                            serverPlayer.getUuid(), sheep.getColor(), sheep.isSheared());
                }
                if (entity instanceof PandaEntity panda) {
                    net.sam.samrequiemmod.possession.passive.PandaAppearanceState.setServerAppearance(
                            serverPlayer.getUuid(), panda.getMainGene(), panda.getHiddenGene());
                }
                if (isBabyPassive) {
                    net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.setServerBaby(serverPlayer.getUuid(), true);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                if (isBabyPassive) {
                    net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.broadcast(serverPlayer, true);
                }
                if (entity instanceof net.minecraft.entity.passive.SheepEntity sheep) {
                    net.sam.samrequiemmod.possession.passive.SheepAppearanceNetworking.broadcast(
                            serverPlayer, sheep.getColor(), sheep.isSheared());
                }
                if (entity instanceof PandaEntity panda) {
                    net.sam.samrequiemmod.possession.passive.PandaAppearanceNetworking.broadcast(
                            serverPlayer, panda.getMainGene(), panda.getHiddenGene());
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()
                                + (isBabyPassive ? " §7(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.HOGLIN || type == EntityType.ZOGLIN) {
                boolean isBaby = (entity instanceof net.minecraft.entity.mob.HoglinEntity hoglin && !hoglin.isAdult())
                        || (entity instanceof net.minecraft.entity.mob.ZoglinEntity zoglin && !zoglin.isAdult());
                if (isBaby) {
                    net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.setServerBaby(serverPlayer.getUuid(), true);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                if (isBaby) {
                    net.sam.samrequiemmod.possession.hoglin.BabyHoglinNetworking.broadcast(serverPlayer, true);
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()
                                + (isBaby ? " §7(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.SILVERFISH) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fSilverfish"),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.BLAZE || type == EntityType.GHAST) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
                int slimeSize = entity instanceof net.minecraft.entity.mob.SlimeEntity slime ? slime.getSize() : 4;
                net.sam.samrequiemmod.possession.slime.SlimePossessionController.setInitialSize(serverPlayer, slimeSize);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.slime.SlimePossessionController.broadcastSize(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("Â§5You used the Possession Relic on Â§f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.WOLF && entity instanceof net.minecraft.entity.passive.WolfEntity wolf) {
                net.sam.samrequiemmod.possession.wolf.WolfPossessionController.initializeWolfState(serverPlayer, wolf);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.wolf.WolfPossessionController.syncAllState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("Â§5You used the Possession Relic on Â§fWolf"
                                + (wolf.isBaby() ? " Â§7(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.FOX && entity instanceof net.minecraft.entity.passive.FoxEntity fox) {
                net.sam.samrequiemmod.possession.fox.FoxPossessionController.initializeFoxState(serverPlayer, fox);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.fox.FoxPossessionController.syncAllState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7fFox"
                                + (fox.isBaby() ? " \u00A77(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.OCELOT && entity instanceof net.minecraft.entity.passive.OcelotEntity ocelot) {
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.initializeOcelotState(serverPlayer, ocelot);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.syncOcelotState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7fOcelot"
                                + (ocelot.isBaby() ? " \u00A77(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.CAT && entity instanceof net.minecraft.entity.passive.CatEntity cat) {
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.initializeCatState(serverPlayer, cat);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.syncCatState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7fCat"
                                + (cat.isBaby() ? " \u00A77(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.VEX || type == EntityType.BAT) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.BEE || type == EntityType.PARROT) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §f"
                                + livingTarget.getType().getName().getString()),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.HORSE && entity instanceof net.minecraft.entity.passive.HorseEntity horse) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializeHorseState(serverPlayer, horse);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if ((type == EntityType.DONKEY || type == EntityType.MULE || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE)
                    && entity instanceof net.minecraft.entity.passive.AbstractHorseEntity horse) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, horse);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.ENDERMITE || type == EntityType.SHULKER) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.GOAT && entity instanceof net.minecraft.entity.passive.GoatEntity goat) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, goat);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.POLAR_BEAR && entity instanceof net.minecraft.entity.passive.PolarBearEntity bear) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, bear);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.RABBIT && entity instanceof net.minecraft.entity.passive.RabbitEntity rabbit) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializeRabbitState(serverPlayer, rabbit);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.TURTLE && entity instanceof net.minecraft.entity.passive.TurtleEntity turtle) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, turtle);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.STRIDER && entity instanceof net.minecraft.entity.passive.StriderEntity strider) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, strider);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.AXOLOTL && entity instanceof net.minecraft.entity.passive.AxolotlEntity axolotl) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializeAxolotlState(serverPlayer, axolotl);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.SNOW_GOLEM || (type == EntityType.CAMEL && entity instanceof net.minecraft.entity.passive.CamelEntity camel)) {
                if (entity instanceof net.minecraft.entity.passive.CamelEntity camelEntity) {
                    net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, camelEntity);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.ARMADILLO && entity instanceof net.minecraft.entity.passive.ArmadilloEntity armadillo) {
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.initializePassiveBaby(serverPlayer, armadillo);
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.syncState(serverPlayer);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.VILLAGER && entity instanceof net.minecraft.entity.passive.VillagerEntity villager) {
                boolean isBabyVillager = villager.isBaby();
                if (isBabyVillager) {
                    net.sam.samrequiemmod.possession.villager.VillagerState.setServerBaby(serverPlayer.getUuid(), true);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                if (isBabyVillager) {
                    net.sam.samrequiemmod.possession.villager.VillagerNetworking.broadcastBaby(serverPlayer, true);
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7fVillager"
                                + (isBabyVillager ? " \u00A77(Baby)" : "")),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (type == EntityType.WANDERING_TRADER && entity instanceof net.minecraft.entity.passive.WanderingTraderEntity) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("\u00A75You used the Possession Relic on \u00A7fWandering Trader"),
                        true
                );
                return ActionResult.SUCCESS;
            }

            // Piglin: detect baby, copy armor from mob
            if (type == EntityType.PIGLIN) {
                boolean isBabyPiglin = (entity instanceof net.minecraft.entity.mob.PiglinEntity piglin)
                        && piglin.isBaby();
                if (isBabyPiglin) {
                    net.sam.samrequiemmod.possession.piglin.BabyPiglinState.setServerBaby(serverPlayer.getUuid(), true);
                }
                float mobHealth = livingTarget.getHealth();
                // Copy armor before discarding the entity
                if (!isBabyPiglin) {
                    net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.copyArmorFromMob(serverPlayer, livingTarget);
                }
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                if (isBabyPiglin) {
                    net.sam.samrequiemmod.possession.piglin.BabyPiglinNetworking.broadcast(serverPlayer, true);
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fPiglin"
                                + (isBabyPiglin ? " §7(Baby)" : "")),
                        true);
                return ActionResult.SUCCESS;
            }

            // Piglin Brute: copy armor from mob
            if (type == EntityType.PIGLIN_BRUTE) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fPiglin Brute"), true);
                return ActionResult.SUCCESS;
            }

            // Zombified Piglin: detect baby
            if (type == EntityType.ZOMBIFIED_PIGLIN) {
                boolean isBabyZP = (entity instanceof net.minecraft.entity.mob.ZombifiedPiglinEntity zp)
                        && zp.isBaby();
                if (isBabyZP) {
                    net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.setServerBaby(serverPlayer.getUuid(), true);
                }
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                if (isBabyZP) {
                    net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinNetworking.broadcast(serverPlayer, true);
                }
                entity.discard();
                serverPlayer.sendMessage(
                        Text.literal("§5You used the Possession Relic on §fZombified Piglin"
                                + (isBabyZP ? " §7(Baby)" : "")),
                        true);
                return ActionResult.SUCCESS;
            }

            // Detect baby zombie (EntityType.ZOMBIE + isBaby)
            boolean isBabyZombie = (type == EntityType.ZOMBIE)
                    && (entity instanceof ZombieEntity zombie)
                    && zombie.isBaby();

            // Detect baby husk (EntityType.HUSK + isBaby)
            // HuskEntity extends ZombieEntity, so we check HuskEntity first
            boolean isBabyHusk = (type == EntityType.HUSK)
                    && (entity instanceof HuskEntity husk)
                    && husk.isBaby();

            // Detect baby drowned
            boolean isBabyDrowned = (type == EntityType.DROWNED)
                    && (entity instanceof DrownedEntity drowned)
                    && drowned.isBaby();

            // Detect baby zombie villager
            boolean isBabyZombieVillager = (type == EntityType.ZOMBIE_VILLAGER)
                    && (entity instanceof net.minecraft.entity.mob.ZombieVillagerEntity zv)
                    && zv.isBaby();

            // Set baby states BEFORE startPossession so PossessionEffects picks up the correct profile
            if (isBabyZombie)  BabyZombieState.setServerBaby(serverPlayer.getUuid(), true);
            if (isBabyHusk)    BabyHuskState.setServerBaby(serverPlayer.getUuid(), true);
            if (isBabyDrowned) BabyDrownedState.setServerBaby(serverPlayer.getUuid(), true);
            if (isBabyZombieVillager) net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.setServerBaby(serverPlayer.getUuid(), true);

            // Start possession — pass mob's current health so player health updates to match
            float mobHealth = livingTarget.getHealth();
            PossessionManager.startPossession(serverPlayer, type, mobHealth);

            // Broadcast baby states to clients after possession starts
            if (isBabyZombie)  BabyZombieNetworking.broadcastBabyZombieSync(serverPlayer, true);
            if (isBabyHusk)    BabyHuskNetworking.broadcastBabyHuskSync(serverPlayer, true);
            if (isBabyDrowned) BabyDrownedNetworking.broadcast(serverPlayer, true);
            if (isBabyZombieVillager) net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.broadcast(serverPlayer, true);

            entity.discard();

            serverPlayer.sendMessage(
                    Text.literal("§5You used the Possession Relic on §f"
                            + livingTarget.getType().getName().getString()
                            + (isBabyZombie || isBabyHusk || isBabyDrowned || isBabyZombieVillager ? " §7(Baby)" : "")),
                    true
            );

            return ActionResult.SUCCESS;
        });
    }
}






