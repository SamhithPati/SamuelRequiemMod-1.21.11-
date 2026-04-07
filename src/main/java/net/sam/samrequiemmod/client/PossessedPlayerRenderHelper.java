package net.sam.samrequiemmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.BoggedEntity;
import net.minecraft.entity.mob.ParchedEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.CowVariant;
import net.minecraft.entity.passive.CowVariants;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.NautilusEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PigVariant;
import net.minecraft.entity.passive.PigVariants;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.CodEntity;
import net.minecraft.entity.passive.SalmonEntity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.sam.samrequiemmod.client.CrossbowAnimationOverride;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.passive.PandaAppearanceState;
import net.sam.samrequiemmod.possession.passive.SheepAppearanceState;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackClientState;
import net.sam.samrequiemmod.possession.passive.MooshroomClientState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PossessedPlayerRenderHelper {

    private static final java.lang.invoke.VarHandle CREEPER_CURRENT_FUSE_TIME;
    private static final java.lang.invoke.VarHandle CREEPER_LAST_FUSE_TIME;
    static {
        try {
            // Creeper fuse time accessors
            java.lang.invoke.MethodHandles.Lookup creeperLookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.mob.CreeperEntity.class,
                            java.lang.invoke.MethodHandles.lookup());
            CREEPER_CURRENT_FUSE_TIME = creeperLookup.findVarHandle(net.minecraft.entity.mob.CreeperEntity.class, "currentFuseTime", int.class);
            CREEPER_LAST_FUSE_TIME = creeperLookup.findVarHandle(net.minecraft.entity.mob.CreeperEntity.class, "lastFuseTime", int.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access render animation fields", e);
        }
    }

    private static ZombieEntity cachedZombie;
    private static DrownedEntity cachedDrowned;
    private static HuskEntity cachedHusk;
    private static SkeletonEntity cachedSkeleton;
    private static PiglinEntity cachedPiglin;
    private static PillagerEntity cachedPillager;
    private static EvokerEntity cachedEvoker;
    private static VindicatorEntity cachedVindicator;
    private static PiglinBruteEntity cachedPiglinBrute;
    private static SpiderEntity cachedSpider;
    private static CaveSpiderEntity cachedCaveSpider;
    private static HoglinEntity cachedHoglin;
    private static GuardianEntity cachedGuardian;
    private static SilverfishEntity cachedSilverfish;
    private static BlazeEntity cachedBlaze;
    private static ZombieVillagerEntity cachedZombieVillager;
    private static RavagerEntity cachedRavager;
    private static WitchEntity cachedWitch;
    private static IronGolemEntity cachedIronGolem;
    private static BreezeEntity cachedBreeze;
    private static CowEntity cachedCow;
    private static MooshroomEntity cachedMooshroom;
    private static PigEntity cachedPig;
    private static SheepEntity cachedSheep;
    private static ChickenEntity cachedChicken;
    private static PandaEntity cachedPanda;
    private static BoggedEntity cachedBogged;
    private static ParchedEntity cachedParched;
    private static StrayEntity cachedStray;
    private static WitherSkeletonEntity cachedWitherSkeleton;
    private static EndermanEntity cachedEnderman;
    private static WardenEntity cachedWarden;
    private static CreeperEntity cachedCreeper;
    private static CodEntity cachedCod;
    private static SalmonEntity cachedSalmon;
    private static PufferfishEntity cachedPufferfish;
    private static TropicalFishEntity cachedTropicalFish;
    private static SquidEntity cachedSquid;
    private static DolphinEntity cachedDolphin;
    private static NautilusEntity cachedNautilus;
    private static ZombieNautilusEntity cachedZombieNautilus;
    private static ZombifiedPiglinEntity cachedZombifiedPiglin;
    private static ZoglinEntity cachedZoglin;
    private static ElderGuardianEntity cachedElderGuardian;
    private static GhastEntity cachedGhast;
    private static SlimeEntity cachedSlime;
    private static MagmaCubeEntity cachedMagmaCube;
    private static WolfEntity cachedWolf;
    private static FoxEntity cachedFox;
    private static OcelotEntity cachedOcelot;
    private static CatEntity cachedCat;
    private static FrogEntity cachedFrog;
    private static VexEntity cachedVex;
    private static BatEntity cachedBat;
    private static BeeEntity cachedBee;
    private static ParrotEntity cachedParrot;
    private static VillagerEntity cachedVillager;
    private static HorseEntity cachedHorse;
    private static MuleEntity cachedMule;
    private static ZombieHorseEntity cachedZombieHorse;
    private static final Map<UUID, Integer> LAST_WARDEN_ATTACK_START = new ConcurrentHashMap<>();
    private static SkeletonHorseEntity cachedSkeletonHorse;
    private static EndermiteEntity cachedEndermite;
    private static GoatEntity cachedGoat;
    private static PolarBearEntity cachedPolarBear;
    private static RabbitEntity cachedRabbit;
    private static TurtleEntity cachedTurtle;
    private static ShulkerEntity cachedShulker;
    private static StriderEntity cachedStrider;
    private static AxolotlEntity cachedAxolotl;
    private static SnowGolemEntity cachedSnowGolem;
    private static CamelEntity cachedCamel;
    private static ArmadilloEntity cachedArmadillo;
    private static final Map<UUID, Integer> LAST_ENDERMAN_PARTICLE_TICK = new ConcurrentHashMap<>();

    private PossessedPlayerRenderHelper() {
    }

    public static boolean shouldRenderAsPossessed(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) != null;
    }

    public static void renderPossessed(
            AbstractClientPlayerEntity player,
            float tickDelta,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState
    ) {
        EntityType<?> type = ClientPossessionState.get(player);
        if (type == null) return;

        LivingEntity shell = getOrCreateShell(type, player.getEntityWorld());
        if (shell == null) return;

        copyPlayerStateToShell(player, shell, tickDelta);
        EntityRenderManager renderManager = MinecraftClient.getInstance().getEntityRenderDispatcher();
        var shellState = renderManager.getAndUpdateRenderState(shell, tickDelta);
        @SuppressWarnings({"rawtypes", "unchecked"})
        EntityRenderer renderer = renderManager.getRenderer(shell);
        matrices.push();
        if (type == EntityType.ENDERMAN
                && net.sam.samrequiemmod.possession.enderman.EndermanClientState.isAngry(player.getUuid())) {
            double jitter = 0.02 * shell.getScale();
            matrices.translate(
                    player.getRandom().nextGaussian() * jitter,
                    0.0,
                    player.getRandom().nextGaussian() * jitter
            );
        }
        renderer.render(shellState, matrices, queue, cameraState);
        matrices.pop();
    }

    private static void spawnEndermanParticles(AbstractClientPlayerEntity player) {
        Integer lastTick = LAST_ENDERMAN_PARTICLE_TICK.get(player.getUuid());
        if (lastTick != null && lastTick == player.age) {
            return;
        }
        LAST_ENDERMAN_PARTICLE_TICK.put(player.getUuid(), player.age);

        for (int i = 0; i < 2; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * player.getWidth();
            double y = player.getBodyY(player.getRandom().nextDouble());
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * player.getWidth();
            double vx = (player.getRandom().nextDouble() - 0.5) * 0.6;
            double vy = (player.getRandom().nextDouble() - 0.5) * 0.2;
            double vz = (player.getRandom().nextDouble() - 0.5) * 0.6;
            MinecraftClient.getInstance().world.addParticleClient(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }

    private static boolean isFireproofPossession(AbstractClientPlayerEntity player) {
        EntityType<?> type = net.sam.samrequiemmod.possession.ClientPossessionState.get(player);
        return type == EntityType.BLAZE
                || type == EntityType.GHAST
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.WARDEN
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.STRIDER
                || type == EntityType.PIGLIN
                || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.ZOMBIFIED_PIGLIN;
    }

    private static RegistryEntry<CowVariant> getCowVariantForBiome(AbstractClientPlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.COW_VARIANT);
        var biome = player.getEntityWorld().getBiome(player.getBlockPos());
        if (biome.isIn(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
            return registry.getOrThrow(CowVariants.WARM);
        }
        if (biome.isIn(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS)) {
            return registry.getOrThrow(CowVariants.COLD);
        }
        return registry.getOrThrow(CowVariants.TEMPERATE);
    }

    private static RegistryEntry<PigVariant> getPigVariantForBiome(AbstractClientPlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.PIG_VARIANT);
        var biome = player.getEntityWorld().getBiome(player.getBlockPos());
        if (biome.isIn(BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) {
            return registry.getOrThrow(PigVariants.WARM);
        }
        if (biome.isIn(BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS)) {
            return registry.getOrThrow(PigVariants.COLD);
        }
        return registry.getOrThrow(PigVariants.TEMPERATE);
    }

    private static LivingEntity getOrCreateShell(EntityType<?> type, World world) {
        if (type == EntityType.ZOMBIE) {
            if (cachedZombie == null || cachedZombie.getEntityWorld() != world)
                cachedZombie = new ZombieEntity(EntityType.ZOMBIE, world);
            return cachedZombie;
        }
        if (type == EntityType.HUSK) {
            if (cachedHusk == null || cachedHusk.getEntityWorld() != world)
                cachedHusk = new HuskEntity(EntityType.HUSK, world);
            return cachedHusk;
        }
        if (type == EntityType.DROWNED) {
            if (cachedDrowned == null || cachedDrowned.getEntityWorld() != world)
                cachedDrowned = new DrownedEntity(EntityType.DROWNED, world);
            return cachedDrowned;
        }
        if (type == EntityType.SKELETON) {
            if (cachedSkeleton == null || cachedSkeleton.getEntityWorld() != world)
                cachedSkeleton = new SkeletonEntity(EntityType.SKELETON, world);
            return cachedSkeleton;
        }
        if (type == EntityType.BOGGED) {
            if (cachedBogged == null || cachedBogged.getEntityWorld() != world)
                cachedBogged = new BoggedEntity(EntityType.BOGGED, world);
            return cachedBogged;
        }
        if (type == EntityType.PARCHED) {
            if (cachedParched == null || cachedParched.getEntityWorld() != world)
                cachedParched = new ParchedEntity(EntityType.PARCHED, world);
            return cachedParched;
        }
        if (type == EntityType.STRAY) {
            if (cachedStray == null || cachedStray.getEntityWorld() != world)
                cachedStray = new StrayEntity(EntityType.STRAY, world);
            return cachedStray;
        }
        if (type == EntityType.WITHER_SKELETON) {
            if (cachedWitherSkeleton == null || cachedWitherSkeleton.getEntityWorld() != world)
                cachedWitherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
            return cachedWitherSkeleton;
        }
        if (type == EntityType.PILLAGER) {
            if (cachedPillager == null || cachedPillager.getEntityWorld() != world)
                cachedPillager = new PillagerEntity(EntityType.PILLAGER, world);
            return cachedPillager;
        }
        if (type == EntityType.VINDICATOR) {
            if (cachedVindicator == null || cachedVindicator.getEntityWorld() != world)
                cachedVindicator = new VindicatorEntity(EntityType.VINDICATOR, world);
            return cachedVindicator;
        }
        if (type == EntityType.EVOKER) {
            if (cachedEvoker == null || cachedEvoker.getEntityWorld() != world)
                cachedEvoker = new EvokerEntity(EntityType.EVOKER, world);
            return cachedEvoker;
        }
        if (type == EntityType.PIGLIN) {
            if (cachedPiglin == null || cachedPiglin.getEntityWorld() != world)
                cachedPiglin = new PiglinEntity(EntityType.PIGLIN, world);
            return cachedPiglin;
        }
        if (type == EntityType.PIGLIN_BRUTE) {
            if (cachedPiglinBrute == null || cachedPiglinBrute.getEntityWorld() != world)
                cachedPiglinBrute = new PiglinBruteEntity(EntityType.PIGLIN_BRUTE, world);
            return cachedPiglinBrute;
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN) {
            if (cachedZombifiedPiglin == null || cachedZombifiedPiglin.getEntityWorld() != world)
                cachedZombifiedPiglin = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
            return cachedZombifiedPiglin;
        }
        if (type == EntityType.SPIDER) {
            if (cachedSpider == null || cachedSpider.getEntityWorld() != world)
                cachedSpider = new SpiderEntity(EntityType.SPIDER, world);
            return cachedSpider;
        }
        if (type == EntityType.CAVE_SPIDER) {
            if (cachedCaveSpider == null || cachedCaveSpider.getEntityWorld() != world)
                cachedCaveSpider = new CaveSpiderEntity(EntityType.CAVE_SPIDER, world);
            return cachedCaveSpider;
        }
        if (type == EntityType.HOGLIN) {
            if (cachedHoglin == null || cachedHoglin.getEntityWorld() != world)
                cachedHoglin = new HoglinEntity(EntityType.HOGLIN, world);
            return cachedHoglin;
        }
        if (type == EntityType.ZOGLIN) {
            if (cachedZoglin == null || cachedZoglin.getEntityWorld() != world)
                cachedZoglin = new ZoglinEntity(EntityType.ZOGLIN, world);
            return cachedZoglin;
        }
        if (type == EntityType.GUARDIAN) {
            if (cachedGuardian == null || cachedGuardian.getEntityWorld() != world)
                cachedGuardian = new GuardianEntity(EntityType.GUARDIAN, world);
            return cachedGuardian;
        }
        if (type == EntityType.ELDER_GUARDIAN) {
            if (cachedElderGuardian == null || cachedElderGuardian.getEntityWorld() != world)
                cachedElderGuardian = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, world);
            return cachedElderGuardian;
        }
        if (type == EntityType.SILVERFISH) {
            if (cachedSilverfish == null || cachedSilverfish.getEntityWorld() != world)
                cachedSilverfish = new SilverfishEntity(EntityType.SILVERFISH, world);
            return cachedSilverfish;
        }
        if (type == EntityType.BLAZE) {
            if (cachedBlaze == null || cachedBlaze.getEntityWorld() != world)
                cachedBlaze = new BlazeEntity(EntityType.BLAZE, world);
            return cachedBlaze;
        }
        if (type == EntityType.GHAST) {
            if (cachedGhast == null || cachedGhast.getEntityWorld() != world)
                cachedGhast = new GhastEntity(EntityType.GHAST, world);
            return cachedGhast;
        }
        if (type == EntityType.SLIME) {
            if (cachedSlime == null || cachedSlime.getEntityWorld() != world)
                cachedSlime = new SlimeEntity(EntityType.SLIME, world);
            return cachedSlime;
        }
        if (type == EntityType.MAGMA_CUBE) {
            if (cachedMagmaCube == null || cachedMagmaCube.getEntityWorld() != world)
                cachedMagmaCube = new MagmaCubeEntity(EntityType.MAGMA_CUBE, world);
            return cachedMagmaCube;
        }
        if (type == EntityType.WOLF) {
            if (cachedWolf == null || cachedWolf.getEntityWorld() != world)
                cachedWolf = new WolfEntity(EntityType.WOLF, world);
            return cachedWolf;
        }
        if (type == EntityType.FOX) {
            if (cachedFox == null || cachedFox.getEntityWorld() != world)
                cachedFox = new FoxEntity(EntityType.FOX, world);
            return cachedFox;
        }
        if (type == EntityType.OCELOT) {
            if (cachedOcelot == null || cachedOcelot.getEntityWorld() != world)
                cachedOcelot = new OcelotEntity(EntityType.OCELOT, world);
            return cachedOcelot;
        }
        if (type == EntityType.CAT) {
            if (cachedCat == null || cachedCat.getEntityWorld() != world)
                cachedCat = new CatEntity(EntityType.CAT, world);
            return cachedCat;
        }
        if (type == EntityType.FROG) {
            if (cachedFrog == null || cachedFrog.getEntityWorld() != world)
                cachedFrog = new FrogEntity(EntityType.FROG, world);
            return cachedFrog;
        }
        if (type == EntityType.VEX) {
            if (cachedVex == null || cachedVex.getEntityWorld() != world)
                cachedVex = new VexEntity(EntityType.VEX, world);
            return cachedVex;
        }
        if (type == EntityType.BAT) {
            if (cachedBat == null || cachedBat.getEntityWorld() != world)
                cachedBat = new BatEntity(EntityType.BAT, world);
            return cachedBat;
        }
        if (type == EntityType.BEE) {
            if (cachedBee == null || cachedBee.getEntityWorld() != world)
                cachedBee = new BeeEntity(EntityType.BEE, world);
            return cachedBee;
        }
        if (type == EntityType.PARROT) {
            if (cachedParrot == null || cachedParrot.getEntityWorld() != world)
                cachedParrot = new ParrotEntity(EntityType.PARROT, world);
            return cachedParrot;
        }
        if (type == EntityType.VILLAGER) {
            if (cachedVillager == null || cachedVillager.getEntityWorld() != world)
                cachedVillager = new VillagerEntity(EntityType.VILLAGER, world);
            return cachedVillager;
        }
        if (type == EntityType.HORSE) {
            if (cachedHorse == null || cachedHorse.getEntityWorld() != world)
                cachedHorse = new HorseEntity(EntityType.HORSE, world);
            return cachedHorse;
        }
        if (type == EntityType.MULE) {
            if (cachedMule == null || cachedMule.getEntityWorld() != world)
                cachedMule = new MuleEntity(EntityType.MULE, world);
            return cachedMule;
        }
        if (type == EntityType.ZOMBIE_HORSE) {
            if (cachedZombieHorse == null || cachedZombieHorse.getEntityWorld() != world)
                cachedZombieHorse = new ZombieHorseEntity(EntityType.ZOMBIE_HORSE, world);
            return cachedZombieHorse;
        }
        if (type == EntityType.SKELETON_HORSE) {
            if (cachedSkeletonHorse == null || cachedSkeletonHorse.getEntityWorld() != world)
                cachedSkeletonHorse = new SkeletonHorseEntity(EntityType.SKELETON_HORSE, world);
            return cachedSkeletonHorse;
        }
        if (type == EntityType.ENDERMITE) {
            if (cachedEndermite == null || cachedEndermite.getEntityWorld() != world)
                cachedEndermite = new EndermiteEntity(EntityType.ENDERMITE, world);
            return cachedEndermite;
        }
        if (type == EntityType.GOAT) {
            if (cachedGoat == null || cachedGoat.getEntityWorld() != world)
                cachedGoat = new GoatEntity(EntityType.GOAT, world);
            return cachedGoat;
        }
        if (type == EntityType.POLAR_BEAR) {
            if (cachedPolarBear == null || cachedPolarBear.getEntityWorld() != world)
                cachedPolarBear = new PolarBearEntity(EntityType.POLAR_BEAR, world);
            return cachedPolarBear;
        }
        if (type == EntityType.RABBIT) {
            if (cachedRabbit == null || cachedRabbit.getEntityWorld() != world)
                cachedRabbit = new RabbitEntity(EntityType.RABBIT, world);
            return cachedRabbit;
        }
        if (type == EntityType.TURTLE) {
            if (cachedTurtle == null || cachedTurtle.getEntityWorld() != world)
                cachedTurtle = new TurtleEntity(EntityType.TURTLE, world);
            return cachedTurtle;
        }
        if (type == EntityType.SHULKER) {
            if (cachedShulker == null || cachedShulker.getEntityWorld() != world)
                cachedShulker = new ShulkerEntity(EntityType.SHULKER, world);
            return cachedShulker;
        }
        if (type == EntityType.STRIDER) {
            if (cachedStrider == null || cachedStrider.getEntityWorld() != world)
                cachedStrider = new StriderEntity(EntityType.STRIDER, world);
            return cachedStrider;
        }
        if (type == EntityType.AXOLOTL) {
            if (cachedAxolotl == null || cachedAxolotl.getEntityWorld() != world)
                cachedAxolotl = new AxolotlEntity(EntityType.AXOLOTL, world);
            return cachedAxolotl;
        }
        if (type == EntityType.SNOW_GOLEM) {
            if (cachedSnowGolem == null || cachedSnowGolem.getEntityWorld() != world)
                cachedSnowGolem = new SnowGolemEntity(EntityType.SNOW_GOLEM, world);
            return cachedSnowGolem;
        }
        if (type == EntityType.CAMEL) {
            if (cachedCamel == null || cachedCamel.getEntityWorld() != world)
                cachedCamel = new CamelEntity(EntityType.CAMEL, world);
            return cachedCamel;
        }
        if (type == EntityType.ARMADILLO) {
            if (cachedArmadillo == null || cachedArmadillo.getEntityWorld() != world)
                cachedArmadillo = new ArmadilloEntity(EntityType.ARMADILLO, world);
            return cachedArmadillo;
        }
        if (type == EntityType.ZOMBIE_VILLAGER) {
            if (cachedZombieVillager == null || cachedZombieVillager.getEntityWorld() != world)
                cachedZombieVillager = new ZombieVillagerEntity(EntityType.ZOMBIE_VILLAGER, world);
            return cachedZombieVillager;
        }
        if (type == EntityType.RAVAGER) {
            if (cachedRavager == null || cachedRavager.getEntityWorld() != world)
                cachedRavager = new RavagerEntity(EntityType.RAVAGER, world);
            return cachedRavager;
        }
        if (type == EntityType.WITCH) {
            if (cachedWitch == null || cachedWitch.getEntityWorld() != world)
                cachedWitch = new WitchEntity(EntityType.WITCH, world);
            return cachedWitch;
        }
        if (type == EntityType.IRON_GOLEM) {
            if (cachedIronGolem == null || cachedIronGolem.getEntityWorld() != world)
                cachedIronGolem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
            return cachedIronGolem;
        }
        if (type == EntityType.BREEZE) {
            if (cachedBreeze == null || cachedBreeze.getEntityWorld() != world)
                cachedBreeze = new BreezeEntity(EntityType.BREEZE, world);
            return cachedBreeze;
        }
        if (type == EntityType.COW) {
            if (cachedCow == null || cachedCow.getEntityWorld() != world)
                cachedCow = new CowEntity(EntityType.COW, world);
            return cachedCow;
        }
        if (type == EntityType.MOOSHROOM) {
            if (cachedMooshroom == null || cachedMooshroom.getEntityWorld() != world)
                cachedMooshroom = new MooshroomEntity(EntityType.MOOSHROOM, world);
            return cachedMooshroom;
        }
        if (type == EntityType.PIG) {
            if (cachedPig == null || cachedPig.getEntityWorld() != world)
                cachedPig = new PigEntity(EntityType.PIG, world);
            return cachedPig;
        }
        if (type == EntityType.SHEEP) {
            if (cachedSheep == null || cachedSheep.getEntityWorld() != world)
                cachedSheep = new SheepEntity(EntityType.SHEEP, world);
            return cachedSheep;
        }
        if (type == EntityType.CHICKEN) {
            if (cachedChicken == null || cachedChicken.getEntityWorld() != world)
                cachedChicken = new ChickenEntity(EntityType.CHICKEN, world);
            return cachedChicken;
        }
        if (type == EntityType.PANDA) {
            if (cachedPanda == null || cachedPanda.getEntityWorld() != world)
                cachedPanda = new PandaEntity(EntityType.PANDA, world);
            return cachedPanda;
        }
        if (type == EntityType.ENDERMAN) {
            if (cachedEnderman == null || cachedEnderman.getEntityWorld() != world)
                cachedEnderman = new EndermanEntity(EntityType.ENDERMAN, world);
            return cachedEnderman;
        }
        if (type == EntityType.WARDEN) {
            if (cachedWarden == null || cachedWarden.getEntityWorld() != world)
                cachedWarden = new WardenEntity(EntityType.WARDEN, world);
            return cachedWarden;
        }
        if (type == EntityType.CREEPER) {
            if (cachedCreeper == null || cachedCreeper.getEntityWorld() != world)
                cachedCreeper = new CreeperEntity(EntityType.CREEPER, world);
            return cachedCreeper;
        }
        if (type == EntityType.COD) {
            if (cachedCod == null || cachedCod.getEntityWorld() != world)
                cachedCod = new CodEntity(EntityType.COD, world);
            return cachedCod;
        }
        if (type == EntityType.SALMON) {
            if (cachedSalmon == null || cachedSalmon.getEntityWorld() != world)
                cachedSalmon = new SalmonEntity(EntityType.SALMON, world);
            return cachedSalmon;
        }
        if (type == EntityType.PUFFERFISH) {
            if (cachedPufferfish == null || cachedPufferfish.getEntityWorld() != world)
                cachedPufferfish = new PufferfishEntity(EntityType.PUFFERFISH, world);
            return cachedPufferfish;
        }
        if (type == EntityType.TROPICAL_FISH) {
            if (cachedTropicalFish == null || cachedTropicalFish.getEntityWorld() != world)
                cachedTropicalFish = new TropicalFishEntity(EntityType.TROPICAL_FISH, world);
            return cachedTropicalFish;
        }
        if (type == EntityType.SQUID) {
            if (cachedSquid == null || cachedSquid.getEntityWorld() != world)
                cachedSquid = new SquidEntity(EntityType.SQUID, world);
            return cachedSquid;
        }
        if (type == EntityType.DOLPHIN) {
            if (cachedDolphin == null || cachedDolphin.getEntityWorld() != world)
                cachedDolphin = new DolphinEntity(EntityType.DOLPHIN, world);
            return cachedDolphin;
        }
        if (type == EntityType.NAUTILUS) {
            if (cachedNautilus == null || cachedNautilus.getEntityWorld() != world)
                cachedNautilus = new NautilusEntity(EntityType.NAUTILUS, world);
            return cachedNautilus;
        }
        if (type == EntityType.ZOMBIE_NAUTILUS) {
            if (cachedZombieNautilus == null || cachedZombieNautilus.getEntityWorld() != world)
                cachedZombieNautilus = new ZombieNautilusEntity(EntityType.ZOMBIE_NAUTILUS, world);
            return cachedZombieNautilus;
        }
        return null;
    }

    private static void copyPlayerStateToShell(
            AbstractClientPlayerEntity player,
            LivingEntity shell,
            float tickDelta
    ) {
        // ── Position / rotation ──────────────────────────────────────────────
        shell.setPosition(player.getX(), player.getY(), player.getZ());

        shell.lastRenderX = player.lastRenderX;
        shell.lastRenderY = player.lastRenderY;
        shell.lastRenderZ = player.lastRenderZ;

        shell.lastX = player.lastX;
        shell.lastY = player.lastY;
        shell.lastZ = player.lastZ;

        float yaw   = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);

        if (shell instanceof ShulkerEntity) {
            yaw = 0.0f;
            pitch = 0.0f;
            shell.lastRenderX = player.getX();
            shell.lastRenderY = player.getY();
            shell.lastRenderZ = player.getZ();
            shell.lastX = player.getX();
            shell.lastY = player.getY();
            shell.lastZ = player.getZ();
        }

        shell.setYaw(yaw);
        shell.lastYaw   = player.lastYaw;
        shell.setPitch(pitch);
        shell.lastPitch = player.lastPitch;

        shell.bodyYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
        shell.lastBodyYaw = player.lastBodyYaw;
        shell.headYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.lastHeadYaw, player.headYaw);
        shell.lastHeadYaw = player.lastHeadYaw;

        if (shell instanceof ShulkerEntity) {
            shell.lastYaw = 0.0f;
            shell.lastPitch = 0.0f;
            shell.bodyYaw = 0.0f;
            shell.lastBodyYaw = 0.0f;
            shell.headYaw = 0.0f;
            shell.lastHeadYaw = 0.0f;
        }

        // ── Animation state ──────────────────────────────────────────────────
        shell.handSwingProgress     = player.handSwingProgress;
        shell.lastHandSwingProgress = player.lastHandSwingProgress;
        shell.handSwinging          = player.handSwinging;

        shell.setSneaking(player.isSneaking());
        shell.setSprinting(player.isSprinting());
        shell.setOnGround(player.isOnGround());
        shell.setInvisible(player.isInvisible());
        shell.setSwimming(player.isSwimming());

        // Copy touchingWater so aquatic renderers (fish, etc.) can check isTouchingWater()
        shell.touchingWater = player.isTouchingWater();

        // Copy velocity for animation-driven models (dolphin tail, fish wiggle)
        shell.setVelocity(player.getVelocity());

        shell.hurtTime   = player.hurtTime;
        shell.deathTime  = player.deathTime;
        shell.age        = player.age;

        shell.setHealth(Math.max(1.0F, player.getHealth()));
        shell.fallDistance = player.fallDistance;
        if (isFireproofPossession(player)) {
            shell.setFireTicks(0);
            shell.extinguish();
        } else {
            shell.setFireTicks(player.getFireTicks());
        }

        // Copy the player's current limb animation state directly.
        // Advancing the shell animator every render frame makes walk cycles run too fast.
        var playerLimbAnimator = (net.sam.samrequiemmod.mixin.client.LimbAnimatorAccessor) player.limbAnimator;
        var shellLimbAnimator = (net.sam.samrequiemmod.mixin.client.LimbAnimatorAccessor) shell.limbAnimator;
        shellLimbAnimator.samrequiemmod$setLastSpeed(playerLimbAnimator.samrequiemmod$getLastSpeed());
        shellLimbAnimator.samrequiemmod$setSpeedField(playerLimbAnimator.samrequiemmod$getSpeedField());
        shellLimbAnimator.samrequiemmod$setAnimationProgress(playerLimbAnimator.samrequiemmod$getAnimationProgress());
        shellLimbAnimator.samrequiemmod$setTimeScale(playerLimbAnimator.samrequiemmod$getTimeScale());

        if (shell instanceof ShulkerEntity) {
            shell.setSneaking(false);
            shell.setSprinting(false);
            shell.setSwimming(false);
            shell.setVelocity(0.0, 0.0, 0.0);
            shell.limbAnimator.reset();
        }

        // ── Equipment: copy all held items and worn armour from the player ───
        // The LivingEntity renderer reads these slots directly to draw held
        // items in each hand and all four armour layers.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            shell.equipStack(slot, player.getEquippedStack(slot).copy());
        }

        // ── Mob-specific overrides ───────────────────────────────────────────
        if (shell instanceof PiglinEntity piglin) {
            // Baby piglin state
            boolean isBabyPiglin = net.sam.samrequiemmod.possession.piglin.BabyPiglinState.isClientBaby(player.getUuid());
            piglin.setBaby(isBabyPiglin);

            // Attack animation: drive arms raised via ZombieAttackClientState (reused for piglins)
            boolean armsRaised = ZombieAttackClientState.isAttacking(player.getUuid());
            piglin.setAttacking(armsRaised);

            // Crossbow charging animation (same as pillager)
            net.minecraft.item.ItemStack mainHand = player.getMainHandStack();
            net.minecraft.item.ItemStack offHand = player.getOffHandStack();
            boolean usingCrossbow = player.isUsingItem()
                    && (mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    || offHand.isOf(net.minecraft.item.Items.CROSSBOW));
            net.minecraft.item.ItemStack activeCrossbow = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    ? mainHand : offHand;
            boolean charging = usingCrossbow && !net.minecraft.item.CrossbowItem.isCharged(activeCrossbow);
            piglin.setCharging(charging);

            // Shaking in overworld (piglin conversion)
            // AbstractPiglinEntity.isShaking() checks !isImmuneToZombification() && timeInOverworld > 0
            boolean isShaking = net.sam.samrequiemmod.possession.WaterShakeNetworking.SHAKING_PLAYERS
                    .contains(player.getUuid());
            piglin.setImmuneToZombification(!isShaking);
            piglin.timeInOverworld = isShaking ? 1 : 0;
        }

        // Piglin Brute: attack animation + shaking
        if (shell instanceof PiglinBruteEntity piglinBrute) {
            boolean armsRaised = ZombieAttackClientState.isAttacking(player.getUuid());
            piglinBrute.setAttacking(armsRaised);

            boolean isShaking = net.sam.samrequiemmod.possession.WaterShakeNetworking.SHAKING_PLAYERS
                    .contains(player.getUuid());
            piglinBrute.setImmuneToZombification(!isShaking);
            piglinBrute.timeInOverworld = isShaking ? 1 : 0;
        }

        // Zombified Piglin: baby state + arms raised
        if (shell instanceof ZombifiedPiglinEntity zombifiedPiglin) {
            boolean isBabyZP = net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.isClientBaby(player.getUuid());
            zombifiedPiglin.setBaby(isBabyZP);

            boolean armsRaised = ZombieAttackClientState.isAttacking(player.getUuid());
            ZombieArmsHelper.setArmsRaised(zombifiedPiglin, armsRaised);
        }

        // Pillager crossbow animation: setCharging drives the arm-raise pose,
        // and getItemUseTimeLeft()/getActiveItem() overrides drive the pull-back progress
        // (read by CrossbowPosing.charge() inside IllagerModel.setAngles).
        if (shell instanceof PillagerEntity pillager) {
            net.minecraft.item.ItemStack mainHand = player.getMainHandStack();
            net.minecraft.item.ItemStack offHand  = player.getOffHandStack();
            boolean holdingCrossbow = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    || offHand.isOf(net.minecraft.item.Items.CROSSBOW);
            boolean usingCrossbow = player.isUsingItem() && holdingCrossbow;
            net.minecraft.util.Hand activeHand = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    ? net.minecraft.util.Hand.MAIN_HAND
                    : net.minecraft.util.Hand.OFF_HAND;
            net.minecraft.item.ItemStack activeCrossbow = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    ? mainHand : offHand;
            boolean charging = usingCrossbow && !net.minecraft.item.CrossbowItem.isCharged(activeCrossbow);
            pillager.setCharging(charging);

            net.sam.samrequiemmod.client.IllagerStateSetter stateSetter =
                    (net.sam.samrequiemmod.client.IllagerStateSetter) pillager;
            if (charging) {
                stateSetter.samrequiemmod$setState(net.minecraft.entity.mob.IllagerEntity.State.CROSSBOW_CHARGE);
            } else if (holdingCrossbow) {
                stateSetter.samrequiemmod$setState(net.minecraft.entity.mob.IllagerEntity.State.CROSSBOW_HOLD);
            } else {
                stateSetter.samrequiemmod$setState(net.minecraft.entity.mob.IllagerEntity.State.NEUTRAL);
            }

            CrossbowAnimationOverride override = (CrossbowAnimationOverride) pillager;
            if (charging) {
                pillager.setCurrentHand(activeHand);
                override.samrequiemmod$setUseTimeOverride(player.getItemUseTimeLeft(), player.getItemUseTime(), activeCrossbow);
            } else {
                pillager.stopUsingItem();
                override.samrequiemmod$clearUseTimeOverride();
            }
        }

        // Vindicator attack/celebration animation
        if (shell instanceof VindicatorEntity vindicator) {
            boolean celebrating = net.sam.samrequiemmod.possession.illager.CaptainNetworking
                    .CELEBRATING_PLAYERS.contains(player.getUuid());
            if (celebrating) {
                // setCelebrating is on RaiderEntity, drives getState() -> CELEBRATING
                vindicator.setCelebrating(true);
                vindicator.setAttacking(false);
            } else {
                vindicator.setCelebrating(false);
                long lastAttack = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController
                        .LAST_ATTACK_TICK.getOrDefault(player.getUuid(), -1000L);
                boolean attacking = (player.age - lastAttack) < 100; // 5 seconds = 100 ticks
                vindicator.setAttacking(attacking);
            }
        }

        // Evoker casting animation: drive spell type on shell via ordinal setter
        // Spell ordinals: 0=NONE, 1=SUMMON_VEX, 2=FANGS
        if (shell instanceof EvokerEntity evoker && evoker instanceof net.sam.samrequiemmod.client.EvokerSpellSetter setter) {
            boolean celebrating = net.sam.samrequiemmod.possession.illager.CaptainNetworking
                    .CELEBRATING_PLAYERS.contains(player.getUuid());
            if (celebrating) {
                // setCelebrating is on RaiderEntity, drives getState() -> CELEBRATING
                evoker.setCelebrating(true);
            } else {
                evoker.setCelebrating(false);
                int castType = net.sam.samrequiemmod.possession.illager.EvokerClientState.getCasting(player.getUuid());
                if (castType == 1) {
                    setter.samrequiemmod$setSpellByOrdinal(2); // FANGS
                } else if (castType == 2) {
                    setter.samrequiemmod$setSpellByOrdinal(1); // SUMMON_VEX
                } else {
                    setter.samrequiemmod$setSpellByOrdinal(0); // NONE
                }
            }
        }

        if (shell instanceof ZombieEntity zombie) {
            zombie.setBaby(false);
            boolean armsRaised = ZombieAttackClientState.isAttacking(player.getUuid());
            ZombieArmsHelper.setArmsRaised(zombie, armsRaised);
            // Apply shaking if player is in water conversion state
            boolean isShaking = net.sam.samrequiemmod.possession.WaterShakeNetworking.SHAKING_PLAYERS
                    .contains(player.getUuid());
            net.minecraft.entity.data.TrackedData<Boolean> convertingKey =
                    net.sam.samrequiemmod.mixin.client.ZombieEntityInWaterAccessor.getConvertingInWaterKey();
            zombie.getDataTracker().set(convertingKey, isShaking);
            // Baby zombie
            if (net.sam.samrequiemmod.possession.zombie.BabyZombieState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby husk (HuskEntity extends ZombieEntity, same setBaby logic)
            if (net.sam.samrequiemmod.possession.husk.BabyHuskState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby drowned (DrownedEntity extends ZombieEntity)
            if (net.sam.samrequiemmod.possession.drowned.BabyDrownedState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby zombie villager (ZombieVillagerEntity extends ZombieEntity)
            if (net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby zombified piglin (ZombifiedPiglinEntity extends ZombieEntity)
            if (net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Drowned trident charging pose: when player is using a trident, raise arms
            if (shell instanceof DrownedEntity && player.isUsingItem()) {
                net.minecraft.item.ItemStack activeItem = player.getActiveItem();
                if (activeItem.isOf(net.minecraft.item.Items.TRIDENT)) {
                    ZombieArmsHelper.setArmsRaised(zombie, true);
                }
            }
        }
        // Ravager: drive bite/roar animations via the entity's internal tick fields.
        // Fields are accessible via access widener (samrequiemmod.accesswidener).
        // Vanilla RavagerEntityModel.animateModel() reads these as countdown values
        // (attackTick counts 10->0, roarTick counts 20->0) so we compute the
        // remaining ticks based on elapsed time since the animation started.
        if (shell instanceof RavagerEntity ravager) {
            int biteRemaining = net.sam.samrequiemmod.possession.illager.RavagerClientState
                    .getBiteTicksRemaining(player.getUuid(), player.age);
            int roarRemaining = net.sam.samrequiemmod.possession.illager.RavagerClientState
                    .getRoarTicksRemaining(player.getUuid(), player.age);
            // attackTick drives head-lunge + jaw bite in animateModel()
            ravager.attackTick = biteRemaining;
            // roarTick drives jaw-open roar in the else branch of animateModel()
            // (only plays when attackTick == 0, which is handled naturally by timing)
            ravager.roarTick = roarRemaining;
            // Always clear stun so it doesn't interfere
            ravager.stunTick = 0;
        }

        // Iron Golem: drive attack animation and set health for crack rendering.
        // IronGolemEntityRenderer uses getCrackLevel() which checks getHealth()/getMaxHealth().
        // Both the shell and the possessed player have 100 HP max, so the ratio maps correctly.
        // attackTicksLeft drives the arm swing animation in IronGolemEntityModel.
        if (shell instanceof IronGolemEntity ironGolem) {
            int attackRemaining = net.sam.samrequiemmod.possession.iron_golem.IronGolemClientState
                    .getAttackTicksRemaining(player.getUuid(), player.age);
            ironGolem.attackTicksLeft = attackRemaining;
        }

        // Witch: drive drinking animation from client state.
        // The WitchEntityRenderer checks getMainHandStack().isEmpty() to set the
        // nose-lifting pose, and WitchHeldItemFeatureRenderer renders the held item.
        // Vanilla puts the potion in MAINHAND during drinking (see WitchEntity.tickMovement).
        if (shell instanceof WitchEntity witch) {
            boolean isDrinking = net.sam.samrequiemmod.possession.illager.WitchClientState.isDrinking(player.getUuid());
            witch.setDrinking(isDrinking);
            if (isDrinking) {
                shell.equipStack(EquipmentSlot.MAINHAND,
                        new net.minecraft.item.ItemStack(net.minecraft.item.Items.POTION));
            }
        }

        // Enderman: drive angry state from client state for open-mouth texture
        if (shell instanceof EndermanEntity enderman) {
            boolean angry = net.sam.samrequiemmod.possession.enderman.EndermanClientState.isAngry(player.getUuid());
            net.minecraft.entity.data.TrackedData<Boolean> angryKey =
                    net.sam.samrequiemmod.mixin.client.EndermanEntityAngryAccessor.getAngryKey();
            enderman.getDataTracker().set(angryKey, angry);
            spawnEndermanParticles(player);
        }

        if (shell instanceof WardenEntity warden) {
            int attackStart = net.sam.samrequiemmod.possession.warden.WardenClientState.getAttackStart(player.getUuid());
            boolean attacking = player.handSwinging
                    || player.handSwingProgress > 0.01f
                    || net.sam.samrequiemmod.possession.warden.WardenClientState.isAttacking(player.getUuid(), player.age);
            boolean sonic = net.sam.samrequiemmod.possession.warden.WardenClientState.isChargingSonic(player.getUuid(), player.age);
            boolean roaring = net.sam.samrequiemmod.possession.warden.WardenClientState.isRoaring(player.getUuid(), player.age);
            boolean sniffing = net.sam.samrequiemmod.possession.warden.WardenClientState.isSniffing(player.getUuid(), player.age)
                    || (player.age % 300) < net.sam.samrequiemmod.possession.warden.WardenClientState.SNIFF_DURATION;

            if (attacking) {
                Integer lastAttackStart = LAST_WARDEN_ATTACK_START.get(player.getUuid());
                if (attackStart != Integer.MIN_VALUE && !java.util.Objects.equals(lastAttackStart, attackStart)) {
                    warden.attackingAnimationState.start(attackStart);
                    LAST_WARDEN_ATTACK_START.put(player.getUuid(), attackStart);
                } else {
                    warden.attackingAnimationState.startIfNotRunning(player.age);
                }
            } else {
                warden.attackingAnimationState.stop();
                LAST_WARDEN_ATTACK_START.remove(player.getUuid());
            }
            if (sonic) {
                if (!warden.chargingSonicBoomAnimationState.isRunning()) {
                    // Vanilla starts the sonic charge from entity status 62.
                    warden.handleStatus((byte) 62);
                }
            } else {
                warden.chargingSonicBoomAnimationState.stop();
            }
            if (roaring) {
                // Vanilla roar uses both the animation state and the ROARING pose.
                warden.setPose(EntityPose.ROARING);
                warden.roaringAnimationState.startIfNotRunning(player.age);
            } else {
                if (warden.isInPose(EntityPose.ROARING)) {
                    warden.setPose(EntityPose.STANDING);
                }
                warden.roaringAnimationState.stop();
            }
            if (sniffing) {
                warden.sniffingAnimationState.startIfNotRunning(player.age);
            } else {
                warden.sniffingAnimationState.stop();
            }
        }

        if (shell instanceof BreezeEntity breeze) {
            var accessor = (net.sam.samrequiemmod.mixin.client.BreezeEntityAnimationAccessor) breeze;
            boolean inhaling = net.sam.samrequiemmod.possession.breeze.BreezeClientState.isInhaling(player.getUuid(), player.age);
            boolean shooting = net.sam.samrequiemmod.possession.breeze.BreezeClientState.isShooting(player.getUuid(), player.age);
            boolean jumping = net.sam.samrequiemmod.possession.breeze.BreezeClientState.isJumping(player.getUuid(), player.age);
            boolean sliding = jumping || player.getVelocity().horizontalLengthSquared() > 0.01;

            if (inhaling) {
                accessor.samrequiemmod$getInhalingAnimationState().startIfNotRunning(player.age);
            } else {
                accessor.samrequiemmod$getInhalingAnimationState().stop();
            }
            if (shooting) {
                accessor.samrequiemmod$getShootingAnimationState().startIfNotRunning(player.age);
            } else {
                accessor.samrequiemmod$getShootingAnimationState().stop();
            }
            if (sliding) {
                accessor.samrequiemmod$getSlidingAnimationState().startIfNotRunning(player.age);
            } else {
                accessor.samrequiemmod$getSlidingAnimationState().stop();
            }
        }

        // Creeper: drive fuse animation and charged glow from client state
        if (shell instanceof CreeperEntity creeper) {
            // Set fuse time based on charging state
            int fuseTicks = net.sam.samrequiemmod.possession.creeper.CreeperClientState.getFuseTicks(player.getUuid());
            boolean isCharging = net.sam.samrequiemmod.possession.creeper.CreeperClientState.isCharging(player.getUuid());
            
            if (isCharging && fuseTicks > 0) {
                // When charging, set fuse and speed to 1 (expanding)
                int fuseValue = 30 - fuseTicks;
                CREEPER_CURRENT_FUSE_TIME.set(creeper, fuseValue);
                CREEPER_LAST_FUSE_TIME.set(creeper, Math.max(0, fuseValue - 1));
                creeper.getDataTracker().set(
                        net.sam.samrequiemmod.mixin.client.CreeperEntityFuseAccessor.getFuseSpeedKey(), 1);
            } else {
                // Not charging - reset fuse
                CREEPER_CURRENT_FUSE_TIME.set(creeper, 0);
                CREEPER_LAST_FUSE_TIME.set(creeper, 0);
                creeper.getDataTracker().set(
                        net.sam.samrequiemmod.mixin.client.CreeperEntityFuseAccessor.getFuseSpeedKey(), 0);
            }
            
            // Set charged state from client state
            boolean isCharged = net.sam.samrequiemmod.possession.creeper.CreeperClientState.isCharged(player.getUuid());
            creeper.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.CreeperEntityFuseAccessor.getChargedKey(), isCharged);
        }

        // Passive mobs: set baby state from client state
        if (shell instanceof net.minecraft.entity.passive.PassiveEntity passiveShell) {
            boolean isBabyPassive = net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid());
            passiveShell.setBaby(isBabyPassive);
        }

        if (shell instanceof CowEntity cow) {
            cow.setVariant(getCowVariantForBiome(player));
        }

        if (shell instanceof PigEntity pig) {
            ((net.sam.samrequiemmod.mixin.client.PigEntityVariantInvoker) pig)
                    .samrequiemmod$setVariant(getPigVariantForBiome(player));
        }

        if (shell instanceof SheepEntity sheep) {
            sheep.setColor(SheepAppearanceState.getClientColor(player.getUuid()));
            sheep.setSheared(SheepAppearanceState.isClientSheared(player.getUuid()));
        }

        if (shell instanceof PandaEntity panda) {
            panda.setMainGene(PandaAppearanceState.getClientMainGene(player.getUuid()));
            panda.setHiddenGene(PandaAppearanceState.getClientHiddenGene(player.getUuid()));
        }

        if (shell instanceof HoglinEntity hoglin) {
            hoglin.setBaby(net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isClientBaby(player.getUuid()));
            hoglin.movementCooldownTicks =
                    net.sam.samrequiemmod.possession.hoglin.HoglinAttackClientState.getRemainingTicks(player.getUuid());
        }

        if (shell instanceof ZoglinEntity zoglin) {
            zoglin.setBaby(net.sam.samrequiemmod.possession.hoglin.BabyHoglinState.isClientBaby(player.getUuid()));
            zoglin.movementCooldownTicks =
                    net.sam.samrequiemmod.possession.hoglin.HoglinAttackClientState.getRemainingTicks(player.getUuid());
        }

        if (shell instanceof GuardianEntity guardian) {
            UUID targetUuid = net.sam.samrequiemmod.possession.guardian.GuardianClientState.getTargetUuid(player.getUuid());
            if (targetUuid != null) {
                Entity target = player.getEntityWorld().getPlayerByUuid(targetUuid);
                if (target == null) {
                    for (LivingEntity entity : player.getEntityWorld().getEntitiesByClass(
                            LivingEntity.class,
                            player.getBoundingBox().expand(64.0),
                            e -> e.getUuid().equals(targetUuid))) {
                        target = entity;
                        break;
                    }
                }
                if (target instanceof LivingEntity livingTarget) {
                    guardian.setBeamTarget(livingTarget.getId());
                    guardian.beamTicks = net.sam.samrequiemmod.possession.guardian.GuardianClientState.getElapsedTicks(player.getUuid());
                } else {
                    guardian.setBeamTarget(0);
                    guardian.beamTicks = 0;
                }
            } else {
                guardian.setBeamTarget(0);
                guardian.beamTicks = 0;
            }
        }

        if (shell instanceof BlazeEntity blaze) {
            blaze.setFireActive(net.sam.samrequiemmod.possession.firemob.FireMobAttackClientState.isBlazeActive(player.getUuid()));
        }

        if (shell instanceof GhastEntity ghast) {
            ghast.setShooting(net.sam.samrequiemmod.possession.firemob.FireMobAttackClientState.isGhastShooting(player.getUuid()));
        }

        if (shell instanceof SlimeEntity slime) {
            slime.setSize(net.sam.samrequiemmod.possession.slime.SlimeSizeState.getClientSize(player.getUuid()), false);
            float horizontalSpeed = (float) player.getVelocity().horizontalLength();
            boolean hopping = !player.isOnGround() || Math.abs(player.getVelocity().y) > 0.08f;
            float wave = hopping
                    ? 0.85f + 0.55f * MathHelper.sin((player.age + tickDelta) * 0.95f)
                    : Math.min(horizontalSpeed * 0.3f, 0.10f);
            slime.lastStretch = slime.stretch;
            slime.stretch = wave;
            slime.limbAnimator.reset();
        }

        if (shell instanceof WolfEntity wolf) {
            wolf.setBaby(net.sam.samrequiemmod.possession.wolf.WolfBabyState.isClientBaby(player.getUuid()));
            String variantId = net.sam.samrequiemmod.possession.wolf.WolfState.getClientVariant(player.getUuid());
            player.getEntityWorld().getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.WOLF_VARIANT)
                    .getEntry(net.minecraft.util.Identifier.of(variantId))
                    .ifPresent(variant -> ((net.sam.samrequiemmod.mixin.client.WolfEntityVariantInvoker) wolf)
                            .samrequiemmod$setVariant(variant));

            boolean angry = net.sam.samrequiemmod.possession.wolf.WolfState.isClientAngry(player.getUuid());
            if (angry) {
                wolf.chooseRandomAngerTime();
                wolf.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
            } else {
                wolf.setAngerEndTime(0L);
                wolf.setAngryAt(null);
            }

            var shakeAccessor = (net.sam.samrequiemmod.mixin.client.WolfEntityShakeAccessor) wolf;
            boolean shaking = net.sam.samrequiemmod.possession.wolf.WolfState.isClientShaking(player.getUuid(), player.age);
            if (shaking) {
                float progress = Math.min(1.0f, ((player.age % 28) + tickDelta) / 28.0f);
                shakeAccessor.setFurWet(true);
                shakeAccessor.setCanShakeWaterOff(true);
                shakeAccessor.setLastShakeProgress(shakeAccessor.getShakeProgress());
                shakeAccessor.setShakeProgress(progress);
            } else {
                shakeAccessor.setFurWet(false);
                shakeAccessor.setCanShakeWaterOff(false);
                shakeAccessor.setLastShakeProgress(0.0f);
                shakeAccessor.setShakeProgress(0.0f);
            }
        }

        if (shell instanceof CatEntity cat) {
            String variantId = net.sam.samrequiemmod.possession.feline.CatState.getClientVariant(player.getUuid());
            player.getEntityWorld().getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.CAT_VARIANT)
                    .getEntry(net.minecraft.util.Identifier.of(variantId))
                    .ifPresent(variant -> ((net.sam.samrequiemmod.mixin.client.CatEntityVariantInvoker) cat)
                            .samrequiemmod$setVariant(variant));
        }

        if (shell instanceof VexEntity vex) {
            vex.setCharging(net.sam.samrequiemmod.possession.vex.VexState.isClientAngry(player.getUuid()));
        }

        if (shell instanceof BatEntity bat) {
            bat.setRoosting(false);
            bat.roostingAnimationState.stop();
            bat.flyingAnimationState.startIfNotRunning(player.age);
        }

        if (shell instanceof BeeEntity bee) {
            boolean angry = net.sam.samrequiemmod.possession.beast.BeastState.isClientBeeAngry(player.getUuid());
            if (angry) {
                bee.chooseRandomAngerTime();
                bee.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
            } else {
                bee.setAngerEndTime(0L);
                bee.setAngryAt(null);
            }
            bee.setOnGround(false);
        }

        if (shell instanceof ParrotEntity parrot) {
            boolean flying = net.sam.samrequiemmod.possession.beast.BeastState.isClientParrotFlying(player.getUuid());
            float flap = flying
                    ? 0.9f + 0.35f * MathHelper.sin((player.age + tickDelta) * 1.35f)
                    : 0.0f;
            parrot.lastFlapProgress = parrot.flapProgress;
            parrot.lastMaxWingDeviation = parrot.maxWingDeviation;
            parrot.flapProgress += flying ? 0.8f : 0.0f;
            parrot.maxWingDeviation = flap;
            parrot.setOnGround(!flying);
        }

        if (shell instanceof VillagerEntity villager) {
            villager.setBaby(net.sam.samrequiemmod.possession.villager.VillagerState.isClientBaby(player.getUuid()));
            var typeLookup = player.getEntityWorld().getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.VILLAGER_TYPE);
            var professionLookup = player.getEntityWorld().getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.VILLAGER_PROFESSION);
            var villagerType = typeLookup.getOrThrow(VillagerType.forBiome(player.getEntityWorld().getBiome(player.getBlockPos())));
            var villagerProfession = professionLookup.getOrThrow(net.minecraft.village.VillagerProfession.NONE);
            villager.setVillagerData(new VillagerData(villagerType, villagerProfession, 1));
        }

        if (shell instanceof HorseEntity horse) {
            horse.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            horse.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.HorseEntityVariantAccessor.getVariantKey(),
                    net.sam.samrequiemmod.possession.beast.BeastState.getClientHorseVariant(player.getUuid()));
        }

        if (shell instanceof MuleEntity mule) {
            mule.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof ZombieHorseEntity zombieHorse) {
            zombieHorse.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof SkeletonHorseEntity skeletonHorse) {
            skeletonHorse.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof GoatEntity goat) {
            goat.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof PolarBearEntity polarBear) {
            polarBear.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            polarBear.setWarning(net.sam.samrequiemmod.possession.beast.BeastAttackClientState.isPolarAttacking(player.getUuid(), player.age));
        }

        if (shell instanceof RabbitEntity rabbit) {
            rabbit.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            rabbit.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.RabbitEntityVariantAccessor.getVariantKey(),
                    net.sam.samrequiemmod.possession.beast.BeastState.getClientRabbitVariant(player.getUuid()));
        }

        if (shell instanceof TurtleEntity turtle) {
            turtle.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof ShulkerEntity shulker) {
            byte peek = (byte) (net.sam.samrequiemmod.possession.beast.BeastState.isClientShulkerOpen(player.getUuid(), player.age) ? 100 : 0);
            shulker.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.ShulkerEntityPeekAccessor.getPeekAmountKey(),
                    peek);
            shulker.tick();
        }

        if (shell instanceof StriderEntity strider) {
            strider.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            strider.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.StriderEntityColdAccessor.getColdKey(),
                    !player.isInLava());
        }

        if (shell instanceof AxolotlEntity axolotl) {
            axolotl.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            axolotl.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.AxolotlEntityVariantAccessor.getVariantKey(),
                    net.sam.samrequiemmod.possession.beast.BeastState.getClientAxolotlVariant(player.getUuid()));
            axolotl.getDataTracker().set(
                    net.sam.samrequiemmod.mixin.client.AxolotlEntityVariantAccessor.getPlayingDeadKey(),
                    net.sam.samrequiemmod.possession.beast.BeastState.isClientAxolotlPlayingDead(player.getUuid(), player.age));
        }

        if (shell instanceof CamelEntity camel) {
            camel.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
        }

        if (shell instanceof ArmadilloEntity armadillo) {
            armadillo.setBaby(net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isClientBaby(player.getUuid()));
            armadillo.setState(net.sam.samrequiemmod.possession.beast.BeastState.isClientArmadilloCurled(player.getUuid())
                    ? ArmadilloEntity.State.SCARED
                    : ArmadilloEntity.State.IDLE);
        }

        // Chicken: drive wing flapping animation based on player's airborne state.
        // Vanilla ChickenEntity.tickMovement() calculates flapProgress/maxWingDeviation
        // from movement, but our cached shell doesn't tick — so we set the fields manually.
        if (shell instanceof ChickenEntity chicken) {
            chicken.lastFlapProgress = chicken.flapProgress;
            chicken.lastMaxWingDeviation = chicken.maxWingDeviation;
            if (!player.isOnGround() && player.getVelocity().y < 0.0) {
                // In the air and falling — flap wings
                chicken.maxWingDeviation += 4.0f * 0.3f;
            } else {
                // On the ground — fold wings
                chicken.maxWingDeviation -= 1.0f * 0.3f;
            }
            chicken.maxWingDeviation = net.minecraft.util.math.MathHelper.clamp(
                    chicken.maxWingDeviation, 0.0f, 1.0f);
            // Drive wing rotation speed
            float flapSpeed = (!player.isOnGround()) ? 1.0f : 0.0f;
            chicken.flapProgress += flapSpeed * 2.0f;
        }

        // Skeleton/Bogged/Stray: drive bow charging animation.
        // AbstractSkeletonEntity.setAttacking(true) raises both arms in the bow-aiming pose.
        if (shell instanceof net.minecraft.entity.mob.AbstractSkeletonEntity skeleton) {
            boolean usingBow = player.isUsingItem()
                    && player.getActiveItem().isOf(net.minecraft.item.Items.BOW);
            if (shell instanceof WitherSkeletonEntity) {
                // Wither skeleton: arms out from melee attacking OR bow use
                boolean meleeArms = net.sam.samrequiemmod.possession.skeleton.WitherSkeletonAttackClientState
                        .isAttacking(player.getUuid());
                skeleton.setAttacking(meleeArms || usingBow);
            } else {
                skeleton.setAttacking(usingBow);
            }
        }

        // Pufferfish: set puff state from client state
        if (shell instanceof PufferfishEntity pufferfish) {
            boolean puffed = net.sam.samrequiemmod.possession.aquatic.PufferfishState.isPuffed(player.getUuid());
            net.minecraft.entity.data.TrackedData<Integer> puffStateKey =
                    net.sam.samrequiemmod.mixin.client.PufferfishEntityPuffStateAccessor.getPuffStateKey();
            pufferfish.getDataTracker().set(puffStateKey, puffed ? 2 : 0);
        }

        // Tropical fish: set variant from client state for correct color/pattern
        if (shell instanceof TropicalFishEntity tropicalFish) {
            int variant = net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantState.getClientVariant(player.getUuid());
            net.minecraft.entity.data.TrackedData<Integer> variantKey =
                    net.sam.samrequiemmod.mixin.client.TropicalFishEntityVariantAccessor.getVariantKey();
            tropicalFish.getDataTracker().set(variantKey, variant);
        }

        // Squid: manually drive tentacle animation fields.
        // Vanilla SquidEntity.tickMovement() updates these from thrust cycles;
        // since our shell never ticks, we simulate the animation here.
        if (shell instanceof SquidEntity squid) {
            float ageInTicks = (float) player.age + tickDelta;
            float thrustSpeed = 0.2f;
            float thrustPhase = (ageInTicks * thrustSpeed) % ((float) Math.PI * 2);

            squid.lastTentacleAngle = squid.tentacleAngle;
            squid.lastTiltAngle = squid.tiltAngle;
            squid.lastRollAngle = squid.rollAngle;

            if (player.isTouchingWater()) {
                // Swimming: pulsing tentacle animation
                if (thrustPhase < (float) Math.PI) {
                    float f = thrustPhase / (float) Math.PI;
                    squid.tentacleAngle = MathHelper.sin(f * f * (float) Math.PI) * (float) Math.PI * 0.25f;
                } else {
                    squid.tentacleAngle = 0.0f;
                }

                // Tilt based on vertical movement
                net.minecraft.util.math.Vec3d vel = player.getVelocity();
                double hLen = vel.horizontalLength();
                squid.tiltAngle += (-((float) MathHelper.atan2(hLen, vel.y)) * (180.0f / (float) Math.PI) - squid.tiltAngle) * 0.1f;

                // Rolling animation
                squid.rollAngle += (float) Math.PI * 0.5f * 0.02f;
            } else {
                // On land: slow pulsing
                squid.tentacleAngle = MathHelper.abs(MathHelper.sin(thrustPhase)) * (float) Math.PI * 0.25f;
                squid.tiltAngle += (-90.0f - squid.tiltAngle) * 0.02f;
            }
        }

        // Dolphin: ensure proper body pitch for swimming animation.
        // DolphinEntityModel.setAngles() reads headPitch which maps to shell.getPitch().
        // Also driven by velocity (checked above via setVelocity).
        // No extra fields needed — velocity + pitch is sufficient.

        // Set riding pose: directly set the vehicle field so hasVehicle() returns true
        // and the biped model renders with sitting legs. No startRiding() to avoid
        // double-offset positioning.
        if (player.hasVehicle() && (player.getVehicle() instanceof net.minecraft.entity.passive.ChickenEntity
                || player.getVehicle() instanceof net.minecraft.entity.mob.RavagerEntity
                || player.getVehicle() instanceof SpiderEntity
                || player.getVehicle() instanceof ZombieHorseEntity
                || player.getVehicle() instanceof SkeletonHorseEntity
                || player.getVehicle() instanceof ZombieNautilusEntity)) {
            shell.vehicle = player.getVehicle();
        } else {
            shell.vehicle = null;
        }

        if (shell instanceof Entity entity) {
            entity.setCustomName(player.getDisplayName());
            entity.setCustomNameVisible(false);
        }
    }
}

