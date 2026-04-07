package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.MountedPlayerInputHelper;
import net.sam.samrequiemmod.possession.drowned.ZombieNautilusRidingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ZombieNautilusEntityMixin {

    @Mixin(Entity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowZombieNautilusRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieNautilusEntity)) return;
            if (!(passenger instanceof PlayerEntity player)) return;
            if (!ZombieNautilusRidingHandler.isDrownedType(player)) return;
            if (!self.hasPassengers()) cir.setReturnValue(true);
        }
    }

    @Mixin(Entity.class)
    public abstract static class ControllingPassengerMixin {
        @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$controllingZombieNautilus(CallbackInfoReturnable<Entity> cir) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieNautilusEntity)) return;
            Entity first = self.getFirstPassenger();
            if (first instanceof PlayerEntity player && ZombieNautilusRidingHandler.isDrownedType(player)) {
                cir.setReturnValue(first);
            }
        }
    }

    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V",
                at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$zombieNautilusHeight(Entity passenger, Entity.PositionUpdater positionUpdater,
                                                        CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieNautilusEntity zombieNautilus)) return;
            if (!(passenger instanceof PlayerEntity player) || !ZombieNautilusRidingHandler.isDrownedType(player)) return;

            double y = zombieNautilus.getY() + zombieNautilus.getHeight() * 0.38;
            positionUpdater.accept(passenger, zombieNautilus.getX(), y, zombieNautilus.getZ());
            if (passenger instanceof LivingEntity living) {
                living.bodyYaw = zombieNautilus.bodyYaw;
            }
            ci.cancel();
        }
    }

    @Mixin(Entity.class)
    public abstract static class TickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void samrequiemmod$zombieNautilusSneakDismount(CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof ZombieNautilusEntity)) return;
            if (self.getEntityWorld().isClient()) return;
            Entity first = self.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!ZombieNautilusRidingHandler.isDrownedType(rider)) return;
            if (rider.isSneaking()) rider.stopRiding();
        }
    }

    @Mixin(LivingEntity.class)
    public abstract static class TravelMixin {
        @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$zombieNautilusTravel(Vec3d input, CallbackInfo ci) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof ZombieNautilusEntity zombieNautilus)) return;
            if (self.getEntityWorld().isClient()) return;

            Entity first = zombieNautilus.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!ZombieNautilusRidingHandler.isDrownedType(rider)) return;
            if (rider.isSneaking()) return;

            float yaw = rider.getYaw();
            zombieNautilus.setYaw(yaw);
            zombieNautilus.headYaw = yaw;
            zombieNautilus.bodyYaw = yaw;

            float forwardInput = MountedPlayerInputHelper.getForwardInput(rider);
            float strafeInput = MountedPlayerInputHelper.getStrafeInput(rider);
            double yawRad = Math.toRadians(yaw);
            Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
            Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
            double verticalInput = 0.0;
            if (zombieNautilus.isTouchingWater() && Math.abs(forwardInput) > 0.01f) {
                verticalInput = net.minecraft.util.math.MathHelper.clamp(-rider.getPitch() / 45.0f, -0.6f, 0.6f) * Math.abs(forwardInput);
            }

            boolean inWater = zombieNautilus.isTouchingWater();
            double forwardSpeed = inWater ? 0.22 : 0.06;
            double strafeSpeed = inWater ? 0.12 : 0.03;
            double verticalSpeed = inWater ? 0.12 : 0.0;

            Vec3d desired = forward.multiply(forwardInput * forwardSpeed)
                    .add(right.multiply(strafeInput * strafeSpeed))
                    .add(0.0, verticalInput * verticalSpeed, 0.0);

            Vec3d velocity = zombieNautilus.getVelocity();
            if (desired.lengthSquared() > 0.0001) {
                desired = desired.normalize().multiply(inWater ? 0.32 : 0.10);
                velocity = new Vec3d(desired.x, desired.y, desired.z);
            } else {
                velocity = velocity.multiply(inWater ? 0.75 : 0.55, inWater ? 0.75 : 0.98, inWater ? 0.75 : 0.55);
            }

            if (!inWater) {
                velocity = new Vec3d(velocity.x, velocity.y - 0.08, velocity.z);
            }

            zombieNautilus.setVelocity(velocity);
            zombieNautilus.velocityDirty = true;
            zombieNautilus.move(MovementType.SELF, zombieNautilus.getVelocity());
            zombieNautilus.setVelocity(zombieNautilus.getVelocity().multiply(
                    inWater ? 0.88 : 0.60,
                    inWater ? 0.88 : 0.98,
                    inWater ? 0.88 : 0.60));
            ci.cancel();
        }
    }
}
