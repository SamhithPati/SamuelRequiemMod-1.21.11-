package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.illager.PillagerPossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes villagers flee from pillager-possessed players.
 *
 * VillagerEntity in MC 1.21 does not override initGoals() — it registers
 * goals directly in its constructor. We inject at the tail of the constructor
 * so our goal is added after all vanilla goals are registered.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("TAIL"))
    private void samrequiemmod$addFleeFromPillagerPlayerGoal(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;

        self.goalSelector.add(3, new FleeEntityGoal<>(
                self,
                PlayerEntity.class,
                8.0f,
                0.8,
                1.2,
                entity -> entity instanceof PlayerEntity player
                        && (PillagerPossessionController.isPillagerPossessing(player)
                        || net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player))
        ));
    }
}





