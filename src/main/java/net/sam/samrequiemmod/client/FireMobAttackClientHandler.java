package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.firemob.FireMobNetworking;

import java.util.Optional;
import java.util.UUID;

public final class FireMobAttackClientHandler {

    private static boolean wasAttackKeyDown = false;

    private FireMobAttackClientHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                return;
            }
            EntityType<?> type = ClientPossessionState.get(client.player);
            if (type != EntityType.BLAZE && type != EntityType.GHAST) {
                wasAttackKeyDown = false;
                return;
            }

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                UUID targetUuid = raycastTarget(client);
                ClientPlayNetworking.send(new FireMobNetworking.AttackRequestPayload(Optional.ofNullable(targetUuid)));
            }
            wasAttackKeyDown = attackDown;
        });
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






