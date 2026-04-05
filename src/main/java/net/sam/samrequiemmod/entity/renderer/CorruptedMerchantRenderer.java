package net.sam.samrequiemmod.entity.renderer;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.render.entity.state.VillagerEntityRenderState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.entity.CorruptedMerchantEntity;

public class CorruptedMerchantRenderer extends MobEntityRenderer<CorruptedMerchantEntity, VillagerEntityRenderState, VillagerResemblingModel> {

    private static final Identifier TEXTURE =
            Identifier.of(SamuelRequiemMod.MOD_ID, "textures/entity/corrupted_merchant.png");

    public CorruptedMerchantRenderer(EntityRendererFactory.Context context) {
        super(context, new VillagerResemblingModel(context.getPart(EntityModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public Identifier getTexture(VillagerEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillagerEntityRenderState createRenderState() {
        return new VillagerEntityRenderState();
    }

    @Override
    public void updateRenderState(CorruptedMerchantEntity entity, VillagerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        RegistryEntry<VillagerType> type = Registries.VILLAGER_TYPE.getEntry(Registries.VILLAGER_TYPE.get(VillagerType.PLAINS));
        RegistryEntry<VillagerProfession> profession = Registries.VILLAGER_PROFESSION.getEntry(Registries.VILLAGER_PROFESSION.get(VillagerProfession.NONE));
        state.villagerData = new VillagerData(type, profession, 1);
    }
}






