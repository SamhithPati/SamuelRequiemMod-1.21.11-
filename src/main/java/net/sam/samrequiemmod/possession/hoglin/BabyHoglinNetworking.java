package net.sam.samrequiemmod.possession.hoglin;

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

public final class BabyHoglinNetworking {

    private static final Identifier ID = Identifier.of(SamuelRequiemMod.MOD_ID, "baby_hoglin_sync");

    private BabyHoglinNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) ->
                context.client().execute(() ->
                        BabyHoglinState.setClientBaby(payload.playerUuid(), payload.isBaby())));
    }

    public static void broadcast(ServerPlayerEntity player, boolean isBaby) {
        if (player.getEntityWorld().getServer() == null) return;
        Payload payload = new Payload(player.getUuid(), isBaby);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record Payload(UUID playerUuid, boolean isBaby) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(BabyHoglinNetworking.ID);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);

        private static Payload read(RegistryByteBuf buf) {
            return new Payload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.isBaby);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}






