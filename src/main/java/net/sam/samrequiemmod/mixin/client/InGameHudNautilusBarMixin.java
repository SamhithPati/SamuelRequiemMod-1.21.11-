package net.sam.samrequiemmod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.sam.samrequiemmod.client.NautilusHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public abstract class InGameHudNautilusBarMixin {

    @Inject(method = "shouldShowExperienceBar", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$hideExperienceBarForNautilus(CallbackInfoReturnable<Boolean> cir) {
        if (NautilusHudRenderer.shouldHideExperienceBar(MinecraftClient.getInstance())) {
            cir.setReturnValue(false);
        }
    }
}
