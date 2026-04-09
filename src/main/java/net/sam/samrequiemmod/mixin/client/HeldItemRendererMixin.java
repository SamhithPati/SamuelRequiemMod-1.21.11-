package net.sam.samrequiemmod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.client.FirstPersonPossessionArmHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    private static final float SKELETON_ARM_WIDTH_SCALE = 0.64f;
    private static final float SKELETON_ARM_LENGTH_SCALE = 1.08f;
    private static final float WITHER_SKELETON_ARM_LENGTH_SCALE = 1.14f;
    private static final float ENDERMAN_ARM_WIDTH_SCALE = 0.70f;
    private static final float ENDERMAN_ARM_LENGTH_SCALE = 1.28f;
    private static final float IRON_GOLEM_ARM_WIDTH_SCALE = 1.18f;
    private static final float IRON_GOLEM_ARM_LENGTH_SCALE = 1.10f;
    private static final float WARDEN_ARM_WIDTH_SCALE = 1.22f;
    private static final float WARDEN_ARM_LENGTH_SCALE = 1.16f;

    @Shadow @Final private MinecraftClient client;

    @ModifyArg(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            ),
            index = 3
    )
    private Identifier samrequiemmod$replaceRightArmTextureInRenderArm(Identifier original) {
        return getReplacementTexture(original);
    }

    @ModifyArg(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            ),
            index = 3
    )
    private Identifier samrequiemmod$replaceLeftArmTextureInRenderArm(Identifier original) {
        return getReplacementTexture(original);
    }

    @ModifyArg(
            method = "renderArmHoldingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            ),
            index = 3
    )
    private Identifier samrequiemmod$replaceRightArmTextureInHeldItem(Identifier original) {
        return getReplacementTexture(original);
    }

    @ModifyArg(
            method = "renderArmHoldingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            ),
            index = 3
    )
    private Identifier samrequiemmod$replaceLeftArmTextureInHeldItem(Identifier original) {
        return getReplacementTexture(original);
    }

    @WrapOperation(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            )
    )
    private void samrequiemmod$thinRightArmInRenderArm(
            PlayerEntityRenderer<?> renderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier texture,
            boolean sleeveVisible,
            Operation<Void> original
    ) {
        renderArmWithOptionalSkeletonScale(renderer, matrices, queue, light, texture, sleeveVisible, original);
    }

    @WrapOperation(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            )
    )
    private void samrequiemmod$thinLeftArmInRenderArm(
            PlayerEntityRenderer<?> renderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier texture,
            boolean sleeveVisible,
            Operation<Void> original
    ) {
        renderArmWithOptionalSkeletonScale(renderer, matrices, queue, light, texture, sleeveVisible, original);
    }

    @WrapOperation(
            method = "renderArmHoldingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            )
    )
    private void samrequiemmod$thinRightArmInHeldItem(
            PlayerEntityRenderer<?> renderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier texture,
            boolean sleeveVisible,
            Operation<Void> original
    ) {
        renderArmWithOptionalSkeletonScale(renderer, matrices, queue, light, texture, sleeveVisible, original);
    }

    @WrapOperation(
            method = "renderArmHoldingItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"
            )
    )
    private void samrequiemmod$thinLeftArmInHeldItem(
            PlayerEntityRenderer<?> renderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier texture,
            boolean sleeveVisible,
            Operation<Void> original
    ) {
        renderArmWithOptionalSkeletonScale(renderer, matrices, queue, light, texture, sleeveVisible, original);
    }

    private Identifier getReplacementTexture(Identifier original) {
        AbstractClientPlayerEntity player = this.client.player;
        if (player == null) {
            return original;
        }

        return FirstPersonPossessionArmHelper.getArmTexture(player, original);
    }

    private void renderArmWithOptionalSkeletonScale(
            PlayerEntityRenderer<?> renderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier texture,
            boolean sleeveVisible,
            Operation<Void> original
    ) {
        AbstractClientPlayerEntity player = this.client.player;
        if (player != null && FirstPersonPossessionArmHelper.shouldHideArms(player)) {
            return;
        }

        if (player == null || !FirstPersonPossessionArmHelper.isSkeletonFamily(player)) {
            if (player != null && FirstPersonPossessionArmHelper.isEnderman(player)) {
                matrices.push();
                matrices.scale(ENDERMAN_ARM_WIDTH_SCALE, ENDERMAN_ARM_LENGTH_SCALE, ENDERMAN_ARM_WIDTH_SCALE);
                original.call(renderer, matrices, queue, light, texture, sleeveVisible);
                matrices.pop();
                return;
            }

            if (player != null && FirstPersonPossessionArmHelper.isIronGolem(player)) {
                matrices.push();
                matrices.scale(IRON_GOLEM_ARM_WIDTH_SCALE, IRON_GOLEM_ARM_LENGTH_SCALE, IRON_GOLEM_ARM_WIDTH_SCALE);
                original.call(renderer, matrices, queue, light, texture, sleeveVisible);
                matrices.pop();
                return;
            }

            if (player != null && FirstPersonPossessionArmHelper.isWarden(player)) {
                matrices.push();
                matrices.scale(WARDEN_ARM_WIDTH_SCALE, WARDEN_ARM_LENGTH_SCALE, WARDEN_ARM_WIDTH_SCALE);
                original.call(renderer, matrices, queue, light, texture, sleeveVisible);
                matrices.pop();
                return;
            }

            original.call(renderer, matrices, queue, light, texture, sleeveVisible);
            return;
        }

        float lengthScale = FirstPersonPossessionArmHelper.isWitherSkeleton(player)
                ? WITHER_SKELETON_ARM_LENGTH_SCALE
                : SKELETON_ARM_LENGTH_SCALE;

        matrices.push();
        matrices.scale(SKELETON_ARM_WIDTH_SCALE, lengthScale, SKELETON_ARM_WIDTH_SCALE);
        original.call(renderer, matrices, queue, light, texture, sleeveVisible);
        matrices.pop();
    }
}
