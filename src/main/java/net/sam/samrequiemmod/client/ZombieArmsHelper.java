package net.sam.samrequiemmod.client;

import net.minecraft.entity.mob.ZombieEntity;

/**
 * Drives the arms-raised pose on a ZombieEntity shell.
 *
 * ZombieEntityModel (via AbstractZombieModel) raises arms when
 * entity.isAttacking() returns true. MobEntity.setAttacking(boolean)
 * sets the underlying DataTracker flag — inherited by ZombieEntity.
 */
public final class ZombieArmsHelper {

    private ZombieArmsHelper() {
    }

    public static void setArmsRaised(ZombieEntity zombie, boolean raised) {
        zombie.setAttacking(raised);
    }
}
