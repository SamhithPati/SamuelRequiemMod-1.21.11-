package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.StriderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StriderEntity.class)
public interface StriderEntityColdAccessor {
    @Accessor("COLD")
    static TrackedData<Boolean> getColdKey() {
        throw new AssertionError();
    }
}






