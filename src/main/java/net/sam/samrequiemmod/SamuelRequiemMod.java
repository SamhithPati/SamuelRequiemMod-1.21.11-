package net.sam.samrequiemmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.sam.samrequiemmod.entity.CorruptedMerchantEntity;
import net.sam.samrequiemmod.entity.ModEntities;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionCommand;
import net.sam.samrequiemmod.possession.PossessionDimensionHelper;
import net.sam.samrequiemmod.possession.PossessionEffects;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.PossessionNetworking;
import net.sam.samrequiemmod.world.ModStructures;
import net.sam.samrequiemmod.world.ShrineCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sam.samrequiemmod.possession.RelicPossessionHandler;
import net.sam.samrequiemmod.possession.RelicUnpossessHandler;
import net.sam.samrequiemmod.possession.zombie.ZombieFoodUseHandler;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombiePossessionController;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedPossessionController;
import net.sam.samrequiemmod.possession.drowned.DrownedPossessionController;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskPossessionController;
import net.sam.samrequiemmod.possession.husk.HuskPossessionController;

public class SamuelRequiemMod implements ModInitializer {

	public static final String MOD_ID = "samrequiemmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Players currently in post-possession immunity (UUID -> expiry tick). */
	public static final java.util.Map<java.util.UUID, Long> POST_POSSESSION_IMMUNITY =
			new java.util.concurrent.ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModEntities.register();
		PossessionNetworking.registerCommon();
		ZombieAttackSyncNetworking.registerCommon();
		BabyZombieNetworking.registerCommon();
		BabyHuskNetworking.registerCommon();
		BabyDrownedNetworking.registerCommon();
		net.sam.samrequiemmod.possession.WaterShakeNetworking.registerCommon();
		net.sam.samrequiemmod.possession.illager.EvokerNetworking.registerCommon();
		net.sam.samrequiemmod.possession.zombie.ChickenJumpNetworking.registerCommon();
		net.sam.samrequiemmod.possession.illager.RavagerJumpNetworking.registerCommon();
		net.sam.samrequiemmod.possession.illager.CaptainNetworking.registerCommon();
		net.sam.samrequiemmod.possession.illager.RavagerNetworking.registerCommon();
		net.sam.samrequiemmod.possession.illager.WitchNetworking.registerCommon();
		net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.registerCommon();
		net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking.registerCommon();
		net.sam.samrequiemmod.possession.passive.MooshroomNetworking.registerCommon();
		net.sam.samrequiemmod.possession.iron_golem.IronGolemNetworking.registerCommon();
		net.sam.samrequiemmod.possession.skeleton.WitherSkeletonAttackNetworking.registerCommon();
		net.sam.samrequiemmod.possession.skeleton.SpiderJumpNetworking.registerCommon();
		net.sam.samrequiemmod.possession.enderman.EndermanNetworking.registerCommon();
		net.sam.samrequiemmod.possession.creeper.CreeperNetworking.registerCommon();
		net.sam.samrequiemmod.possession.aquatic.PufferfishNetworking.registerCommon();
		net.sam.samrequiemmod.possession.aquatic.TropicalFishVariantNetworking.registerCommon();
		net.sam.samrequiemmod.possession.piglin.BabyPiglinNetworking.registerCommon();
		net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinNetworking.registerCommon();
		net.sam.samrequiemmod.possession.hoglin.BabyHoglinNetworking.registerCommon();
		net.sam.samrequiemmod.possession.hoglin.HoglinAttackNetworking.registerCommon();
		net.sam.samrequiemmod.possession.guardian.GuardianNetworking.registerCommon();
		net.sam.samrequiemmod.possession.silverfish.SilverfishHideNetworking.registerCommon();
		net.sam.samrequiemmod.possession.firemob.FireMobNetworking.registerCommon();
		net.sam.samrequiemmod.possession.slime.SlimeSizeNetworking.registerCommon();
		net.sam.samrequiemmod.possession.wolf.WolfNetworking.registerCommon();
		net.sam.samrequiemmod.possession.fox.FoxNetworking.registerCommon();
		net.sam.samrequiemmod.possession.feline.CatNetworking.registerCommon();
		net.sam.samrequiemmod.possession.vex.VexNetworking.registerCommon();
		net.sam.samrequiemmod.possession.villager.VillagerNetworking.registerCommon();
		net.sam.samrequiemmod.possession.beast.BeastNetworking.registerCommon();
		net.sam.samrequiemmod.possession.beast.BeastAttackNetworking.registerCommon();

		FabricDefaultAttributeRegistry.register(
				ModEntities.CORRUPTED_MERCHANT,
				CorruptedMerchantEntity.createAttributes()
		);

		ModStructures.register();
		net.sam.samrequiemmod.world.ModLootTableModifiers.register();
		ShrineCommand.register();
		PossessionCommand.register();
		RelicPossessionHandler.register();
		RelicUnpossessHandler.register();
		ZombiePossessionController.register();
		BabyZombiePossessionController.register();
		HuskPossessionController.register();
		BabyHuskPossessionController.register();
		DrownedPossessionController.register();
		BabyDrownedPossessionController.register();
		net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.register();
		net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.register();
		net.sam.samrequiemmod.possession.illager.PillagerPossessionController.register();
		net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.register();
		net.sam.samrequiemmod.possession.illager.EvokerPossessionController.register();
		net.sam.samrequiemmod.possession.illager.EvokerNetworking.registerServer();
		net.sam.samrequiemmod.possession.zombie.ChickenJumpNetworking.registerServer();
		net.sam.samrequiemmod.possession.illager.RavagerJumpNetworking.registerServer();
		net.sam.samrequiemmod.possession.illager.RavagerRidingHandler.register();
		net.sam.samrequiemmod.possession.illager.CaptainNetworking.registerServer();
		net.sam.samrequiemmod.possession.illager.RavagerNetworking.registerServer();
		net.sam.samrequiemmod.possession.illager.RavagerPossessionController.register();
		net.sam.samrequiemmod.possession.illager.WitchPossessionController.register();
		net.sam.samrequiemmod.possession.illager.WitchNetworking.registerServer();
		net.sam.samrequiemmod.possession.illager.CaptainHandler.register();
		net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.register();
		net.sam.samrequiemmod.possession.passive.MooshroomPossessionController.register();
		net.sam.samrequiemmod.possession.passive.MooshroomNetworking.registerServer();
		net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.register();
		net.sam.samrequiemmod.possession.iron_golem.IronGolemNetworking.registerServer();
		net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.register();
		net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.register();
		net.sam.samrequiemmod.possession.skeleton.SpiderJumpNetworking.registerServer();
		net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.register();
		net.sam.samrequiemmod.possession.enderman.EndermanNetworking.registerServer();
		net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.register();
		net.sam.samrequiemmod.possession.creeper.CreeperNetworking.registerServer();
		net.sam.samrequiemmod.possession.aquatic.FishPossessionController.register();
		net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.register();
		net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.register();
		net.sam.samrequiemmod.possession.spider.SpiderPossessionController.register();
		net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.register();
		net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.register();
		net.sam.samrequiemmod.possession.guardian.GuardianNetworking.registerServer();
		net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.register();
		net.sam.samrequiemmod.possession.silverfish.SilverfishHideNetworking.registerServer();
		net.sam.samrequiemmod.possession.firemob.FireMobNetworking.registerServer();
		net.sam.samrequiemmod.possession.firemob.BlazePossessionController.register();
		net.sam.samrequiemmod.possession.firemob.GhastPossessionController.register();
		net.sam.samrequiemmod.possession.slime.SlimePossessionController.register();
		net.sam.samrequiemmod.possession.wolf.WolfNetworking.registerServer();
		net.sam.samrequiemmod.possession.wolf.WolfPossessionController.register();
		net.sam.samrequiemmod.possession.fox.FoxPossessionController.register();
		net.sam.samrequiemmod.possession.feline.FelinePossessionController.register();
		net.sam.samrequiemmod.possession.vex.VexNetworking.registerServer();
		net.sam.samrequiemmod.possession.vex.VexPossessionController.register();
		net.sam.samrequiemmod.possession.bat.BatPossessionController.register();
		net.sam.samrequiemmod.possession.villager.VillagerPossessionController.register();
		net.sam.samrequiemmod.possession.beast.BeastNetworking.registerServer();
		net.sam.samrequiemmod.possession.beast.BeastPossessionController.register();
		net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.register();
		net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.register();
		net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.register();
		net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.register();
		net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.register();
		ZombieFoodUseHandler.register();

		// When a possessed player dies, cancel the death and unpossess them instead
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					if (!PossessionManager.isPossessing(player)) return true;
					// If this damage would kill the player, cancel it and unpossess instead.
					// Use getHealth() directly — vanilla already applies armor before calling ALLOW_DAMAGE
					// for most damage sources, so comparing against health is sufficient.
					float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();
					if (effectiveHealth - amount <= 0) {
						if (net.sam.samrequiemmod.possession.slime.SlimePossessionController.handleLethalSplit(player)) {
							return false;
						}
						// Play death sound for illager possessions
						if (net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_PILLAGER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_VINDICATOR_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_EVOKER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_RAVAGER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_WITCH_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.isIronGolemPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_IRON_GOLEM_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.isAnySkeletonPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_SKELETON_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.isWitherSkeletonPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_WITHER_SKELETON_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.isEndermanPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.isCreeperPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_CREEPER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.spider.SpiderPossessionController.isAnySpiderPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_SPIDER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isAnyHoglinTypePossessing(player)) {
							net.minecraft.sound.SoundEvent deathSound =
									net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isZoglinPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_ZOGLIN_DEATH
											: net.minecraft.sound.SoundEvents.ENTITY_HOGLIN_DEATH;
							float pitch = net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isBabyHoglinPossessing(player)
									|| net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.isBabyZoglinPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									deathSound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isAnyGuardianPossessing(player)) {
							net.minecraft.sound.SoundEvent deathSound =
									net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.isElderGuardianPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_ELDER_GUARDIAN_DEATH
											: net.minecraft.sound.SoundEvents.ENTITY_GUARDIAN_DEATH;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									deathSound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.isSilverfishPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_SILVERFISH_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.firemob.BlazePossessionController.isBlazePossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_BLAZE_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.firemob.GhastPossessionController.isGhastPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_GHAST_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isWolfPossessing(player)) {
							float pitch = net.sam.samrequiemmod.possession.wolf.WolfPossessionController.isBabyWolfPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_WOLF_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.fox.FoxPossessionController.isFoxPossessing(player)) {
							float pitch = net.sam.samrequiemmod.possession.fox.FoxPossessionController.isBabyFoxPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_FOX_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.feline.FelinePossessionController.isOcelotPossessing(player)) {
							float pitch = net.sam.samrequiemmod.possession.feline.FelinePossessionController.isBabyOcelotPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_OCELOT_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.feline.FelinePossessionController.isCatPossessing(player)) {
							float pitch = net.sam.samrequiemmod.possession.feline.FelinePossessionController.isBabyCatPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_CAT_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.vex.VexPossessionController.isVexPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_VEX_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.bat.BatPossessionController.isBatPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_BAT_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isVillagerPossessing(player)) {
							float pitch = net.sam.samrequiemmod.possession.villager.VillagerPossessionController.isBabyVillagerPossessing(player)
									? 1.35f : 1.0f;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, pitch);
						} else if (net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTrackedType(PossessionManager.getPossessedType(player))) {
							net.minecraft.sound.SoundEvent deathSound =
									net.sam.samrequiemmod.possession.beast.BeastPossessionController.isShulkerPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_SHULKER_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isEndermitePossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_ENDERMITE_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isGoatPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_GOAT_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isPolarBearPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_POLAR_BEAR_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isRabbitPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_RABBIT_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isTurtlePossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_TURTLE_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isAxolotlPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_AXOLOTL_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isStriderPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_STRIDER_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isSnowGolemPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_SNOW_GOLEM_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isCamelPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_CAMEL_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isBeePossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_BEE_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isParrotPossessing(player)
											? net.minecraft.sound.SoundEvents.ENTITY_PARROT_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isHorseLikePossessing(player)
											&& PossessionManager.getPossessedType(player) == net.minecraft.entity.EntityType.ZOMBIE_HORSE
											? net.minecraft.sound.SoundEvents.ENTITY_ZOMBIE_HORSE_DEATH
											: net.sam.samrequiemmod.possession.beast.BeastPossessionController.isHorseLikePossessing(player)
											&& PossessionManager.getPossessedType(player) == net.minecraft.entity.EntityType.SKELETON_HORSE
											? net.minecraft.sound.SoundEvents.ENTITY_SKELETON_HORSE_DEATH
											: net.minecraft.sound.SoundEvents.ENTITY_HORSE_DEATH;
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									deathSound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f,
									net.sam.samrequiemmod.possession.passive.BabyPassiveMobState.isServerBaby(player) ? 1.35f : 1.0f);
						} else if (net.sam.samrequiemmod.possession.aquatic.FishPossessionController.isFishPossessing(player)) {
							net.minecraft.entity.EntityType<?> possType = PossessionManager.getPossessedType(player);
							net.minecraft.sound.SoundEvent fishDeathSound = net.sam.samrequiemmod.possession.aquatic.FishPossessionController.getDeathSound(possType);
							if (fishDeathSound != null) {
								player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
										fishDeathSound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
							}
						} else if (net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.isSquidPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_SQUID_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.isDolphinPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_DOLPHIN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isPiglinPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_PIGLIN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.isBabyPiglinPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_PIGLIN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.5f);
						} else if (net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isZombifiedPiglinPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
						} else if (net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.isBabyZombifiedPiglinPossessing(player)) {
							player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
									net.minecraft.sound.SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH,
									net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.6f);
						} else {
							// Passive mob death sounds
							net.minecraft.entity.EntityType<?> possType = PossessionManager.getPossessedType(player);
							net.minecraft.sound.SoundEvent deathSound = net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.getDeathSound(possType);
							if (deathSound != null) {
								player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
										deathSound, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
							}
						}
						PossessionManager.clearPossession(player);
						player.getServer().execute(() -> {
							if (player.getHealth() <= 0) player.setHealth(1.0f);
							// 5 seconds = invisibility + invincibility effects
							// (immunity window was already set synchronously in clearPossession)
							player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
									net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 100, 0, false, false));
							player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
									net.minecraft.entity.effect.StatusEffects.RESISTANCE, 100, 4, false, false));
							// Clear all mob targets on this player so they stop chasing immediately
							net.minecraft.util.math.Box box = player.getBoundingBox().expand(48.0);
							for (net.minecraft.entity.mob.MobEntity mob : player.getServerWorld()
									.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box, m -> m.getTarget() == player)) {
								mob.setTarget(null);
							}
						});
						return false;
					}
					return true;
				});

		// Block all damage during post-possession immunity window.
		// Only applies when NOT currently possessing — immunity is for after unpossession,
		// not during active possession (which would make the player invincible while possessed).
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					if (PossessionManager.isPossessing(player)) return true; // actively possessed — allow damage
					Long expiry = POST_POSSESSION_IMMUNITY.get(player.getUuid());
					if (expiry == null) return true;
					if ((long) player.age >= expiry) {
						POST_POSSESSION_IMMUNITY.remove(player.getUuid());
						return true;
					}
					return false;
				});

		// Creeper-possessed players are immune to their own explosion.
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					if (net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.EXPLOSION_IMMUNE.contains(player.getUuid())) {
						return false;
					}
					return true;
				});

		// Slimes and magma cubes must never damage possessed players.
		// SlimeEntity.canAttack() can't be reliably mixined (no-arg, no inherited shadow),
		// so we cancel the damage at the ALLOW_DAMAGE event level instead.
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					if (!PossessionManager.isPossessing(player)) return true;
					if (source.getAttacker() instanceof net.minecraft.entity.mob.SlimeEntity) return false;
					return true;
				});

		// Evoker fangs must never damage pillager or vindicator-possessed players.
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					boolean isIllagerPossessed =
							net.sam.samrequiemmod.possession.illager.PillagerPossessionController.isPillagerPossessing(player)
									|| net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.isVindicatorPossessing(player)
									|| net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player)
									|| net.sam.samrequiemmod.possession.illager.RavagerPossessionController.isRavagerPossessing(player)
									|| net.sam.samrequiemmod.possession.illager.WitchPossessionController.isWitchPossessing(player)
									|| net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.isAnyPiglinPossessing(player)
									|| net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.isPiglinBrutePossessing(player);
					if (!isIllagerPossessed) return true;
					if (source.getSource() instanceof net.minecraft.entity.mob.EvokerFangsEntity) return false;
					return true;
				});

		// Evoker-possessed player's fangs: block damage to allies, aggro non-allies.
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(source.getSource() instanceof net.minecraft.entity.mob.EvokerFangsEntity fangs)) return true;
					if (!(fangs.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner)) return true;
					if (!net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(owner)) return true;
					// Block fang damage only to pure illagers (not ravagers or witches)
					if (entity instanceof net.minecraft.entity.mob.PillagerEntity
							|| entity instanceof net.minecraft.entity.mob.VindicatorEntity
							|| entity instanceof net.minecraft.entity.mob.EvokerEntity
							|| entity instanceof net.minecraft.entity.mob.IllusionerEntity
							|| entity instanceof net.minecraft.entity.mob.VexEntity) return false;
					// Block fang damage to the ravager the evoker is currently riding
					if (entity instanceof net.minecraft.entity.mob.RavagerEntity
							&& owner.getVehicle() == entity) return false;
					// Aggro non-ally mobs — mark them provoked and target the player
					if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
						net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.markProvoked(mob.getUuid(), owner.getUuid());
						mob.setAttacker(owner);
						if (mob.getTarget() == null) mob.setTarget(owner);
					}
					return true;
				});

		// Fire/lava immunity for zombified piglin and piglin-type possessed players.
		// Blocks fire, lava, and blaze fireball damage. Also blocks fire damage for piglins (they're nether mobs).
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register(
				(entity, source, amount) -> {
					if (!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player)) return true;
					boolean isZombifiedPiglin =
							net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.isAnyZombifiedPiglinPossessing(player);
					if (!isZombifiedPiglin) return true;
					// Block fire, lava, and "on fire" damage
					if (source.equals(player.getDamageSources().onFire())
							|| source.equals(player.getDamageSources().inFire())
							|| source.equals(player.getDamageSources().lava())
							|| source.equals(player.getDamageSources().hotFloor())) {
						return false;
					}
					// Block blaze fireball damage (fireball source)
					if (source.getSource() instanceof net.minecraft.entity.projectile.SmallFireballEntity) {
						return false;
					}
					return true;
				});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			var joinedPlayer = handler.player;

			for (var other : server.getPlayerManager().getPlayerList()) {
				PossessionNetworking.sendPossessionSync(
						joinedPlayer,
						other.getUuid(),
						PossessionManager.getPossessedType(other)
				);
			}
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
				net.sam.samrequiemmod.world.SoulTraderShrine.tick(world);

				for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
					PossessionDimensionHelper.refreshDimensionsIfNeeded(player);

					// During post-possession immunity, keep clearing any mobs that re-acquire this player as target
					Long immunityExpiry = POST_POSSESSION_IMMUNITY.get(player.getUuid());
					if (immunityExpiry != null) {
						boolean expired = (long) player.age >= immunityExpiry;
						if (expired) POST_POSSESSION_IMMUNITY.remove(player.getUuid());

						// Every tick during immunity (and once at expiry):
						// Aggressively wipe ALL player-related state from nearby mobs.
						net.minecraft.util.math.Box clearBox = player.getBoundingBox().expand(64.0);
						for (net.minecraft.entity.mob.MobEntity mob : player.getServerWorld()
								.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, clearBox, m -> m.isAlive())) {

							boolean targetsPlayer  = mob.getTarget() == player;
							boolean attackedPlayer = mob.getAttacker() == player;
							boolean angryAtPlayer  = mob instanceof net.minecraft.entity.mob.Angerable a
									&& player.getUuid().equals(a.getAngryAt());

							if (!targetsPlayer && !attackedPlayer && !angryAtPlayer) continue;

							// Wipe all state and stop navigation so mob physically stops chasing
							mob.setTarget(null);
							mob.setAttacker(null);
							mob.getNavigation().stop();
							if (mob instanceof net.minecraft.entity.mob.Angerable angerable) {
								angerable.setAngryAt(null);
								angerable.setAngerTime(0);
							}
							if (mob instanceof net.minecraft.entity.passive.IronGolemEntity golem) {
								golem.setAttacking(false);
							}
						}
					}

					// Per-mob forget loop: keep clearing any mob registered to forget this player
					for (java.util.UUID mobUuid : new java.util.HashSet<>(
							net.sam.samrequiemmod.possession.zombie.ZombieForgetPlayerState.activeMobUuids())) {
						if (!net.sam.samrequiemmod.possession.zombie.ZombieForgetPlayerState.isActive(mobUuid, player.age)) continue;
						java.util.UUID targetPlayerUuid = net.sam.samrequiemmod.possession.zombie.ZombieForgetPlayerState.getTrackedPlayer(mobUuid);
						if (!player.getUuid().equals(targetPlayerUuid)) continue;
						// Find this mob in world and clear its state
						net.minecraft.util.math.Box searchBox = player.getBoundingBox().expand(128.0);
						for (net.minecraft.entity.mob.MobEntity mob : player.getServerWorld()
								.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, searchBox,
										m -> m.getUuid().equals(mobUuid) && m.isAlive())) {
							if (mob.getTarget() == player || mob.getAttacker() == player
									|| (mob instanceof net.minecraft.entity.mob.Angerable a && player.getUuid().equals(a.getAngryAt()))) {
								mob.setTarget(null);
								mob.setAttacker(null);
								mob.getNavigation().stop();
								if (mob instanceof net.minecraft.entity.mob.Angerable a2) { a2.setAngryAt(null); a2.setAngerTime(0); }
								if (mob instanceof net.minecraft.entity.passive.IronGolemEntity g) g.setAttacking(false);
							}
						}
					}

					net.sam.samrequiemmod.possession.illager.PillagerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.illager.VindicatorPossessionController.tick(player);
					net.sam.samrequiemmod.possession.illager.EvokerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.illager.CaptainHandler.tick(player);
					net.sam.samrequiemmod.possession.illager.RaidHandler.tick(player);
					net.sam.samrequiemmod.possession.illager.RavagerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.illager.WitchPossessionController.tick(player);

					if (PossessionManager.isPossessing(player)) {
						// Hunger effect immunity for all possession types
						if (player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.HUNGER)) {
							player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.HUNGER);
						}
					}

					// Tick zombie-specific behaviours (sounds, burn, villagers, golems)
					ZombiePossessionController.tick(player);
					BabyZombiePossessionController.tick(player);
					HuskPossessionController.tick(player);
					BabyHuskPossessionController.tick(player);
					DrownedPossessionController.tick(player);
					BabyDrownedPossessionController.tick(player);
					net.sam.samrequiemmod.possession.zombie_villager.ZombieVillagerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.passive.PassiveMobPossessionController.tick(player);
					net.sam.samrequiemmod.possession.iron_golem.IronGolemPossessionController.tick(player);
					net.sam.samrequiemmod.possession.skeleton.SkeletonPossessionController.tick(player);
					net.sam.samrequiemmod.possession.skeleton.WitherSkeletonPossessionController.tick(player);
					net.sam.samrequiemmod.possession.enderman.EndermanPossessionController.tick(player);
					net.sam.samrequiemmod.possession.creeper.CreeperPossessionController.tick(player);
					net.sam.samrequiemmod.possession.aquatic.FishPossessionController.tick(player);
					net.sam.samrequiemmod.possession.aquatic.SquidPossessionController.tick(player);
					net.sam.samrequiemmod.possession.aquatic.DolphinPossessionController.tick(player);
					net.sam.samrequiemmod.possession.spider.SpiderPossessionController.tick(player);
					net.sam.samrequiemmod.possession.hoglin.HoglinPossessionController.tick(player);
					net.sam.samrequiemmod.possession.guardian.GuardianPossessionController.tick(player);
					net.sam.samrequiemmod.possession.silverfish.SilverfishPossessionController.tick(player);
					net.sam.samrequiemmod.possession.firemob.BlazePossessionController.tick(player);
					net.sam.samrequiemmod.possession.firemob.GhastPossessionController.tick(player);
					net.sam.samrequiemmod.possession.slime.SlimePossessionController.tick(player);
					net.sam.samrequiemmod.possession.wolf.WolfPossessionController.tick(player);
					net.sam.samrequiemmod.possession.fox.FoxPossessionController.tick(player);
					net.sam.samrequiemmod.possession.feline.FelinePossessionController.tick(player);
					net.sam.samrequiemmod.possession.vex.VexPossessionController.tick(player);
					net.sam.samrequiemmod.possession.bat.BatPossessionController.tick(player);
					net.sam.samrequiemmod.possession.villager.VillagerPossessionController.tick(player);
					net.sam.samrequiemmod.possession.beast.BeastPossessionController.tick(player);
					net.sam.samrequiemmod.possession.piglin.PiglinPossessionController.tick(player);
					net.sam.samrequiemmod.possession.piglin.BabyPiglinPossessionController.tick(player);
					net.sam.samrequiemmod.possession.piglin.PiglinBrutePossessionController.tick(player);
					net.sam.samrequiemmod.possession.piglin.ZombifiedPiglinPossessionController.tick(player);
					net.sam.samrequiemmod.possession.piglin.BabyZombifiedPiglinPossessionController.tick(player);
					ZombieFoodUseHandler.tick(player);
				}
			}
		});
	}
}
