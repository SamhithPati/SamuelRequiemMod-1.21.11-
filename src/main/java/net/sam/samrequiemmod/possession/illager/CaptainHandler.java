package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.IllusionerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all captain mechanics:
 * - Banner equip via right-click
 * - Goat horn: summon 4 pillagers OR start raid near village
 * - Attack rally (captain hits mob → all nearby illagers/ravagers attack it)
 * - Y-key recruit/dismiss of followers, despawn for summoned pillagers
 * - Follower AI: follow captain, attack commanded targets, return after kill
 * - Witch followers: throw regen potions at damaged captain/followers
 * - Arrow immunity for summoned pillagers
 */
public final class CaptainHandler {

    private CaptainHandler() {}

    /** Witch UUID -> tick of last potion throw (3-second cooldown). */
    private static final Map<UUID, Long> WITCH_POTION_COOLDOWN = new ConcurrentHashMap<>();

    public static void register() {

        // ── Banner equip: right-click on block ────────────────────────────────
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!RavagerRidingHandler.isIllagerPossessed(sp)) return ActionResult.PASS;
            ItemStack held = player.getStackInHand(hand);
            if (held.isEmpty() || !(held.getItem() instanceof BannerItem)) return ActionResult.PASS;
            ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
            if (!head.isEmpty()) return ActionResult.PASS;
            player.equipStack(EquipmentSlot.HEAD, held.copyWithCount(1));
            held.decrement(1);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        });

        // ── Banner equip: right-click in air ──────────────────────────────────
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getStackInHand(hand);
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!RavagerRidingHandler.isIllagerPossessed(sp)) return ActionResult.PASS;

            // ── Goat horn interception ────────────────────────────────────────
            if (held.isOf(Items.GOAT_HORN)) {
                if (!CaptainState.isCaptain(sp)) return ActionResult.PASS;
                handleGoatHornUse(sp);
                return ActionResult.SUCCESS;
            }

            if (held.isEmpty() || !(held.getItem() instanceof BannerItem)) return ActionResult.PASS;
            ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
            if (!head.isEmpty()) return ActionResult.PASS;
            player.equipStack(EquipmentSlot.HEAD, held.copyWithCount(1));
            held.decrement(1);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        });

        // ── Goat horn on block: also intercept ────────────────────────────────
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!RavagerRidingHandler.isIllagerPossessed(sp)) return ActionResult.PASS;
            ItemStack held = player.getStackInHand(hand);
            if (!held.isOf(Items.GOAT_HORN)) return ActionResult.PASS;
            if (!CaptainState.isCaptain(sp)) return ActionResult.PASS;
            handleGoatHornUse(sp);
            return ActionResult.SUCCESS;
        });

        // ── Captain attack rally: when captain damages a non-ally entity ──────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(source.getAttacker() instanceof ServerPlayerEntity captain)) return true;
            if (!CaptainState.isCaptain(captain)) return true;
            if (!(entity instanceof LivingEntity target)) return true;
            if (PillagerPossessionController.isIllagerAlly(entity)) return true;

            // Rally ALL nearby illagers and ravagers (40 block radius)
            Box box = captain.getBoundingBox().expand(60.0);
            for (MobEntity mob : captain.getEntityWorld()
                    .getEntitiesByClass(MobEntity.class, box,
                            m -> PillagerPossessionController.isRallyMob(m) && m.isAlive())) {
                if (mob instanceof WitchEntity witch) {
                    witch.setTarget(null);
                    witch.getNavigation().stop();
                    continue;
                }
                mob.setTarget(target);
            }

            // Set commanded target for followers specifically
            CaptainState.setCommandedTarget(captain.getUuid(), entity.getUuid());
            return true;
        });

        // ── Arrow immunity: summoned pillager arrows don't hurt captain or each other ──
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity attacker = source.getAttacker();
            if (attacker == null) return true;
            UUID attackerUuid = attacker.getUuid();

            if (!SummonedPillagerState.isSummoned(attackerUuid)) return true;

            UUID captainUuid = SummonedPillagerState.getCaptain(attackerUuid);
            if (captainUuid == null) return true;

            // Block damage to the captain player
            if (entity instanceof ServerPlayerEntity player && player.getUuid().equals(captainUuid)) {
                return false;
            }

            // Block damage to other summoned pillagers of the same captain
            if (SummonedPillagerState.isSummonedBy(captainUuid, entity.getUuid())) {
                return false;
            }

            return true;
        });
    }

    // ── Goat horn usage ───────────────────────────────────────────────────────

    private static void handleGoatHornUse(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        // Play goat horn sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GOAT_HORN_SOUNDS.get(0), SoundCategory.PLAYERS, 3.0f, 1.0f);

        // If near a village, start a raid instead of summoning
        if (RaidHandler.isNearVillage(player)) {
            RaidHandler.handleRaidStart(player);
            return;
        }

        // Check cooldown
        if (SummonedPillagerState.isOnCooldown(player.getUuid(), player.age)) {
            // Play a fail sound to indicate cooldown
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0f, 0.5f);
            return;
        }

        // Don't summon if already have summoned pillagers
        if (SummonedPillagerState.hasSummoned(player.getUuid())) {
            return;
        }

        // Spawn 4 pillagers around the player
        summonPillagers(player);
    }

    private static void summonPillagers(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        UUID captainUuid = player.getUuid();

        for (int i = 0; i < 4; i++) {
            PillagerEntity pillager = EntityType.PILLAGER.create(world, SpawnReason.MOB_SUMMONED);
            if (pillager == null) continue;

            // Position in a circle around the player (3-5 blocks out)
            double angle = (Math.PI * 2.0 / 4) * i;
            double radius = 3.0 + player.getRandom().nextDouble() * 2.0;
            double spawnX = player.getX() + Math.cos(angle) * radius;
            double spawnZ = player.getZ() + Math.sin(angle) * radius;

            // Find valid Y position
            BlockPos spawnPos = BlockPos.ofFloored(spawnX, player.getY(), spawnZ);
            // Try to find ground
            for (int y = 0; y < 5; y++) {
                BlockPos check = spawnPos.up(y);
                if (world.isSpaceEmpty(pillager, pillager.getType().getDimensions().getBoxAt(
                        check.getX() + 0.5, check.getY(), check.getZ() + 0.5))) {
                    spawnPos = check;
                    break;
                }
            }

            pillager.refreshPositionAndAngles(
                    spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    player.getYaw() + (90 * i), 0.0f);
            pillager.setPersistent();

            // Equip with crossbow
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            pillager.equipStack(EquipmentSlot.MAINHAND, crossbow);

            world.spawnEntity(pillager);

            // Register as follower and summoned
            UUID pillagerUuid = pillager.getUuid();
            CaptainState.addFollower(captainUuid, pillagerUuid);
            SummonedPillagerState.addSummoned(captainUuid, pillagerUuid);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PILLAGER_CELEBRATE, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    // ── Recruit / dismiss handler ─────────────────────────────────────────────

    public static void handleRecruitPacket(ServerPlayerEntity player, UUID targetUuid) {
        if (!CaptainState.isCaptain(player)) return;
        Entity target = player.getEntityWorld().getEntity(targetUuid);
        if (target == null || !target.isAlive()) return;

        // Must be within 6 blocks
        if (player.squaredDistanceTo(target) > 36.0) return;

        UUID captainUuid = player.getUuid();
        UUID mobUuid = target.getUuid();

        // If it's a summoned pillager, despawn it entirely
        if (SummonedPillagerState.isSummonedBy(captainUuid, mobUuid)) {
            SummonedPillagerState.removeSummoned(captainUuid, mobUuid);
            CaptainState.removeFollower(captainUuid, mobUuid);
            target.discard();
            player.getEntityWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.6f, 1.2f);
            // Check if all summoned are gone → start cooldown
            SummonedPillagerState.checkAndStartCooldown(captainUuid, player.age);
            return;
        }

        boolean canRecruit = target instanceof PillagerEntity
                || target instanceof VindicatorEntity
                || target instanceof EvokerEntity
                || target instanceof IllusionerEntity
                || target instanceof RavagerEntity
                || target instanceof WitchEntity;
        if (!canRecruit) return;

        if (CaptainState.isFollowingCaptain(captainUuid, mobUuid)) {
            // Already following — dismiss
            CaptainState.removeFollower(captainUuid, mobUuid);
            WITCH_POTION_COOLDOWN.remove(mobUuid);
            player.getEntityWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.6f, 0.6f);
        } else {
            // Not following — recruit
            CaptainState.addFollower(captainUuid, mobUuid);
            player.getEntityWorld().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 1.0f, 1.2f);
        }
    }

    // ── Server tick (called from SamuelRequiemMod) ────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!RavagerRidingHandler.isIllagerPossessed(player)) return;

        boolean isCaptain = CaptainState.isCaptain(player);
        boolean wasCaptain = CaptainState.WAS_CAPTAIN.contains(player.getUuid());

        if (wasCaptain && !isCaptain) {
            // Lost captain status (banner removed) — dismiss all followers and despawn summoned
            dismissAllFollowers(player);
        }

        if (isCaptain) {
            CaptainState.WAS_CAPTAIN.add(player.getUuid());
            tickFollowers(player);
        } else {
            CaptainState.WAS_CAPTAIN.remove(player.getUuid());
        }
    }

    private static void dismissAllFollowers(ServerPlayerEntity player) {
        // Despawn all summoned pillagers first
        SummonedPillagerState.despawnAll(player.getUuid(), player.getEntityWorld());

        Set<UUID> followers = CaptainState.getFollowers(player.getUuid());
        for (UUID f : new HashSet<>(followers)) {
            WITCH_POTION_COOLDOWN.remove(f);
            // Clear the follower's target so it stops chasing
            Entity e = player.getEntityWorld().getEntity(f);
            if (e instanceof MobEntity mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        CaptainState.clearAll(player.getUuid());
    }

    // ── Follower tick logic ───────────────────────────────────────────────────

    private static void tickFollowers(ServerPlayerEntity captain) {
        UUID captainUuid = captain.getUuid();
        Set<UUID> followers = CaptainState.getFollowers(captainUuid);
        if (followers.isEmpty()) return;

        // During an active raid, let vanilla raid AI and RaidHandler drive raider behavior.
        // Our custom follow/command logic can otherwise pull raiders out of raid aggression.
        if (hasActiveRaidNearby(captain)) {
            CaptainState.clearCommandedTarget(captainUuid);
            return;
        }

        // Check if commanded target is still alive
        UUID targetUuid = CaptainState.getCommandedTarget(captainUuid);
        if (targetUuid != null) {
            Entity target = captain.getEntityWorld().getEntity(targetUuid);
            if (target == null || !target.isAlive() || !(target instanceof LivingEntity)) {
                CaptainState.clearCommandedTarget(captainUuid);
                targetUuid = null;
            }
        }

        for (UUID followerUuid : new HashSet<>(followers)) {
            Entity entity = captain.getEntityWorld().getEntity(followerUuid);
            if (entity == null || !entity.isAlive()) {
                CaptainState.removeFollower(captainUuid, followerUuid);
                SummonedPillagerState.removeSummoned(captainUuid, followerUuid);
                WITCH_POTION_COOLDOWN.remove(followerUuid);
                // Check if all summoned pillagers are gone → start cooldown
                SummonedPillagerState.checkAndStartCooldown(captainUuid, captain.age);
                continue;
            }
            if (!(entity instanceof MobEntity mob)) continue;

            // Teleport to captain if too far away (like tamed wolves, >12 blocks)
            // But don't teleport if the mob is actively fighting a target
            LivingEntity currentTarget = mob.getTarget();
            boolean isInCombat = currentTarget != null && currentTarget.isAlive();
            if (mob.squaredDistanceTo(captain) > 144.0 && !isInCombat) {
                tryTeleportToOwner(mob, captain);
            }

            if (entity instanceof WitchEntity witch) {
                tickWitchFollower(captain, witch, followers);
            } else {
                tickCombatFollower(captain, mob, targetUuid);
            }
        }
    }

    private static void tickCombatFollower(ServerPlayerEntity captain, MobEntity mob, UUID targetUuid) {
        if (targetUuid != null) {
            // Captain has commanded an attack — set target
            Entity target = captain.getEntityWorld().getEntity(targetUuid);
            if (target instanceof LivingEntity le && le.isAlive()) {
                if (mob.getTarget() != le) mob.setTarget(le);
                return;
            }
        }

        // No commanded target — follow captain
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && !currentTarget.isAlive()) {
            mob.setTarget(null);
        }

        // Only navigate toward captain if the mob has no active target
        if (mob.getTarget() == null) {
            double dist = mob.squaredDistanceTo(captain);
            if (dist > 25.0) { // > 5 blocks
                // Sprint at 1.6x speed to keep up with the captain
                mob.getNavigation().startMovingTo(captain, 1.6);
            }
        }
    }

    private static void tickWitchFollower(ServerPlayerEntity captain, WitchEntity witch,
                                           Set<UUID> allFollowers) {
        // Witches never attack — clear any targets vanilla AI may have set
        witch.setTarget(null);

        // Check potion cooldown (60 ticks = 3 seconds)
        long now = captain.age;
        Long lastThrow = WITCH_POTION_COOLDOWN.get(witch.getUuid());
        boolean canThrow = lastThrow == null || now - lastThrow >= 60;

        if (canThrow) {
            LivingEntity healTarget = findHealTarget(captain, witch, allFollowers);
            if (healTarget != null) {
                throwRegenPotion(witch, healTarget);
                WITCH_POTION_COOLDOWN.put(witch.getUuid(), now);
                return;
            }
        }

        // No one needs healing — follow captain
        double dist = witch.squaredDistanceTo(captain);
        if (dist > 25.0) {
            witch.getNavigation().startMovingTo(captain, 1.6);
        }
    }

    /**
     * Teleports a follower mob to the captain, trying random positions nearby
     * (same algorithm as tamed wolves in vanilla Minecraft).
     */
    private static void tryTeleportToOwner(MobEntity mob, ServerPlayerEntity captain) {
        ServerWorld world = captain.getEntityWorld();
        // Try 10 random positions in a 7x7 area centered on the captain
        for (int i = 0; i < 10; i++) {
            int dx = mob.getRandom().nextBetween(-3, 3);
            int dz = mob.getRandom().nextBetween(-3, 3);
            double tx = captain.getX() + dx;
            double tz = captain.getZ() + dz;
            BlockPos groundPos = BlockPos.ofFloored(tx, captain.getY(), tz);

            // Search up/down a few blocks for valid ground
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos check = groundPos.up(dy);
                BlockPos below = check.down();
                if (world.getBlockState(below).isSolidBlock(world, below)
                        && world.isSpaceEmpty(mob, mob.getType().getDimensions().getBoxAt(
                        check.getX() + 0.5, check.getY(), check.getZ() + 0.5))) {
                    mob.refreshPositionAndAngles(
                            check.getX() + 0.5, check.getY(), check.getZ() + 0.5,
                            mob.getYaw(), mob.getPitch());
                    mob.getNavigation().stop();
                    world.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.5f, 1.0f);
                    return;
                }
            }
        }
    }

    private static LivingEntity findHealTarget(ServerPlayerEntity captain, WitchEntity witch,
                                                Set<UUID> allFollowers) {
        // Check captain first
        if (captain.getHealth() < captain.getMaxHealth()
                && witch.squaredDistanceTo(captain) < 256.0) { // within 16 blocks
            return captain;
        }

        // Check all followers
        for (UUID uuid : allFollowers) {
            Entity e = captain.getEntityWorld().getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive() && le != witch
                    && le.getHealth() < le.getMaxHealth()
                    && witch.squaredDistanceTo(le) < 256.0) {
                return le;
            }
        }
        return null;
    }

    private static boolean hasActiveRaidNearby(ServerPlayerEntity captain) {
        var raid = captain.getEntityWorld().getRaidManager().getRaidAt(captain.getBlockPos(), 9216);
        return raid != null && raid.isActive() && !raid.hasStopped();
    }

    /** Throws a regen potion from a witch at a target. Package-visible for RaidHandler. */
    static void throwRegenPotion(WitchEntity witch, LivingEntity target) {
        ServerWorld world = (ServerWorld) witch.getEntityWorld();

        // Directly apply regeneration effect to the target (guaranteed to work)
        target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.REGENERATION, 100, 1, false, true));

        // Throw a visual splash potion for the animation
        ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
        potionStack.set(DataComponentTypes.POTION_CONTENTS,
                new PotionContentsComponent(Potions.REGENERATION));

        SplashPotionEntity potionEntity = new SplashPotionEntity(world, witch, potionStack);
        potionEntity.setItem(potionStack);

        double dx = target.getX() - witch.getX();
        double dy = target.getEyeY() - witch.getEyeY();
        double dz = target.getZ() - witch.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        potionEntity.setVelocity(dx, dy + horizontal * 0.2, dz, 0.75f, 1.0f);
        world.spawnEntity(potionEntity);

        world.playSound(null, witch.getX(), witch.getY(), witch.getZ(),
                SoundEvents.ENTITY_WITCH_THROW, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    // ── Cleanup on unpossess ──────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        // Despawn all summoned pillagers from the world
        SummonedPillagerState.despawnAll(playerUuid, player.getEntityWorld());
        SummonedPillagerState.clearAll(playerUuid);
        // Clean up follower state
        Set<UUID> followers = CaptainState.getFollowers(playerUuid);
        for (UUID f : followers) {
            WITCH_POTION_COOLDOWN.remove(f);
        }
        CaptainState.clearAll(playerUuid);
    }

    public static void onUnpossessUuid(UUID playerUuid) {
        SummonedPillagerState.clearAll(playerUuid);
        Set<UUID> followers = CaptainState.getFollowers(playerUuid);
        for (UUID f : followers) {
            WITCH_POTION_COOLDOWN.remove(f);
        }
        CaptainState.clearAll(playerUuid);
    }
}








