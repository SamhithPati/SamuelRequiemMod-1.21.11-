package net.sam.samrequiemmod.possession.zombie_villager;

import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZombieVillagerCureTracker {

    private static final int CURE_TICKS = 20 * 60 * 5;
    private static final Map<UUID, Long> FINISH_TICK = new ConcurrentHashMap<>();

    private ZombieVillagerCureTracker() {
    }

    public static boolean isCuring(ServerPlayerEntity player) {
        return FINISH_TICK.containsKey(player.getUuid());
    }

    public static void start(ServerPlayerEntity player) {
        if (isCuring(player)) {
            return;
        }

        FINISH_TICK.put(player.getUuid(), ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getServer().getTicks() + (long) CURE_TICKS);
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f
        );
    }

    public static void tick(ServerPlayerEntity player) {
        Long finishTick = FINISH_TICK.get(player.getUuid());
        if (finishTick == null) {
            return;
        }

        boolean isZombieVillager = ZombieVillagerPossessionController.isZombieVillagerPossessing(player);
        boolean isBabyZombieVillager = BabyZombieVillagerPossessionController.isBabyZombieVillagerPossessing(player);
        if (!isZombieVillager && !isBabyZombieVillager) {
            FINISH_TICK.remove(player.getUuid());
            return;
        }

        if (((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getServer().getTicks() < finishTick) {
            return;
        }

        boolean baby = isBabyZombieVillager;
        FINISH_TICK.remove(player.getUuid());
        BabyZombieVillagerState.setServerBaby(player.getUuid(), false);
        BabyZombieVillagerNetworking.broadcast(player, false);
        net.sam.samrequiemmod.possession.villager.VillagerState.setServerBaby(player.getUuid(), baby);
        net.sam.samrequiemmod.possession.villager.VillagerNetworking.broadcastBaby(player, baby);
        PossessionManager.switchPossessionType(player, EntityType.VILLAGER, player.getHealth());
        player.getEntityWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_VILLAGER_CELEBRATE,
                SoundCategory.PLAYERS,
                1.0f,
                baby ? 1.35f : 1.0f
        );
    }

    public static void cancel(UUID uuid) {
        FINISH_TICK.remove(uuid);
    }
}
