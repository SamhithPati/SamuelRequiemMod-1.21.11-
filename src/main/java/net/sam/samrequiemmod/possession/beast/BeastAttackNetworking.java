package net.sam.samrequiemmod.possession.beast;

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

public final class BeastAttackNetworking {

    private static final Identifier POLAR_ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_polar_attack");

    private BeastAttackNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(PolarAttackPayload.ID, PolarAttackPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(PolarAttackPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastAttackClientState.triggerPolar(payload.playerUuid(), payload.playerAge())));
    }

    public static void broadcastPolarAttack(ServerPlayerEntity player) {
        if (player.getServer() == null) return;
        PolarAttackPayload payload = new PolarAttackPayload(player.getUuid(), player.age);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record PolarAttackPayload(UUID playerUuid, int playerAge) implements CustomPayload {
        public static final Id<PolarAttackPayload> ID = new Id<>(POLAR_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, PolarAttackPayload> CODEC =
                PacketCodec.of(PolarAttackPayload::write, PolarAttackPayload::read);

        private static PolarAttackPayload read(RegistryByteBuf buf) {
            return new PolarAttackPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(playerAge);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
