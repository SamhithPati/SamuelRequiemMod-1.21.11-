package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.illager.RavagerNetworking;

import java.util.Optional;

/**
 * Client-side input handler for ravager possession.
 * Left-click: bite attack (raycast to 4 blocks).
 * Hold right-click for 2+ seconds: roar attack.
 */
public final class RavagerHudHandler {

    private RavagerHudHandler() {}

    private static boolean wasAttackKeyDown = false;
    private static boolean lastRoarHolding = false;
    private static int roarHoldTicks = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                lastRoarHolding = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.RAVAGER) {
                wasAttackKeyDown = false;
                lastRoarHolding = false;
                return;
            }

            // ── Left-click: bite attack ──────────────────────────────────────
            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                LivingEntity target = raycastTarget(client, 4.0);
                if (target != null) {
                    // Start bite animation immediately on client (no server round-trip delay)
                    net.sam.samrequiemmod.possession.illager.RavagerClientState
                            .setBiting(client.player.getUuid(), client.player.age);
                    ClientPlayNetworking.send(new RavagerNetworking.BiteAttackPayload(target.getUuid()));
                }
            }
            wasAttackKeyDown = attackDown;

            // ── Right-click hold: roar charge ────────────────────────────────
            // Send every tick while holding so the server-side counter increments
            boolean useDown = client.options.useKey.isPressed();
            if (useDown) {
                roarHoldTicks++;
                ClientPlayNetworking.send(new RavagerNetworking.RoarChargePayload(true));
                // Start roar animation immediately on client at 40 ticks (matching server threshold)
                if (roarHoldTicks == 40) {
                    net.sam.samrequiemmod.possession.illager.RavagerClientState
                            .setRoaring(client.player.getUuid(), client.player.age);
                }
            } else {
                if (lastRoarHolding) {
                    ClientPlayNetworking.send(new RavagerNetworking.RoarChargePayload(false));
                }
                roarHoldTicks = 0;
            }
            lastRoarHolding = useDown;
        });
    }

    /** Manual entity raycast up to maxDist blocks in look direction. */
    private static LivingEntity raycastTarget(MinecraftClient client, double maxDist) {
        if (client.player == null || client.world == null) return null;
        Vec3d eye = client.player.getCameraPosVec(1.0f);
        Vec3d look = client.player.getRotationVec(1.0f);
        Vec3d end = eye.add(look.multiply(maxDist));
        Box searchBox = client.player.getBoundingBox().stretch(look.multiply(maxDist)).expand(2.0);
        LivingEntity closest = null;
        double closestDist = maxDist * maxDist;
        for (LivingEntity e : client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                le -> le != client.player && le.isAlive())) {
            Optional<Vec3d> hit = e.getBoundingBox().expand(0.3).raycast(eye, end);
            if (hit.isPresent()) {
                double d = eye.squaredDistanceTo(hit.get());
                if (d < closestDist) {
                    closestDist = d;
                    closest = e;
                }
            }
        }
        return closest;
    }

}
