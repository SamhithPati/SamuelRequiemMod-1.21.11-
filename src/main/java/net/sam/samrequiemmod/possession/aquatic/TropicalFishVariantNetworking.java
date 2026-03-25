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
 * Syncs the tropical fish variant (color/pattern) from server to all clients
 * so the rendered fish shell shows the correct appearance.
 */
public final class TropicalFishVariantNetworking {

    private static final Identifier VARIANT_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "tropical_fish_variant_sync");

    private TropicalFishVariantNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(VariantSyncPayload.ID, VariantSyncPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(VariantSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        TropicalFishVariantState.setClientVariant(payload.playerUuid(), payload.variant())));
    }

    public static void broadcastVariant(ServerPlayerEntity player, int variant) {
        if (player.getServer() == null) return;
        var pkt = new VariantSyncPayload(player.getUuid(), variant);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payload ────────────────────────────────────────────────────────────────

    public record VariantSyncPayload(UUID playerUuid, int variant) implements CustomPayload {
        public static final Id<VariantSyncPayload> ID = new Id<>(VARIANT_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, VariantSyncPayload> CODEC =
                PacketCodec.of(VariantSyncPayload::write, VariantSyncPayload::read);

        private static VariantSyncPayload read(RegistryByteBuf buf) {
            return new VariantSyncPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(variant);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
