package net.sam.samrequiemmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;
import net.sam.samrequiemmod.entity.ModEntities;
import net.sam.samrequiemmod.entity.renderer.CorruptedMerchantRenderer;
import net.sam.samrequiemmod.entity.renderer.SoulBossRenderer;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.PossessionNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;

public class SamuelRequiemModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.CORRUPTED_MERCHANT, CorruptedMerchantRenderer::new);
        EntityRendererRegistry.register(ModEntities.SOUL_BOSS, SoulBossRenderer::new);
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
        net.sam.samrequiemmod.possession.illager.RavagerJumpNetworking.registerClient();
        net.sam.samrequiemmod.possession.illager.CaptainNetworking.registerClient();
        net.sam.samrequiemmod.possession.illager.RavagerNetworking.registerClient();
        net.sam.samrequiemmod.client.RavagerHudHandler.register();
        net.sam.samrequiemmod.possession.illager.WitchNetworking.registerClient();
        net.sam.samrequiemmod.client.WitchHudRenderer.register();
        net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.registerClient();
        net.sam.samrequiemmod.possession.passive.MooshroomNetworking.registerClient();
        net.sam.samrequiemmod.possession.passive.SheepAppearanceNetworking.registerClient();
        net.sam.samrequiemmod.possession.passive.PandaAppearanceNetworking.registerClient();
        net.sam.samrequiemmod.possession.iron_golem.IronGolemNetworking.registerClient();
        net.sam.samrequiemmod.client.IronGolemHudHandler.register();
        net.sam.samrequiemmod.possession.skeleton.WitherSkeletonAttackNetworking.registerClient();
        net.sam.samrequiemmod.possession.skeleton.SpiderJumpNetworking.registerClient();
        net.sam.samrequiemmod.possession.enderman.EndermanNetworking.registerClient();
        net.sam.samrequiemmod.possession.creeper.CreeperNetworking.registerClient();
        net.sam.samrequiemmod.possession.aquatic.PufferfishNetworking.registerClient();
        net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantNetworking.registerClient();
        net.sam.samrequiemmod.client.NautilusHudRenderer.register();
        net.sam.samrequiemmod.possession.piglin.BabyPiglinNetworking.registerClient();
        net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinNetworking.registerClient();
        net.sam.samrequiemmod.possession.hoglin.BabyHoglinNetworking.registerClient();
        net.sam.samrequiemmod.possession.hoglin.HoglinAttackNetworking.registerClient();
        net.sam.samrequiemmod.possession.guardian.GuardianNetworking.registerClient();
        net.sam.samrequiemmod.client.GuardianHudRenderer.register();
        net.sam.samrequiemmod.possession.warden.WardenNetworking.registerClient();
        net.sam.samrequiemmod.client.WardenHudRenderer.register();
        net.sam.samrequiemmod.possession.breeze.BreezeNetworking.registerClient();
        net.sam.samrequiemmod.client.BreezeHudRenderer.register();
        net.sam.samrequiemmod.client.SilverfishHideClientHandler.register();
        net.sam.samrequiemmod.possession.firemob.FireMobNetworking.registerClient();
        net.sam.samrequiemmod.client.FireMobAttackClientHandler.register();
        net.sam.samrequiemmod.possession.slime.SlimeSizeNetworking.registerClient();
        net.sam.samrequiemmod.possession.wolf.WolfNetworking.registerClient();
        net.sam.samrequiemmod.possession.fox.FoxNetworking.registerClient();
        net.sam.samrequiemmod.possession.feline.CatNetworking.registerClient();
        net.sam.samrequiemmod.possession.vex.VexNetworking.registerClient();
        net.sam.samrequiemmod.possession.villager.VillagerNetworking.registerClient();
        net.sam.samrequiemmod.possession.beast.BeastNetworking.registerClient();
        net.sam.samrequiemmod.possession.beast.BeastAttackNetworking.registerClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            EntityType<?> type = ClientPossessionState.get(client.player);
            boolean fireproof = type == EntityType.BLAZE
                    || type == EntityType.GHAST
                    || type == EntityType.WITHER_SKELETON
                    || type == EntityType.WARDEN
                    || type == EntityType.MAGMA_CUBE
                    || type == EntityType.STRIDER
                    || type == EntityType.PIGLIN
                    || type == EntityType.PIGLIN_BRUTE
                    || type == EntityType.ZOMBIFIED_PIGLIN;
            if (!fireproof) return;

            client.player.setOnFire(false);
            client.player.setFireTicks(0);
            client.player.extinguish();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPossessionState.clearAll();
            net.sam.samrequiemmod.possession.passive.SheepAppearanceState.clearAllClient();
            net.sam.samrequiemmod.possession.passive.PandaAppearanceState.clearAllClient();
        });
    }
}






