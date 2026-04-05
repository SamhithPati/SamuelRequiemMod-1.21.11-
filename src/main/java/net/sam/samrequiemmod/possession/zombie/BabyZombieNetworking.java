package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.UUID;

public final class BabyZombieNetworking {

    public static final Identifier BABY_ZOMBIE_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "baby_zombie_sync");

    private BabyZombieNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(BabyZombiePayload.ID, BabyZombiePayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BabyZombiePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    // Update the client-side baby state
                    BabyZombieState.setClientBaby(payload.playerUuid(), payload.isBaby());

                    MinecraftClient client = context.client();
                    if (client.world == null) return;

                    // Find the player entity this packet is about and recalculate
                    // their dimensions so the camera eye height updates immediately.
                    // Without this, dimensions stay cached until the next pose change
                    // (e.g. sneaking), which is why height only aligned on sneak.
                    for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                        if (player.getUuid().equals(payload.playerUuid())) {
                            player.calculateDimensions();
                            break;
                        }
                    }
                })
        );
    }

    public static void broadcastBabyZombieSync(ServerPlayerEntity player, boolean isBaby) {
        if (player.getEntityWorld().getServer() == null) return;
        var pkt = new BabyZombiePayload(player.getUuid(), isBaby);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, pkt);
        }
    }

    public record BabyZombiePayload(UUID playerUuid, boolean isBaby) implements CustomPayload {
        public static final Id<BabyZombiePayload> ID = new Id<>(BABY_ZOMBIE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BabyZombiePayload> CODEC =
                PacketCodec.of(BabyZombiePayload::write, BabyZombiePayload::read);

        private static BabyZombiePayload read(RegistryByteBuf buf) {
            return new BabyZombiePayload(buf.readUuid(), buf.readBoolean());
        }
        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.isBaby);
        }
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}





