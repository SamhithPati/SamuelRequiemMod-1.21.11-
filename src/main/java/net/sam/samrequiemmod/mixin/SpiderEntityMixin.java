package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.skeleton.SpiderRidingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class SpiderEntityMixin {

    // ── 1. Allow player to be a passenger ────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowSpiderRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            Entity self = (Entity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            if (!(passenger instanceof PlayerEntity p)) return;
            if (!SpiderRidingHelper.isSkeletonType(p)) return;
            if (!self.hasPassengers()) cir.setReturnValue(true);
        }
    }

    // ── 2. Right-click to mount ───────────────────────────────────────────────
    @Mixin(MobEntity.class)
    public abstract static class InteractMixin {
        @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$mountSpider(PlayerEntity player, Hand hand,
                                               CallbackInfoReturnable<ActionResult> cir) {
            if (player.getWorld().isClient) return;
            MobEntity self = (MobEntity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            if (!SpiderRidingHelper.isSkeletonType(player)) return;
            if (player.getVehicle() == self) return;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(self, true);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    // ── 3. Make player the controlling passenger ─────────────────────────────
    @Mixin(Entity.class)
    public abstract static class ControllingPassengerMixin {
        @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$controllingSpider(CallbackInfoReturnable<Entity> cir) {
            Entity self = (Entity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            Entity first = self.getFirstPassenger();
            if (first instanceof PlayerEntity p && SpiderRidingHelper.isSkeletonType(p)) {
                cir.setReturnValue(first);
            }
        }
    }

    // ── 4. Passenger height offset ───────────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
                at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$spiderHeight(Entity passenger, Entity.PositionUpdater positionUpdater, CallbackInfo ci) {
            Entity self = (Entity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            if (!(passenger instanceof PlayerEntity p) || !SpiderRidingHelper.isSkeletonType(p)) return;

            double y = self.getY() + self.getHeight() * 0.20;
            positionUpdater.accept(passenger, self.getX(), y, self.getZ());
            if (passenger instanceof LivingEntity living) {
                living.bodyYaw = self.getYaw();
            }
            ci.cancel();
        }
    }

    // ── 5. Tick: sneak to dismount ───────────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class TickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void samrequiemmod$spiderSneakDismount(CallbackInfo ci) {
            Entity self = (Entity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            if (self.getWorld().isClient) return;
            Entity first = self.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!SpiderRidingHelper.isSkeletonType(rider)) return;
            if (rider.isSneaking()) rider.stopRiding();
        }
    }

    // ── 6. Override travel to steer spider with player input ──────────────────
    @Mixin(LivingEntity.class)
    public abstract static class TravelMixin {
        @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$spiderTravel(Vec3d input, CallbackInfo ci) {
            LivingEntity self = (LivingEntity)(Object)this;
            if (!SpiderRidingHelper.isSpiderType(self)) return;
            if (self.getWorld().isClient) return;
            Entity first = self.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!SpiderRidingHelper.isSkeletonType(rider)) return;
            if (rider.isSneaking()) return;

            // Steer with player's look direction and WASD
            float yaw = rider.getYaw();
            self.setYaw(yaw);
            self.headYaw = yaw;
            self.bodyYaw = yaw;

            float fwd    = rider.forwardSpeed;
            float strafe = rider.sidewaysSpeed;
            double speed = 0.35; // spider speed
            double rad   = Math.toRadians(yaw);
            double dx    = (-Math.sin(rad) * fwd + Math.cos(rad) * strafe) * speed;
            double dz    = ( Math.cos(rad) * fwd + Math.sin(rad) * strafe) * speed;

            Vec3d vel = self.getVelocity();
            double vy = vel.y;

            // Wall climbing: if the spider is colliding horizontally and the rider
            // is pressing forward, climb the wall like a regular spider.
            boolean isClimbing = self.horizontalCollision && (fwd > 0 || strafe != 0);
            if (isClimbing) {
                vy = 0.2; // climb speed
            } else {
                // Apply gravity
                vy -= 0.08;
            }

            // Jump when rider presses space and spider is on the ground
            if (net.sam.samrequiemmod.possession.skeleton.SpiderJumpNetworking
                    .JUMP_REQUESTED.contains(rider.getUuid()) && self.isOnGround()) {
                vy = 0.42;
            }

            self.setVelocity(dx, vy, dz);
            self.velocityModified = true;

            self.move(net.minecraft.entity.MovementType.SELF, self.getVelocity());
            self.setVelocity(self.getVelocity().multiply(0.91, isClimbing ? 1.0 : 0.98, 0.91));
            ci.cancel();
        }
    }
}
