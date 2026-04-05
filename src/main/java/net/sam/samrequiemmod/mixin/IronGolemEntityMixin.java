package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.illager.PillagerPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes iron golems actively hunt possession types that should stay hostile to them.
 * targetSelector is exposed via the access widener so no @Shadow needed.
 */
@Mixin(IronGolemEntity.class)
public abstract class IronGolemEntityMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void samrequiemmod$addTargetPillagerPlayerGoal(CallbackInfo ci) {
        IronGolemEntity self = (IronGolemEntity) (Object) this;

        self.targetSelector.add(3, new ActiveTargetGoal<PlayerEntity>(
                self,
                PlayerEntity.class,
                10,
                true,
                false,
                (target, world) -> target instanceof PlayerEntity player
                        && !net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player)
                        && (PillagerPossessionController.isPillagerPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player)
                        || net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(player)
                        || net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isAnyPiglinPossessing(player)
                        || net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(player)
                        || net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player)
                        || net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(player)
                        || net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(player))
                        
        ));
    }
}





