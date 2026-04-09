package net.sam.samrequiemmod.possession.creaking;

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

public final class CreakingNetworking {

    private static final Identifier STATE_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "creaking_state_sync");

    private CreakingNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(CreakingStatePayload.ID, CreakingStatePayload.CODEC);
    }

    public static void registerServer() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(CreakingStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    CreakingState.setClientInvulnerableUntil(payload.playerUuid(), payload.invulnerableUntil());
                    CreakingState.setClientCrumblingUntil(payload.playerUuid(), payload.crumblingUntil());
                }));
    }

    public static void broadcastState(ServerPlayerEntity player) {
        if (player.getEntityWorld().getServer() == null) return;
        CreakingStatePayload payload = new CreakingStatePayload(
                player.getUuid(),
                CreakingState.getServerInvulnerableUntil(player.getUuid()),
                CreakingState.getServerCrumblingUntil(player.getUuid())
        );
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record CreakingStatePayload(UUID playerUuid, long invulnerableUntil, long crumblingUntil)
            implements CustomPayload {
        public static final Id<CreakingStatePayload> ID = new Id<>(STATE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, CreakingStatePayload> CODEC =
                PacketCodec.of(CreakingStatePayload::write, CreakingStatePayload::read);

        private static CreakingStatePayload read(RegistryByteBuf buf) {
            return new CreakingStatePayload(buf.readUuid(), buf.readVarLong(), buf.readVarLong());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarLong(invulnerableUntil);
            buf.writeVarLong(crumblingUntil);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
