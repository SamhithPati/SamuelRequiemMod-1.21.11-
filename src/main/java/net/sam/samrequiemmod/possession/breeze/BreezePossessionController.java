package net.sam.samrequiemmod.possession.breeze;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.BreezeWindChargeEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BreezePossessionController {

    private static final double ATTACK_RANGE = 20.0;
    private static final int JUMP_COOLDOWN_TICKS = 4;
    private static final double SUPER_JUMP_HORIZONTAL = 2.8;
    private static final double SUPER_JUMP_VERTICAL = 1.45;

    private static final Map<UUID, Integer> LAST_ATTACK_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_JUMP_TICK = new ConcurrentHashMap<>();

    private BreezePossessionController() {}

    public static boolean isBreezePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.BREEZE;
    }

    public static boolean isBreezeAlly(Entity entity) {
        return entity instanceof BreezeEntity;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isBreezePossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity living)) return ActionResult.FAIL;

            handleAttackRequest(sp, living.getUuid());
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isBreezePossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (isFallDamage(player, source)) {
                return false;
            }
            if (source.getSource() instanceof TridentEntity || source.getSource() instanceof PersistentProjectileEntity) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_BREEZE_DEFLECT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                return false;
            }

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_BREEZE_HURT, 1.0f);

            if (source.getAttacker() instanceof MobEntity mob && !isBreezeAlly(mob)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isBreezePossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isBreezePossessing(player)) return;

        player.fallDistance = 0.0f;

        if (player.age % 100 == 0) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    player.isOnGround() ? SoundEvents.ENTITY_BREEZE_IDLE_GROUND : SoundEvents.ENTITY_BREEZE_IDLE_AIR,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (player.age % 15 == 0) {
            aggroThreats(player);
        }
    }

    public static void handleAttackRequest(ServerPlayerEntity player, UUID targetUuid) {
        if (!isBreezePossessing(player) || targetUuid == null) return;
        if (LAST_ATTACK_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE) == player.age) return;

        Entity entity = player.getEntityWorld().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (player.squaredDistanceTo(target) > ATTACK_RANGE * ATTACK_RANGE) return;

        LAST_ATTACK_TICK.put(player.getUuid(), player.age);

        BreezeEntity projectileOwner = new BreezeEntity(EntityType.BREEZE, player.getEntityWorld());
        projectileOwner.copyPositionAndRotation(player);

        BreezeWindChargeEntity windCharge = new BreezeWindChargeEntity(projectileOwner, player.getEntityWorld());
        windCharge.setOwner(player);

        Vec3d origin = player.getEyePos().add(player.getRotationVec(1.0f).multiply(0.6));
        Vec3d direction = new Vec3d(
                target.getX(),
                target.getY() + target.getHeight() * 0.5,
                target.getZ()
        ).subtract(origin).normalize();
        windCharge.refreshPositionAndAngles(origin.x, origin.y, origin.z, player.getYaw(), player.getPitch());
        windCharge.setVelocity(direction.x, direction.y, direction.z, 1.15f, 0.0f);

        player.getEntityWorld().spawnEntity(windCharge);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BREEZE_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        BreezeNetworking.broadcastAnimation(player, 1);

        if (target instanceof MobEntity mob && !isBreezeAlly(mob)) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }
    }

    public static void handleJumpRequest(ServerPlayerEntity player) {
        if (!isBreezePossessing(player)) return;
        int lastJump = LAST_JUMP_TICK.getOrDefault(player.getUuid(), Integer.MIN_VALUE);
        if (player.age - lastJump < JUMP_COOLDOWN_TICKS) return;
        LAST_JUMP_TICK.put(player.getUuid(), player.age);

        Vec3d jumpVelocity = getSuperJumpVelocity(player);
        player.setVelocity(jumpVelocity.x, jumpVelocity.y, jumpVelocity.z);
        player.velocityDirty = true;
        player.setOnGround(false);
        player.fallDistance = 0.0f;
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        BreezeNetworking.broadcastAnimation(player, 2);
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_ATTACK_TICK.remove(uuid);
        LAST_JUMP_TICK.remove(uuid);
        BreezeClientState.clear(uuid);
    }

    private static void aggroThreats(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
        }
        for (ZoglinEntity zoglin : player.getEntityWorld().getEntitiesByClass(ZoglinEntity.class, box, ZoglinEntity::isAlive)) {
            zoglin.setTarget(player);
        }
        for (WardenEntity warden : player.getEntityWorld().getEntitiesByClass(WardenEntity.class, box, WardenEntity::isAlive)) {
            warden.setTarget(player);
        }
        for (WitherEntity wither : player.getEntityWorld().getEntitiesByClass(WitherEntity.class, box, WitherEntity::isAlive)) {
            wither.setTarget(player);
        }
    }

    private static boolean isFallDamage(ServerPlayerEntity player, DamageSource source) {
        return source.equals(player.getDamageSources().fall());
    }

    public static boolean isBreezeFood(ItemStack stack) {
        return stack.isOf(Items.BREEZE_ROD);
    }

    public static float getBreezeFoodHealing(ItemStack stack) {
        return isBreezeFood(stack) ? 4.0f : 0.0f;
    }

    public static String getFoodErrorMessage() {
        return "§cAs a breeze, you can only heal from breeze rods.";
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null && !isBreezeFood(stack);
    }

    public static Vec3d getSuperJumpVelocity(PlayerEntity player) {
        Vec3d forward = player.getRotationVec(1.0f);
        Vec3d horizontal = new Vec3d(forward.x, 0.0, forward.z);
        if (horizontal.lengthSquared() < 1.0E-4) {
            horizontal = Vec3d.fromPolar(0.0f, player.getYaw());
            horizontal = new Vec3d(horizontal.x, 0.0, horizontal.z);
        }
        horizontal = horizontal.normalize().multiply(SUPER_JUMP_HORIZONTAL);
        return new Vec3d(horizontal.x, SUPER_JUMP_VERTICAL, horizontal.z);
    }
}
