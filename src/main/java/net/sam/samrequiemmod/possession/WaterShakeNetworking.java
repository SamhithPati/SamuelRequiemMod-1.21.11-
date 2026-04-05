package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WaterShakeNetworking {

    public static final Identifier SHAKE_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "water_shake_sync");

    /** Client-side set of player UUIDs currently shaking. */
    public static final Set<UUID> SHAKING_PLAYERS = ConcurrentHashMap.newKeySet();

    private WaterShakeNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.shaking()) SHAKING_PLAYERS.add(payload.playerUuid());
                    else SHAKING_PLAYERS.remove(payload.playerUuid());
                })
        );
    }

    public static void broadcast(ServerPlayerEntity player, boolean shaking) {
        if (player.getEntityWorld().getServer() == null) return;
        var pkt = new Payload(player.getUuid(), shaking);
        for (ServerPlayerEntity r : player.getEntityWorld().getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    public record Payload(UUID playerUuid, boolean shaking) implements CustomPayload {
        public static final Id<Payload> TYPE = new Id<>(SHAKE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);
        private static Payload read(RegistryByteBuf buf) { return new Payload(buf.readUuid(), buf.readBoolean()); }
        private void write(RegistryByteBuf buf) { buf.writeUuid(playerUuid); buf.writeBoolean(shaking); }
        @Override public Id<? extends CustomPayload> getId() { return TYPE; }
    }
}





