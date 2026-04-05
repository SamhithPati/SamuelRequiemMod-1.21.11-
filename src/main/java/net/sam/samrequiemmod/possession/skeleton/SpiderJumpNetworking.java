package net.sam.samrequiemmod.possession.skeleton;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Syncs jump key state from client to server for skeleton-possessed players riding spiders.
 */
public final class SpiderJumpNetworking {

    private static final Identifier SPIDER_JUMP_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "spider_jump");

    /** Server-side set of players currently holding jump while riding a spider. */
    public static final Set<UUID> JUMP_REQUESTED = ConcurrentHashMap.newKeySet();

    private SpiderJumpNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(SpiderJumpPayload.ID, SpiderJumpPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SpiderJumpPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!SkeletonPossessionController.isAnySkeletonPossessing(player)
                            && !WitherSkeletonPossessionController.isWitherSkeletonPossessing(player)) return;
                    if (!(player.getVehicle() instanceof SpiderEntity)) return;
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
            if (!(player.getVehicle() instanceof SpiderEntity)) return;

            boolean jumping = client.options.jumpKey.isPressed();
            ClientPlayNetworking.send(new SpiderJumpPayload(jumping));
        });
    }

    public record SpiderJumpPayload(boolean jumping) implements CustomPayload {
        public static final Id<SpiderJumpPayload> ID = new Id<>(SPIDER_JUMP_ID);
        public static final PacketCodec<RegistryByteBuf, SpiderJumpPayload> CODEC =
                PacketCodec.of(SpiderJumpPayload::write, SpiderJumpPayload::read);

        private static SpiderJumpPayload read(RegistryByteBuf buf) {
            return new SpiderJumpPayload(buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(jumping);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}






