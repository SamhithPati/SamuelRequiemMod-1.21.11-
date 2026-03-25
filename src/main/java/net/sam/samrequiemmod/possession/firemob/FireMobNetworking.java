package net.sam.samrequiemmod.possession.firemob;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Optional;
import java.util.UUID;

public final class FireMobNetworking {

    private static final Identifier ATTACK_REQUEST_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "firemob_attack_request");
    private static final Identifier BLAZE_ATTACK_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "blaze_attack_sync");
    private static final Identifier GHAST_ATTACK_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "ghast_attack_sync");

    private FireMobNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(AttackRequestPayload.ID, AttackRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BlazeAttackSyncPayload.ID, BlazeAttackSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GhastAttackSyncPayload.ID, GhastAttackSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AttackRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(context.player())) {
                        net.sam.samrequiemmod.possession.firemob.BlazePossessionController.handleAttackRequest(context.player(), payload.targetUuid().orElse(null));
                    } else if (net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(context.player())) {
                        net.sam.samrequiemmod.possession.firemob.GhastPossessionController.handleAttackRequest(context.player(), payload.targetUuid().orElse(null));
                    }
                }));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlazeAttackSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> FireMobAttackClientState.triggerBlaze(payload.playerUuid(), payload.durationTicks())));
        ClientPlayNetworking.registerGlobalReceiver(GhastAttackSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> FireMobAttackClientState.triggerGhast(payload.playerUuid(), payload.durationTicks())));
    }

    public static void broadcastBlazeAttack(ServerPlayerEntity player, int durationTicks) {
        if (player.getServer() == null) return;
        BlazeAttackSyncPayload payload = new BlazeAttackSyncPayload(player.getUuid(), durationTicks);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastGhastAttack(ServerPlayerEntity player, int durationTicks) {
        if (player.getServer() == null) return;
        GhastAttackSyncPayload payload = new GhastAttackSyncPayload(player.getUuid(), durationTicks);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record AttackRequestPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<AttackRequestPayload> ID = new Id<>(ATTACK_REQUEST_ID);
        public static final PacketCodec<RegistryByteBuf, AttackRequestPayload> CODEC =
                PacketCodec.of(AttackRequestPayload::write, AttackRequestPayload::read);

        private static AttackRequestPayload read(RegistryByteBuf buf) {
            return new AttackRequestPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) buf.writeUuid(targetUuid.get());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BlazeAttackSyncPayload(UUID playerUuid, int durationTicks) implements CustomPayload {
        public static final Id<BlazeAttackSyncPayload> ID = new Id<>(BLAZE_ATTACK_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BlazeAttackSyncPayload> CODEC =
                PacketCodec.of(BlazeAttackSyncPayload::write, BlazeAttackSyncPayload::read);

        private static BlazeAttackSyncPayload read(RegistryByteBuf buf) {
            return new BlazeAttackSyncPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(durationTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record GhastAttackSyncPayload(UUID playerUuid, int durationTicks) implements CustomPayload {
        public static final Id<GhastAttackSyncPayload> ID = new Id<>(GHAST_ATTACK_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, GhastAttackSyncPayload> CODEC =
                PacketCodec.of(GhastAttackSyncPayload::write, GhastAttackSyncPayload::read);

        private static GhastAttackSyncPayload read(RegistryByteBuf buf) {
            return new GhastAttackSyncPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(durationTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
