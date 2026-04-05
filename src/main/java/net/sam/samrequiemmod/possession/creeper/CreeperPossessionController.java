package net.sam.samrequiemmod.possession.creeper;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.sam.samrequiemmod.possession.PossessionManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CreeperPossessionController {

    private CreeperPossessionController() {}

    /** Fuse duration in ticks (vanilla creeper = 30 ticks = 1.5 seconds). */
    private static final int FUSE_TIME = 30;

    /** Players currently charging — maps UUID to current fuse tick count. */
    private static final Map<UUID, Integer> CHARGING_PLAYERS = new ConcurrentHashMap<>();

    /** Players struck by lightning — permanently charged until unpossess. */
    private static final Set<UUID> CHARGED_PLAYERS = ConcurrentHashMap.newKeySet();

    /** Players currently immune to their own explosion (brief window). */
    public static final Set<UUID> EXPLOSION_IMMUNE = ConcurrentHashMap.newKeySet();

    // ── Query ──────────────────────────────────────────────────────────────────

    public static boolean isCreeperPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.CREEPER;
    }

    public static boolean isCharging(UUID uuid) {
        return CHARGING_PLAYERS.containsKey(uuid);
    }

    public static int getFuseTicks(UUID uuid) {
        return CHARGING_PLAYERS.getOrDefault(uuid, 0);
    }

    public static boolean isCharged(UUID uuid) {
        return CHARGED_PLAYERS.contains(uuid);
    }

    // ── Registration ───────────────────────────────────────────────────────────

    public static void register() {

        // Left-click on entity: block melee damage (creepers can't hit mobs)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!isCreeperPossessing(sp)) return ActionResult.PASS;
            // Block all melee attacks — creepers don't melee
            return ActionResult.FAIL;
        });

        // Damage handling: lightning detection + hurt sound
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isCreeperPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            // Lightning strike → become charged creeper
            if (source.isOf(DamageTypes.LIGHTNING_BOLT)) {
                CHARGED_PLAYERS.add(player.getUuid());
                // Broadcast charged state to clients
                CreeperNetworking.broadcastChargeSync(player, isCharging(player.getUuid()),
                        getFuseTicks(player.getUuid()), true);
                player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_CREEPER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                return false; // don't take the lightning damage
            }

            // Play creeper hurt sound for other damage
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_CREEPER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return true;
        });

        // Death sound
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return;
            if (!isCreeperPossessing(player)) return;
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_CREEPER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
        });
    }

    // ── Server tick ────────────────────────────────────────────────────────────

    public static void tick(ServerPlayerEntity player) {
        if (!isCreeperPossessing(player)) return;

        lockHunger(player);
        tickFuse(player);
        ambientSound(player);
    }

    private static void lockHunger(ServerPlayerEntity player) {
        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);
    }

    private static void tickFuse(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Integer fuseTicks = CHARGING_PLAYERS.get(uuid);
        if (fuseTicks == null) return;

        int newFuse = fuseTicks + 1;
        if (newFuse >= FUSE_TIME) {
            // Fuse complete — explode!
            CHARGING_PLAYERS.remove(uuid);
            explode(player);
        } else {
            CHARGING_PLAYERS.put(uuid, newFuse);
            // Broadcast fuse progress to clients
            CreeperNetworking.broadcastChargeSync(player, true, newFuse, isCharged(uuid));
        }
    }

    private static void ambientSound(ServerPlayerEntity player) {
        if (player.age % 160 == 0 && player.getRandom().nextFloat() < 0.3f) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 0.3f, 1.0f);
        }
    }

    // ── Charge toggle (called from networking) ─────────────────────────────────

    public static void handleChargeToggle(ServerPlayerEntity player) {
        if (!isCreeperPossessing(player)) return;
        UUID uuid = player.getUuid();

        if (CHARGING_PLAYERS.containsKey(uuid)) {
            // Cancel charge
            CHARGING_PLAYERS.remove(uuid);
            CreeperNetworking.broadcastChargeSync(player, false, 0, isCharged(uuid));
        } else {
            // Start charging
            CHARGING_PLAYERS.put(uuid, 0);
            // Play fuse ignite sound
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.PLAYERS, 1.0f, 0.5f);
            CreeperNetworking.broadcastChargeSync(player, true, 0, isCharged(uuid));
        }
    }

    // ── Explosion ──────────────────────────────────────────────────────────────

    private static void explode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        World world = player.getEntityWorld();
        float power = CHARGED_PLAYERS.contains(uuid) ? 6.0f : 3.0f;

        // Grant brief explosion immunity so player survives their own blast
        EXPLOSION_IMMUNE.add(uuid);

        // Create the explosion
        world.createExplosion(player, player.getX(), player.getY(), player.getZ(),
                power, World.ExplosionSourceType.MOB);

        // Remove explosion immunity after a short delay
        player.getEntityWorld().getServer().execute(() -> EXPLOSION_IMMUNE.remove(uuid));

        // Broadcast cleared charge state
        CreeperNetworking.broadcastChargeSync(player, false, 0, isCharged(uuid));

        // Unpossess — return to player form
        CHARGED_PLAYERS.remove(uuid);
        PossessionManager.clearPossession(player);

        // Apply post-possession protections (same pattern as death handler)
        player.getEntityWorld().getServer().execute(() -> {
            if (player.getHealth() <= 0) player.setHealth(1.0f);
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 100, 0, false, false));
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE, 100, 4, false, false));
            // Clear mob targets on this player
            net.minecraft.util.math.Box box = player.getBoundingBox().expand(48.0);
            for (net.minecraft.entity.mob.MobEntity mob : player.getEntityWorld()
                    .getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box, m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
        });
    }

    // ── Food helpers ───────────────────────────────────────────────────────────

    public static boolean isCreeperFood(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isOf(Items.GUNPOWDER);
    }

    public static float getCreeperFoodHealing(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.isOf(Items.GUNPOWDER)) return 3.0f; // 1.5 hearts
        return 0;
    }

    // ── Unpossess cleanup ──────────────────────────────────────────────────────

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        cleanup(uuid);
        CreeperNetworking.broadcastChargeSync(player, false, 0, false);
    }

    public static void onUnpossessUuid(UUID uuid) {
        cleanup(uuid);
    }

    private static void cleanup(UUID uuid) {
        CHARGING_PLAYERS.remove(uuid);
        CHARGED_PLAYERS.remove(uuid);
        EXPLOSION_IMMUNE.remove(uuid);
    }
}






