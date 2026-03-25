package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController;
import net.sam.samrequiemmod.possession.drowned.DrownedPossessionController;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;
import net.sam.samrequiemmod.possession.zombie.BabyZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;

/**
 * Makes endermen ignore any zombie-type-possessed player entirely.
 * EndermanEntity#isPlayerStaring() is called every tick to decide whether
 * to become angry — returning false prevents the anger animation and sound.
 */
@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin {

    @Inject(method = "isPlayerStaring", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$ignoreZombiePlayerStare(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ZombiePossessionController.isZombiePossessing(player)
                || BabyZombiePossessionController.isBabyZombiePossessing(player)
                || HuskPossessionController.isHuskPossessing(player)
                || BabyHuskPossessionController.isBabyHuskPossessing(player)
                || DrownedPossessionController.isDrownedPossessing(player)
                || BabyDrownedPossessionController.isBabyDrownedPossessing(player)
                || net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player)
                || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)
                || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player)
                || net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player)
                || net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(player)) {
            cir.setReturnValue(false);
        }
    }
}