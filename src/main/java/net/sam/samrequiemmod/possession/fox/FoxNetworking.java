package net.sam.samrequiemmod.possession.fox;

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

public final class FoxNetworking {

    private static final Identifier VARIANT_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "fox_variant_sync");

    private FoxNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(VariantSyncPayload.ID, VariantSyncPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(VariantSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        FoxState.setClientVariant(payload.playerUuid(), payload.variantId())));
    }

    public static void broadcastVariant(ServerPlayerEntity player, String variantId) {
        if (player.getEntityWorld().getServer() == null) return;
        VariantSyncPayload payload = new VariantSyncPayload(player.getUuid(), variantId);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record VariantSyncPayload(UUID playerUuid, String variantId) implements CustomPayload {
        public static final Id<VariantSyncPayload> ID = new Id<>(VARIANT_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, VariantSyncPayload> CODEC =
                PacketCodec.of(VariantSyncPayload::write, VariantSyncPayload::read);

        private static VariantSyncPayload read(RegistryByteBuf buf) {
            return new VariantSyncPayload(buf.readUuid(), buf.readString());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeString(this.variantId);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}






