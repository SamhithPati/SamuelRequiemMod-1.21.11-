package net.sam.samrequiemmod.possession.enderman;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.UUID;

public final class EndermanNetworking {

    private static final Identifier ANGRY_TOGGLE_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "enderman_angry_toggle");
    private static final Identifier ANGRY_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "enderman_angry_sync");
    private static final Identifier TELEPORT_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "enderman_teleport");

    private EndermanNetworking() {}

    // ── Common registration (both sides) ────────────────────────────────────

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(AngryTogglePayload.ID, AngryTogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportPayload.ID, TeleportPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AngrySyncPayload.ID, AngrySyncPayload.CODEC);
    }

    // ── Server registration ─────────────────────────────────────────────────

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AngryTogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        EndermanPossessionController.handleAngryToggle(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(TeleportPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        EndermanPossessionController.handleDirectionalTeleport(context.player())));
    }

    /** Edge-detection flags for raw input polling. */
    private static boolean wasUseKeyDown = false;
    private static boolean wasYKeyDown = false;

    // ── Client registration ─────────────────────────────────────────────────

    public static void registerClient() {
        // No custom KeyBinding for Y — the captain recruit system already registers one.
        // Using raw GLFW polling with edge detection avoids keybinding conflicts.

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            var possType = ClientPossessionState.get(client.player);
            if (possType != EntityType.ENDERMAN) {
                wasUseKeyDown = false;
                wasYKeyDown = false;
                return;
            }

            long window = client.getWindow().getHandle();

            // Y key: toggle angry — raw GLFW poll to avoid keybinding conflict with captain recruit
            boolean yDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_Y)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (yDown && !wasYKeyDown && client.currentScreen == null) {
                ClientPlayNetworking.send(new AngryTogglePayload());
            }
            wasYKeyDown = yDown;

            // Right-click teleport: detect rising edge of vanilla use key
            boolean useDown = client.options.useKey.isPressed();
            if (useDown && !wasUseKeyDown) {
                net.minecraft.item.ItemStack mainHand = client.player.getMainHandStack();
                if (mainHand.isEmpty()) {
                    ClientPlayNetworking.send(new TeleportPayload());
                }
            }
            wasUseKeyDown = useDown;
        });

        // Angry sync receiver
        ClientPlayNetworking.registerGlobalReceiver(AngrySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        EndermanClientState.setAngry(payload.playerUuid(), payload.angry())));
    }

    // ── Broadcast angry state to all clients ────────────────────────────────

    public static void broadcastAngry(ServerPlayerEntity player, boolean angry) {
        if (player.getServer() == null) return;
        var pkt = new AngrySyncPayload(player.getUuid(), angry);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ────────────────────────────────────────────────────────────

    /** Client -> Server: toggle angry state. */
    public record AngryTogglePayload() implements CustomPayload {
        public static final Id<AngryTogglePayload> ID = new Id<>(ANGRY_TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, AngryTogglePayload> CODEC =
                PacketCodec.of(AngryTogglePayload::write, AngryTogglePayload::read);

        private static AngryTogglePayload read(RegistryByteBuf buf) {
            buf.readByte(); // dummy
            return new AngryTogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0); // dummy byte — some codecs don't like zero-length payloads
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Client -> Server: request directional teleport. */
    public record TeleportPayload() implements CustomPayload {
        public static final Id<TeleportPayload> ID = new Id<>(TELEPORT_ID);
        public static final PacketCodec<RegistryByteBuf, TeleportPayload> CODEC =
                PacketCodec.of(TeleportPayload::write, TeleportPayload::read);

        private static TeleportPayload read(RegistryByteBuf buf) {
            buf.readByte(); // dummy
            return new TeleportPayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0); // dummy byte
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Server -> Client: sync angry state for a player. */
    public record AngrySyncPayload(UUID playerUuid, boolean angry) implements CustomPayload {
        public static final Id<AngrySyncPayload> ID = new Id<>(ANGRY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, AngrySyncPayload> CODEC =
                PacketCodec.of(AngrySyncPayload::write, AngrySyncPayload::read);

        private static AngrySyncPayload read(RegistryByteBuf buf) {
            return new AngrySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(angry);
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
