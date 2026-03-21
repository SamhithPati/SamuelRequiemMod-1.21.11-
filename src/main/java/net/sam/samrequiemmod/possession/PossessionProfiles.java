package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class PossessionProfiles {

    private static final Map<EntityType<?>, PossessionProfile> PROFILES = new HashMap<>();

    // Baby drowned uses EntityType.DROWNED but with baby dimensions
    public static final PossessionProfile BABY_DROWNED_PROFILE = new PossessionProfile(
            0.6f,    // width
            0.975f,  // height
            0.84f,   // eye height
            0.0,
            0.04     // same speed as baby zombie
    );

    // Baby husk uses EntityType.HUSK but with its own dimensions profile
    public static final PossessionProfile BABY_HUSK_PROFILE = new PossessionProfile(
            0.6f,    // width
            0.975f,  // height
            0.84f,   // eye height
            0.0,
            0.04     // same speed as baby zombie
    );

    // Baby zombie villager uses EntityType.ZOMBIE_VILLAGER but with baby dimensions
    public static final PossessionProfile BABY_ZOMBIE_VILLAGER_PROFILE = new PossessionProfile(
            0.6f,    // width
            0.975f,  // height
            0.84f,   // eye height
            0.0,
            0.04     // same speed as baby zombie
    );

    // Baby zombie uses EntityType.ZOMBIE but with its own dimensions profile
    public static final PossessionProfile BABY_ZOMBIE_PROFILE = new PossessionProfile(
            0.6f,    // width
            0.975f,  // height (half of adult)
            0.84f,   // eye height
            0.0,     // no health bonus
            0.04     // slightly faster than normal walk
    );

    public static final PossessionProfile PILLAGER_PROFILE = new PossessionProfile(
            0.6f, 1.8f, 1.62f, 4.0f, 0.0f); // +4 HP on top of player base 20 = 24 HP = 12 hearts

    public static final PossessionProfile VINDICATOR_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.74f, 4.0f, 0.0f); // 12 hearts like pillager

    public static final PossessionProfile EVOKER_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.74f, 4.0f, 0.05f); // 12 hearts, slightly faster

    static {
        // 🧟 Zombie profile
        register(EntityType.PILLAGER, PILLAGER_PROFILE);
        register(EntityType.VINDICATOR, VINDICATOR_PROFILE);
        register(EntityType.EVOKER, EVOKER_PROFILE);
        register(EntityType.ZOMBIE, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                0.0,
                -0.04
        ));

        // 🏜️ Husk profile — same dimensions as adult zombie, same speed
        register(EntityType.HUSK, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                0.0,
                -0.04
        ));

        // 🧟‍ Zombie Villager profile — same dimensions as adult zombie
        register(EntityType.ZOMBIE_VILLAGER, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                0.0,
                -0.04
        ));

        // 🌊 Drowned profile — same as adult zombie
        register(EntityType.DROWNED, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                0.0,
                -0.04
        ));

        // 💀 Skeleton profile
        register(EntityType.SKELETON, new PossessionProfile(
                0.6f,
                1.99f,
                1.74f,
                0.0,
                0.0
        ));

        // 🐷 Piglin profile
        register(EntityType.PIGLIN, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                4.0,
                0.05
        ));

        // 💀 Piglin brute profile
        register(EntityType.PIGLIN_BRUTE, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                10.0,
                0.0
        ));

        // 🕷 Spider (future use)
        register(EntityType.SPIDER, new PossessionProfile(
                1.4f,
                0.9f,
                0.65f,
                0.0,
                0.10
        ));
    }

    private PossessionProfiles() {}

    private static void register(EntityType<?> type, PossessionProfile profile) {
        PROFILES.put(type, profile);
    }

    public static PossessionProfile get(EntityType<?> type) {
        return PROFILES.get(type);
    }
}