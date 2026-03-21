package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
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

        // For zombie-type possession: block all other mobs except golems and provoked mobs
        boolean isZombiePossessed = ZombiePossessionController.isZombiePossessing(serverPlayer)
                || BabyZombiePossessionController.isBabyZombiePossessing(serverPlayer)
                || HuskPossessionController.isHuskPossessing(serverPlayer)
                || BabyHuskPossessionController.isBabyHuskPossessing(serverPlayer)
                || DrownedPossessionController.isDrownedPossessing(serverPlayer)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(serverPlayer);
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