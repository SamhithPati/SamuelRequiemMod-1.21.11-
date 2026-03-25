package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.drowned.DrownedTridentManager;
import net.sam.samrequiemmod.possession.WaterConversionTracker;
import net.sam.samrequiemmod.possession.WaterShakeNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PossessionManager {
    private static final Map<UUID, PossessionData> PLAYER_POSSESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, EntityType<?>> LAST_DIMENSION_TYPE = new ConcurrentHashMap<>();

    /** Stores the player's health and hunger level before they possessed a mob. */
    private static final Map<UUID, float[]> PRE_POSSESSION_STATE = new ConcurrentHashMap<>();

    private PossessionManager() {
    }

    private static PossessionData getOrCreate(ServerPlayerEntity player) {
        return PLAYER_POSSESSIONS.computeIfAbsent(player.getUuid(), uuid -> new PossessionData());
    }

    @Nullable
    public static PossessionData get(PlayerEntity player) {
        return PLAYER_POSSESSIONS.get(player.getUuid());
    }

    public static boolean isPossessing(PlayerEntity player) {
        PossessionData data = get(player);
        return data != null && data.isPossessing();
    }

    @Nullable
    public static EntityType<?> getPossessedType(PlayerEntity player) {
        PossessionData data = get(player);
        return data == null ? null : data.getPossessedType();
    }

    public static void startPossession(ServerPlayerEntity player, EntityType<?> type) {
        startPossession(player, type, -1f);
    }

    public static void startPossession(ServerPlayerEntity player, EntityType<?> type, float mobHealth) {
        // Save pre-possession state before anything changes
        PRE_POSSESSION_STATE.put(player.getUuid(), new float[]{
                player.getHealth(),
                (float) player.getHungerManager().getFoodLevel()
        });

        PossessionData data = getOrCreate(player);
        data.setPossessedType(type);

        // Apply stat modifiers (max health, speed, attack) immediately so
        // getMaxHealth() is correct before we set the player's health below.
        net.sam.samrequiemmod.possession.PossessionEffects.apply(player);

        PossessionNetworking.broadcastPossessionSync(player, type);

        // Set player health to match the possessed mob's current health,
        // clamped to the (now-updated) max health.
        if (mobHealth > 0) {
            final float h = mobHealth;
            player.setHealth(Math.min(h, player.getMaxHealth()));
        } else {
            // No mob health provided — top up to full
            player.setHealth(player.getMaxHealth());
        }

        // Immediately clear all unprovoked mob targets so mobs that were
        // already chasing the player stop the moment possession begins.
        clearNearbyMobTargets(player);

        SamuelRequiemMod.LOGGER.info(
                "[Possession] {} started possessing {}",
                player.getGameProfile().getName(),
                EntityType.getId(type)
        );
    }

    /**
     * Clears the target of every nearby mob that is currently chasing this player,
     * unless the mob has been provoked (it hit the player first).
     * Called on possession start so mobs stop mid-chase immediately.
     */
    private static void clearNearbyMobTargets(ServerPlayerEntity player) {
        if (player.getServerWorld() == null) return;
        boolean villagerSafe = net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player);
        boolean batSafe = net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(player);

        net.minecraft.util.math.Box box = player.getBoundingBox().expand(48.0);
        java.util.List<net.minecraft.entity.mob.MobEntity> mobs =
                player.getServerWorld().getEntitiesByClass(
                        net.minecraft.entity.mob.MobEntity.class,
                        box,
                        mob -> mob.isAlive() && mob.getTarget() == player);

        for (net.minecraft.entity.mob.MobEntity mob : mobs) {
            // Golems are always hostile — don't clear them
            if (mob instanceof net.minecraft.entity.passive.IronGolemEntity && !villagerSafe && !batSafe) continue;
            if (mob instanceof net.minecraft.entity.passive.SnowGolemEntity && !batSafe) continue;
            // Provoked mobs (have hit the player) keep their target
            if (net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(mob.getUuid())) continue;
            mob.setTarget(null);
        }
    }

    public static void clearPossession(ServerPlayerEntity player) {
        // Check possession type BEFORE clearing data so onUnpossess gets the right type
        boolean wasPillager = net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player);
        boolean wasVindicator = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player);
        boolean wasEvoker = net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player);
        boolean wasRavager = net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player);
        boolean wasWitch = net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player);
        boolean wasSkeleton = net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(player);
        boolean wasWitherSkeleton = net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(player);
        boolean wasIronGolem = net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player);
        boolean wasEnderman = net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(player);
        boolean wasCreeper = net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.isCreeperPossessing(player);
        boolean wasFish = net.sam.samrequiemmod.possession.aquatic.FishPossessionController.isFishPossessing(player);
        boolean wasSquid = net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(player);
        boolean wasDolphin = net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.isDolphinPossessing(player);
        boolean wasSpider = net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(player);
        boolean wasHoglinType = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isAnyHoglinTypePossessing(player);
        boolean wasGuardianType = net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(player);
        boolean wasSilverfish = net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishPossessing(player);
        boolean wasBlaze = net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(player);
        boolean wasGhast = net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(player);
        boolean wasSlimeType = net.sam.samrequiemmod.possession.slime.SlimePossessionController.isAnySlimePossessing(player);
        boolean wasWolf = net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(player);
        boolean wasFox = net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(player);
        boolean wasFeline = net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(player);
        boolean wasVex = net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexPossessing(player);
        boolean wasBat = net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(player);
        boolean wasVillager = net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player);
        boolean wasBeast = net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(getPossessedType(player));
        boolean wasPiglin = net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinPossessing(player);
        boolean wasBabyPiglin = net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(player);
        boolean wasPiglinBrute = net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player);
        boolean wasZombifiedPiglin = net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isZombifiedPiglinPossessing(player);
        boolean wasBabyZombifiedPiglin = net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.isBabyZombifiedPiglinPossessing(player);

        PossessionData data = PLAYER_POSSESSIONS.get(player.getUuid());
        if (data != null) {
            data.clear();
        }

        // Clear stat modifiers immediately so getMaxHealth() is correct
        // before we restore the player's health below.
        net.sam.samrequiemmod.possession.PossessionEffects.clear(player);

        // Restore pre-possession health and hunger
        float[] saved = PRE_POSSESSION_STATE.remove(player.getUuid());
        if (saved != null) {
            float restoredHealth = Math.min(saved[0], player.getMaxHealth());
            player.setHealth(restoredHealth);
            player.getHungerManager().setFoodLevel((int) saved[1]);
        }

        LAST_DIMENSION_TYPE.remove(player.getUuid());
        PossessionNetworking.broadcastPossessionSync(player, null);

        // Clear baby zombie state on unpossess
        BabyZombieState.setServerBaby(player.getUuid(), false);
        BabyZombieNetworking.broadcastBabyZombieSync(player, false);
        // Clear baby husk state on unpossess
        BabyHuskState.setServerBaby(player.getUuid(), false);
        BabyHuskNetworking.broadcastBabyHuskSync(player, false);
        // Clear baby drowned state and remove trident on unpossess
        BabyDrownedState.setServerBaby(player.getUuid(), false);
        BabyDrownedNetworking.broadcast(player, false);
        // Clear baby zombie villager state on unpossess
        net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.broadcast(player, false);
        DrownedTridentManager.removeTrident(player);
        DrownedTridentManager.clearPlayer(player.getUuid());
        // Reset water conversion timer and shaking state on unpossess
        WaterConversionTracker.resetSubmerged(player.getUuid());
        WaterShakeNetworking.broadcast(player, false);
        // Clean up illager possession state — remove items given during possession
        if (wasPillager) {
            net.sam.samrequiemmod.possession.illager.PillagerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.PillagerPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasVindicator) {
            net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasEvoker) {
            net.sam.samrequiemmod.possession.illager.EvokerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.EvokerPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up ravager possession state
        if (wasRavager) {
            net.sam.samrequiemmod.possession.illager.RavagerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.RavagerPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up witch possession state
        if (wasWitch) {
            net.sam.samrequiemmod.possession.illager.WitchPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.illager.WitchPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up captain state (followers, commanded target, summoned pillagers)
        net.sam.samrequiemmod.possession.illager.CaptainHandler.onUnpossess(player);
        // Clean up raid/celebration state
        net.sam.samrequiemmod.possession.illager.RaidHandler.clearAll(player.getUuid());

        // Clean up iron golem possession state
        if (wasIronGolem) {
            net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up skeleton possession state
        if (wasSkeleton) {
            net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up wither skeleton possession state
        if (wasWitherSkeleton) {
            net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up enderman possession state
        if (wasEnderman) {
            net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up creeper possession state (fuse, charged status, explosion immunity)
        if (wasCreeper) {
            net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up aquatic possession state
        if (wasFish) {
            net.sam.samrequiemmod.possession.aquatic.FishPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.aquatic.FishPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasSquid) {
            net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasDolphin) {
            net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasSpider) {
            net.sam.samrequiemmod.possession.spider.SpiderPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.spider.SpiderPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasHoglinType) {
            net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasGuardianType) {
            net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasSilverfish) {
            net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasBlaze) {
            net.sam.samrequiemmod.possession.firemob.BlazePossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.firemob.BlazePossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasGhast) {
            net.sam.samrequiemmod.possession.firemob.GhastPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.firemob.GhastPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasSlimeType) {
            net.sam.samrequiemmod.possession.slime.SlimePossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.slime.SlimePossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasWolf) {
            net.sam.samrequiemmod.possession.wolf.WolfPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.wolf.WolfPossessionController.onUnpossessUuid(player.getUuid());
        }
        // Clear tropical fish variant state on unpossess
        net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantState.removeServer(player.getUuid());
        net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantNetworking.broadcastVariant(player, 0);

        // Clear baby passive mob state on unpossess
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.broadcast(player, false);
        net.sam.samrequiemmod.possession.wolf.WolfBabyState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.wolf.WolfNetworking.broadcastBaby(player, false);
        net.sam.samrequiemmod.possession.wolf.WolfNetworking.broadcastAngry(player, false);
        net.sam.samrequiemmod.possession.wolf.WolfNetworking.broadcastVariant(player, "minecraft:pale");
        net.sam.samrequiemmod.possession.fox.FoxNetworking.broadcastVariant(player, "red");
        net.sam.samrequiemmod.possession.feline.CatNetworking.broadcastVariant(player, "minecraft:black");
        net.sam.samrequiemmod.possession.villager.VillagerState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.villager.VillagerNetworking.broadcastBaby(player, false);
        net.sam.samrequiemmod.possession.vex.VexState.setServerAngry(player.getUuid(), false);
        net.sam.samrequiemmod.possession.vex.VexNetworking.broadcastAngry(player, false);

        if (wasFox) {
            net.sam.samrequiemmod.possession.fox.FoxPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.fox.FoxPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasFeline) {
            net.sam.samrequiemmod.possession.feline.FelinePossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.feline.FelinePossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasVex) {
            net.sam.samrequiemmod.possession.vex.VexPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.vex.VexPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasBat) {
            net.sam.samrequiemmod.possession.bat.BatPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.bat.BatPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasVillager) {
            net.sam.samrequiemmod.possession.villager.VillagerPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.villager.VillagerPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasBeast) {
            net.sam.samrequiemmod.possession.beast.BeastPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.beast.BeastPossessionController.onUnpossessUuid(player.getUuid());
        }

        // Clean up piglin possession state
        if (wasPiglin) {
            net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasBabyPiglin) {
            net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasPiglinBrute) {
            net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasZombifiedPiglin) {
            net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.onUnpossessUuid(player.getUuid());
        }
        if (wasBabyZombifiedPiglin) {
            net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.onUnpossess(player);
        } else {
            net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.onUnpossessUuid(player.getUuid());
        }
        // Clear baby piglin states on unpossess
        net.sam.samrequiemmod.possession.piglin.BabyPiglinState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.piglin.BabyPiglinNetworking.broadcast(player, false);
        net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinNetworking.broadcast(player, false);
        net.sam.samrequiemmod.possession.piglin.OverworldConversionTracker.reset(player.getUuid());
        net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.setServerBaby(player.getUuid(), false);
        net.sam.samrequiemmod.possession.hoglin.BabyHoglinNetworking.broadcast(player, false);
        net.sam.samrequiemmod.possession.hoglin.HoglinConversionTracker.reset(player.getUuid());

        // Grant immunity window FIRST — this makes MobEntityTargetMixin
        // block any re-targeting before we clear the provoked mobs below
        SamuelRequiemMod.POST_POSSESSION_IMMUNITY.put(player.getUuid(), (long) player.age + 100L);

        // Clear all mobs this player provoked during possession, and wipe their anger memory
        java.util.Set<java.util.UUID> provokedMobs =
                net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.clearPlayerProvoked(player.getUuid());
        if (!provokedMobs.isEmpty() && player.getServerWorld() != null) {
            net.minecraft.util.math.Box box = player.getBoundingBox().expand(128.0);
            for (net.minecraft.entity.mob.MobEntity mob : player.getServerWorld()
                    .getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box, m -> m.isAlive())) {
                if (!provokedMobs.contains(mob.getUuid())) continue;
                net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.clearProvoked(mob.getUuid());
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob.getAttacker() == player) mob.setAttacker(null);
                if (mob instanceof net.minecraft.entity.mob.Angerable angerable
                        && player.getUuid().equals(angerable.getAngryAt())) {
                    angerable.setAngryAt(null);
                    angerable.setAngerTime(0);
                }
                if (mob instanceof net.minecraft.entity.passive.IronGolemEntity golem)
                    golem.setAttacking(false);
                // Register this mob to keep forgetting the player for 30 seconds
                net.sam.samrequiemmod.possession.zombie.ZombieForgetPlayerState.registerForget(
                        mob.getUuid(), player.getUuid(), player.age, 600);
            }
        }

        SamuelRequiemMod.LOGGER.info(
                "[Possession] {} cleared possession",
                player.getGameProfile().getName()
        );
    }

    public static void removePlayer(ServerPlayerEntity player) {
        PLAYER_POSSESSIONS.remove(player.getUuid());
        LAST_DIMENSION_TYPE.remove(player.getUuid());
        PRE_POSSESSION_STATE.remove(player.getUuid());
    }

    @Nullable
    public static EntityType<?> getLastDimensionType(PlayerEntity player) {
        return LAST_DIMENSION_TYPE.get(player.getUuid());
    }

    public static void setLastDimensionType(PlayerEntity player, @Nullable EntityType<?> type) {
        if (type == null) {
            LAST_DIMENSION_TYPE.remove(player.getUuid());
        } else {
            LAST_DIMENSION_TYPE.put(player.getUuid(), type);
        }
    }
}
