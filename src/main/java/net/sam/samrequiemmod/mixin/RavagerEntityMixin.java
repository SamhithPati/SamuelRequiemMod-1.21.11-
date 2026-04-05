package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.illager.RavagerRidingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class RavagerEntityMixin {

    // ── 1. Allow player to be a passenger ────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowRavagerRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof RavagerEntity)) return;
            if (!(passenger instanceof PlayerEntity p)) return;
            if (!RavagerRidingHandler.isIllagerPossessed(p)) return;
            if (!self.hasPassengers()) cir.setReturnValue(true);
        }
    }

    // ── 2. Right-click to mount ───────────────────────────────────────────────
    @Mixin(MobEntity.class)
    public abstract static class InteractMixin {
        @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$mountRavager(PlayerEntity player, Hand hand,
                                                CallbackInfoReturnable<ActionResult> cir) {
            if (player.getEntityWorld().isClient()) return;
            MobEntity self = (MobEntity)(Object)this;
            if (!(self instanceof RavagerEntity ravager)) return;
            if (!RavagerRidingHandler.isIllagerPossessed(player)) return;
            if (player.getVehicle() == ravager) return;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(ravager, true, false);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    // ── 3. Make player the controlling passenger ─────────────────────────────
    @Mixin(Entity.class)
    public abstract static class ControllingPassengerMixin {
        @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$controllingRavager(CallbackInfoReturnable<Entity> cir) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof RavagerEntity)) return;
            Entity first = self.getFirstPassenger();
            if (first instanceof PlayerEntity p && RavagerRidingHandler.isIllagerPossessed(p)) {
                cir.setReturnValue(first);
            }
        }
    }

    // ── 4. Passenger height offset ───────────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
                at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$ravagerHeight(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof RavagerEntity ravager)) return;
            if (!(passenger instanceof PlayerEntity p) || !RavagerRidingHandler.isIllagerPossessed(p)) return;

            double y = ravager.getY() + ravager.getHeight() * 0.75;
            positionUpdater.accept(passenger, ravager.getX(), y, ravager.getZ());
            if (passenger instanceof LivingEntity living) {
                living.bodyYaw = ravager.bodyYaw;
            }
            ci.cancel();
        }
    }

    // ── 5. Tick: sneak to dismount ───────────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class TickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void samrequiemmod$ravagerSneakDismount(CallbackInfo ci) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof RavagerEntity)) return;
            if (self.getEntityWorld().isClient()) return;
            Entity first = self.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!RavagerRidingHandler.isIllagerPossessed(rider)) return;
            if (rider.isSneaking()) rider.stopRiding();
        }
    }

    // ── 6. Override travel to steer ravager with player input ─────────────────
    @Mixin(LivingEntity.class)
    public abstract static class TravelMixin {
        @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$ravagerTravel(Vec3d input, CallbackInfo ci) {
            LivingEntity self = (LivingEntity)(Object)this;
            if (!(self instanceof RavagerEntity ravager)) return;
            if (self.getEntityWorld().isClient()) return;
            Entity first = ravager.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!RavagerRidingHandler.isIllagerPossessed(rider)) return;
            if (rider.isSneaking()) return;

            // Steer with player's look direction and WASD
            float yaw = rider.getYaw();
            ravager.setYaw(yaw);
            ravager.headYaw = yaw;
            ravager.bodyYaw = yaw;

            float fwd = net.sam.samrequiemmod.possession.MountedPlayerInputHelper.getForwardInput(rider);
            float strafe = net.sam.samrequiemmod.possession.MountedPlayerInputHelper.getStrafeInput(rider);
            double speed = 0.45;
            double rad   = Math.toRadians(yaw);
            double dx    = (-Math.sin(rad) * fwd + Math.cos(rad) * strafe) * speed;
            double dz    = ( Math.cos(rad) * fwd + Math.sin(rad) * strafe) * speed;

            Vec3d vel = ravager.getVelocity();
            double vy = vel.y;

            // Apply gravity
            vy -= 0.08;

            // Jump when rider presses space and ravager is on the ground
            if (net.sam.samrequiemmod.possession.illager.RavagerJumpNetworking
                    .JUMP_REQUESTED.contains(rider.getUuid()) && ravager.isOnGround()) {
                vy = 0.42;
            }

            ravager.setVelocity(dx, vy, dz);
            ravager.velocityDirty = true;

            ravager.move(net.minecraft.entity.MovementType.SELF, ravager.getVelocity());
            ravager.setVelocity(ravager.getVelocity().multiply(0.91, 0.98, 0.91));
            ci.cancel();
        }
    }
}






