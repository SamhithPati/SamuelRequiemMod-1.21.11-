package net.sam.samrequiemmod.possession.illager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.raid.Raid;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles raid mechanics and custom raid-win celebration for illager captains.
 */
public final class RaidHandler {

    private static final long CELEBRATION_DURATION_TICKS = 400L;
    private static final int CELEBRATION_JUMP_CHECK_INTERVAL = 10;
    private static final double CELEBRATION_JUMP_VELOCITY = 0.42;
    private static final double RAID_VILLAGER_SEARCH_RADIUS = 96.0;

    /** Captain UUID -> tick when celebration ends. */
    private static final Map<UUID, Long> CELEBRATION_END_TICK = new ConcurrentHashMap<>();

    /** Captain UUID -> whether villagers were nearby last check. */
    private static final Map<UUID, Boolean> HAD_VILLAGERS = new ConcurrentHashMap<>();

    /** Witch UUID -> tick of last potion throw during raid. */
    private static final Map<UUID, Long> RAID_WITCH_COOLDOWN = new ConcurrentHashMap<>();

    private RaidHandler() {}

    /** Returns true if the player is near an occupied village or near villagers. */
    public static boolean isNearVillage(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        if (world.isNearOccupiedPointOfInterest(player.getBlockPos())) {
            return true;
        }

        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class,
                player.getBoundingBox().expand(64),
                Entity::isAlive
        );
        return !villagers.isEmpty();
    }

    /** Starts a raid immediately via the vanilla raid manager. */
    public static void handleRaidStart(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.RAID_OMEN, 600, 0, false, true
        ));

        Raid raid = world.getRaidManager().startRaid(player, player.getBlockPos());
        if (raid != null) {
            world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.EVENT_MOB_EFFECT_RAID_OMEN,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
            );
        }

        player.removeStatusEffect(StatusEffects.RAID_OMEN);
    }

    /** Ticked from the main server loop. */
    public static void tick(ServerPlayerEntity player) {
        if (!RavagerRidingHandler.isIllagerPossessed(player)) return;
        if (!CaptainState.isCaptain(player)) return;

        UUID uuid = player.getUuid();
        if (CELEBRATION_END_TICK.containsKey(uuid)) {
            tickCelebration(player);
            return;
        }

        tickVillagerDeathDetection(player);
    }

    private static void tickRaidWitchDiscipline(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        Box searchBox = Box.of(raid.getCenter().toCenterPos(), RAID_VILLAGER_SEARCH_RADIUS * 2.0, 64.0, RAID_VILLAGER_SEARCH_RADIUS * 2.0);
        for (WitchEntity witch : world.getEntitiesByClass(WitchEntity.class, searchBox, Entity::isAlive)) {
            witch.setTarget(null);
            witch.setAttacker(null);
            witch.getNavigation().stop();
        }
    }

    private static void tickRaidWitchHealing(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;

        ServerWorld world = player.getEntityWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        List<WitchEntity> witches = world.getEntitiesByClass(
                WitchEntity.class,
                player.getBoundingBox().expand(16),
                Entity::isAlive
        );

        long now = player.age;
        for (WitchEntity witch : witches) {
            witch.setTarget(null);
            witch.getNavigation().stop();

            if (player.getHealth() >= player.getMaxHealth()) {
                continue;
            }

            Long lastThrow = RAID_WITCH_COOLDOWN.get(witch.getUuid());
            if (lastThrow != null && now - lastThrow < 60L) continue;

            CaptainHandler.throwRegenPotion(witch, player);
            RAID_WITCH_COOLDOWN.put(witch.getUuid(), now);
            break;
        }
    }

    private static void tickBabyVillagerTargeting(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;

        ServerWorld world = player.getEntityWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        List<VillagerEntity> babyVillagers = getRaidVillagers(world, raid).stream()
                .filter(VillagerEntity::isBaby)
                .toList();
        if (babyVillagers.isEmpty()) return;

        VillagerEntity target = babyVillagers.get(0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class,
                player.getBoundingBox().expand(48),
                m -> PillagerPossessionController.isRallyMob(m)
                        && m.isAlive()
                        && (m.getTarget() == null || !m.getTarget().isAlive())
        )) {
            raider.setTarget(target);
        }
    }

    private static void tickRaiderVillageTargeting(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;

        ServerWorld world = player.getEntityWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) return;

        Box searchBox = Box.of(raid.getCenter().toCenterPos(), RAID_VILLAGER_SEARCH_RADIUS * 2.0, 64.0, RAID_VILLAGER_SEARCH_RADIUS * 2.0);
        List<VillagerEntity> villagers = getRaidVillagers(world, raid);
        if (villagers.isEmpty()) return;

        List<VillagerEntity> babyVillagers = villagers.stream()
                .filter(VillagerEntity::isBaby)
                .toList();

        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class,
                searchBox,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive()
        )) {
            LivingEntity currentTarget = raider.getTarget();
            if (!babyVillagers.isEmpty()) {
                VillagerEntity nearestBabyVillager = findNearestVillager(raider, babyVillagers);
                if (nearestBabyVillager != null && currentTarget != nearestBabyVillager) {
                    raider.setTarget(nearestBabyVillager);
                }
                continue;
            }

            if (currentTarget instanceof VillagerEntity villager && villager.isAlive()) {
                continue;
            }

            VillagerEntity nearestVillager = findNearestVillager(raider, villagers);
            if (nearestVillager != null) {
                raider.setTarget(nearestVillager);
            }
        }
    }

    private static VillagerEntity findNearestVillager(MobEntity raider, List<VillagerEntity> villagers) {
        VillagerEntity nearestVillager = null;
        double nearestDistance = Double.MAX_VALUE;
        for (VillagerEntity villager : villagers) {
            double distance = raider.squaredDistanceTo(villager);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestVillager = villager;
            }
        }
        return nearestVillager;
    }

    private static void tickVillagerDeathDetection(ServerPlayerEntity player) {
        if (player.age % 40 != 0) return;

        UUID uuid = player.getUuid();
        ServerWorld world = player.getEntityWorld();
        Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
        if (raid == null || !raid.isActive()) {
            HAD_VILLAGERS.remove(uuid);
            return;
        }

        List<VillagerEntity> villagers = world.getEntitiesByClass(
                VillagerEntity.class,
                getRaidVillagerSearchBox(raid),
                Entity::isAlive
        );

        boolean hadVillagers = HAD_VILLAGERS.getOrDefault(uuid, false);
        boolean hasVillagers = !villagers.isEmpty();
        HAD_VILLAGERS.put(uuid, hasVillagers);

        if (hadVillagers && !hasVillagers) {
            startCelebration(player, raid);
        }
    }

    private static void startCelebration(ServerPlayerEntity player, Raid raid) {
        UUID uuid = player.getUuid();
        CELEBRATION_END_TICK.put(uuid, (long) player.age + CELEBRATION_DURATION_TICKS);
        HAD_VILLAGERS.put(uuid, false);

        ServerWorld world = player.getEntityWorld();
        if (raid != null && raid.isActive() && !raid.hasStopped()) {
            raid.invalidate();
        }

        Box box = player.getBoundingBox().expand(48.0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class,
                box,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive()
        )) {
            raider.setTarget(null);
            raider.getNavigation().stop();
            if (raider instanceof RaiderEntity raiderEntity) {
                raiderEntity.setCelebrating(true);
            }
        }

        CaptainNetworking.broadcastCelebration(player, true);
        playCelebrationSound(player);
    }

    private static List<VillagerEntity> getRaidVillagers(ServerWorld world, Raid raid) {
        return world.getEntitiesByClass(
                VillagerEntity.class,
                getRaidVillagerSearchBox(raid),
                Entity::isAlive
        );
    }

    private static Box getRaidVillagerSearchBox(Raid raid) {
        BlockPos center = raid.getCenter();
        return Box.of(center.toCenterPos(), RAID_VILLAGER_SEARCH_RADIUS * 2.0, 64.0, RAID_VILLAGER_SEARCH_RADIUS * 2.0);
    }

    private static void tickCelebration(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Long endTick = CELEBRATION_END_TICK.get(uuid);
        if (endTick == null) return;

        if ((long) player.age >= endTick) {
            CELEBRATION_END_TICK.remove(uuid);
            HAD_VILLAGERS.put(uuid, false);
            CaptainNetworking.broadcastCelebration(player, false);

            ServerWorld world = player.getEntityWorld();
            Box box = player.getBoundingBox().expand(48.0);
            for (MobEntity raider : world.getEntitiesByClass(
                    MobEntity.class,
                    box,
                    m -> m instanceof RaiderEntity && m.isAlive()
            )) {
                if (raider instanceof RaiderEntity raiderEntity) {
                    raiderEntity.setCelebrating(false);
                }
            }
            return;
        }

        if (player.age % 20 == 0) {
            ServerWorld world = player.getEntityWorld();
            Box box = player.getBoundingBox().expand(48.0);
            for (MobEntity raider : world.getEntitiesByClass(
                    MobEntity.class,
                    box,
                    m -> PillagerPossessionController.isRallyMob(m) && m.isAlive()
            )) {
                raider.setTarget(null);
                raider.getNavigation().stop();
                if (raider instanceof RaiderEntity raiderEntity) {
                    raiderEntity.setCelebrating(true);
                }
            }
        }

        if (player.age % CELEBRATION_JUMP_CHECK_INTERVAL == 0) {
            tickCelebrationJumps(player);
        }

        if (player.age % 60 == 0) {
            playCelebrationSound(player);
            playRaiderCelebrationSounds(player);
        }
    }

    private static void playCelebrationSound(ServerPlayerEntity player) {
        EntityType<?> possType = PossessionManager.getPossessedType(player);
        if (possType == EntityType.PILLAGER) {
            player.getEntityWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        } else if (possType == EntityType.VINDICATOR) {
            player.getEntityWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        } else if (possType == EntityType.EVOKER) {
            player.getEntityWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EVOKER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        } else if (possType == EntityType.WITCH) {
            player.getEntityWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        } else {
            player.getEntityWorld().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.0f
            );
        }
    }

    private static void playRaiderCelebrationSounds(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box box = player.getBoundingBox().expand(48.0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class,
                box,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive()
        )) {
            if (raider instanceof PillagerEntity) {
                world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                        SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
            } else if (raider instanceof VindicatorEntity) {
                world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                        SoundEvents.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
            } else if (raider instanceof EvokerEntity) {
                world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                        SoundEvents.ENTITY_EVOKER_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
            } else if (raider instanceof WitchEntity) {
                world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                        SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
            } else if (raider instanceof RavagerEntity) {
                world.playSound(null, raider.getX(), raider.getY(), raider.getZ(),
                        SoundEvents.ENTITY_RAVAGER_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
            }
        }
    }

    private static void tickCelebrationJumps(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        if (player.isOnGround() && player.getRandom().nextFloat() < 0.35f) {
            player.setVelocity(player.getVelocity().x, CELEBRATION_JUMP_VELOCITY, player.getVelocity().z);
            player.velocityDirty = true;
        }

        Box box = player.getBoundingBox().expand(48.0);
        for (MobEntity raider : world.getEntitiesByClass(
                MobEntity.class,
                box,
                m -> PillagerPossessionController.isRallyMob(m) && m.isAlive() && m.isOnGround()
        )) {
            if (raider.getRandom().nextFloat() >= 0.35f) continue;
            raider.setVelocity(raider.getVelocity().x, CELEBRATION_JUMP_VELOCITY, raider.getVelocity().z);
            raider.velocityDirty = true;
        }
    }

    public static boolean isCelebrating(UUID playerUuid) {
        return CELEBRATION_END_TICK.containsKey(playerUuid);
    }

    public static void clearAll(UUID playerUuid) {
        CELEBRATION_END_TICK.remove(playerUuid);
        HAD_VILLAGERS.remove(playerUuid);
    }
}
