package net.sam.samrequiemmod.possession.silverfish;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SilverfishPossessionController {

    private static final int HIDE_CHARGE_TICKS = 40;

    private static final Map<UUID, HideCharge> HIDE_CHARGES = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> HIDDEN_BLOCKS = new ConcurrentHashMap<>();

    private SilverfishPossessionController() {}

    public static boolean isSilverfishPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SILVERFISH;
    }

    public static boolean isHidden(PlayerEntity player) {
        return HIDDEN_BLOCKS.containsKey(player.getUuid());
    }

    public static boolean isSilverfishAlly(Entity entity) {
        return entity instanceof SilverfishEntity;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isSilverfishPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;
            if (isHidden(sp)) revealFromBlock(sp, false);

            float damage = switch (sp.getEntityWorld().getDifficulty()) {
                case HARD -> 1.5f;
                case EASY, NORMAL -> 1.0f;
                default -> 1.0f;
            };
            target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);

            if (entity instanceof MobEntity mob && !isSilverfishAlly(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENTITY_SILVERFISH_HURT, SoundCategory.PLAYERS, 0.6f, 1.25f);
            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isSilverfishPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;
            if (isHidden(player)) return false;

            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SILVERFISH_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Entity attacker = source.getAttacker();
            if (attacker instanceof MobEntity mob && !isSilverfishAlly(attacker)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }

            if (attacker instanceof LivingEntity livingAttacker && !isSilverfishAlly(attacker)) {
                rallyNearbySilverfish(player, livingAttacker);
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isSilverfishPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SILVERFISH_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void beginHideCharge(ServerPlayerEntity player, BlockPos blockPos) {
        if (!isSilverfishPossessing(player) || isHidden(player)) return;
        if (!isHideableBlock(player.getEntityWorld().getBlockState(blockPos))) return;
        if (player.squaredDistanceTo(blockPos.toCenterPos()) > 9.0) return;
        HIDE_CHARGES.put(player.getUuid(), new HideCharge(blockPos.toImmutable(), player.age));
    }

    public static void cancelHideCharge(ServerPlayerEntity player) {
        HIDE_CHARGES.remove(player.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isSilverfishPossessing(player)) return;

        lockHunger(player);
        aggroIronGolems(player);
        handleAmbientSound(player);
        tickHideCharge(player);
        tickHiddenState(player);
    }

    private static void tickHideCharge(ServerPlayerEntity player) {
        HideCharge charge = HIDE_CHARGES.get(player.getUuid());
        if (charge == null) return;
        if (isHidden(player)) {
            HIDE_CHARGES.remove(player.getUuid());
            return;
        }
        if (!isHideableBlock(player.getEntityWorld().getBlockState(charge.blockPos()))) {
            HIDE_CHARGES.remove(player.getUuid());
            return;
        }
        if (player.squaredDistanceTo(charge.blockPos().toCenterPos()) > 9.0) {
            HIDE_CHARGES.remove(player.getUuid());
            return;
        }
        if (player.age - charge.startedTick() < HIDE_CHARGE_TICKS) return;

        HIDE_CHARGES.remove(player.getUuid());
        hideInBlock(player, charge.blockPos());
    }

    private static void tickHiddenState(ServerPlayerEntity player) {
        BlockPos hiddenPos = HIDDEN_BLOCKS.get(player.getUuid());
        if (hiddenPos == null) return;

        BlockState state = player.getEntityWorld().getBlockState(hiddenPos);
        if (!isHideableBlock(state)) {
            revealFromBlock(player, true);
            return;
        }

        player.noClip = true;
        player.setInvisible(true);
        player.setVelocity(0.0, 0.0, 0.0);
        player.teleport(player.getEntityWorld(),
                hiddenPos.getX() + 0.5, hiddenPos.getY() + 0.1, hiddenPos.getZ() + 0.5,
                java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);

        if (player.isSneaking()) {
            revealFromBlock(player, true);
        }
    }

    private static void hideInBlock(ServerPlayerEntity player, BlockPos blockPos) {
        HIDDEN_BLOCKS.put(player.getUuid(), blockPos.toImmutable());
        player.noClip = true;
        player.setInvisible(true);
        player.setVelocity(0.0, 0.0, 0.0);
        player.teleport(player.getEntityWorld(),
                blockPos.getX() + 0.5, blockPos.getY() + 0.1, blockPos.getZ() + 0.5,
                java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
        forgetNearbyAttackers(player);
    }

    private static void revealFromBlock(ServerPlayerEntity player, boolean placeOutside) {
        BlockPos hiddenPos = HIDDEN_BLOCKS.remove(player.getUuid());
        player.noClip = false;
        player.setInvisible(false);
        HIDE_CHARGES.remove(player.getUuid());
        if (hiddenPos == null || !placeOutside) return;

        BlockPos revealPos = findRevealPos(player, hiddenPos);
        player.teleport(player.getEntityWorld(),
                revealPos.getX() + 0.5, revealPos.getY(), revealPos.getZ() + 0.5,
                java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
    }

    private static BlockPos findRevealPos(ServerPlayerEntity player, BlockPos hiddenPos) {
        if (player.getEntityWorld().getBlockState(hiddenPos.up()).isAir()) return hiddenPos.up();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos side = hiddenPos.offset(direction);
            if (player.getEntityWorld().getBlockState(side).isAir()) return side;
        }
        return hiddenPos.up();
    }

    private static void forgetNearbyAttackers(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(32.0);
        for (MobEntity mob : player.getEntityWorld().getEntitiesByClass(MobEntity.class, box, MobEntity::isAlive)) {
            if (mob.getTarget() == player || mob.getAttacker() == player) {
                mob.setTarget(null);
                mob.setAttacker(null);
                mob.getNavigation().stop();
                if (mob instanceof net.minecraft.entity.mob.Angerable angerable
                        && player.getUuid().equals(angerable.getAngryAt())) {
                    angerable.setAngryAt(null);
                    angerable.stopAnger();
                }
                ZombieTargetingState.clearProvoked(mob.getUuid());
            }
        }
    }

    private static void rallyNearbySilverfish(ServerPlayerEntity player, LivingEntity threat) {
        Box box = player.getBoundingBox().expand(20.0);
        List<SilverfishEntity> silverfish = player.getEntityWorld().getEntitiesByClass(
                SilverfishEntity.class, box, SilverfishEntity::isAlive);
        for (SilverfishEntity ally : silverfish) {
            ally.setTarget(threat);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 120 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SILVERFISH_AMBIENT, SoundCategory.HOSTILE, 0.8f, 1.0f);
    }

    private static boolean isHideableBlock(BlockState state) {
        return state.isOf(Blocks.STONE)
                || state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.STONE_BRICKS)
                || state.isOf(Blocks.MOSSY_STONE_BRICKS)
                || state.isOf(Blocks.CRACKED_STONE_BRICKS)
                || state.isOf(Blocks.CHISELED_STONE_BRICKS)
                || state.isOf(Blocks.DEEPSLATE);
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        revealFromBlock(player, true);
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        HIDE_CHARGES.remove(uuid);
        HIDDEN_BLOCKS.remove(uuid);
    }

    private record HideCharge(BlockPos blockPos, int startedTick) {}
}






