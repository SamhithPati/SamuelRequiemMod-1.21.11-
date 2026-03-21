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

        if (type == null) return;

        PossessionProfile profile;
        if (type == EntityType.ZOMBIE && BabyZombieState.isBaby(player)) {
            profile = PossessionProfiles.BABY_ZOMBIE_PROFILE;
        } else if (type == EntityType.HUSK && BabyHuskState.isBaby(player)) {
            profile = PossessionProfiles.BABY_HUSK_PROFILE;
        } else if (type == EntityType.DROWNED && BabyDrownedState.isBaby(player)) {
            profile = PossessionProfiles.BABY_DROWNED_PROFILE;
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