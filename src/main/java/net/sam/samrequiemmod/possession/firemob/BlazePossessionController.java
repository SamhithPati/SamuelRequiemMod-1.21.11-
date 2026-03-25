package net.sam.samrequiemmod.possession.firemob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlazePossessionController {

    private static final int MAX_SHOTS = 3;
    private static final int BURST_COOLDOWN_TICKS = 60;
    private static final int FIRE_ACTIVE_TICKS = 20;

    private static final Map<UUID, Integer> SHOTS_FIRED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> FIRE_ACTIVE_UNTIL = new ConcurrentHashMap<>();

    private BlazePossessionController() {}

    public static boolean isBlazePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.BLAZE;
    }

    public static boolean isBlazeAlly(Entity entity) {
        return entity instanceof BlazeEntity;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isBlazePossessing(player)) return true;

            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.getSource() instanceof SmallFireballEntity) {
                return false;
            }

            if (source.getSource() instanceof SnowballEntity) {
                player.damage(player.getDamageSources().freeze(), 4.0f);
                return false;
            }

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (source.getAttacker() instanceof LivingEntity attacker) {
                if (attacker instanceof MobEntity mob && !isBlazeAlly(attacker)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                rallyNearbyBlazes(player, attacker);
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isBlazePossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void handleAttackRequest(ServerPlayerEntity player, UUID targetUuid) {
        if (!isBlazePossessing(player)) return;

        long cooldown = COOLDOWN_UNTIL.getOrDefault(player.getUuid(), -1L);
        if ((long) player.age < cooldown) return;

        LivingEntity target = null;
        if (targetUuid != null) {
            Entity entity = player.getServerWorld().getEntity(targetUuid);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                target = living;
            }
        }

        if (target != null && target.squaredDistanceTo(player) <= 9.0) {
            meleeAttack(player, target);
            return;
        }

        shootFireball(player);
    }

    private static void meleeAttack(ServerPlayerEntity player, LivingEntity target) {
        float damage = switch (player.getServerWorld().getDifficulty()) {
            case EASY -> 4.0f;
            case NORMAL -> 6.0f;
            case HARD -> 9.0f;
            default -> 6.0f;
        };
        target.damage(player.getDamageSources().playerAttack(player), damage);
        if (target instanceof MobEntity mob && !isBlazeAlly(target)) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        setFireActive(player);
    }

    private static void shootFireball(ServerPlayerEntity player) {
        Vec3d direction = player.getRotationVec(1.0f).normalize();
        SmallFireballEntity fireball = new SmallFireballEntity(player.getWorld(), player, direction.multiply(0.1));
        fireball.refreshPositionAndAngles(
                player.getX() + direction.x * 0.8,
                player.getEyeY() - 0.1,
                player.getZ() + direction.z * 0.8,
                player.getYaw(),
                player.getPitch()
        );
        player.getWorld().spawnEntity(fireball);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        setFireActive(player);

        int shots = SHOTS_FIRED.getOrDefault(player.getUuid(), 0) + 1;
        if (shots >= MAX_SHOTS) {
            SHOTS_FIRED.put(player.getUuid(), 0);
            COOLDOWN_UNTIL.put(player.getUuid(), (long) player.age + BURST_COOLDOWN_TICKS);
        } else {
            SHOTS_FIRED.put(player.getUuid(), shots);
        }
    }

    private static void setFireActive(ServerPlayerEntity player) {
        FIRE_ACTIVE_UNTIL.put(player.getUuid(), (long) player.age + FIRE_ACTIVE_TICKS);
        FireMobNetworking.broadcastBlazeAttack(player, FIRE_ACTIVE_TICKS);
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isBlazePossessing(player)) return;

        lockHunger(player);
        enforceFlight(player);
        handleAmbientSound(player);
        aggroGolems(player);
        handleWaterDamage(player);

        long activeUntil = FIRE_ACTIVE_UNTIL.getOrDefault(player.getUuid(), -1L);
        if ((long) player.age >= activeUntil) {
            FIRE_ACTIVE_UNTIL.remove(player.getUuid());
        }
    }

    private static void handleWaterDamage(ServerPlayerEntity player) {
        if ((player.isTouchingWater() || player.isWet()) && player.age % 20 == 0) {
            player.damage(player.getDamageSources().drown(), 4.0f);
        }
    }

    private static void enforceFlight(ServerPlayerEntity player) {
        if (!player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, false, false, false));
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 100 != 0) return;
        if (player.getRandom().nextFloat() >= 0.5f) return;
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void aggroGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(player.getUuid());
        }
        for (SnowGolemEntity golem : player.getWorld().getEntitiesByClass(SnowGolemEntity.class, box, SnowGolemEntity::isAlive)) {
            golem.setTarget(player);
        }
    }

    private static void rallyNearbyBlazes(ServerPlayerEntity player, LivingEntity attacker) {
        List<BlazeEntity> blazes = player.getServerWorld().getEntitiesByClass(
                BlazeEntity.class, player.getBoundingBox().expand(20.0), BlazeEntity::isAlive);
        for (BlazeEntity blaze : blazes) {
            blaze.setTarget(attacker);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    public static boolean isBlazeFood(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.isOf(Items.BLAZE_ROD) || stack.isOf(Items.BLAZE_POWDER));
    }

    public static float getBlazeFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.BLAZE_POWDER)) return 3.0f;
        if (stack.isOf(Items.BLAZE_ROD)) return 5.0f;
        return 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isBlazeFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a blaze, you can only heal from blaze rods and blaze powder.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
    }

    public static void onUnpossessUuid(UUID uuid) {
        SHOTS_FIRED.remove(uuid);
        COOLDOWN_UNTIL.remove(uuid);
        FIRE_ACTIVE_UNTIL.remove(uuid);
    }
}
