package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PillagerPossessionController {

    private PillagerPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final java.util.Map<UUID, net.minecraft.item.ItemStack> GIVEN_BANNER =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Tracks the last entity that hit the pillager player, so allies can persistently target it. */
    private static final java.util.Map<UUID, UUID> LAST_ATTACKER =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Mobs that are always allied — never target the pillager player */
    public static boolean isIllagerAlly(Entity e) {
        return e instanceof PillagerEntity
                || e instanceof VindicatorEntity
                || e instanceof EvokerEntity
                || e instanceof IllusionerEntity
                || e instanceof WitchEntity
                || e instanceof RavagerEntity
                || e instanceof VexEntity;
    }

    /** Mobs that rally to defend the pillager player */
    public static boolean isRallyMob(Entity e) {
        return e instanceof PillagerEntity
                || e instanceof VindicatorEntity
                || e instanceof EvokerEntity
                || e instanceof IllusionerEntity
                || e instanceof RavagerEntity;
    }

    public static boolean isPillagerPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PILLAGER;
    }

    public static void register() {

        // ── Attack: mark provoked, handle melee damage ────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isPillagerPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity)) return ActionResult.PASS;
            if (isIllagerAlly(entity)) return ActionResult.PASS;
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            return ActionResult.PASS;
        });

        // ── ALLOW_DAMAGE: sounds, rally, provoke ──────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isPillagerPossessing(player)) return true;

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PILLAGER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            ensureArrows(player);

            Entity attacker = source.getAttacker();
            if (attacker == null || isIllagerAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                Box box = player.getBoundingBox().expand(40.0);
                for (MobEntity mob : player.getServerWorld()
                        .getEntitiesByClass(MobEntity.class, box, m -> isRallyMob(m) && m.isAlive())) {
                    mob.setTarget(livingAttacker);
                }
            }
            return true;
        });

        // ── Death sound ───────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isPillagerPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PILLAGER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static final java.util.Set<UUID> HEALTH_INITIALIZED = ConcurrentHashMap.newKeySet();

    public static void tick(ServerPlayerEntity player) {
        if (!isPillagerPossessing(player)) return;

        lockHunger(player);
        ensurePillagerItems(player);
        handleMeleeDamage(player);

        // Health init is handled from SamuelRequiemMod after PossessionEffects.apply()

        // Crossbow reload animation: if player is charging crossbow, show pillager pose
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof CrossbowItem) {
            if (player.isUsingItem() && CrossbowItem.isCharged(mainHand) == false) {
                // Player is actively charging — play charging sound occasionally
                if (player.age % 10 == 0) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_CROSSBOW_LOADING_MIDDLE,
                            SoundCategory.PLAYERS, 0.8f, 1.0f);
                }
            }
        }

        // Ambient sound
        if (player.age % 100 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PILLAGER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Make nearby villagers flee — check every 10 ticks for performance
        if (player.age % 10 == 0) {
            scarVillagersPublic(player);
        }

        // Re-rally allies onto the last attacker every 20 ticks
        if (player.age % 20 == 0) {
            persistRally(player);
        }
    }


    // ── Persistent rally (only targets that actually hit the player) ──────────
    private static void persistRally(ServerPlayerEntity player) {
        UUID attackerUuid = LAST_ATTACKER.get(player.getUuid());
        if (attackerUuid == null) return;

        net.minecraft.entity.Entity attackerEntity = player.getServerWorld().getEntity(attackerUuid);
        if (!(attackerEntity instanceof net.minecraft.entity.LivingEntity attacker)
                || !attacker.isAlive()) {
            LAST_ATTACKER.remove(player.getUuid());
            return;
        }

        net.minecraft.util.math.Box box = player.getBoundingBox().expand(40.0);
        for (net.minecraft.entity.mob.MobEntity ally : player.getServerWorld()
                .getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box,
                        m -> isRallyMob(m) && m.isAlive())) {
            if (ally.getTarget() == null || !ally.getTarget().isAlive()) {
                ally.setTarget(attacker);
            }
        }
    }

    // ── Villager fleeing ──────────────────────────────────────────────────────
    public static void scarVillagersPublic(ServerPlayerEntity player) {
        net.minecraft.util.math.Box box = player.getBoundingBox().expand(16.0);
        java.util.List<net.minecraft.entity.passive.VillagerEntity> villagers =
                player.getServerWorld().getEntitiesByClass(
                        net.minecraft.entity.passive.VillagerEntity.class, box,
                        v -> v.isAlive() && v.squaredDistanceTo(player) <= 16.0 * 16.0);
        for (net.minecraft.entity.passive.VillagerEntity villager : villagers) {
            // Tell the villager the pillager player is its attacker so it flees
            villager.setAttacker(player);
            // Also nudge navigation directly away from the player
            double dx = villager.getX() - player.getX();
            double dz = villager.getZ() - player.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                double fleeX = villager.getX() + (dx / len) * 12.0;
                double fleeZ = villager.getZ() + (dz / len) * 12.0;
                villager.getNavigation().startMovingTo(fleeX, villager.getY(), fleeZ, 0.6);
            }
        }
    }

    // ── Melee damage ──────────────────────────────────────────────────────────
    // Pillager unarmed: Easy=2, Normal=3, Hard=4.5 HP (1/1.5/2.25 hearts)
    private static void handleMeleeDamage(ServerPlayerEntity player) {
        // Applied via ALLOW_DAMAGE on entities we hit — use attack damage attribute instead
        // Pillager base attack = 5 HP (2.5 hearts). We set this via attribute.
        // The actual scaling is handled by vanilla difficulty.
        // We just need to ensure their base attack attribute is correct.
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensurePillagerItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureArrows(player);
            ensureCrossbowUnbreakable(player);
            return;
        }

        // Unbreakable crossbow with Infinity
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        try {
            crossbow.addEnchantment(
                    player.getServerWorld().getRegistryManager()
                            .get(RegistryKeys.ENCHANTMENT)
                            .getEntry(Enchantments.INFINITY).orElseThrow(),
                    1);
        } catch (Exception ignored) {}
        crossbow.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));

        // Arrows (with Infinity on crossbow, one arrow is enough but give 64 for safety)
        ItemStack arrows = new ItemStack(Items.ARROW, 64);

        // Ominous banner via reflection
        ItemStack banner = createOminousBannerPublic(player);

        // Goat horn
        ItemStack horn = new ItemStack(Items.GOAT_HORN);

        giveToSlot(player, crossbow, 0);
        player.getInventory().insertStack(arrows);
        if (!player.getInventory().insertStack(banner.copy()))
            player.dropItem(banner.copy(), false);
        GIVEN_BANNER.put(player.getUuid(), banner);
        player.getInventory().insertStack(horn);

        ITEMS_GIVEN.add(player.getUuid());
    }

    public static ItemStack createOminousBannerPublic(ServerPlayerEntity player) {
        try {
            var patReg = player.getServerWorld().getRegistryManager()
                    .get(net.minecraft.registry.RegistryKeys.BANNER_PATTERN);

            // Log ALL available pattern IDs so we can confirm exact names
            patReg.getIds().forEach(id ->
                    net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info("[Pillager] PatternID: {}", id.getPath()));

            var layers = new java.util.ArrayList<net.minecraft.component.type.BannerPatternsComponent.Layer>();

            // Vanilla illager captain banner — exact pattern sequence from MC source
            // Base color: gray banner
            String[][] pats = {
                    {"rhombus",       "cyan"},
                    {"stripe_bottom", "light_gray"},
                    {"stripe_center", "gray"},
                    {"border",        "light_gray"},
                    {"stripe_middle", "black"},
                    {"half_horizontal", "light_gray"},
                    {"circle",        "light_gray"},
                    {"border",        "black"}
            };

            for (String[] p : pats) {
                var key = net.minecraft.registry.RegistryKey.of(
                        net.minecraft.registry.RegistryKeys.BANNER_PATTERN,
                        net.minecraft.util.Identifier.of("minecraft", p[0]));
                var entry = patReg.getEntry(key);
                final net.minecraft.util.DyeColor color = net.minecraft.util.DyeColor.byName(p[1], net.minecraft.util.DyeColor.WHITE);
                entry.ifPresent(e -> layers.add(new net.minecraft.component.type.BannerPatternsComponent.Layer(e, color)));
            }
            var banner = new ItemStack(Items.WHITE_BANNER);
            if (!layers.isEmpty())
                banner.set(net.minecraft.component.DataComponentTypes.BANNER_PATTERNS,
                        new net.minecraft.component.type.BannerPatternsComponent(layers));
            return banner;
        } catch (Exception e) {
            net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.warn("[Pillager] Banner error: {}", e.getMessage());
            return new ItemStack(Items.GRAY_BANNER);
        }
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    private static void ensureCrossbowUnbreakable(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(Items.CROSSBOW) && !s.contains(DataComponentTypes.UNBREAKABLE))
                s.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
        }
    }

    static void ensureArrows(ServerPlayerEntity player) {
        boolean has = false;
        for (int i = 0; i < player.getInventory().size(); i++)
            if (player.getInventory().getStack(i).isOf(Items.ARROW)) { has = true; break; }
        if (!has) player.getInventory().offerOrDrop(new ItemStack(Items.ARROW, 64));
    }

    // ── Hunger lock ───────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        // Lock at 19 (not 20) so vanilla's hunger check allows eating
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Food helpers (used by ZombieFoodUseHandler) ──────────────────────────
    public static boolean isPillagerFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return getPillagerFoodHealing(stack) > 0;
    }

    public static float getPillagerFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item == Items.COOKED_BEEF)             return 8;
        if (item == Items.COOKED_PORKCHOP)         return 8;
        if (item == Items.COOKED_CHICKEN)          return 6;
        if (item == Items.COOKED_MUTTON)           return 6;
        if (item == Items.COOKED_RABBIT)           return 5;
        if (item == Items.COOKED_COD)              return 5;
        if (item == Items.COOKED_SALMON)           return 6;
        if (item == Items.GOLDEN_APPLE)            return 4;
        if (item == Items.ENCHANTED_GOLDEN_APPLE)  return 8;
        return 0;
    }

    // ── Remove pillager items on unpossess ────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        HEALTH_INITIALIZED.remove(player.getUuid());
        LAST_ATTACKER.remove(player.getUuid());
        // Remove standard pillager items
        removeItemType(player, Items.CROSSBOW);
        removeItemType(player, Items.ARROW);
        removeItemType(player, Items.GOAT_HORN);
        // Remove ALL banner types to catch any banner we gave
        net.minecraft.registry.Registries.ITEM.stream().forEach(item -> {
            var id = net.minecraft.registry.Registries.ITEM.getId(item);
            if (id != null) {
                String path = id.getPath();
                if (path.endsWith("_banner") || path.equals("ominous_banner"))
                    removeItemType(player, item);
            }
        });
        // Also remove by exact tracked stack
        var trackedBanner = GIVEN_BANNER.remove(player.getUuid());
        if (trackedBanner != null && !trackedBanner.isEmpty()) {
            for (int i = player.getInventory().size() - 1; i >= 0; i--) {
                var s = player.getInventory().getStack(i);
                if (!s.isEmpty() && net.minecraft.item.ItemStack.areItemsAndComponentsEqual(s, trackedBanner))
                    player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
        }
    }

    public static void onUnpossessUuid(UUID playerUuid) {
        ITEMS_GIVEN.remove(playerUuid);
        GIVEN_BANNER.remove(playerUuid);
    }
}