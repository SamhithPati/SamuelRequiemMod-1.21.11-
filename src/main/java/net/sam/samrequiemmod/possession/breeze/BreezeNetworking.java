package net.sam.samrequiemmod.possession.breeze;

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

public final class BreezeNetworking {

    private static final Identifier ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "breeze_attack");
    private static final Identifier JUMP_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "breeze_jump");
    private static final Identifier ANIM_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "breeze_anim_sync");

    private BreezeNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(AttackPayload.ID, AttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(JumpPayload.ID, JumpPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimationSyncPayload.ID, AnimationSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        BreezePossessionController.handleAttackRequest(context.player(), payload.targetUuid().orElse(null))));
        ServerPlayNetworking.registerGlobalReceiver(JumpPayload.ID, (payload, context) ->
                context.server().execute(() -> BreezePossessionController.handleJumpRequest(context.player())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(AnimationSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    int localStartTick = payload.playerAge();
                    if (context.client().world != null) {
                        var targetPlayer = context.client().world.getPlayerByUuid(payload.playerUuid());
                        if (targetPlayer != null) {
                            localStartTick = targetPlayer.age;
                        } else if (context.client().player != null) {
                            localStartTick = context.client().player.age;
                        }
                    } else if (context.client().player != null) {
                        localStartTick = context.client().player.age;
                    }

                    switch (payload.animationType()) {
                        case 1 -> BreezeClientState.startShoot(payload.playerUuid(), localStartTick);
                        case 2 -> BreezeClientState.startJump(payload.playerUuid(), localStartTick);
                        default -> BreezeClientState.clear(payload.playerUuid());
                    }
                }));
    }

    public static void broadcastAnimation(ServerPlayerEntity player, int animationType) {
        if (player.getEntityWorld().getServer() == null) return;
        AnimationSyncPayload payload = new AnimationSyncPayload(player.getUuid(), animationType, player.age);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record AttackPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<AttackPayload> ID = new Id<>(ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, AttackPayload> CODEC =
                PacketCodec.of(AttackPayload::write, AttackPayload::read);

        private static AttackPayload read(RegistryByteBuf buf) {
            return new AttackPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) {
                buf.writeUuid(targetUuid.get());
            }
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record JumpPayload() implements CustomPayload {
        public static final Id<JumpPayload> ID = new Id<>(JUMP_ID);
        public static final PacketCodec<RegistryByteBuf, JumpPayload> CODEC =
                PacketCodec.of((payload, buf) -> {}, buf -> new JumpPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AnimationSyncPayload(UUID playerUuid, int animationType, int playerAge) implements CustomPayload {
        public static final Id<AnimationSyncPayload> ID = new Id<>(ANIM_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, AnimationSyncPayload> CODEC =
                PacketCodec.of(AnimationSyncPayload::write, AnimationSyncPayload::read);

        private static AnimationSyncPayload read(RegistryByteBuf buf) {
            return new AnimationSyncPayload(buf.readUuid(), buf.readByte(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeByte(animationType);
            buf.writeInt(playerAge);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
