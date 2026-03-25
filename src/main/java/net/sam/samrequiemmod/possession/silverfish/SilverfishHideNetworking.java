package net.sam.samrequiemmod.possession.silverfish;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.sam.samrequiemmod.SamuelRequiemMod;

public final class SilverfishHideNetworking {

    private static final Identifier START_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "silverfish_hide_start");
    private static final Identifier STOP_ID = Identifier.of(SamuelRequiemMod.MOD_ID, "silverfish_hide_stop");

    private SilverfishHideNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.playC2S().register(StartHidePayload.ID, StartHidePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StopHidePayload.ID, StopHidePayload.CODEC);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(StartHidePayload.ID, (payload, context) ->
                context.server().execute(() -> SilverfishPossessionController.beginHideCharge(context.player(), payload.blockPos())));
        ServerPlayNetworking.registerGlobalReceiver(StopHidePayload.ID, (payload, context) ->
                context.server().execute(() -> SilverfishPossessionController.cancelHideCharge(context.player())));
    }

    public static void registerClient() {}

    public record StartHidePayload(BlockPos blockPos) implements CustomPayload {
        public static final Id<StartHidePayload> ID = new Id<>(START_ID);
        public static final PacketCodec<RegistryByteBuf, StartHidePayload> CODEC = PacketCodec.of(StartHidePayload::write, StartHidePayload::read);

        private static StartHidePayload read(RegistryByteBuf buf) {
            return new StartHidePayload(buf.readBlockPos());
        }

        private void write(RegistryByteBuf buf) {
            buf.writeBlockPos(blockPos);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record StopHidePayload() implements CustomPayload {
        public static final Id<StopHidePayload> ID = new Id<>(STOP_ID);
        public static final PacketCodec<RegistryByteBuf, StopHidePayload> CODEC = PacketCodec.unit(new StopHidePayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
