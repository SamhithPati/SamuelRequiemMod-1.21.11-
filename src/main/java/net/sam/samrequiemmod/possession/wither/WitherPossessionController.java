package net.sam.samrequiemmod.possession.wither;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WitherPossessionController {

    private static final int SKULL_COOLDOWN_TICKS = 20;
    private static final int EXPLOSION_COOLDOWN_TICKS = 600;
    private static final Set<UUID> EXPLOSION_IMMUNE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_SKULL_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_EXPLOSION_TICK = new ConcurrentHashMap<>();

    private WitherPossessionController() {}

    public static boolean isWitherPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.WITHER;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isWitherPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isWitherPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.isOf(DamageTypes.WITHER)
                    || source.getSource() instanceof SmallFireballEntity) {
                return false;
            }

            if (EXPLOSION_IMMUNE.contains(player.getUuid())
                    && (source.isOf(DamageTypes.EXPLOSION) || source.isOf(DamageTypes.PLAYER_EXPLOSION))) {
                return false;
            }

            if (isShielded(player) && source.getSource() instanceof ProjectileEntity) {
                return false;
            }

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_WITHER_HURT, 1.0f);

            if (source.getAttacker() instanceof MobEntity mob && !(mob instanceof WitherEntity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            }

            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isWitherPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isWitherPossessing(player)) return;

        lockHunger(player);
        enforceFlight(player);
        handleAmbient(player);
        handleWitherImmunity(player);
        handleRegeneration(player);
        aggroCounters(player);
    }

    public static void handleAttackRequest(ServerPlayerEntity player, UUID targetUuid) {
        if (!isWitherPossessing(player)) return;

        long lastShot = LAST_SKULL_TICK.getOrDefault(player.getUuid(), -100L);
        if ((long) player.age - lastShot < SKULL_COOLDOWN_TICKS) return;

        LivingEntity target = null;
        if (targetUuid != null) {
            Entity entity = player.getEntityWorld().getEntity(targetUuid);
            if (entity instanceof LivingEntity living && living.isAlive() && living != player) {
                target = living;
            }
        }

        shootWitherSkull(player, target);
        LAST_SKULL_TICK.put(player.getUuid(), (long) player.age);
    }

    public static void handleExplosionRequest(ServerPlayerEntity player) {
        if (!isWitherPossessing(player)) return;

        long lastExplosion = LAST_EXPLOSION_TICK.getOrDefault(player.getUuid(), -1000L);
        if ((long) player.age - lastExplosion < EXPLOSION_COOLDOWN_TICKS) return;

        LAST_EXPLOSION_TICK.put(player.getUuid(), (long) player.age);
        explode(player);
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
        LAST_SKULL_TICK.remove(uuid);
        LAST_EXPLOSION_TICK.remove(uuid);
        EXPLOSION_IMMUNE.remove(uuid);
    }

    private static void shootWitherSkull(ServerPlayerEntity player, LivingEntity target) {
        Vec3d direction;
        double spawnY = player.getBodyY(0.8D);
        WitherSkullEntity skull = new WitherSkullEntity(player.getEntityWorld(), player, Vec3d.ZERO);
        skull.setPosition(player.getX(), spawnY, player.getZ());

        if (target != null) {
            double dx = target.getX() - player.getX();
            double dy = target.getBodyY(0.5D) - skull.getY();
            double dz = target.getZ() - player.getZ();
            skull.setVelocity(dx, dy, dz, 1.25F, 0.0F);
        } else {
            direction = player.getRotationVec(1.0f).normalize();
            skull.setVelocity(direction.x, direction.y, direction.z, 1.25F, 0.0F);
        }

        player.getEntityWorld().spawnEntity(skull);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void explode(ServerPlayerEntity player) {
        World world = player.getEntityWorld();
        UUID uuid = player.getUuid();
        EXPLOSION_IMMUNE.add(uuid);
        world.createExplosion(player, player.getX(), player.getY(), player.getZ(), 6.0f, World.ExplosionSourceType.MOB);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.2f, 0.8f);
        if (world.getServer() != null) {
            world.getServer().execute(() -> EXPLOSION_IMMUNE.remove(uuid));
        } else {
            EXPLOSION_IMMUNE.remove(uuid);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
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
        player.fallDistance = 0.0f;
    }

    private static void handleAmbient(ServerPlayerEntity player) {
        if (player.age % 110 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void handleWitherImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.WITHER)) {
            player.removeStatusEffect(StatusEffects.WITHER);
        }
    }

    private static void handleRegeneration(ServerPlayerEntity player) {
        if (player.getHealth() <= player.getMaxHealth() * 0.5f) return;
        if (player.age % 10 != 0) return;
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0f));
        }
    }

    private static void aggroCounters(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(32.0);
        for (WitherEntity wither : player.getEntityWorld().getEntitiesByClass(WitherEntity.class, box, WitherEntity::isAlive)) {
            wither.setTarget(null);
            wither.setAttacker(null);
            wither.getNavigation().stop();
        }
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
        for (ZoglinEntity zoglin : player.getEntityWorld().getEntitiesByClass(ZoglinEntity.class, box, ZoglinEntity::isAlive)) {
            zoglin.setTarget(player);
            zoglin.setAttacker(player);
        }
        for (WardenEntity warden : player.getEntityWorld().getEntitiesByClass(WardenEntity.class, box, WardenEntity::isAlive)) {
            warden.setTarget(player);
            warden.setAttacker(player);
        }
    }

    private static boolean isShielded(PlayerEntity player) {
        return player.getHealth() <= player.getMaxHealth() * 0.5f;
    }
}
