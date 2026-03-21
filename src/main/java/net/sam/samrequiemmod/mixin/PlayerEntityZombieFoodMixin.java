package net.sam.samrequiemmod.mixin;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sam.samrequiemmod.possession.zombie.BabyZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityZombieFoodMixin {

    @Inject(method = "eatFood", at = @At("RETURN"))
    private void samrequiemmod$healZombieFood(
            World world,
            ItemStack stack,
            FoodComponent foodComponent,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (world.isClient) return;

        // Handle both adult and baby zombie possession
        boolean isZombie = ZombiePossessionController.isZombiePossessing(player);
        boolean isBabyZombie = BabyZombiePossessionController.isBabyZombiePossessing(player);
        boolean isHusk = net.sam.samrequiemmod.possession.husk.HuskPossessionController.isHuskPossessing(player);
        boolean isBabyHusk = net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController.isBabyHuskPossessing(player);

        boolean isDrowned = net.sam.samrequiemmod.possession.drowned.DrownedPossessionController.isDrownedPossessing(player);
        boolean isBabyDrowned = net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController.isBabyDrownedPossessing(player);
        if (!isZombie && !isBabyZombie && !isHusk && !isBabyHusk && !isDrowned && !isBabyDrowned) return;
        if (!ZombiePossessionController.isZombieFood(stack)) return;

        float healAmount = ZombiePossessionController.getZombieFoodHealing(stack);
        if (healAmount > 0.0f) {
            player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        }
    }
}