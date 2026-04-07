package net.sam.samrequiemmod.possession.beast;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionHurtSoundHelper;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.passive.BabyPassiveMobNetworking;
import net.sam.samrequiemmod.possession.passive.BabyPassiveMobState;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.UUID;

public final class BeastPossessionController {

    private static final java.util.Map<UUID, Long> LAST_CAMEL_DASH = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<UUID> ARMADILLO_DAMAGE_BYPASS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private BeastPossessionController() {}

    public static boolean isHorseLikePossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.HORSE || type == EntityType.MULE
                || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE;
    }

    public static boolean isEndermitePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ENDERMITE;
    }

    public static boolean isGoatPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.GOAT;
    }

    public static boolean isPolarBearPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.POLAR_BEAR;
    }

    public static boolean isRabbitPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.RABBIT;
    }

    public static boolean isTurtlePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.TURTLE;
    }

    public static boolean isShulkerPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SHULKER;
    }

    public static boolean isStriderPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.STRIDER;
    }

    public static boolean isAxolotlPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.AXOLOTL;
    }

    public static boolean isSnowGolemPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.SNOW_GOLEM;
    }

    public static boolean isCamelPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.CAMEL;
    }

    public static boolean isBeePossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.BEE;
    }

    public static boolean isParrotPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PARROT;
    }

    public static boolean isArmadilloPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ARMADILLO;
    }

    public static boolean isTrackedType(EntityType<?> type) {
        return type == EntityType.HORSE || type == EntityType.MULE || type == EntityType.ZOMBIE_HORSE
                || type == EntityType.SKELETON_HORSE || type == EntityType.ENDERMITE
                || type == EntityType.GOAT || type == EntityType.POLAR_BEAR
                || type == EntityType.RABBIT || type == EntityType.TURTLE || type == EntityType.SHULKER
                || type == EntityType.STRIDER || type == EntityType.AXOLOTL
                || type == EntityType.SNOW_GOLEM || type == EntityType.CAMEL
                || type == EntityType.BEE || type == EntityType.PARROT
                || type == EntityType.ARMADILLO;
    }

    public static boolean isPolarBearAlly(Entity entity) {
        return entity instanceof PolarBearEntity;
    }

    public static boolean isBeeAlly(Entity entity) {
        return entity instanceof BeeEntity;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;

            if (isHorseLikePossessing(serverPlayer)
                    || isRabbitPossessing(serverPlayer) || isTurtlePossessing(serverPlayer)
                    || (isGoatPossessing(serverPlayer) && BabyPassiveMobState.isServerBaby(serverPlayer))
                    || (isPolarBearPossessing(serverPlayer) && BabyPassiveMobState.isServerBaby(serverPlayer))
                    || isArmadilloPossessing(serverPlayer)) {
                return ActionResult.FAIL;
            }

            if (isShulkerPossessing(serverPlayer)) {
                if (entity instanceof LivingEntity target && target.squaredDistanceTo(serverPlayer) <= 400.0) {
                    handleShulkerAttack(serverPlayer, target.getUuid());
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }

            if (isSnowGolemPossessing(serverPlayer)) {
                return ActionResult.FAIL;
            }

            if (isCamelPossessing(serverPlayer) || isStriderPossessing(serverPlayer)) {
                return ActionResult.FAIL;
            }

            if (isParrotPossessing(serverPlayer)) {
                return ActionResult.FAIL;
            }

            if (isEndermitePossessing(serverPlayer) && entity instanceof LivingEntity target) {
                target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), getEndermiteDamage(serverPlayer.getEntityWorld().getDifficulty()));
                markProvoked(target, serverPlayer);
                serverPlayer.swingHand(hand, true);
                serverPlayer.getEntityWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_ENDERMITE_HURT, SoundCategory.PLAYERS, 0.8f, 1.2f);
                return ActionResult.SUCCESS;
            }

            if (isGoatPossessing(serverPlayer) && entity instanceof LivingEntity target) {
                handleGoatRam(serverPlayer, target);
                return ActionResult.SUCCESS;
            }

            if (isPolarBearPossessing(serverPlayer) && entity instanceof LivingEntity target) {
                target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), getPolarDamage(serverPlayer.getEntityWorld().getDifficulty()));
                Vec3d knock = target.getEntityPos().subtract(serverPlayer.getEntityPos()).normalize().multiply(0.75);
                target.addVelocity(knock.x, 0.12, knock.z);
                target.velocityDirty = true;
                markProvoked(target, serverPlayer);
                serverPlayer.swingHand(hand, true);
                net.sam.samrequiemmod.possession.beast.BeastAttackNetworking.broadcastPolarAttack(serverPlayer);
                serverPlayer.getEntityWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, getPitch(serverPlayer));
                return ActionResult.SUCCESS;
            }

            if (isAxolotlPossessing(serverPlayer) && entity instanceof LivingEntity target) {
                target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), 2.0f);
                markProvoked(target, serverPlayer);
                serverPlayer.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            if (isBeePossessing(serverPlayer) && entity instanceof LivingEntity target) {
                target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), serverPlayer.getDamageSources().playerAttack(serverPlayer), getBeeDamage(serverPlayer.getEntityWorld().getDifficulty()));
                int poisonSeconds = getBeePoisonSeconds(serverPlayer.getEntityWorld().getDifficulty());
                if (poisonSeconds > 0) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poisonSeconds * 20, 0));
                }
                markProvoked(target, serverPlayer);
                serverPlayer.swingHand(hand, true);
                serverPlayer.getEntityWorld().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 1.0f, 1.0f);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            EntityType<?> type = PossessionManager.getPossessedType(player);
            if (!isTrackedType(type)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (type == EntityType.ARMADILLO && BeastState.isServerArmadilloCurled(player.getUuid())) {
                if (ARMADILLO_DAMAGE_BYPASS.contains(player.getUuid())) return true;
                float reduced = Math.max(0.0f, (amount - 1.0f) / 2.0f);
                if (reduced <= 0.0f) return false;

                ARMADILLO_DAMAGE_BYPASS.add(player.getUuid());
                try {
                    player.damage(player.getEntityWorld(), source, reduced);
                } finally {
                    ARMADILLO_DAMAGE_BYPASS.remove(player.getUuid());
                }
                return false;
            }

            if (type == EntityType.TURTLE && source.equals(player.getDamageSources().drown())) {
                return false;
            }
            if (type == EntityType.AXOLOTL && source.equals(player.getDamageSources().drown())) {
                return false;
            }
            if (type == EntityType.STRIDER && (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.getSource() instanceof net.minecraft.entity.projectile.SmallFireballEntity)) {
                return false;
            }
            if (type == EntityType.SNOW_GOLEM && source.equals(player.getDamageSources().fall())) {
                return false;
            }
            if (type == EntityType.BEE && (source.equals(player.getDamageSources().drown())
                    || source.equals(player.getDamageSources().fall()))) {
                return false;
            }

            SoundEvent hurt = getHurtSound(player);
            if (hurt != null) {
                PossessionHurtSoundHelper.playIfReady(player, hurt, getPitch(player));
            }

            if (source.getAttacker() instanceof LivingEntity attacker) {
                markProvoked(attacker, player);
                if (isPolarBearPossessing(player) && !isPolarBearAlly(attacker)) {
                    rallyPolarBears(player, attacker);
                }
                if (isAxolotlPossessing(player) && !BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age)) {
                    maybeStartAxolotlPlayDead(player);
                }
                if (isBeePossessing(player) && !isBeeAlly(attacker)) {
                    rallyBees(player, attacker);
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isTrackedType(PossessionManager.getPossessedType(player))) return;
            SoundEvent death = getDeathSound(player);
            if (death != null) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        death, SoundCategory.PLAYERS, 1.0f, getPitch(player));
            }
        });
    }

    public static void tick(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (!isTrackedType(type)) return;

        lockHunger(player);
        handleAmbient(player);

        if (type == EntityType.HORSE || type == EntityType.MULE || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE) {
            if (player.age % 400 == 0) {
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_HORSE_EAT, SoundCategory.PLAYERS, 0.8f, getPitch(player));
            }
            if (type == EntityType.ZOMBIE_HORSE) {
                handleZombieHorseSunlightBurn(player);
            }
        }

        if (isGoatPossessing(player) && player.age % 160 == 0 && player.getRandom().nextFloat() < 0.35f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GOAT_AMBIENT, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        }

        if (isRabbitPossessing(player)) {
            player.fallDistance = 0.0f;
        }

        if (isArmadilloPossessing(player)) {
            handleArmadilloCurlMovement(player);
            handleArmadilloSpiderFlee(player);
        }

        if (isTurtlePossessing(player)) {
            if (player.isTouchingWater()) {
                player.setAir(player.getMaxAir());
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20, 0, false, false, false));
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 2, false, false, false));
            }
        }

        if (isEndermitePossessing(player)) {
            aggroGolems(player, 24.0);
            aggroEndermen(player, 24.0);
        }

        if (isStriderPossessing(player)) {
            if (player.isInLava()) {
                player.setAir(player.getMaxAir());
                if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 1, false, false, false));
                }
            } else {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1, false, false, false));
            }
        }

        if (isAxolotlPossessing(player)) {
            player.setAir(player.getMaxAir());
            if (player.isTouchingWater()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20, 0, false, false, false));
            }
            if (BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age)) {
                player.setVelocity(0.0, 0.0, 0.0);
                player.velocityDirty = true;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20, 0, false, false, false));
            } else {
                maybeStartAxolotlPlayDead(player);
                if (!BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age) && player.age % 40 == 0) {
                    BeastNetworking.broadcastAxolotlPlayDead(player, 0L);
                }
            }
        }

        if (isSnowGolemPossessing(player)) {
            if (player.isTouchingWaterOrRain()) {
                player.damage(player.getEntityWorld(), player.getDamageSources().drown(), 2.0f);
            }
            if (isHotSnowGolemLocation(player) && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && player.age % 20 == 0) {
                player.damage(player.getEntityWorld(), player.getDamageSources().onFire(), 1.0f);
            }
            leaveSnowTrail(player);
        }

        if (isCamelPossessing(player)) {
            player.fallDistance = 0.0f;
        }

        if (isBeePossessing(player)) {
            enforceAlwaysFlight(player);
        }

        if (isParrotPossessing(player)) {
            handleParrotFlight(player);
        }

        if (isShulkerPossessing(player)) {
            double[] anchor = BeastState.getServerShulkerAnchor(player.getUuid());
            if (anchor == null) {
                BeastState.setServerShulkerAnchor(player.getUuid(), player.getX(), player.getY(), player.getZ());
                anchor = BeastState.getServerShulkerAnchor(player.getUuid());
            }
            player.refreshPositionAndAngles(anchor[0], anchor[1], anchor[2], player.getYaw(), 0.0f);
            player.setVelocity(0.0, 0.0, 0.0);
            player.velocityDirty = true;
            player.fallDistance = 0.0f;
            aggroGolems(player, 24.0);
        }
    }

    public static void handleShulkerAttack(ServerPlayerEntity player, UUID targetUuid) {
        if (!isShulkerPossessing(player) || targetUuid == null) return;
        ItemStack shulkerShellStack = new ItemStack(Items.SHULKER_SHELL);
        if (player.getItemCooldownManager().isCoolingDown(shulkerShellStack)) return;

        Entity entity = player.getEntityWorld().getEntity(targetUuid);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (target.squaredDistanceTo(player) > 400.0) return;

        ShulkerBulletEntity bullet = new ShulkerBulletEntity(player.getEntityWorld(), player, target, net.minecraft.util.math.Direction.Axis.X);
        bullet.refreshPositionAndAngles(player.getX(), player.getEyeY(), player.getZ(), player.getYaw(), player.getPitch());
        player.getEntityWorld().spawnEntity(bullet);
        player.getItemCooldownManager().set(shulkerShellStack, 20);
        long untilTick = player.age + 40L;
        BeastState.setServerShulkerOpenUntil(player.getUuid(), untilTick);
        BeastNetworking.broadcastShulkerOpen(player, untilTick);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void handleSnowballAttack(ServerPlayerEntity player, UUID targetUuid) {
        if (!isSnowGolemPossessing(player)) return;
        ItemStack snowballStack = new ItemStack(Items.SNOWBALL);
        if (player.getItemCooldownManager().isCoolingDown(snowballStack)) return;

        LivingEntity target = null;
        if (targetUuid != null) {
            Entity entity = player.getEntityWorld().getEntity(targetUuid);
            if (entity instanceof LivingEntity living && living.isAlive() && living.squaredDistanceTo(player) <= 225.0) {
                target = living;
            }
        }

        SnowballEntity snowball = new SnowballEntity(player.getEntityWorld(), player, snowballStack);
        snowball.refreshPositionAndAngles(player.getX(), player.getEyeY(), player.getZ(), player.getYaw(), player.getPitch());
        if (target != null) {
            Vec3d dir = target.getEyePos().subtract(player.getEyePos()).normalize();
            snowball.setVelocity(dir.x, dir.y, dir.z, 1.5f, 1.0f);
        } else {
            snowball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 1.5f, 1.0f);
        }
        player.getEntityWorld().spawnEntity(snowball);
        player.getItemCooldownManager().set(snowballStack, 10);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void initializeHorseState(ServerPlayerEntity player, HorseEntity horse) {
        int variant = horse.getDataTracker().get(net.sam.samrequiemmod.mixin.client.HorseEntityVariantAccessor.getVariantKey());
        BeastState.setServerHorseVariant(player.getUuid(), variant);
        BabyPassiveMobState.setServerBaby(player.getUuid(), horse.isBaby());
    }

    public static void initializeRabbitState(ServerPlayerEntity player, RabbitEntity rabbit) {
        int variant = rabbit.getDataTracker().get(net.sam.samrequiemmod.mixin.client.RabbitEntityVariantAccessor.getVariantKey());
        BeastState.setServerRabbitVariant(player.getUuid(), variant);
        BabyPassiveMobState.setServerBaby(player.getUuid(), rabbit.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, AbstractHorseEntity horse) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), horse.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, GoatEntity goat) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), goat.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, PolarBearEntity bear) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), bear.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, TurtleEntity turtle) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), turtle.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, StriderEntity strider) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), strider.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, CamelEntity camel) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), camel.isBaby());
    }

    public static void initializePassiveBaby(ServerPlayerEntity player, ArmadilloEntity armadillo) {
        BabyPassiveMobState.setServerBaby(player.getUuid(), armadillo.isBaby());
    }

    public static void initializeAxolotlState(ServerPlayerEntity player, AxolotlEntity axolotl) {
        int variant = axolotl.getDataTracker().get(net.sam.samrequiemmod.mixin.client.AxolotlEntityVariantAccessor.getVariantKey());
        BeastState.setServerAxolotlVariant(player.getUuid(), variant);
        BabyPassiveMobState.setServerBaby(player.getUuid(), axolotl.isBaby());
    }

    public static void syncState(ServerPlayerEntity player) {
        BeastNetworking.broadcastHorseVariant(player, BeastState.getServerHorseVariant(player.getUuid()));
        BeastNetworking.broadcastRabbitVariant(player, BeastState.getServerRabbitVariant(player.getUuid()));
        BeastNetworking.broadcastAxolotlVariant(player, BeastState.getServerAxolotlVariant(player.getUuid()));
        BeastNetworking.broadcastAxolotlPlayDead(player, BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age) ? player.age + 1L : 0L);
        BeastNetworking.broadcastShulkerOpen(player, 0L);
        BeastNetworking.broadcastBeeAngry(player, BeastState.isServerBeeAngry(player.getUuid()));
        BeastNetworking.broadcastParrotFlying(player, BeastState.isServerParrotFlying(player.getUuid()));
        BeastNetworking.broadcastArmadilloCurled(player, BeastState.isServerArmadilloCurled(player.getUuid()));
        if (isShulkerPossessing(player)) {
            BeastState.setServerShulkerAnchor(player.getUuid(), player.getX(), player.getY(), player.getZ());
        }
        BabyPassiveMobNetworking.broadcast(player, BabyPassiveMobState.isServerBaby(player));
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        boolean disableFlight = isBeePossessing(player) || isParrotPossessing(player);
        onUnpossessUuid(player.getUuid());
        if (disableFlight && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
    }

    public static void onUnpossessUuid(UUID uuid) {
        ARMADILLO_DAMAGE_BYPASS.remove(uuid);
        BeastState.clear(uuid);
    }

    public static void handleArmadilloCurlToggle(ServerPlayerEntity player, boolean curled) {
        if (!isArmadilloPossessing(player)) return;
        BeastState.setServerArmadilloCurled(player.getUuid(), curled);
        BeastNetworking.broadcastArmadilloCurled(player, curled);
    }

    public static boolean isHorseFood(ItemStack stack) {
        return stack.isOf(Items.APPLE) || stack.isOf(Items.WHEAT) || stack.isOf(Items.CARROT)
                || stack.isOf(Items.GOLDEN_CARROT) || stack.isOf(Items.GOLDEN_APPLE);
    }

    public static float getHorseFoodHealing(ItemStack stack) {
        if (stack.isOf(Items.GOLDEN_APPLE)) return 8.0f;
        if (stack.isOf(Items.GOLDEN_CARROT)) return 4.0f;
        if (stack.isOf(Items.APPLE) || stack.isOf(Items.CARROT)) return 3.0f;
        if (stack.isOf(Items.WHEAT)) return 2.0f;
        return 0.0f;
    }

    public static boolean isZombieHorseFood(ItemStack stack) {
        return net.sam.samrequiemmod.possession.zombie.ZombiePossessionController.isZombieFood(stack);
    }

    public static float getZombieHorseFoodHealing(ItemStack stack) {
        return net.sam.samrequiemmod.possession.zombie.ZombiePossessionController.getZombieFoodHealing(stack);
    }

    public static boolean isSkeletonHorseFood(ItemStack stack) {
        return stack.isOf(Items.BONE);
    }

    public static float getSkeletonHorseFoodHealing(ItemStack stack) {
        return stack.isOf(Items.BONE) ? 3.0f : 0.0f;
    }

    public static boolean isPolarFood(ItemStack stack) {
        return stack.isOf(Items.SALMON);
    }

    public static float getPolarFoodHealing(ItemStack stack) {
        return stack.isOf(Items.SALMON) ? 4.0f : 0.0f;
    }

    public static boolean isStriderFood(ItemStack stack) {
        return stack.isOf(Items.WARPED_FUNGUS);
    }

    public static float getStriderFoodHealing(ItemStack stack) {
        return stack.isOf(Items.WARPED_FUNGUS) ? 4.0f : 0.0f;
    }

    public static boolean isAxolotlFood(ItemStack stack) {
        return stack.isOf(Items.TROPICAL_FISH);
    }

    public static float getAxolotlFoodHealing(ItemStack stack) {
        return stack.isOf(Items.TROPICAL_FISH) ? 4.0f : 0.0f;
    }

    public static boolean isCamelFood(ItemStack stack) {
        return stack.isOf(Items.CACTUS);
    }

    public static float getCamelFoodHealing(ItemStack stack) {
        return stack.isOf(Items.CACTUS) ? 4.0f : 0.0f;
    }

    public static boolean isBeeFood(ItemStack stack) {
        return stack.isOf(Items.SUNFLOWER) || stack.isOf(Items.POPPY);
    }

    public static float getBeeFoodHealing(ItemStack stack) {
        return isBeeFood(stack) ? 3.0f : 0.0f;
    }

    public static boolean isParrotFood(ItemStack stack) {
        return stack.isOf(Items.WHEAT_SEEDS)
                || stack.isOf(Items.MELON_SEEDS)
                || stack.isOf(Items.PUMPKIN_SEEDS)
                || stack.isOf(Items.BEETROOT_SEEDS)
                || stack.isOf(Items.TORCHFLOWER_SEEDS);
    }

    public static boolean isArmadilloFood(ItemStack stack) {
        return stack.isOf(Items.SPIDER_EYE);
    }

    public static float getParrotFoodHealing(ItemStack stack) {
        return isParrotFood(stack) ? 2.0f : 0.0f;
    }

    public static float getArmadilloFoodHealing(ItemStack stack) {
        return isArmadilloFood(stack) ? 3.0f : 0.0f;
    }

    public static boolean blocksFoodUse(PlayerEntity player, ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null && !isSkeletonHorseFood(stack) && !isBeeFood(stack) && !isParrotFood(stack) && !isArmadilloFood(stack)) return false;
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.ZOMBIE_HORSE) return !isZombieHorseFood(stack);
        if (type == EntityType.SKELETON_HORSE) return !isSkeletonHorseFood(stack);
        if (type == EntityType.HORSE || type == EntityType.MULE) return !isHorseFood(stack);
        if (type == EntityType.POLAR_BEAR) return !isPolarFood(stack);
        if (type == EntityType.STRIDER) return !isStriderFood(stack);
        if (type == EntityType.AXOLOTL) return !isAxolotlFood(stack);
        if (type == EntityType.CAMEL) return !isCamelFood(stack);
        if (type == EntityType.BEE) return !isBeeFood(stack);
        if (type == EntityType.PARROT) return !isParrotFood(stack);
        if (type == EntityType.ARMADILLO) return !isArmadilloFood(stack);
        return type == EntityType.ENDERMITE || type == EntityType.GOAT || type == EntityType.RABBIT
                || type == EntityType.TURTLE || type == EntityType.SHULKER || type == EntityType.SNOW_GOLEM;
    }

    public static String getFoodErrorMessage(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.ZOMBIE_HORSE) return "§cAs a zombie horse, you can only heal from raw meat and rotten flesh.";
        if (type == EntityType.SKELETON_HORSE) return "§cAs a skeleton horse, you can only heal from bones.";
        if (type == EntityType.HORSE || type == EntityType.MULE) return "§cAs a horse, you can only heal from apples, wheat, carrots, golden carrots, and golden apples.";
        if (type == EntityType.POLAR_BEAR) return "§cAs a polar bear, you can only heal from salmon.";
        if (type == EntityType.STRIDER) return "§cAs a strider, you can only heal from warped fungus.";
        if (type == EntityType.AXOLOTL) return "§cAs an axolotl, you can only heal from tropical fish.";
        if (type == EntityType.CAMEL) return "§cAs a camel, you can only heal from cactus.";
        if (type == EntityType.BEE) return "§cAs a bee, you can only heal from sunflowers and poppies.";
        if (type == EntityType.PARROT) return "§cAs a parrot, you can only heal from seeds.";
        if (type == EntityType.ARMADILLO) return "§cAs an armadillo, you can only heal from spider eyes.";
        return "§cThis possession cannot heal by eating.";
    }

    public static double getJumpBoost(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.GOAT) return 1.15;
        if (type == EntityType.CAMEL) return 0.0;
        if (type == EntityType.HORSE || type == EntityType.MULE || type == EntityType.ZOMBIE_HORSE || type == EntityType.SKELETON_HORSE) {
            return 0.75;
        }
        return -1.0;
    }

    public static void handleBeeAngryToggle(ServerPlayerEntity player) {
        if (!isBeePossessing(player)) return;
        boolean angry = !BeastState.isServerBeeAngry(player.getUuid());
        BeastState.setServerBeeAngry(player.getUuid(), angry);
        BeastNetworking.broadcastBeeAngry(player, angry);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                angry ? SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE : SoundEvents.ENTITY_BEE_POLLINATE,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static void handleParrotFlightToggle(ServerPlayerEntity player) {
        if (!isParrotPossessing(player)) return;
        boolean flying = !BeastState.isServerParrotFlying(player.getUuid());
        BeastState.setServerParrotFlying(player.getUuid(), flying);
        BeastNetworking.broadcastParrotFlying(player, flying);
        if (!flying) {
            player.getAbilities().flying = false;
        }
        player.sendAbilitiesUpdate();
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 0.8f, 1.0f);
    }

    public static boolean tryCamelDash(ServerPlayerEntity player) {
        if (!isCamelPossessing(player)) return false;
        long lastDash = LAST_CAMEL_DASH.getOrDefault(player.getUuid(), -100L);
        if (player.age - lastDash < 40L) return true;
        if (!player.isSprinting()) return true;

        Vec3d look = player.getRotationVec(1.0f);
        Vec3d horizontal = new Vec3d(look.x, 0.0, look.z).normalize();
        if (horizontal.lengthSquared() < 0.0001) return true;

        player.setVelocity(horizontal.x * 1.6, 0.7, horizontal.z * 1.6);
        player.velocityDirty = true;
        player.fallDistance = 0.0f;
        LAST_CAMEL_DASH.put(player.getUuid(), (long) player.age);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_CAMEL_DASH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private static void handleGoatRam(ServerPlayerEntity player, LivingEntity target) {
        Vec3d direction = target.getEntityPos().subtract(player.getEntityPos());
        direction = direction.lengthSquared() < 0.0001 ? player.getRotationVec(1.0f) : direction.normalize();
        player.setVelocity(direction.x * 1.55, Math.max(player.getVelocity().y, 0.18), direction.z * 1.55);
        player.velocityDirty = true;
        target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), player.getDamageSources().playerAttack(player), 2.0f);
        target.takeKnockback(4.5f, -direction.x, -direction.z);
        markProvoked(target, player);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GOAT_RAM_IMPACT, SoundCategory.PLAYERS, 1.0f, getPitch(player));
        player.swingHand(Hand.MAIN_HAND, true);
    }

    private static void rallyPolarBears(ServerPlayerEntity player, LivingEntity attacker) {
        List<PolarBearEntity> bears = player.getEntityWorld().getEntitiesByClass(
                PolarBearEntity.class, player.getBoundingBox().expand(15.0), LivingEntity::isAlive);
        for (PolarBearEntity bear : bears) {
            bear.setTarget(attacker);
            bear.setAttacker(attacker);
        }
    }

    private static void rallyBees(ServerPlayerEntity player, LivingEntity attacker) {
        List<BeeEntity> bees = player.getEntityWorld().getEntitiesByClass(
                BeeEntity.class, player.getBoundingBox().expand(20.0), LivingEntity::isAlive);
        for (BeeEntity bee : bees) {
            bee.setTarget(attacker);
            bee.setAttacker(attacker);
            bee.setAngryAt(net.minecraft.entity.LazyEntityReference.of(attacker));
            bee.setAngerDuration(200L);
        }
    }

    private static void aggroGolems(ServerPlayerEntity player, double radius) {
        if (player.age % 10 != 0) return;
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class,
                player.getBoundingBox().expand(radius), LivingEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static void aggroEndermen(ServerPlayerEntity player, double radius) {
        if (player.age % 10 != 0) return;
        for (EndermanEntity enderman : player.getEntityWorld().getEntitiesByClass(EndermanEntity.class,
                player.getBoundingBox().expand(radius), LivingEntity::isAlive)) {
            enderman.setTarget(player);
            enderman.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static void maybeStartAxolotlPlayDead(ServerPlayerEntity player) {
        if (!isAxolotlPossessing(player)) return;
        if (player.getHealth() > 4.0f) return;
        if (BeastState.isServerAxolotlPlayingDead(player.getUuid(), player.age)) return;
        long until = player.age + 100L;
        BeastState.setServerAxolotlPlayDeadUntil(player.getUuid(), until);
        BeastNetworking.broadcastAxolotlPlayDead(player, until);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, false, false, false));
        Box box = player.getBoundingBox().expand(24.0);
        for (MobEntity mob : player.getEntityWorld().getEntitiesByClass(MobEntity.class, box, MobEntity::isAlive)) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
                mob.setAttacker(null);
                mob.getNavigation().stop();
                if (mob instanceof net.minecraft.entity.mob.Angerable angerable) {
                    angerable.setAngryAt(null);
                    angerable.stopAnger();
                }
                net.sam.samrequiemmod.possession.zombie.ZombieTargetingState.clearProvoked(mob.getUuid());
            }
        }
    }

    private static boolean isHotSnowGolemLocation(ServerPlayerEntity player) {
        if (player.getEntityWorld().getRegistryKey() == net.minecraft.world.World.NETHER) return true;
        RegistryKey<net.minecraft.world.biome.Biome> key = player.getEntityWorld().getBiome(player.getBlockPos()).getKey().orElse(null);
        if (key == null) return false;
        String path = key.getValue().getPath();
        return path.contains("desert") || path.contains("savanna") || path.contains("badlands");
    }

    private static void leaveSnowTrail(ServerPlayerEntity player) {
        if (!player.isOnGround()) return;
        BlockPos pos = player.getBlockPos();
        BlockPos ground = pos.down();
        if (player.getEntityWorld().getBlockState(pos).isAir()
                && player.getEntityWorld().getBlockState(ground).isOpaqueFullCube()) {
            player.getEntityWorld().setBlockState(pos, net.minecraft.block.Blocks.SNOW.getDefaultState());
        }
    }

    private static void handleZombieHorseSunlightBurn(ServerPlayerEntity player) {
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

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void enforceAlwaysFlight(ServerPlayerEntity player) {
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

    private static void handleParrotFlight(ServerPlayerEntity player) {
        boolean flying = BeastState.isServerParrotFlying(player.getUuid());
        if (player.getAbilities().allowFlying != flying) {
            player.getAbilities().allowFlying = flying;
            player.sendAbilitiesUpdate();
        }
        if (!flying && player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
        if (flying) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, false, false, false));
            player.fallDistance = 0.0f;
        }
    }

    private static void handleArmadilloCurlMovement(ServerPlayerEntity player) {
        if (!BeastState.isServerArmadilloCurled(player.getUuid())) return;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 5, false, false, false));
    }

    private static void handleArmadilloSpiderFlee(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(16.0);
        List<MobEntity> spiders = player.getEntityWorld().getEntitiesByClass(
                MobEntity.class,
                box,
                mob -> mob.isAlive() && (mob.getType() == EntityType.SPIDER || mob.getType() == EntityType.CAVE_SPIDER));
        for (MobEntity spider : spiders) {
            spider.setTarget(null);
            spider.setAttacker(null);
            spider.getNavigation().stop();
            ZombieTargetingState.clearProvoked(spider.getUuid());
            double dx = spider.getX() - player.getX();
            double dz = spider.getZ() - player.getZ();
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            spider.getNavigation().startMovingTo(
                    spider.getX() + (dx / len) * 8.0,
                    spider.getY(),
                    spider.getZ() + (dz / len) * 8.0,
                    1.2
            );
        }
    }

    private static void handleAmbient(ServerPlayerEntity player) {
        if (player.age % 140 != 0 || player.getRandom().nextFloat() >= 0.35f) return;
        SoundEvent sound = getAmbientSound(player);
        if (sound == null) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundCategory.PLAYERS, 1.0f, getPitch(player));
    }

    private static void markProvoked(Entity entity, ServerPlayerEntity player) {
        if (entity instanceof MobEntity mob && !isPolarBearAlly(entity)) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
        }
    }

    private static float getPitch(PlayerEntity player) {
        return BabyPassiveMobState.isBaby(player) ? 1.35f : 1.0f;
    }

    private static SoundEvent getAmbientSound(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.BEE) return BeastState.isServerBeeAngry(player.getUuid())
                ? SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE : SoundEvents.ENTITY_BEE_LOOP;
        if (type == EntityType.PARROT) return SoundEvents.ENTITY_PARROT_AMBIENT;
        if (type == EntityType.HORSE || type == EntityType.MULE) return SoundEvents.ENTITY_HORSE_AMBIENT;
        if (type == EntityType.ZOMBIE_HORSE) return SoundEvents.ENTITY_ZOMBIE_HORSE_AMBIENT;
        if (type == EntityType.SKELETON_HORSE) return SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT;
        if (type == EntityType.ENDERMITE) return SoundEvents.ENTITY_ENDERMITE_AMBIENT;
        if (type == EntityType.GOAT) return SoundEvents.ENTITY_GOAT_AMBIENT;
        if (type == EntityType.POLAR_BEAR) return BabyPassiveMobState.isBaby(player) ? SoundEvents.ENTITY_POLAR_BEAR_AMBIENT_BABY : SoundEvents.ENTITY_POLAR_BEAR_AMBIENT;
        if (type == EntityType.RABBIT) return SoundEvents.ENTITY_RABBIT_AMBIENT;
        if (type == EntityType.TURTLE) return SoundEvents.ENTITY_TURTLE_AMBIENT_LAND;
        if (type == EntityType.SHULKER) return SoundEvents.ENTITY_SHULKER_AMBIENT;
        if (type == EntityType.STRIDER) return SoundEvents.ENTITY_STRIDER_AMBIENT;
        if (type == EntityType.AXOLOTL) return SoundEvents.ENTITY_AXOLOTL_IDLE_WATER;
        if (type == EntityType.SNOW_GOLEM) return SoundEvents.ENTITY_SNOW_GOLEM_AMBIENT;
        if (type == EntityType.CAMEL) return SoundEvents.ENTITY_CAMEL_AMBIENT;
        if (type == EntityType.ARMADILLO) return SoundEvents.ENTITY_ARMADILLO_AMBIENT;
        return null;
    }

    private static SoundEvent getHurtSound(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.BEE) return SoundEvents.ENTITY_BEE_HURT;
        if (type == EntityType.PARROT) return SoundEvents.ENTITY_PARROT_HURT;
        if (type == EntityType.HORSE || type == EntityType.MULE) return SoundEvents.ENTITY_HORSE_HURT;
        if (type == EntityType.ZOMBIE_HORSE) return SoundEvents.ENTITY_ZOMBIE_HORSE_HURT;
        if (type == EntityType.SKELETON_HORSE) return SoundEvents.ENTITY_SKELETON_HORSE_HURT;
        if (type == EntityType.ENDERMITE) return SoundEvents.ENTITY_ENDERMITE_HURT;
        if (type == EntityType.GOAT) return SoundEvents.ENTITY_GOAT_HURT;
        if (type == EntityType.POLAR_BEAR) return SoundEvents.ENTITY_POLAR_BEAR_HURT;
        if (type == EntityType.RABBIT) return SoundEvents.ENTITY_RABBIT_HURT;
        if (type == EntityType.TURTLE) return SoundEvents.ENTITY_TURTLE_HURT;
        if (type == EntityType.SHULKER) return SoundEvents.ENTITY_SHULKER_HURT;
        if (type == EntityType.STRIDER) return SoundEvents.ENTITY_STRIDER_HURT;
        if (type == EntityType.AXOLOTL) return SoundEvents.ENTITY_AXOLOTL_HURT;
        if (type == EntityType.SNOW_GOLEM) return SoundEvents.ENTITY_SNOW_GOLEM_HURT;
        if (type == EntityType.CAMEL) return SoundEvents.ENTITY_CAMEL_HURT;
        if (type == EntityType.ARMADILLO) return SoundEvents.ENTITY_ARMADILLO_HURT;
        return null;
    }

    private static SoundEvent getDeathSound(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type == EntityType.BEE) return SoundEvents.ENTITY_BEE_DEATH;
        if (type == EntityType.PARROT) return SoundEvents.ENTITY_PARROT_DEATH;
        if (type == EntityType.HORSE || type == EntityType.MULE) return SoundEvents.ENTITY_HORSE_DEATH;
        if (type == EntityType.ZOMBIE_HORSE) return SoundEvents.ENTITY_ZOMBIE_HORSE_DEATH;
        if (type == EntityType.SKELETON_HORSE) return SoundEvents.ENTITY_SKELETON_HORSE_DEATH;
        if (type == EntityType.ENDERMITE) return SoundEvents.ENTITY_ENDERMITE_DEATH;
        if (type == EntityType.GOAT) return SoundEvents.ENTITY_GOAT_DEATH;
        if (type == EntityType.POLAR_BEAR) return SoundEvents.ENTITY_POLAR_BEAR_DEATH;
        if (type == EntityType.RABBIT) return SoundEvents.ENTITY_RABBIT_DEATH;
        if (type == EntityType.TURTLE) return SoundEvents.ENTITY_TURTLE_DEATH;
        if (type == EntityType.SHULKER) return SoundEvents.ENTITY_SHULKER_DEATH;
        if (type == EntityType.STRIDER) return SoundEvents.ENTITY_STRIDER_DEATH;
        if (type == EntityType.AXOLOTL) return SoundEvents.ENTITY_AXOLOTL_DEATH;
        if (type == EntityType.SNOW_GOLEM) return SoundEvents.ENTITY_SNOW_GOLEM_DEATH;
        if (type == EntityType.CAMEL) return SoundEvents.ENTITY_CAMEL_DEATH;
        if (type == EntityType.ARMADILLO) return SoundEvents.ENTITY_ARMADILLO_DEATH;
        return null;
    }

    private static float getEndermiteDamage(Difficulty difficulty) {
        return difficulty == Difficulty.HARD ? 3.0f : 2.0f;
    }

    private static float getPolarDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 4.0f;
            case NORMAL -> 6.0f;
            case HARD -> 9.0f;
            default -> 6.0f;
        };
    }

    private static float getBeeDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY, NORMAL -> 2.0f;
            case HARD -> 3.0f;
            default -> 2.0f;
        };
    }

    private static int getBeePoisonSeconds(Difficulty difficulty) {
        return switch (difficulty) {
            case NORMAL -> 10;
            case HARD -> 18;
            default -> 0;
        };
    }
}






