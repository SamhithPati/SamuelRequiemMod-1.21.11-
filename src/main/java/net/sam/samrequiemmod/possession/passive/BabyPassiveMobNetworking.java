package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.UUID;

public final class BabyPassiveMobNetworking {

    public static final Identifier SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "baby_passive_mob_sync");

    private BabyPassiveMobNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(BabyPassiveMobPayload.ID, BabyPassiveMobPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BabyPassiveMobPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    BabyPassiveMobState.setClientBaby(payload.playerUuid(), payload.isBaby());

                    MinecraftClient client = context.client();
                    if (client.world == null) return;

                    for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                        if (player.getUuid().equals(payload.playerUuid())) {
                            player.calculateDimensions();
                            break;
                        }
                    }
                })
        );
    }

    public static void broadcast(ServerPlayerEntity player, boolean isBaby) {
        if (player.getServer() == null) return;
        var pkt = new BabyPassiveMobPayload(player.getUuid(), isBaby);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    public record BabyPassiveMobPayload(UUID playerUuid, boolean isBaby) implements CustomPayload {
        public static final Id<BabyPassiveMobPayload> ID = new Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BabyPassiveMobPayload> CODEC =
                PacketCodec.of(BabyPassiveMobPayload::write, BabyPassiveMobPayload::read);

        private static BabyPassiveMobPayload read(RegistryByteBuf buf) {
            return new BabyPassiveMobPayload(buf.readUuid(), buf.readBoolean());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.isBaby);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
