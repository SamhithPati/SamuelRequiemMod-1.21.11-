package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;

public final class PossessionEffects {

    private static final Identifier HEALTH_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "zombie_health");

    private static final Identifier SPEED_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "zombie_speed");
    private static final Identifier PILLAGER_ATTACK_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "pillager_attack");

    private static final Identifier VINDICATOR_ATTACK_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "vindicator_attack");

    private static final Identifier RAVAGER_REACH_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "ravager_reach");

    private static final Identifier IRON_GOLEM_REACH_MODIFIER_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "iron_golem_reach");

    private static final Identifier IRON_GOLEM_KNOCKBACK_RESISTANCE_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "iron_golem_knockback_resistance");
    private static final Identifier WARDEN_KNOCKBACK_RESISTANCE_ID =
            Identifier.of(SamuelRequiemMod.MOD_ID, "warden_knockback_resistance");

    private PossessionEffects() {
    }

    public static void apply(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);

        clear(player);

        if (type == null) return;

        PossessionProfile profile;
        if (type == EntityType.ZOMBIE && BabyZombieState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIE_PROFILE;
        } else if (type == EntityType.HUSK && BabyHuskState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_HUSK_PROFILE;
        } else if (type == EntityType.DROWNED && BabyDrownedState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_DROWNED_PROFILE;
        } else if (type == EntityType.ZOMBIE_VILLAGER && net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIE_VILLAGER_PROFILE;
        } else if (type == EntityType.COW && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_COW_PROFILE;
        } else if (type == EntityType.PIG && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_PIG_PROFILE;
        } else if (type == EntityType.SHEEP && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_SHEEP_PROFILE;
        } else if (type == EntityType.CHICKEN && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_CHICKEN_PROFILE;
        } else if (type == EntityType.PANDA && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_PANDA_PROFILE;
        } else if (type == EntityType.FOX && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_FOX_PROFILE;
        } else if (type == EntityType.OCELOT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_OCELOT_PROFILE;
        } else if (type == EntityType.CAT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_CAT_PROFILE;
        } else if (type == EntityType.HORSE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_HORSE_PROFILE;
        } else if (type == EntityType.DONKEY && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_DONKEY_PROFILE;
        } else if (type == EntityType.MULE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_MULE_PROFILE;
        } else if (type == EntityType.GOAT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_GOAT_PROFILE;
        } else if (type == EntityType.POLAR_BEAR && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_POLAR_BEAR_PROFILE;
        } else if (type == EntityType.RABBIT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_RABBIT_PROFILE;
        } else if (type == EntityType.TURTLE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_TURTLE_PROFILE;
        } else if (type == EntityType.STRIDER && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_STRIDER_PROFILE;
        } else if (type == EntityType.AXOLOTL && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_AXOLOTL_PROFILE;
        } else if (type == EntityType.CAMEL && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_CAMEL_PROFILE;
        } else if (type == EntityType.PIGLIN && net.sam.samrequiemmod.possession.piglin.BabyPiglinState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_PIGLIN_PROFILE;
        } else if (type == EntityType.ZOMBIFIED_PIGLIN && net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIFIED_PIGLIN_PROFILE;
        } else if (type == EntityType.HOGLIN && net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_HOGLIN_PROFILE;
        } else if (type == EntityType.ZOGLIN && net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_ZOGLIN_PROFILE;
        } else if (type == EntityType.SLIME) {
            int size = net.sam.samrequiemmod.possession.slime.SlimeSizeState.getServerSize(player);
            profile = switch (size) {
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_SLIME_PROFILE;
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.SMALL -> PossessionProfiles.SMALL_SLIME_PROFILE;
                default -> PossessionProfiles.BIG_SLIME_PROFILE;
            };
        } else if (type == EntityType.MAGMA_CUBE) {
            int size = net.sam.samrequiemmod.possession.slime.SlimeSizeState.getServerSize(player);
            profile = switch (size) {
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_MAGMA_CUBE_PROFILE;
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.SMALL -> PossessionProfiles.SMALL_MAGMA_CUBE_PROFILE;
                default -> PossessionProfiles.BIG_MAGMA_CUBE_PROFILE;
            };
        } else if (type == EntityType.WOLF && net.sam.samrequiemmod.possession.wolf.WolfBabyState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_WOLF_PROFILE;
        } else if (type == EntityType.VILLAGER && net.sam.samrequiemmod.possession.villager.VillagerState.isServerBaby(player)) {
            profile = PossessionProfiles.BABY_VILLAGER_PROFILE;
        } else {
            profile = PossessionProfiles.get(type);
        }

        if (profile == null) return;

        applyModifier(
                player,
                EntityAttributes.MAX_HEALTH,
                HEALTH_MODIFIER_ID,
                profile.getHealthBonus(),
                EntityAttributeModifier.Operation.ADD_VALUE
        );

        applyModifier(
                player,
                EntityAttributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_ID,
                profile.getSpeedModifier(),
                EntityAttributeModifier.Operation.ADD_VALUE
        );

        // Pillager: set attack damage (base player = 1, pillager unarmed = ~2 which scales with difficulty)
        if (type == EntityType.PILLAGER) {
            applyModifier(
                    player,
                    EntityAttributes.ATTACK_DAMAGE,
                    PILLAGER_ATTACK_MODIFIER_ID,
                    2.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        // Vindicator unarmed: Easy=1H, Normal=1.5H, Hard=2.25H -> base 3dmg (1.5H)
        // With iron axe: Easy=3.5H, Normal=6.5H, Hard=9.5H -> iron axe base handles this
        if (type == EntityType.VINDICATOR) {
            applyModifier(
                    player,
                    EntityAttributes.ATTACK_DAMAGE,
                    VINDICATOR_ATTACK_MODIFIER_ID,
                    1.0, // player base 2 + 1 = 3dmg = 1.5H on Normal
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        // Vindicator: unarmed = 1 heart easy / 1.5 normal / 2.25 hard
        // We set base to 1 heart (2 HP) and let difficulty scaling apply
        // Axe damage is handled separately in ZombieFoodUseHandler / attack event
        if (type == EntityType.VINDICATOR) {
            applyModifier(
                    player,
                    EntityAttributes.ATTACK_DAMAGE,
                    PILLAGER_ATTACK_MODIFIER_ID,
                    1.0, // +1 on top of player base 1 = 2 HP = 1 heart on Easy
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        // Ravager: extend entity interaction reach to 4 blocks for bite attack
        if (type == EntityType.RAVAGER) {
            applyModifier(
                    player,
                    EntityAttributes.ENTITY_INTERACTION_RANGE,
                    RAVAGER_REACH_MODIFIER_ID,
                    1.0, // player base 3 + 1 = 4 blocks
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        // Iron Golem: extend entity interaction reach to 4 blocks
        if (type == EntityType.IRON_GOLEM) {
            applyModifier(
                    player,
                    EntityAttributes.ENTITY_INTERACTION_RANGE,
                    IRON_GOLEM_REACH_MODIFIER_ID,
                    1.0, // player base 3 + 1 = 4 blocks
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
            // Iron golem has 1.0 knockback resistance (fully immune)
            applyModifier(
                    player,
                    EntityAttributes.KNOCKBACK_RESISTANCE,
                    IRON_GOLEM_KNOCKBACK_RESISTANCE_ID,
                    1.0, // player base 0 + 1.0 = full immunity
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

        if (type == EntityType.WARDEN) {
            applyModifier(
                    player,
                    EntityAttributes.KNOCKBACK_RESISTANCE,
                    WARDEN_KNOCKBACK_RESISTANCE_ID,
                    1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }

    }

    public static void clear(ServerPlayerEntity player) {
        removeModifier(player, EntityAttributes.MAX_HEALTH, HEALTH_MODIFIER_ID);
        removeModifier(player, EntityAttributes.ATTACK_DAMAGE, PILLAGER_ATTACK_MODIFIER_ID);
        removeModifier(player, EntityAttributes.ATTACK_DAMAGE, VINDICATOR_ATTACK_MODIFIER_ID);
        removeModifier(player, EntityAttributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
        removeModifier(player, EntityAttributes.ENTITY_INTERACTION_RANGE, RAVAGER_REACH_MODIFIER_ID);
        removeModifier(player, EntityAttributes.ENTITY_INTERACTION_RANGE, IRON_GOLEM_REACH_MODIFIER_ID);
        removeModifier(player, EntityAttributes.KNOCKBACK_RESISTANCE, IRON_GOLEM_KNOCKBACK_RESISTANCE_ID);
        removeModifier(player, EntityAttributes.KNOCKBACK_RESISTANCE, WARDEN_KNOCKBACK_RESISTANCE_ID);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void applyModifier(
            ServerPlayerEntity player,
            RegistryEntry<EntityAttribute> attribute,
            Identifier id,
            double value,
            EntityAttributeModifier.Operation operation
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        instance.addPersistentModifier(new EntityAttributeModifier(id, value, operation));
    }

    private static void removeModifier(
            ServerPlayerEntity player,
            RegistryEntry<EntityAttribute> attribute,
            Identifier id
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
    }

}






