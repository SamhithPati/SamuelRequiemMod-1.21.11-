package net.sam.samrequiemmod.possession.iron_golem;

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

public final class IronGolemNetworking {

    public static final Identifier ATTACK_C2S_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "iron_golem_attack");
    public static final Identifier ATTACK_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "iron_golem_attack_sync");

    private IronGolemNetworking() {}

    public static void registerCommon() {
        // C2S: client tells server about attack target
        PayloadTypeRegistry.playC2S().register(AttackPayload.ID, AttackPayload.CODEC);
        // S2C: server tells all clients about attack animation
        PayloadTypeRegistry.playS2C().register(AttackSyncPayload.ID, AttackSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        IronGolemPossessionController.handleAttackPacket(context.player(), payload.targetUuid())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(AttackSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Skip for local player — animation was already started immediately on input
                    var local = net.minecraft.client.MinecraftClient.getInstance().player;
                    if (local != null && payload.playerUuid().equals(local.getUuid())) return;
                    IronGolemClientState.setAttacking(payload.playerUuid(), payload.playerAge());
                }));
    }

    /** Broadcast attack animation to all clients. */
    public static void broadcastAttack(ServerPlayerEntity player) {
        if (player.getEntityWorld().getServer() == null) return;
        var pkt = new AttackSyncPayload(player.getUuid(), player.age);
        for (ServerPlayerEntity r : player.getEntityWorld().getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ──────────────────────────────────────────────────────────────

    /** C2S: Player requests attack on a target. */
    public record AttackPayload(UUID targetUuid) implements CustomPayload {
        public static final Id<AttackPayload> ID = new Id<>(ATTACK_C2S_ID);
        public static final PacketCodec<RegistryByteBuf, AttackPayload> CODEC =
                PacketCodec.of(AttackPayload::write, AttackPayload::read);
        private static AttackPayload read(RegistryByteBuf buf) {
            boolean has = buf.readBoolean();
            return new AttackPayload(has ? buf.readUuid() : null);
        }
        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid != null);
            if (targetUuid != null) buf.writeUuid(targetUuid);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** S2C: Attack animation sync. */
    public record AttackSyncPayload(UUID playerUuid, int playerAge) implements CustomPayload {
        public static final Id<AttackSyncPayload> ID = new Id<>(ATTACK_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, AttackSyncPayload> CODEC =
                PacketCodec.of(AttackSyncPayload::write, AttackSyncPayload::read);
        private static AttackSyncPayload read(RegistryByteBuf buf) {
            return new AttackSyncPayload(buf.readUuid(), buf.readInt());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeInt(playerAge);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}






