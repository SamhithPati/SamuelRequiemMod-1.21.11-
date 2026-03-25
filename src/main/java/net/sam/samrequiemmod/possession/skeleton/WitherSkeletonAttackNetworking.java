package net.sam.samrequiemmod.possession.skeleton;

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

/**
 * Syncs wither skeleton "arms out" attacking state to clients for rendering.
 */
public final class WitherSkeletonAttackNetworking {

    public static final Identifier WITHER_SKEL_ATTACK_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "wither_skel_attack_sync");

    private WitherSkeletonAttackNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(Payload.ID, Payload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payload.ID, (payload, context) -> {
            context.client().execute(() ->
                    WitherSkeletonAttackClientState.set(payload.playerUuid(), payload.attacking())
            );
        });
    }

    public static void broadcastAttacking(ServerPlayerEntity player, boolean attacking) {
        if (player.getServer() == null) return;
        var pkt = new Payload(player.getUuid(), attacking);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    public record Payload(UUID playerUuid, boolean attacking) implements CustomPayload {
        public static final Id<Payload> ID = new Id<>(WITHER_SKEL_ATTACK_ID);

        public static final PacketCodec<RegistryByteBuf, Payload> CODEC =
                PacketCodec.of(Payload::write, Payload::read);

        private static Payload read(RegistryByteBuf buf) {
            return new Payload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.attacking);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
