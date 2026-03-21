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
 * Makes iron golems actively hunt pillager-possessed players.
 * targetSelector is exposed via the access widener so no @Shadow needed.
 */
@Mixin(IronGolemEntity.class)
public abstract class IronGolemEntityMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void samrequiemmod$addTargetPillagerPlayerGoal(CallbackInfo ci) {
        IronGolemEntity self = (IronGolemEntity) (Object) this;

        self.targetSelector.add(3, new ActiveTargetGoal<>(
                self,
                PlayerEntity.class,
                10,
                true,
                false,
                entity -> entity instanceof PlayerEntity player
                        && (PillagerPossessionController.isPillagerPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player))
        ));
    }
}