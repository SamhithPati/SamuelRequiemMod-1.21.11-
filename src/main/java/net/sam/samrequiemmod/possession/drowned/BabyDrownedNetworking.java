package net.sam.samrequiemmod.possession.drowned;

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

public final class BabyDrownedNetworking {
    public static final Identifier BABY_DROWNED_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "baby_drowned_sync");

    private BabyDrownedNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    BabyDrownedState.setClientBaby(payload.uuid(), payload.baby());
                    MinecraftClient client = context.client();
                    if (client.world == null) return;
                    for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                        if (p.getUuid().equals(payload.uuid())) { p.calculateDimensions(); break; }
                    }
                })
        );
    }

    public static void broadcast(ServerPlayerEntity player, boolean baby) {
        if (player.getServer() == null) return;
        var pkt = new Payload(player.getUuid(), baby);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    public record Payload(UUID uuid, boolean baby) implements CustomPayload {
        public static final Id<Payload> TYPE = new Id<>(BABY_DROWNED_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);
        private static Payload read(RegistryByteBuf buf) { return new Payload(buf.readUuid(), buf.readBoolean()); }
        private void write(RegistryByteBuf buf) { buf.writeUuid(uuid); buf.writeBoolean(baby); }
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }
}