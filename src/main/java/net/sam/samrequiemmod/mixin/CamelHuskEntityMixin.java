package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.possession.MountedPlayerInputHelper;
import net.sam.samrequiemmod.possession.camel_husk.CamelHuskRidingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class CamelHuskEntityMixin {

    @Mixin(CamelEntity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowCamelHuskRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            CamelEntity self = (CamelEntity) (Object) this;
            if (!CamelHuskRidingHandler.isCamelHusk(self.getType())) return;
            if (!(passenger instanceof PlayerEntity player)) return;
            if (!CamelHuskRidingHandler.isAllowedRider(player)) return;
            if (!self.hasPassengers()) {
                cir.setReturnValue(true);
            }
        }
    }

    @Mixin(CamelEntity.class)
    public abstract static class InteractMixin {
        @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$mountCamelHusk(PlayerEntity player, Hand hand,
                                                  CallbackInfoReturnable<ActionResult> cir) {
            if (player.getEntityWorld().isClient()) return;
            CamelEntity self = (CamelEntity) (Object) this;
            if (!CamelHuskRidingHandler.isCamelHusk(self.getType())) return;
            if (!CamelHuskRidingHandler.isAllowedRider(player)) return;
            if (player.getVehicle() == self) return;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(self, true, false);
            self.setStanding();
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Mixin(AbstractHorseEntity.class)
    public abstract static class ControllingPassengerMixin {
        @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$controllingCamelHusk(CallbackInfoReturnable<net.minecraft.entity.LivingEntity> cir) {
            AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;
            if (!(self instanceof CamelEntity camel)) return;
            if (!CamelHuskRidingHandler.isCamelHusk(camel.getType())) return;
            Entity first = camel.getFirstPassenger();
            if (first instanceof PlayerEntity player && CamelHuskRidingHandler.isAllowedRider(player)) {
                cir.setReturnValue(player);
            }
        }
    }

    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
                at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$camelHuskHeight(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (!CamelHuskRidingHandler.isCamelHusk(self.getType())) return;
            if (!(passenger instanceof PlayerEntity player) || !CamelHuskRidingHandler.isAllowedRider(player)) return;

            double y = self.getY() + self.getHeight() * 0.54;
            positionUpdater.accept(passenger, self.getX(), y, self.getZ());
            if (passenger instanceof LivingEntity living) {
                living.bodyYaw = self.getYaw();
            }
            ci.cancel();
        }
    }

    @Mixin(CamelEntity.class)
    public abstract static class TickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void samrequiemmod$camelHuskMountedTick(CallbackInfo ci) {
            CamelEntity self = (CamelEntity) (Object) this;
            if (!CamelHuskRidingHandler.isCamelHusk(self.getType())) return;

            Entity first = self.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!CamelHuskRidingHandler.isAllowedRider(rider)) return;

            if (rider.isSneaking()) {
                rider.stopRiding();
                return;
            }

            boolean movingInput = MountedPlayerInputHelper.getForwardInput(rider) != 0.0f
                    || MountedPlayerInputHelper.getStrafeInput(rider) != 0.0f;
            boolean jumpInput = MountedPlayerInputHelper.isJumping(rider);

            if ((movingInput || jumpInput) && self.isSitting() && !self.isChangingPose()) {
                self.startStanding();
            }

            if ((movingInput || jumpInput) && !self.isChangingPose()) {
                self.setStanding();
            }

            CamelHuskRidingHandler.tryCamelHuskDash(self, rider);
        }
    }
}
