package net.sam.samrequiemmod.possession.zombie;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.NoPenaltyTargeting;
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
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Behaviour controller for baby zombie possession.
 * Identical to adult zombie in all ways except:
 *  - Baby zombie sounds (ambient, hurt, death, attack)
 *  - Faster movement speed
 *  - Smaller hitbox (handled in PossessionProfiles)
 */
public final class BabyZombiePossessionController {

    private BabyZombiePossessionController() {}

    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100;

    public static void register() {

        // ── Player attacks a mob ─────────────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isBabyZombiePossessing(serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            // Always-passive mobs (zombie subtypes + slimes): apply our damage values,
            // but never mark them as provoked.
            if (ZombiePossessionController.isAlwaysPassive(entity)) {
                float passiveDamage = calculateDamage(serverPlayer);
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), passiveDamage);
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
                    serverPlayer.getEntityWorld(),
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    damage);

            if (damaged) {
                // Baby zombie uses a higher-pitched attack sound
                world.playSound(null,
                        serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                        SoundCategory.PLAYERS, 0.65f, 1.6f); // high pitch = baby
            }

            LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
            serverPlayer.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── A mob damages the baby zombie player ─────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isBabyZombiePossessing(player)) return true;

            // Poison immunity
            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            // Baby zombie hurt sound (higher pitch)
            Entity attacker = source.getAttacker();
            // Slimes cancel damage entirely in SamuelRequiemMod — don't play hurt sound for them
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_ZOMBIE_HURT, 1.6f);

            if (attacker == null) return true;

            if (!ZombiePossessionController.isZombieSubtype(attacker)) {
                ZombieTargetingState.markProvoked(attacker.getUuid(), player.getUuid());
            }

            rallyNearbyZombies(player, attacker);
            return true;
        });

        // ── Death ────────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && isBabyZombiePossessing(player)) {
                // Baby zombie death sound (higher pitch)
                player.getEntityWorld().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.6f);
            }
        });

        // ── Villager zombification (same as adult) ───────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof VillagerEntity villager)) return true;
            if (!(entity.getEntityWorld() instanceof ServerWorld serverWorld)) return true;

            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity zombiePlayer)) return true;
            if (!isBabyZombiePossessing(zombiePlayer)) return true;

            if (amount < villager.getHealth()) return true;

            float chance = switch (serverWorld.getDifficulty()) {
                case EASY   -> 0.0f;
                case NORMAL -> 0.5f;
                case HARD   -> 1.0f;
                default     -> 0.0f;
            };

            if (chance <= 0.0f) return true;
            if (chance < 1.0f && serverWorld.getRandom().nextFloat() >= chance) return true;

            ZombieVillagerEntity zombieVillager = EntityType.ZOMBIE_VILLAGER.create(serverWorld, SpawnReason.CONVERSION);
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
        if (!isBabyZombiePossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }

        preventNaturalHealing(player);
        lockHunger(player);
        handleSunlightBurn(player);
        handleAmbientSound(player);
        preventSwimming(player);
        preventDrowning(player);
        ZombiePossessionController.handleWaterConversion(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        repelVillagers(player);
        aggroIronGolems(player);
        aggroSnowGolems(player);
        tickArmsRaised(player);
        net.sam.samrequiemmod.possession.drowned.DrownedTridentManager.removeTrident(player);
        net.sam.samrequiemmod.possession.drowned.DrownedTridentManager.clearPlayer(player.getUuid());
    }

    // ── Check ────────────────────────────────────────────────────────────────

    public static boolean isBabyZombiePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOMBIE
                && BabyZombieState.isServerBaby(player);
    }

    // ── Damage ───────────────────────────────────────────────────────────────

    private static float calculateDamage(ServerPlayerEntity player) {
        double playerAttackDamage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        boolean holdingWeapon = playerAttackDamage > 1.5;
        if (holdingWeapon) return (float) playerAttackDamage;
        return getZombieBaseDamage(player.getEntityWorld().getDifficulty());
    }

    private static float getZombieBaseDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 2.0f;
            case NORMAL -> 3.0f;
            case HARD   -> 4.5f;
            default     -> 3.0f;
        };
    }

    // ── Rally ────────────────────────────────────────────────────────────────

    private static void rallyNearbyZombies(ServerPlayerEntity player, Entity threat) {
        if (!(threat instanceof LivingEntity livingThreat)) return;
        Box box = player.getBoundingBox().expand(40.0);
        List<ZombieEntity> nearbyZombies = player.getEntityWorld().getEntitiesByClass(
                ZombieEntity.class, box, z -> z.isAlive());
        for (ZombieEntity zombie : nearbyZombies) {
            zombie.setTarget(livingThreat);
        }
    }

    // ── Arms raised ──────────────────────────────────────────────────────────

    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    // ── Tick helpers (identical to adult) ────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        // Keep hunger at 6: low enough to allow eating (vanilla blocks eating at 20),
        // high enough to prevent starvation damage (damage fires at 0 on Hard, <3 on Normal).
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void preventNaturalHealing(ServerPlayerEntity player) {
        // HungerManagerMixin already blocks passive healing for possessed players.
        // Do not touch timeUntilRegen here: vanilla also uses it for hurt i-frames.
    }

    private static void preventSwimming(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.NoSwimPossessionHelper.disableSwimmingPose(player);
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
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    private static void handleSunlightBurn(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getEntityWorld().isDay()) return;
        if (player.isTouchingWaterOrRain()) return;
        BlockPos eyePos = BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ());
        if (!player.getEntityWorld().isSkyVisible(eyePos)) return;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isEmpty())
            player.setOnFireFor(8);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        // Baby zombie ambient: same sound event, higher pitch
        player.getEntityWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.6f);
    }

    private static void repelVillagers(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return;
        Box box = player.getBoundingBox().expand(16.0);
        List<VillagerEntity> villagers = player.getEntityWorld().getEntitiesByClass(
                VillagerEntity.class, box, villager -> villager.isAlive());
        for (VillagerEntity villager : villagers) {
            if (villager.squaredDistanceTo(player) > 8.0 * 8.0) continue;
            Vec3d target = NoPenaltyTargeting.findFrom(villager, 16, 7, player.getEntityPos());
            if (target == null) {
                Vec3d away = villager.getEntityPos().subtract(player.getEntityPos());
                if (away.lengthSquared() < 0.001) away = new Vec3d(1, 0, 0);
                target = villager.getEntityPos().add(away.normalize().multiply(10.0));
            }
            villager.getNavigation().startMovingTo(target.x, target.y, target.z, 0.6);
        }
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        List<IronGolemEntity> golems = player.getEntityWorld().getEntitiesByClass(
                IronGolemEntity.class, box, golem -> golem.isAlive());
        for (IronGolemEntity golem : golems) {
            if (golem.squaredDistanceTo(player) <= 24.0 * 24.0) {
                golem.setTarget(player);
                golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
            }
        }
    }

    private static void aggroSnowGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(10.0);
        List<SnowGolemEntity> golems = player.getEntityWorld().getEntitiesByClass(
                SnowGolemEntity.class, box, golem -> golem.isAlive());
        for (SnowGolemEntity golem : golems) {
            golem.setTarget(player);
        }
    }

    // ── Food helpers (used by ZombieFoodUseHandler) ───────────────────────────

    public static boolean isBabyZombieFood(ItemStack stack) {
        return ZombiePossessionController.isZombieFood(stack);
    }

    public static float getBabyZombieFoodHealing(ItemStack stack) {
        return ZombiePossessionController.getZombieFoodHealing(stack);
    }
}





