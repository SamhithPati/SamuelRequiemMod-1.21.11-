package net.sam.samrequiemmod.possession.skeleton;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Wither Skeleton possession.
 * - 10 hearts, stone sword (unbreakable), custom difficulty-scaled damage
 * - Immune to poison, harming, wither, fire/lava
 * - Heal from bones, can't swim, can't drown
 * - Wither effect on melee hit
 * - Arms out when attacking (5-second timeout)
 * - Piglins, piglin brutes, and iron golems attack
 * - Can ride spiders/cave spiders
 */
public final class WitherSkeletonPossessionController {

    private WitherSkeletonPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100; // 5 seconds

    public static boolean isWitherSkeletonPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WITHER_SKELETON;
    }

    public static void register() {

        // ── Attack: custom damage, wither effect, arms raised ──────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isWitherSkeletonPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            // Mark provoked
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());

            // Calculate damage
            float damage = calculateDamage(sp);
            boolean damaged = livingTarget.damage(
                    sp.getDamageSources().playerAttack(sp), damage);

            if (damaged) {
                // Apply wither effect on hit (10 seconds)
                livingTarget.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER, 200, 0, false, true));

                world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 0.65f, 1.0f);
            }

            // Arms raised animation
            LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
            WitherSkeletonAttackNetworking.broadcastAttacking(sp, true);

            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── ALLOW_DAMAGE: hurt sound, immunities ──────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWitherSkeletonPossessing(player)) return true;

            // Fire/lava immunity (includes blaze fireball)
            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE)) {
                return false;
            }

            // Poison immunity
            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            // Play hurt sound
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker == null) return true;
            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            return true;
        });

        // ── Death sound ────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWitherSkeletonPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isWitherSkeletonPossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                WitherSkeletonAttackNetworking.broadcastAttacking(player, false);
            }
            return;
        }

        lockHunger(player);
        ensureWitherSkeletonItems(player);
        handleAmbientSound(player);
        preventSwimming(player);
        preventDrowning(player);
        handlePoisonImmunity(player);
        handleWitherImmunity(player);
        handleHarmingHeals(player);
        handleFireImmunity(player);
        aggroIronGolems(player);
        aggroPiglins(player);
        tickArmsRaised(player);
    }

    // ── Damage calculation ────────────────────────────────────────────────────
    private static float calculateDamage(ServerPlayerEntity player) {
        double playerAttackDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        boolean holdingWeapon = playerAttackDamage > 1.5;
        if (holdingWeapon) {
            // Weapon damage: base weapon damage + difficulty bonus
            // Stone sword base = 5 dmg. We want: Easy=5, Normal=8, Hard=12
            // So add difficulty bonus on top of weapon damage
            float difficultyBonus = getDifficultyWeaponBonus(player.getServerWorld().getDifficulty());
            return (float) playerAttackDamage + difficultyBonus;
        } else {
            return getWitherSkeletonBaseDamage(player.getServerWorld().getDifficulty());
        }
    }

    /** Unarmed damage: Easy=4, Normal=6, Hard=9 */
    private static float getWitherSkeletonBaseDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 4.0f;
            case NORMAL -> 6.0f;
            case HARD   -> 9.0f;
            default     -> 6.0f;
        };
    }

    /**
     * Bonus damage added on top of weapon base.
     * Stone sword base = 5. Target: Easy=5, Normal=8, Hard=12.
     * So bonus: Easy=0, Normal=3, Hard=7.
     */
    private static float getDifficultyWeaponBonus(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 0.0f;
            case NORMAL -> 3.0f;
            case HARD   -> 7.0f;
            default     -> 3.0f;
        };
    }

    // ── Arms raised animation ─────────────────────────────────────────────────
    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            WitherSkeletonAttackNetworking.broadcastAttacking(player, false);
        }
    }

    public static boolean isArmsRaised(UUID playerUuid) {
        return LAST_HIT_TICK.containsKey(playerUuid);
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensureWitherSkeletonItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureSword(player);
            return;
        }

        // Unbreakable stone sword
        ItemStack sword = new ItemStack(Items.STONE_SWORD);
        sword.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));

        giveToSlot(player, sword, 0);
        ITEMS_GIVEN.add(player.getUuid());
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    static void ensureSword(ServerPlayerEntity player) {
        boolean has = false;
        for (int i = 0; i < player.getInventory().size(); i++)
            if (player.getInventory().getStack(i).isOf(Items.STONE_SWORD)) { has = true; break; }
        if (!has) {
            ItemStack sword = new ItemStack(Items.STONE_SWORD);
            sword.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
            player.getInventory().offerOrDrop(sword);
        }
    }

    // ── Hunger lock ───────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Ambient sound ─────────────────────────────────────────────────────────
    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    // ── Prevent swimming ──────────────────────────────────────────────────────
    private static void preventSwimming(ServerPlayerEntity player) {
        if (!player.isTouchingWater()) return;
        if (player.isSwimming()) player.setSwimming(false);
        Vec3d vel = player.getVelocity();
        // Clamp upward velocity — wither skeletons cannot rise in water
        double vy = Math.min(vel.y, -0.04);
        player.setVelocity(vel.x * 0.5, vy, vel.z * 0.5);
        player.velocityModified = true;
    }

    // ── Prevent drowning ──────────────────────────────────────────────────────
    private static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) {
            player.setAir(player.getMaxAir());
        }
    }

    // ── Poison immunity ───────────────────────────────────────────────────────
    private static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON)) {
            player.removeStatusEffect(StatusEffects.POISON);
        }
    }

    // ── Wither immunity ───────────────────────────────────────────────────────
    private static void handleWitherImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.WITHER)) {
            player.removeStatusEffect(StatusEffects.WITHER);
        }
    }

    // ── Instant Harming → heals ───────────────────────────────────────────────
    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    // ── Fire/lava immunity ────────────────────────────────────────────────────
    private static void handleFireImmunity(ServerPlayerEntity player) {
        if (player.isOnFire()) {
            player.setOnFireFor(0);
            player.extinguish();
        }
    }

    // ── Iron golem aggro ──────────────────────────────────────────────────────
    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        List<IronGolemEntity> golems = player.getWorld().getEntitiesByClass(
                IronGolemEntity.class, box, golem -> golem.isAlive());
        for (IronGolemEntity golem : golems) {
            if (golem.squaredDistanceTo(player) <= 24.0 * 24.0) {
                golem.setTarget(player);
                golem.setAngryAt(player.getUuid());
            }
        }
    }

    // ── Piglin/brute aggro ────────────────────────────────────────────────────
    private static void aggroPiglins(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (MobEntity mob : player.getServerWorld()
                .getEntitiesByClass(MobEntity.class, box,
                        m -> (m instanceof PiglinEntity || m instanceof PiglinBruteEntity) && m.isAlive())) {
            if (mob.squaredDistanceTo(player) <= 24.0 * 24.0) {
                mob.setTarget(player);
            }
        }
    }

    // ── Food helpers (bones) ──────────────────────────────────────────────────
    public static boolean isWitherSkeletonFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isOf(Items.BONE);
    }

    public static float getWitherSkeletonFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.isOf(Items.BONE)) return 3.0f;
        return 0;
    }

    // ── Unpossess cleanup ─────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        LAST_HIT_TICK.remove(player.getUuid());
        removeItemType(player, Items.STONE_SWORD);
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
        }
    }

    public static void onUnpossessUuid(UUID playerUuid) {
        ITEMS_GIVEN.remove(playerUuid);
        LAST_HIT_TICK.remove(playerUuid);
    }
}
