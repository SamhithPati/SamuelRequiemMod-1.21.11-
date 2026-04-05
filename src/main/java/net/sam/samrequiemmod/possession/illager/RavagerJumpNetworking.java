package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RavagerJumpNetworking {

    private static final Identifier RAVAGER_JUMP_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_jump");

    /** Server-side set of players currently holding jump while riding a ravager. */
    public static final Set<UUID> JUMP_REQUESTED = ConcurrentHashMap.newKeySet();

    private RavagerJumpNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(RavagerJumpPayload.ID, RavagerJumpPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(RavagerJumpPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!RavagerRidingHandler.isIllagerPossessed(player)) return;
                    if (!(player.getVehicle() instanceof RavagerEntity)) return;
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
            if (!(player.getVehicle() instanceof RavagerEntity)) return;

            boolean jumping = client.options.jumpKey.isPressed();
            ClientPlayNetworking.send(new RavagerJumpPayload(jumping));
        });
    }

    public record RavagerJumpPayload(boolean jumping) implements CustomPayload {
        public static final Id<RavagerJumpPayload> ID = new Id<>(RAVAGER_JUMP_ID);
        public static final PacketCodec<RegistryByteBuf, RavagerJumpPayload> CODEC =
                PacketCodec.of(RavagerJumpPayload::write, RavagerJumpPayload::read);

        private static RavagerJumpPayload read(RegistryByteBuf buf) {
            return new RavagerJumpPayload(buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(jumping);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}






