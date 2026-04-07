package net.sam.samrequiemmod.possession.aquatic;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;

public final class NautilusNetworking {

    private static final Identifier DASH_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "nautilus_dash");

    private NautilusNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(DashPayload.ID, DashPayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(DashPayload.ID, (payload, context) ->
                context.server().execute(() ->
                        NautilusPossessionController.handleDashRequest(context.player(), payload.chargeTicks())));
    }

    public record DashPayload(int chargeTicks) implements CustomPayload {
        public static final Id<DashPayload> ID = new Id<>(DASH_ID);
        public static final PacketCodec<RegistryByteBuf, DashPayload> CODEC =
                PacketCodec.of(DashPayload::write, DashPayload::read);

        private static DashPayload read(RegistryByteBuf buf) {
            return new DashPayload(buf.readVarInt());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeVarInt(chargeTicks);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
