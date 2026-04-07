package net.sam.samrequiemmod.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.aquatic.NautilusClientState;
import net.sam.samrequiemmod.possession.aquatic.NautilusNetworking;

public final class NautilusHudRenderer {

    private static int chargeTicks = 0;
    private static int localCooldownTicks = 0;
    private static boolean wasJumpDown = false;
    private static boolean playedReadySound = false;

    private NautilusHudRenderer() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                resetState();
                return;
            }
            EntityType<?> type = ClientPossessionState.get(client.player);
            boolean isNautilus = type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS;
            if (!isNautilus) {
                resetState();
                return;
            }

            if (localCooldownTicks > 0) {
                localCooldownTicks--;
            }

            boolean inWater = client.player.isTouchingWater();
            boolean jumpDown = client.options.jumpKey.isPressed();

            if (!inWater || localCooldownTicks > 0) {
                chargeTicks = 0;
                playedReadySound = false;
                wasJumpDown = jumpDown;
                return;
            }

            if (jumpDown) {
                chargeTicks = Math.min(chargeTicks + 1, NautilusClientState.MAX_CHARGE_TICKS);
                client.player.setSwimming(false);
                if (client.player.getVelocity().y > 0.0) {
                    client.player.setVelocity(client.player.getVelocity().x, 0.0, client.player.getVelocity().z);
                }
                if (chargeTicks >= NautilusClientState.MAX_CHARGE_TICKS && !playedReadySound) {
                    client.player.getEntityWorld().playSound(
                            null, client.player.getX(), client.player.getY(), client.player.getZ(),
                            type == EntityType.ZOMBIE_NAUTILUS ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_DASH_READY : SoundEvents.ENTITY_NAUTILUS_DASH_READY,
                            SoundCategory.PLAYERS, 0.8f, 1.0f);
                    playedReadySound = true;
                }
            }

            if (!jumpDown && wasJumpDown && chargeTicks > 0) {
                var dashVelocity = net.sam.samrequiemmod.possession.aquatic.NautilusPossessionController.getDashVelocity(client.player, chargeTicks);
                client.player.setVelocity(dashVelocity.x, dashVelocity.y, dashVelocity.z);
                client.player.velocityDirty = true;
                client.player.setOnGround(false);
                client.player.fallDistance = 0.0f;
                ClientPlayNetworking.send(new NautilusNetworking.DashPayload(chargeTicks));
                localCooldownTicks = NautilusClientState.DASH_COOLDOWN_TICKS;
                chargeTicks = 0;
                playedReadySound = false;
            } else if (!jumpDown) {
                playedReadySound = false;
            }

            wasJumpDown = jumpDown;
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            EntityType<?> type = ClientPossessionState.get(client.player);
            if (type != EntityType.NAUTILUS && type != EntityType.ZOMBIE_NAUTILUS) return;
            if (!client.player.isTouchingWater()) return;

            drawDashBar(context, client);
        });
    }

    public static boolean shouldHideExperienceBar(MinecraftClient client) {
        if (client.player == null) return false;
        EntityType<?> type = ClientPossessionState.get(client.player);
        return (type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS) && client.player.isTouchingWater();
    }

    private static void drawDashBar(DrawContext context, MinecraftClient client) {
        int width = 182;
        int filled = localCooldownTicks > 0
                ? width - Math.round((localCooldownTicks / (float) NautilusClientState.DASH_COOLDOWN_TICKS) * width)
                : Math.round((chargeTicks / (float) NautilusClientState.MAX_CHARGE_TICKS) * width);
        int x = client.getWindow().getScaledWidth() / 2 - 91;
        int y = client.getWindow().getScaledHeight() - 32 + 3;
        int background = 0xB0101A24;
        int border = 0xFF2B6A7A;
        int fill = localCooldownTicks > 0 ? 0xFF5A7C88 : 0xFF5FD4E6;

        context.fill(x - 1, y - 1, x + width + 1, y + 6, border);
        context.fill(x, y, x + width, y + 5, background);
        if (filled > 0) {
            context.fill(x, y, x + filled, y + 5, fill);
        }
    }

    private static void resetState() {
        chargeTicks = 0;
        localCooldownTicks = 0;
        wasJumpDown = false;
        playedReadySound = false;
    }
}
