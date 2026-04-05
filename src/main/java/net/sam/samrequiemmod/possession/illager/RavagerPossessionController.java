package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.rule.GameRules;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RavagerPossessionController {

    private RavagerPossessionController() {}

    /** Tracks last entity that hit the ravager player for persistent ally rally. */
    static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    /** Last time the player performed a bite attack (ticks) — 1 second cooldown. */
    public static final Map<UUID, Long> LAST_BITE_TICK = new ConcurrentHashMap<>();

    /** Last time the player performed a roar attack (ticks). */
    public static final Map<UUID, Long> LAST_ROAR_TICK = new ConcurrentHashMap<>();

    /** Tracks how long the player has been holding right-click (ticks). */
    private static final Map<UUID, Integer> ROAR_CHARGE_TICKS = new ConcurrentHashMap<>();

    public static boolean isRavagerPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.RAVAGER;
    }

    /** Mobs that are completely passive to the ravager player — illagers, ravagers, witches, vexes. */
    public static boolean isRavagerAlly(Entity e) {
        return e instanceof PillagerEntity
                || e instanceof VindicatorEntity
                || e instanceof EvokerEntity
                || e instanceof IllusionerEntity
                || e instanceof WitchEntity
                || e instanceof RavagerEntity
                || e instanceof VexEntity;
    }

    /** Mobs that rally when the ravager player is attacked — only other ravagers. */
    private static boolean isRavagerRallyMob(Entity e) {
        return e instanceof RavagerEntity;
    }

    public static void register() {

        // ── Attack callback: intercept left-click for bite attack ─────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isRavagerPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

            // Check cooldown (20 ticks = 1 second)
            long lastBite = LAST_BITE_TICK.getOrDefault(sp.getUuid(), -1000L);
            if ((long) sp.age - lastBite < 20) return ActionResult.FAIL;

            // Perform bite attack (allies take damage but won't retaliate — handled by MobEntityCanTargetMixin)
            performBite(sp, target);
            LAST_BITE_TICK.put(sp.getUuid(), (long) sp.age);

            // Only mark non-allies as provoked so allies stay passive
            if (!isRavagerAlly(entity) && entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());

            return ActionResult.FAIL; // consume the attack — we handle damage ourselves
        });

        // ── ALLOW_DAMAGE: sounds, rally when hit ────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isRavagerPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_RAVAGER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker == null || isRavagerAlly(attacker)) return true;

            if (attacker instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());

            // Rally all ravagers within 40 blocks to attack the aggressor
            if (attacker instanceof LivingEntity livingAttacker) {
                LAST_ATTACKER.put(player.getUuid(), livingAttacker.getUuid());
                Box box = player.getBoundingBox().expand(40.0);
                for (MobEntity mob : player.getEntityWorld()
                        .getEntitiesByClass(MobEntity.class, box,
                                m -> isRavagerRallyMob(m) && m.isAlive())) {
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

        // ── Death sound ──────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isRavagerPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_RAVAGER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Bite attack ──────────────────────────────────────────────────────────
    private static void performBite(ServerPlayerEntity player, LivingEntity target) {
        // Damage based on difficulty
        float damage = switch (player.getEntityWorld().getDifficulty()) {
            case EASY -> 7.0f;      // 3.5 hearts
            case HARD -> 18.0f;     // 9 hearts
            default -> 12.0f;       // 6 hearts (Normal + Peaceful)
        };

        target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), player.getDamageSources().playerAttack(player), damage);

        // Knockback: 2-4 blocks horizontal
        Vec3d direction = target.getEntityPos().subtract(player.getEntityPos()).normalize();
        double knockbackStrength = 2.0 + player.getRandom().nextDouble() * 2.0; // 2-4
        target.setVelocity(
                direction.x * knockbackStrength * 0.35,
                0.25 + player.getRandom().nextDouble() * 0.12, // slight launcher effect
                direction.z * knockbackStrength * 0.35
        );
        target.velocityDirty = true;

        // Play bite sound
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_ATTACK, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Sync bite animation to clients
        RavagerNetworking.broadcastBite(player);
    }

    // ── Roar attack ─────────────────────────────────────────────────────────
    public static void handleRoarCharge(ServerPlayerEntity player, boolean holding) {
        if (!isRavagerPossessing(player)) return;

        UUID uuid = player.getUuid();
        if (holding) {
            int ticks = ROAR_CHARGE_TICKS.getOrDefault(uuid, 0) + 1;
            ROAR_CHARGE_TICKS.put(uuid, ticks);

            // 2 seconds = 40 ticks of holding
            if (ticks >= 40) {
                performRoar(player);
                ROAR_CHARGE_TICKS.remove(uuid);
            }
        } else {
            ROAR_CHARGE_TICKS.remove(uuid);
        }
    }

    private static void performRoar(ServerPlayerEntity player) {
        // Play roar sound
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Sync roar animation to clients
        RavagerNetworking.broadcastRoar(player);
        LAST_ROAR_TICK.put(player.getUuid(), (long) player.age);

        // Damage all non-ally mobs within 4 blocks
        Box aoeBox = player.getBoundingBox().expand(4.0);
        List<LivingEntity> targets = player.getEntityWorld()
                .getEntitiesByClass(LivingEntity.class, aoeBox,
                        e -> e != player && e.isAlive() && !isRoarImmune(e)
                                && e.squaredDistanceTo(player) <= 4.0 * 4.0);

        for (LivingEntity target : targets) {
            // 3 hearts = 6 damage
            target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), player.getDamageSources().mobAttack(player), 6.0f);

            // Knockback: 7 blocks horizontal
            Vec3d direction = target.getEntityPos().subtract(player.getEntityPos()).normalize();
            double knockbackStrength = 7.0;
            // Vertical launch: 2-3 blocks
            double verticalLaunch = 0.6 + player.getRandom().nextDouble() * 0.3; // 2-3 blocks up
            target.setVelocity(
                    direction.x * knockbackStrength * 0.4,
                    verticalLaunch,
                    direction.z * knockbackStrength * 0.4
            );
            target.velocityDirty = true;

            // Mark provoked
            if (target instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }
    }

    /** Illagers and ravagers are immune to the roar. */
    private static boolean isRoarImmune(Entity e) {
        return e instanceof PillagerEntity
                || e instanceof VindicatorEntity
                || e instanceof EvokerEntity
                || e instanceof IllusionerEntity
                || e instanceof RavagerEntity;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    public static void tick(ServerPlayerEntity player) {
        if (!isRavagerPossessing(player)) return;

        lockHunger(player);
        breakLeaves(player);

        // Ambient sound
        if (player.age % 120 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_RAVAGER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Re-rally ravagers onto last attacker every 20 ticks
        if (player.age % 20 == 0) {
            persistRally(player);
        }

        // Make nearby villagers flee every 10 ticks
        if (player.age % 10 == 0) {
            PillagerPossessionController.scarVillagersPublic(player);
        }
    }

    // ── Persistent rally ────────────────────────────────────────────────────
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
                .getEntitiesByClass(MobEntity.class, box,
                        m -> isRavagerRallyMob(m) && m.isAlive())) {
            if (ally instanceof WitchEntity witch) {
                witch.setTarget(null);
                witch.getNavigation().stop();
                continue;
            }
            if (ally.getTarget() == null || !ally.getTarget().isAlive())
                ally.setTarget(attacker);
        }
    }

    // ── Leaf breaking (matches vanilla RavagerEntity.tickMovement) ─────────
    private static void breakLeaves(ServerPlayerEntity player) {
        if (!Boolean.TRUE.equals(player.getEntityWorld().getGameRules().getValue(GameRules.DO_MOB_GRIEFING))) return;

        // Use a wider box than the player's hitbox to match ravager's destructive path
        Box box = player.getBoundingBox().expand(0.6);
        for (net.minecraft.util.math.BlockPos blockPos : net.minecraft.util.math.BlockPos.iterate(
                net.minecraft.util.math.MathHelper.floor(box.minX),
                net.minecraft.util.math.MathHelper.floor(box.minY),
                net.minecraft.util.math.MathHelper.floor(box.minZ),
                net.minecraft.util.math.MathHelper.floor(box.maxX),
                net.minecraft.util.math.MathHelper.floor(box.maxY),
                net.minecraft.util.math.MathHelper.floor(box.maxZ)
        )) {
            net.minecraft.block.BlockState blockState = player.getEntityWorld().getBlockState(blockPos);
            if (blockState.getBlock() instanceof net.minecraft.block.LeavesBlock) {
                player.getEntityWorld().breakBlock(blockPos, true, player);
            }
        }
    }

    // ── Hunger lock ─────────────────────────────────────────────────────────
    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Food helpers ────────────────────────────────────────────────────────
    public static boolean isRavagerFood(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return getRavagerFoodHealing(stack) > 0;
    }

    public static float getRavagerFoodHealing(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var item = stack.getItem();
        // Raw meats
        if (item == net.minecraft.item.Items.BEEF) return 6;
        if (item == net.minecraft.item.Items.PORKCHOP) return 6;
        if (item == net.minecraft.item.Items.CHICKEN) return 4;
        if (item == net.minecraft.item.Items.MUTTON) return 4;
        if (item == net.minecraft.item.Items.RABBIT) return 4;
        if (item == net.minecraft.item.Items.COD) return 3;
        if (item == net.minecraft.item.Items.SALMON) return 3;
        // Cooked meats
        if (item == net.minecraft.item.Items.COOKED_BEEF) return 8;
        if (item == net.minecraft.item.Items.COOKED_PORKCHOP) return 8;
        if (item == net.minecraft.item.Items.COOKED_CHICKEN) return 6;
        if (item == net.minecraft.item.Items.COOKED_MUTTON) return 6;
        if (item == net.minecraft.item.Items.COOKED_RABBIT) return 5;
        if (item == net.minecraft.item.Items.COOKED_COD) return 5;
        if (item == net.minecraft.item.Items.COOKED_SALMON) return 6;
        return 0;
    }

    // ── Unpossess cleanup ───────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        LAST_ATTACKER.remove(player.getUuid());
        LAST_BITE_TICK.remove(player.getUuid());
        LAST_ROAR_TICK.remove(player.getUuid());
        ROAR_CHARGE_TICKS.remove(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_ATTACKER.remove(uuid);
        LAST_BITE_TICK.remove(uuid);
        LAST_ROAR_TICK.remove(uuid);
        ROAR_CHARGE_TICKS.remove(uuid);
    }

    /** Called from client via networking when the bite key is pressed with a target. */
    public static void handleBitePacket(ServerPlayerEntity player, UUID targetUuid) {
        if (!isRavagerPossessing(player)) return;

        // Check cooldown (20 ticks = 1 second)
        long lastBite = LAST_BITE_TICK.getOrDefault(player.getUuid(), -1000L);
        if ((long) player.age - lastBite < 20) return;

        // Find target entity within 4 blocks (extended reach)
        if (targetUuid == null) return;
        Entity targetEntity = player.getEntityWorld().getEntity(targetUuid);
        if (!(targetEntity instanceof LivingEntity target)) return;
        if (!target.isAlive()) return;

        // Verify within 4 blocks
        if (player.squaredDistanceTo(target) > 4.0 * 4.0) return;

        performBite(player, target);
        LAST_BITE_TICK.put(player.getUuid(), (long) player.age);

        // Only mark non-allies as provoked so allies stay passive
        if (!isRavagerAlly(target) && target instanceof MobEntity mob)
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
    }
}






