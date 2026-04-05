package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.hoglin.BabyHoglinState;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.passive.BabyPassiveMobState;
import net.sam.samrequiemmod.possession.piglin.BabyPiglinState;
import net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState;
import net.sam.samrequiemmod.possession.slime.SlimeSizeState;
import net.sam.samrequiemmod.possession.villager.VillagerState;
import net.sam.samrequiemmod.possession.wolf.WolfBabyState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;
import net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState;

public final class PossessionProfileResolver {

    private PossessionProfileResolver() {
    }

    public static PossessionProfile get(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == null && player.getEntityWorld().isClient()) {
            type = ClientPossessionState.get(player);
        }

        if (type == null) {
            return null;
        }

        if (type == EntityType.ZOMBIE && BabyZombieState.isBaby(player)) {
            return PossessionProfiles.BABY_ZOMBIE_PROFILE;
        }
        if (type == EntityType.HUSK && BabyHuskState.isBaby(player)) {
            return PossessionProfiles.BABY_HUSK_PROFILE;
        }
        if (type == EntityType.DROWNED && BabyDrownedState.isBaby(player)) {
            return PossessionProfiles.BABY_DROWNED_PROFILE;
        }
        if (type == EntityType.ZOMBIE_VILLAGER && BabyZombieVillagerState.isBaby(player)) {
            return PossessionProfiles.BABY_ZOMBIE_VILLAGER_PROFILE;
        }
        if (type == EntityType.COW && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_COW_PROFILE;
        }
        if (type == EntityType.PIG && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_PIG_PROFILE;
        }
        if (type == EntityType.SHEEP && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_SHEEP_PROFILE;
        }
        if (type == EntityType.CHICKEN && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_CHICKEN_PROFILE;
        }
        if (type == EntityType.PANDA && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_PANDA_PROFILE;
        }
        if (type == EntityType.FOX && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_FOX_PROFILE;
        }
        if (type == EntityType.OCELOT && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_OCELOT_PROFILE;
        }
        if (type == EntityType.CAT && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_CAT_PROFILE;
        }
        if (type == EntityType.HORSE && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_HORSE_PROFILE;
        }
        if (type == EntityType.MULE && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_MULE_PROFILE;
        }
        if (type == EntityType.GOAT && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_GOAT_PROFILE;
        }
        if (type == EntityType.POLAR_BEAR && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_POLAR_BEAR_PROFILE;
        }
        if (type == EntityType.RABBIT && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_RABBIT_PROFILE;
        }
        if (type == EntityType.TURTLE && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_TURTLE_PROFILE;
        }
        if (type == EntityType.STRIDER && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_STRIDER_PROFILE;
        }
        if (type == EntityType.AXOLOTL && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_AXOLOTL_PROFILE;
        }
        if (type == EntityType.CAMEL && BabyPassiveMobState.isBaby(player)) {
            return PossessionProfiles.BABY_CAMEL_PROFILE;
        }
        if (type == EntityType.PIGLIN && BabyPiglinState.isBaby(player)) {
            return PossessionProfiles.BABY_PIGLIN_PROFILE;
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN && BabyZombifiedPiglinState.isBaby(player)) {
            return PossessionProfiles.BABY_ZOMBIFIED_PIGLIN_PROFILE;
        }
        if (type == EntityType.HOGLIN && BabyHoglinState.isBaby(player)) {
            return PossessionProfiles.BABY_HOGLIN_PROFILE;
        }
        if (type == EntityType.ZOGLIN && BabyHoglinState.isBaby(player)) {
            return PossessionProfiles.BABY_ZOGLIN_PROFILE;
        }
        if (type == EntityType.SLIME) {
            int size = SlimeSizeState.getSize(player);
            return switch (size) {
                case SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_SLIME_PROFILE;
                case SlimeSizeState.SMALL -> PossessionProfiles.SMALL_SLIME_PROFILE;
                default -> PossessionProfiles.BIG_SLIME_PROFILE;
            };
        }
        if (type == EntityType.MAGMA_CUBE) {
            int size = SlimeSizeState.getSize(player);
            return switch (size) {
                case SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_MAGMA_CUBE_PROFILE;
                case SlimeSizeState.SMALL -> PossessionProfiles.SMALL_MAGMA_CUBE_PROFILE;
                default -> PossessionProfiles.BIG_MAGMA_CUBE_PROFILE;
            };
        }
        if (type == EntityType.WOLF && WolfBabyState.isBaby(player)) {
            return PossessionProfiles.BABY_WOLF_PROFILE;
        }
        if (type == EntityType.VILLAGER && VillagerState.isBaby(player)) {
            return PossessionProfiles.BABY_VILLAGER_PROFILE;
        }

        return PossessionProfiles.get(type);
    }
}
