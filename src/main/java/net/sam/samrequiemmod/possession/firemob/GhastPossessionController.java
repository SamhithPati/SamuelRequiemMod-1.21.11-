package net.sam.samrequiemmod.possession.firemob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GhastPossessionController {

    private static final int COOLDOWN_TICKS = 40;
    private static final int SHOOTING_TICKS = 10;

    private static final Map<UUID, Long> COOLDOWN_UNTIL = new ConcurrentHashMap<>();

    private GhastPossessionController() {}

    public static boolean isGhastPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.GHAST;
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isGhastPossessing(player)) return true;

            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.getSource() instanceof SmallFireballEntity) {
                return false;
            }

            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GHAST_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (source.getAttacker() instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isGhastPossessing(player)) return;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GHAST_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void handleAttackRequest(ServerPlayerEntity player, UUID targetUuid) {
        if (!isGhastPossessing(player)) return;
        long cooldown = COOLDOWN_UNTIL.getOrDefault(player.getUuid(), -1L);
        if ((long) player.age < cooldown) return;

        Vec3d direction = player.getRotationVec(1.0f).normalize();
        FireballEntity fireball = new FireballEntity(player.getWorld(), player, direction.multiply(0.1), 1);
        fireball.refreshPositionAndAngles(
                player.getX() + direction.x * 2.0,
                player.getEyeY(),
                player.getZ() + direction.z * 2.0,
                player.getYaw(),
                player.getPitch()
        );
        player.getWorld().spawnEntity(fireball);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GHAST_WARN, SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        FireMobNetworking.broadcastGhastAttack(player, SHOOTING_TICKS);
        COOLDOWN_UNTIL.put(player.getUuid(), (long) player.age + COOLDOWN_TICKS);

        if (targetUuid != null) {
            Entity entity = player.getServerWorld().getEntity(targetUuid);
            if (entity instanceof MobEntity mob) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
        }
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isGhastPossessing(player)) return;

        lockHunger(player);
        enforceFlight(player);
        handleAmbientSound(player);
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
        if (player.age % 120 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GHAST_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    public static boolean isGhastFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.GHAST_TEAR);
    }

    public static float getGhastFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        return stack.isOf(Items.GHAST_TEAR) ? 6.0f : 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isGhastFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a ghast, you can only heal from ghast tears.";
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
        COOLDOWN_UNTIL.remove(uuid);
    }
}
