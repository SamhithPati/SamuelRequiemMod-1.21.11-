package net.sam.samrequiemmod.possession.piglin;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.util.Unit;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieAttackSyncNetworking;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adult zombified piglin possession controller.
 * Same health/speed/healing as other zombie subtypes.
 * Can't drown, can't swim. Immune to poison, harming heals.
 * If hit, all zombified piglins in 40 blocks rally.
 * Zombified piglins are passive even when hit.
 */
public final class ZombifiedPiglinPossessionController {

    private static final String GOLD_SPEAR_TAG = "samrequiemmod_gold_spear";

    private ZombifiedPiglinPossessionController() {}

    private static final Map<UUID, Long> LAST_HIT_TICK = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> ITEMS_GIVEN = ConcurrentHashMap.newKeySet();
    private static final int ARMS_RAISED_TICKS = 100; // 5 seconds

    public static boolean isZombifiedPiglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOMBIFIED_PIGLIN
                && !BabyZombifiedPiglinState.isServerBaby(player);
    }

    public static boolean isAnyZombifiedPiglinPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.ZOMBIFIED_PIGLIN;
    }

    public static void register() {

        // ── Attack ────────────────────────────────────────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isZombifiedPiglinPossessing(sp)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            // Zombified piglins always passive — deal damage but never provoke
            if (entity instanceof ZombifiedPiglinEntity) {
                float damage = calculateDamage(sp);
                livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);
                LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
                ZombieAttackSyncNetworking.broadcastZombieAttacking(sp, true);
                sp.swingHand(hand, true);
                return ActionResult.SUCCESS;
            }

            // Mark provoked
            if (entity instanceof MobEntity mob)
                ZombieTargetingState.markProvoked(mob.getUuid());

            float damage = calculateDamage(sp);
            boolean damaged = livingTarget.damage(((net.minecraft.server.world.ServerWorld) livingTarget.getEntityWorld()), sp.getDamageSources().playerAttack(sp), damage);

            if (damaged) {
                world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
                        SoundCategory.PLAYERS, 0.65f, 1.0f);
            }

            LAST_HIT_TICK.put(sp.getUuid(), (long) sp.age);
            ZombieAttackSyncNetworking.broadcastZombieAttacking(sp, true);
            sp.swingHand(hand, true);
            return ActionResult.SUCCESS;
        });

        // ── Hurt ──────────────────────────────────────────────────────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isZombifiedPiglinPossessing(player)) return true;

            // Poison immunity
            if (source.equals(player.getDamageSources().magic())) {
                if (player.hasStatusEffect(StatusEffects.POISON)) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    return false;
                }
            }

            Entity attacker = source.getAttacker();
            if (attacker instanceof net.minecraft.entity.mob.SlimeEntity) return true;

            net.sam.samrequiemmod.possession.PossessionHurtSoundHelper.playIfReady(
                    player, SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_HURT, 1.0f);

            if (attacker == null) return true;

            if (!(attacker instanceof ZombifiedPiglinEntity))
                ZombieTargetingState.markProvoked(attacker.getUuid(), player.getUuid());

            rallyNearbyZombifiedPiglins(player, attacker);
            return true;
        });

        // ── Death ─────────────────────────────────────────────────────────────
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isZombifiedPiglinPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isZombifiedPiglinPossessing(player)) {
            if (LAST_HIT_TICK.containsKey(player.getUuid())) {
                LAST_HIT_TICK.remove(player.getUuid());
                ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
            }
            return;
        }

        lockHunger(player);
        preventNaturalHealing(player);
        preventSwimming(player);
        preventDrowning(player);
        handlePoisonImmunity(player);
        handleHarmingHeals(player);
        handleFireImmunity(player);
        ensureZombifiedPiglinItems(player);
        tickArmsRaised(player);

        // Ambient sound
        if (player.age % 160 == 0 && player.getRandom().nextFloat() < 0.45f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────
    private static float calculateDamage(ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        Difficulty diff = player.getEntityWorld().getDifficulty();
        // Golden sword: custom damage
        if (held.isOf(Items.GOLDEN_SWORD)) {
            return switch (diff) {
                case EASY   -> 5.0f;   // 2.5 hearts
                case NORMAL -> 9.0f;   // 4.5 hearts
                case HARD   -> 13.5f;  // 6.75 hearts
                default     -> 9.0f;
            };
        }
        // Golden axe: custom damage
        if (held.isOf(Items.GOLDEN_AXE)) {
            return switch (diff) {
                case EASY   -> 6.0f;   // 3 hearts
                case NORMAL -> 10.0f;  // 5 hearts
                case HARD   -> 15.0f;  // 7.5 hearts
                default     -> 10.0f;
            };
        }
        if (held.isOf(Items.GOLDEN_SPEAR)) {
            return switch (diff) {
                case EASY   -> 5.0f;   // 2.5 hearts
                case NORMAL -> 9.0f;   // 4.5 hearts
                case HARD   -> 13.5f;  // 6.75 hearts
                default     -> 9.0f;
            };
        }
        // Better weapons (netherite, diamond, etc.) use vanilla damage
        double attr = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        if (attr > 2.0) return (float) attr;
        // Bare hands
        return switch (diff) {
            case EASY   -> 3.0f;   // 1.5 hearts
            case NORMAL -> 5.0f;   // 2.5 hearts
            case HARD   -> 7.5f;   // 3.75 hearts
            default     -> 5.0f;
        };
    }

    // ── Rally ─────────────────────────────────────────────────────────────────
    static void rallyNearbyZombifiedPiglins(ServerPlayerEntity player, Entity threat) {
        if (!(threat instanceof LivingEntity livingThreat)) return;
        Box box = player.getBoundingBox().expand(40.0);
        List<ZombifiedPiglinEntity> nearby = player.getEntityWorld().getEntitiesByClass(
                ZombifiedPiglinEntity.class, box, z -> z.isAlive());
        for (ZombifiedPiglinEntity zp : nearby) {
            zp.setTarget(livingThreat);
        }
    }

    // ── Tick helpers ──────────────────────────────────────────────────────────
    private static void tickArmsRaised(ServerPlayerEntity player) {
        Long lastHit = LAST_HIT_TICK.get(player.getUuid());
        if (lastHit == null) return;
        if ((long) player.age - lastHit >= ARMS_RAISED_TICKS) {
            LAST_HIT_TICK.remove(player.getUuid());
            ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
        }
    }

    static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    static void preventNaturalHealing(ServerPlayerEntity player) {
        // HungerManagerMixin already blocks passive healing for possessed players.
        // Do not touch timeUntilRegen here: vanilla also uses it for hurt i-frames.
    }

    static void preventSwimming(ServerPlayerEntity player) {
        net.sam.samrequiemmod.possession.NoSwimPossessionHelper.disableSwimmingPose(player);
    }

    static void preventDrowning(ServerPlayerEntity player) {
        if (player.isSubmergedInWater()) player.setAir(player.getMaxAir());
    }

    static void handlePoisonImmunity(ServerPlayerEntity player) {
        if (player.hasStatusEffect(StatusEffects.POISON))
            player.removeStatusEffect(StatusEffects.POISON);
    }

    static void handleHarmingHeals(ServerPlayerEntity player) {
        StatusEffectInstance harming = player.getStatusEffect(StatusEffects.INSTANT_DAMAGE);
        if (harming == null) return;
        float healAmount = 6.0f * (float) Math.pow(2, harming.getAmplifier());
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        player.removeStatusEffect(StatusEffects.INSTANT_DAMAGE);
    }

    // ── Fire/lava immunity ──────────────────────────────────────────────────
    static void handleFireImmunity(ServerPlayerEntity player) {
        if (player.isOnFire()) player.extinguish();
        if (player.getFireTicks() > 0) player.setFireTicks(0);
    }

    // ── Item management ───────────────────────────────────────────────────────
    private static void ensureZombifiedPiglinItems(ServerPlayerEntity player) {
        if (ITEMS_GIVEN.contains(player.getUuid())) {
            ensureUnbreakable(player, Items.GOLDEN_SWORD);
            ensureUnbreakable(player, Items.GOLDEN_AXE);
            ensureGoldSpear(player);
            return;
        }

        ItemStack sword = new ItemStack(Items.GOLDEN_SWORD);
        sword.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        ItemStack axe = new ItemStack(Items.GOLDEN_AXE);
        axe.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        ItemStack spear = createGoldSpear();

        giveToSlot(player, sword, 0);
        giveToSlot(player, axe, 1);
        giveToSlot(player, spear, 2);
        ITEMS_GIVEN.add(player.getUuid());
    }

    private static void giveToSlot(ServerPlayerEntity player, ItemStack stack, int slot) {
        if (player.getInventory().getStack(slot).isEmpty())
            player.getInventory().setStack(slot, stack);
        else
            player.getInventory().offerOrDrop(stack);
    }

    private static void ensureUnbreakable(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(item) && !s.contains(DataComponentTypes.UNBREAKABLE))
                s.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        }
    }

    private static ItemStack createGoldSpear() {
        ItemStack spear = new ItemStack(Items.GOLDEN_SPEAR);
        spear.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean(GOLD_SPEAR_TAG, true);
        spear.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        return spear;
    }

    private static boolean isGoldSpear(ItemStack stack) {
        if (!stack.isOf(Items.GOLDEN_SPEAR)) return false;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().contains(GOLD_SPEAR_TAG);
    }

    private static void ensureGoldSpear(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isGoldSpear(stack)) {
                if (!stack.contains(DataComponentTypes.UNBREAKABLE)) {
                    stack.set(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE);
                }
                return;
            }
        }
        player.getInventory().offerOrDrop(createGoldSpear());
    }

    private static void removeItemType(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--)
            if (player.getInventory().getStack(i).isOf(item))
                player.getInventory().setStack(i, ItemStack.EMPTY);
    }

    private static void removeGoldSpears(ServerPlayerEntity player) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            if (isGoldSpear(player.getInventory().getStack(i))) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
    }

    // ── Unpossess ─────────────────────────────────────────────────────────────
    public static void onUnpossess(ServerPlayerEntity player) {
        LAST_HIT_TICK.remove(player.getUuid());
        ITEMS_GIVEN.remove(player.getUuid());
        removeItemType(player, Items.GOLDEN_SWORD);
        removeItemType(player, Items.GOLDEN_AXE);
        removeGoldSpears(player);
        ZombieAttackSyncNetworking.broadcastZombieAttacking(player, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        LAST_HIT_TICK.remove(uuid);
        ITEMS_GIVEN.remove(uuid);
    }
}








