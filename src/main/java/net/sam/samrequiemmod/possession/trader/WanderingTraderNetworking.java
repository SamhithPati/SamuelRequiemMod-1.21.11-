package net.sam.samrequiemmod.possession.trader;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.UUID;

public final class WanderingTraderNetworking {

    private static final Identifier SUMMON_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wandering_trader_summon");
    private static final Identifier DRINK_SYNC_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "wandering_trader_drink_sync");

    private WanderingTraderNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(SummonLlamasPayload.ID, SummonLlamasPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DrinkSyncPayload.ID, DrinkSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SummonLlamasPayload.ID, (payload, context) ->
                context.server().execute(() -> WanderingTraderPossessionController.handleSummonKey(context.player())));
    }

    public static void registerClient() {
        KeyBinding summonKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.samrequiemmod.wandering_trader_llamas",
                InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_Y,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (ClientPossessionState.get(client.player) != EntityType.WANDERING_TRADER) return;

            while (summonKey.wasPressed()) {
                ClientPlayNetworking.send(new SummonLlamasPayload());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(DrinkSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    long localEndTick = payload.durationTicks();
                    if (context.client().world != null) {
                        var targetPlayer = context.client().world.getPlayerByUuid(payload.playerUuid());
                        if (targetPlayer != null) {
                            localEndTick = targetPlayer.age + payload.durationTicks();
                        } else if (context.client().player != null) {
                            localEndTick = context.client().player.age + payload.durationTicks();
                        }
                    } else if (context.client().player != null) {
                        localEndTick = context.client().player.age + payload.durationTicks();
                    }
                    WanderingTraderState.startClientDrink(payload.playerUuid(), payload.drinkType(), localEndTick);
                }));
    }

    public static void broadcastDrink(ServerPlayerEntity player, int drinkType, int durationTicks) {
        if (player.getEntityWorld().getServer() == null) return;
        DrinkSyncPayload payload = new DrinkSyncPayload(player.getUuid(), drinkType, durationTicks);
        for (ServerPlayerEntity recipient : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(recipient, payload);
        }
    }

    public record SummonLlamasPayload() implements CustomPayload {
        public static final Id<SummonLlamasPayload> ID = new Id<>(SUMMON_ID);
        public static final PacketCodec<RegistryByteBuf, SummonLlamasPayload> CODEC =
                PacketCodec.of((payload, buf) -> {}, buf -> new SummonLlamasPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DrinkSyncPayload(UUID playerUuid, int drinkType, int durationTicks) implements CustomPayload {
        public static final Id<DrinkSyncPayload> ID = new Id<>(DRINK_SYNC_ID);
        public static final PacketCodec<RegistryByteBuf, DrinkSyncPayload> CODEC =
                PacketCodec.of(DrinkSyncPayload::write, DrinkSyncPayload::read);

        private static DrinkSyncPayload read(RegistryByteBuf buf) {
            return new DrinkSyncPayload(buf.readUuid(), buf.readByte(), buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeByte(drinkType);
            buf.writeVarInt(durationTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
