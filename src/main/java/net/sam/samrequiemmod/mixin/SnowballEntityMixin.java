package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.sam.samrequiemmod.possession.firemob.BlazePossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SnowballEntity.class)
public abstract class SnowballEntityMixin {

    @Inject(method = "onEntityHit", at = @At("TAIL"))
    private void samrequiemmod$damageBlazePossessedPlayer(EntityHitResult entityHitResult, CallbackInfo ci) {
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (!BlazePossessionController.isBlazePossessing(player)) return;

        player.damage(player.getDamageSources().freeze(), 4.0f);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
