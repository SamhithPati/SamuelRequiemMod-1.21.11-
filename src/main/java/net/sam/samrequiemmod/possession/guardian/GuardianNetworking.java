package net.sam.samrequiemmod.possession.guardian;

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

public final class GuardianNetworking {

    private static final Identifier START_ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "guardian_start_attack");
    private static final Identifier BEAM_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "guardian_beam_sync");

    private GuardianNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(StartAttackPayload.ID, StartAttackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeamSyncPayload.ID, BeamSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(StartAttackPayload.ID, (payload, context) ->
                context.server().execute(() -> GuardianPossessionController.handleAttackRequest(context.player(), payload.targetUuid().orElse(null))));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BeamSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        GuardianClientState.setBeam(payload.playerUuid(), payload.targetUuid().orElse(null), payload.warmupTicks())));
    }

    public static void broadcastBeam(ServerPlayerEntity player, UUID targetUuid, int warmupTicks) {
        if (player.getServer() == null) return;
        BeamSyncPayload payload = new BeamSyncPayload(player.getUuid(), Optional.ofNullable(targetUuid), warmupTicks);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record StartAttackPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<StartAttackPayload> ID = new Id<>(START_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, StartAttackPayload> CODEC = PacketCodec.of(StartAttackPayload::write, StartAttackPayload::read);

        private static StartAttackPayload read(RegistryByteBuf buf) {
            return new StartAttackPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
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

    public record BeamSyncPayload(UUID playerUuid, Optional<UUID> targetUuid, int warmupTicks) implements CustomPayload {
        public static final Id<BeamSyncPayload> ID = new Id<>(BEAM_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BeamSyncPayload> CODEC = PacketCodec.of(BeamSyncPayload::write, BeamSyncPayload::read);

        private static BeamSyncPayload read(RegistryByteBuf buf) {
            UUID playerUuid = buf.readUuid();
            Optional<UUID> targetUuid = buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty();
            int warmupTicks = buf.readVarInt();
            return new BeamSyncPayload(playerUuid, targetUuid, warmupTicks);
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) buf.writeUuid(targetUuid.get());
            buf.writeVarInt(warmupTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
