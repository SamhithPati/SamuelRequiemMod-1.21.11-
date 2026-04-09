package net.sam.samrequiemmod.possession.trader;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionHurtSoundHelper;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.villager.VillagerPossessionController;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WanderingTraderPossessionController {

    private static final int THREAT_INVIS_DURATION = 45 * 20;
    private static final int DRINK_VISUAL_TICKS = 32;
    private static final int LLAMA_COOLDOWN_TICKS = 15 * 20;
    private static final int LLAMA_ATTACK_COOLDOWN_TICKS = 20;
    private static final int LLAMA_SUMMON_DEBOUNCE_TICKS = 8;
    private static final Map<UUID, Set<UUID>> SUMMONED_LLAMAS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LLAMA_COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LLAMA_ATTACK_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LLAMA_INPUT_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> LLAMA_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LLAMA_TARGET_UNTIL = new ConcurrentHashMap<>();

    private WanderingTraderPossessionController() {
    }

    public static boolean isWanderingTraderPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WANDERING_TRADER;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isWanderingTraderPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWanderingTraderPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_WANDERING_TRADER_HURT, 1.0f);

            if (source.getAttacker() instanceof LivingEntity attacker) {
                rallyNearbyGolems(player, attacker);
                rallySummonedLlamas(player, attacker);
                if (attacker instanceof MobEntity mob && !VillagerPossessionController.isVillagerAlwaysHostile(mob)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                if (attacker instanceof net.minecraft.entity.player.PlayerEntity) {
                    triggerThreatInvisibility(player);
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWanderingTraderPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WANDERING_TRADER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
            despawnLlamas(player, false);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isWanderingTraderPossessing(player)) return;
        lockHunger(player);
        handleAmbientSound(player);
        handleThreatInvisibility(player);
        tickSummonedLlamas(player);
    }

    public static void handleSummonKey(ServerPlayerEntity player) {
        if (!isWanderingTraderPossessing(player)) return;
        int inputUntil = LLAMA_INPUT_COOLDOWNS.getOrDefault(player.getUuid(), 0);
        if (player.age < inputUntil) return;
        LLAMA_INPUT_COOLDOWNS.put(player.getUuid(), player.age + LLAMA_SUMMON_DEBOUNCE_TICKS);

        Set<UUID> llamaIds = getTrackedLlamaIds(player);
        cleanupDeadLlamas(player, llamaIds);
        if (!llamaIds.isEmpty()) {
            despawnLlamas(player, true);
            return;
        }

        long cooldownUntil = LLAMA_COOLDOWN_UNTIL.getOrDefault(player.getUuid(), 0L);
        if (player.age < cooldownUntil) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            TraderLlamaEntity llama = new TraderLlamaEntity(EntityType.TRADER_LLAMA, player.getEntityWorld());
            Vec3d offset = new Vec3d((i == 0 ? 1.5 : -1.5), 0.0, 1.5);
            llama.refreshPositionAndAngles(
                    player.getX() + offset.x,
                    player.getY(),
                    player.getZ() + offset.z,
                    player.getYaw(),
                    0.0f
            );
            llama.setPersistent();
            llama.setTame(true);
            llama.addCommandTag(ownerTag(player.getUuid()));
            llama.addCommandTag("samreq_wandering_trader_llama");
            player.getEntityWorld().spawnEntity(llama);
            llamaIds.add(llama.getUuid());
        }

        SUMMONED_LLAMAS.put(player.getUuid(), llamaIds);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WANDERING_TRADER_REAPPEARED, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        despawnLlamas(player, false);
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        WanderingTraderState.clear(uuid);
        SUMMONED_LLAMAS.remove(uuid);
        LLAMA_COOLDOWN_UNTIL.remove(uuid);
        LLAMA_INPUT_COOLDOWNS.remove(uuid);
        LLAMA_TARGETS.remove(uuid);
        LLAMA_TARGET_UNTIL.remove(uuid);
    }

    private static void handleThreatInvisibility(ServerPlayerEntity player) {
        long age = player.age;
        boolean night = isNight(player.getEntityWorld());
        boolean nearbyThreat = hasNearbyTraderThreat(player);
        long tempUntil = WanderingTraderState.getServerTempInvisUntil(player.getUuid());
        boolean nightMode = WanderingTraderState.isServerNightInvis(player.getUuid());

        if (night && !nightMode) {
            WanderingTraderState.setServerNightInvis(player.getUuid(), true);
            WanderingTraderState.setServerTempInvisUntil(player.getUuid(), 0L);
            drinkInvisibility(player, Integer.MAX_VALUE);
            return;
        }

        if (!night && nightMode) {
            WanderingTraderState.setServerNightInvis(player.getUuid(), false);
            removeInvisibilityWithMilk(player);
            return;
        }

        if (!nightMode && nearbyThreat && age >= tempUntil && !player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            triggerThreatInvisibility(player);
            return;
        }

        if (!nightMode && tempUntil > 0L && age >= tempUntil) {
            WanderingTraderState.setServerTempInvisUntil(player.getUuid(), 0L);
            if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                removeInvisibilityWithMilk(player);
            }
        }
    }

    private static void triggerThreatInvisibility(ServerPlayerEntity player) {
        WanderingTraderState.setServerNightInvis(player.getUuid(), false);
        WanderingTraderState.setServerTempInvisUntil(player.getUuid(), player.age + THREAT_INVIS_DURATION);
        drinkInvisibility(player, THREAT_INVIS_DURATION);
    }

    private static void drinkInvisibility(ServerPlayerEntity player, int duration) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration, 0, false, false, false));
        WanderingTraderNetworking.broadcastDrink(player, WanderingTraderState.DRINK_INVIS, DRINK_VISUAL_TICKS);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WANDERING_TRADER_DRINK_POTION, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void removeInvisibilityWithMilk(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        WanderingTraderNetworking.broadcastDrink(player, WanderingTraderState.DRINK_MILK, DRINK_VISUAL_TICKS);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WANDERING_TRADER_DRINK_MILK, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static boolean hasNearbyTraderThreat(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(10.0);
        for (MobEntity mob : player.getEntityWorld().getEntitiesByClass(MobEntity.class, box, LivingEntity::isAlive)) {
            if (mob instanceof ZombieEntity && !(mob instanceof net.minecraft.entity.mob.ZombifiedPiglinEntity)) {
                return true;
            }
            if (mob instanceof IllagerEntity || mob instanceof RavagerEntity) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNight(World world) {
        return !world.isDay();
    }

    private static void tickSummonedLlamas(ServerPlayerEntity player) {
        Set<UUID> llamaIds = getTrackedLlamaIds(player);
        if (llamaIds == null || llamaIds.isEmpty()) return;

        cleanupDeadLlamas(player, llamaIds);
        if (llamaIds.isEmpty()) {
            return;
        }
        UUID ownerUuid = player.getUuid();
        UUID targetUuid = LLAMA_TARGETS.get(ownerUuid);
        int targetUntil = LLAMA_TARGET_UNTIL.getOrDefault(ownerUuid, 0);
        LivingEntity sharedTarget = null;
        if (targetUuid != null && player.age < targetUntil) {
            Entity targetEntity = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getEntity(targetUuid);
            if (targetEntity instanceof LivingEntity living && living.isAlive()) {
                sharedTarget = living;
            } else {
                LLAMA_TARGETS.remove(ownerUuid);
                LLAMA_TARGET_UNTIL.remove(ownerUuid);
            }
        } else {
            LLAMA_TARGETS.remove(ownerUuid);
            LLAMA_TARGET_UNTIL.remove(ownerUuid);
        }

        int index = 0;
        for (UUID llamaUuid : new HashSet<>(llamaIds)) {
            Entity entity = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getEntity(llamaUuid);
            if (!(entity instanceof TraderLlamaEntity llama) || !llama.isAlive()) continue;

            double distanceSq = llama.squaredDistanceTo(player);
            if (distanceSq > 144.0) {
                teleportNearOwner(llama, player);
                distanceSq = llama.squaredDistanceTo(player);
            }

            if (sharedTarget != null) {
                llama.setTarget(sharedTarget);
                llama.setAttacker(sharedTarget);
                driveLlamaTowardTarget(llama, sharedTarget);
                tryLlamaAttack(player, llama, sharedTarget);
            } else {
                llama.setTarget(null);
                driveLlamaTowardOwner(llama, player, index++);
            }
        }
    }

    private static void driveLlamaTowardOwner(TraderLlamaEntity llama, ServerPlayerEntity player, int index) {
        double side = index % 2 == 0 ? 1.4 : -1.4;
        double back = index < 2 ? -1.6 : -2.4;
        double yawRad = Math.toRadians(player.getYaw());
        double desiredX = player.getX() - Math.sin(yawRad) * back + Math.cos(yawRad) * side;
        double desiredZ = player.getZ() + Math.cos(yawRad) * back + Math.sin(yawRad) * side;
        double dx = desiredX - llama.getX();
        double dz = desiredZ - llama.getZ();
        double horizontalSq = dx * dx + dz * dz;
        if (horizontalSq < 0.64) {
            llama.setVelocity(llama.getVelocity().multiply(0.5, 0.8, 0.5));
            return;
        }
        double scale = 0.22 / Math.sqrt(horizontalSq);
        double vx = dx * scale;
        double vz = dz * scale;
        double dy = (player.getY() - llama.getY()) * 0.08;
        llama.setVelocity(vx, dy, vz);
        llama.velocityDirty = true;
    }

    private static void driveLlamaTowardTarget(TraderLlamaEntity llama, LivingEntity target) {
        double dx = target.getX() - llama.getX();
        double dz = target.getZ() - llama.getZ();
        double horizontalSq = dx * dx + dz * dz;
        if (horizontalSq < 1.44) {
            llama.setVelocity(llama.getVelocity().multiply(0.4, 0.8, 0.4));
            return;
        }
        double scale = 0.28 / Math.sqrt(horizontalSq);
        double vx = dx * scale;
        double vz = dz * scale;
        double dy = (target.getY() - llama.getY()) * 0.08;
        llama.setVelocity(vx, dy, vz);
        llama.velocityDirty = true;
    }

    private static void tryLlamaAttack(ServerPlayerEntity owner, TraderLlamaEntity llama, LivingEntity target) {
        if (!target.isAlive()) return;
        if (llama.squaredDistanceTo(target) > 100.0) return;
        if (!llama.getVisibilityCache().canSee(target)) return;

        UUID llamaUuid = llama.getUuid();
        int cooldownUntil = LLAMA_ATTACK_COOLDOWNS.getOrDefault(llamaUuid, 0);
        if (owner.age < cooldownUntil) return;

        LLAMA_ATTACK_COOLDOWNS.put(llamaUuid, owner.age + LLAMA_ATTACK_COOLDOWN_TICKS);
        target.timeUntilRegen = 0;
        target.damage((net.minecraft.server.world.ServerWorld) owner.getEntityWorld(), owner.getDamageSources().mobAttack(llama), 3.0f);
        Vec3d push = new Vec3d(
                target.getX() - llama.getX(),
                0.0,
                target.getZ() - llama.getZ()
        );
        if (push.lengthSquared() > 1.0E-6) {
            push = push.normalize().multiply(0.35).add(0.0, 0.12, 0.0);
            target.setVelocity(target.getVelocity().add(push));
        }
        owner.getEntityWorld().playSound(
                null,
                llama.getX(),
                llama.getY(),
                llama.getZ(),
                SoundEvents.ENTITY_LLAMA_SPIT,
                SoundCategory.NEUTRAL,
                1.0f,
                1.0f
        );
    }

    private static void teleportNearOwner(TraderLlamaEntity llama, ServerPlayerEntity player) {
        BlockPos base = player.getBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = base.add(dx, 0, dz);
                if (player.getEntityWorld().getBlockState(pos).isAir()
                        && player.getEntityWorld().getBlockState(pos.up()).isAir()) {
                    llama.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), 0.0f);
                    llama.getNavigation().stop();
                    return;
                }
            }
        }
        llama.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
        llama.getNavigation().stop();
    }

    private static void cleanupDeadLlamas(ServerPlayerEntity player, Set<UUID> llamaIds) {
        boolean hadActive = !llamaIds.isEmpty();
        llamaIds.removeIf(uuid -> {
            Entity entity = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getEntity(uuid);
            if (!(entity instanceof TraderLlamaEntity llama) || !llama.isAlive()) {
                LLAMA_ATTACK_COOLDOWNS.remove(uuid);
                return true;
            }
            return false;
        });
        if (llamaIds.isEmpty()) {
            SUMMONED_LLAMAS.remove(player.getUuid());
            LLAMA_TARGETS.remove(player.getUuid());
            LLAMA_TARGET_UNTIL.remove(player.getUuid());
            if (hadActive) {
                LLAMA_COOLDOWN_UNTIL.put(player.getUuid(), (long) player.age + LLAMA_COOLDOWN_TICKS);
            }
        } else {
            SUMMONED_LLAMAS.put(player.getUuid(), llamaIds);
        }
    }

    private static void rallyNearbyGolems(ServerPlayerEntity player, LivingEntity attacker) {
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(
                IronGolemEntity.class, player.getBoundingBox().expand(30.0), IronGolemEntity::isAlive)) {
            golem.setTarget(attacker);
            golem.setAngryAt(LazyEntityReference.of(attacker));
        }
    }

    private static void rallySummonedLlamas(ServerPlayerEntity player, LivingEntity attacker) {
        Set<UUID> llamaIds = getTrackedLlamaIds(player);
        if (llamaIds == null) return;
        LLAMA_TARGETS.put(player.getUuid(), attacker.getUuid());
        LLAMA_TARGET_UNTIL.put(player.getUuid(), player.age + 20 * 20);
        for (UUID llamaUuid : new HashSet<>(llamaIds)) {
            Entity entity = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getEntity(llamaUuid);
            if (entity instanceof TraderLlamaEntity llama && llama.isAlive()) {
                llama.setTarget(attacker);
                llama.setAttacker(attacker);
                driveLlamaTowardTarget(llama, attacker);
            }
        }
    }

    private static void despawnLlamas(ServerPlayerEntity player, boolean startCooldown) {
        Set<UUID> llamaIds = getTrackedLlamaIds(player);
        SUMMONED_LLAMAS.remove(player.getUuid());
        if (llamaIds == null) return;
        for (UUID llamaUuid : llamaIds) {
            Entity entity = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getEntity(llamaUuid);
            LLAMA_ATTACK_COOLDOWNS.remove(llamaUuid);
            if (entity instanceof TraderLlamaEntity llama && llama.isAlive()) {
                llama.discard();
            }
        }
        LLAMA_TARGETS.remove(player.getUuid());
        LLAMA_TARGET_UNTIL.remove(player.getUuid());
        if (startCooldown) {
            LLAMA_COOLDOWN_UNTIL.put(player.getUuid(), (long) player.age + LLAMA_COOLDOWN_TICKS);
        }
    }

    private static Set<UUID> getTrackedLlamaIds(ServerPlayerEntity player) {
        Set<UUID> llamaIds = ConcurrentHashMap.newKeySet();
        String ownerTag = ownerTag(player.getUuid());
        for (TraderLlamaEntity llama : player.getEntityWorld().getEntitiesByClass(
                TraderLlamaEntity.class,
                player.getBoundingBox().expand(256.0),
                entity -> entity.isAlive() && entity.getCommandTags().contains(ownerTag))) {
            llamaIds.add(llama.getUuid());
        }
        if (!llamaIds.isEmpty()) {
            SUMMONED_LLAMAS.put(player.getUuid(), llamaIds);
            return llamaIds;
        }
        return SUMMONED_LLAMAS.computeIfAbsent(player.getUuid(), uuid -> ConcurrentHashMap.newKeySet());
    }

    private static String ownerTag(UUID uuid) {
        return "samreq_trader_llama_owner_" + uuid;
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WANDERING_TRADER_AMBIENT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
    }
}
