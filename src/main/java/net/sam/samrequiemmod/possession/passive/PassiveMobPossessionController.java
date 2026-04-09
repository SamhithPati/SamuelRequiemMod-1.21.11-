package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;

public final class PassiveMobPossessionController {

    private PassiveMobPossessionController() {}

    private static float getPassivePitch(ServerPlayerEntity player) {
        return BabyPassiveMobState.isServerBaby(player) ? 1.35f : 1.0f;
    }

    public static void register() {
        // Block all melee attacks when possessing a passive mob
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isPassiveMobPossessing(serverPlayer)) return ActionResult.PASS;
            // Allow possession relic usage
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // Hurt sounds for passive mob possessed players
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            EntityType<?> type = PossessionManager.getPossessedType(player);
            if (type == null) return true;

            SoundEvent hurtSound = getHurtSound(type);
            if (hurtSound == null) return true;

            // Slimes cancel damage elsewhere — don't play hurt sound for them
            if (source.getAttacker() instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, hurtSound, getPassivePitch(player));
            return true;
        });

        // Cancel fall damage for chicken-possessed players
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            EntityType<?> type = PossessionManager.getPossessedType(player);
            if (type != EntityType.CHICKEN) return true;
            if (source.equals(player.getDamageSources().fall())) return false;
            return true;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isPassiveMobPossessing(player)) return;

        lockHunger(player);
        handleAmbientSound(player);
        handleChickenSlowFall(player);
        handleChickenPredators(player);
        handleSnifferAmbient(player);
    }

    // ── Hunger lock ──────────────────────────────────────────────────────────

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    // ── Chicken slow fall ────────────────────────────────────────────────────

    private static void handleChickenSlowFall(ServerPlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        if (type != EntityType.CHICKEN) return;

        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SLOW_FALLING,
                20,
                0,
                false,
                false,
                false
        ));

        if (!player.isOnGround() && player.getVelocity().y < 0.0) {
            Vec3d vel = player.getVelocity();
            double slowedY = Math.max(vel.y * 0.35, -0.08);
            player.setVelocity(vel.x, slowedY, vel.z);
            player.fallDistance = 0.0f;
            player.velocityDirty = true;
        }
    }

    private static void handleChickenPredators(ServerPlayerEntity player) {
        if (PossessionManager.getPossessedType(player) != EntityType.CHICKEN) return;
        if (player.age % 10 != 0) return;

        var box = player.getBoundingBox().expand(20.0);

        for (FoxEntity fox : player.getEntityWorld().getEntitiesByClass(FoxEntity.class, box, FoxEntity::isAlive)) {
            forcePredatorTarget(fox, player);
        }

        for (OcelotEntity ocelot : player.getEntityWorld().getEntitiesByClass(OcelotEntity.class, box, OcelotEntity::isAlive)) {
            forcePredatorTarget(ocelot, player);
        }

        for (CatEntity cat : player.getEntityWorld().getEntitiesByClass(CatEntity.class, box, cat -> cat.isAlive() && !cat.isTamed())) {
            forcePredatorTarget(cat, player);
        }
    }

    private static void forcePredatorTarget(MobEntity predator, ServerPlayerEntity player) {
        predator.setTarget(player);
        predator.setAttacker(player);
        predator.getNavigation().startMovingTo(player, 1.25);
    }

    private static void handleSnifferAmbient(ServerPlayerEntity player) {
        if (PossessionManager.getPossessedType(player) != EntityType.SNIFFER) return;
        if (player.age % 160 != 0) return;
        if (player.getRandom().nextFloat() >= 0.45f) return;
        player.getEntityWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SNIFFER_SNIFFING, SoundCategory.PLAYERS, 1.0f, getPassivePitch(player));
    }

    // ── Ambient sounds ───────────────────────────────────────────────────────

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 200 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;

        EntityType<?> type = PossessionManager.getPossessedType(player);
        SoundEvent ambientSound = getAmbientSound(type);
        if (ambientSound == null) return;

        player.getEntityWorld().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                ambientSound, SoundCategory.PLAYERS, 1.0f, getPassivePitch(player));
    }

    // ── Sound lookups ────────────────────────────────────────────────────────

    private static SoundEvent getAmbientSound(EntityType<?> type) {
        if (type == EntityType.COW)       return SoundEvents.ENTITY_COW_AMBIENT;
        if (type == EntityType.MOOSHROOM) return SoundEvents.ENTITY_COW_AMBIENT;
        if (type == EntityType.PIG)       return SoundEvents.ENTITY_PIG_AMBIENT;
        if (type == EntityType.SHEEP)     return SoundEvents.ENTITY_SHEEP_AMBIENT;
        if (type == EntityType.CHICKEN)   return SoundEvents.ENTITY_CHICKEN_AMBIENT;
        if (type == EntityType.SNIFFER)   return SoundEvents.ENTITY_SNIFFER_IDLE;
        return null;
    }

    private static SoundEvent getHurtSound(EntityType<?> type) {
        if (type == EntityType.COW)       return SoundEvents.ENTITY_COW_HURT;
        if (type == EntityType.MOOSHROOM) return SoundEvents.ENTITY_COW_HURT;
        if (type == EntityType.PIG)       return SoundEvents.ENTITY_PIG_HURT;
        if (type == EntityType.SHEEP)     return SoundEvents.ENTITY_SHEEP_HURT;
        if (type == EntityType.CHICKEN)   return SoundEvents.ENTITY_CHICKEN_HURT;
        if (type == EntityType.SNIFFER)   return SoundEvents.ENTITY_SNIFFER_HURT;
        return null;
    }

    public static SoundEvent getDeathSound(EntityType<?> type) {
        if (type == EntityType.COW)       return SoundEvents.ENTITY_COW_DEATH;
        if (type == EntityType.MOOSHROOM) return SoundEvents.ENTITY_COW_DEATH;
        if (type == EntityType.PIG)       return SoundEvents.ENTITY_PIG_DEATH;
        if (type == EntityType.SHEEP)     return SoundEvents.ENTITY_SHEEP_DEATH;
        if (type == EntityType.CHICKEN)   return SoundEvents.ENTITY_CHICKEN_DEATH;
        if (type == EntityType.SNIFFER)   return SoundEvents.ENTITY_SNIFFER_DEATH;
        return null;
    }

    // ── Food validation ──────────────────────────────────────────────────────

    public static boolean isPassiveMobFood(EntityType<?> type, ItemStack stack) {
        if (type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.SHEEP) {
            return stack.isOf(Items.WHEAT);
        }
        if (type == EntityType.CHICKEN) {
            return stack.isOf(Items.WHEAT_SEEDS)
                    || stack.isOf(Items.MELON_SEEDS)
                    || stack.isOf(Items.PUMPKIN_SEEDS)
                    || stack.isOf(Items.BEETROOT_SEEDS)
                    || stack.isOf(Items.TORCHFLOWER_SEEDS)
                    || stack.isOf(Items.PITCHER_POD);
        }
        if (type == EntityType.PIG) {
            return stack.isOf(Items.POTATO)
                    || stack.isOf(Items.CARROT);
        }
        if (type == EntityType.SNIFFER) {
            return stack.isOf(Items.TORCHFLOWER_SEEDS);
        }
        return false;
    }

    public static float getPassiveMobFoodHealing(EntityType<?> type, ItemStack stack) {
        // Wheat heals 2 HP (1 heart) for cow/mooshroom/sheep
        if (type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.SHEEP) {
            if (stack.isOf(Items.WHEAT)) return 2.0f;
        }
        // Seeds heal 1 HP (0.5 hearts) for chicken
        if (type == EntityType.CHICKEN) {
            if (stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.MELON_SEEDS)
                    || stack.isOf(Items.PUMPKIN_SEEDS) || stack.isOf(Items.BEETROOT_SEEDS)
                    || stack.isOf(Items.TORCHFLOWER_SEEDS) || stack.isOf(Items.PITCHER_POD)) {
                return 1.0f;
            }
        }
        // Potato/carrot heals 2 HP (1 heart) for pig
        if (type == EntityType.PIG) {
            if (stack.isOf(Items.POTATO) || stack.isOf(Items.CARROT)) {
                return 2.0f;
            }
        }
        if (type == EntityType.SNIFFER && stack.isOf(Items.TORCHFLOWER_SEEDS)) {
            return 2.0f;
        }
        return 0.0f;
    }

    public static String getFoodErrorMessage(EntityType<?> type) {
        if (type == EntityType.COW)       return "§cAs a cow, you can only heal from wheat.";
        if (type == EntityType.MOOSHROOM) return "§cAs a mooshroom, you can only heal from wheat.";
        if (type == EntityType.SHEEP)     return "§cAs a sheep, you can only heal from wheat.";
        if (type == EntityType.CHICKEN)   return "§cAs a chicken, you can only heal from seeds.";
        if (type == EntityType.PIG)       return "§cAs a pig, you can only heal from potatoes and carrots.";
        if (type == EntityType.SNIFFER)   return "§cAs a sniffer, you can only heal from torchflower seeds.";
        return "§cYou cannot eat that.";
    }

    // ── State checks ─────────────────────────────────────────────────────────

    public static boolean isPassiveMobPossessing(PlayerEntity player) {
        EntityType<?> type = PossessionManager.getPossessedType(player);
        return type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.PIG
                || type == EntityType.SHEEP || type == EntityType.CHICKEN
                || type == EntityType.SNIFFER;
    }

    public static boolean isPassiveMobType(EntityType<?> type) {
        return type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.PIG
                || type == EntityType.SHEEP || type == EntityType.CHICKEN
                || type == EntityType.SNIFFER;
    }

    public static boolean isSnifferAlwaysHostile(Entity entity) {
        return entity instanceof ZoglinEntity
                || entity instanceof WitherEntity
                || entity instanceof WardenEntity;
    }
}






