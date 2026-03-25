package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.iron_golem.IronGolemClientState;
import net.sam.samrequiemmod.possession.iron_golem.IronGolemNetworking;

import java.util.Optional;

/**
 * Client-side input handler for iron golem possession.
 * Left-click: golem attack (raycast to 4 blocks).
 */
public final class IronGolemHudHandler {

    private IronGolemHudHandler() {}

    private static boolean wasAttackKeyDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackKeyDown = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.IRON_GOLEM) {
                wasAttackKeyDown = false;
                return;
            }

            // ── Left-click: golem attack ──────────────────────────────────────
            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown) {
                LivingEntity target = raycastTarget(client, 4.0);
                if (target != null) {
                    // Start attack animation immediately on client
                    IronGolemClientState.setAttacking(client.player.getUuid(), client.player.age);
                    ClientPlayNetworking.send(new IronGolemNetworking.AttackPayload(target.getUuid()));
                }
            }
            wasAttackKeyDown = attackDown;
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
