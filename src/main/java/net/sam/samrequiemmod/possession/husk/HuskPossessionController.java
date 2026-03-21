package net.sam.samrequiemmod.possession.husk;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombiePossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HuskPossessionController {

    private HuskPossessionController() {}

    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100;
    // Hunger effect duration: 20 seconds = 400 ticks
    private static final int HUNGER_DURATION_TICKS = 400;

    public static void register() {

        // ── Player attacks a mob ─────────────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isHuskPossessing(serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            // Always-passive mobs (zombie subtypes + slimes): apply our damage values,
            // but never mark them as provoked.
            if (ZombiePossessionController.isAlwaysPassive(entity)) {
                float passiveDamage = calculateDamage(serverPlayer);
                livingTarget.damage(serverPlayer.getDamageSources().playerAttack(serverPlayer), passiveDamage);
                LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
                ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
                serverPlayer.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            // Mark provoked BEFORE damage so mob can retaliate
            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid());
            }

            float damage = calculateDamage(serverPlayer);
            boolean damaged = livingTarget.damage(
                    serverPlayer.getDamageSources().playerAttack(serverPlayer), damage);

            if (damaged) {
                // Apply Hunger I for 20 seconds — husk's signature effect
                livingTarget.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.HUNGER, HUNGER_DURATION_TICKS, 0, false, true));

                world.playSound(null,
                        serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_HUSK_AMBIENT,
                        SoundCategory.PLAYERS, 0.65f, 1.0f);
            }

            LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
            serverPlayer.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── A mob damages the husk player ────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isHuskPossessing(player)) return true;

            // Poison immunity
            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            Entity attacker = source.getAttacker();
            // Slimes cancel damage entirely in SamuelRequiemMod — don't play hurt sound for them
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            player.getWorld().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_HUSK_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (attacker == null) return true;

            if (!ZombiePossessionController.isZombieSubtype(attacker)) {
                ZombieTargetingState.markProvoked(attacker.getUuid(), player.getUuid());
            }

            rallyNearbyZombies(player, attacker);
            return true;
        });

        // ── Death ────────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && isHuskPossessing(player)) {
                player.getWorld().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_HUSK_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            if (entity instanceof MobEntity) {
                ZombieTargetingState.clearProvoked(entity.getUuid());
            }
        });

        // ── Villager zombification ────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof VillagerEntity villager)) return true;
            if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return true;

            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity huskPlayer)) return true;
            if (!isHuskPossessing(huskPlayer)) return true;

            if (amount < villager.getHealth()) return true;

            float chance = switch (serverWorld.getDifficulty()) {
                case EASY   -> 0.0f;
                case NORMAL -> 0.5f;
                case HARD   -> 1.0f;
                default     -> 0.0f;
            };

            if (chance <= 0.0f) return true;
            if (chance < 1.0f && serverWorld.getRandom().nextFloat() >= chance) return true;

            ZombieVillagerEntity zombieVillager = EntityType.ZOMBIE_VILLAGER.create(serverWorld);
            if (zombieVillager == null) return true;

            zombieVillager.refreshPositionAndAngles(
                    villager.getX(), villager.getY(), villager.getZ(),
                    villager.getYaw(), villager.getPitch());
            zombieVillager.setVillagerData(villager.getVillagerData());
            zombieVillager.setBaby(villager.isBaby());
            zombieVillager.initialize(serverWorld,
                    serverWorld.getLocalDifficulty(zombieVillager.getBlockPos()),
                    SpawnReason.CONVERSION, null);
            zombieVillager.setBaby(villager.isBaby());
            zombieVillager.setPersistent();

            villager.discard();
            serverWorld.spawnEntityAndPassengers(zombieVillager);
            serverWorld.playSound(null,
                    villager.getX(), villager.getY(), villager.getZ(),
                    SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 1.0f, 1.0f);
            return false;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isHuskPossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }

        preventNaturalHealing(player);
        lockHunger(player);
        // NO sunlight burn — husks are immune to daylight
        handleAmbientSound(player);
        preventSwimming(player);
        preventDrowning(player);
        handleWaterConversion(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        repelVillagers(player);
        aggroIronGolems(player);
        aggroSnowGolems(player);
        tickArmsRaised(player);
    }

    public static boolean isHuskPossessing(PlayerEntity player) {
        // Exclude baby husks — they are handled by BabyHuskPossessionController
        return PossessionManager.getPossessedType(player) == EntityType.HUSK
                && !BabyHuskState.isServerBaby(player);
    }

    private static float calculateDamage(ServerPlayerEntity player) {
        double atk = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (atk > 1.5) return (float) atk;
        return getHuskBaseDamage(player.getServerWorld().getDifficulty());
    }

    private static float getHuskBaseDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 2.0f;
            case NORMAL -> 3.0f;
            case HARD   -> 4.5f;
            default     -> 3.0f;
        };
    }

    private static void rallyNearbyZombies(ServerPlayerEntity player, Entity threat) {
        if (!(threat instanceof LivingEntity livingThreat)) return;
        Box box = player.getBoundingBox().expand(40.0);
        List<ZombieEntity> nearby = player.getWorld().getEntitiesByClass(
                ZombieEntity.class, box, z -> z.isAlive());
        for (ZombieEntity zombie : nearby) zombie.setTarget(livingThreat);
    }

    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        // Keep hunger at 6: low enough to allow eating (vanilla blocks eating at 20),
        // high enough to prevent starvation damage (damage fires at 0 on Hard, <3 on Normal).
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void preventNaturalHealing(ServerPlayerEntity player) {
        if (player.timeUntilRegen > 0) player.timeUntilRegen = 0;
    }

    /**
     * Husk submerged 15s→shake, 30s→converts to zombie.
     * Baby husk converts to baby zombie.
     */
    public static void handleWaterConversion(ServerPlayerEntity player) {
        if (!player.isSubmergedInWater()) {
            int prevTicks = net.sam.samrequiemmod.possession.WaterConversionTracker.getSubmergedTicks(player.getUuid());
            net.sam.samrequiemmod.possession.WaterConversionTracker.resetSubmerged(player.getUuid());
            if (prevTicks >= 300) net.sam.samrequiemmod.possession.WaterShakeNetworking.broadcast(player, false);
            return;
        }

        net.sam.samrequiemmod.possession.WaterConversionTracker.tickSubmerged(player.getUuid());
        int ticks = net.sam.samrequiemmod.possession.WaterConversionTracker.getSubmergedTicks(player.getUuid());

        if (ticks == 300) {
            net.sam.samrequiemmod.possession.WaterShakeNetworking.broadcast(player, true);
        }

        if (ticks >= 600) {
            net.sam.samrequiemmod.possession.WaterConversionTracker.resetSubmerged(player.getUuid());

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_HUSK_CONVERTED_TO_ZOMBIE,
                    net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 1.0f);

            net.sam.samrequiemmod.possession.WaterShakeNetworking.broadcast(player, false);

            boolean isBaby = BabyHuskState.isServerBaby(player);

            // Set baby state BEFORE startPossession so PossessionEffects picks up correct profile
            net.sam.samrequiemmod.possession.PossessionManager.clearPossession(player);
            if (isBaby) {
                BabyZombieState.setServerBaby(player.getUuid(), true);
            }
            net.sam.samrequiemmod.possession.PossessionManager.startPossession(
                    player, net.minecraft.entity.EntityType.ZOMBIE, player.getHealth());
            if (isBaby) {
                BabyZombieNetworking.broadcastBabyZombieSync(player, true);
            }
        }
    }

    private static void preventSwimming(ServerPlayerEntity player) {
        if (!player.isTouchingWater()) return;
        if (player.isSwimming()) player.setSwimming(false);
        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x * 0.5, vel.y - 0.03, vel.z * 0.5);
        player.velocityModified = true;
    }

    private static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) player.setAir(player.getMaxAir());
    }

    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON))
            player.removeStatusEffect(StatusEffects.POISON);
    }

    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;
        float heal = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_HUSK_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void repelVillagers(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return;
        Box box = player.getBoundingBox().expand(16.0);
        List<VillagerEntity> villagers = player.getWorld().getEntitiesByClass(
                VillagerEntity.class, box, v -> v.isAlive());
        for (VillagerEntity villager : villagers) {
            if (villager.squaredDistanceTo(player) > 8.0 * 8.0) continue;
            Vec3d target = net.minecraft.entity.ai.NoPenaltyTargeting.findFrom(villager, 16, 7, player.getPos());
            if (target == null) {
                Vec3d away = villager.getPos().subtract(player.getPos());
                if (away.lengthSquared() < 0.001) away = new Vec3d(1, 0, 0);
                target = villager.getPos().add(away.normalize().multiply(10.0));
            }
            villager.getNavigation().startMovingTo(target.x, target.y, target.z, 0.6);
        }
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        List<IronGolemEntity> golems = player.getWorld().getEntitiesByClass(
                IronGolemEntity.class, box, g -> g.isAlive());
        for (IronGolemEntity golem : golems) {
            if (golem.squaredDistanceTo(player) <= 24.0 * 24.0) {
                golem.setTarget(player);
                golem.setAngryAt(player.getUuid());
            }
        }
    }

    private static void aggroSnowGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(10.0);
        List<SnowGolemEntity> golems = player.getWorld().getEntitiesByClass(
                SnowGolemEntity.class, box, g -> g.isAlive());
        for (SnowGolemEntity golem : golems) golem.setTarget(player);
    }

    public static boolean isHuskFood(ItemStack stack) {
        return ZombiePossessionController.isZombieFood(stack);
    }

    public static float getHuskFoodHealing(ItemStack stack) {
        return ZombiePossessionController.getZombieFoodHealing(stack);
    }
}