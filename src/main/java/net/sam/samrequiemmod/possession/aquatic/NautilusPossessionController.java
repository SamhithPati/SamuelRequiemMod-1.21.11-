package net.sam.samrequiemmod.possession.aquatic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieNautilusEntity;
import net.minecraft.entity.passive.NautilusEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NautilusPossessionController {

    private static final Map<UUID, Long> LAST_DASH_TICK = new ConcurrentHashMap<>();

    private NautilusPossessionController() {}

    public static boolean isNautilusPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.NAUTILUS;
    }

    public static boolean isZombieNautilusPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOMBIE_NAUTILUS;
    }

    public static boolean isAnyNautilusPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isAnyNautilusPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isAnyNautilusPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (source.equals(player.getDamageSources().drown())) {
                return false;
            }
            if (source.getSource() instanceof net.minecraft.entity.projectile.PersistentProjectileEntity
                    || source.getSource() instanceof net.minecraft.entity.projectile.TridentEntity) {
                return true;
            }

            SoundEvent hurtSound = player.isTouchingWater() ? getHurtSound(player) : getLandHurtSound(player);
            if (hurtSound != null) {
                net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(player, hurtSound, 1.0f);
            }

            if (source.getAttacker() instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }

            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isAnyNautilusPossessing(player)) return;
            SoundEvent deathSound = player.isTouchingWater() ? getDeathSound(player) : getLandDeathSound(player);
            if (deathSound != null) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        deathSound, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isAnyNautilusPossessing(player)) return;

        lockHunger(player);
        player.setAir(player.getMaxAir());
        player.fallDistance = 0.0f;
        handleWaterMobility(player);
        handleAmbient(player);
        if (isZombieNautilusPossessing(player)) {
            handleZombieNautilusSunlightBurn(player);
        }
    }

    public static void handleDashRequest(ServerPlayerEntity player, int chargeTicks) {
        if (!isAnyNautilusPossessing(player)) return;
        if (!player.isTouchingWater()) return;

        long lastDash = LAST_DASH_TICK.getOrDefault(player.getUuid(), -100L);
        if (player.age - lastDash < NautilusClientState.DASH_COOLDOWN_TICKS) return;

        Vec3d dashVelocity = getDashVelocity(player, chargeTicks);
        player.setVelocity(dashVelocity);
        player.velocityDirty = true;
        player.fallDistance = 0.0f;
        player.setOnGround(false);
        LAST_DASH_TICK.put(player.getUuid(), (long) player.age);
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                getDashSound(player), SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static Vec3d getDashVelocity(PlayerEntity player, int chargeTicks) {
        int clampedCharge = Math.max(1, Math.min(chargeTicks, NautilusClientState.MAX_CHARGE_TICKS));
        double power = clampedCharge / (double) NautilusClientState.MAX_CHARGE_TICKS;
        Vec3d look = player.getRotationVec(1.0f).normalize();
        if (look.lengthSquared() < 0.0001) {
            look = new Vec3d(0.0, 0.0, 1.0);
        }

        double speed = 0.8 + (2.0 * power);
        return new Vec3d(look.x * speed, look.y * speed, look.z * speed);
    }

    public static boolean isNautilusFood(ItemStack stack) {
        return stack.isOf(Items.PUFFERFISH)
                || stack.isOf(Items.TROPICAL_FISH)
                || stack.isOf(Items.COD)
                || stack.isOf(Items.SALMON)
                || stack.isOf(Items.COOKED_COD)
                || stack.isOf(Items.COOKED_SALMON);
    }

    public static float getNautilusFoodHealing(ItemStack stack) {
        if (stack.isOf(Items.PUFFERFISH)) return 4.0f;
        if (stack.isOf(Items.TROPICAL_FISH)) return 3.0f;
        if (stack.isOf(Items.COD) || stack.isOf(Items.SALMON)) return 3.0f;
        if (stack.isOf(Items.COOKED_COD) || stack.isOf(Items.COOKED_SALMON)) return 4.0f;
        return 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        return stack.get(net.minecraft.component.DataComponentTypes.FOOD) != null && !isNautilusFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a nautilus, you can only heal from fish.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        cleanup(player.getUuid());
        if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
        }
        if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            player.removeStatusEffect(StatusEffects.SLOWNESS);
        }
    }

    public static void onUnpossessUuid(UUID uuid) {
        cleanup(uuid);
    }

    private static void cleanup(UUID uuid) {
        LAST_DASH_TICK.remove(uuid);
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void handleWaterMobility(ServerPlayerEntity player) {
        if (player.isTouchingWater()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20, 2, false, false, false));
            if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                player.removeStatusEffect(StatusEffects.SLOWNESS);
            }
        } else {
            if (player.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
            }
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 0, false, false, false));
        }
    }

    private static void handleAmbient(ServerPlayerEntity player) {
        if (player.age % 140 != 0 || player.getRandom().nextFloat() >= 0.35f) return;
        SoundEvent sound = player.isTouchingWater() ? getAmbientSound(player) : getLandAmbientSound(player);
        if (sound != null) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static void handleZombieNautilusSunlightBurn(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getEntityWorld().isDay()) return;
        if (player.isTouchingWaterOrRain()) return;
        BlockPos eyePos = BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ());
        if (!player.getEntityWorld().isSkyVisible(eyePos)) return;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isEmpty()) {
            player.setOnFireFor(8);
        }
    }

    private static SoundEvent getAmbientSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_AMBIENT
                : SoundEvents.ENTITY_NAUTILUS_AMBIENT;
    }

    private static SoundEvent getLandAmbientSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_AMBIENT_LAND
                : SoundEvents.ENTITY_NAUTILUS_AMBIENT_LAND;
    }

    private static SoundEvent getHurtSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_HURT
                : SoundEvents.ENTITY_NAUTILUS_HURT;
    }

    private static SoundEvent getLandHurtSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_HURT_LAND
                : SoundEvents.ENTITY_NAUTILUS_HURT_LAND;
    }

    private static SoundEvent getDeathSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_DEATH
                : SoundEvents.ENTITY_NAUTILUS_DEATH;
    }

    private static SoundEvent getLandDeathSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_DEATH_LAND
                : SoundEvents.ENTITY_NAUTILUS_DEATH_LAND;
    }

    private static SoundEvent getDashSound(PlayerEntity player) {
        return isZombieNautilusPossessing(player)
                ? SoundEvents.ENTITY_ZOMBIE_NAUTILUS_DASH
                : SoundEvents.ENTITY_NAUTILUS_DASH;
    }
}
