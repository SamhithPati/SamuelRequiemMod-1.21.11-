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
		net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerNetworking.registerCommon();

		FabricDefaultAttributeRegistry.register(
				ModEntities.CORRUPTED_MERCHANT,
				CorruptedMerchantEntity.createAttributes()
		);

		ModStructures.register();
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
									|| net.sam.samrequiemmod.possession.illager.EvokerPossessionController.isEvokerPossessing(player);
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
					// Aggro non-ally mobs — mark them provoked and target the player
					if (entity instanceof net.minecraft.entity.mob.MobEntity mob) {
						net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.markProvoked(mob.getUuid(), owner.getUuid());
						mob.setAttacker(owner);
						if (mob.getTarget() == null) mob.setTarget(owner);
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
					ZombieFoodUseHandler.tick(player);
				}
			}
		});
	}
}