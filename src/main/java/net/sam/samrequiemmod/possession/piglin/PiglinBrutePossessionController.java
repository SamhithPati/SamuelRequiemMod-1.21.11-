package net.sam.samrequiemmod.possession.piglin;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

public final class PiglinBrutePossessionController {

    private PiglinBrutePossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();
    private static final int ARMS_RAISED_TICKS = 100; // 5 seconds

    public static boolean isPiglinBrutePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PIGLIN_BRUTE;
    }

    public static void register() {

        // ── Attack: custom axe damage, arms raised, rally ─────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isPiglinBrutePossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;

            LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(sp, true);

            // Custom golden axe damage
            ItemStack mainHand = sp.getMainHandStack();
            if (!mainHand.isEmpty() && mainHand.isOf(Items.GOLDEN_AXE)) {
                float axeDmg = switch (world.getDifficulty()) {
                    case EASY   -> 7.0f;   // 3.5 hearts
                    case HARD   -> 19.0f;  // 9.5 hearts
                    default     -> 13.0f;  // 6.5 hearts
                };
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), axeDmg);
            } else {
                float damage = calculateUnarmedDamage(sp);
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
            }

            // Piglins/brutes always passive — don't provoke
            if (PiglinPossessionController.isPiglinAlly(entity)) {
                sp.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());

            // Rally nearby piglins and brutes when hitting a non-piglin
            if (entity instanceof LivingEntity lt)
                PiglinPossessionController.rallyNearbyPiglins(sp, lt);

            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── Hurt: sound, rally, provoke ───────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isPiglinBrutePossessing(player)) return true;

            Entity attacker = source.getAttacker();
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_PIGLIN_BRUTE_HURT, 1.0f);

            ensureAxe(player);

            if (attacker == null || PiglinPossessionController.isPiglinAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                PiglinPossessionController.rallyNearbyPiglins(player, livingAttacker);
            }
            return true;
        });

        // ── Death sound ───────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isPiglinBrutePossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isPiglinBrutePossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }

        PiglinPossessionController.lockHunger(player);
        PiglinPossessionController.preventNaturalHealing(player);
        ensureBruteItems(player);
        tickArmsRaised(player);
        handleOverworldConversion(player);

        // Ambient sound
        if (player.age % 120 == 0 && player.getRandom().nextFloat() < 0.35f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Re-rally allies every 20 ticks
        if (player.age % 20 == 0) persistRally(player);
    }

    // ── Overworld conversion ──────────────────────────────────────────────────
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

        if (ticks == 1) WaterShakeNetworking.broadcast(player, true);

        if (ticks >= 400) {
            OverworldConversionTracker.reset(player.getUuid());
            WaterShakeNetworking.broadcast(player, false);

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED,
                    SoundCategory.HOSTILE, 1.0f, 1.0f);

            float health = player.getHealth();
            PossessionManager.clearPossession(player);
            PossessionManager.startPossession(player, EntityType.ZOMBIFIED_PIGLIN, health);
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────
    private static float calculateUnarmedDamage(ServerPlayerEntity player) {
        return switch (player.getEntityWorld().getDifficulty()) {
            case EASY   -> 5.0f;   // 2.5 hearts
            case NORMAL -> 8.0f;   // 4 hearts
            case HARD   -> 12.0f;  // 6 hearts
            default     -> 8.0f;
        };
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
                .getEntitiesByClass(MobEntity.class, box, m -> PiglinPossessionController.isPiglinAlly(m) && m.isAlive())) {
            if (ally instanceof net.minecraft.entity.mob.PiglinEntity piglin && piglin.isBaby()) continue;
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
    private static void ensureBruteItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureAxe(player);
            return;
        }

        ItemStack axe = new ItemStack(Items.GOLDEN_AXE);
        axe.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        giveToSlot(player, axe, 0);

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
            if (player.getInventory().getStack(i).isOf(Items.GOLDEN_AXE)) { has = true; break; }
        if (!has) player.getInventory().offerOrDrop(new ItemStack(Items.GOLDEN_AXE));
    }

    // ── Unpossess ─────────────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        ITEMS_GIVEN.remove(player.getUuid());
        LAST_HIT_TICK.remove(player.getUuid());
        LAST_ATTACKER.remove(player.getUuid());
        OverworldConversionTracker.reset(player.getUuid());
        removeItemType(player, Items.GOLDEN_AXE);
        ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        WaterShakeNetworking.broadcast(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        ITEMS_GIVEN.remove(uuid);
        LAST_HIT_TICK.remove(uuid);
        LAST_ATTACKER.remove(uuid);
        OverworldConversionTracker.reset(uuid);
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--)
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
    }
}








