package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.possession.zombie.ZombieHorseRidingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ZombieHorseEntityMixin {

    @Mixin(Entity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowZombieHorseRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieHorseEntity)) return;
            if (!(passenger instanceof PlayerEntity player)) return;
            if (!ZombieHorseRidingHandler.isZombieType(player)) return;
            if (!self.hasPassengers()) cir.setReturnValue(true);
        }
    }

    @Mixin(AbstractHorseEntity.class)
    public abstract static class InteractMixin {
        @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$mountZombieHorse(PlayerEntity player, Hand hand,
                                                    CallbackInfoReturnable<ActionResult> cir) {
            if (player.getEntityWorld().isClient()) return;
            AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;
            if (!(self instanceof ZombieHorseEntity zombieHorse)) return;
            if (!ZombieHorseRidingHandler.isZombieType(player)) return;
            if (player.getVehicle() == zombieHorse) return;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(zombieHorse, true, false);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
                at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$zombieHorseHeight(Entity passenger, Entity.PositionUpdater positionUpdater,
                                                     org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieHorseEntity horse)) return;
            if (!(passenger instanceof PlayerEntity player) || !ZombieHorseRidingHandler.isZombieType(player)) return;

            double y = horse.getY() + horse.getHeight() * 0.42;
            positionUpdater.accept(passenger, horse.getX(), y, horse.getZ());
            if (passenger instanceof LivingEntity living) {
                living.bodyYaw = horse.bodyYaw;
            }
            ci.cancel();
        }
    }
}
