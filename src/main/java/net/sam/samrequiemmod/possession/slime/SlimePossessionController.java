package net.sam.samrequiemmod.possession.slime;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionEffects;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.UUID;

public final class SlimePossessionController {

    private SlimePossessionController() {}

    public static boolean isSlimePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SLIME;
    }

    public static boolean isMagmaCubePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.MAGMA_CUBE;
    }

    public static boolean isAnySlimePossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.SLIME || type == EntityType.MAGMA_CUBE;
    }

    public static boolean isSlimeAlly(Entity entity) {
        return entity instanceof SlimeEntity;
    }

    public static int getSize(PlayerEntity player) {
        return SlimeSizeState.getSize(player);
    }

    public static boolean isSmall(PlayerEntity player) {
        return getSize(player) == SlimeSizeState.SMALL;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isAnySlimePossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            sp.swingHand(hand, true);
            if (isSmall(sp) && isSlimePossessing(sp)) {
                playAttackSound(sp);
                return ActionResult.SUCCESS;
            }

            float damage = getAttackDamage(sp);
            if (damage <= 0.0f) return ActionResult.SUCCESS;

            boolean damaged = target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
            if (damaged && entity instanceof MobEntity mob && !isSlimeAlly(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }

            playAttackSound(sp);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnySlimePossessing(player)) return true;

            if (isMagmaCubePossessing(player)
                    && (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.getSource() instanceof SmallFireballEntity)) {
                return false;
            }

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, getHurtSound(player), getPitch(player));

            Entity attacker = source.getAttacker();
            if (attacker instanceof MobEntity mob) {
                if (!isSlimeAlly(attacker)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                if (isSlimeAlly(attacker)) return false;
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnySlimePossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    getDeathSound(player), SoundCategory.PLAYERS, 1.0f, getPitch(player));
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnySlimePossessing(player)) return;

        lockHunger(player);
        handleAmbientSound(player);
        aggroIronGolems(player);
    }

    public static void setInitialSize(ServerPlayerEntity player, int size) {
        SlimeSizeState.setServerSize(player.getUuid(), size);
    }

    public static void broadcastSize(ServerPlayerEntity player) {
        SlimeSizeNetworking.broadcast(player, getSize(player));
    }

    public static boolean handleLethalSplit(ServerPlayerEntity player) {
        if (!isAnySlimePossessing(player)) return false;

        int currentSize = getSize(player);
        int nextSize = switch (currentSize) {
            case SlimeSizeState.BIG -> SlimeSizeState.MEDIUM;
            case SlimeSizeState.MEDIUM -> SlimeSizeState.SMALL;
            default -> 0;
        };
        if (nextSize == 0) return false;

        int spawnCount = 3;
        for (int i = 0; i < spawnCount; i++) {
            SlimeEntity child = isMagmaCubePossessing(player)
                    ? new MagmaCubeEntity(EntityType.MAGMA_CUBE, player.getEntityWorld())
                    : new SlimeEntity(EntityType.SLIME, player.getEntityWorld());
            child.setSize(nextSize, true);
            double offsetX = ((i % 2) - 0.5) * 1.2;
            double offsetZ = ((i / 2) - 0.5) * 1.2;
            child.refreshPositionAndAngles(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ,
                    player.getYaw(), 0.0f);
            player.getEntityWorld().spawnEntity(child);
        }

        SlimeSizeState.setServerSize(player.getUuid(), nextSize);
        SlimeSizeNetworking.broadcast(player, nextSize);
        PossessionEffects.apply(player);
        player.calculateDimensions();
        player.setHealth(player.getMaxHealth());
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getJumpSound(player, nextSize), SoundCategory.PLAYERS, 1.0f, getPitch(player));
        return true;
    }

    public static boolean isSlimeFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.SLIME_BALL);
    }

    public static float getSlimeFoodHealing(ItemStack stack) {
        return isSlimeFood(stack) ? 4.0f : 0.0f;
    }

    public static boolean isMagmaFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.FIRE_CHARGE);
    }

    public static float getMagmaFoodHealing(ItemStack stack) {
        return isMagmaFood(stack) ? 4.0f : 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack, boolean magmaCube) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return false;
        return magmaCube ? !isMagmaFood(stack) : !isSlimeFood(stack);
    }

    public static String getFoodErrorMessage(boolean magmaCube) {
        return magmaCube
                ? "§cAs a magma cube, you can only heal from fire charges."
                : "§cAs a slime, you can only heal from slime balls.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        SlimeSizeState.clear(uuid);
    }

    private static float getAttackDamage(ServerPlayerEntity player) {
        boolean magmaCube = isMagmaCubePossessing(player);
        int size = getSize(player);
        return switch (size) {
            case SlimeSizeState.BIG -> magmaCube ? switch (player.getEntityWorld().getDifficulty()) {
                case EASY -> 4.0f;
                case NORMAL -> 6.0f;
                case HARD -> 9.0f;
                default -> 6.0f;
            } : switch (player.getEntityWorld().getDifficulty()) {
                case EASY -> 3.0f;
                case NORMAL -> 4.0f;
                case HARD -> 6.0f;
                default -> 4.0f;
            };
            case SlimeSizeState.MEDIUM -> magmaCube ? switch (player.getEntityWorld().getDifficulty()) {
                case EASY -> 3.0f;
                case NORMAL -> 4.0f;
                case HARD -> 6.0f;
                default -> 4.0f;
            } : switch (player.getEntityWorld().getDifficulty()) {
                case EASY, NORMAL -> 2.0f;
                case HARD -> 3.0f;
                default -> 2.0f;
            };
            case SlimeSizeState.SMALL -> magmaCube ? switch (player.getEntityWorld().getDifficulty()) {
                case EASY -> 2.0f;
                case NORMAL -> 3.0f;
                case HARD -> 4.5f;
                default -> 3.0f;
            } : 0.0f;
            default -> 0.0f;
        };
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 110 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getAmbientSound(player), SoundCategory.HOSTILE, 0.9f, getPitch(player));
    }

    private static void aggroIronGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    public static double getJumpVelocity(PlayerEntity player) {
        int size = getSize(player);
        double jumpVelocity = switch (size) {
            case SlimeSizeState.BIG -> 0.92;
            case SlimeSizeState.MEDIUM -> 0.84;
            default -> 0.76;
        };
        if (isMagmaCubePossessing(player)) jumpVelocity += 0.04;
        return jumpVelocity;
    }

    public static double getJumpHorizontalBoost(PlayerEntity player) {
        return switch (getSize(player)) {
            case SlimeSizeState.BIG -> 1.18;
            case SlimeSizeState.MEDIUM -> 1.14;
            default -> 1.10;
        };
    }

    public static void playJumpSound(PlayerEntity player) {
        int size = getSize(player);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getJumpSound(player, size), SoundCategory.PLAYERS, 0.8f, getPitch(player));
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void playAttackSound(ServerPlayerEntity player) {
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                isMagmaCubePossessing(player) ? SoundEvents.ENTITY_MAGMA_CUBE_SQUISH : SoundEvents.ENTITY_SLIME_ATTACK,
                SoundCategory.PLAYERS, 0.9f, getPitch(player));
    }

    private static SoundEvent getAmbientSound(ServerPlayerEntity player) {
        return isMagmaCubePossessing(player) ? SoundEvents.ENTITY_MAGMA_CUBE_SQUISH : SoundEvents.ENTITY_SLIME_SQUISH;
    }

    private static SoundEvent getHurtSound(ServerPlayerEntity player) {
        return isMagmaCubePossessing(player) ? SoundEvents.ENTITY_MAGMA_CUBE_HURT : SoundEvents.ENTITY_SLIME_HURT;
    }

    private static SoundEvent getDeathSound(ServerPlayerEntity player) {
        return isMagmaCubePossessing(player) ? SoundEvents.ENTITY_MAGMA_CUBE_DEATH : SoundEvents.ENTITY_SLIME_DEATH;
    }

    private static SoundEvent getJumpSound(PlayerEntity player, int size) {
        if (isMagmaCubePossessing(player)) {
            return size == SlimeSizeState.SMALL ? SoundEvents.ENTITY_MAGMA_CUBE_JUMP : SoundEvents.ENTITY_MAGMA_CUBE_JUMP;
        }
        return size == SlimeSizeState.SMALL ? SoundEvents.ENTITY_SLIME_SQUISH_SMALL : SoundEvents.ENTITY_SLIME_JUMP;
    }

    private static float getPitch(PlayerEntity player) {
        return switch (getSize(player)) {
            case SlimeSizeState.SMALL -> 1.35f;
            case SlimeSizeState.MEDIUM -> 1.12f;
            default -> 1.0f;
        };
    }
}






