package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.breeze.BreezeClientState;
import net.sam.samrequiemmod.possession.breeze.BreezeNetworking;

import java.util.Optional;

public final class BreezeHudRenderer {

    private static boolean wasAttackKeyDown = false;

    private BreezeHudRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.BREEZE) {
                wasAttackKeyDown = false;
                return;
            }

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                LivingEntity target = GuardianHudRenderer.raycastTarget(client, 20.0);
                if (target != null) {
                    BreezeClientState.startShoot(client.player.getUuid(), client.player.age);
                    ClientPlayNetworking.send(new BreezeNetworking.AttackPayload(Optional.of(target.getUuid())));
                }
            }
            wasAttackKeyDown = attackDown;

            boolean jumpDown = client.options.jumpKey.isPressed();
            if (jumpDown && client.player.isOnGround()) {
                var jumpVelocity = BreezeNetworkingClientHelper.getJumpVelocity(client);
                if (jumpVelocity != null) {
                    client.player.setVelocity(jumpVelocity.x, jumpVelocity.y, jumpVelocity.z);
                    client.player.setOnGround(false);
                    client.player.fallDistance = 0.0f;
                    BreezeClientState.startJump(client.player.getUuid(), client.player.age);
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.BREEZE) return;
            if (GuardianHudRenderer.raycastTarget(client, 20.0) == null) return;

            drawTargetCircle(context, client);
        });
    }

    private static void drawTargetCircle(DrawContext context, MinecraftClient client) {
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int radius = 14;
        int segments = 32;
        int color = 0xDDE4F8FF;
        for (int i = 0; i < segments; i++) {
            if ((i / 2) % 2 == 1) continue;
            double a1 = (i / (double) segments) * 2 * Math.PI;
            double a2 = ((i + 1) / (double) segments) * 2 * Math.PI;
            drawLine(context,
                    cx + (int) (Math.cos(a1) * radius), cy + (int) (Math.sin(a1) * radius),
                    cx + (int) (Math.cos(a2) * radius), cy + (int) (Math.sin(a2) * radius), color);
        }
    }

    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, err = dx - dy;
        while (true) {
            ctx.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private static final class BreezeNetworkingClientHelper {
        private BreezeNetworkingClientHelper() {}

        private static net.minecraft.util.math.Vec3d getJumpVelocity(MinecraftClient client) {
            if (client.player == null) return null;
            return net.sam.samrequiemmod.possession.breeze.BreezePossessionController.getSuperJumpVelocity(client.player);
        }
    }
}
