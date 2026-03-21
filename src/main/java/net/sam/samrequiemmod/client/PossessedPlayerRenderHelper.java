package net.sam.samrequiemmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.sam.samrequiemmod.client.CrossbowAnimationOverride;
import net.sam.samrequiemmod.possession.ClientPossessionState;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackClientState;

public final class PossessedPlayerRenderHelper {

    // MethodHandle-based accessors for LimbAnimator.pos and LimbAnimator.speed.
    // Using privateLookupIn() to bypass module access restrictions on private fields.
    private static final java.lang.invoke.VarHandle LIMB_POS;
    private static final java.lang.invoke.VarHandle LIMB_SPEED;
    static {
        try {
            java.lang.invoke.MethodHandles.Lookup lookup = java.lang.invoke.MethodHandles
                    .privateLookupIn(net.minecraft.entity.LimbAnimator.class,
                            java.lang.invoke.MethodHandles.lookup());
            LIMB_POS   = lookup.findVarHandle(net.minecraft.entity.LimbAnimator.class, "pos",   float.class);
            LIMB_SPEED = lookup.findVarHandle(net.minecraft.entity.LimbAnimator.class, "speed", float.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access LimbAnimator fields", e);
        }
    }

    private static ZombieEntity cachedZombie;
    private static DrownedEntity cachedDrowned;
    private static HuskEntity cachedHusk;
    private static SkeletonEntity cachedSkeleton;
    private static PiglinEntity cachedPiglin;
    private static PillagerEntity cachedPillager;
    private static EvokerEntity cachedEvoker;
    private static VindicatorEntity cachedVindicator;
    private static PiglinBruteEntity cachedPiglinBrute;
    private static SpiderEntity cachedSpider;
    private static ZombieVillagerEntity cachedZombieVillager;

    private PossessedPlayerRenderHelper() {
    }

    public static boolean shouldRenderAsPossessed(AbstractClientPlayerEntity player) {
        return ClientPossessionState.get(player) != null;
    }

    public static void renderPossessed(
            AbstractClientPlayerEntity player,
            float entityYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        EntityType<?> type = ClientPossessionState.get(player);
        if (type == null) return;

        LivingEntity shell = getOrCreateShell(type, player.getWorld());
        if (shell == null) return;

        copyPlayerStateToShell(player, shell, tickDelta);

        MinecraftClient.getInstance().getEntityRenderDispatcher().render(
                shell, 0.0, 0.0, 0.0, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static LivingEntity getOrCreateShell(EntityType<?> type, World world) {
        if (type == EntityType.ZOMBIE) {
            if (cachedZombie == null || cachedZombie.getWorld() != world)
                cachedZombie = new ZombieEntity(EntityType.ZOMBIE, world);
            return cachedZombie;
        }
        if (type == EntityType.HUSK) {
            if (cachedHusk == null || cachedHusk.getWorld() != world)
                cachedHusk = new HuskEntity(EntityType.HUSK, world);
            return cachedHusk;
        }
        if (type == EntityType.DROWNED) {
            if (cachedDrowned == null || cachedDrowned.getWorld() != world)
                cachedDrowned = new DrownedEntity(EntityType.DROWNED, world);
            return cachedDrowned;
        }
        if (type == EntityType.SKELETON) {
            if (cachedSkeleton == null || cachedSkeleton.getWorld() != world)
                cachedSkeleton = new SkeletonEntity(EntityType.SKELETON, world);
            return cachedSkeleton;
        }
        if (type == EntityType.PILLAGER) {
            if (cachedPillager == null || cachedPillager.getWorld() != world)
                cachedPillager = new PillagerEntity(EntityType.PILLAGER, world);
            return cachedPillager;
        }
        if (type == EntityType.VINDICATOR) {
            if (cachedVindicator == null || cachedVindicator.getWorld() != world)
                cachedVindicator = new VindicatorEntity(EntityType.VINDICATOR, world);
            return cachedVindicator;
        }
        if (type == EntityType.EVOKER) {
            if (cachedEvoker == null || cachedEvoker.getWorld() != world)
                cachedEvoker = new EvokerEntity(EntityType.EVOKER, world);
            return cachedEvoker;
        }
        if (type == EntityType.PIGLIN) {
            if (cachedPiglin == null || cachedPiglin.getWorld() != world)
                cachedPiglin = new PiglinEntity(EntityType.PIGLIN, world);
            return cachedPiglin;
        }
        if (type == EntityType.PIGLIN_BRUTE) {
            if (cachedPiglinBrute == null || cachedPiglinBrute.getWorld() != world)
                cachedPiglinBrute = new PiglinBruteEntity(EntityType.PIGLIN_BRUTE, world);
            return cachedPiglinBrute;
        }
        if (type == EntityType.SPIDER) {
            if (cachedSpider == null || cachedSpider.getWorld() != world)
                cachedSpider = new SpiderEntity(EntityType.SPIDER, world);
            return cachedSpider;
        }
        if (type == EntityType.ZOMBIE_VILLAGER) {
            if (cachedZombieVillager == null || cachedZombieVillager.getWorld() != world)
                cachedZombieVillager = new ZombieVillagerEntity(EntityType.ZOMBIE_VILLAGER, world);
            return cachedZombieVillager;
        }
        return null;
    }

    private static void copyPlayerStateToShell(
            AbstractClientPlayerEntity player,
            LivingEntity shell,
            float tickDelta
    ) {
        // ── Position / rotation ──────────────────────────────────────────────
        shell.setPosition(player.getX(), player.getY(), player.getZ());

        shell.lastRenderX = player.lastRenderX;
        shell.lastRenderY = player.lastRenderY;
        shell.lastRenderZ = player.lastRenderZ;

        shell.prevX = player.prevX;
        shell.prevY = player.prevY;
        shell.prevZ = player.prevZ;

        float yaw   = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);

        shell.setYaw(yaw);
        shell.prevYaw   = player.prevYaw;
        shell.setPitch(pitch);
        shell.prevPitch = player.prevPitch;

        shell.bodyYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        shell.prevBodyYaw = player.prevBodyYaw;
        shell.headYaw     = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        shell.prevHeadYaw = player.prevHeadYaw;

        // ── Animation state ──────────────────────────────────────────────────
        shell.handSwingProgress     = player.handSwingProgress;
        shell.lastHandSwingProgress = player.lastHandSwingProgress;
        shell.handSwinging          = player.handSwinging;

        shell.setSneaking(player.isSneaking());
        shell.setSprinting(player.isSprinting());
        shell.setOnGround(player.isOnGround());
        shell.setInvisible(player.isInvisible());
        shell.setSwimming(player.isSwimming());

        shell.hurtTime   = player.hurtTime;
        shell.deathTime  = player.deathTime;
        shell.age        = player.age;

        shell.setHealth(Math.max(1.0F, player.getHealth()));
        shell.fallDistance = player.fallDistance;
        shell.setFireTicks(player.getFireTicks());

        // Set shell limb animator to the player's interpolated limb position.
        // Using getPos(tickDelta) for smooth inter-tick values, and setting
        // both pos and speed so the model animates at the correct amplitude.
        float limbPos   = player.limbAnimator.getPos(tickDelta);
        float limbSpeed = player.limbAnimator.getSpeed();
        LIMB_POS.set(shell.limbAnimator,   limbPos);
        LIMB_SPEED.set(shell.limbAnimator, limbSpeed);

        // ── Equipment: copy all held items and worn armour from the player ───
        // The LivingEntity renderer reads these slots directly to draw held
        // items in each hand and all four armour layers.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            shell.equipStack(slot, player.getEquippedStack(slot).copy());
        }

        // ── Mob-specific overrides ───────────────────────────────────────────
        if (shell instanceof PiglinEntity piglin) {
            piglin.setBaby(false);
        }

        // Pillager crossbow animation: setCharging drives the arm-raise pose,
        // and getItemUseTimeLeft()/getActiveItem() overrides drive the pull-back progress
        // (read by CrossbowPosing.charge() inside IllagerModel.setAngles).
        if (shell instanceof PillagerEntity pillager) {
            net.minecraft.item.ItemStack mainHand = player.getMainHandStack();
            net.minecraft.item.ItemStack offHand  = player.getOffHandStack();
            boolean usingCrossbow = player.isUsingItem()
                    && (mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    || offHand.isOf(net.minecraft.item.Items.CROSSBOW));
            net.minecraft.item.ItemStack activeCrossbow = mainHand.isOf(net.minecraft.item.Items.CROSSBOW)
                    ? mainHand : offHand;
            boolean charging = usingCrossbow && !net.minecraft.item.CrossbowItem.isCharged(activeCrossbow);
            pillager.setCharging(charging);

            CrossbowAnimationOverride override = (CrossbowAnimationOverride) pillager;
            if (charging) {
                override.samrequiemmod$setUseTimeOverride(player.getItemUseTimeLeft(), player.getItemUseTime(), activeCrossbow);
            } else {
                override.samrequiemmod$clearUseTimeOverride();
            }
        }

        // Vindicator attack animation: setAttacking drives the arm-raise pose
        if (shell instanceof VindicatorEntity vindicator) {
            long lastAttack = net.sam.samrequiemmod.possession.illager.VindicatorPossessionController
                    .LAST_ATTACK_TICK.getOrDefault(player.getUuid(), -1000L);
            boolean attacking = (player.age - lastAttack) < 100; // 5 seconds = 100 ticks
            vindicator.setAttacking(attacking);
        }

        // Evoker casting animation: drive spell type on shell via ordinal setter
        // Spell ordinals: 0=NONE, 1=SUMMON_VEX, 2=FANGS
        if (shell instanceof EvokerEntity evoker && evoker instanceof net.sam.samrequiemmod.client.EvokerSpellSetter setter) {
            int castType = net.sam.samrequiemmod.possession.illager.EvokerClientState.getCasting(player.getUuid());
            if (castType == 1) {
                setter.samrequiemmod$setSpellByOrdinal(2); // FANGS
            } else if (castType == 2) {
                setter.samrequiemmod$setSpellByOrdinal(1); // SUMMON_VEX
            } else {
                setter.samrequiemmod$setSpellByOrdinal(0); // NONE
            }
        }

        if (shell instanceof ZombieEntity zombie) {
            zombie.setBaby(false);
            boolean armsRaised = ZombieAttackClientState.isAttacking(player.getUuid());
            ZombieArmsHelper.setArmsRaised(zombie, armsRaised);
            // Apply shaking if player is in water conversion state
            boolean isShaking = net.sam.samrequiemmod.possession.WaterShakeNetworking.SHAKING_PLAYERS
                    .contains(player.getUuid());
            net.minecraft.entity.data.TrackedData<Boolean> convertingKey =
                    net.sam.samrequiemmod.mixin.client.ZombieEntityInWaterAccessor.getConvertingInWaterKey();
            zombie.getDataTracker().set(convertingKey, isShaking);
            // Baby zombie
            if (net.sam.samrequiemmod.possession.zombie.BabyZombieState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby husk (HuskEntity extends ZombieEntity, same setBaby logic)
            if (net.sam.samrequiemmod.possession.husk.BabyHuskState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby drowned (DrownedEntity extends ZombieEntity)
            if (net.sam.samrequiemmod.possession.drowned.BabyDrownedState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Baby zombie villager (ZombieVillagerEntity extends ZombieEntity)
            if (net.sam.samrequiemmod.possession.zombie_villager.BabyZombieVillagerState.isClientBaby(player.getUuid())) {
                zombie.setBaby(true);
            }
            // Drowned trident charging pose: when player is using a trident, raise arms
            if (shell instanceof DrownedEntity && player.isUsingItem()) {
                net.minecraft.item.ItemStack activeItem = player.getActiveItem();
                if (activeItem.isOf(net.minecraft.item.Items.TRIDENT)) {
                    ZombieArmsHelper.setArmsRaised(zombie, true);
                }
            }
        }
        // Set riding pose: directly set the vehicle field so hasVehicle() returns true
        // and the biped model renders with sitting legs. No startRiding() to avoid
        // double-offset positioning.
        if (player.hasVehicle() && player.getVehicle() instanceof net.minecraft.entity.passive.ChickenEntity) {
            shell.vehicle = player.getVehicle();
        } else {
            shell.vehicle = null;
        }

        if (shell instanceof Entity entity) {
            entity.setCustomName(player.getDisplayName());
            entity.setCustomNameVisible(false);
        }
    }
}