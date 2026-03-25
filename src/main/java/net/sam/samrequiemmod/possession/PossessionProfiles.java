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

    // Baby piglin uses EntityType.PIGLIN but with baby dimensions — 8 hearts, slightly faster than adult
    public static final PossessionProfile BABY_PIGLIN_PROFILE = new PossessionProfile(
            0.6f,    // width
            0.975f,  // height
            0.84f,   // eye height
            -4.0,    // 8 hearts
            0.02     // slightly faster than adult
    );

    // Baby zombified piglin — same as other baby zombie subtypes
    public static final PossessionProfile BABY_ZOMBIFIED_PIGLIN_PROFILE = new PossessionProfile(
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

    // Baby passive mob profiles
    public static final PossessionProfile BABY_COW_PROFILE = new PossessionProfile(
            0.4f, 0.7f, 0.6f, -10.0, 0.0
    );

    public static final PossessionProfile BABY_PIG_PROFILE = new PossessionProfile(
            0.4f, 0.45f, 0.36f, -10.0, 0.0
    );

    public static final PossessionProfile BABY_SHEEP_PROFILE = new PossessionProfile(
            0.4f, 0.65f, 0.55f, -12.0, 0.0
    );

    public static final PossessionProfile BABY_CHICKEN_PROFILE = new PossessionProfile(
            0.2f, 0.35f, 0.28f, -16.0, 0.0
    );

    public static final PossessionProfile BABY_HOGLIN_PROFILE = new PossessionProfile(
            0.7f, 0.7f, 0.63f, 20.0f, 0.0f
    );

    public static final PossessionProfile BABY_ZOGLIN_PROFILE = new PossessionProfile(
            0.7f, 0.7f, 0.63f, 20.0f, 0.0f
    );

    public static final PossessionProfile BIG_SLIME_PROFILE = new PossessionProfile(
            2.04f, 2.04f, 1.05f, -4.0f, -0.04f
    );

    public static final PossessionProfile MEDIUM_SLIME_PROFILE = new PossessionProfile(
            1.02f, 1.02f, 0.55f, -16.0f, -0.04f
    );

    public static final PossessionProfile SMALL_SLIME_PROFILE = new PossessionProfile(
            0.51f, 0.51f, 0.28f, -19.0f, -0.02f
    );

    public static final PossessionProfile BIG_MAGMA_CUBE_PROFILE = new PossessionProfile(
            2.04f, 2.04f, 1.05f, -4.0f, -0.04f
    );

    public static final PossessionProfile MEDIUM_MAGMA_CUBE_PROFILE = new PossessionProfile(
            1.02f, 1.02f, 0.55f, -16.0f, -0.04f
    );

    public static final PossessionProfile SMALL_MAGMA_CUBE_PROFILE = new PossessionProfile(
            0.51f, 0.51f, 0.28f, -19.0f, -0.02f
    );

    public static final PossessionProfile WOLF_PROFILE = new PossessionProfile(
            0.6f, 0.85f, 0.72f, -12.0f, -0.02f
    );

    public static final PossessionProfile BABY_WOLF_PROFILE = new PossessionProfile(
            0.3f, 0.425f, 0.36f, -12.0f, 0.0f
    );

    public static final PossessionProfile FOX_PROFILE = new PossessionProfile(
            0.6f, 0.7f, 0.42f, -10.0f, -0.02f
    );

    public static final PossessionProfile BABY_FOX_PROFILE = new PossessionProfile(
            0.3f, 0.35f, 0.22f, -10.0f, 0.0f
    );

    public static final PossessionProfile OCELOT_PROFILE = new PossessionProfile(
            0.6f, 0.7f, 0.42f, -10.0f, 0.05f
    );

    public static final PossessionProfile BABY_OCELOT_PROFILE = new PossessionProfile(
            0.3f, 0.35f, 0.22f, -10.0f, 0.05f
    );

    public static final PossessionProfile CAT_PROFILE = new PossessionProfile(
            0.6f, 0.7f, 0.42f, -10.0f, 0.05f
    );

    public static final PossessionProfile BABY_CAT_PROFILE = new PossessionProfile(
            0.3f, 0.35f, 0.22f, -10.0f, 0.05f
    );

    public static final PossessionProfile FROG_PROFILE = new PossessionProfile(
            0.5f, 0.55f, 0.32f, -10.0f, -0.06f
    );

    public static final PossessionProfile HORSE_PROFILE = new PossessionProfile(
            1.3965f, 1.6f, 1.44f, 20.0f, 0.05f
    );

    public static final PossessionProfile BABY_HORSE_PROFILE = new PossessionProfile(
            0.698f, 0.8f, 0.72f, 20.0f, 0.05f
    );

    public static final PossessionProfile ZOMBIE_HORSE_PROFILE = new PossessionProfile(
            1.3965f, 1.6f, 1.44f, 20.0f, -0.04f
    );

    public static final PossessionProfile SKELETON_HORSE_PROFILE = new PossessionProfile(
            1.3965f, 1.6f, 1.44f, 20.0f, -0.04f
    );

    public static final PossessionProfile MULE_PROFILE = new PossessionProfile(
            1.3965f, 1.6f, 1.44f, 20.0f, 0.05f
    );

    public static final PossessionProfile BABY_MULE_PROFILE = new PossessionProfile(
            0.698f, 0.8f, 0.72f, 20.0f, 0.05f
    );

    public static final PossessionProfile ENDERMITE_PROFILE = new PossessionProfile(
            0.4f, 0.3f, 0.18f, -12.0f, -0.04f
    );

    public static final PossessionProfile GOAT_PROFILE = new PossessionProfile(
            0.9f, 1.3f, 1.16f, -10.0f, -0.04f
    );

    public static final PossessionProfile BABY_GOAT_PROFILE = new PossessionProfile(
            0.45f, 0.65f, 0.58f, -10.0f, -0.04f
    );

    public static final PossessionProfile POLAR_BEAR_PROFILE = new PossessionProfile(
            1.4f, 1.4f, 1.26f, 10.0f, -0.02f
    );

    public static final PossessionProfile BABY_POLAR_BEAR_PROFILE = new PossessionProfile(
            0.7f, 0.7f, 0.63f, 10.0f, -0.02f
    );

    public static final PossessionProfile RABBIT_PROFILE = new PossessionProfile(
            0.4f, 0.5f, 0.25f, -17.0f, 0.05f
    );

    public static final PossessionProfile BABY_RABBIT_PROFILE = new PossessionProfile(
            0.2f, 0.25f, 0.12f, -17.0f, 0.05f
    );

    public static final PossessionProfile TURTLE_PROFILE = new PossessionProfile(
            1.2f, 0.4f, 0.2f, 10.0f, -0.08f
    );

    public static final PossessionProfile BABY_TURTLE_PROFILE = new PossessionProfile(
            0.36f, 0.12f, 0.06f, -14.0f, -0.08f
    );

    public static final PossessionProfile SHULKER_PROFILE = new PossessionProfile(
            1.0f, 1.0f, 0.5f, 40.0f, -0.10f
    );

    public static final PossessionProfile STRIDER_PROFILE = new PossessionProfile(
            0.9f, 1.7f, 1.45f, 0.0f, -0.06f
    );

    public static final PossessionProfile BABY_STRIDER_PROFILE = new PossessionProfile(
            0.45f, 0.85f, 0.72f, 0.0f, -0.06f
    );

    public static final PossessionProfile AXOLOTL_PROFILE = new PossessionProfile(
            0.75f, 0.42f, 0.21f, -6.0f, -0.06f
    );

    public static final PossessionProfile BABY_AXOLOTL_PROFILE = new PossessionProfile(
            0.375f, 0.21f, 0.10f, -6.0f, -0.06f
    );

    public static final PossessionProfile SNOW_GOLEM_PROFILE = new PossessionProfile(
            0.6f, 1.9f, 1.72f, -16.0f, -0.02f
    );

    public static final PossessionProfile CAMEL_PROFILE = new PossessionProfile(
            1.7f, 2.375f, 2.18f, 12.0f, 0.0f
    );

    public static final PossessionProfile BABY_CAMEL_PROFILE = new PossessionProfile(
            0.85f, 1.18f, 1.05f, 12.0f, 0.0f
    );

    public static final PossessionProfile VEX_PROFILE = new PossessionProfile(
            0.4f, 0.8f, 0.6f, 0.0f, 0.0f
    );

    public static final PossessionProfile BAT_PROFILE = new PossessionProfile(
            0.5f, 0.9f, 0.5f, -14.0f, 0.0f
    );

    public static final PossessionProfile VILLAGER_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.62f, 0.0f, 0.05f
    );

    public static final PossessionProfile BABY_VILLAGER_PROFILE = new PossessionProfile(
            0.6f, 0.975f, 0.84f, 0.0f, 0.07f
    );

    public static final PossessionProfile BEE_PROFILE = new PossessionProfile(
            0.7f, 0.6f, 0.32f, -10.0f, 0.0f
    );

    public static final PossessionProfile PARROT_PROFILE = new PossessionProfile(
            0.5f, 0.9f, 0.5f, -14.0f, 0.0f
    );

    public static final PossessionProfile PILLAGER_PROFILE = new PossessionProfile(
            0.6f, 1.8f, 1.62f, 4.0f, 0.0f); // +4 HP on top of player base 20 = 24 HP = 12 hearts

    public static final PossessionProfile VINDICATOR_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.74f, 4.0f, 0.0f); // 12 hearts like pillager

    public static final PossessionProfile EVOKER_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.74f, 4.0f, 0.05f); // 12 hearts, slightly faster

    public static final PossessionProfile RAVAGER_PROFILE = new PossessionProfile(
            0.6f, 2.2f, 1.98f, 80.0f, 0.0f); // 50 hearts (20+80=100 HP), player-width hitbox to avoid getting stuck on blocks

    public static final PossessionProfile WITCH_PROFILE = new PossessionProfile(
            0.6f, 1.95f, 1.74f, 6.0f, 0.0f); // 13 hearts (20+6=26 HP), same speed as pillager/vindicator

    public static final PossessionProfile IRON_GOLEM_PROFILE = new PossessionProfile(
            0.6f, 2.7f, 2.5f, 80.0f, -0.02f); // 50 hearts (20+80=100 HP), player-width hitbox, pig/sheep speed

    public static final PossessionProfile SKELETON_PROFILE = new PossessionProfile(
            0.6f, 1.99f, 1.74f, 0.0f, -0.02f); // 10 hearts (20 HP), same speed as iron golem

    public static final PossessionProfile BOGGED_PROFILE = new PossessionProfile(
            0.6f, 1.99f, 1.74f, 0.0f, -0.02f); // 10 hearts, same as skeleton

    public static final PossessionProfile STRAY_PROFILE = new PossessionProfile(
            0.6f, 1.99f, 1.74f, 0.0f, -0.02f); // 10 hearts, same as skeleton

    public static final PossessionProfile WITHER_SKELETON_PROFILE = new PossessionProfile(
            0.6f, 2.4f, 2.1f, 0.0f, -0.02f); // 10 hearts, taller camera height

    public static final PossessionProfile ENDERMAN_PROFILE = new PossessionProfile(
            0.6f, 2.9f, 2.55f, 20.0f, 0.0f); // 20 hearts (20+20=40 HP), same speed as pillager/vindicator

    // 🐟 Cod — 1.5 hearts (3 HP), greatly reduced land speed
    public static final PossessionProfile COD_PROFILE = new PossessionProfile(
            0.5f, 0.3f, 0.2f, -17.0, -0.06);

    // 🐟 Salmon — 1.5 hearts (3 HP)
    public static final PossessionProfile SALMON_PROFILE = new PossessionProfile(
            0.7f, 0.4f, 0.3f, -17.0, -0.06);

    // 🐡 Pufferfish — 1.5 hearts (3 HP)
    public static final PossessionProfile PUFFERFISH_PROFILE = new PossessionProfile(
            0.7f, 0.7f, 0.5f, -17.0, -0.06);

    // 🐠 Tropical Fish — 1.5 hearts (3 HP)
    public static final PossessionProfile TROPICAL_FISH_PROFILE = new PossessionProfile(
            0.5f, 0.4f, 0.3f, -17.0, -0.06);

    // 🦑 Squid — 5 hearts (10 HP)
    public static final PossessionProfile SQUID_PROFILE = new PossessionProfile(
            0.6f, 0.8f, 0.6f, -10.0, -0.06);

    // 🐬 Dolphin — 5 hearts (10 HP), same land penalty
    public static final PossessionProfile DOLPHIN_PROFILE = new PossessionProfile(
            0.6f, 0.6f, 0.5f, -10.0, -0.06);

    static {
        // 🧟 Zombie profile
        register(EntityType.PILLAGER, PILLAGER_PROFILE);
        register(EntityType.VINDICATOR, VINDICATOR_PROFILE);
        register(EntityType.EVOKER, EVOKER_PROFILE);
        register(EntityType.RAVAGER, RAVAGER_PROFILE);
        register(EntityType.WITCH, WITCH_PROFILE);
        register(EntityType.IRON_GOLEM, IRON_GOLEM_PROFILE);
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
        register(EntityType.SKELETON, SKELETON_PROFILE);

        // 🧪 Bogged profile
        register(EntityType.BOGGED, BOGGED_PROFILE);

        // ❄ Stray profile
        register(EntityType.STRAY, STRAY_PROFILE);

        // 🖤 Wither Skeleton profile
        register(EntityType.WITHER_SKELETON, WITHER_SKELETON_PROFILE);

        // 🐷 Piglin profile — 8 hearts (16 HP), same speed as pillager/vindicator
        register(EntityType.PIGLIN, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                -4.0,
                0.0
        ));

        // 💀 Piglin brute profile — 25 hearts (50 HP), same speed as adult piglin
        register(EntityType.PIGLIN_BRUTE, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                30.0,
                0.0
        ));

        // 🧟🐷 Zombified piglin profile — same as other zombie subtypes
        register(EntityType.ZOMBIFIED_PIGLIN, new PossessionProfile(
                0.6f,
                1.95f,
                1.74f,
                0.0,
                -0.04
        ));

        // 👁 Enderman — 20 hearts (40 HP), same speed as pillager/vindicator
        register(EntityType.ENDERMAN, ENDERMAN_PROFILE);

        // 💥 Creeper — 10 hearts (20 HP), same speed as skeleton
        register(EntityType.CREEPER, new PossessionProfile(
                0.6f, 1.7f, 1.445f, 0.0f, -0.02f
        ));

        register(EntityType.SPIDER, new PossessionProfile(
                1.4f,
                0.9f,
                0.65f,
                -4.0,
                0.10
        ));

        register(EntityType.CAVE_SPIDER, new PossessionProfile(
                0.7f,
                0.5f,
                0.45f,
                -8.0,
                0.12
        ));

        register(EntityType.HOGLIN, new PossessionProfile(
                1.3964844f, 1.4f, 1.27f, 20.0f, -0.02f
        ));

        register(EntityType.ZOGLIN, new PossessionProfile(
                1.3964844f, 1.4f, 1.27f, 20.0f, -0.02f
        ));

        register(EntityType.GUARDIAN, new PossessionProfile(
                0.85f, 0.85f, 0.425f, 10.0f, -0.02f
        ));

        register(EntityType.ELDER_GUARDIAN, new PossessionProfile(
                1.9975f, 1.9975f, 0.998f, 60.0f, -0.02f
        ));

        register(EntityType.SILVERFISH, new PossessionProfile(
                0.4f, 0.3f, 0.15f, -12.0f, -0.04f
        ));

        register(EntityType.BLAZE, new PossessionProfile(
                0.6f, 1.8f, 1.62f, 0.0f, 0.0f
        ));

        register(EntityType.GHAST, new PossessionProfile(
                4.0f, 4.0f, 2.6f, -10.0f, 0.0f
        ));

        register(EntityType.SLIME, BIG_SLIME_PROFILE);

        register(EntityType.MAGMA_CUBE, BIG_MAGMA_CUBE_PROFILE);

        register(EntityType.WOLF, WOLF_PROFILE);
        register(EntityType.FOX, FOX_PROFILE);
        register(EntityType.OCELOT, OCELOT_PROFILE);
        register(EntityType.CAT, CAT_PROFILE);
        register(EntityType.FROG, FROG_PROFILE);
        register(EntityType.HORSE, HORSE_PROFILE);
        register(EntityType.ZOMBIE_HORSE, ZOMBIE_HORSE_PROFILE);
        register(EntityType.SKELETON_HORSE, SKELETON_HORSE_PROFILE);
        register(EntityType.MULE, MULE_PROFILE);
        register(EntityType.ENDERMITE, ENDERMITE_PROFILE);
        register(EntityType.GOAT, GOAT_PROFILE);
        register(EntityType.POLAR_BEAR, POLAR_BEAR_PROFILE);
        register(EntityType.RABBIT, RABBIT_PROFILE);
        register(EntityType.TURTLE, TURTLE_PROFILE);
        register(EntityType.SHULKER, SHULKER_PROFILE);
        register(EntityType.STRIDER, STRIDER_PROFILE);
        register(EntityType.AXOLOTL, AXOLOTL_PROFILE);
        register(EntityType.SNOW_GOLEM, SNOW_GOLEM_PROFILE);
        register(EntityType.CAMEL, CAMEL_PROFILE);
        register(EntityType.VEX, VEX_PROFILE);
        register(EntityType.BAT, BAT_PROFILE);
        register(EntityType.VILLAGER, VILLAGER_PROFILE);
        register(EntityType.BEE, BEE_PROFILE);
        register(EntityType.PARROT, PARROT_PROFILE);

        // 🐄 Cow — 5 hearts (10 HP), slightly slower than illagers
        register(EntityType.COW, new PossessionProfile(
                0.6f, 1.4f, 1.2f, -10.0, -0.02
        ));

        // 🍄 Mooshroom — 5 hearts (10 HP), same as cow
        register(EntityType.MOOSHROOM, new PossessionProfile(
                0.6f, 1.4f, 1.2f, -10.0, -0.02
        ));

        // 🐷 Pig — 5 hearts (10 HP)
        register(EntityType.PIG, new PossessionProfile(
                0.6f, 0.9f, 0.72f, -10.0, -0.02
        ));

        // 🐑 Sheep — 4 hearts (8 HP)
        register(EntityType.SHEEP, new PossessionProfile(
                0.6f, 1.3f, 1.1f, -12.0, -0.02
        ));

        // 🐔 Chicken — 2 hearts (4 HP)
        register(EntityType.CHICKEN, new PossessionProfile(
                0.4f, 0.7f, 0.56f, -16.0, -0.02
        ));

        // 🐟 Aquatic mobs
        register(EntityType.COD, COD_PROFILE);
        register(EntityType.SALMON, SALMON_PROFILE);
        register(EntityType.PUFFERFISH, PUFFERFISH_PROFILE);
        register(EntityType.TROPICAL_FISH, TROPICAL_FISH_PROFILE);
        register(EntityType.SQUID, SQUID_PROFILE);
        register(EntityType.DOLPHIN, DOLPHIN_PROFILE);
    }

    private PossessionProfiles() {}

    private static void register(EntityType<?> type, PossessionProfile profile) {
        PROFILES.put(type, profile);
    }

    public static PossessionProfile get(EntityType<?> type) {
        return PROFILES.get(type);
    }
}
