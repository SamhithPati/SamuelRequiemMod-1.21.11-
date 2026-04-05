package net.sam.samrequiemmod.possession.warden;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.client.WardenHudRenderer;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.Optional;
import java.util.UUID;

public final class WardenNetworking {

    private static final Identifier SONIC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "warden_sonic");
    private static final Identifier ROAR_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "warden_roar");
    private static final Identifier ANIM_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "warden_anim_sync");

    private WardenNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(SonicBoomPayload.ID, SonicBoomPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RoarPayload.ID, RoarPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimationSyncPayload.ID, AnimationSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SonicBoomPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        WardenPossessionController.handleSonicBoom(context.player(), payload.targetUuid().orElse(null))));
        ServerPlayNetworking.registerGlobalReceiver(RoarPayload.ID, (payload, context) ->
                context.server().execute(() -> WardenPossessionController.handleRoar(context.player())));
    }

    public static void registerClient() {
        KeyBinding roarKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.samrequiemmod.warden_roar",
                InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_Y,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.WARDEN) return;

            while (roarKey.wasPressed()) {
                WardenClientState.startRoar(client.player.getUuid(), client.player.age);
                ClientPlayNetworking.send(new RoarPayload());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(AnimationSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    int localStartTick = payload.playerAge();
                    if (context.client().world != null) {
                        PlayerEntity targetPlayer = context.client().world.getPlayerByUuid(payload.playerUuid());
                        if (targetPlayer != null) {
                            localStartTick = targetPlayer.age;
                        } else if (context.client().player != null) {
                            localStartTick = context.client().player.age;
                        }
                    } else if (context.client().player != null) {
                        localStartTick = context.client().player.age;
                    }
                    switch (payload.animationType()) {
                        case 1 -> WardenClientState.startAttack(payload.playerUuid(), localStartTick);
                        case 2 -> WardenClientState.startSonic(payload.playerUuid(), localStartTick);
                        case 3 -> WardenClientState.startRoar(payload.playerUuid(), localStartTick);
                        case 4 -> WardenClientState.startSniff(payload.playerUuid(), localStartTick);
                        default -> WardenClientState.clear(payload.playerUuid());
                    }
                }));
    }

    public static void broadcastAnimation(ServerPlayerEntity player, int animationType) {
        if (player.getEntityWorld().getServer() == null) return;
        AnimationSyncPayload payload = new AnimationSyncPayload(player.getUuid(), animationType, player.age);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record SonicBoomPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<SonicBoomPayload> ID = new Id<>(SONIC_ID);
        public static final PacketCodec<RegistryByteBuf, SonicBoomPayload> CODEC =
                PacketCodec.of(SonicBoomPayload::write, SonicBoomPayload::read);

        private static SonicBoomPayload read(RegistryByteBuf buf) {
            return new SonicBoomPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) buf.writeUuid(targetUuid.get());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RoarPayload() implements CustomPayload {
        public static final Id<RoarPayload> ID = new Id<>(ROAR_ID);
        public static final PacketCodec<RegistryByteBuf, RoarPayload> CODEC =
                PacketCodec.of((payload, buf) -> {}, buf -> new RoarPayload());

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AnimationSyncPayload(UUID playerUuid, int animationType, int playerAge) implements CustomPayload {
        public static final Id<AnimationSyncPayload> ID = new Id<>(ANIM_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, AnimationSyncPayload> CODEC =
                PacketCodec.of(AnimationSyncPayload::write, AnimationSyncPayload::read);

        private static AnimationSyncPayload read(RegistryByteBuf buf) {
            return new AnimationSyncPayload(buf.readUuid(), buf.readByte(), buf.readInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeByte(animationType);
            buf.writeInt(playerAge);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
