package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.ShulkerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerEntity.class)
public interface ShulkerEntityPeekAccessor {
    @Accessor("PEEK_AMOUNT")
    static TrackedData<Byte> getPeekAmountKey() {
        throw new AssertionError();
    }
}






