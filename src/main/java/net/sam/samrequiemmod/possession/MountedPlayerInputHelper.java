package net.sam.samrequiemmod.possession;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PlayerInput;

public final class MountedPlayerInputHelper {

    private MountedPlayerInputHelper() {
    }

    public static float getForwardInput(ServerPlayerEntity player) {
        PlayerInput input = player.getPlayerInput();
        if (input.forward() == input.backward()) {
            return 0.0f;
        }
        return input.forward() ? 1.0f : -1.0f;
    }

    public static float getStrafeInput(ServerPlayerEntity player) {
        PlayerInput input = player.getPlayerInput();
        if (input.left() == input.right()) {
            return 0.0f;
        }
        return input.left() ? 1.0f : -1.0f;
    }
}
