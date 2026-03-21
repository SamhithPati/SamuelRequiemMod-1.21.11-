package net.sam.samrequiemmod.possession.zombie_villager;

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

public final class BabyZombieVillagerNetworking {

    public static final Identifier SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "baby_zombie_villager_sync");

    private BabyZombieVillagerNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) ->
                context.client().execute(() -> {
                    BabyZombieVillagerState.setClientBaby(payload.playerUuid(), payload.isBaby());

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
        var pkt = new Payload(player.getUuid(), isBaby);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    public record Payload(UUID playerUuid, boolean isBaby) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);

        private static Payload read(RegistryByteBuf buf) {
            return new Payload(buf.readUuid(), buf.readBoolean());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.isBaby);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
