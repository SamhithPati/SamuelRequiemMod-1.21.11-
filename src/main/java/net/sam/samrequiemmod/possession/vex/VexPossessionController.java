package net.sam.samrequiemmod.possession.vex;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Unit;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.UUID;

public final class VexPossessionController {

    private static final String SWORD_NAME = "Vex Blade";

    private VexPossessionController() {}

    public static boolean isVexPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.VEX;
    }

    public static boolean isVexAlly(Entity entity) {
        return entity instanceof VexEntity;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isVexPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (player.getAttackCooldownProgress(0.5f) < 0.9f) return ActionResult.SUCCESS;

            float damage = switch (sp.getEntityWorld().getDifficulty()) {
                case EASY -> 5.5f;
                case NORMAL -> 9.0f;
                case HARD -> 13.5f;
                default -> 9.0f;
            };
            target.damage(((net.minecraft.server.world.ServerWorld) target.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
            if (entity instanceof MobEntity mob && !isVexAlly(entity)) {
                ZombieTargetingState.markProvoked(mob.getUuid(), sp.getUuid());
            }
            sp.swingHand(hand, true);
            sp.getEntityWorld().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                    VexState.isServerAngry(sp.getUuid()) ? SoundEvents.ENTITY_VEX_CHARGE : SoundEvents.ENTITY_VEX_AMBIENT,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isVexPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            if (source.equals(player.getDamageSources().onFire())
                    || source.equals(player.getDamageSources().inFire())
                    || source.equals(player.getDamageSources().lava())
                    || source.equals(player.getDamageSources().hotFloor())
                    || source.equals(player.getDamageSources().drown())) {
                return false;
            }

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_VEX_HURT, 1.0f);

            if (source.getAttacker() instanceof LivingEntity attacker) {
                if (attacker instanceof MobEntity mob && !isVexAlly(attacker)) {
                    ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
                }
                if (!isVexAlly(attacker)) rallyNearbyVexes(player, attacker);
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isVexPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isVexPossessing(player)) return;

        lockHunger(player);
        preventHealing(player);
        enforceFlight(player);
        ensureSword(player);
        handleAmbientSound(player);
        aggroGolems(player);
        player.noClip = true;
        player.setOnFire(false);
    }

    public static void handleAngryToggle(ServerPlayerEntity player) {
        if (!isVexPossessing(player)) return;
        boolean angry = !VexState.isServerAngry(player.getUuid());
        VexState.setServerAngry(player.getUuid(), angry);
        VexNetworking.broadcastAngry(player, angry);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                angry ? SoundEvents.ENTITY_VEX_CHARGE : SoundEvents.ENTITY_VEX_AMBIENT,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public static boolean blocksFoodUse(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        return food != null;
    }

    public static String getFoodErrorMessage() {
        return "§cAs a vex, you cannot heal by eating.";
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().allowFlying = false;
            player.sendAbilitiesUpdate();
        }
        player.noClip = false;
        removeSword(player);
    }

    public static void onUnpossessUuid(UUID uuid) {
        VexState.clear(uuid);
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

    private static void ensureSword(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isVexSword(stack)) {
                if (!stack.contains(DataComponentTypes.UNBREAKABLE)) {
                    stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
                }
                return;
            }
        }

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        sword.set(DataComponentTypes.CUSTOM_NAME, Text.literal(SWORD_NAME));
        if (player.getInventory().getStack(0).isEmpty()) {
            player.getInventory().setStack(0, sword);
        } else {
            player.getInventory().offerOrDrop(sword);
        }
    }

    private static boolean isVexSword(ItemStack stack) {
        return stack.isOf(Items.IRON_SWORD)
                && stack.contains(DataComponentTypes.CUSTOM_NAME)
                && SWORD_NAME.equals(stack.get(DataComponentTypes.CUSTOM_NAME).getString());
    }

    private static void removeSword(ServerPlayerEntity player) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isVexSword(stack)) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static void handleAmbientSound(ServerPlayerEntity player) {
        if (player.age % 100 != 0) return;
        if (player.getRandom().nextFloat() >= 0.4f) return;
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                VexState.isServerAngry(player.getUuid()) ? SoundEvents.ENTITY_VEX_CHARGE : SoundEvents.ENTITY_VEX_AMBIENT,
                SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static void aggroGolems(ServerPlayerEntity player) {
        if (player.age % 10 != 0) return;
        Box box = player.getBoundingBox().expand(24.0);
        for (IronGolemEntity golem : player.getEntityWorld().getEntitiesByClass(IronGolemEntity.class, box, IronGolemEntity::isAlive)) {
            golem.setTarget(player);
            golem.setAngryAt(net.minecraft.entity.LazyEntityReference.of(player));
        }
    }

    private static void rallyNearbyVexes(ServerPlayerEntity player, LivingEntity attacker) {
        List<VexEntity> vexes = player.getEntityWorld().getEntitiesByClass(
                VexEntity.class, player.getBoundingBox().expand(30.0), VexEntity::isAlive);
        for (VexEntity vex : vexes) {
            vex.setTarget(attacker);
        }
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void preventHealing(ServerPlayerEntity player) {
        // HungerManagerMixin already blocks passive healing for possessed players.
        // Do not touch timeUntilRegen here: vanilla also uses it for hurt i-frames.
    }
}








