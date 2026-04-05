package net.sam.samrequiemmod.possession.wolf;

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

public final class WolfNetworking {

    private static final Identifier TOGGLE_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wolf_angry_toggle");
    private static final Identifier ANGRY_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wolf_angry_sync");
    private static final Identifier BABY_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wolf_baby_sync");
    private static final Identifier VARIANT_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wolf_variant_sync");
    private static final Identifier SHAKE_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wolf_shake_sync");

    private static boolean wasYKeyDown = false;

    private WolfNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(AngryTogglePayload.ID, AngryTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AngrySyncPayload.ID, AngrySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BabySyncPayload.ID, BabySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VariantSyncPayload.ID, VariantSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShakeSyncPayload.ID, ShakeSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(AngryTogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        WolfPossessionController.handleAngryToggle(context.player())));
    }

    public static void registerClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasYKeyDown = false;
                return;
            }
            EntityType<?> type = ClientPossessionState.get(client.player);
            if (type != EntityType.WOLF) {
                wasYKeyDown = false;
                return;
            }

            long window = client.getWindow().getHandle();
            boolean yDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_Y)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (yDown && !wasYKeyDown && client.currentScreen == null) {
                ClientPlayNetworking.send(new AngryTogglePayload());
            }
            wasYKeyDown = yDown;
        });

        ClientPlayNetworking.registerGlobalReceiver(AngrySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        WolfState.setClientAngry(payload.playerUuid(), payload.angry())));
        ClientPlayNetworking.registerGlobalReceiver(BabySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        WolfBabyState.setClientBaby(payload.playerUuid(), payload.baby())));
        ClientPlayNetworking.registerGlobalReceiver(VariantSyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        WolfState.setClientVariant(payload.playerUuid(), payload.variantId())));
        ClientPlayNetworking.registerGlobalReceiver(ShakeSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().player != null) {
                        WolfState.startClientShake(payload.playerUuid(), context.client().player.age, payload.durationTicks());
                    }
                }));
    }

    public static void broadcastAngry(ServerPlayerEntity player, boolean angry) {
        if (player.getEntityWorld().getServer() == null) return;
        AngrySyncPayload payload = new AngrySyncPayload(player.getUuid(), angry);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastBaby(ServerPlayerEntity player, boolean baby) {
        if (player.getEntityWorld().getServer() == null) return;
        BabySyncPayload payload = new BabySyncPayload(player.getUuid(), baby);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastVariant(ServerPlayerEntity player, String variantId) {
        if (player.getEntityWorld().getServer() == null) return;
        VariantSyncPayload payload = new VariantSyncPayload(player.getUuid(), variantId);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastShake(ServerPlayerEntity player, int durationTicks) {
        if (player.getEntityWorld().getServer() == null) return;
        ShakeSyncPayload payload = new ShakeSyncPayload(player.getUuid(), durationTicks);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record AngryTogglePayload() implements CustomPayload {
        public static final Id<AngryTogglePayload> ID = new Id<>(TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, AngryTogglePayload> CODEC =
                PacketCodec.of(AngryTogglePayload::write, AngryTogglePayload::read);

        private static AngryTogglePayload read(RegistryByteBuf buf) {
            buf.readByte();
            return new AngryTogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record AngrySyncPayload(UUID playerUuid, boolean angry) implements CustomPayload {
        public static final Id<AngrySyncPayload> ID = new Id<>(ANGRY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, AngrySyncPayload> CODEC =
                PacketCodec.of(AngrySyncPayload::write, AngrySyncPayload::read);

        private static AngrySyncPayload read(RegistryByteBuf buf) {
            return new AngrySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.angry);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BabySyncPayload(UUID playerUuid, boolean baby) implements CustomPayload {
        public static final Id<BabySyncPayload> ID = new Id<>(BABY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BabySyncPayload> CODEC =
                PacketCodec.of(BabySyncPayload::write, BabySyncPayload::read);

        private static BabySyncPayload read(RegistryByteBuf buf) {
            return new BabySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeBoolean(this.baby);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record VariantSyncPayload(UUID playerUuid, String variantId) implements CustomPayload {
        public static final Id<VariantSyncPayload> ID = new Id<>(VARIANT_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, VariantSyncPayload> CODEC =
                PacketCodec.of(VariantSyncPayload::write, VariantSyncPayload::read);

        private static VariantSyncPayload read(RegistryByteBuf buf) {
            return new VariantSyncPayload(buf.readUuid(), buf.readString());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeString(this.variantId);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShakeSyncPayload(UUID playerUuid, int durationTicks) implements CustomPayload {
        public static final Id<ShakeSyncPayload> ID = new Id<>(SHAKE_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, ShakeSyncPayload> CODEC =
                PacketCodec.of(ShakeSyncPayload::write, ShakeSyncPayload::read);

        private static ShakeSyncPayload read(RegistryByteBuf buf) {
            return new ShakeSyncPayload(buf.readUuid(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(this.playerUuid);
            buf.writeInt(this.durationTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}






