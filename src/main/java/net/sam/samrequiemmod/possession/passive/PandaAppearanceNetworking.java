package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.UUID;

public final class PandaAppearanceNetworking {

    private static final Identifier SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "panda_appearance_sync");

    private PandaAppearanceNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(PandaAppearancePayload.ID, PandaAppearancePayload.CODEC);
    }

    public static void registerServer() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(PandaAppearancePayload.ID, (payload, context) ->
                context.client().execute(() ->
                        PandaAppearanceState.setClientAppearance(
                                payload.playerUuid(),
                                payload.mainGene(),
                                payload.hiddenGene()
                        )));
    }

    public static void broadcast(ServerPlayerEntity player, PandaEntity.Gene mainGene, PandaEntity.Gene hiddenGene) {
        if (player.getEntityWorld().getServer() == null) return;
        var payload = new PandaAppearancePayload(player.getUuid(), mainGene, hiddenGene);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record PandaAppearancePayload(UUID playerUuid, PandaEntity.Gene mainGene, PandaEntity.Gene hiddenGene)
            implements CustomPayload {
        public static final Id<PandaAppearancePayload> ID = new Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, PandaAppearancePayload> CODEC =
                PacketCodec.of(PandaAppearancePayload::write, PandaAppearancePayload::read);

        private static PandaAppearancePayload read(RegistryByteBuf buf) {
            return new PandaAppearancePayload(
                    buf.readUuid(),
                    readGene(buf.readVarInt()),
                    readGene(buf.readVarInt())
            );
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(mainGene.ordinal());
            buf.writeVarInt(hiddenGene.ordinal());
        }

        private static PandaEntity.Gene readGene(int index) {
            PandaEntity.Gene[] values = PandaEntity.Gene.values();
            if (index < 0 || index >= values.length) {
                return PandaEntity.Gene.NORMAL;
            }
            return values[index];
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
