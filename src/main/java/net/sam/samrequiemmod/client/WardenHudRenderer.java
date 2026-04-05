package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.warden.WardenNetworking;

import java.util.Optional;

public final class WardenHudRenderer {

    private static boolean wasUseKeyDown = false;
    private static boolean wasAttackKeyDown = false;

    private WardenHudRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasUseKeyDown = false;
                wasAttackKeyDown = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.WARDEN) {
                wasUseKeyDown = false;
                wasAttackKeyDown = false;
                return;
            }

            boolean useDown = client.options.useKey.isPressed();
            if (useDown && !wasUseKeyDown) {
                LivingEntity target = GuardianHudRenderer.raycastTarget(client, 20.0);
                if (target != null) {
                    net.sam.samrequiemmod.possession.warden.WardenClientState.startSonic(client.player.getUuid(), client.player.age);
                    ClientPlayNetworking.send(new WardenNetworking.SonicBoomPayload(Optional.of(target.getUuid())));
                }
            }
            wasUseKeyDown = useDown;

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown
                    && client.targetedEntity instanceof LivingEntity target
                    && client.player.squaredDistanceTo(target) <= 9.0) {
                net.sam.samrequiemmod.possession.warden.WardenClientState.startAttack(client.player.getUuid(), client.player.age);
            }
            wasAttackKeyDown = attackDown;
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.WARDEN) return;
            if (GuardianHudRenderer.raycastTarget(client, 20.0) == null) return;

            drawTargetCircle(context, client);
        });
    }

    private static void drawTargetCircle(DrawContext context, MinecraftClient client) {
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int radius = 15;
        int segments = 32;
        int color = 0xDD0E9AA7;
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
}
