package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
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

        // Golems and provoked mobs can still target the possessed player
        if (self instanceof IronGolemEntity || self instanceof SnowGolemEntity) return;
        if (self instanceof net.minecraft.entity.mob.MobEntity mob
                && ZombieTargetingState.isProvoked(mob.getUuid())) return;

        boolean isZombiePossessed = ZombiePossessionController.isZombiePossessing(serverPlayer)
                || BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer)
                || HuskPossessionController.isHuskPossessing(serverPlayer)
                || BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer)
                || DrownedPossessionController.isDrownedPossessing(serverPlayer)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.isZombieVillagerPossessing(serverPlayer)
                || net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(serverPlayer);

        // Also block targeting if player is in post-possession immunity window
        boolean isImmune = net.sam.samrequiemmod.SamuelRequiemMod.POST_POSSESSION_IMMUNITY
                .containsKey(serverPlayer.getUuid());

        // Block mobs from targeting pillager/vindicator-possessed player unless provoked
        boolean isIllagerPossessed =
                net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(serverPlayer)
                        || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(serverPlayer);
        net.minecraft.entity.LivingEntity selfEntity = (net.minecraft.entity.LivingEntity)(Object)this;
        boolean isIllagerAlly = isIllagerPossessed &&
                net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isIllagerAlly(selfEntity);
        boolean illagerBlocked = isIllagerPossessed && !isIllagerAlly &&
                !net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.isProvoked(selfEntity.getUuid());

        if (isZombiePossessed || isImmune || isIllagerAlly || illagerBlocked) {
            cir.setReturnValue(false);
        }
    }
}