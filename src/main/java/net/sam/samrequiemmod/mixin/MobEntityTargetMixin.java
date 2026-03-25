package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController;
import net.sam.samrequiemmod.possession.drowned.DrownedPossessionController;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;
import net.sam.samrequiemmod.possession.zombie.BabyZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts MobEntity.setTarget(). Blocks mobs from targeting possessed players.
 *
 * Rules:
 *  - Slimes/magma cubes: ALWAYS blocked for any possession type, even if provoked.
 *  - Golems: NEVER blocked — always hostile to zombie-type players.
 *  - All other mobs: blocked for zombie/baby/husk possession unless provoked.
 */
@Mixin(MobEntity.class)
public abstract class MobEntityTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$blockZombiePlayerTargeting(
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (!(target instanceof ServerPlayerEntity serverPlayer)) return;

        MobEntity self = (MobEntity) (Object) this;
        if (self instanceof WardenEntity) return;

        if (self instanceof WitherEntity) {
            boolean isZombieType = ZombiePossessionController.isZombiePossessing(serverPlayer)
                    || BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer)
                    || HuskPossessionController.isHuskPossessing(serverPlayer)
                    || BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer)
                    || DrownedPossessionController.isDrownedPossessing(serverPlayer)
                    || BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer)
                    || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(serverPlayer)
                    || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(serverPlayer);
            boolean isSkeletonType = net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(serverPlayer)
                    || net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(serverPlayer);
            boolean isZoglinType = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(serverPlayer);
            boolean isZombifiedPiglinType = net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(serverPlayer);
            if (!(isZombieType || isSkeletonType || isZoglinType || isZombifiedPiglinType)) {
                return;
            }
        }

        // Slimes and magma cubes are always passive toward ANY possessed player,
        // regardless of provocation — they never retaliate even when hit.
        if (self instanceof SlimeEntity && PossessionManager.isPossessing(serverPlayer)) {
            ci.cancel();
            return;
        }

        // Also block all targeting during post-possession immunity window (no golem exception)
        if (net.sam.samrequiemmod.SamuelRequiemMod.POST_POSSESSION_IMMUNITY.containsKey(serverPlayer.getUuid())) {
            ci.cancel();
            return;
        }

        // Pillager/Vindicator possession: illager allies never target, non-allies blocked unless provoked
        // Golems are always allowed to target these players
        boolean isIllagerPossessed =
                net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(serverPlayer);
        if (isIllagerPossessed) {
            if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity) return;
            if (net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isIllagerAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        // Enderman possession: block all targeting except golems, endermites, and provoked mobs
        boolean isEndermanPossessed =
                net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(serverPlayer);
        if (isEndermanPossessed) {
            if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity
                    || self instanceof net.minecraft.entity.mob.EndermiteEntity) return;
            if (ZombieTargetingState.isProvoked(self.getUuid())) return;
            ci.cancel();
            return;
        }

        boolean isSpiderPossessed =
                net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(serverPlayer);
        if (isSpiderPossessed) {
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isHoglinTypePossessed =
                net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isAnyHoglinTypePossessing(serverPlayer);
        if (isHoglinTypePossessed) {
            if (self instanceof IronGolemEntity) return;
            if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isHoglinAlly(self)
                    || net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isGuardianPossessed =
                net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(serverPlayer);
        if (isGuardianPossessed) {
            if (self instanceof IronGolemEntity) return;
            if (net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isGuardianAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isSilverfishPossessed =
                net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishPossessing(serverPlayer);
        if (isSilverfishPossessed) {
            if (self instanceof IronGolemEntity) return;
            if (net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isBlazePossessed =
                net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(serverPlayer);
        if (isBlazePossessed) {
            if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity) return;
            if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazeAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isGhastPossessed =
                net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(serverPlayer);
        if (isGhastPossessed) {
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isSlimePossessed =
                net.sam.samrequiemmod.possession.slime.SlimePossessionController.isAnySlimePossessing(serverPlayer);
        if (isSlimePossessed) {
            if (self instanceof IronGolemEntity) return;
            if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.isSlimeAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isWolfPossessed =
                net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(serverPlayer);
        if (isWolfPossessed) {
            if (self instanceof net.minecraft.entity.mob.AbstractSkeletonEntity) {
                ci.cancel();
                return;
            }
            if (net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isFoxPossessed =
                net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(serverPlayer);
        if (isFoxPossessed) {
            if (self instanceof net.minecraft.entity.passive.WolfEntity
                    || self instanceof net.minecraft.entity.passive.PolarBearEntity) {
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isFelinePossessed =
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(serverPlayer);
        if (isFelinePossessed) {
            if (self instanceof net.minecraft.entity.mob.CreeperEntity) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isVexPossessed =
                net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexPossessing(serverPlayer);
        if (isVexPossessed) {
            if (self instanceof IronGolemEntity) return;
            if (net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isBatPossessed =
                net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(serverPlayer);
        if (isBatPossessed) {
            ci.cancel();
            return;
        }

        boolean isVillagerPossessed =
                net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(serverPlayer);
        if (isVillagerPossessed) {
            if (self instanceof IronGolemEntity) {
                ci.cancel();
                return;
            }
            if (net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerAlwaysHostile(self)) {
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        boolean isBeastPossessed =
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(
                        net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer));
        if (isBeastPossessed) {
            var type = net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer);
            if (type == net.minecraft.entity.EntityType.ENDERMITE) {
                if (self instanceof IronGolemEntity || self instanceof net.minecraft.entity.mob.EndermanEntity) return;
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.GOAT) {
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.POLAR_BEAR) {
                if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.isPolarBearAlly(self)) {
                    ci.cancel();
                    return;
                }
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.RABBIT) {
                if (!(self instanceof net.minecraft.entity.passive.WolfEntity
                        || self instanceof net.minecraft.entity.passive.FoxEntity
                        || self instanceof net.minecraft.entity.passive.OcelotEntity)) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.TURTLE) {
                boolean baby = net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(serverPlayer);
                boolean babyHostile = self instanceof net.minecraft.entity.mob.ZombieEntity
                        || self instanceof net.minecraft.entity.mob.AbstractSkeletonEntity
                        || self instanceof net.minecraft.entity.passive.WolfEntity
                        || self instanceof net.minecraft.entity.passive.OcelotEntity
                        || self instanceof net.minecraft.entity.passive.FoxEntity;
                if (!(baby && babyHostile)) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.STRIDER || type == net.minecraft.entity.EntityType.CAMEL
                    || type == net.minecraft.entity.EntityType.SNOW_GOLEM) {
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.AXOLOTL) {
                if (!(self instanceof net.minecraft.entity.mob.GuardianEntity) && !ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.BEE) {
                if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.isBeeAlly(self)) {
                    ci.cancel();
                    return;
                }
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.PARROT) {
                ci.cancel();
                return;
            }
            if (type == net.minecraft.entity.EntityType.SHULKER) {
                if (self instanceof IronGolemEntity) return;
                if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                    ci.cancel();
                    return;
                }
            }
            if (type == net.minecraft.entity.EntityType.HORSE || type == net.minecraft.entity.EntityType.MULE
                    || type == net.minecraft.entity.EntityType.ZOMBIE_HORSE || type == net.minecraft.entity.EntityType.SKELETON_HORSE) {
                ci.cancel();
                return;
            }
        }

        // Aquatic mob possession: block targeting unless provoked (squid: guardians always attack)
        boolean isAquaticPossessed =
                net.sam.samrequiemmod.possession.aquatic.FishPossessionController.isFishPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.isDolphinPossessing(serverPlayer);
        if (isAquaticPossessed) {
            // Squid: guardians and elder guardians always attack
            if (net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(serverPlayer)
                    && self instanceof net.minecraft.entity.mob.GuardianEntity) {
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        // Piglin possession: wither skeletons always attack, piglins/brutes always passive, others blocked unless provoked
        boolean isPiglinPossessed =
                net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isAnyPiglinPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(serverPlayer);
        if (isPiglinPossessed) {
            // Iron golems always attack piglins
            if (self instanceof IronGolemEntity) return;
            // Wither skeletons always attack piglins
            if (self instanceof net.minecraft.entity.mob.WitherSkeletonEntity) return;
            // Piglins/brutes are always passive — never target the player
            if (net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinAlly(self)) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        // Zombified piglin possession: zombified piglins always passive, others blocked unless provoked
        boolean isZombifiedPiglinPossessed =
                net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(serverPlayer);
        if (isZombifiedPiglinPossessed) {
            // Iron golems always attack zombified piglins
            if (self instanceof IronGolemEntity) return;
            // Zombified piglins are always passive — never target
            if (self instanceof net.minecraft.entity.mob.ZombifiedPiglinEntity) {
                ci.cancel();
                return;
            }
            if (!ZombieTargetingState.isProvoked(self.getUuid())) {
                ci.cancel();
                return;
            }
        }

        // For zombie-type possession: block all other mobs except golems and provoked mobs
        boolean isZombiePossessed = ZombiePossessionController.isZombiePossessing(serverPlayer)
                || BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer)
                || HuskPossessionController.isHuskPossessing(serverPlayer)
                || BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer)
                || DrownedPossessionController.isDrownedPossessing(serverPlayer)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(serverPlayer);
        if (!isZombiePossessed) return;

        // Golems are always allowed to target us
        if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity) return;

        // Provoked mobs (have hit the zombie player) keep their target
        if (ZombieTargetingState.isProvoked(self.getUuid())) return;

        // If the mob already has a non-player target (e.g. chasing a villager),
        // don't cancel — let it keep that target. We only block targeting US.
        LivingEntity currentTarget = self.getTarget();
        if (currentTarget != null && !(currentTarget instanceof ServerPlayerEntity)) return;

        ci.cancel();
    }
}
