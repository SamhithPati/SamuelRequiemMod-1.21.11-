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
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ZombiePossessionController {

    private ZombiePossessionController() {
    }

    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100;

    public static void register() {

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isZombiePossessing(serverPlayer)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            // Always-passive mobs (zombie subtypes + slimes): apply our damage values
            // (vanilla would use base 1.0 HP attack), but never mark them as provoked.
            if (isAlwaysPassive(entity)) {
                float passiveDamage = calculateDamage(serverPlayer);
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), passiveDamage);
                LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
                ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
                serverPlayer.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            // Mark provoked BEFORE damage so the mob can retaliate via setTarget()
            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid());
            }

            float damage = calculateDamage(serverPlayer);
            boolean damaged = livingTarget.damage(
                    serverPlayer.getEntityWorld(),
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    damage);

            if (damaged) {
                world.playSound(null,
                        serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                        SoundCategory.PLAYERS, 0.65f, 1.0f);
            }

            LAST_HIT_TICK.put(serverPlayer.getUuid(), (long) serverPlayer.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(serverPlayer, true);
            serverPlayer.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isZombiePossessing(player)) return true;

            // ── Poison immunity ──────────────────────────────────────────────
            // If this damage is from the poison effect, cancel it.
            // We also clear the poison effect itself so it doesn't keep ticking.
            if (source.equals(player.getDamageSources().magic())) {
                // Check if the player currently has poison — if so, this is likely
                // a poison tick; clear it and cancel the damage.
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            // ── Instant Harming → heals instead ─────────────────────────────
            // Instant Damage (harming potion) should heal the zombie player.
            // The damage source for instant damage is also "magic".
            // We detect this by checking if the entity has a INSTANT_DAMAGE effect
            // that just applied. Since we can't distinguish poison tick vs harming
            // in ALLOW_DAMAGE cleanly, we handle harming in the tick loop instead.

            Entity attacker = source.getAttacker();
            // Slimes cancel damage entirely in SamuelRequiemMod — don't play hurt sound for them
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_ZOMBIE_HURT, 1.0f);

            if (attacker == null) return true;

            if (!isZombieSubtype(attacker)) {
                ZombieTargetingState.markProvoked(attacker.getUuid(), player.getUuid());
            }

            rallyNearbyZombies(player, attacker);
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && isZombiePossessing(player)) {
                player.getEntityWorld().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            if (entity instanceof MobEntity) {
                ZombieTargetingState.clearProvoked(entity.getUuid());
            }
        });

        // ── Villager zombification (vanilla-style, intercept killing blow) ────
        // We hook ALLOW_DAMAGE on the villager itself. When our zombie player
        // delivers a killing blow, we cancel the damage, discard the villager
        // silently, and spawn a zombie villager in its place — no death animation.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            // Only intercept villager death
            if (!(entity instanceof VillagerEntity villager)) return true;
            if (!(entity.getEntityWorld() instanceof ServerWorld serverWorld)) return true;

            // Only when our zombie player is the killer
            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity zombiePlayer)) return true;
            if (!isZombiePossessing(zombiePlayer)) return true;

            // Only intercept the killing blow (damage >= remaining health)
            if (amount < villager.getHealth()) return true;

            // Difficulty-based conversion chance: Easy=0%, Normal=50%, Hard=100%
            float chance = switch (serverWorld.getDifficulty()) {
                case EASY   -> 0.0f;
                case NORMAL -> 0.5f;
                case HARD   -> 1.0f;
                default     -> 0.0f;
            };

            if (chance <= 0.0f) return true; // no conversion — let it die normally
            if (chance < 1.0f && serverWorld.getRandom().nextFloat() >= chance) return true;

            // --- Convert ---
            // Cancel the killing damage so the villager doesn't die visually
            // then discard it and replace with a zombie villager.

            ZombieVillagerEntity zombieVillager =
                    EntityType.ZOMBIE_VILLAGER.create(serverWorld, SpawnReason.CONVERSION);
            if (zombieVillager == null) return true; // fallback: die normally

            zombieVillager.refreshPositionAndAngles(
                    villager.getX(), villager.getY(), villager.getZ(),
                    villager.getYaw(), villager.getPitch());

            // Copy profession/level and preserve baby state
            zombieVillager.setVillagerData(villager.getVillagerData());

            // Only a baby villager should produce a baby zombie villager
            zombieVillager.setBaby(villager.isBaby());

            zombieVillager.initialize(
                    serverWorld,
                    serverWorld.getLocalDifficulty(zombieVillager.getBlockPos()),
                    SpawnReason.CONVERSION,
                    null);

            // initialize() may reset baby — re-apply after
            zombieVillager.setBaby(villager.isBaby());

            zombieVillager.setPersistent();

            // Remove the villager silently before spawning replacement
            villager.discard();

            serverWorld.spawnEntityAndPassengers(zombieVillager);

            serverWorld.playSound(null,
                    villager.getX(), villager.getY(), villager.getZ(),
                    SoundEvents.ENTITY_ZOMBIE_INFECT,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);

            // Cancel the damage — villager is already discarded
            return false;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isZombiePossessing(player)) {
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
        handleWaterConversion(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        repelVillagers(player);
        aggroIronGolems(player);
        aggroSnowGolems(player);
        tickArmsRaised(player);
        net.sam.samrequiemmod.possession.drowned.DrownedTridentManager.removeTrident(player);
        net.sam.samrequiemmod.possession.drowned.DrownedTridentManager.clearPlayer(player.getUuid());
    }

    // ── Damage calculation ───────────────────────────────────────────────────

    private static float calculateDamage(ServerPlayerEntity player) {
        double playerAttackDamage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        boolean holdingWeapon = playerAttackDamage > 1.5;
        if (holdingWeapon) {
            return (float) playerAttackDamage;
        } else {
            return getZombieBaseDamage(player.getEntityWorld().getDifficulty());
        }
    }

    private static float getZombieBaseDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 2.0f;
            case NORMAL -> 3.0f;
            case HARD   -> 4.5f;
            default     -> 3.0f;
        };
    }

    // ── Poison immunity ──────────────────────────────────────────────────────

    /**
     * Remove poison the moment it is applied. Zombies are immune to poison.
     */
    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) {
            player.removeStatusEffect(StatusEffects.POISON);
        }
    }

    /**
     * Instant Damage (Harming) heals zombies instead of hurting them.
     * We detect the INSTANT_DAMAGE effect and convert it to healing, then remove it.
     */
    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;

        // Heal amount mirrors vanilla: 6 * 2^amplifier hp (same as Instant Health for players)
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    // ── Rally nearby zombies when player is hit ──────────────────────────────

    private static void rallyNearbyZombies(ServerPlayerEntity player, Entity threat) {
        if (!(threat instanceof LivingEntity livingThreat)) return;
        Box box = player.getBoundingBox().expand(40.0);
        List<ZombieEntity> nearbyZombies = player.getEntityWorld().getEntitiesByClass(
                ZombieEntity.class, box, z -> z.isAlive());
        for (ZombieEntity zombie : nearbyZombies) {
            zombie.setTarget(livingThreat);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public static boolean isZombieSubtype(Entity entity) {
        return entity instanceof ZombieEntity;
    }

    /**
     * Returns true for mob types that should ALWAYS be passive toward any
     * possessed player, even when hit — they never retaliate.
     * Covers:
     *  - All ZombieEntity subclasses (zombie, husk, drowned, zombie villager, etc.)
     *  - All SlimeEntity subclasses (slime, magma cube)
     */
    public static boolean isAlwaysPassive(Entity entity) {
        return entity instanceof ZombieEntity
                || entity instanceof net.minecraft.entity.mob.SlimeEntity;
    }

    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    public static boolean isArmsRaised(UUID playerUuid) {
        return LAST_HIT_TICK.containsKey(playerUuid);
    }

    public static boolean isZombiePossessing(PlayerEntity player) {
        // Only true for ADULT zombie possession — baby zombies are handled by BabyZombiePossessionController
        return PossessionManager.getPossessedType(player) == EntityType.ZOMBIE
                && !BabyZombieState.isServerBaby(player);
    }

    public static boolean isZombieFood(ItemStack stack) {
        return stack.isOf(Items.ROTTEN_FLESH)
                || stack.isOf(Items.BEEF)
                || stack.isOf(Items.CHICKEN)
                || stack.isOf(Items.MUTTON)
                || stack.isOf(Items.PORKCHOP)
                || stack.isOf(Items.RABBIT);
    }

    public static float getZombieFoodHealing(ItemStack stack) {
        if (stack.isOf(Items.ROTTEN_FLESH)) return 3.0f;
        if (stack.isOf(Items.BEEF))         return 4.0f;
        if (stack.isOf(Items.CHICKEN))      return 3.0f;
        if (stack.isOf(Items.MUTTON))       return 4.0f;
        if (stack.isOf(Items.PORKCHOP))     return 4.0f;
        if (stack.isOf(Items.RABBIT))       return 3.0f;
        return 0.0f;
    }

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

    /**
     * Tracks time underwater. At 15s starts shaking (nausea effect),
     * at 30s converts the zombie player to a drowned player.
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

        // 15 seconds (300 ticks) — start shaking
        if (ticks == 300) {
            net.sam.samrequiemmod.possession.WaterShakeNetworking.broadcast(player, true);
        }

        // 30 seconds (600 ticks) — convert to drowned
        if (ticks >= 600) {
            net.sam.samrequiemmod.possession.WaterConversionTracker.resetSubmerged(player.getUuid());

            // Play conversion sound
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED,
                    net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 1.0f);

            // Stop shaking
            net.sam.samrequiemmod.possession.WaterShakeNetworking.broadcast(player, false);

            boolean isBaby = BabyZombieState.isServerBaby(player);

            // Switch possession to drowned — set baby state BEFORE startPossession
            // so PossessionEffects picks up the correct baby drowned profile
            net.sam.samrequiemmod.possession.PossessionManager.clearPossession(player);
            if (isBaby) {
                net.sam.samrequiemmod.possession.drowned.BabyDrownedState.setServerBaby(player.getUuid(), true);
            }
            net.sam.samrequiemmod.possession.PossessionManager.startPossession(
                    player, net.minecraft.entity.EntityType.DROWNED, player.getHealth());
            if (isBaby) {
                net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking.broadcast(player, true);
            }
        }
    }

    private static void preventSwimming(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.NoSwimPossessionHelper.disableSwimmingPose(player);
    }

    private static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) {
            player.setAir(player.getMaxAir());
        }
    }

    private static void handleSunlightBurn(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getEntityWorld().isDay()) return;
        if (player.isTouchingWaterOrRain()) return;
        BlockPos eyePos = BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ());
        if (!player.getEntityWorld().isSkyVisible(eyePos)) return;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isEmpty()) {
            player.setOnFireFor(8);
        }
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
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
}





