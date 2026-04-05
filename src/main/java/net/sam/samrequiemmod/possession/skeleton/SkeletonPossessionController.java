package net.sam.samrequiemmod.possession.skeleton;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Skeleton, Bogged, Stray, and Parched possession.
 * - 10 hearts, bow + arrows that never run out
 * - Heal from bones, immune to poison and harming
 * - Can't swim, can't drown, burn in daylight
 * - Iron golems attack
 * - Bogged shoots poison arrows, Stray shoots slowness arrows
 */
public final class SkeletonPossessionController {

    private SkeletonPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();

    public static boolean isSkeletonPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SKELETON;
    }

    public static boolean isBoggedPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.BOGGED;
    }

    public static boolean isStrayPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.STRAY;
    }

    public static boolean isParchedPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PARCHED;
    }

    public static boolean isAnySkeletonPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.SKELETON
                || type == EntityType.BOGGED
                || type == EntityType.STRAY
                || type == EntityType.PARCHED;
    }

    public static void register() {

        // ── Attack: mark provoked ──────────────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isAnySkeletonPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            return ActionResult.PASS;
        });

        // ── ALLOW_DAMAGE: hurt sound, provoke attacker, poison/harming immunity ──
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnySkeletonPossessing(player)) return true;

            // Poison immunity (from magic damage source when poisoned)
            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            // Play skeleton hurt sound
            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, getHurtSound(player), 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker == null) return true;
            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            ensureArrows(player);
            return true;
        });

        // ── Death sound ────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnySkeletonPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getDeathSound(player), SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isAnySkeletonPossessing(player)) return;

        lockHunger(player);
        ensureSkeletonItems(player);
        handleSunlightBurn(player);
        handleAmbientSound(player);
        preventSwimming(player);
        preventDrowning(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        aggroIronGolems(player);
        aggroWolves(player);
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensureSkeletonItems(ServerPlayerEntity player) {
        normalizeAmmo(player);
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureArrows(player);
            ensureBowUnbreakable(player);
            return;
        }

        // Unbreakable bow with Infinity
        ItemStack bow = new ItemStack(Items.BOW);
        try {
            bow.addEnchantment(
                    player.getEntityWorld().getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.INFINITY),
                    1);
        } catch (Exception ignored) {}
        bow.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);

        // Arrows: give tipped arrows based on skeleton type
        ItemStack arrows = getArrowsForType(player);

        giveToSlot(player, bow, 0);
        player.getInventory().insertStack(arrows);

        ITEMS_GIVEN.add(player.getUuid());
    }

    private static ItemStack getArrowsForType(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.BOGGED) {
            // Poison-tipped arrows
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.POISON));
            return arrows;
        } else if (type == EntityType.PARCHED) {
            // Weakness-tipped arrows
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.WEAKNESS));
            return arrows;
        } else if (type == EntityType.STRAY) {
            // Slowness-tipped arrows
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.SLOWNESS));
            return arrows;
        }
        return new ItemStack(Items.ARROW, 64);
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    private static void ensureBowUnbreakable(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(Items.BOW) && !s.contains(DataComponentTypes.UNBREAKABLE))
                s.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        }
    }

    static void ensureArrows(ServerPlayerEntity player) {
        normalizeAmmo(player);
        EntityType<?> type = PossessionManager.getPossessedType(player);
        boolean hasTipped = false;
        boolean hasNormal = false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(Items.TIPPED_ARROW)) hasTipped = true;
            if (s.isOf(Items.ARROW)) hasNormal = true;
        }

        if (type == EntityType.BOGGED && !hasTipped) {
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.POISON));
            player.getInventory().offerOrDrop(arrows);
        } else if (type == EntityType.PARCHED && !hasTipped) {
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.WEAKNESS));
            player.getInventory().offerOrDrop(arrows);
        } else if (type == EntityType.STRAY && !hasTipped) {
            ItemStack arrows = new ItemStack(Items.TIPPED_ARROW, 64);
            arrows.set(DataComponentTypes.POTION_CONTENTS,
                    new PotionContentsComponent(Potions.SLOWNESS));
            player.getInventory().offerOrDrop(arrows);
        } else if ((type == EntityType.SKELETON || type == EntityType.PARCHED) && !hasNormal) {
            player.getInventory().offerOrDrop(new ItemStack(Items.ARROW, 64));
        }
    }

    private static void normalizeAmmo(ServerPlayerEntity player) {
        if (!isParchedPossessing(player)) return;

        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.isOf(Items.ARROW)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
                continue;
            }
            if (stack.isOf(Items.TIPPED_ARROW) && !isWeaknessArrow(stack)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isWeaknessArrow(ItemStack stack) {
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return contents != null && contents.matches(Potions.WEAKNESS);
    }

    // ── Hunger lock ───────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Sunlight burn ─────────────────────────────────────────────────────────
    private static void handleSunlightBurn(ServerPlayerEntity player) {
        if (isParchedPossessing(player)) return;
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

    // ── Ambient sound ─────────────────────────────────────────────────────────
    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                getAmbientSound(player), SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    public static net.minecraft.sound.SoundEvent getAmbientSound(PlayerEntity player) {
        return isParchedPossessing(player)
                ? SoundEvents.ENTITY_PARCHED_AMBIENT
                : SoundEvents.ENTITY_SKELETON_AMBIENT;
    }

    public static net.minecraft.sound.SoundEvent getHurtSound(PlayerEntity player) {
        return isParchedPossessing(player)
                ? SoundEvents.ENTITY_PARCHED_HURT
                : SoundEvents.ENTITY_SKELETON_HURT;
    }

    public static net.minecraft.sound.SoundEvent getDeathSound(PlayerEntity player) {
        return isParchedPossessing(player)
                ? SoundEvents.ENTITY_PARCHED_DEATH
                : SoundEvents.ENTITY_SKELETON_DEATH;
    }

    // ── Prevent swimming ──────────────────────────────────────────────────────
    private static void preventSwimming(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.NoSwimPossessionHelper.disableSwimmingPose(player);
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

    // ── Instant Harming → heals ───────────────────────────────────────────────
    private static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    // ── Iron golem aggro ──────────────────────────────────────────────────────
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

    private static void aggroWolves(ServerPlayerEntity player) {
        if (!isParchedPossessing(player) || player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        List<WolfEntity> wolves = player.getEntityWorld().getEntitiesByClass(
                WolfEntity.class, box, wolf -> wolf.isAlive());
        for (WolfEntity wolf : wolves) {
            if (wolf.squaredDistanceTo(player) > 24.0 * 24.0) continue;
            wolf.setTarget(player);
            wolf.chooseRandomAngerTime();
            wolf.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    // ── Food helpers (bones) ──────────────────────────────────────────────────
    public static boolean isSkeletonFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isOf(Items.BONE);
    }

    public static float getSkeletonFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.isOf(Items.BONE)) return 3.0f;
        return 0;
    }

    // ── Unpossess cleanup ─────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        removeItemType(player, Items.BOW);
        removeItemType(player, Items.ARROW);
        removeItemType(player, Items.TIPPED_ARROW);
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
        }
    }

    public static void onUnpossessUuid(UUID playerUuid) {
        ITEMS_GIVEN.remove(playerUuid);
    }
}








