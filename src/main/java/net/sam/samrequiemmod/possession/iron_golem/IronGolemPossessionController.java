package net.sam.samrequiemmod.possession.iron_golem;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
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
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IronGolemPossessionController {

    private IronGolemPossessionController() {}

    /** Last time the player performed an attack (ticks) — 1 second cooldown. */
    public static final Map<UUID, Long> LAST_ATTACK_TICK = new ConcurrentHashMap<>();

    public static boolean isIronGolemPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.IRON_GOLEM;
    }

    public static void register() {

        // ── Attack callback: intercept left-click for golem attack ────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isIronGolemPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

            // Check cooldown (20 ticks = 1 second)
            long lastAttack = LAST_ATTACK_TICK.getOrDefault(sp.getUuid(), -1000L);
            if ((long) sp.age - lastAttack < 20) return ActionResult.FAIL;

            performAttack(sp, target);
            LAST_ATTACK_TICK.put(sp.getUuid(), (long) sp.age);

            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());

            return ActionResult.FAIL; // consume the attack — we handle damage ourselves
        });

        // ── ALLOW_DAMAGE: hurt sound when hit ────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isIronGolemPossessing(player)) return true;

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return true;
        });

        // ── Fall damage immunity ─────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isIronGolemPossessing(player)) return true;
            if (source.equals(player.getDamageSources().fall())) return false;
            return true;
        });

        // ── Drowning immunity ────────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isIronGolemPossessing(player)) return true;
            if (source.equals(player.getDamageSources().drown())) return false;
            return true;
        });
    }

    // ── Golem attack ─────────────────────────────────────────────────────────
    private static void performAttack(ServerPlayerEntity player, LivingEntity target) {
        // Damage based on difficulty (vanilla iron golem ranges)
        float damage = switch (player.getServerWorld().getDifficulty()) {
            case EASY -> 4.75f + player.getRandom().nextFloat() * (11.75f - 4.75f);
            case HARD -> 11.25f + player.getRandom().nextFloat() * (32.25f - 11.25f);
            default -> 7.5f + player.getRandom().nextFloat() * (21.5f - 7.5f); // Normal + Peaceful
        };

        target.damage(player.getDamageSources().playerAttack(player), damage);

        // Vanilla iron golem knockback: purely vertical, 0.4 scaled by knockback resistance
        double knockbackResistance = 0.0;
        net.minecraft.entity.attribute.EntityAttributeInstance kbAttr =
                target.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (kbAttr != null) knockbackResistance = kbAttr.getValue();
        double scale = Math.max(0.0, 1.0 - knockbackResistance);
        target.setVelocity(target.getVelocity().add(0.0, 0.4 * scale, 0.0));
        target.velocityModified = true;

        // Play attack sound
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Sync attack animation to clients
        IronGolemNetworking.broadcastAttack(player);
    }

    /** Called from client via networking when the attack key is pressed with a target. */
    public static void handleAttackPacket(ServerPlayerEntity player, UUID targetUuid) {
        if (!isIronGolemPossessing(player)) return;

        // Check cooldown (20 ticks = 1 second)
        long lastAttack = LAST_ATTACK_TICK.getOrDefault(player.getUuid(), -1000L);
        if ((long) player.age - lastAttack < 20) return;

        // Find target entity within 4 blocks (extended reach)
        if (targetUuid == null) return;
        net.minecraft.entity.Entity targetEntity = player.getServerWorld().getEntity(targetUuid);
        if (!(targetEntity instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        // Verify within 4 blocks
        if (player.squaredDistanceTo(target) > 4.0 * 4.0) return;

        performAttack(player, target);
        LAST_ATTACK_TICK.put(player.getUuid(), (long) player.age);

        if (target instanceof MobEntity mob)
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isIronGolemPossessing(player)) return;

        lockHunger(player);
        handleSinking(player);
    }

    // ── Hunger lock ──────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Sinking in water (can't swim, but can jump) ────────────────────────
    private static void handleSinking(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            player.setSwimming(false);
            Vec3d vel = player.getVelocity();
            // Allow upward velocity from jumping (space bar), only pull down when not jumping
            if (vel.y > 0.0) {
                // Player is jumping — don't interfere, just disable swimming
                return;
            }
            // Sink when not jumping — gentle downward pull so player walks on the bottom
            player.setVelocity(vel.x, vel.y - 0.02, vel.z);
            player.velocityModified = true;
        }
    }

    // ── Food helpers: iron ingots ────────────────────────────────────────────
    public static boolean isIronGolemFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.isOf(Items.IRON_INGOT);
    }

    public static float getIronGolemFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.isOf(Items.IRON_INGOT)) return 25.0f; // vanilla iron golem repair amount
        return 0;
    }

    // ── Unpossess cleanup ────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        LAST_ATTACK_TICK.remove(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_ATTACK_TICK.remove(uuid);
    }
}
