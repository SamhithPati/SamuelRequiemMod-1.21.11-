package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

public final class SnowGolemTargetingHelper {

    private SnowGolemTargetingHelper() {}

    public static boolean shouldAttack(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == null) return false;

        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.SKELETON
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.STRAY
                || type == EntityType.BOGGED
                || type == EntityType.CREEPER
                || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN
                || type == EntityType.ENDERMITE
                || type == EntityType.SILVERFISH
                || type == EntityType.SLIME
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.BLAZE
                || type == EntityType.GHAST
                || type == EntityType.VEX
                || type == EntityType.HOGLIN
                || type == EntityType.ZOGLIN
                || type == EntityType.GUARDIAN
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.PILLAGER
                || type == EntityType.VINDICATOR
                || type == EntityType.EVOKER
                || type == EntityType.ILLUSIONER
                || type == EntityType.WITCH
                || type == EntityType.RAVAGER
                || type == EntityType.WARDEN
                || type == EntityType.PIGLIN
                || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.SHULKER;
    }

    public static void tickNearbySnowGolems(ServerPlayerEntity player) {
        if (!shouldAttack(player) || player.age % 20 != 0) return;

        Box box = player.getBoundingBox().expand(16.0);
        for (SnowGolemEntity golem : player.getEntityWorld()
                .getEntitiesByClass(SnowGolemEntity.class, box, MobEntity::isAlive)) {
            if (golem.getTarget() == null || !golem.getTarget().isAlive() || golem.getTarget() == player) {
                golem.setTarget(player);
            }
        }
    }
}
