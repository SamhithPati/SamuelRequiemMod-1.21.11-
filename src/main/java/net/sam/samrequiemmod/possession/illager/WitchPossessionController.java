package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WitchPossessionController {

    private WitchPossessionController() {}

    // ── Drinking state ─────────────────────────────────────────────────────────
    /** Tick at which current drink animation finishes. */
    private static final Map<UUID, Long> DRINKING_UNTIL = new ConcurrentHashMap<>();
    /** What effect to apply when the drink animation completes. */
    private static final Map<UUID, Integer> DRINKING_TYPE = new ConcurrentHashMap<>();

    /** Cooldown for potion throws (1-second = 20 ticks). */
    private static final Map<UUID, Long> LAST_THROW_TICK = new ConcurrentHashMap<>();

    /** Tracks consecutive ticks the player has been submerged. */
    private static final Map<UUID, Integer> UNDERWATER_TICKS = new ConcurrentHashMap<>();

    /** Cooldown for instant-healing self-drink (3 seconds). */
    private static final Map<UUID, Long> LAST_HEAL_DRINK_TICK = new ConcurrentHashMap<>();

    /** Last entity that attacked the witch player — for persistent rally. */
    private static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    // Drink-type constants
    private static final int DRINK_REGEN = 1;
    private static final int DRINK_FIRE_RES = 2;
    private static final int DRINK_WATER_BREATH = 3;
    private static final int DRINK_HEALING = 4;

    /** Animation length in ticks (1 second). */
    private static final int DRINK_DURATION = 20;

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isWitchPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WITCH;
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // ── Left-click: always cancel melee ────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isWitchPossessing(sp)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // ── ALLOW_DAMAGE: hurt sound, auto-regen drink, rally allies ───────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWitchPossessing(player)) return true;

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            // Start regen drink if not already drinking and doesn't have regen
            if (!DRINKING_UNTIL.containsKey(player.getUuid())
                    && !player.hasStatusEffect(StatusEffects.REGENERATION)) {
                startDrinking(player, DRINK_REGEN);
            }

            // Rally nearby illagers against attacker
            Entity attacker = source.getAttacker();
            if (attacker != null && !PillagerPossessionController.isIllagerAlly(attacker)) {
                LAST_ATTACKER.put(player.getUuid(), attacker.getUuid());
                if (attacker instanceof MobEntity mob)
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                if (attacker instanceof LivingEntity livingAttacker) {
                    Box box = player.getBoundingBox().expand(40.0);
                    for (MobEntity mob : player.getServerWorld()
                            .getEntitiesByClass(MobEntity.class, box,
                                    m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
                        mob.setTarget(livingAttacker);
                    }
                }
            }
            return true;
        });

        // ── Death sound ────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWitchPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isWitchPossessing(player)) return;

        lockHunger(player);

        // Handle drinking animation countdown
        Long drinkUntil = DRINKING_UNTIL.get(player.getUuid());
        if (drinkUntil != null) {
            if ((long) player.age >= drinkUntil) {
                completeDrink(player);
            }
            // Still drinking — skip auto-drink checks
        } else {
            // ── Auto-drink checks (priority order) ─────────────────────────────

            // 1. Instant healing when below 6 hearts (<12 HP)
            if (player.getHealth() < 12.0f) {
                long lastHeal = LAST_HEAL_DRINK_TICK.getOrDefault(player.getUuid(), -1000L);
                if ((long) player.age - lastHeal >= 60) { // 3-second cooldown
                    startDrinking(player, DRINK_HEALING);
                }
            }
            // 2. Fire resistance when on fire or in lava
            else if ((player.isOnFire() || player.isInLava())
                    && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                startDrinking(player, DRINK_FIRE_RES);
            }
            // 3. Water breathing when underwater 5+ seconds (100 ticks)
            else if (player.isSubmergedInWater()) {
                int ticks = UNDERWATER_TICKS.getOrDefault(player.getUuid(), 0) + 1;
                UNDERWATER_TICKS.put(player.getUuid(), ticks);
                if (ticks >= 100 && !player.hasStatusEffect(StatusEffects.WATER_BREATHING)) {
                    startDrinking(player, DRINK_WATER_BREATH);
                    UNDERWATER_TICKS.remove(player.getUuid());
                }
            }
        }

        // Reset underwater counter when not submerged
        if (!player.isSubmergedInWater()) {
            UNDERWATER_TICKS.remove(player.getUuid());
        }

        // Ambient sound
        if (player.age % 100 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Scare villagers
        if (player.age % 10 == 0) {
            PillagerPossessionController.scarVillagersPublic(player);
        }

        // Persistent rally
        if (player.age % 20 == 0) {
            persistRally(player);
        }
    }

    // ── Drinking helpers ───────────────────────────────────────────────────────

    private static void startDrinking(ServerPlayerEntity player, int drinkType) {
        DRINKING_UNTIL.put(player.getUuid(), (long) player.age + DRINK_DURATION);
        DRINKING_TYPE.put(player.getUuid(), drinkType);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1.0f, 1.0f);
        WitchNetworking.broadcastDrinking(player, true);
    }

    private static void completeDrink(ServerPlayerEntity player) {
        int type = DRINKING_TYPE.getOrDefault(player.getUuid(), 0);
        DRINKING_UNTIL.remove(player.getUuid());
        DRINKING_TYPE.remove(player.getUuid());
        WitchNetworking.broadcastDrinking(player, false);

        switch (type) {
            case DRINK_REGEN -> player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.REGENERATION, 1800, 1, false, true));
            case DRINK_FIRE_RES -> player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3600, 0, false, true));
            case DRINK_WATER_BREATH -> player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.WATER_BREATHING, 3600, 0, false, true));
            case DRINK_HEALING -> {
                player.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 0, false, true));
                LAST_HEAL_DRINK_TICK.put(player.getUuid(), (long) player.age);
            }
        }
    }

    // ── Potion throwing (called from networking) ───────────────────────────────

    public static void handlePotionThrow(ServerPlayerEntity player, UUID targetUuid) {
        if (!isWitchPossessing(player)) return;

        // Check 1-second cooldown (20 ticks)
        long lastThrow = LAST_THROW_TICK.getOrDefault(player.getUuid(), -1000L);
        if ((long) player.age - lastThrow < 20) return;

        if (targetUuid == null) return;
        Entity targetEntity = player.getServerWorld().getEntity(targetUuid);
        if (!(targetEntity instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        double dist = player.distanceTo(target);
        if (dist > 12.0) return;

        LAST_THROW_TICK.put(player.getUuid(), (long) player.age);

        boolean isAlly = PillagerPossessionController.isIllagerAlly(target);

        if (isAlly) {
            // Throw regen splash at allies (illagers & ravagers)
            target.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.REGENERATION, 600, 0, false, true));
            throwVisualPotion(player, target, Potions.REGENERATION);
        } else {
            // Choose offensive potion based on target health and distance
            if (target.getHealth() < 8.0f) {
                // Target below 4 hearts → Harming I
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 0, false, true));
                throwVisualPotion(player, target, Potions.HARMING);
            } else if (dist < 3.0) {
                // Under 3 blocks → Weakness (90 seconds)
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.WEAKNESS, 1800, 0, false, true));
                throwVisualPotion(player, target, Potions.WEAKNESS);
            } else if (dist < 8.0) {
                // Under 8 blocks → Poison (90 seconds)
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.POISON, 1800, 0, false, true));
                throwVisualPotion(player, target, Potions.POISON);
            } else {
                // 8+ blocks → Slowness (1 minute)
                target.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 0, false, true));
                throwVisualPotion(player, target, Potions.SLOWNESS);
            }
        }

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void throwVisualPotion(ServerPlayerEntity player, LivingEntity target,
                                          net.minecraft.registry.entry.RegistryEntry<net.minecraft.potion.Potion> potionType) {
        ServerWorld world = player.getServerWorld();
        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(potionType));

        PotionEntity potionEntity = new PotionEntity(world, player);
        potionEntity.setItem(potionStack);

        double dx = target.getX() - player.getX();
        double dy = target.getEyeY() - player.getEyeY();
        double dz = target.getZ() - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        potionEntity.setVelocity(dx, dy + horizontal * 0.2, dz, 0.75f, 1.0f);
        world.spawnEntity(potionEntity);
    }

    // ── Persistent rally ───────────────────────────────────────────────────────

    private static void persistRally(ServerPlayerEntity player) {
        UUID attackerUuid = LAST_ATTACKER.get(player.getUuid());
        if (attackerUuid == null) return;
        Entity e = player.getServerWorld().getEntity(attackerUuid);
        if (!(e instanceof LivingEntity attacker) || !attacker.isAlive()) {
            LAST_ATTACKER.remove(player.getUuid());
            return;
        }
        Box box = player.getBoundingBox().expand(40.0);
        for (MobEntity ally : player.getServerWorld()
                .getEntitiesByClass(MobEntity.class, box,
                        m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
            if (ally.getTarget() == null || !ally.getTarget().isAlive())
                ally.setTarget(attacker);
        }
    }

    // ── Hunger lock ────────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Food helpers (used by ZombieFoodUseHandler) ────────────────────────────

    public static boolean isWitchFood(ItemStack stack) {
        return getWitchFoodHealing(stack) > 0;
    }

    public static float getWitchFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item == Items.CARROT)                    return 3;
        if (item == Items.GOLDEN_CARROT)             return 6;
        if (item == Items.MUSHROOM_STEW)             return 6;
        if (item == Items.POTATO)                    return 1;
        if (item == Items.BAKED_POTATO)              return 5;
        if (item == Items.GOLDEN_APPLE)              return 4;
        if (item == Items.ENCHANTED_GOLDEN_APPLE)    return 8;
        return 0;
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        DRINKING_UNTIL.remove(uuid);
        DRINKING_TYPE.remove(uuid);
        LAST_THROW_TICK.remove(uuid);
        UNDERWATER_TICKS.remove(uuid);
        LAST_HEAL_DRINK_TICK.remove(uuid);
        LAST_ATTACKER.remove(uuid);
        WitchNetworking.broadcastDrinking(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        DRINKING_UNTIL.remove(uuid);
        DRINKING_TYPE.remove(uuid);
        LAST_THROW_TICK.remove(uuid);
        UNDERWATER_TICKS.remove(uuid);
        LAST_HEAL_DRINK_TICK.remove(uuid);
        LAST_ATTACKER.remove(uuid);
    }
}
