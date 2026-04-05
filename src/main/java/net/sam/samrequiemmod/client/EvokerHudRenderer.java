package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.illager.EvokerNetworking;

import java.util.Optional;
import java.util.UUID;

public final class EvokerHudRenderer {

    private EvokerHudRenderer() {}

    private static boolean wasAttackKeyDown = false;
    private static boolean wasUseKeyDown    = false;

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                wasUseKeyDown    = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.EVOKER) {
                wasAttackKeyDown = false;
                wasUseKeyDown    = false;
                return;
            }

            // ── Left-click: fang attack ──────────────────────────────────────
            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                // Raycast to find target up to 20 blocks away
                LivingEntity target = raycastTarget(client, 20.0);
                // Don't summon fangs when looking at the ravager we're riding
                boolean isRiddenRavager = target instanceof net.minecraft.entity.mob.RavagerEntity
                        && client.player.getVehicle() == target;
                if (!isRiddenRavager) {
                    UUID targetUuid = (target != null) ? target.getUuid() : null;
                    ClientPlayNetworking.send(new EvokerNetworking.FangAttackPayload(targetUuid));
                }
            }
            wasAttackKeyDown = attackDown;

            // ── Right-click: vex summon ──────────────────────────────────────
            boolean useDown = client.options.useKey.isPressed();
            if (useDown && !wasUseKeyDown) {
                ClientPlayNetworking.send(new EvokerNetworking.VexKeyPayload());
            }
            wasUseKeyDown = useDown;
        });

        // ── HUD: targeting circle ────────────────────────────────────────────
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.EVOKER) return;
            if (raycastTarget(client, 20.0) != null) drawTargetCircle(context, client);
        });
    }

    /** Manual entity raycast up to maxDist blocks in look direction. */
    public static LivingEntity raycastTarget(MinecraftClient client, double maxDist) {
        if (client.player == null || client.world == null) return null;
        Vec3d eye  = client.player.getCameraPosVec(1.0f);
        Vec3d look = client.player.getRotationVec(1.0f);
        Vec3d end  = eye.add(look.multiply(maxDist));
        Box searchBox = client.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(2.0);
        LivingEntity closest = null;
        double closestDist = maxDist * maxDist;
        for (LivingEntity e : client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                le -> le != client.player && le.isAlive())) {
            Optional<Vec3d> hit = e.getBoundingBox().expand(0.3).raycast(eye, end);
            if (hit.isPresent()) {
                double d = eye.squaredDistanceTo(hit.get());
                if (d < closestDist) { closestDist = d; closest = e; }
            }
        }
        return closest;
    }

    private static void drawTargetCircle(DrawContext context, MinecraftClient client) {
        int cx = client.getWindow().getScaledWidth()  / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int radius = 14, segments = 32;
        int color = 0xDD9B59FF;
        for (int i = 0; i < segments; i++) {
            if ((i / 2) % 2 == 1) continue;
            double a1 = (i / (double)segments) * 2 * Math.PI;
            double a2 = ((i+1) / (double)segments) * 2 * Math.PI;
            drawLine(context,
                    cx + (int)(Math.cos(a1)*radius), cy + (int)(Math.sin(a1)*radius),
                    cx + (int)(Math.cos(a2)*radius), cy + (int)(Math.sin(a2)*radius), color);
        }
        for (int i = 0; i < 4; i++) {
            double a = (i/4.0)*2*Math.PI + Math.PI/4.0;
            drawLine(context,
                    cx + (int)(Math.cos(a)*(radius-4)), cy + (int)(Math.sin(a)*(radius-4)),
                    cx + (int)(Math.cos(a)*(radius+2)), cy + (int)(Math.sin(a)*(radius+2)), color);
        }
    }

    private static void drawLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2-x1), dy = Math.abs(y2-y1);
        int sx = x1<x2?1:-1, sy = y1<y2?1:-1, err = dx-dy;
        while (true) {
            ctx.fill(x1, y1, x1+1, y1+1, color);
            if (x1==x2 && y1==y2) break;
            int e2 = 2*err;
            if (e2>-dy){err-=dy; x1+=sx;}
            if (e2< dx){err+=dx; y1+=sy;}
        }
    }
}





