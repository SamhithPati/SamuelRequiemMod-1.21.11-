package net.sam.samrequiemmod.possession.husk;

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

public final class BabyHuskNetworking {

    public static final Identifier BABY_HUSK_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "baby_husk_sync");

    private BabyHuskNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(BabyHuskPayload.ID, BabyHuskPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BabyHuskPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    BabyHuskState.setClientBaby(payload.playerUuid(), payload.isBaby());

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

    public static void broadcastBabyHuskSync(ServerPlayerEntity player, boolean isBaby) {
        if (player.getServer() == null) return;
        var pkt = new BabyHuskPayload(player.getUuid(), isBaby);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    public record BabyHuskPayload(UUID playerUuid, boolean isBaby) implements CustomPayload {
        public static final Id<BabyHuskPayload> ID = new Id<>(BABY_HUSK_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BabyHuskPayload> CODEC =
                PacketCodec.of(BabyHuskPayload::write, BabyHuskPayload::read);

        private static BabyHuskPayload read(RegistryByteBuf buf) {
            return new BabyHuskPayload(buf.readUuid(), buf.readBoolean());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.isBaby);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}