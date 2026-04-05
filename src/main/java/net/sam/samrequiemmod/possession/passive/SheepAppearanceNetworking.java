package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.UUID;

public final class SheepAppearanceNetworking {

    private static final Identifier SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "sheep_appearance_sync");

    private SheepAppearanceNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(SheepAppearancePayload.ID, SheepAppearancePayload.CODEC);
    }

    public static void registerServer() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SheepAppearancePayload.ID, (payload, context) ->
                context.client().execute(() ->
                        SheepAppearanceState.setClientAppearance(
                                payload.playerUuid(),
                                payload.color(),
                                payload.sheared()
                        )));
    }

    public static void broadcast(ServerPlayerEntity player, DyeColor color, boolean sheared) {
        if (player.getEntityWorld().getServer() == null) return;
        var payload = new SheepAppearancePayload(player.getUuid(), color, sheared);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record SheepAppearancePayload(UUID playerUuid, DyeColor color, boolean sheared) implements CustomPayload {
        public static final Id<SheepAppearancePayload> ID = new Id<>(SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, SheepAppearancePayload> CODEC =
                PacketCodec.of(SheepAppearancePayload::write, SheepAppearancePayload::read);

        private static SheepAppearancePayload read(RegistryByteBuf buf) {
            return new SheepAppearancePayload(buf.readUuid(), DyeColor.byIndex(buf.readVarInt()), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(color.getIndex());
            buf.writeBoolean(sheared);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
