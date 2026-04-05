package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EvokerPossessionController {

    private EvokerPossessionController() {}

    private static final Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, ItemStack> GIVEN_BANNER = new ConcurrentHashMap<>();

    /** Last fang attack tick per player (2-second cooldown = 40 ticks). */
    private static final Map<UUID, Long> LAST_FANG_TICK = new ConcurrentHashMap<>();

    /** Last vex summon tick per player (5-second cooldown = 100 ticks). */
    private static final Map<UUID, Long> LAST_VEX_TICK = new ConcurrentHashMap<>();

    /** Target entity UUID set by left-click, expires after 5 seconds. */
    static final Map<UUID, UUID> VEX_TARGET = new ConcurrentHashMap<>();
    static final Map<UUID, Long> VEX_TARGET_TICK = new ConcurrentHashMap<>();

    /** Tracks summoned vex UUIDs per player so we can redirect them. */
    private static final Map<UUID, Set<UUID>> SUMMONED_VEXES = new ConcurrentHashMap<>();

    /** Last attacker UUID per player (from ALLOW_DAMAGE), for auto-vex targeting. */
    static final Map<UUID, UUID> LAST_ATTACKER = new ConcurrentHashMap<>();

    public static boolean isEvokerPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.EVOKER;
    }

    public static void register() {

        // ── Left-click: cancel melee — actual fang attack handled via FangAttackPayload from client ────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isEvokerPossessing(sp)) return ActionResult.PASS;
            // Always cancel vanilla melee for evoker
            return ActionResult.FAIL;
        });

        // ── ALLOW_DAMAGE: sounds, rally, auto-vex target ─────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isEvokerPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EVOKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity livingAttacker
                    && !PillagerPossessionController.isIllagerAlly(attacker)) {
                LAST_ATTACKER.put(player.getUuid(), attacker.getUuid());
                VEX_TARGET.put(player.getUuid(), attacker.getUuid());
                VEX_TARGET_TICK.put(player.getUuid(), (long) player.age);
                redirectVexes(player, livingAttacker);
                if (attacker instanceof MobEntity mob)
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                Box box = player.getBoundingBox().expand(40.0);
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
            if (!isEvokerPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EVOKER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Server packet handlers ────────────────────────────────────────────────

    public static void handleFangAttackPacket(ServerPlayerEntity player, UUID targetUuid) {
        if (!isEvokerPossessing(player)) return;
        long last = LAST_FANG_TICK.getOrDefault(player.getUuid(), -100L);
        if (player.age - last < 40) return;
        LAST_FANG_TICK.put(player.getUuid(), (long) player.age);

        if (targetUuid == null) {
            // No target in range — do nothing
            return;
        } else {
            Entity target = player.getEntityWorld().getEntity(targetUuid);
            if (target instanceof LivingEntity le) {
                VEX_TARGET.put(player.getUuid(), targetUuid);
                VEX_TARGET_TICK.put(player.getUuid(), (long) player.age);
                redirectVexes(player, le);
                EvokerNetworking.broadcastTarget(player, targetUuid);

                if (CaptainState.isCaptain(player) && !PillagerPossessionController.isIllagerAlly(le)) {
                    commandNearbyAllies(player, le);
                }

                double dist = player.squaredDistanceTo(target);
                if (dist <= 9.0) {
                    spawnFangCircle(player);
                } else {
                    spawnFangLine(player, le);
                }
                EvokerNetworking.broadcastCasting(player, 1);
            }
        }
    }

    private static void commandNearbyAllies(ServerPlayerEntity player, LivingEntity target) {
        Box box = player.getBoundingBox().expand(40.0);
        for (MobEntity mob : player.getEntityWorld()
                .getEntitiesByClass(MobEntity.class, box,
                        m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
            if (mob instanceof WitchEntity witch) {
                witch.setTarget(null);
                witch.getNavigation().stop();
                continue;
            }
            mob.setTarget(target);
        }

        if (CaptainState.isCaptain(player)) {
            CaptainState.setCommandedTarget(player.getUuid(), target.getUuid());
        }
    }

    private static void rallyNearbyAllies(ServerPlayerEntity player, LivingEntity target) {
        if (target instanceof MobEntity mobTarget) {
            ZombieTargetingState.markProvoked(mobTarget.getUuid(), player.getUuid());
            mobTarget.setAttacker(player);
        }

        commandNearbyAllies(player, target);
    }

    public static void onFangHit(ServerPlayerEntity player, LivingEntity target) {
        if (!isEvokerPossessing(player)) return;
        if (PillagerPossessionController.isIllagerAlly(target)) return;
        if (!CaptainState.isCaptain(player)) return;
        rallyNearbyAllies(player, target);
    }

    public static void handleVexSummonPacket(ServerPlayerEntity player) {
        if (!isEvokerPossessing(player)) return;
        long lastVex = LAST_VEX_TICK.getOrDefault(player.getUuid(), -200L);
        if (player.age - lastVex < 100) return;

        // Check vex target — from left-click OR from damage event
        UUID targetUuid = VEX_TARGET.get(player.getUuid());
        Long targetTick = VEX_TARGET_TICK.get(player.getUuid());
        if (targetUuid == null || targetTick == null || player.age - targetTick > 100) {
            targetUuid = LAST_ATTACKER.get(player.getUuid());
        }
        if (targetUuid == null) return;

        Entity targetEntity = player.getEntityWorld().getEntity(targetUuid);
        if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()) return;

        LAST_VEX_TICK.put(player.getUuid(), (long) player.age);
        summonVexes(player, target, 3);
        EvokerNetworking.broadcastCasting(player, 2);
    }

    // ── Fang spawning ─────────────────────────────────────────────────────────

    /** Vanilla-style fang circle: 8 inner ring then 8 outer ring, staggered warmup. */
    private static void spawnFangCircle(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        double x = player.getX(), y = player.getY(), z = player.getZ();
        // Vanilla evoker: inner ring radius=1.25, outer=2.5, staggered by ring
        for (int ring = 0; ring < 2; ring++) {
            double radius = ring == 0 ? 1.25 : 2.5;
            int baseWarmup = ring * 8; // outer ring appears after inner
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * 2 * Math.PI + (ring * Math.PI / 8.0); // offset outer ring
                double fx = x + Math.cos(angle) * radius;
                double fz = z + Math.sin(angle) * radius;
                spawnFang(world, fx, y, fz, 0f, baseWarmup + i, player);
            }
        }
        world.playSound(null, x, y, z, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void spawnFangLine(ServerPlayerEntity player, LivingEntity target) {
        ServerWorld world = player.getEntityWorld();
        Vec3d start = player.getEntityPos().add(0, 0.5, 0);
        Vec3d end = target.getEntityPos().add(0, 0.5, 0);
        Vec3d dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        int count = (int) Math.min(dist / 1.5, 14);
        for (int i = 0; i < count; i++) {
            double t = (i + 1) * 1.5;
            Vec3d pos = start.add(dir.multiply(t));
            spawnFang(world, pos.x, pos.y - 0.5, pos.z, 0f, i, player);
        }
        spawnFang(world, target.getX(), target.getY(), target.getZ(), 0f, count, player);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void spawnFang(ServerWorld world, double x, double y, double z,
                                  float yaw, int warmup, ServerPlayerEntity owner) {
        EvokerFangsEntity fang = new EvokerFangsEntity(world, x, y, z, yaw, warmup, owner);
        world.spawnEntity(fang);
    }

    // ── Vex summoning ─────────────────────────────────────────────────────────

    private static void summonVexes(ServerPlayerEntity player, LivingEntity target, int count) {
        ServerWorld world = player.getEntityWorld();
        Set<UUID> playerVexes = SUMMONED_VEXES.computeIfAbsent(player.getUuid(), k -> ConcurrentHashMap.newKeySet());
        net.minecraft.util.math.random.Random rng = player.getRandom();
        for (int i = 0; i < count; i++) {
            VexEntity vex = EntityType.VEX.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (vex == null) continue;
            double ox = player.getX() + (rng.nextDouble() - 0.5) * 3;
            double oy = player.getY() + rng.nextDouble() * 2;
            double oz = player.getZ() + (rng.nextDouble() - 0.5) * 3;
            vex.refreshPositionAndAngles(ox, oy, oz, rng.nextFloat() * 360f, 0f);
            vex.setTarget(target);
            vex.setLifeTicks(60 * 20);
            // Equip with iron sword
            vex.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_SWORD));
            world.spawnEntity(vex);
            playerVexes.add(vex.getUuid());
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    static void redirectVexes(ServerPlayerEntity player, LivingEntity target) {
        Set<UUID> vexUuids = SUMMONED_VEXES.get(player.getUuid());
        if (vexUuids == null || vexUuids.isEmpty()) return;
        ServerWorld world = player.getEntityWorld();
        Set<UUID> dead = new HashSet<>();
        for (UUID vexUuid : vexUuids) {
            Entity e = world.getEntity(vexUuid);
            if (e instanceof VexEntity vex && vex.isAlive()) vex.setTarget(target);
            else dead.add(vexUuid);
        }
        vexUuids.removeAll(dead);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isEvokerPossessing(player)) return;

        lockHunger(player);
        ensureEvokerItems(player);

        if (player.age % 100 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EVOKER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (player.age % 10 == 0) {
            PillagerPossessionController.scarVillagersPublic(player);
        }

        Long targetTick = VEX_TARGET_TICK.get(player.getUuid());
        if (targetTick != null && player.age - targetTick > 100) {
            VEX_TARGET.remove(player.getUuid());
            VEX_TARGET_TICK.remove(player.getUuid());
            EvokerNetworking.broadcastTarget(player, null);
        }

        if (player.age % 20 == 0) cleanDeadVexes(player);
    }

    private static void cleanDeadVexes(ServerPlayerEntity player) {
        Set<UUID> vexUuids = SUMMONED_VEXES.get(player.getUuid());
        if (vexUuids == null || vexUuids.isEmpty()) return;
        ServerWorld world = player.getEntityWorld();
        vexUuids.removeIf(uuid -> {
            Entity e = world.getEntity(uuid);
            return !(e instanceof VexEntity) || !e.isAlive();
        });
    }

    // ── Item management ───────────────────────────────────────────────────────

    private static void ensureEvokerItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) return;

        ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
        ItemStack banner = PillagerPossessionController.createOminousBannerPublic(player);
        ItemStack horn = new ItemStack(Items.GOAT_HORN);

        giveToSlot(player, totem, 0);
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

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Unpossess cleanup ─────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ITEMS_GIVEN.remove(uuid); LAST_FANG_TICK.remove(uuid); LAST_VEX_TICK.remove(uuid);
        VEX_TARGET.remove(uuid); VEX_TARGET_TICK.remove(uuid); SUMMONED_VEXES.remove(uuid);
        LAST_ATTACKER.remove(uuid); GIVEN_BANNER.remove(uuid);
        removeItemType(player, Items.TOTEM_OF_UNDYING);
        removeItemType(player, Items.GOAT_HORN);
        net.minecraft.registry.Registries.ITEM.stream().forEach(item -> {
            var id = net.minecraft.registry.Registries.ITEM.getId(item);
            if (id != null && (id.getPath().endsWith("_banner") || id.getPath().equals("ominous_banner")))
                removeItemType(player, item);
        });
        EvokerNetworking.broadcastTarget(player, null);
        EvokerNetworking.broadcastCasting(player, 0);
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--)
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
    }

    public static void onUnpossessUuid(UUID uuid) {
        ITEMS_GIVEN.remove(uuid); LAST_FANG_TICK.remove(uuid); LAST_VEX_TICK.remove(uuid);
        VEX_TARGET.remove(uuid); VEX_TARGET_TICK.remove(uuid); SUMMONED_VEXES.remove(uuid);
        LAST_ATTACKER.remove(uuid); GIVEN_BANNER.remove(uuid);
    }
}





