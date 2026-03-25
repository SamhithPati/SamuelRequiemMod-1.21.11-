package net.sam.samrequiemmod.possession.slime;

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

public final class SlimeSizeNetworking {

    private static final Identifier IDENTIFIER = Identifier.of(SamuelRequiemMod.MOD_ID, "slime_size_sync");

    private SlimeSizeNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) ->
                context.client().execute(() ->
                        SlimeSizeState.setClientSize(payload.playerUuid(), payload.size())));
    }

    public static void broadcast(ServerPlayerEntity player, int size) {
        if (player.getServer() == null) return;
        Payload payload = new Payload(player.getUuid(), size);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record Payload(UUID playerUuid, int size) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(IDENTIFIER);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);

        private static Payload read(RegistryByteBuf buf) {
            return new Payload(buf.readUuid(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeInt(this.size);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
