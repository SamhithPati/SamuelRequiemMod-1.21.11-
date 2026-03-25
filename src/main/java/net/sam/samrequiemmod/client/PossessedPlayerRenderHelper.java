package net.sam.samrequiemmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
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
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.StrayEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.WitchEntity;
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
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieHorseEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.AxolotlEntity;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import net.sam.samrequiemmod.client.CrossbowAnimationOverride;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackClientState;
import net.sam.samrequiemmod.possession.passive.MooshroomClientState;

import java.util.UUID;

public final class PossessedPlayerRenderHelper {

    // MethodHandle-based accessors for LimbAnimator.pos and LimbAnimator.speed.
    // Using privateLookupIn() to bypass module access restrictions on private fields.
    private static final java.lang.invoke.VarHandle LIMB_POS;
    private static final java.lang.invoke.VarHandle LIMB_SPEED;
    private static final java.lang.invoke.VarHandle CREEPER_CURRENT_FUSE_TIME;
    private static final java.lang.invoke.VarHandle CREEPER_LAST_FUSE_TIME;
    private static final java.lang.invoke.VarHandle SLIME_STRETCH;
    private static final java.lang.invoke.VarHandle SLIME_LAST_STRETCH;
    private static final java.lang.invoke.VarHandle PARROT_FLAP_PROGRESS;
    private static final java.lang.invoke.VarHandle PARROT_PREV_FLAP_PROGRESS;
    private static final java.lang.invoke.VarHandle PARROT_MAX_WING_DEVIATION;
    private static final java.lang.invoke.VarHandle PARROT_PREV_MAX_WING_DEVIATION;
    static {
        try {
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.LimbAnimator.class,
                            java.lang.invoke.MethodHandles.lookup());
            LIMB_POS   = lookup.findVarHandle(net.minecraft.entity.LimbAnimator.class, "pos",   float.class);
            LIMB_SPEED = lookup.findVarHandle(net.minecraft.entity.LimbAnimator.class, "speed", float.class);
            
            // Creeper fuse time accessors
            java.lang.invoke.MethodHandles.Lookup creeperLookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.mob.CreeperEntity.class,
                            java.lang.invoke.MethodHandles.lookup());
            CREEPER_CURRENT_FUSE_TIME = creeperLookup.findVarHandle(net.minecraft.entity.mob.CreeperEntity.class, "currentFuseTime", int.class);
            CREEPER_LAST_FUSE_TIME = creeperLookup.findVarHandle(net.minecraft.entity.mob.CreeperEntity.class, "lastFuseTime", int.class);

            java.lang.invoke.MethodHandles.Lookup slimeLookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.mob.SlimeEntity.class,
                            java.lang.invoke.MethodHandles.lookup());
            SLIME_STRETCH = slimeLookup.findVarHandle(net.minecraft.entity.mob.SlimeEntity.class, "stretch", float.class);
            SLIME_LAST_STRETCH = slimeLookup.findVarHandle(net.minecraft.entity.mob.SlimeEntity.class, "lastStretch", float.class);

            java.lang.invoke.MethodHandles.Lookup parrotLookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.passive.ParrotEntity.class,
                            java.lang.invoke.MethodHandles.lookup());
            PARROT_FLAP_PROGRESS = parrotLookup.findVarHandle(net.minecraft.entity.passive.ParrotEntity.class, "flapProgress", float.class);
            PARROT_PREV_FLAP_PROGRESS = parrotLookup.findVarHandle(net.minecraft.entity.passive.ParrotEntity.class, "prevFlapProgress", float.class);
            PARROT_MAX_WING_DEVIATION = parrotLookup.findVarHandle(net.minecraft.entity.passive.ParrotEntity.class, "maxWingDeviation", float.class);
            PARROT_PREV_MAX_WING_DEVIATION = parrotLookup.findVarHandle(net.minecraft.entity.passive.ParrotEntity.class, "prevMaxWingDeviation", float.class);
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
    private static CowEntity cachedCow;
    private static MooshroomEntity cachedMooshroom;
    private static PigEntity cachedPig;
    private static SheepEntity cachedSheep;
    private static ChickenEntity cachedChicken;
    private static BoggedEntity cachedBogged;
    private static StrayEntity cachedStray;
    private static WitherSkeletonEntity cachedWitherSkeleton;
    private static EndermanEntity cachedEnderman;
    private static CreeperEntity cachedCreeper;
    private static CodEntity cachedCod;
    private static SalmonEntity cachedSalmon;
    private static PufferfishEntity cachedPufferfish;
    private static TropicalFishEntity cachedTropicalFish;
    private static SquidEntity cachedSquid;
    private static DolphinEntity cachedDolphin;
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

    private PossessedPlayerRenderHelper() {
    }

    public static boolean shouldRenderAsPossessed(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) != null;
    }

    public static void renderPossessed(
            AbstractClientPlayerEntity player,
            float entityYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        EntityType<?> type = ClientPossessionState.get(player);
        if (type == null) return;

        LivingEntity shell = getOrCreateShell(type, player.getWorld());
        if (shell == null) return;

        copyPlayerStateToShell(player, shell, tickDelta);

        // Apply mooshroom color variant before rendering
        if (shell instanceof MooshroomEntity mooshroom) {
            boolean isBrown = MooshroomClientState.isBrownMooshroom(player.getUuid());
            mooshroom.setVariant(isBrown ? net.minecraft.entity.passive.MooshroomEntity.Type.BROWN : net.minecraft.entity.passive.MooshroomEntity.Type.RED);
        }

        MinecraftClient.getInstance().getEntityRenderDispatcher().render(
                shell, 0.0, 0.0, 0.0, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static LivingEntity getOrCreateShell(EntityType<?> type, World world) {
        if (type == EntityType.ZOMBIE) {
            if (cachedZombie == null || cachedZombie.getWorld() != world)
                cachedZombie = new ZombieEntity(EntityType.ZOMBIE, world);
            return cachedZombie;
        }
        if (type == EntityType.HUSK) {
            if (cachedHusk == null || cachedHusk.getWorld() != world)
                cachedHusk = new HuskEntity(EntityType.HUSK, world);
            return cachedHusk;
        }
        if (type == EntityType.DROWNED) {
            if (cachedDrowned == null || cachedDrowned.getWorld() != world)
                cachedDrowned = new DrownedEntity(EntityType.DROWNED, world);
            return cachedDrowned;
        }
        if (type == EntityType.SKELETON) {
            if (cachedSkeleton == null || cachedSkeleton.getWorld() != world)
                cachedSkeleton = new SkeletonEntity(EntityType.SKELETON, world);
            return cachedSkeleton;
        }
        if (type == EntityType.BOGGED) {
            if (cachedBogged == null || cachedBogged.getWorld() != world)
                cachedBogged = new BoggedEntity(EntityType.BOGGED, world);
            return cachedBogged;
        }
        if (type == EntityType.STRAY) {
            if (cachedStray == null || cachedStray.getWorld() != world)
                cachedStray = new StrayEntity(EntityType.STRAY, world);
            return cachedStray;
        }
        if (type == EntityType.WITHER_SKELETON) {
            if (cachedWitherSkeleton == null || cachedWitherSkeleton.getWorld() != world)
                cachedWitherSkeleton = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, world);
            return cachedWitherSkeleton;
        }
        if (type == EntityType.PILLAGER) {
            if (cachedPillager == null || cachedPillager.getWorld() != world)
                cachedPillager = new PillagerEntity(EntityType.PILLAGER, world);
            return cachedPillager;
        }
        if (type == EntityType.VINDICATOR) {
            if (cachedVindicator == null || cachedVindicator.getWorld() != world)
                cachedVindicator = new VindicatorEntity(EntityType.VINDICATOR, world);
            return cachedVindicator;
        }
        if (type == EntityType.EVOKER) {
            if (cachedEvoker == null || cachedEvoker.getWorld() != world)
                cachedEvoker = new EvokerEntity(EntityType.EVOKER, world);
            return cachedEvoker;
        }
        if (type == EntityType.PIGLIN) {
            if (cachedPiglin == null || cachedPiglin.getWorld() != world)
                cachedPiglin = new PiglinEntity(EntityType.PIGLIN, world);
            return cachedPiglin;
        }
        if (type == EntityType.PIGLIN_BRUTE) {
            if (cachedPiglinBrute == null || cachedPiglinBrute.getWorld() != world)
                cachedPiglinBrute = new PiglinBruteEntity(EntityType.PIGLIN_BRUTE, world);
            return cachedPiglinBrute;
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN) {
            if (cachedZombifiedPiglin == null || cachedZombifiedPiglin.getWorld() != world)
                cachedZombifiedPiglin = new ZombifiedPiglinEntity(EntityType.ZOMBIFIED_PIGLIN, world);
            return cachedZombifiedPiglin;
        }
        if (type == EntityType.SPIDER) {
            if (cachedSpider == null || cachedSpider.getWorld() != world)
                cachedSpider = new SpiderEntity(EntityType.SPIDER, world);
            return cachedSpider;
        }
        if (type == EntityType.CAVE_SPIDER) {
            if (cachedCaveSpider == null || cachedCaveSpider.getWorld() != world)
                cachedCaveSpider = new CaveSpiderEntity(EntityType.CAVE_SPIDER, world);
            return cachedCaveSpider;
        }
        if (type == EntityType.HOGLIN) {
            if (cachedHoglin == null || cachedHoglin.getWorld() != world)
                cachedHoglin = new HoglinEntity(EntityType.HOGLIN, world);
            return cachedHoglin;
        }
        if (type == EntityType.ZOGLIN) {
            if (cachedZoglin == null || cachedZoglin.getWorld() != world)
                cachedZoglin = new ZoglinEntity(EntityType.ZOGLIN, world);
            return cachedZoglin;
        }
        if (type == EntityType.GUARDIAN) {
            if (cachedGuardian == null || cachedGuardian.getWorld() != world)
                cachedGuardian = new GuardianEntity(EntityType.GUARDIAN, world);
            return cachedGuardian;
        }
        if (type == EntityType.ELDER_GUARDIAN) {
            if (cachedElderGuardian == null || cachedElderGuardian.getWorld() != world)
                cachedElderGuardian = new ElderGuardianEntity(EntityType.ELDER_GUARDIAN, world);
            return cachedElderGuardian;
        }
        if (type == EntityType.SILVERFISH) {
            if (cachedSilverfish == null || cachedSilverfish.getWorld() != world)
                cachedSilverfish = new SilverfishEntity(EntityType.SILVERFISH, world);
            return cachedSilverfish;
        }
        if (type == EntityType.BLAZE) {
            if (cachedBlaze == null || cachedBlaze.getWorld() != world)
                cachedBlaze = new BlazeEntity(EntityType.BLAZE, world);
            return cachedBlaze;
        }
        if (type == EntityType.GHAST) {
            if (cachedGhast == null || cachedGhast.getWorld() != world)
                cachedGhast = new GhastEntity(EntityType.GHAST, world);
            return cachedGhast;
        }
        if (type == EntityType.SLIME) {
            if (cachedSlime == null || cachedSlime.getWorld() != world)
                cachedSlime = new SlimeEntity(EntityType.SLIME, world);
            return cachedSlime;
        }
        if (type == EntityType.MAGMA_CUBE) {
            if (cachedMagmaCube == null || cachedMagmaCube.getWorld() != world)
                cachedMagmaCube = new MagmaCubeEntity(EntityType.MAGMA_CUBE, world);
            return cachedMagmaCube;
        }
        if (type == EntityType.WOLF) {
            if (cachedWolf == null || cachedWolf.getWorld() != world)
                cachedWolf = new WolfEntity(EntityType.WOLF, world);
            return cachedWolf;
        }
        if (type == EntityType.FOX) {
            if (cachedFox == null || cachedFox.getWorld() != world)
                cachedFox = new FoxEntity(EntityType.FOX, world);
            return cachedFox;
        }
        if (type == EntityType.OCELOT) {
            if (cachedOcelot == null || cachedOcelot.getWorld() != world)
                cachedOcelot = new OcelotEntity(EntityType.OCELOT, world);
            return cachedOcelot;
        }
        if (type == EntityType.CAT) {
            if (cachedCat == null || cachedCat.getWorld() != world)
                cachedCat = new CatEntity(EntityType.CAT, world);
            return cachedCat;
        }
        if (type == EntityType.FROG) {
            if (cachedFrog == null || cachedFrog.getWorld() != world)
                cachedFrog = new FrogEntity(EntityType.FROG, world);
            return cachedFrog;
        }
        if (type == EntityType.VEX) {
            if (cachedVex == null || cachedVex.getWorld() != world)
                cachedVex = new VexEntity(EntityType.VEX, world);
            return cachedVex;
        }
        if (type == EntityType.BAT) {
            if (cachedBat == null || cachedBat.getWorld() != world)
                cachedBat = new BatEntity(EntityType.BAT, world);
            return cachedBat;
        }
        if (type == EntityType.BEE) {
            if (cachedBee == null || cachedBee.getWorld() != world)
                cachedBee = new BeeEntity(EntityType.BEE, world);
            return cachedBee;
        }
        if (type == EntityType.PARROT) {
            if (cachedParrot == null || cachedParrot.getWorld() != world)
                cachedParrot = new ParrotEntity(EntityType.PARROT, world);
            return cachedParrot;
        }
        if (type == EntityType.VILLAGER) {
            if (cachedVillager == null || cachedVillager.getWorld() != world)
                cachedVillager = new VillagerEntity(EntityType.VILLAGER, world);
            return cachedVillager;
        }
        if (type == EntityType.HORSE) {
            if (cachedHorse == null || cachedHorse.getWorld() != world)
                cachedHorse = new HorseEntity(EntityType.HORSE, world);
            return cachedHorse;
        }
        if (type == EntityType.MULE) {
            if (cachedMule == null || cachedMule.getWorld() != world)
                cachedMule = new MuleEntity(EntityType.MULE, world);
            return cachedMule;
        }
        if (type == EntityType.ZOMBIE_HORSE) {
            if (cachedZombieHorse == null || cachedZombieHorse.getWorld() != world)
                cachedZombieHorse = new ZombieHorseEntity(EntityType.ZOMBIE_HORSE, world);
            return cachedZombieHorse;
        }
        if (type == EntityType.SKELETON_HORSE) {
            if (cachedSkeletonHorse == null || cachedSkeletonHorse.getWorld() != world)
                cachedSkeletonHorse = new SkeletonHorseEntity(EntityType.SKELETON_HORSE, world);
            return cachedSkeletonHorse;
        }
        if (type == EntityType.ENDERMITE) {
            if (cachedEndermite == null || cachedEndermite.getWorld() != world)
                cachedEndermite = new EndermiteEntity(EntityType.ENDERMITE, world);
            return cachedEndermite;
        }
        if (type == EntityType.GOAT) {
            if (cachedGoat == null || cachedGoat.getWorld() != world)
                cachedGoat = new GoatEntity(EntityType.GOAT, world);
            return cachedGoat;
        }
        if (type == EntityType.POLAR_BEAR) {
            if (cachedPolarBear == null || cachedPolarBear.getWorld() != world)
                cachedPolarBear = new PolarBearEntity(EntityType.POLAR_BEAR, world);
            return cachedPolarBear;
        }
        if (type == EntityType.RABBIT) {
            if (cachedRabbit == null || cachedRabbit.getWorld() != world)
                cachedRabbit = new RabbitEntity(EntityType.RABBIT, world);
            return cachedRabbit;
        }
        if (type == EntityType.TURTLE) {
            if (cachedTurtle == null || cachedTurtle.getWorld() != world)
                cachedTurtle = new TurtleEntity(EntityType.TURTLE, world);
            return cachedTurtle;
        }
        if (type == EntityType.SHULKER) {
            if (cachedShulker == null || cachedShulker.getWorld() != world)
                cachedShulker = new ShulkerEntity(EntityType.SHULKER, world);
            return cachedShulker;
        }
        if (type == EntityType.STRIDER) {
            if (cachedStrider == null || cachedStrider.getWorld() != world)
                cachedStrider = new StriderEntity(EntityType.STRIDER, world);
            return cachedStrider;
        }
        if (type == EntityType.AXOLOTL) {
            if (cachedAxolotl == null || cachedAxolotl.getWorld() != world)
                cachedAxolotl = new AxolotlEntity(EntityType.AXOLOTL, world);
            return cachedAxolotl;
        }
        if (type == EntityType.SNOW_GOLEM) {
            if (cachedSnowGolem == null || cachedSnowGolem.getWorld() != world)
                cachedSnowGolem = new SnowGolemEntity(EntityType.SNOW_GOLEM, world);
            return cachedSnowGolem;
        }
        if (type == EntityType.CAMEL) {
            if (cachedCamel == null || cachedCamel.getWorld() != world)
                cachedCamel = new CamelEntity(EntityType.CAMEL, world);
            return cachedCamel;
        }
        if (type == EntityType.ZOMBIE_VILLAGER) {
            if (cachedZombieVillager == null || cachedZombieVillager.getWorld() != world)
                cachedZombieVillager = new ZombieVillagerEntity(EntityType.ZOMBIE_VILLAGER, world);
            return cachedZombieVillager;
        }
        if (type == EntityType.RAVAGER) {
            if (cachedRavager == null || cachedRavager.getWorld() != world)
                cachedRavager = new RavagerEntity(EntityType.RAVAGER, world);
            return cachedRavager;
        }
        if (type == EntityType.WITCH) {
            if (cachedWitch == null || cachedWitch.getWorld() != world)
                cachedWitch = new WitchEntity(EntityType.WITCH, world);
            return cachedWitch;
        }
        if (type == EntityType.IRON_GOLEM) {
            if (cachedIronGolem == null || cachedIronGolem.getWorld() != world)
                cachedIronGolem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
            return cachedIronGolem;
        }
        if (type == EntityType.COW) {
            if (cachedCow == null || cachedCow.getWorld() != world)
                cachedCow = new CowEntity(EntityType.COW, world);
            return cachedCow;
        }
        if (type == EntityType.MOOSHROOM) {
            if (cachedMooshroom == null || cachedMooshroom.getWorld() != world)
                cachedMooshroom = new MooshroomEntity(EntityType.MOOSHROOM, world);
            return cachedMooshroom;
        }
        if (type == EntityType.PIG) {
            if (cachedPig == null || cachedPig.getWorld() != world)
                cachedPig = new PigEntity(EntityType.PIG, world);
            return cachedPig;
        }
        if (type == EntityType.SHEEP) {
            if (cachedSheep == null || cachedSheep.getWorld() != world)
                cachedSheep = new SheepEntity(EntityType.SHEEP, world);
            return cachedSheep;
        }
        if (type == EntityType.CHICKEN) {
            if (cachedChicken == null || cachedChicken.getWorld() != world)
                cachedChicken = new ChickenEntity(EntityType.CHICKEN, world);
            return cachedChicken;
        }
        if (type == EntityType.ENDERMAN) {
            if (cachedEnderman == null || cachedEnderman.getWorld() != world)
                cachedEnderman = new EndermanEntity(EntityType.ENDERMAN, world);
            return cachedEnderman;
        }
        if (type == EntityType.CREEPER) {
            if (cachedCreeper == null || cachedCreeper.getWorld() != world)
                cachedCreeper = new CreeperEntity(EntityType.CREEPER, world);
            return cachedCreeper;
        }
        if (type == EntityType.COD) {
            if (cachedCod == null || cachedCod.getWorld() != world)
                cachedCod = new CodEntity(EntityType.COD, world);
            return cachedCod;
        }
        if (type == EntityType.SALMON) {
            if (cachedSalmon == null || cachedSalmon.getWorld() != world)
                cachedSalmon = new SalmonEntity(EntityType.SALMON, world);
            return cachedSalmon;
        }
        if (type == EntityType.PUFFERFISH) {
            if (cachedPufferfish == null || cachedPufferfish.getWorld() != world)
                cachedPufferfish = new PufferfishEntity(EntityType.PUFFERFISH, world);
            return cachedPufferfish;
        }
        if (type == EntityType.TROPICAL_FISH) {
            if (cachedTropicalFish == null || cachedTropicalFish.getWorld() != world)
                cachedTropicalFish = new TropicalFishEntity(EntityType.TROPICAL_FISH, world);
            return cachedTropicalFish;
        }
        if (type == EntityType.SQUID) {
            if (cachedSquid == null || cachedSquid.getWorld() != world)
                cachedSquid = new SquidEntity(EntityType.SQUID, world);
            return cachedSquid;
        }
        if (type == EntityType.DOLPHIN) {
            if (cachedDolphin == null || cachedDolphin.getWorld() != world)
                cachedDolphin = new DolphinEntity(EntityType.DOLPHIN, world);
            return cachedDolphin;
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

        shell.prevX = player.prevX;
        shell.prevY = player.prevY;
        shell.prevZ = player.prevZ;

        float yaw   = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);

        if (shell instanceof ShulkerEntity) {
            yaw = 0.0f;
            pitch = 0.0f;
            shell.lastRenderX = player.getX();
            shell.lastRenderY = player.getY();
            shell.lastRenderZ = player.getZ();
            shell.prevX = player.getX();
            shell.prevY = player.getY();
            shell.prevZ = player.getZ();
        }

        shell.setYaw(yaw);
        shell.prevYaw   = player.prevYaw;
        shell.setPitch(pitch);
        shell.prevPitch = player.prevPitch;

        shell.bodyYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        shell.prevBodyYaw = player.prevBodyYaw;
        shell.headYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        shell.prevHeadYaw = player.prevHeadYaw;

        if (shell instanceof ShulkerEntity) {
            shell.prevYaw = 0.0f;
            shell.prevPitch = 0.0f;
            shell.bodyYaw = 0.0f;
            shell.prevBodyYaw = 0.0f;
            shell.headYaw = 0.0f;
            shell.prevHeadYaw = 0.0f;
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
        shell.setFireTicks(player.getFireTicks());

        // Set shell limb animator to the player's interpolated limb position.
        // Using getPos(tickDelta) for smooth inter-tick values, and setting
        // both pos and speed so the model animates at the correct amplitude.
        float limbPos   = player.limbAnimator.getPos(tickDelta);
        float limbSpeed = player.limbAnimator.getSpeed();
        LIMB_POS.set(shell.limbAnimator,   limbPos);
        LIMB_SPEED.set(shell.limbAnimator, limbSpeed);

        if (shell instanceof ShulkerEntity) {
            shell.setSneaking(false);
            shell.setSprinting(false);
            shell.setSwimming(false);
            shell.setVelocity(0.0, 0.0, 0.0);
            LIMB_POS.set(shell.limbAnimator, 0.0f);
            LIMB_SPEED.set(shell.limbAnimator, 0.0f);
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
            boolean usingCrossbow = player.isUsingItem()
                    && (mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    || offHand.isOf(net.minecraft.item.Items.CROSSBOW));
            net.minecraft.item.ItemStack activeCrossbow = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    ? mainHand : offHand;
            boolean charging = usingCrossbow && !net.minecraft.item.CrossbowItem.isCharged(activeCrossbow);
            pillager.setCharging(charging);

            CrossbowAnimationOverride override = (CrossbowAnimationOverride) pillager;
            if (charging) {
                override.samrequiemmod$setUseTimeOverride(player.getItemUseTimeLeft(), player.getItemUseTime(), activeCrossbow);
            } else {
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
                Entity target = player.getWorld().getPlayerByUuid(targetUuid);
                if (target == null) {
                    for (LivingEntity entity : player.getWorld().getEntitiesByClass(
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
            SLIME_LAST_STRETCH.set(slime, (float) SLIME_STRETCH.get(slime));
            SLIME_STRETCH.set(slime, wave);
            LIMB_POS.set(slime.limbAnimator, 0.0f);
            LIMB_SPEED.set(slime.limbAnimator, 0.0f);
        }

        if (shell instanceof WolfEntity wolf) {
            wolf.setBaby(net.sam.samrequiemmod.possession.wolf.WolfBabyState.isClientBaby(player.getUuid()));
            String variantId = net.sam.samrequiemmod.possession.wolf.WolfState.getClientVariant(player.getUuid());
            net.minecraft.registry.RegistryKey<net.minecraft.entity.passive.WolfVariant> key =
                    net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WOLF_VARIANT,
                            net.minecraft.util.Identifier.of(variantId));
            player.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.WOLF_VARIANT)
                    .getEntry(key).ifPresent(wolf::setVariant);

            boolean angry = net.sam.samrequiemmod.possession.wolf.WolfState.isClientAngry(player.getUuid());
            wolf.setAngerTime(angry ? 40 : 0);
            wolf.setAngryAt(angry ? player.getUuid() : null);

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

        if (shell instanceof FoxEntity fox) {
            fox.setVariant(FoxEntity.Type.byName(net.sam.samrequiemmod.possession.fox.FoxState.getClientVariant(player.getUuid())));
        }

        if (shell instanceof CatEntity cat) {
            String variantId = net.sam.samrequiemmod.possession.feline.CatState.getClientVariant(player.getUuid());
            net.minecraft.registry.RegistryKey<net.minecraft.entity.passive.CatVariant> key =
                    net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.CAT_VARIANT,
                            net.minecraft.util.Identifier.of(variantId));
            player.getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.CAT_VARIANT)
                    .getEntry(key).ifPresent(cat::setVariant);
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
            bee.setAngerTime(angry ? 200 : 0);
            bee.setAngryAt(angry ? player.getUuid() : null);
            bee.setOnGround(false);
        }

        if (shell instanceof ParrotEntity parrot) {
            boolean flying = net.sam.samrequiemmod.possession.beast.BeastState.isClientParrotFlying(player.getUuid());
            float flap = flying
                    ? 0.9f + 0.35f * MathHelper.sin((player.age + tickDelta) * 1.35f)
                    : 0.0f;
            PARROT_PREV_FLAP_PROGRESS.set(parrot, (float) PARROT_FLAP_PROGRESS.get(parrot));
            PARROT_PREV_MAX_WING_DEVIATION.set(parrot, (float) PARROT_MAX_WING_DEVIATION.get(parrot));
            PARROT_FLAP_PROGRESS.set(parrot, (float) PARROT_FLAP_PROGRESS.get(parrot) + (flying ? 0.8f : 0.0f));
            PARROT_MAX_WING_DEVIATION.set(parrot, flap);
            parrot.setOnGround(!flying);
        }

        if (shell instanceof VillagerEntity villager) {
            villager.setBaby(net.sam.samrequiemmod.possession.villager.VillagerState.isClientBaby(player.getUuid()));
            VillagerType villagerType = VillagerType.forBiome(player.getWorld().getBiome(player.getBlockPos()));
            villager.setVillagerData(new VillagerData(villagerType, VillagerProfession.NONE, 1));
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

        // Chicken: drive wing flapping animation based on player's airborne state.
        // Vanilla ChickenEntity.tickMovement() calculates flapProgress/maxWingDeviation
        // from movement, but our cached shell doesn't tick — so we set the fields manually.
        if (shell instanceof ChickenEntity chicken) {
            chicken.prevFlapProgress = chicken.flapProgress;
            chicken.prevMaxWingDeviation = chicken.maxWingDeviation;
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

            squid.prevTentacleAngle = squid.tentacleAngle;
            squid.prevTiltAngle = squid.tiltAngle;
            squid.prevRollAngle = squid.rollAngle;

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
                || player.getVehicle() instanceof SpiderEntity)) {
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
