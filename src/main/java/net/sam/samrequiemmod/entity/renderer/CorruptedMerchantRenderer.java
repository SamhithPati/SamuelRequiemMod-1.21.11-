package net.sam.samrequiemmod.entity.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.util.Identifier;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.entity.CorruptedMerchantEntity;

public class CorruptedMerchantRenderer extends MobEntityRenderer<CorruptedMerchantEntity, VillagerResemblingModel<CorruptedMerchantEntity>> {

    private static final Identifier TEXTURE =
            Identifier.of(SamuelRequiemMod.MOD_ID, "textures/entity/corrupted_merchant.png");

    public CorruptedMerchantRenderer(EntityRendererFactory.Context context) {
        super(context,
                new VillagerResemblingModel<>(context.getPart(EntityModelLayers.VILLAGER)),
                0.5F);
    }

    @Override
    public Identifier getTexture(CorruptedMerchantEntity entity) {
        return TEXTURE;
    }
}