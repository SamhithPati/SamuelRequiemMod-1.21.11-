package net.sam.samrequiemmod.possession.zombie;

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
 * Syncs whether a zombie-possessed player is in "arms raised" (attacking) state
 * so the client can drive the ZombieEntity shell's attackingPlayer field.
 */
public final class ZombieAttackSyncNetworking {

    public static final Identifier ZOMBIE_ATTACK_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "zombie_attack_sync");

    private ZombieAttackSyncNetworking() {
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(ZombieAttackPayload.ID, ZombieAttackPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ZombieAttackPayload.ID, (payload, context) -> {
            context.client().execute(() ->
                    ZombieAttackClientState.set(payload.playerUuid(), payload.attacking())
            );
        });
    }

    // ── Server-side send helpers ──────────────────────────────────────────────

    public static void broadcastZombieAttacking(ServerPlayerEntity player, boolean attacking) {
        if (player.getEntityWorld().getServer() == null) return;
        UUID uuid = player.getUuid();
        var pkt = new ZombieAttackPayload(uuid, attacking);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    // ── Payload ───────────────────────────────────────────────────────────────

    public record ZombieAttackPayload(UUID playerUuid, boolean attacking) implements CustomPayload {
        public static final Id<ZombieAttackPayload> ID = new Id<>(ZOMBIE_ATTACK_SYNC_ID);

        public static final PacketCodec<RegistryByteBuf, ZombieAttackPayload> CODEC =
                PacketCodec.of(ZombieAttackPayload::write, ZombieAttackPayload::read);

        private static ZombieAttackPayload read(RegistryByteBuf buf) {
            UUID uuid = buf.readUuid();
            boolean attacking = buf.readBoolean();
            return new ZombieAttackPayload(uuid, attacking);
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.attacking);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}






