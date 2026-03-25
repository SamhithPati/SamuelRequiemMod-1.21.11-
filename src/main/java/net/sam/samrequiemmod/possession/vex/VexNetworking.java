package net.sam.samrequiemmod.possession.vex;

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

public final class VexNetworking {

    private static final Identifier TOGGLE_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "vex_angry_toggle");
    private static final Identifier SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "vex_angry_sync");
    private static boolean wasYKeyDown = false;

    private VexNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.ID, SyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        VexPossessionController.handleAngryToggle(context.player())));
    }

    public static void registerClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasYKeyDown = false;
                return;
            }
            if (ClientPossessionState.get(client.player) != EntityType.VEX) {
                wasYKeyDown = false;
                return;
            }

            long window = client.getWindow().getHandle();
            boolean yDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_Y)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (yDown && !wasYKeyDown && client.currentScreen == null) {
                ClientPlayNetworking.send(new TogglePayload());
            }
            wasYKeyDown = yDown;
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        VexState.setClientAngry(payload.playerUuid(), payload.angry())));
    }

    public static void broadcastAngry(ServerPlayerEntity player, boolean angry) {
        if (player.getServer() == null) return;
        SyncPayload payload = new SyncPayload(player.getUuid(), angry);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record TogglePayload() implements CustomPayload {
        public static final Id<TogglePayload> ID = new Id<>(TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, TogglePayload> CODEC =
                PacketCodec.of(TogglePayload::write, TogglePayload::read);

        private static TogglePayload read(RegistryByteBuf buf) {
            buf.readByte();
            return new TogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record SyncPayload(UUID playerUuid, boolean angry) implements CustomPayload {
        public static final Id<SyncPayload> ID = new Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, SyncPayload> CODEC =
                PacketCodec.of(SyncPayload::write, SyncPayload::read);

        private static SyncPayload read(RegistryByteBuf buf) {
            return new SyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.angry);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
