package net.sam.samrequiemmod.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.PossessionProfile;
import net.sam.samrequiemmod.possession.PossessionProfiles;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDimensionsMixin {

    @Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true)
    private void samrequiemmod$overridePossessionDimensions(
            EntityPose pose,
            CallbackInfoReturnable<EntityDimensions> cir
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        EntityType<?> type = PossessionManager.getPossessedType(player);

        // On the client, PossessionManager has no data — check ClientPossessionState instead
        if (type == null && player.getWorld().isClient) {
            type = net.sam.samrequiemmod.possession.ClientPossessionState.get(player);
        }

        if (type == null) return;

        PossessionProfile profile;
        if (type == EntityType.ZOMBIE && BabyZombieState.isBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIE_PROFILE;
        } else if (type == EntityType.HUSK && BabyHuskState.isBaby(player)) {
            profile = PossessionProfiles.BABY_HUSK_PROFILE;
        } else if (type == EntityType.DROWNED && BabyDrownedState.isBaby(player)) {
            profile = PossessionProfiles.BABY_DROWNED_PROFILE;
        } else if (type == EntityType.ZOMBIE_VILLAGER && net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.isBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIE_VILLAGER_PROFILE;
        } else if (type == EntityType.COW && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_COW_PROFILE;
        } else if (type == EntityType.PIG && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_PIG_PROFILE;
        } else if (type == EntityType.SHEEP && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_SHEEP_PROFILE;
        } else if (type == EntityType.CHICKEN && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_CHICKEN_PROFILE;
        } else if (type == EntityType.FOX && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_FOX_PROFILE;
        } else if (type == EntityType.OCELOT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_OCELOT_PROFILE;
        } else if (type == EntityType.CAT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_CAT_PROFILE;
        } else if (type == EntityType.HORSE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_HORSE_PROFILE;
        } else if (type == EntityType.MULE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_MULE_PROFILE;
        } else if (type == EntityType.GOAT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_GOAT_PROFILE;
        } else if (type == EntityType.POLAR_BEAR && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_POLAR_BEAR_PROFILE;
        } else if (type == EntityType.RABBIT && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_RABBIT_PROFILE;
        } else if (type == EntityType.TURTLE && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_TURTLE_PROFILE;
        } else if (type == EntityType.STRIDER && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_STRIDER_PROFILE;
        } else if (type == EntityType.AXOLOTL && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_AXOLOTL_PROFILE;
        } else if (type == EntityType.CAMEL && net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isBaby(player)) {
            profile = PossessionProfiles.BABY_CAMEL_PROFILE;
        } else if (type == EntityType.PIGLIN && net.sam.samrequiemmod.possession.piglin.BabyPiglinState.isBaby(player)) {
            profile = PossessionProfiles.BABY_PIGLIN_PROFILE;
        } else if (type == EntityType.ZOMBIFIED_PIGLIN && net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.isBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIFIED_PIGLIN_PROFILE;
        } else if (type == EntityType.HOGLIN && net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isBaby(player)) {
            profile = PossessionProfiles.BABY_HOGLIN_PROFILE;
        } else if (type == EntityType.ZOGLIN && net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isBaby(player)) {
            profile = PossessionProfiles.BABY_ZOGLIN_PROFILE;
        } else if (type == EntityType.SLIME) {
            int size = net.sam.samrequiemmod.possession.slime.SlimeSizeState.getSize(player);
            profile = switch (size) {
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_SLIME_PROFILE;
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.SMALL -> PossessionProfiles.SMALL_SLIME_PROFILE;
                default -> PossessionProfiles.BIG_SLIME_PROFILE;
            };
        } else if (type == EntityType.MAGMA_CUBE) {
            int size = net.sam.samrequiemmod.possession.slime.SlimeSizeState.getSize(player);
            profile = switch (size) {
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.MEDIUM -> PossessionProfiles.MEDIUM_MAGMA_CUBE_PROFILE;
                case net.sam.samrequiemmod.possession.slime.SlimeSizeState.SMALL -> PossessionProfiles.SMALL_MAGMA_CUBE_PROFILE;
                default -> PossessionProfiles.BIG_MAGMA_CUBE_PROFILE;
            };
        } else if (type == EntityType.WOLF && net.sam.samrequiemmod.possession.wolf.WolfBabyState.isBaby(player)) {
            profile = PossessionProfiles.BABY_WOLF_PROFILE;
        } else if (type == EntityType.VILLAGER && net.sam.samrequiemmod.possession.villager.VillagerState.isBaby(player)) {
            profile = PossessionProfiles.BABY_VILLAGER_PROFILE;
        } else {
            profile = PossessionProfiles.get(type);
        }

        if (profile == null) return;

        EntityDimensions custom = EntityDimensions
                .changing(profile.getWidth(), profile.getHeight())
                .withEyeHeight(profile.getEyeHeight());

        cir.setReturnValue(custom);
    }
}
