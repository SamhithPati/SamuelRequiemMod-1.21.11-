package net.sam.samrequiemmod.possession.creeper;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.UUID;

public final class CreeperNetworking {

    private static final Identifier CHARGE_TOGGLE_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "creeper_charge_toggle");
    private static final Identifier CHARGE_SYNC_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "creeper_charge_sync");

    private CreeperNetworking() {}

    // ── Common registration (both sides) ────────────────────────────────────

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(ChargeTogglePayload.ID, ChargeTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChargeSyncPayload.ID, ChargeSyncPayload.CODEC);
    }

    // ── Server registration ─────────────────────────────────────────────────

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ChargeTogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        CreeperPossessionController.handleChargeToggle(context.player())));
    }

    // ── Client registration ─────────────────────────────────────────────────

    /** Edge-detection flag for attack key polling. */
    private static boolean wasAttackKeyDown = false;

    public static void registerClient() {
        // Client tick: detect attack key press to toggle charge
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            var possType = ClientPossessionState.get(client.player);
            if (possType != EntityType.CREEPER) {
                wasAttackKeyDown = false;
                return;
            }

            // Detect rising edge of attack key
            boolean attackDown = client.options.attackKey.isPressed();
            if (attackDown && !wasAttackKeyDown && client.currentScreen == null) {
                ClientPlayNetworking.send(new ChargeTogglePayload());
            }
            wasAttackKeyDown = attackDown;
        });

        // Charge sync receiver
        ClientPlayNetworking.registerGlobalReceiver(ChargeSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    CreeperClientState.setCharging(payload.playerUuid(), payload.charging(), payload.fuseTicks());
                    CreeperClientState.setCharged(payload.playerUuid(), payload.charged());
                }));
    }

    // ── Broadcast charge state to all clients ────────────────────────────────

    public static void broadcastChargeSync(ServerPlayerEntity player, boolean charging, int fuseTicks, boolean charged) {
        if (player.getServer() == null) return;
        var pkt = new ChargeSyncPayload(player.getUuid(), charging, fuseTicks, charged);
        for (ServerPlayerEntity r : player.getServer().getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(r, pkt);
    }

    // ── Payloads ────────────────────────────────────────────────────────────

    /** Client -> Server: toggle charge state. */
    public record ChargeTogglePayload() implements CustomPayload {
        public static final Id<ChargeTogglePayload> ID = new Id<>(CHARGE_TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, ChargeTogglePayload> CODEC =
                PacketCodec.of(ChargeTogglePayload::write, ChargeTogglePayload::read);

        private static ChargeTogglePayload read(RegistryByteBuf buf) {
            buf.readByte(); // dummy
            return new ChargeTogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0); // dummy byte — some codecs don't like zero-length payloads
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    /** Server -> Client: sync charge state for a player. */
    public record ChargeSyncPayload(UUID playerUuid, boolean charging, int fuseTicks, boolean charged) implements CustomPayload {
        public static final Id<ChargeSyncPayload> ID = new Id<>(CHARGE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, ChargeSyncPayload> CODEC =
                PacketCodec.of(ChargeSyncPayload::write, ChargeSyncPayload::read);

        private static ChargeSyncPayload read(RegistryByteBuf buf) {
            return new ChargeSyncPayload(buf.readUuid(), buf.readBoolean(), buf.readInt(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(charging);
            buf.writeInt(fuseTicks);
            buf.writeBoolean(charged);
        }

        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
