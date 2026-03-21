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

public final class EvokerNetworking {

    public static final Identifier FANG_ATTACK_ID  = Identifier.of(SamuelRequiemMod.MOD_ID, "evoker_fang_attack");
    public static final Identifier VEX_SUMMON_ID   = Identifier.of(SamuelRequiemMod.MOD_ID, "evoker_vex_summon");
    public static final Identifier TARGET_SYNC_ID  = Identifier.of(SamuelRequiemMod.MOD_ID, "evoker_target_sync");
    public static final Identifier CASTING_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "evoker_casting_sync");
    public static final Identifier VEX_KEY_ID      = Identifier.of(SamuelRequiemMod.MOD_ID, "evoker_vex_key");

    private EvokerNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(FangAttackPayload.ID, FangAttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VexSummonPayload.ID, VexSummonPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VexKeyPayload.ID, VexKeyPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TargetSyncPayload.ID, TargetSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CastingSyncPayload.ID, CastingSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(FangAttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        EvokerPossessionController.handleFangAttackPacket(context.player(), payload.targetUuid())));
        ServerPlayNetworking.registerGlobalReceiver(VexSummonPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        EvokerPossessionController.handleVexSummonPacket(context.player())));
        // VexKey is the right-click packet sent by EvokerHudRenderer tick
        ServerPlayNetworking.registerGlobalReceiver(VexKeyPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        EvokerPossessionController.handleVexSummonPacket(context.player())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TargetSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        EvokerClientState.setTarget(payload.playerUuid(), payload.targetUuid())));
        ClientPlayNetworking.registerGlobalReceiver(CastingSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        EvokerClientState.setCasting(payload.playerUuid(), payload.castType())));
    }

    public static void broadcastTarget(ServerPlayerEntity player, UUID targetUuid) {
        if (player.getServer() == null) return;
        var pkt = new TargetSyncPayload(player.getUuid(), targetUuid);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    /** castType: 0=stop, 1=fang, 2=vex */
    public static void broadcastCasting(ServerPlayerEntity player, int castType) {
        if (player.getServer() == null) return;
        var pkt = new CastingSyncPayload(player.getUuid(), castType);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ──────────────────────────────────────────────────────────────

    public record FangAttackPayload(UUID targetUuid) implements CustomPayload {
        public static final Id<FangAttackPayload> ID = new Id<>(FANG_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, FangAttackPayload> CODEC =
                PacketCodec.of(FangAttackPayload::write, FangAttackPayload::read);
        private static FangAttackPayload read(RegistryByteBuf buf) {
            boolean has = buf.readBoolean();
            return new FangAttackPayload(has ? buf.readUuid() : null);
        }
        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid != null);
            if (targetUuid != null) buf.writeUuid(targetUuid);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record VexSummonPayload() implements CustomPayload {
        public static final Id<VexSummonPayload> ID = new Id<>(VEX_SUMMON_ID);
        public static final PacketCodec<RegistryByteBuf, VexSummonPayload> CODEC =
                PacketCodec.of((p, buf) -> {}, buf -> new VexSummonPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Client -> Server: right-click key pressed while possessing evoker */
    public record VexKeyPayload() implements CustomPayload {
        public static final Id<VexKeyPayload> ID = new Id<>(VEX_KEY_ID);
        public static final PacketCodec<RegistryByteBuf, VexKeyPayload> CODEC =
                PacketCodec.of((p, buf) -> {}, buf -> new VexKeyPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record TargetSyncPayload(UUID playerUuid, UUID targetUuid) implements CustomPayload {
        public static final Id<TargetSyncPayload> ID = new Id<>(TARGET_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, TargetSyncPayload> CODEC =
                PacketCodec.of(TargetSyncPayload::write, TargetSyncPayload::read);
        private static TargetSyncPayload read(RegistryByteBuf buf) {
            UUID p = buf.readUuid();
            boolean has = buf.readBoolean();
            return new TargetSyncPayload(p, has ? buf.readUuid() : null);
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(targetUuid != null);
            if (targetUuid != null) buf.writeUuid(targetUuid);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Server -> Client: castType 0=none, 1=fangs, 2=vexes */
    public record CastingSyncPayload(UUID playerUuid, int castType) implements CustomPayload {
        public static final Id<CastingSyncPayload> ID = new Id<>(CASTING_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, CastingSyncPayload> CODEC =
                PacketCodec.of(CastingSyncPayload::write, CastingSyncPayload::read);
        private static CastingSyncPayload read(RegistryByteBuf buf) {
            return new CastingSyncPayload(buf.readUuid(), buf.readByte());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeByte(castType);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}