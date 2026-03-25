package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.illager.WitchNetworking;

/**
 * Client-side input handler and HUD renderer for witch possession.
 * Left-click while aiming at a mob within 12 blocks → throw splash potion.
 * Draws a targeting circle around the crosshair when a target is in range.
 */
public final class WitchHudRenderer {

    private WitchHudRenderer() {}

    private static boolean wasAttackKeyDown = false;

    public static void register() {

        // ── Tick: handle left-click → potion throw packet ──────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.WITCH) {
                wasAttackKeyDown = false;
                return;
            }

            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                LivingEntity target = EvokerHudRenderer.raycastTarget(client, 12.0);
                if (target != null) {
                    ClientPlayNetworking.send(new WitchNetworking.PotionThrowPayload(target.getUuid()));
                }
            }
            wasAttackKeyDown = attackDown;
        });

        // ── HUD: targeting circle when aiming at a mob ─────────────────────────
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.WITCH) return;
            if (EvokerHudRenderer.raycastTarget(client, 12.0) != null) {
                drawTargetCircle(context, client);
            }
        });
    }

    /** Draws a dashed circle with tick-marks around the crosshair (witch-green). */
    private static void drawTargetCircle(DrawContext context, MinecraftClient client) {
        int cx = client.getWindow().getScaledWidth()  / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int radius = 14, segments = 32;
        int color = 0xDD5B8731; // witch green
        for (int i = 0; i < segments; i++) {
            if ((i / 2) % 2 == 1) continue; // dashed pattern
            double a1 = (i / (double) segments) * 2 * Math.PI;
            double a2 = ((i + 1) / (double) segments) * 2 * Math.PI;
            drawLine(context,
                    cx + (int)(Math.cos(a1) * radius), cy + (int)(Math.sin(a1) * radius),
                    cx + (int)(Math.cos(a2) * radius), cy + (int)(Math.sin(a2) * radius), color);
        }
        for (int i = 0; i < 4; i++) {
            double a = (i / 4.0) * 2 * Math.PI + Math.PI / 4.0;
            drawLine(context,
                    cx + (int)(Math.cos(a) * (radius - 4)), cy + (int)(Math.sin(a) * (radius - 4)),
                    cx + (int)(Math.cos(a) * (radius + 2)), cy + (int)(Math.sin(a) * (radius + 2)), color);
        }
    }

    /** Bresenham line rasteriser (same as EvokerHudRenderer). */
    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1, err = dx - dy;
        while (true) {
            ctx.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 <  dx) { err += dx; y1 += sy; }
        }
    }
}
