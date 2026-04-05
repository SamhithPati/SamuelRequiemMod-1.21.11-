package net.sam.samrequiemmod.possession;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PossessionHurtSoundHelper {

    private static final Map<UUID, Long> LAST_HURT_SOUND_TICK = new ConcurrentHashMap<>();
    private static final long HURT_SOUND_COOLDOWN_TICKS = 10L;

    private PossessionHurtSoundHelper() {
    }

    public static void playIfReady(ServerPlayerEntity player, SoundEvent sound, float pitch) {
        if (sound == null || !shouldPlay(player)) {
            return;
        }
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundCategory.PLAYERS,
                1.0f,
                pitch
        );
    }

    public static void playIfReady(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, float pitch) {
        if (sound == null || !shouldPlay(player)) {
            return;
        }
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundCategory.PLAYERS,
                1.0f,
                pitch
        );
    }

    public static void clear(UUID playerUuid) {
        LAST_HURT_SOUND_TICK.remove(playerUuid);
    }

    private static boolean shouldPlay(ServerPlayerEntity player) {
        long now = player.age;
        Long last = LAST_HURT_SOUND_TICK.get(player.getUuid());
        if (last != null && now - last < HURT_SOUND_COOLDOWN_TICKS) {
            return false;
        }
        LAST_HURT_SOUND_TICK.put(player.getUuid(), now);
        return true;
    }
}
