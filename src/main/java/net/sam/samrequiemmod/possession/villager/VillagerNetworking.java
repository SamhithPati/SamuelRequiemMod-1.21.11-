package net.sam.samrequiemmod.possession.villager;

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

public final class VillagerNetworking {

    private static final Identifier BABY_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "villager_baby_sync");

    private VillagerNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(BabySyncPayload.ID, BabySyncPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BabySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        VillagerState.setClientBaby(payload.playerUuid(), payload.baby())));
    }

    public static void broadcastBaby(ServerPlayerEntity player, boolean baby) {
        if (player.getServer() == null) return;
        BabySyncPayload payload = new BabySyncPayload(player.getUuid(), baby);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record BabySyncPayload(UUID playerUuid, boolean baby) implements CustomPayload {
        public static final Id<BabySyncPayload> ID = new Id<>(BABY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BabySyncPayload> CODEC =
                PacketCodec.of(BabySyncPayload::write, BabySyncPayload::read);

        private static BabySyncPayload read(RegistryByteBuf buf) {
            return new BabySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.baby);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
