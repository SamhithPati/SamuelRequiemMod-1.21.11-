package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPossessionState {

    private static final Map<UUID, EntityType<?>> POSSESSIONS = new ConcurrentHashMap<>();

    private ClientPossessionState() {
    }

    public static void set(UUID playerUuid, @Nullable EntityType<?> type) {
        if (type == null) {
            POSSESSIONS.remove(playerUuid);
        } else {
            POSSESSIONS.put(playerUuid, type);
        }
    }

    @Nullable
    public static EntityType<?> get(PlayerEntity player) {
        return POSSESSIONS.get(player.getUuid());
    }

    public static boolean isPossessing(PlayerEntity player) {
        return get(player) != null;
    }

    public static void clearAll() {
        POSSESSIONS.clear();
    }
}