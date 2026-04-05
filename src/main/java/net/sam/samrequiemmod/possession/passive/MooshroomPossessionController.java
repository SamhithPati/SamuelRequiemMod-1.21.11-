package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MooshroomPossessionController {

    private MooshroomPossessionController() {}

    /** Players currently with brown mooshroom variant (struck by lightning). */
    private static final Set<UUID> BROWN_MOOSHROOM_PLAYERS = ConcurrentHashMap.newKeySet();

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isMooshroomPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.MOOSHROOM;
    }

    public static boolean isBrownMooshroom(UUID uuid) {
        return BROWN_MOOSHROOM_PLAYERS.contains(uuid);
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {
        // Damage handling: lightning detection + brown variant change
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isMooshroomPossessing(player)) return true;

            // Lightning strike → become brown mooshroom
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                BROWN_MOOSHROOM_PLAYERS.add(player.getUuid());
                // Broadcast color state to clients
                MooshroomNetworking.broadcastColorSync(player, true);
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_COW_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                return false; // don't take the lightning damage
            }

            return true;
        });
    }

    // ── Unpossession cleanup ───────────────────────────────────────────────────

    public static void onUnpossess(UUID playerUuid) {
        BROWN_MOOSHROOM_PLAYERS.remove(playerUuid);
    }
}






