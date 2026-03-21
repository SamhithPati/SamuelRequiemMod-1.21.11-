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
        } else {
            profile = PossessionProfiles.get(type);
        }

        if (profile == null) return;

        applyModifier(
                player,
                EntityAttributes.GENERIC_MAX_HEALTH,
                HEALTH_MODIFIER_ID,
                profile.getHealthBonus(),
                EntityAttributeModifier.Operation.ADD_VALUE
        );

        applyModifier(
                player,
                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                SPEED_MODIFIER_ID,
                profile.getSpeedModifier(),
                EntityAttributeModifier.Operation.ADD_VALUE
        );

        // Pillager: set attack damage (base player = 1, pillager unarmed = ~2 which scales with difficulty)
        if (type == EntityType.PILLAGER) {
            applyModifier(
                    player,
                    EntityAttributes.GENERIC_ATTACK_DAMAGE,
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
                    EntityAttributes.GENERIC_ATTACK_DAMAGE,
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
                    EntityAttributes.GENERIC_ATTACK_DAMAGE,
                    PILLAGER_ATTACK_MODIFIER_ID,
                    1.0, // +1 on top of player base 1 = 2 HP = 1 heart on Easy
                    EntityAttributeModifier.Operation.ADD_VALUE
            );
        }
    }

    public static void clear(ServerPlayerEntity player) {
        removeModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER_ID);
        removeModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, PILLAGER_ATTACK_MODIFIER_ID);
        removeModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, VINDICATOR_ATTACK_MODIFIER_ID);
        removeModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER_ID);

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