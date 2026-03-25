package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.UUID;

public final class MooshroomNetworking {

    private static final Identifier COLOR_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "mooshroom_color_sync");

    private MooshroomNetworking() {}

    // ── Common registration (both sides) ────────────────────────────────

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(ColorSyncPayload.ID, ColorSyncPayload.CODEC);
    }

    // ── Server registration ─────────────────────────────────────────────

    public static void registerServer() {
        // No specific server-side listener needed — broadcasting only
    }

    // ── Client registration ─────────────────────────────────────────────

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ColorSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        MooshroomClientState.setBrownMooshroom(payload.playerUuid(), payload.isBrown())));
    }

    // ── Broadcast color state to all clients ────────────────────────────

    public static void broadcastColorSync(ServerPlayerEntity player, boolean isBrown) {
        if (player.getServer() == null) return;
        var pkt = new ColorSyncPayload(player.getUuid(), isBrown);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payload ──────────────────────────────────────────────────────────

    /** Server -> Client: sync mooshroom color variant for a player. */
    public record ColorSyncPayload(UUID playerUuid, boolean isBrown) implements CustomPayload {
        public static final Id<ColorSyncPayload> ID = new Id<>(COLOR_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, ColorSyncPayload> CODEC =
                PacketCodec.of(ColorSyncPayload::write, ColorSyncPayload::read);

        private static ColorSyncPayload read(RegistryByteBuf buf) {
            return new ColorSyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(isBrown);
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
