package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VindicatorPossessionController {

    private VindicatorPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ItemStack> GIVEN_BANNER = new ConcurrentHashMap<>();

    /** Tracks last entity that hit the vindicator player for persistent ally rally. */
    static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    /** Last time the player hit a mob — for attack animation timeout (in ticks). */
    public static final Map<UUID, Long> LAST_ATTACK_TICK = new ConcurrentHashMap<>();

    public static boolean isVindicatorPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.VINDICATOR;
    }

    public static void register() {

        // ── Attack: mark provoked + rally + custom axe damage ───────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isVindicatorPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

            // Always track attack time and apply axe damage — even against allies
            LAST_ATTACK_TICK.put(sp.getUuid(), (long) sp.age);

            // Apply custom axe damage if holding iron axe
            ItemStack mainHand = sp.getMainHandStack();
            if (!mainHand.isEmpty() && mainHand.isOf(net.minecraft.item.Items.IRON_AXE)) {
                float axeDmg = switch (world.getDifficulty()) {
                    case EASY   -> 7.0f;   // 3.5 hearts
                    case HARD   -> 19.0f;  // 9.5 hearts
                    default     -> 13.0f;  // 6.5 hearts (Normal + Peaceful)
                };
                target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), axeDmg);
            }

            // Only mark provoked for non-allies
            if (PillagerPossessionController.isIllagerAlly(entity)) return ActionResult.PASS;
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            return ActionResult.PASS;
        });

        // ── ALLOW_DAMAGE: sounds, rally, provoke ─────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isVindicatorPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_VINDICATOR_HURT, 1.0f);

            ensureAxe(player);

            Entity attacker = source.getAttacker();
            if (attacker == null || PillagerPossessionController.isIllagerAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                Box box = player.getBoundingBox().expand(60.0);
                for (MobEntity mob : player.getEntityWorld()
                        .getEntitiesByClass(MobEntity.class, box,
                                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
                    if (mob instanceof WitchEntity witch) {
                        witch.setTarget(null);
                        witch.getNavigation().stop();
                        continue;
                    }
                    mob.setTarget(livingAttacker);
                }
            }
            return true;
        });

        // ── Death sound ───────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isVindicatorPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_VINDICATOR_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isVindicatorPossessing(player)) return;

        lockHunger(player);
        ensureVindicatorItems(player);

        // Ambient sound
        if (player.age % 100 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_VINDICATOR_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Re-rally allies onto last attacker every 20 ticks
        if (player.age % 20 == 0) {
            persistRally(player);
        }

        // Make nearby villagers flee every 10 ticks
        if (player.age % 10 == 0) {
            PillagerPossessionController.scarVillagersPublic(player);
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
        Box box = player.getBoundingBox().expand(60.0);
        for (MobEntity ally : player.getEntityWorld()
                .getEntitiesByClass(MobEntity.class, box,
                        m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
            if (ally instanceof WitchEntity witch) {
                witch.setTarget(null);
                witch.getNavigation().stop();
                continue;
            }
            if (ally.getTarget() == null || !ally.getTarget().isAlive())
                ally.setTarget(attacker);
        }
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensureVindicatorItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureAxe(player);
            return;
        }

        // Unbreakable iron axe
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        axe.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);

        // Ominous banner
        ItemStack banner = PillagerPossessionController.createOminousBannerPublic(player);

        // Goat horn
        ItemStack horn = new ItemStack(Items.GOAT_HORN);

        giveToSlot(player, axe, 0);
        if (!player.getInventory().insertStack(banner.copy()))
            player.dropItem(banner.copy(), false);
        GIVEN_BANNER.put(player.getUuid(), banner);
        player.getInventory().insertStack(horn);

        ITEMS_GIVEN.add(player.getUuid());
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    static void ensureAxe(ServerPlayerEntity player) {
        boolean has = false;
        for (int i = 0; i < player.getInventory().size(); i++)
            if (player.getInventory().getStack(i).isOf(Items.IRON_AXE)) { has = true; break; }
        if (!has) player.getInventory().offerOrDrop(new ItemStack(Items.IRON_AXE));
    }

    // ── Hunger lock ───────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Damage helpers ────────────────────────────────────────────────────────
    public static boolean isVindicatorWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.IRON_AXE);
    }

    public static float getVindicatorWeaponHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item == Items.COOKED_BEEF)            return 8;
        if (item == Items.COOKED_PORKCHOP)        return 8;
        if (item == Items.COOKED_CHICKEN)         return 6;
        if (item == Items.COOKED_MUTTON)          return 6;
        if (item == Items.COOKED_RABBIT)          return 5;
        if (item == Items.COOKED_COD)             return 5;
        if (item == Items.COOKED_SALMON)          return 6;
        if (item == Items.GOLDEN_APPLE)           return 4;
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return 8;
        return 0;
    }

    // ── Unpossess cleanup ─────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        LAST_ATTACKER.remove(player.getUuid());
        LAST_ATTACK_TICK.remove(player.getUuid());
        IllagerRavagerCallController.onUnpossess(player);
        removeItemType(player, Items.IRON_AXE);
        removeItemType(player, Items.GOAT_HORN);
        net.minecraft.registry.Registries.ITEM.stream().forEach(item -> {
            var id = net.minecraft.registry.Registries.ITEM.getId(item);
            if (id != null && (id.getPath().endsWith("_banner") || id.getPath().equals("ominous_banner")))
                removeItemType(player, item);
        });
        var trackedBanner = GIVEN_BANNER.remove(player.getUuid());
        if (trackedBanner != null && !trackedBanner.isEmpty()) {
            for (int i = player.getInventory().size() - 1; i >= 0; i--) {
                var s = player.getInventory().getStack(i);
                if (!s.isEmpty() && ItemStack.areItemsAndComponentsEqual(s, trackedBanner))
                    player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--)
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
    }

    public static void onUnpossessUuid(UUID uuid) {
        ITEMS_GIVEN.remove(uuid);
        GIVEN_BANNER.remove(uuid);
        LAST_ATTACKER.remove(uuid);
        LAST_ATTACK_TICK.remove(uuid);
        IllagerRavagerCallController.onUnpossessUuid(uuid);
    }
}







