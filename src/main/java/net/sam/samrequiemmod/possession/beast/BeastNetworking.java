package net.sam.samrequiemmod.possession.beast;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.client.GuardianHudRenderer;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.Optional;
import java.util.UUID;

public final class BeastNetworking {

    private static final Identifier HORSE_VARIANT_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_horse_variant");
    private static final Identifier RABBIT_VARIANT_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_rabbit_variant");
    private static final Identifier AXOLOTL_VARIANT_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_axolotl_variant");
    private static final Identifier SHULKER_ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_shulker_attack");
    private static final Identifier SHULKER_OPEN_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_shulker_open");
    private static final Identifier AXOLOTL_PLAY_DEAD_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_axolotl_play_dead");
    private static final Identifier SNOWBALL_ATTACK_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_snowball_attack");
    private static final Identifier BEE_ANGRY_TOGGLE_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_bee_angry_toggle");
    private static final Identifier BEE_ANGRY_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_bee_angry_sync");
    private static final Identifier PARROT_FLY_TOGGLE_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_parrot_fly_toggle");
    private static final Identifier PARROT_FLY_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "beast_parrot_fly_sync");

    private static boolean wasAttackDown = false;
    private static boolean wasYDown = false;
    private static boolean wasJumpDown = false;
    private static long lastParrotJumpTapMs = 0L;

    private BeastNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(HorseVariantPayload.ID, HorseVariantPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RabbitVariantPayload.ID, RabbitVariantPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AxolotlVariantPayload.ID, AxolotlVariantPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ShulkerAttackPayload.ID, ShulkerAttackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ShulkerOpenPayload.ID, ShulkerOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AxolotlPlayDeadPayload.ID, AxolotlPlayDeadPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SnowballAttackPayload.ID, SnowballAttackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BeeAngryTogglePayload.ID, BeeAngryTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeeAngrySyncPayload.ID, BeeAngrySyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ParrotFlyTogglePayload.ID, ParrotFlyTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ParrotFlySyncPayload.ID, ParrotFlySyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ShulkerAttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        BeastPossessionController.handleShulkerAttack(context.player(), payload.targetUuid().orElse(null))));
        ServerPlayNetworking.registerGlobalReceiver(SnowballAttackPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        BeastPossessionController.handleSnowballAttack(context.player(), payload.targetUuid().orElse(null))));
        ServerPlayNetworking.registerGlobalReceiver(BeeAngryTogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        BeastPossessionController.handleBeeAngryToggle(context.player())));
        ServerPlayNetworking.registerGlobalReceiver(ParrotFlyTogglePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        BeastPossessionController.handleParrotFlightToggle(context.player())));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(HorseVariantPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientHorseVariant(payload.playerUuid(), payload.variant())));
        ClientPlayNetworking.registerGlobalReceiver(RabbitVariantPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientRabbitVariant(payload.playerUuid(), payload.variant())));
        ClientPlayNetworking.registerGlobalReceiver(AxolotlVariantPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientAxolotlVariant(payload.playerUuid(), payload.variant())));
        ClientPlayNetworking.registerGlobalReceiver(ShulkerOpenPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientShulkerOpenUntil(payload.playerUuid(), payload.untilTick())));
        ClientPlayNetworking.registerGlobalReceiver(AxolotlPlayDeadPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientAxolotlPlayDeadUntil(payload.playerUuid(), payload.untilTick())));
        ClientPlayNetworking.registerGlobalReceiver(BeeAngrySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientBeeAngry(payload.playerUuid(), payload.angry())));
        ClientPlayNetworking.registerGlobalReceiver(ParrotFlySyncPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        BeastState.setClientParrotFlying(payload.playerUuid(), payload.flying())));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                wasAttackDown = false;
                wasYDown = false;
                wasJumpDown = false;
                lastParrotJumpTapMs = 0L;
                return;
            }
            EntityType<?> type = ClientPossessionState.get(client.player);
            long window = client.getWindow().getHandle();

            boolean yDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_Y)
                    == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            if (type == EntityType.BEE && yDown && !wasYDown && client.currentScreen == null) {
                ClientPlayNetworking.send(new BeeAngryTogglePayload());
            }
            wasYDown = yDown;

            boolean jumpDown = client.options.jumpKey.isPressed();
            if (type == EntityType.PARROT && jumpDown && !wasJumpDown && client.currentScreen == null) {
                long now = net.minecraft.util.Util.getMeasuringTimeMs();
                if (now - lastParrotJumpTapMs <= 300L) {
                    ClientPlayNetworking.send(new ParrotFlyTogglePayload());
                    lastParrotJumpTapMs = 0L;
                } else {
                    lastParrotJumpTapMs = now;
                }
            } else if (type != EntityType.PARROT) {
                lastParrotJumpTapMs = 0L;
            }
            wasJumpDown = jumpDown;

            if (type == EntityType.SHULKER) {
                boolean attackDown = client.options.attackKey.isPressed();
                if (attackDown && !wasAttackDown && client.currentScreen == null) {
                    LivingEntity target = GuardianHudRenderer.raycastTarget(client, 20.0);
                    ClientPlayNetworking.send(new ShulkerAttackPayload(
                            target == null ? Optional.empty() : Optional.of(target.getUuid())));
                }
                wasAttackDown = attackDown;
                return;
            }

            boolean attackDown = client.options.attackKey.isPressed();
            if (type == EntityType.SNOW_GOLEM && attackDown && !wasAttackDown && client.currentScreen == null) {
                LivingEntity target = GuardianHudRenderer.raycastTarget(client, 15.0);
                ClientPlayNetworking.send(new SnowballAttackPayload(
                        target == null ? Optional.empty() : Optional.of(target.getUuid())));
            }
            wasAttackDown = attackDown;
        });
    }

    public static void broadcastHorseVariant(ServerPlayerEntity player, int variant) {
        if (player.getServer() == null) return;
        HorseVariantPayload payload = new HorseVariantPayload(player.getUuid(), variant);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastRabbitVariant(ServerPlayerEntity player, int variant) {
        if (player.getServer() == null) return;
        RabbitVariantPayload payload = new RabbitVariantPayload(player.getUuid(), variant);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastAxolotlVariant(ServerPlayerEntity player, int variant) {
        if (player.getServer() == null) return;
        AxolotlVariantPayload payload = new AxolotlVariantPayload(player.getUuid(), variant);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastShulkerOpen(ServerPlayerEntity player, long untilTick) {
        if (player.getServer() == null) return;
        ShulkerOpenPayload payload = new ShulkerOpenPayload(player.getUuid(), untilTick);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastAxolotlPlayDead(ServerPlayerEntity player, long untilTick) {
        if (player.getServer() == null) return;
        AxolotlPlayDeadPayload payload = new AxolotlPlayDeadPayload(player.getUuid(), untilTick);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastBeeAngry(ServerPlayerEntity player, boolean angry) {
        if (player.getServer() == null) return;
        BeeAngrySyncPayload payload = new BeeAngrySyncPayload(player.getUuid(), angry);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public static void broadcastParrotFlying(ServerPlayerEntity player, boolean flying) {
        if (player.getServer() == null) return;
        ParrotFlySyncPayload payload = new ParrotFlySyncPayload(player.getUuid(), flying);
        for (ServerPlayerEntity recipient : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record HorseVariantPayload(UUID playerUuid, int variant) implements CustomPayload {
        public static final Id<HorseVariantPayload> ID = new Id<>(HORSE_VARIANT_ID);
        public static final PacketCodec<RegistryByteBuf, HorseVariantPayload> CODEC =
                PacketCodec.of(HorseVariantPayload::write, HorseVariantPayload::read);

        private static HorseVariantPayload read(RegistryByteBuf buf) {
            return new HorseVariantPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(variant);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record RabbitVariantPayload(UUID playerUuid, int variant) implements CustomPayload {
        public static final Id<RabbitVariantPayload> ID = new Id<>(RABBIT_VARIANT_ID);
        public static final PacketCodec<RegistryByteBuf, RabbitVariantPayload> CODEC =
                PacketCodec.of(RabbitVariantPayload::write, RabbitVariantPayload::read);

        private static RabbitVariantPayload read(RegistryByteBuf buf) {
            return new RabbitVariantPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(variant);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AxolotlVariantPayload(UUID playerUuid, int variant) implements CustomPayload {
        public static final Id<AxolotlVariantPayload> ID = new Id<>(AXOLOTL_VARIANT_ID);
        public static final PacketCodec<RegistryByteBuf, AxolotlVariantPayload> CODEC =
                PacketCodec.of(AxolotlVariantPayload::write, AxolotlVariantPayload::read);

        private static AxolotlVariantPayload read(RegistryByteBuf buf) {
            return new AxolotlVariantPayload(buf.readUuid(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarInt(variant);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ShulkerAttackPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<ShulkerAttackPayload> ID = new Id<>(SHULKER_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, ShulkerAttackPayload> CODEC =
                PacketCodec.of(ShulkerAttackPayload::write, ShulkerAttackPayload::read);

        private static ShulkerAttackPayload read(RegistryByteBuf buf) {
            return new ShulkerAttackPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) buf.writeUuid(targetUuid.get());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ShulkerOpenPayload(UUID playerUuid, long untilTick) implements CustomPayload {
        public static final Id<ShulkerOpenPayload> ID = new Id<>(SHULKER_OPEN_ID);
        public static final PacketCodec<RegistryByteBuf, ShulkerOpenPayload> CODEC =
                PacketCodec.of(ShulkerOpenPayload::write, ShulkerOpenPayload::read);

        private static ShulkerOpenPayload read(RegistryByteBuf buf) {
            return new ShulkerOpenPayload(buf.readUuid(), buf.readVarLong());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarLong(untilTick);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record AxolotlPlayDeadPayload(UUID playerUuid, long untilTick) implements CustomPayload {
        public static final Id<AxolotlPlayDeadPayload> ID = new Id<>(AXOLOTL_PLAY_DEAD_ID);
        public static final PacketCodec<RegistryByteBuf, AxolotlPlayDeadPayload> CODEC =
                PacketCodec.of(AxolotlPlayDeadPayload::write, AxolotlPlayDeadPayload::read);

        private static AxolotlPlayDeadPayload read(RegistryByteBuf buf) {
            return new AxolotlPlayDeadPayload(buf.readUuid(), buf.readVarLong());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeVarLong(untilTick);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record SnowballAttackPayload(Optional<UUID> targetUuid) implements CustomPayload {
        public static final Id<SnowballAttackPayload> ID = new Id<>(SNOWBALL_ATTACK_ID);
        public static final PacketCodec<RegistryByteBuf, SnowballAttackPayload> CODEC =
                PacketCodec.of(SnowballAttackPayload::write, SnowballAttackPayload::read);

        private static SnowballAttackPayload read(RegistryByteBuf buf) {
            return new SnowballAttackPayload(buf.readBoolean() ? Optional.of(buf.readUuid()) : Optional.empty());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(targetUuid.isPresent());
            if (targetUuid.isPresent()) buf.writeUuid(targetUuid.get());
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BeeAngryTogglePayload() implements CustomPayload {
        public static final Id<BeeAngryTogglePayload> ID = new Id<>(BEE_ANGRY_TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, BeeAngryTogglePayload> CODEC =
                PacketCodec.of(BeeAngryTogglePayload::write, BeeAngryTogglePayload::read);

        private static BeeAngryTogglePayload read(RegistryByteBuf buf) {
            buf.readByte();
            return new BeeAngryTogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record BeeAngrySyncPayload(UUID playerUuid, boolean angry) implements CustomPayload {
        public static final Id<BeeAngrySyncPayload> ID = new Id<>(BEE_ANGRY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, BeeAngrySyncPayload> CODEC =
                PacketCodec.of(BeeAngrySyncPayload::write, BeeAngrySyncPayload::read);

        private static BeeAngrySyncPayload read(RegistryByteBuf buf) {
            return new BeeAngrySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(angry);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ParrotFlyTogglePayload() implements CustomPayload {
        public static final Id<ParrotFlyTogglePayload> ID = new Id<>(PARROT_FLY_TOGGLE_ID);
        public static final PacketCodec<RegistryByteBuf, ParrotFlyTogglePayload> CODEC =
                PacketCodec.of(ParrotFlyTogglePayload::write, ParrotFlyTogglePayload::read);

        private static ParrotFlyTogglePayload read(RegistryByteBuf buf) {
            buf.readByte();
            return new ParrotFlyTogglePayload();
        }

        private void write(RegistryByteBuf buf) {
            buf.writeByte(0);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record ParrotFlySyncPayload(UUID playerUuid, boolean flying) implements CustomPayload {
        public static final Id<ParrotFlySyncPayload> ID = new Id<>(PARROT_FLY_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, ParrotFlySyncPayload> CODEC =
                PacketCodec.of(ParrotFlySyncPayload::write, ParrotFlySyncPayload::read);

        private static ParrotFlySyncPayload read(RegistryByteBuf buf) {
            return new ParrotFlySyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(flying);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}
