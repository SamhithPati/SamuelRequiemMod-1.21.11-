package net.sam.samrequiemmod.possession.illager;

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

public final class WitchNetworking {

    public static final Identifier POTION_THROW_ID  = Identifier.of(SamuelRequiemMod.MOD_ID, "witch_potion_throw");
    public static final Identifier DRINKING_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "witch_drinking_sync");

    private WitchNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(PotionThrowPayload.ID, PotionThrowPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DrinkingSyncPayload.ID, DrinkingSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(PotionThrowPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        WitchPossessionController.handlePotionThrow(context.player(), payload.targetUuid())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(DrinkingSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        WitchClientState.setDrinking(payload.playerUuid(), payload.drinking())));
    }

    public static void broadcastDrinking(ServerPlayerEntity player, boolean drinking) {
        if (player.getEntityWorld().getServer() == null) return;
        var pkt = new DrinkingSyncPayload(player.getUuid(), drinking);
        for (ServerPlayerEntity r : player.getEntityWorld().getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ───────────────────────────────────────────────────────────────

    /** Client → Server: player left-clicked a target while possessing witch. */
    public record PotionThrowPayload(UUID targetUuid) implements CustomPayload {
        public static final Id<PotionThrowPayload> ID = new Id<>(POTION_THROW_ID);
        public static final PacketCodec<RegistryByteBuf, PotionThrowPayload> CODEC =
                PacketCodec.of(PotionThrowPayload::write, PotionThrowPayload::read);

        private static PotionThrowPayload read(RegistryByteBuf buf) {
            return new PotionThrowPayload(buf.readUuid());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(targetUuid);
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Server → Client: sync drinking animation state for a witch-possessed player. */
    public record DrinkingSyncPayload(UUID playerUuid, boolean drinking) implements CustomPayload {
        public static final Id<DrinkingSyncPayload> ID = new Id<>(DRINKING_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, DrinkingSyncPayload> CODEC =
                PacketCodec.of(DrinkingSyncPayload::write, DrinkingSyncPayload::read);

        private static DrinkingSyncPayload read(RegistryByteBuf buf) {
            return new DrinkingSyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(drinking);
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}






