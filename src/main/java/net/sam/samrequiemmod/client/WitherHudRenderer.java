package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.wither.WitherNetworking;

import java.util.Optional;
import java.util.UUID;

public final class WitherHudRenderer {

    private static boolean wasAttackKeyDown = false;
    private static boolean wasUseKeyDown = false;
    private static int useTicks = 0;
    private static boolean explosionSent = false;

    private WitherHudRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                reset();
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.WITHER) {
                reset();
                return;
            }

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                ClientPlayNetworking.send(new WitherNetworking.AttackPayload(Optional.ofNullable(raycastTarget(client))));
            }
            wasAttackKeyDown = attackDown;

            boolean useDown = client.options.useKey.isPressed();
            if (useDown) {
                useTicks++;
                if (useTicks > 40 && !explosionSent) {
                    ClientPlayNetworking.send(new WitherNetworking.ExplosionPayload());
                    explosionSent = true;
                }
            } else {
                useTicks = 0;
                explosionSent = false;
            }
            wasUseKeyDown = useDown;
        });
    }

    private static void reset() {
        wasAttackKeyDown = false;
        wasUseKeyDown = false;
        useTicks = 0;
        explosionSent = false;
    }

    private static UUID raycastTarget(MinecraftClient client) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) client.crosshairTarget;
            if (hit.getEntity() instanceof LivingEntity living && living != client.player && living.isAlive()) {
                return living.getUuid();
            }
        }
        return null;
    }
}
