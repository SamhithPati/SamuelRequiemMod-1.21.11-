package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChickenJumpNetworking {

    private static final Identifier CHICKEN_JUMP_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "chicken_jump");

    /** Server-side set of players currently holding jump while riding a chicken. */
    public static final Set<UUID> JUMP_REQUESTED = ConcurrentHashMap.newKeySet();

    private ChickenJumpNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(ChickenJumpPayload.ID, ChickenJumpPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ChickenJumpPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!ChickenRidingHandler.isBabyUndead(player)) return;
                    if (!(player.getVehicle() instanceof ChickenEntity)) return;
                    if (payload.jumping()) {
                        JUMP_REQUESTED.add(player.getUuid());
                    } else {
                        JUMP_REQUESTED.remove(player.getUuid());
                    }
                }));
    }

    public static void registerClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) return;
            if (!(player.getVehicle() instanceof ChickenEntity)) return;

            boolean jumping = client.options.jumpKey.isPressed();
            ClientPlayNetworking.send(new ChickenJumpPayload(jumping));
        });
    }

    public record ChickenJumpPayload(boolean jumping) implements CustomPayload {
        public static final Id<ChickenJumpPayload> ID = new Id<>(CHICKEN_JUMP_ID);
        public static final PacketCodec<RegistryByteBuf, ChickenJumpPayload> CODEC =
                PacketCodec.of(ChickenJumpPayload::write, ChickenJumpPayload::read);

        private static ChickenJumpPayload read(RegistryByteBuf buf) {
            return new ChickenJumpPayload(buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(jumping);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}






