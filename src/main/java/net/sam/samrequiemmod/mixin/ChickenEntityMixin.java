package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.zombie.ChickenRidingHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ChickenEntityMixin {

    // ── 1. Allow player to be a passenger ────────────────────────────────────
    @Mixin(Entity.class)
    public abstract static class CanAddPassengerMixin {
        @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$allowRider(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof ChickenEntity)) return;
            if (!(passenger instanceof PlayerEntity p)) return;
            if (!ChickenRidingHandler.isBabyUndead(p)) return;
            if (!self.hasPassengers()) cir.setReturnValue(true);
        }
    }

    // ── 2. Right-click to mount ───────────────────────────────────────────────
    @Mixin(MobEntity.class)
    public abstract static class InteractMixin {
        @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$mount(PlayerEntity player, Hand hand,
                                         CallbackInfoReturnable<ActionResult> cir) {
            if (player.getWorld().isClient) return;
            MobEntity self = (MobEntity)(Object)this;
            if (!(self instanceof ChickenEntity chicken)) return;
            if (!ChickenRidingHandler.isBabyUndead(player)) return;
            if (player.getVehicle() == chicken) return;
            if (player.hasVehicle()) player.stopRiding();
            player.startRiding(chicken, true);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    // ── 3. Make player the controlling passenger (declared on Entity) ───
    @Mixin(Entity.class)
    public abstract static class ControllingPassengerMixin {
        @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$controlling(CallbackInfoReturnable<Entity> cir) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof ChickenEntity)) return;
            Entity first = self.getFirstPassenger();
            if (first instanceof PlayerEntity p && ChickenRidingHandler.isBabyUndead(p)) {
                cir.setReturnValue(first);
            }
        }
    }

    // ── 4. Passenger height offset (declared on Entity) ───────────────────────
    @Mixin(Entity.class)
    public abstract static class MountedHeightMixin {
        @Inject(method = "getPassengerRidingPos", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$height(Entity passenger, CallbackInfoReturnable<Vec3d> cir) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof ChickenEntity chicken)) return;
            if (passenger instanceof PlayerEntity p && ChickenRidingHandler.isBabyUndead(p)) {
                float yOffset = chicken.getHeight() * 0.6f;
                cir.setReturnValue(self.getPos().add(0.0, yOffset, 0.0));
            }
        }
    }

    // ── 5. Tick: sneak to dismount (declared on Entity) ───────────────────────
    @Mixin(Entity.class)
    public abstract static class TickMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void samrequiemmod$sneakDismount(CallbackInfo ci) {
            Entity self = (Entity)(Object)this;
            if (!(self instanceof ChickenEntity chicken)) return;
            if (self.getWorld().isClient) return;
            Entity first = chicken.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!ChickenRidingHandler.isBabyUndead(rider)) return;
            if (rider.isSneaking()) rider.stopRiding();
        }
    }

    // ── 6. Override travel to steer chicken with player input ─────────────────
    // travel() is declared on LivingEntity
    @Mixin(LivingEntity.class)
    public abstract static class TravelMixin {
        @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
        private void samrequiemmod$travel(Vec3d input, CallbackInfo ci) {
            LivingEntity self = (LivingEntity)(Object)this;
            if (!(self instanceof ChickenEntity chicken)) return;
            if (self.getWorld().isClient) return;
            Entity first = chicken.getFirstPassenger();
            if (!(first instanceof ServerPlayerEntity rider)) return;
            if (!ChickenRidingHandler.isBabyUndead(rider)) return;
            if (rider.isSneaking()) return;

            // Steer with player's look direction and WASD
            float yaw = rider.getYaw();
            chicken.setYaw(yaw);
            chicken.headYaw = yaw;
            chicken.bodyYaw = yaw;

            // forwardSpeed/sidewaysSpeed are now populated because we return
            // this passenger from getControllingPassenger()
            float fwd    = rider.forwardSpeed;
            float strafe = rider.sidewaysSpeed;
            double speed = 0.2;
            double rad   = Math.toRadians(yaw);
            double dx    = (-Math.sin(rad) * fwd + Math.cos(rad) * strafe) * speed;
            double dz    = ( Math.cos(rad) * fwd + Math.sin(rad) * strafe) * speed;

            Vec3d vel = chicken.getVelocity();
            chicken.setVelocity(dx, vel.y, dz);
            chicken.velocityModified = true;

            // Let vanilla handle gravity/collision but skip the normal movement calc
            // by calling super-minimal: just apply velocity
            chicken.move(net.minecraft.entity.MovementType.SELF, chicken.getVelocity());
            chicken.setVelocity(chicken.getVelocity().multiply(0.91, 0.98, 0.91));
            ci.cancel();
        }
    }
}