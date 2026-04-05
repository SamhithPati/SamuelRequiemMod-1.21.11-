package net.sam.samrequiemmod.entity.renderer;

import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.entity.SoulBossEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SoulBossModel extends DefaultedEntityGeoModel<SoulBossEntity> {
    public SoulBossModel() {
        super(Identifier.of(SamuelRequiemMod.MOD_ID, "soul_boss"), "h_head");
    }
}
