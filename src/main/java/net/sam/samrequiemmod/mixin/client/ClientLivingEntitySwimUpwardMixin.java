package net.sam.samrequiemmod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntitySwimUpwardMixin {

    @Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$blockClientNautilusSwimUpward(TagKey<Fluid> fluidTag, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != player) return;

        EntityType<?> type = ClientPossessionState.get(player);
        if (type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS) {
            ci.cancel();
        }
    }
}
