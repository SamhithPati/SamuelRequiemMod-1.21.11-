package net.sam.samrequiemmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;

public class SoulBossEntity extends HostileEntity implements GeoEntity {
    private static final int ACTION_NONE = 0;
    private static final int ACTION_GROUND_ATTACK = 1;
    private static final int ACTION_SHOOT_ATTACK = 2;
    private static final int ACTION_RAGE = 3;
    private static final int GROUND_ATTACK_DURATION = 60;
    private static final int SHOOT_ATTACK_DURATION = 14;
    private static final int RAGE_DURATION = 50;
    private static final TrackedData<Integer> CURRENT_ACTION = DataTracker.registerData(SoulBossEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ACTION_TICKS = DataTracker.registerData(SoulBossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALKING_ANIM = RawAnimation.begin().thenPlay("walking");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation GROUND_ATTACK_ANIM = RawAnimation.begin().thenPlayAndHold("smash_attack");
    private static final RawAnimation SHOOT_ATTACK_ANIM = RawAnimation.begin().thenPlayAndHold("shoot_attack");
    private static final RawAnimation RAGE_ANIM = RawAnimation.begin().thenPlay("roar");

    private final AnimatableInstanceCache geoCache = new SoulBossAnimatableCache(this);
    private final ServerBossBar bossBar = new ServerBossBar(this.getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);

    private int groundAttackCooldown = 200;
    private int shootAttackCooldown = 60;
    private boolean enraged;

    public SoulBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 10, false);
        this.experiencePoints = 50;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 300.0D)
                .add(EntityAttributes.ATTACK_DAMAGE, 12.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.FLYING_SPEED, 0.28D)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.ARMOR, 8.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(CURRENT_ACTION, ACTION_NONE);
        builder.add(ACTION_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();

        this.setNoGravity(true);

        if (!this.getEntityWorld().isClient()) {
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

            if (!this.enraged && this.getHealth() <= this.getMaxHealth() * 0.5f) {
                this.enraged = true;
                this.setGlowing(true);
                this.startAction(ACTION_RAGE, RAGE_DURATION);
                this.triggerAnim("actions", "rage");
            }

            this.tickActionState();

            if (this.groundAttackCooldown > 0) {
                this.groundAttackCooldown--;
            }

            if (this.shootAttackCooldown > 0) {
                this.shootAttackCooldown--;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                this.getLookControl().lookAt(target, 30.0f, 30.0f);

                if (this.squaredDistanceTo(target) <= 100.0D && this.groundAttackCooldown <= 0) {
                    this.doGroundAttack();
                } else if (this.squaredDistanceTo(target) <= 625.0D && this.shootAttackCooldown <= 0) {
                    this.doShootAttack(target);
                }
            }
        }

        if (!this.isOnGround() && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.85D, 1.0D));
        }
    }

    private void doGroundAttack() {
        this.groundAttackCooldown = this.enraged ? 140 : 200;
        this.startAction(ACTION_GROUND_ATTACK, GROUND_ATTACK_DURATION);
        this.triggerAnim("actions", "ground_attack");
        this.playSound(SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 1.2f, 0.8f);
    }

    private void doShootAttack(LivingEntity target) {
        this.shootAttackCooldown = this.enraged ? 30 : 50;
        this.startAction(ACTION_SHOOT_ATTACK, SHOOT_ATTACK_DURATION);
        this.triggerAnim("actions", "shoot_attack");
        this.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);

        var skull = new net.minecraft.entity.projectile.WitherSkullEntity(this.getEntityWorld(), this, Vec3d.ZERO);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.5D) - skull.getY();
        double dz = target.getZ() - this.getZ();
        skull.setVelocity(dx, dy, dz, 1.25F, 0.0F);
        skull.setPosition(this.getX(), this.getBodyY(0.8D), this.getZ());
        this.getEntityWorld().spawnEntity(skull);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        boolean damaged = super.damage(world, source, amount);

        if (damaged && this.getTarget() == null && source.getAttacker() instanceof LivingEntity living && !(living instanceof PlayerEntity player && player.isCreative())) {
            this.setTarget(living);
        }

        return damaged;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Override
    public float getSoundPitch() {
        return 0.75f;
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        if (this.age % 20 == 0 && !this.enraged && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0f);
        }

        if (this.getTarget() == null) {
            this.setYaw(MathHelper.wrapDegrees(this.getYaw() + 0.5f));
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossBar.clearPlayers();
    }

    public int getCurrentActionId() {
        return this.dataTracker.get(CURRENT_ACTION);
    }

    public int getCurrentActionTicks() {
        return this.dataTracker.get(ACTION_TICKS);
    }

    public int getCurrentActionDuration() {
        return switch (getCurrentActionId()) {
            case ACTION_GROUND_ATTACK -> GROUND_ATTACK_DURATION;
            case ACTION_SHOOT_ATTACK -> SHOOT_ATTACK_DURATION;
            case ACTION_RAGE -> RAGE_DURATION;
            default -> 0;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 2, state -> {
            if (this.isDead()) {
                return state.setAndContinue(DEATH_ANIM);
            }

            if (this.getCurrentActionTicks() > 0) {
                return PlayState.STOP;
            }

            boolean moving = state.isMoving() || this.getVelocity().horizontalLengthSquared() > 1.0E-4D;

            return state.setAndContinue(moving ? WALKING_ANIM : IDLE_ANIM);
        }));

        controllers.add(new AnimationController<>("actions", 0, state -> PlayState.STOP)
                .receiveTriggeredAnimations()
                .triggerableAnim("ground_attack", GROUND_ATTACK_ANIM)
                .triggerableAnim("shoot_attack", SHOOT_ATTACK_ANIM)
                .triggerableAnim("rage", RAGE_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private void startAction(int actionId, int ticks) {
        this.dataTracker.set(CURRENT_ACTION, actionId);
        this.dataTracker.set(ACTION_TICKS, ticks);
    }

    private void tickActionState() {
        int remaining = this.getCurrentActionTicks();
        if (remaining <= 0) {
            if (this.getCurrentActionId() != ACTION_NONE) {
                this.dataTracker.set(CURRENT_ACTION, ACTION_NONE);
            }
            return;
        }

        this.dataTracker.set(ACTION_TICKS, remaining - 1);
        if (remaining - 1 <= 0) {
            this.dataTracker.set(CURRENT_ACTION, ACTION_NONE);
        }
    }

    private static final class SoulBossAnimatableCache extends AnimatableInstanceCache {
        private final AnimatableManager<SoulBossEntity> manager;

        private SoulBossAnimatableCache(SoulBossEntity animatable) {
            super(animatable);
            this.manager = new AnimatableManager<>(animatable);
        }

        @Override
        public <T extends software.bernie.geckolib.animatable.GeoAnimatable> AnimatableManager<T> getManagerForId(long uniqueId) {
            @SuppressWarnings("unchecked")
            AnimatableManager<T> cast = (AnimatableManager<T>) this.manager;
            return cast;
        }
    }
}
