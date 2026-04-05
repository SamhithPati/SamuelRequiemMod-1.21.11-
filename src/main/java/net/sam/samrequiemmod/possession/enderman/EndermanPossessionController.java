package net.sam.samrequiemmod.possession.enderman;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EndermanPossessionController {

    private EndermanPossessionController() {}

    /** Last teleport tick for cooldown tracking (1 second = 20 ticks). */
    private static final Map<UUID, Long> LAST_TELEPORT_TICK = new ConcurrentHashMap<>();

    /** Last attack tick for attack animation. */
    public static final Map<UUID, Long> LAST_ATTACK_TICK = new ConcurrentHashMap<>();

    /** Angry state per player (server-side truth). */
    private static final Map<UUID, Boolean> ANGRY_STATE = new ConcurrentHashMap<>();

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isEndermanPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ENDERMAN;
    }

    public static boolean isAngry(UUID uuid) {
        return ANGRY_STATE.getOrDefault(uuid, false);
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // ── Left-click: custom melee damage ─────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isEndermanPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

            LAST_ATTACK_TICK.put(sp.getUuid(), (long) sp.age);

            float damage = switch (world.getDifficulty()) {
                case EASY     -> 4.5f;
                case HARD     -> 15.5f;
                default       -> 10.5f; // Normal + Peaceful
            };
            target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);

            // Mark provoked for non-allies
            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }
            return ActionResult.FAIL; // we handle damage ourselves
        });

        // ── ALLOW_DAMAGE: hurt sound, projectile dodge ──────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isEndermanPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            // Projectile dodge: auto-teleport away
            if (source.getSource() instanceof ProjectileEntity) {
                boolean teleported = teleportRandomly(player, 20);
                if (teleported) {
                    player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }
                return false; // cancel projectile damage
            }

            // Play hurt sound
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return true;
        });

        // ── Death sound ─────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isEndermanPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isEndermanPossessing(player)) return;

        lockHunger(player);

        // Water damage (1 HP every 10 ticks = 0.5 seconds)
        if (player.isTouchingWater() || player.isTouchingWaterOrRain()) {
            if (player.age % 10 == 0) {
                player.damage(player.getEntityWorld(), player.getDamageSources().drown(), 1.0f);
            }
        }

        // Ambient sounds
        if (player.age % 160 == 0 && player.getRandom().nextFloat() < 0.3f) {
            if (isAngry(player.getUuid())) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 1.0f, 1.0f);
            } else {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        }

        // Stare sound when angry (every 3 seconds)
        if (isAngry(player.getUuid()) && player.age % 60 == 0) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.PLAYERS, 0.5f, 1.0f);
        }

        // Make nearby snow golems and endermites actively target this player.
        // Vanilla AI goals only target MobEntity/EndermanEntity, not PlayerEntity,
        // so we manually assign targets every second.
        if (player.age % 20 == 0) {
            net.minecraft.util.math.Box box = player.getBoundingBox().expand(16.0);
            for (MobEntity mob : player.getEntityWorld()
                    .getEntitiesByClass(MobEntity.class, box, m -> m.isAlive())) {
                if ((mob instanceof net.minecraft.entity.passive.SnowGolemEntity
                        || mob instanceof net.minecraft.entity.mob.EndermiteEntity)
                        && (mob.getTarget() == null || !mob.getTarget().isAlive())) {
                    mob.setTarget(player);
                }
            }
        }
    }

    // ── Teleportation ──────────────────────────────────────────────────────────

    /**
     * Teleports the player in their look direction (up to 30 blocks).
     * If looking at a block, teleports to the surface of that block.
     * If looking into open air, teleports 30 blocks forward and finds a safe spot.
     */
    public static boolean handleDirectionalTeleport(ServerPlayerEntity player) {
        // Check cooldown (1 second = 20 ticks)
        long lastTeleport = LAST_TELEPORT_TICK.getOrDefault(player.getUuid(), -1000L);
        if ((long) player.age - lastTeleport < 20) return false;

        World world = player.getEntityWorld();
        Vec3d eyePos = player.getEyePos();
        Vec3d lookDir = player.getRotationVector();
        Vec3d endPos = eyePos.add(lookDir.multiply(30.0));

        // Raycast to find a block in the look direction
        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos, endPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        BlockPos targetPos;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Teleport to the surface of the hit block
            targetPos = hitResult.getBlockPos().offset(hitResult.getSide());
        } else {
            // No block hit — try to find a safe spot near the 30-block endpoint
            BlockPos endBlockPos = BlockPos.ofFloored(endPos);
            targetPos = findSafeSpotNear(world, endBlockPos);
            if (targetPos == null) return false;
        }

        // Safety checks
        if (!isSafeTeleportDestination(world, targetPos)) {
            // Try to find a nearby safe spot
            targetPos = findSafeSpotNear(world, targetPos);
            if (targetPos == null) return false;
        }

        // Final validation
        if (!isSafeTeleportDestination(world, targetPos)) return false;

        // Play teleport sound at origin
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Teleport
        player.teleport(player.getEntityWorld(),
                targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5,
                java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);

        // Play teleport sound at destination
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        LAST_TELEPORT_TICK.put(player.getUuid(), (long) player.age);
        return true;
    }

    /**
     * Teleports the player to a random safe location within the given radius.
     * Used for projectile dodging.
     */
    private static boolean teleportRandomly(ServerPlayerEntity player, int radius) {
        World world = player.getEntityWorld();

        for (int attempt = 0; attempt < 20; attempt++) {
            double dx = (player.getRandom().nextDouble() - 0.5) * 2.0 * radius;
            double dz = (player.getRandom().nextDouble() - 0.5) * 2.0 * radius;
            int x = (int) Math.floor(player.getX() + dx);
            int z = (int) Math.floor(player.getZ() + dz);

            // Find the highest non-air block at this position
            BlockPos testPos = new BlockPos(x, (int) player.getY() + radius, z);
            while (testPos.getY() > world.getBottomY() && world.getBlockState(testPos).isAir()) {
                testPos = testPos.down();
            }
            // Stand on top of the found block
            BlockPos landingPos = testPos.up();

            if (isSafeTeleportDestination(world, landingPos)) {
                player.teleport(player.getEntityWorld(),
                        landingPos.getX() + 0.5, landingPos.getY(), landingPos.getZ() + 0.5,
                        java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
                LAST_TELEPORT_TICK.put(player.getUuid(), (long) player.age);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a position is safe for teleporting:
     * - The feet position and head position must be non-solid (not inside blocks)
     * - The block below must be solid (standing on something)
     * - No water or lava at or near destination
     * - Accounts for enderman's 3-block height
     */
    private static boolean isSafeTeleportDestination(World world, BlockPos feetPos) {
        BlockPos belowPos = feetPos.down();
        BlockState belowState = world.getBlockState(belowPos);

        // Must have a collidable block below. Using collision instead of isSolidBlock
        // allows normal standable blocks like ice to count as valid landing surfaces.
        VoxelShape belowShape = belowState.getCollisionShape(world, belowPos);
        if (belowShape.isEmpty()) return false;

        // Check that the block below isn't water or lava
        FluidState belowFluid = world.getFluidState(belowPos);
        if (!belowFluid.isEmpty()) return false;

        // Check 3 blocks of height for the enderman (feet, body, head)
        for (int dy = 0; dy < 3; dy++) {
            BlockPos checkPos = feetPos.up(dy);
            BlockState state = world.getBlockState(checkPos);
            if (!state.getCollisionShape(world, checkPos).isEmpty()) return false;

            // No water or lava in the body space
            FluidState fluid = world.getFluidState(checkPos);
            if (!fluid.isEmpty()) return false;
        }

        // Check surrounding area for water/lava (endermen hate water)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos surroundPos = feetPos.add(dx, 0, dz);
                FluidState surroundFluid = world.getFluidState(surroundPos);
                if (surroundFluid.isIn(FluidTags.WATER) || surroundFluid.isIn(FluidTags.LAVA)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Finds a safe teleport spot near the given position by searching
     * in a small radius and vertically.
     */
    private static BlockPos findSafeSpotNear(World world, BlockPos center) {
        // First check column directly at center
        BlockPos spot = findSafeSpotInColumn(world, center);
        if (spot != null) return spot;

        // Search in expanding radius
        for (int r = 1; r <= 5; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // only check perimeter
                    spot = findSafeSpotInColumn(world, center.add(dx, 0, dz));
                    if (spot != null) return spot;
                }
            }
        }
        return null;
    }

    private static BlockPos findSafeSpotInColumn(World world, BlockPos center) {
        // Search up and down from center Y
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos up = center.up(dy);
            if (world.isInBuildLimit(up) && isSafeTeleportDestination(world, up)) return up;
            BlockPos down = center.down(dy);
            if (world.isInBuildLimit(down) && isSafeTeleportDestination(world, down)) return down;
        }
        return null;
    }

    // ── Angry toggle ───────────────────────────────────────────────────────────

    public static void handleAngryToggle(ServerPlayerEntity player) {
        if (!isEndermanPossessing(player)) return;
        boolean current = ANGRY_STATE.getOrDefault(player.getUuid(), false);
        boolean newState = !current;
        ANGRY_STATE.put(player.getUuid(), newState);

        // Play scream sound when becoming angry
        if (newState) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_SCREAM, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // Broadcast to all clients
        EndermanNetworking.broadcastAngry(player, newState);
    }

    // ── Hunger lock ────────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Food helpers (used by ZombieFoodUseHandler) ────────────────────────────

    public static boolean isEndermanFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.CHORUS_FRUIT);
    }

    public static float getEndermanFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.isOf(Items.CHORUS_FRUIT)) return 4.0f; // 2 hearts
        return 0;
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        cleanup(uuid);
        EndermanNetworking.broadcastAngry(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        cleanup(uuid);
    }

    private static void cleanup(UUID uuid) {
        LAST_TELEPORT_TICK.remove(uuid);
        LAST_ATTACK_TICK.remove(uuid);
        ANGRY_STATE.remove(uuid);
    }
}






