package net.sam.samrequiemmod.possession.illager;

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

public final class RavagerNetworking {

    public static final Identifier BITE_ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_bite_attack");
    public static final Identifier ROAR_CHARGE_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_roar_charge");
    public static final Identifier BITE_SYNC_ID   = Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_bite_sync");
    public static final Identifier ROAR_SYNC_ID   = Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_roar_sync");

    private RavagerNetworking() {}

    public static void registerCommon() {
        // C2S: client tells server about bite attack target
        PayloadTypeRegistry.playC2S().register(BiteAttackPayload.ID, BiteAttackPayload.CODEC);
        // C2S: client tells server about roar charge state (holding right-click)
        PayloadTypeRegistry.playC2S().register(RoarChargePayload.ID, RoarChargePayload.CODEC);
        // S2C: server tells all clients about bite animation
        PayloadTypeRegistry.playS2C().register(BiteSyncPayload.ID, BiteSyncPayload.CODEC);
        // S2C: server tells all clients about roar animation
        PayloadTypeRegistry.playS2C().register(RoarSyncPayload.ID, RoarSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(BiteAttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        RavagerPossessionController.handleBitePacket(context.player(), payload.targetUuid())));
        ServerPlayNetworking.registerGlobalReceiver(RoarChargePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        RavagerPossessionController.handleRoarCharge(context.player(), payload.holding())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BiteSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Skip for local player — animation was already started immediately on input
                    var local = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (local != null && payload.playerUuid().equals(local.getUuid())) return;
                    RavagerClientState.setBiting(payload.playerUuid(), payload.playerAge());
                }));
        ClientPlayNetworking.registerGlobalReceiver(RoarSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Skip for local player — animation was already started immediately on input
                    var local = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (local != null && payload.playerUuid().equals(local.getUuid())) return;
                    RavagerClientState.setRoaring(payload.playerUuid(), payload.playerAge());
                }));
    }

    /** Broadcast bite animation to all clients. */
    public static void broadcastBite(ServerPlayerEntity player) {
        if (player.getServer() == null) return;
        var pkt = new BiteSyncPayload(player.getUuid(), player.age);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    /** Broadcast roar animation to all clients. */
    public static void broadcastRoar(ServerPlayerEntity player) {
        if (player.getServer() == null) return;
        var pkt = new RoarSyncPayload(player.getUuid(), player.age);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ──────────────────────────────────────────────────────────────

    /** C2S: Player requests bite attack on a target. */
    public record BiteAttackPayload(UUID targetUuid) implements CustomPayload {
        public static final Id<BiteAttackPayload> ID = new Id<>(BITE_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, BiteAttackPayload> CODEC =
                PacketCodec.of(BiteAttackPayload::write, BiteAttackPayload::read);
        private static BiteAttackPayload read(RegistryByteBuf buf) {
            boolean has = buf.readBoolean();
            return new BiteAttackPayload(has ? buf.readUuid() : null);
        }
        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid != null);
            if (targetUuid != null) buf.writeUuid(targetUuid);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** C2S: Player is holding/releasing right-click for roar charge. */
    public record RoarChargePayload(boolean holding) implements CustomPayload {
        public static final Id<RoarChargePayload> ID = new Id<>(ROAR_CHARGE_ID);
        public static final PacketCodec<RegistryByteBuf, RoarChargePayload> CODEC =
                PacketCodec.of(RoarChargePayload::write, RoarChargePayload::read);
        private static RoarChargePayload read(RegistryByteBuf buf) {
            return new RoarChargePayload(buf.readBoolean());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(holding);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: Bite animation sync. */
    public record BiteSyncPayload(UUID playerUuid, int playerAge) implements CustomPayload {
        public static final Id<BiteSyncPayload> ID = new Id<>(BITE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BiteSyncPayload> CODEC =
                PacketCodec.of(BiteSyncPayload::write, BiteSyncPayload::read);
        private static BiteSyncPayload read(RegistryByteBuf buf) {
            return new BiteSyncPayload(buf.readUuid(), buf.readInt());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeInt(playerAge);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: Roar animation sync. */
    public record RoarSyncPayload(UUID playerUuid, int playerAge) implements CustomPayload {
        public static final Id<RoarSyncPayload> ID = new Id<>(ROAR_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, RoarSyncPayload> CODEC =
                PacketCodec.of(RoarSyncPayload::write, RoarSyncPayload::read);
        private static RoarSyncPayload read(RegistryByteBuf buf) {
            return new RoarSyncPayload(buf.readUuid(), buf.readInt());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeInt(playerAge);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
