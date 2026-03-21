package net.sam.samrequiemmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.sam.samrequiemmod.entity.ModEntities;
import net.sam.samrequiemmod.entity.renderer.CorruptedMerchantRenderer;
import net.sam.samrequiemmod.possession.PossessionNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;

public class SamuelRequiemModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.CORRUPTED_MERCHANT, CorruptedMerchantRenderer::new);
        PossessionNetworking.registerClient();
        ZombieAttackSyncNetworking.registerClient();
        BabyZombieNetworking.registerClient();
        BabyHuskNetworking.registerClient();
        BabyDrownedNetworking.registerClient();
        net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.registerClient();
        net.sam.samrequiemmod.possession.WaterShakeNetworking.registerClient();
        net.sam.samrequiemmod.possession.illager.EvokerNetworking.registerClient();
        net.sam.samrequiemmod.client.EvokerHudRenderer.register();
        net.sam.samrequiemmod.possession.zombie.ChickenJumpNetworking.registerClient();
    }
}