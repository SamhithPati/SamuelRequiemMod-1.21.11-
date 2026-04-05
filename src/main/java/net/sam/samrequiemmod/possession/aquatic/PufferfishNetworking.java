package net.sam.samrequiemmod.possession.aquatic;

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

/**
 * Syncs pufferfish puff state (inflated/deflated) from server to all clients.
 */
public final class PufferfishNetworking {

    private static final Identifier PUFF_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "pufferfish_puff_sync");

    private PufferfishNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(PuffSyncPayload.ID, PuffSyncPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(PuffSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        PufferfishState.setPuffed(payload.playerUuid(), payload.puffed())));
    }

    public static void broadcastPuffState(ServerPlayerEntity player, boolean puffed) {
        if (player.getEntityWorld().getServer() == null) return;
        var pkt = new PuffSyncPayload(player.getUuid(), puffed);
        for (ServerPlayerEntity r : player.getEntityWorld().getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payload ────────────────────────────────────────────────────────────────

    public record PuffSyncPayload(UUID playerUuid, boolean puffed) implements CustomPayload {
        public static final Id<PuffSyncPayload> ID = new Id<>(PUFF_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, PuffSyncPayload> CODEC =
                PacketCodec.of(PuffSyncPayload::write, PuffSyncPayload::read);

        private static PuffSyncPayload read(RegistryByteBuf buf) {
            return new PuffSyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(puffed);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}






