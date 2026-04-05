package net.sam.samrequiemmod.possession;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;

public final class PossessionDamageHelper {
    private PossessionDamageHelper() {}

    public static boolean isHarmlessSlimeContact(DamageSource source) {
        return source.getAttacker() instanceof SlimeEntity
                || source.getAttacker() instanceof MagmaCubeEntity;
    }
}
