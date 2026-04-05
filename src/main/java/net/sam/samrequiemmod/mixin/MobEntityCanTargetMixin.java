package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PolarBearEntity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes possessed players invisible to mob targeting AI entirely.
 * LivingEntity.canTarget(LivingEntity) is called by NearestAttackableTargetGoal
 * when scanning for targets — returning false means the goal never proposes the
 * player as a candidate, so villager-chasing goals win uncontested.
 */
@Mixin(LivingEntity.class)
public abstract class MobEntityCanTargetMixin {

    @Inject(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$ignoreZombiePlayer(
            LivingEntity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(target instanceof ServerPlayerEntity serverPlayer)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof WardenEntity) {
            if (net.sam.samrequiemmod.possession.warden.WardenPossessionController.isWardenPossessing(serverPlayer)) {
                cir.setReturnValue(false);
            }
            return;
        }
        if (self instanceof ZoglinEntity) {
            if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(serverPlayer)) {
                cir.setReturnValue(false);
            }
            return;
        }

        boolean witherShouldIgnore = false;
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
            witherShouldIgnore = isZombieType || isSkeletonType || isZoglinType || isZombifiedPiglinType;
            if (!witherShouldIgnore) return;
        }

        // Golems and provoked mobs can still target the possessed player
        // But iron golems should NOT target iron-golem-possessed players (they're allies)
        if (self instanceof IronGolemEntity) {
            boolean alliedToGolem =
                    net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(serverPlayer)
                            || net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(serverPlayer);
            if (!alliedToGolem && !net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(serverPlayer))
                return;
        }
        if (self instanceof SnowGolemEntity
                && !net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(serverPlayer)) return;
        if (self instanceof net.minecraft.entity.mob.MobEntity mob
                && ZombieTargetingState.isProvoked(mob.getUuid())) return;
        if (self instanceof PolarBearEntity
                && PossessionManager.isPossessing(serverPlayer)
                && !net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(serverPlayer)) {
            cir.setReturnValue(false);
            return;
        }

        boolean isZombiePossessed = ZombiePossessionController.isZombiePossessing(serverPlayer)
                || BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer)
                || HuskPossessionController.isHuskPossessing(serverPlayer)
                || BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer)
                || DrownedPossessionController.isDrownedPossessing(serverPlayer)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(serverPlayer);

        // Passive mob possession — hostile mobs don't attack
        boolean isPassiveMobPossessed =
                net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.isPassiveMobPossessing(serverPlayer);

        boolean isPandaPossessed =
                net.sam.samrequiemmod.possession.passive.PandaPossessionController.isPandaPossessing(serverPlayer);
        boolean pandaBlocked = false;
        if (isPandaPossessed) {
            pandaBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(self.getUuid());
        }

        // Also block targeting if player is in post-possession immunity window
        boolean isImmune = net.sam.samrequiemmod.SamuelRequiemMod.POST_POSSESSION_IMMUNITY
                .containsKey(serverPlayer.getUuid());

        // Block mobs from targeting pillager/vindicator/evoker/ravager-possessed player unless provoked
        boolean isIllagerPossessed =
                net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(serverPlayer);
        net.minecraft.entity.LivingEntity selfEntity = (net.minecraft.entity.LivingEntity)(Object)this;
        boolean isIllagerAlly = isIllagerPossessed &&
                (net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isIllagerAlly(selfEntity)
                        || net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerAlly(selfEntity));
        boolean illagerBlocked = isIllagerPossessed && !isIllagerAlly &&
                !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());

        // Iron golem possession: creepers, piglins, piglin brutes, blazes don't attack unless provoked
        boolean isIronGolemPossessed =
                net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(serverPlayer);
        boolean ironGolemBlocked = false;
        if (isIronGolemPossessed) {
            boolean isIronGolemFriendly = self.getType() == EntityType.CREAKING
                    || self instanceof net.minecraft.entity.mob.CreeperEntity
                    || self instanceof net.minecraft.entity.mob.PiglinEntity
                    || self instanceof net.minecraft.entity.mob.PiglinBruteEntity
                    || self instanceof net.minecraft.entity.mob.BlazeEntity
                    || self instanceof WitchEntity
                    || self instanceof IronGolemEntity;
            ironGolemBlocked = isIronGolemFriendly
                    && !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
        }

        // Skeleton possession: all hostiles ignore unless provoked; iron golems always attack
        boolean isSkeletonPossessed =
                net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(serverPlayer);
        boolean isParchedPossessed =
                net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isParchedPossessing(serverPlayer);
        boolean skeletonBlocked = false;
        if (isSkeletonPossessed) {
            boolean alwaysHostile = self instanceof IronGolemEntity
                    || (isParchedPossessed && (self instanceof ZoglinEntity
                    || self instanceof WardenEntity
                    || self instanceof WolfEntity));
            if (alwaysHostile) {
                skeletonBlocked = false;
            } else {
                skeletonBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Wither skeleton possession: all hostiles ignore unless provoked; iron golems + piglins always attack
        boolean isWitherSkeletonPossessed =
                net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(serverPlayer);
        boolean witherSkeletonBlocked = false;
        if (isWitherSkeletonPossessed) {
            boolean alwaysHostile = self instanceof IronGolemEntity
                    || self instanceof net.minecraft.entity.mob.PiglinEntity
                    || self instanceof net.minecraft.entity.mob.PiglinBruteEntity;
            if (alwaysHostile) {
                witherSkeletonBlocked = false;
            } else {
                witherSkeletonBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Enderman possession: all hostiles ignore unless provoked;
        // iron golems, snow golems, and endermites always attack
        boolean isEndermanPossessed =
                net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(serverPlayer);
        boolean endermanBlocked = false;
        if (isEndermanPossessed) {
            boolean alwaysHostile = self instanceof IronGolemEntity
                    || self instanceof SnowGolemEntity
                    || self instanceof net.minecraft.entity.mob.EndermiteEntity;
            if (alwaysHostile) {
                endermanBlocked = false;
            } else {
                endermanBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isWardenPossessed =
                net.sam.samrequiemmod.possession.warden.WardenPossessionController.isWardenPossessing(serverPlayer);
        boolean wardenBlocked = false;
        if (isWardenPossessed) {
            boolean alwaysHostile = self instanceof IronGolemEntity
                    || self instanceof SnowGolemEntity
                    || self instanceof ZoglinEntity;
            if (alwaysHostile) {
                wardenBlocked = false;
            } else {
                wardenBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isBreezePossessed =
                net.sam.samrequiemmod.possession.breeze.BreezePossessionController.isBreezePossessing(serverPlayer);
        boolean breezeBlocked = false;
        if (isBreezePossessed) {
            boolean alwaysHostile = self instanceof IronGolemEntity
                    || self instanceof WardenEntity
                    || self instanceof WitherEntity
                    || self instanceof ZoglinEntity;
            if (alwaysHostile) {
                breezeBlocked = false;
            } else if (self instanceof BreezeEntity) {
                breezeBlocked = true;
            } else {
                breezeBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Creeper possession: all hostiles ignore unless provoked
        boolean isCreeperPossessed =
                net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.isCreeperPossessing(serverPlayer);
        boolean creeperBlocked = false;
        if (isCreeperPossessed) {
            creeperBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
        }

        boolean isSpiderPossessed =
                net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(serverPlayer);
        boolean spiderBlocked = false;
        if (isSpiderPossessed) {
            if (self instanceof IronGolemEntity) {
                spiderBlocked = false;
            } else {
                spiderBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isHoglinTypePossessed =
                net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isAnyHoglinTypePossessing(serverPlayer);
        boolean hoglinBlocked = false;
        if (isHoglinTypePossessed) {
            if (self instanceof IronGolemEntity) {
                hoglinBlocked = false;
            } else if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isHoglinAlly(selfEntity)
                    || net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinAlly(selfEntity)) {
                hoglinBlocked = true;
            } else {
                hoglinBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isGuardianPossessed =
                net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(serverPlayer);
        boolean guardianBlocked = false;
        if (isGuardianPossessed) {
            if (self instanceof IronGolemEntity) {
                guardianBlocked = false;
            } else if (net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isGuardianAlly(selfEntity)) {
                guardianBlocked = true;
            } else {
                guardianBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isSilverfishPossessed =
                net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishPossessing(serverPlayer);
        boolean silverfishBlocked = false;
        if (isSilverfishPossessed) {
            if (self instanceof IronGolemEntity) {
                silverfishBlocked = false;
            } else if (net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishAlly(selfEntity)) {
                silverfishBlocked = true;
            } else {
                silverfishBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isBlazePossessed =
                net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(serverPlayer);
        boolean blazeBlocked = false;
        if (isBlazePossessed) {
            if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity) {
                blazeBlocked = false;
            } else if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazeAlly(selfEntity)) {
                blazeBlocked = true;
            } else {
                blazeBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isGhastPossessed =
                net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(serverPlayer);
        boolean ghastBlocked = false;
        if (isGhastPossessed) {
            ghastBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
        }

        boolean isWolfPossessed =
                net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(serverPlayer);
        boolean wolfBlocked = false;
        if (isWolfPossessed) {
            if (self instanceof net.minecraft.entity.mob.AbstractSkeletonEntity || self.getType() == net.minecraft.entity.EntityType.PARCHED) {
                wolfBlocked = true;
            } else if (net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfAlly(selfEntity)) {
                wolfBlocked = true;
            } else {
                wolfBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isFoxPossessed =
                net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(serverPlayer);
        boolean foxBlocked = false;
        if (isFoxPossessed) {
            boolean alwaysHostile = self instanceof net.minecraft.entity.passive.WolfEntity
                    || self instanceof net.minecraft.entity.passive.PolarBearEntity;
            foxBlocked = !alwaysHostile
                    && !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
        }

        boolean isFelinePossessed =
                net.sam.samrequiemmod.possession.feline.FelinePossessionController.isAnyFelinePossessing(serverPlayer);
        boolean felineBlocked = false;
        if (isFelinePossessed) {
            if (self instanceof net.minecraft.entity.mob.CreeperEntity) {
                felineBlocked = true;
            } else {
                felineBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isVexPossessed =
                net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexPossessing(serverPlayer);
        boolean vexBlocked = false;
        if (isVexPossessed) {
            if (self instanceof IronGolemEntity) {
                vexBlocked = false;
            } else if (net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexAlly(selfEntity)) {
                vexBlocked = true;
            } else {
                vexBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isBatPossessed =
                net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(serverPlayer);
        boolean batBlocked = isBatPossessed;

        boolean isVillagerPossessed =
                net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(serverPlayer);
        boolean villagerBlocked = false;
        if (isVillagerPossessed) {
            if (self instanceof IronGolemEntity) {
                villagerBlocked = true;
            } else if (net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerAlwaysHostile(selfEntity)) {
                villagerBlocked = false;
            } else {
                villagerBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        boolean isBeastPossessed =
                net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(
                        net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer));
        boolean beastBlocked = false;
        if (isBeastPossessed) {
            var type = net.sam.samrequiemmod.possession.PossessionManager.getPossessedType(serverPlayer);
            if (type == net.minecraft.entity.EntityType.ENDERMITE) {
                beastBlocked = !(self instanceof IronGolemEntity || self instanceof net.minecraft.entity.mob.EndermanEntity)
                        && !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            } else if (type == net.minecraft.entity.EntityType.GOAT || type == net.minecraft.entity.EntityType.POLAR_BEAR
                    || type == net.minecraft.entity.EntityType.SHULKER) {
                if ((type == net.minecraft.entity.EntityType.POLAR_BEAR
                        && net.sam.samrequiemmod.possession.beast.BeastPossessionController.isPolarBearAlly(selfEntity))
                        || (type == net.minecraft.entity.EntityType.SHULKER && self instanceof IronGolemEntity)) {
                    beastBlocked = type == net.minecraft.entity.EntityType.POLAR_BEAR;
                } else {
                    beastBlocked = !(type == net.minecraft.entity.EntityType.SHULKER && self instanceof IronGolemEntity)
                            && !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
                }
            } else if (type == net.minecraft.entity.EntityType.RABBIT) {
                boolean alwaysHostile = self instanceof net.minecraft.entity.passive.WolfEntity
                        || self instanceof net.minecraft.entity.passive.FoxEntity
                        || self instanceof net.minecraft.entity.passive.OcelotEntity;
                beastBlocked = !alwaysHostile;
            } else if (type == net.minecraft.entity.EntityType.TURTLE) {
                boolean baby = net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(serverPlayer);
                boolean babyHostile = self instanceof net.minecraft.entity.mob.ZombieEntity
                        || self instanceof net.minecraft.entity.mob.AbstractSkeletonEntity
                        || self instanceof net.minecraft.entity.passive.WolfEntity
                        || self instanceof net.minecraft.entity.passive.OcelotEntity
                        || self instanceof net.minecraft.entity.passive.FoxEntity;
                beastBlocked = !(baby && babyHostile);
            } else if (type == net.minecraft.entity.EntityType.STRIDER || type == net.minecraft.entity.EntityType.CAMEL
                    || type == net.minecraft.entity.EntityType.SNOW_GOLEM) {
                beastBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            } else if (type == net.minecraft.entity.EntityType.AXOLOTL) {
                boolean alwaysHostile = self instanceof net.minecraft.entity.mob.GuardianEntity;
                beastBlocked = !alwaysHostile && !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            } else if (type == net.minecraft.entity.EntityType.BEE) {
                if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.isBeeAlly(selfEntity)) {
                    beastBlocked = true;
                } else {
                    beastBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
                }
            } else if (type == net.minecraft.entity.EntityType.PARROT) {
                beastBlocked = true;
            } else {
                beastBlocked = true;
            }
        }

        boolean isSlimePossessed =
                net.sam.samrequiemmod.possession.slime.SlimePossessionController.isAnySlimePossessing(serverPlayer);
        boolean slimeBlocked = false;
        if (isSlimePossessed) {
            if (self instanceof IronGolemEntity) {
                slimeBlocked = false;
            } else if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.isSlimeAlly(selfEntity)) {
                slimeBlocked = true;
            } else {
                slimeBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Aquatic mob possession: fish/squid/dolphin — hostile mobs don't attack unless provoked
        boolean isAquaticPossessed =
                net.sam.samrequiemmod.possession.aquatic.FishPossessionController.isFishPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.isDolphinPossessing(serverPlayer);
        boolean aquaticBlocked = false;
        if (isAquaticPossessed) {
            // Squid: guardians and elder guardians always attack
            if (net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(serverPlayer)
                    && (self instanceof net.minecraft.entity.mob.GuardianEntity)) {
                aquaticBlocked = false;
            } else {
                aquaticBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Piglin possession: wither skeletons always attack; piglins/brutes always passive; others blocked unless provoked
        boolean isPiglinPossessed =
                net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isAnyPiglinPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(serverPlayer);
        boolean piglinBlocked = false;
        if (isPiglinPossessed) {
            // Wither skeletons always attack piglins
            if (self instanceof net.minecraft.entity.mob.WitherSkeletonEntity) {
                piglinBlocked = false;
            } else if (net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinAlly(selfEntity)) {
                // Piglins and brutes are always passive
                piglinBlocked = true;
            } else {
                piglinBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        // Zombified piglin possession: all hostiles ignore unless provoked; zombified piglins always passive
        boolean isZombifiedPiglinPossessed =
                net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(serverPlayer);
        boolean zombifiedPiglinBlocked = false;
        if (isZombifiedPiglinPossessed) {
            // Zombified piglins are always passive — never target the player
            if (self instanceof net.minecraft.entity.mob.ZombifiedPiglinEntity) {
                zombifiedPiglinBlocked = true;
            } else {
                zombifiedPiglinBlocked = !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());
            }
        }

        if (isZombiePossessed || isImmune || isIllagerAlly || illagerBlocked || isPassiveMobPossessed || pandaBlocked || ironGolemBlocked || skeletonBlocked || witherSkeletonBlocked || endermanBlocked || wardenBlocked || breezeBlocked || creeperBlocked || spiderBlocked || hoglinBlocked || guardianBlocked || silverfishBlocked || blazeBlocked || ghastBlocked || wolfBlocked || foxBlocked || felineBlocked || vexBlocked || batBlocked || villagerBlocked || beastBlocked || slimeBlocked || aquaticBlocked || piglinBlocked || zombifiedPiglinBlocked) {
            cir.setReturnValue(false);
        }
    }
}






