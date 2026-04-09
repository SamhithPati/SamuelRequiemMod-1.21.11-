package net.sam.samrequiemmod.possession.creaking;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CreakingHeartBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionHurtSoundHelper;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class CreakingPossessionController {

    private static final int INVULNERABLE_VISUAL_TICKS = 10;
    private static final int CRUMBLING_TICKS = 32;

    private CreakingPossessionController() {
    }

    public static boolean isCreakingPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.CREAKING;
    }

    public static void initializeFromCreaking(ServerPlayerEntity player, CreakingEntity creaking) {
        if (creaking.isTransient()) {
            BlockPos pos = creaking.getHomePos();
            if (pos != null) {
                CreakingState.setServerHeartPos(player.getUuid(), pos);
                return;
            }
        }
        CreakingState.setServerHeartPos(player.getUuid(), null);
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isCreakingPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (CreakingState.isServerCrumbling(serverPlayer.getUuid(), serverPlayer.age)) return ActionResult.FAIL;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;

            target.damage((ServerWorld) target.getEntityWorld(),
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    getMeleeDamage(serverPlayer));
            if (target instanceof MobEntity mob && !isAlwaysHostile(mob)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), serverPlayer.getUuid());
            }
            serverPlayer.swingHand(hand, true);
            SoundEvent attackSound = getAttackSound();
            if (attackSound != null) {
                serverPlayer.getEntityWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        attackSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isCreakingPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (CreakingState.isServerCrumbling(player.getUuid(), player.age)) {
                return false;
            }

            PossessionHurtSoundHelper.playIfReady(player, getHurtSound(), 1.0f);

            if (source.getAttacker() instanceof LivingEntity attacker && !isAlwaysHostile(attacker)) {
                markProvoked(attacker, player);
            }

            if (hasLinkedHeart(player)) {
                triggerHeartDamageFeedback(player);
                return false;
            }

            return true;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isCreakingPossessing(player)) return;

        handleAmbientSound(player);

        long crumblingUntil = CreakingState.getServerCrumblingUntil(player.getUuid());
        if (crumblingUntil > 0L) {
            if (player.age >= crumblingUntil) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        getDeathSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
                PossessionManager.clearPossession(player);
                return;
            }

            player.setVelocity(0.0, Math.min(player.getVelocity().y, 0.0), 0.0);
            player.velocityDirty = true;
            player.fallDistance = 0.0f;
            return;
        }

        if (CreakingState.hasServerHeart(player.getUuid()) && !hasLinkedHeart(player)) {
            startCrumbling(player);
        }
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
        CreakingNetworking.broadcastState(player);
    }

    public static void onUnpossessUuid(UUID uuid) {
        CreakingState.clear(uuid);
    }

    private static void startCrumbling(ServerPlayerEntity player) {
        long until = player.age + CRUMBLING_TICKS;
        CreakingState.setServerCrumblingUntil(player.getUuid(), until);
        CreakingState.setServerInvulnerableUntil(player.getUuid(), 0L);
        CreakingNetworking.broadcastState(player);
    }

    private static void triggerHeartDamageFeedback(ServerPlayerEntity player) {
        BlockPos pos = CreakingState.getServerHeartPos(player.getUuid());
        if (pos == null) return;
        BlockEntity blockEntity = player.getEntityWorld().getBlockEntity(pos);
        if (blockEntity instanceof CreakingHeartBlockEntity heart) {
            heart.onPuppetDamage();
        }
        CreakingState.setServerInvulnerableUntil(player.getUuid(), player.age + INVULNERABLE_VISUAL_TICKS);
        CreakingNetworking.broadcastState(player);
    }

    private static boolean hasLinkedHeart(ServerPlayerEntity player) {
        BlockPos pos = CreakingState.getServerHeartPos(player.getUuid());
        if (pos == null) return false;
        return player.getEntityWorld().getBlockEntity(pos) instanceof CreakingHeartBlockEntity;
    }

    private static void markProvoked(LivingEntity attacker, ServerPlayerEntity player) {
        if (attacker instanceof MobEntity mob) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            mob.setTarget(player);
            mob.setAttacker(player);
        }
    }

    private static boolean isAlwaysHostile(Entity entity) {
        return entity instanceof WardenEntity
                || entity instanceof net.minecraft.entity.boss.WitherEntity
                || entity instanceof net.minecraft.entity.mob.ZoglinEntity;
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 140 != 0) return;
        if (player.getRandom().nextFloat() >= 0.35f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAmbientSound(), SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static float getMeleeDamage(ServerPlayerEntity player) {
        return switch (player.getEntityWorld().getDifficulty()) {
            case EASY -> 2.0f;
            case NORMAL -> 3.0f;
            case HARD -> 4.5f;
            default -> 3.0f;
        };
    }

    private static SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_CREAKING_AMBIENT;
    }

    private static SoundEvent getHurtSound() {
        return SoundEvents.ENTITY_CREAKING_TWITCH;
    }

    private static SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_CREAKING_DEATH;
    }

    private static SoundEvent getAttackSound() {
        return SoundEvents.ENTITY_CREAKING_ATTACK;
    }
}
