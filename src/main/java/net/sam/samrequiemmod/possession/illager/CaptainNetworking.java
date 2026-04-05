package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.client.EvokerHudRenderer;
import net.sam.samrequiemmod.possession.ClientPossessionState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C2S networking for the captain recruit/dismiss system.
 * S2C networking for celebration sync.
 * Client registers a Y keybind; pressing it raycasts for a nearby mob and sends its UUID.
 */
public final class CaptainNetworking {

    private static final Identifier RECRUIT_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "captain_recruit");
    private static final Identifier CELEBRATION_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "captain_celebration");

    private CaptainNetworking() {}

    /** Client-side set of player UUIDs currently celebrating. */
    public static final Set<UUID> CELEBRATING_PLAYERS = ConcurrentHashMap.newKeySet();

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(RecruitPayload.ID, RecruitPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CelebrationSyncPayload.ID, CelebrationSyncPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(RecruitPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        CaptainHandler.handleRecruitPacket(context.player(), payload.targetUuid())));
    }

    public static void registerClient() {
        KeyBinding recruitKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.samrequiemmod.recruit",
                InputUtil.Type.KEYSYM,
                org.lwjgl.glfw.GLFW.GLFW_KEY_Y,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            var possType = ClientPossessionState.get(client.player);
            if (possType != EntityType.PILLAGER
                    && possType != EntityType.VINDICATOR
                    && possType != EntityType.EVOKER) return;

            while (recruitKey.wasPressed()) {
                // Raycast within 6 blocks to find the mob the player is looking at
                LivingEntity target = EvokerHudRenderer.raycastTarget(client, 6.0);
                if (target != null) {
                    ClientPlayNetworking.send(new RecruitPayload(target.getUuid()));
                }
            }
        });

        // Celebration sync receiver
        ClientPlayNetworking.registerGlobalReceiver(CelebrationSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.celebrating()) {
                        CELEBRATING_PLAYERS.add(payload.playerUuid());
                    } else {
                        CELEBRATING_PLAYERS.remove(payload.playerUuid());
                    }
                }));
    }

    // ── Broadcast celebration to all clients ──────────────────────────────────

    public static void broadcastCelebration(ServerPlayerEntity player, boolean celebrating) {
        CelebrationSyncPayload payload = new CelebrationSyncPayload(player.getUuid(), celebrating);
        for (ServerPlayerEntity other : player.getEntityWorld().getPlayers()) {
            ServerPlayNetworking.send(other, payload);
        }
    }

    // ── Payloads ─────────────────────────────────────────────────────────────

    public record RecruitPayload(UUID targetUuid) implements CustomPayload {
        public static final Id<RecruitPayload> ID = new Id<>(RECRUIT_ID);
        public static final PacketCodec<RegistryByteBuf, RecruitPayload> CODEC =
                PacketCodec.of(RecruitPayload::write, RecruitPayload::read);

        private static RecruitPayload read(RegistryByteBuf buf) {
            return new RecruitPayload(buf.readUuid());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(targetUuid);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record CelebrationSyncPayload(UUID playerUuid, boolean celebrating) implements CustomPayload {
        public static final Id<CelebrationSyncPayload> ID = new Id<>(CELEBRATION_ID);
        public static final PacketCodec<RegistryByteBuf, CelebrationSyncPayload> CODEC =
                PacketCodec.of(CelebrationSyncPayload::write, CelebrationSyncPayload::read);

        private static CelebrationSyncPayload read(RegistryByteBuf buf) {
            return new CelebrationSyncPayload(buf.readUuid(), buf.readBoolean());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeUuid(playerUuid);
            buf.writeBoolean(celebrating);
        }

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }
}






