package net.sam.samrequiemmod.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.possession.ClientPossessionState;

public final class FirstPersonPossessionArmHelper {

    private static final Identifier SKELETON_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");
    private static final Identifier BOGGED_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/skeleton/bogged.png");
    private static final Identifier PARCHED_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/skeleton/parched.png");
    private static final Identifier STRAY_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/skeleton/stray.png");
    private static final Identifier WITHER_SKELETON_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/skeleton/wither_skeleton.png");
    private static final Identifier ZOMBIE_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/zombie/zombie.png");
    private static final Identifier HUSK_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/zombie/husk.png");
    private static final Identifier DROWNED_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/zombie/drowned.png");
    private static final Identifier ZOMBIE_VILLAGER_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/zombie_villager/zombie_villager.png");
    private static final Identifier PILLAGER_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/pillager_arms.png");
    private static final Identifier VINDICATOR_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/illager/vindicator.png");
    private static final Identifier EVOKER_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/illager/evoker.png");
    private static final Identifier WITCH_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/witch.png");
    private static final Identifier PIGLIN_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/piglin/piglin.png");
    private static final Identifier PIGLIN_BRUTE_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/piglin/piglin_brute.png");
    private static final Identifier ZOMBIFIED_PIGLIN_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/piglin/zombified_piglin.png");
    private static final Identifier ENDERMAN_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/enderman/enderman.png");
    private static final Identifier IRON_GOLEM_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/iron_golem_arms.png");
    private static final Identifier WARDEN_ARM_TEXTURE =
            Identifier.ofVanilla("textures/entity/warden/warden.png");
    private static final Identifier COW_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/cow_arms.png");
    private static final Identifier PIG_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/pig_arms.png");
    private static final Identifier SHEEP_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/sheep_arms.png");
    private static final Identifier MOOSHROOM_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/mooshroom_arms.png");
    private static final Identifier HORSE_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/horse_arms.png");
    private static final Identifier DONKEY_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/mule_arms.png");
    private static final Identifier MULE_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/mule_arms.png");
    private static final Identifier ZOMBIE_HORSE_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/zombie_horse_arms.png");
    private static final Identifier SKELETON_HORSE_ARM_TEXTURE =
            Identifier.of("samrequiemmod", "textures/entity/first_person/skeleton_horse_arms.png");

    private FirstPersonPossessionArmHelper() {
    }

    public static boolean isSkeletonFamily(AbstractClientPlayerEntity player) {
        EntityType<?> type = ClientPossessionState.get(player);
        return type == EntityType.SKELETON
                || type == EntityType.BOGGED
                || type == EntityType.STRAY
                || type == EntityType.PARCHED
                || type == EntityType.WITHER_SKELETON;
    }

    public static boolean isWitherSkeleton(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) == EntityType.WITHER_SKELETON;
    }

    public static boolean isEnderman(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) == EntityType.ENDERMAN;
    }

    public static boolean isIronGolem(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) == EntityType.IRON_GOLEM;
    }

    public static boolean isWarden(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) == EntityType.WARDEN;
    }

    public static boolean isZombieFamily(AbstractClientPlayerEntity player) {
        EntityType<?> type = ClientPossessionState.get(player);
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER;
    }

    public static boolean shouldHideArms(AbstractClientPlayerEntity player) {
        EntityType<?> type = ClientPossessionState.get(player);
        return type == EntityType.WITCH
                || type == EntityType.ARMADILLO
                || type == EntityType.AXOLOTL
                || type == EntityType.BAT
                || type == EntityType.BEE
                || type == EntityType.BLAZE
                || type == EntityType.BREEZE
                || type == EntityType.CAT
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.CHICKEN
                || type == EntityType.COD
                || type == EntityType.CREEPER
                || type == EntityType.DOLPHIN
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.ENDERMITE
                || type == EntityType.FOX
                || type == EntityType.GHAST
                || type == EntityType.GUARDIAN
                || type == EntityType.HOGLIN
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.NAUTILUS
                || type == EntityType.OCELOT
                || type == EntityType.PANDA
                || type == EntityType.PARROT
                || type == EntityType.POLAR_BEAR
                || type == EntityType.PUFFERFISH
                || type == EntityType.RABBIT
                || type == EntityType.RAVAGER
                || type == EntityType.SALMON
                || type == EntityType.SHULKER
                || type == EntityType.SILVERFISH
                || type == EntityType.SLIME
                || type == EntityType.SNOW_GOLEM
                || type == EntityType.SPIDER
                || type == EntityType.SQUID
                || type == EntityType.SNIFFER
                || type == EntityType.STRIDER
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.TURTLE
                || type == EntityType.VEX
                || type == EntityType.VILLAGER
                || type == EntityType.WANDERING_TRADER
                || type == EntityType.WITHER
                || type == EntityType.WOLF
                || type == EntityType.ZOGLIN
                || type == EntityType.ZOMBIE_NAUTILUS;
    }

    public static Identifier getArmTexture(AbstractClientPlayerEntity player, Identifier fallback) {
        if (player == null) {
            return fallback;
        }

        EntityType<?> type = ClientPossessionState.get(player);
        if (type == EntityType.ZOMBIE) {
            return ZOMBIE_ARM_TEXTURE;
        }
        if (type == EntityType.HUSK) {
            return HUSK_ARM_TEXTURE;
        }
        if (type == EntityType.DROWNED) {
            return DROWNED_ARM_TEXTURE;
        }
        if (type == EntityType.ZOMBIE_VILLAGER) {
            return ZOMBIE_VILLAGER_ARM_TEXTURE;
        }
        if (type == EntityType.ENDERMAN) {
            return ENDERMAN_ARM_TEXTURE;
        }
        if (type == EntityType.IRON_GOLEM) {
            return IRON_GOLEM_ARM_TEXTURE;
        }
        if (type == EntityType.WARDEN) {
            return WARDEN_ARM_TEXTURE;
        }
        if (type == EntityType.COW) {
            return COW_ARM_TEXTURE;
        }
        if (type == EntityType.PIG) {
            return PIG_ARM_TEXTURE;
        }
        if (type == EntityType.SHEEP) {
            return SHEEP_ARM_TEXTURE;
        }
        if (type == EntityType.MOOSHROOM) {
            return MOOSHROOM_ARM_TEXTURE;
        }
        if (type == EntityType.HORSE) {
            return HORSE_ARM_TEXTURE;
        }
        if (type == EntityType.DONKEY) {
            return DONKEY_ARM_TEXTURE;
        }
        if (type == EntityType.MULE) {
            return MULE_ARM_TEXTURE;
        }
        if (type == EntityType.ZOMBIE_HORSE) {
            return ZOMBIE_HORSE_ARM_TEXTURE;
        }
        if (type == EntityType.SKELETON_HORSE) {
            return SKELETON_HORSE_ARM_TEXTURE;
        }
        if (type == EntityType.PILLAGER) {
            return PILLAGER_ARM_TEXTURE;
        }
        if (type == EntityType.VINDICATOR) {
            return VINDICATOR_ARM_TEXTURE;
        }
        if (type == EntityType.EVOKER) {
            return EVOKER_ARM_TEXTURE;
        }
        if (type == EntityType.WITCH) {
            return WITCH_ARM_TEXTURE;
        }
        if (type == EntityType.PIGLIN) {
            return PIGLIN_ARM_TEXTURE;
        }
        if (type == EntityType.PIGLIN_BRUTE) {
            return PIGLIN_BRUTE_ARM_TEXTURE;
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN) {
            return ZOMBIFIED_PIGLIN_ARM_TEXTURE;
        }
        if (type == EntityType.BOGGED) {
            return BOGGED_ARM_TEXTURE;
        }
        if (type == EntityType.PARCHED) {
            return PARCHED_ARM_TEXTURE;
        }
        if (type == EntityType.STRAY) {
            return STRAY_ARM_TEXTURE;
        }
        if (type == EntityType.WITHER_SKELETON) {
            return WITHER_SKELETON_ARM_TEXTURE;
        }

        if (isSkeletonFamily(player)) {
            return SKELETON_ARM_TEXTURE;
        }

        return fallback;
    }
}
