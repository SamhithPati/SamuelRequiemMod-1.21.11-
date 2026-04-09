package net.sam.samrequiemmod.possession.warden;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WardenPossessionController {

    private static final double MELEE_RANGE = 3.0;
    private static final int SONIC_COOLDOWN_TICKS = 40;
    private static final int SONIC_CHARGE_TICKS = 34;
    private static final Map<UUID, Long> LAST_SONIC_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SNIFF_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingSonicBoom> PENDING_SONIC = new ConcurrentHashMap<>();

    private WardenPossessionController() {}

    public static boolean isWardenPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WARDEN;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isWardenPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (sp.squaredDistanceTo(target) > MELEE_RANGE * MELEE_RANGE) return ActionResult.FAIL;

            sp.swingHand(hand, true);
            float damage = getMeleeDamage(world.getDifficulty());
            target.damage(sp.getEntityWorld(), sp.getDamageSources().mobAttack(sp), damage);
            if (entity instanceof MobEntity mob && !(mob instanceof WardenEntity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }
            if (target instanceof ServerPlayerEntity blockingPlayer && blockingPlayer.isBlocking()) {
                blockingPlayer.getItemCooldownManager().set(new ItemStack(Items.SHIELD), 100);
                blockingPlayer.clearActiveItem();
            }

            sp.getEntityWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            WardenNetworking.broadcastAnimation(sp, 1);
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWardenPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())) {
                return false;
            }
            if (source.getAttacker() instanceof BlazeEntity || source.getSource() instanceof net.minecraft.entity.projectile.SmallFireballEntity) {
                return false;
            }

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_WARDEN_HURT, 1.0f);
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWardenPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isWardenPossessing(player)) return;

        lockHunger(player);
        if (player.hasStatusEffect(StatusEffects.DARKNESS)) {
            player.removeStatusEffect(StatusEffects.DARKNESS);
        }
        tickPendingSonic(player);

        if (player.age % 120 == 0) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WARDEN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        if (player.age % 80 == 0) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }
        if (player.age % 100 == 0) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }

        if (player.age - LAST_SNIFF_TICK.getOrDefault(player.getUuid(), -300L) >= 300L) {
            LAST_SNIFF_TICK.put(player.getUuid(), (long) player.age);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WARDEN_SNIFF, SoundCategory.PLAYERS, 1.0f, 1.0f);
            WardenNetworking.broadcastAnimation(player, 4);
        }

        if (player.age % 20 == 0) {
            aggroNearbyThreats(player);
        }
        quietNearbyWardens(player);
    }

    public static void handleSonicBoom(ServerPlayerEntity player, UUID targetUuid) {
        if (!isWardenPossessing(player) || targetUuid == null) return;
        long last = LAST_SONIC_TICK.getOrDefault(player.getUuid(), -100L);
        if (player.age - last < SONIC_COOLDOWN_TICKS) return;
        if (PENDING_SONIC.containsKey(player.getUuid())) return;

        Entity entity = player.getEntityWorld().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (player.squaredDistanceTo(target) > 20.0 * 20.0) return;

        LAST_SONIC_TICK.put(player.getUuid(), (long) player.age);
        PENDING_SONIC.put(player.getUuid(), new PendingSonicBoom(targetUuid, player.age + SONIC_CHARGE_TICKS));
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 3.0f, 1.0f);
        WardenNetworking.broadcastAnimation(player, 2);
    }

    public static void handleRoar(ServerPlayerEntity player) {
        if (!isWardenPossessing(player)) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.0f, 1.0f);
        WardenNetworking.broadcastAnimation(player, 3);
    }

    public static boolean isWardenFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.BEEF) || stack.isOf(Items.PORKCHOP) || stack.isOf(Items.CHICKEN)
                || stack.isOf(Items.MUTTON) || stack.isOf(Items.RABBIT)
                || stack.isOf(Items.COD) || stack.isOf(Items.SALMON)) {
            return true;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block.getDefaultState().isIn(BlockTags.SCULK_REPLACEABLE)
                || stack.isOf(Items.SCULK)
                || stack.isOf(Items.SCULK_CATALYST)
                || stack.isOf(Items.SCULK_SENSOR)
                || stack.isOf(Items.SCULK_SHRIEKER)
                || stack.isOf(Items.SCULK_VEIN);
    }

    public static float getWardenFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0f;
        if (stack.isOf(Items.BEEF) || stack.isOf(Items.PORKCHOP)) return 8.0f;
        if (stack.isOf(Items.CHICKEN) || stack.isOf(Items.MUTTON) || stack.isOf(Items.SALMON)) return 6.0f;
        if (stack.isOf(Items.RABBIT) || stack.isOf(Items.COD)) return 5.0f;
        if (stack.isOf(Items.SCULK_CATALYST) || stack.isOf(Items.SCULK_SHRIEKER)) return 10.0f;
        if (stack.isOf(Items.SCULK_SENSOR) || stack.isOf(Items.SCULK)) return 8.0f;
        if (stack.isOf(Items.SCULK_VEIN)) return 4.0f;
        return 0.0f;
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isWardenFood(stack);
    }

    public static String getFoodErrorMessage() {
        return "§cAs a warden, you can only heal from raw meat and skulk blocks.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        LAST_SONIC_TICK.remove(player.getUuid());
        LAST_SNIFF_TICK.remove(player.getUuid());
        PENDING_SONIC.remove(player.getUuid());
        WardenClientState.clear(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_SONIC_TICK.remove(uuid);
        LAST_SNIFF_TICK.remove(uuid);
        PENDING_SONIC.remove(uuid);
        WardenClientState.clear(uuid);
    }

    private static void aggroNearbyThreats(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
        }
        for (ZoglinEntity zoglin : player.getEntityWorld().getEntitiesByClass(ZoglinEntity.class, box, ZoglinEntity::isAlive)) {
            zoglin.setTarget(player);
        }
    }

    private static void quietNearbyWardens(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(32.0);
        for (WardenEntity warden : player.getEntityWorld().getEntitiesByClass(WardenEntity.class, box, WardenEntity::isAlive)) {
            warden.setTarget(null);
            warden.setAttacker(null);
            warden.getNavigation().stop();
            warden.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().forget(MemoryModuleType.ROAR_TARGET);
            warden.getBrain().forget(MemoryModuleType.DISTURBANCE_LOCATION);
            warden.getBrain().forget(MemoryModuleType.WALK_TARGET);
            warden.getBrain().forget(MemoryModuleType.LOOK_TARGET);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(19);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void tickPendingSonic(ServerPlayerEntity player) {
        PendingSonicBoom pending = PENDING_SONIC.get(player.getUuid());
        if (pending == null) {
            return;
        }
        if (player.age < pending.fireTick()) {
            return;
        }
        PENDING_SONIC.remove(player.getUuid());

        Entity entity = player.getEntityWorld().getEntity(pending.targetUuid());
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        if (player.squaredDistanceTo(target) > 20.0 * 20.0) {
            return;
        }

        Vec3d chest = player.getEntityPos().add(0.0, player.getHeight() * 0.68, 0.0);
        Vec3d delta = target.getEyePos().subtract(chest);
        Vec3d direction = delta.normalize();
        int particleSteps = net.minecraft.util.math.MathHelper.floor(delta.length()) + 7;
        for (int i = 1; i < particleSteps; i++) {
            Vec3d point = chest.add(direction.multiply(i));
            ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    point.x, point.y, point.z,
                    1, 0.0, 0.0, 0.0, 0.0
            );
        }

        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 3.0f, 1.0f);

        float damage = getSonicDamage(player.getEntityWorld().getDifficulty());
        if (target.damage(player.getEntityWorld(), player.getDamageSources().sonicBoom(player), damage)
                && target instanceof MobEntity mob && !(mob instanceof WardenEntity)) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }

        double verticalKnockback = 0.5 * (1.0 - target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
        double horizontalKnockback = 2.5 * (1.0 - target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
        target.addVelocity(
                direction.getX() * horizontalKnockback,
                direction.getY() * verticalKnockback,
                direction.getZ() * horizontalKnockback
        );
    }

    private static float getMeleeDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 16.0f;
            case HARD -> 45.0f;
            default -> 30.0f;
        };
    }

    private static float getSonicDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 6.0f;
            case HARD -> 15.0f;
            default -> 10.0f;
        };
    }

    private record PendingSonicBoom(UUID targetUuid, long fireTick) {}
}
