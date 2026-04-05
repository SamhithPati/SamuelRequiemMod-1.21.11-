package net.sam.samrequiemmod.possession.piglin;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.WaterShakeNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PiglinPossessionController {

    private PiglinPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();
    /** Stores armor copied from the possessed piglin mob, to remove on unpossess. */
    private static final Map<UUID, ItemStack[]> COPIED_ARMOR = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100; // 5 seconds

    public static boolean isPiglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PIGLIN
                && !BabyPiglinState.isServerBaby(player);
    }

    public static boolean isAnyPiglinPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE;
    }

    /** Piglins and piglin brutes are always passive — never retaliate even when hit. */
    public static boolean isPiglinAlly(Entity entity) {
        return entity instanceof PiglinEntity || entity instanceof PiglinBruteEntity;
    }

    public static void register() {

        // ── Attack: custom damage, arms raised, rally ─────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isPiglinPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            // Piglins/brutes are always passive — deal damage but never provoke
            if (isPiglinAlly(entity)) {
                float damage = calculateDamage(sp);
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
                LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
                ZombieAttackSyncNetworking.broadcastZombieAttacking(sp, true);
                sp.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            // Mark provoked before damage
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());

            float damage = calculateDamage(sp);
            livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);

            LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(sp, true);
            sp.swingHand(hand, true);

            // Rally nearby piglins and brutes
            if (entity instanceof LivingEntity lt && !isPiglinAlly(entity))
                rallyNearbyPiglins(sp, lt);

            return ActionResult.SUCCESS;
        });

        // ── Hurt: sound, rally, provoke ───────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isPiglinPossessing(player)) return true;

            Entity attacker = source.getAttacker();
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_PIGLIN_HURT, 1.0f);

            ensureArrows(player);

            if (attacker == null || isPiglinAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                rallyNearbyPiglins(player, livingAttacker);
            }
            return true;
        });

        // ── Death sound ───────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isPiglinPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isPiglinPossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }

        lockHunger(player);
        preventNaturalHealing(player);
        ensurePiglinItems(player);
        tickArmsRaised(player);
        handleOverworldConversion(player);

        // Ambient sound
        if (player.age % 120 == 0 && player.getRandom().nextFloat() < 0.35f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Crossbow reloading sound
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty() && mainHand.isOf(Items.CROSSBOW)) {
            if (player.isUsingItem() && !net.minecraft.item.CrossbowItem.isCharged(mainHand)) {
                if (player.age % 10 == 0) {
                    player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE,
                            SoundCategory.PLAYERS, 0.8f, 1.0f);
                }
            }
        }

        // Re-rally allies every 20 ticks
        if (player.age % 20 == 0) persistRally(player);
    }

    // ── Overworld conversion (piglin → zombified piglin) ──────────────────────
    private static void handleOverworldConversion(ServerPlayerEntity player) {
        boolean inOverworld = player.getEntityWorld().getRegistryKey() == World.OVERWORLD;
        if (!inOverworld) {
            int prev = OverworldConversionTracker.getTicks(player.getUuid());
            OverworldConversionTracker.reset(player.getUuid());
            if (prev > 0) WaterShakeNetworking.broadcast(player, false);
            return;
        }

        OverworldConversionTracker.tick(player.getUuid());
        int ticks = OverworldConversionTracker.getTicks(player.getUuid());

        // Start shaking immediately in the overworld
        if (ticks == 1) {
            WaterShakeNetworking.broadcast(player, true);
        }

        // 20 seconds (400 ticks) → convert to zombified piglin
        if (ticks >= 400) {
            OverworldConversionTracker.reset(player.getUuid());
            WaterShakeNetworking.broadcast(player, false);

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_CONVERTED_TO_ZOMBIFIED,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);

            float health = player.getHealth();
            PossessionManager.clearPossession(player);
            PossessionManager.startPossession(player, EntityType.ZOMBIFIED_PIGLIN, health);
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────
    static float calculateDamage(ServerPlayerEntity player) {
        double attr = player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        ItemStack held = player.getMainHandStack();
        // If holding a golden sword, use custom piglin damage values
        if (held.isOf(Items.GOLDEN_SWORD)) {
            return getPiglinSwordDamage(player.getEntityWorld().getDifficulty());
        }
        // If holding a better weapon (netherite, diamond, etc.), use vanilla weapon damage
        if (attr > 2.0) return (float) attr;
        // Unarmed: 1 heart = 2 HP
        return 2.0f;
    }

    private static float getPiglinSwordDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY   -> 3.0f;   // 1.5 hearts
            case NORMAL -> 5.0f;   // 2.5 hearts
            case HARD   -> 7.5f;   // 3.75 hearts
            default     -> 5.0f;
        };
    }

    // ── Rally nearby piglins and brutes ───────────────────────────────────────
    static void rallyNearbyPiglins(ServerPlayerEntity player, LivingEntity threat) {
        Box box = player.getBoundingBox().expand(40.0);
        for (MobEntity mob : player.getEntityWorld().getEntitiesByClass(
                MobEntity.class, box, m -> m.isAlive() && isPiglinAlly(m))) {
            // Only rally adult piglins and brutes
            if (mob instanceof PiglinEntity piglin && piglin.isBaby()) continue;
            // Piglins use Brain AI — setTarget() gets overridden immediately.
            // Set brain memories directly so the piglin's AI picks up the target.
            if (mob instanceof net.minecraft.entity.mob.AbstractPiglinEntity abstractPiglin) {
                abstractPiglin.getBrain().remember(
                        net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET, threat);
                abstractPiglin.getBrain().remember(
                        net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT, threat.getUuid(),
                        600); // 30 seconds anger duration
            }
            mob.setTarget(threat);
        }
    }

    // ── Arms raised ───────────────────────────────────────────────────────────
    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    // ── Persistent rally ──────────────────────────────────────────────────────
    private static void persistRally(ServerPlayerEntity player) {
        UUID attackerUuid = LAST_ATTACKER.get(player.getUuid());
        if (attackerUuid == null) return;
        Entity e = player.getEntityWorld().getEntity(attackerUuid);
        if (!(e instanceof LivingEntity attacker) || !attacker.isAlive()) {
            LAST_ATTACKER.remove(player.getUuid());
            return;
        }
        Box box = player.getBoundingBox().expand(40.0);
        for (MobEntity ally : player.getEntityWorld()
                .getEntitiesByClass(MobEntity.class, box, m -> isPiglinAlly(m) && m.isAlive())) {
            if (ally instanceof PiglinEntity piglin && piglin.isBaby()) continue;
            if (ally.getTarget() == null || !ally.getTarget().isAlive()) {
                if (ally instanceof net.minecraft.entity.mob.AbstractPiglinEntity abstractPiglin) {
                    abstractPiglin.getBrain().remember(
                            net.minecraft.entity.ai.brain.MemoryModuleType.ATTACK_TARGET, attacker);
                    abstractPiglin.getBrain().remember(
                            net.minecraft.entity.ai.brain.MemoryModuleType.ANGRY_AT, attacker.getUuid(),
                            600);
                }
                ally.setTarget(attacker);
            }
        }
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensurePiglinItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureArrows(player);
            ensureUnbreakable(player, Items.GOLDEN_SWORD);
            ensureUnbreakable(player, Items.CROSSBOW);
            return;
        }

        // Golden sword (unbreakable)
        ItemStack sword = new ItemStack(Items.GOLDEN_SWORD);
        sword.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);

        // Crossbow (unbreakable) with Infinity
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        try {
            crossbow.addEnchantment(
                    player.getEntityWorld().getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.INFINITY),
                    1);
        } catch (Exception ignored) {}
        crossbow.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);

        // Arrows
        ItemStack arrows = new ItemStack(Items.ARROW, 64);

        giveToSlot(player, sword, 0);
        giveToSlot(player, crossbow, 1);
        player.getInventory().insertStack(arrows);

        ITEMS_GIVEN.add(player.getUuid());
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    static void ensureArrows(ServerPlayerEntity player) {
        boolean has = false;
        for (int i = 0; i < player.getInventory().size(); i++)
            if (player.getInventory().getStack(i).isOf(Items.ARROW)) { has = true; break; }
        if (!has) player.getInventory().offerOrDrop(new ItemStack(Items.ARROW, 64));
    }

    private static void ensureUnbreakable(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item) && !s.contains(DataComponentTypes.UNBREAKABLE))
                s.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        }
    }

    // ── Hunger ────────────────────────────────────────────────────────────────
    static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    static void preventNaturalHealing(ServerPlayerEntity player) {
        // HungerManagerMixin already blocks passive healing for possessed players.
        // Do not touch timeUntilRegen here: vanilla also uses it for hurt i-frames.
    }

    // ── Food helpers ──────────────────────────────────────────────────────────
    public static boolean isPiglinFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return getPiglinFoodHealing(stack) > 0;
    }

    public static float getPiglinFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item == Items.PORKCHOP)        return 3.0f;
        if (item == Items.COOKED_PORKCHOP) return 5.0f;
        if (item == Items.CARROT)          return 3.0f;
        if (item == Items.GOLDEN_CARROT)   return 6.0f;
        return 0;
    }

    /** Warped/crimson fungus are instant-heal (no FoodComponent). */
    public static boolean isPiglinInstantFood(ItemStack stack) {
        return stack.isOf(Items.WARPED_FUNGUS) || stack.isOf(Items.CRIMSON_FUNGUS);
    }

    public static float getPiglinInstantFoodHealing(ItemStack stack) {
        if (stack.isOf(Items.WARPED_FUNGUS))  return 3.0f;
        if (stack.isOf(Items.CRIMSON_FUNGUS)) return 3.0f;
        return 0;
    }

    // ── Armor copying ─────────────────────────────────────────────────────────
    public static void copyArmorFromMob(ServerPlayerEntity player, LivingEntity mob) {
        ItemStack[] saved = new ItemStack[4];
        EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        for (int i = 0; i < armorSlots.length; i++) {
            ItemStack mobArmor = mob.getEquippedStack(armorSlots[i]);
            saved[i] = player.getEquippedStack(armorSlots[i]).copy();
            if (!mobArmor.isEmpty()) {
                ItemStack armorCopy = mobArmor.copy();
                armorCopy.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
                player.equipStack(armorSlots[i], armorCopy);
            }
        }
        COPIED_ARMOR.put(player.getUuid(), saved);
    }

    // ── Unpossess ─────────────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        LAST_HIT_TICK.remove(player.getUuid());
        LAST_ATTACKER.remove(player.getUuid());
        OverworldConversionTracker.reset(player.getUuid());
        removeItemType(player, Items.GOLDEN_SWORD);
        removeItemType(player, Items.CROSSBOW);
        removeItemType(player, Items.ARROW);
        // Restore original armor
        ItemStack[] saved = COPIED_ARMOR.remove(player.getUuid());
        if (saved != null) {
            EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
            for (int i = 0; i < armorSlots.length; i++) {
                // Remove the copied armor piece
                ItemStack current = player.getEquippedStack(armorSlots[i]);
                if (!current.isEmpty()) player.equipStack(armorSlots[i], ItemStack.EMPTY);
                // Restore the original armor
                if (saved[i] != null && !saved[i].isEmpty())
                    player.equipStack(armorSlots[i], saved[i]);
            }
        }
        ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        WaterShakeNetworking.broadcast(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        ITEMS_GIVEN.remove(uuid);
        LAST_HIT_TICK.remove(uuid);
        LAST_ATTACKER.remove(uuid);
        COPIED_ARMOR.remove(uuid);
        OverworldConversionTracker.reset(uuid);
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--)
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
    }
}








