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

public final class HoglinAttackNetworking {

    private static final Identifier ID = Identifier.of(SamuelRequiemMod.MOD_ID, "hoglin_attack_sync");

    private HoglinAttackNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) ->
                context.client().execute(() ->
                        HoglinAttackClientState.trigger(payload.playerUuid(), payload.durationTicks())));
    }

    public static void broadcastAttack(ServerPlayerEntity player, int durationTicks) {
        if (player.getServer() == null) return;
        Payload payload = new Payload(player.getUuid(), durationTicks);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record Payload(UUID playerUuid, int durationTicks) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(HoglinAttackNetworking.ID);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);

        private static Payload read(RegistryByteBuf buf) {
            return new Payload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeVarInt(this.durationTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
