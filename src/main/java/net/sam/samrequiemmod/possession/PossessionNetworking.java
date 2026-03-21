package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PossessionNetworking {

    public static final Identifier POSSESSION_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "possession_sync");

    private PossessionNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(PossessionSyncPayload.ID, PossessionSyncPayload.CODEC);
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(PossessionSyncPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                EntityType<?> type = null;

                if (payload.entityTypeId() != null && Registries.ENTITY_TYPE.containsId(payload.entityTypeId())) {
                    type = Registries.ENTITY_TYPE.get(payload.entityTypeId());
                }

                ClientPossessionState.set(payload.playerUuid(), type);
            });
        });
    }

    public static void sendPossessionSync(ServerPlayerEntity recipient, UUID playerUuid, @Nullable EntityType<?> type) {
        Identifier entityTypeId = type == null ? null : EntityType.getId(type);
        ServerPlayNetworking.send(recipient, new PossessionSyncPayload(playerUuid, entityTypeId));
    }

    public static void broadcastPossessionSync(ServerPlayerEntity player, @Nullable EntityType<?> type) {
        if (player.getServer() == null) {
            return;
        }

        UUID playerUuid = player.getUuid();

        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            sendPossessionSync(recipient, playerUuid, type);
        }
    }

    public record PossessionSyncPayload(UUID playerUuid, @Nullable Identifier entityTypeId) implements CustomPayload {
        public static final Id<PossessionSyncPayload> ID = new Id<>(POSSESSION_SYNC_ID);

        public static final PacketCodec<RegistryByteBuf, PossessionSyncPayload> CODEC =
                PacketCodec.of(PossessionSyncPayload::write, PossessionSyncPayload::read);

        private static PossessionSyncPayload read(RegistryByteBuf buf) {
            UUID playerUuid = buf.readUuid();
            boolean hasType = buf.readBoolean();
            Identifier entityTypeId = hasType ? buf.readIdentifier() : null;
            return new PossessionSyncPayload(playerUuid, entityTypeId);
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.entityTypeId != null);
            if (this.entityTypeId != null) {
                buf.writeIdentifier(this.entityTypeId);
            }
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}