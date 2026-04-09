package net.sam.samrequiemmod.possession.wither;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Optional;
import java.util.UUID;

public final class WitherNetworking {

    private static final Identifier ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wither_attack");
    private static final Identifier EXPLOSION_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wither_explosion");

    private WitherNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(AttackPayload.ID, AttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ExplosionPayload.ID, ExplosionPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        WitherPossessionController.handleAttackRequest(context.player(), payload.targetUuid().orElse(null))));
        ServerPlayNetworking.registerGlobalReceiver(ExplosionPayload.ID, (payload, context) ->
                context.server().execute(() -> WitherPossessionController.handleExplosionRequest(context.player())));
    }

    public static void registerClient() {
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

    public record ExplosionPayload() implements CustomPayload {
        public static final Id<ExplosionPayload> ID = new Id<>(EXPLOSION_ID);
        public static final PacketCodec<RegistryByteBuf, ExplosionPayload> CODEC =
                PacketCodec.of((payload, buf) -> {}, buf -> new ExplosionPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
