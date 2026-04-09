package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IllagerRavagerCallController {

    private static final int SUMMON_COOLDOWN_TICKS = 300;
    private static final String CALL_NAME = "Ravager Call";
    private static final Map<UUID, UUID> SUMMONED_RAVAGER = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_COOLDOWN_START = new ConcurrentHashMap<>();

    private IllagerRavagerCallController() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isEligible(serverPlayer)) return ActionResult.PASS;

            ItemStack held = player.getStackInHand(hand);
            if (!isCallItem(held)) return ActionResult.PASS;

        handleUse(serverPlayer);
        return ActionResult.SUCCESS;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isEligible(player)) return;

        ensureCallItem(player);

        UUID ravagerUuid = SUMMONED_RAVAGER.get(player.getUuid());
        if (ravagerUuid == null) return;

        Entity entity = player.getEntityWorld().getEntity(ravagerUuid);
        if (!(entity instanceof RavagerEntity ravager) || !ravager.isAlive()) {
            SUMMONED_RAVAGER.remove(player.getUuid());
            LAST_COOLDOWN_START.put(player.getUuid(), (long) player.age);
            return;
        }

        ravager.setPersistent();

        if (ravager.getTarget() == null) {
            double distSq = ravager.squaredDistanceTo(player);
            if (distSq > 144.0) {
                tryTeleportToOwner(ravager, player);
            } else if (distSq > 16.0) {
                ravager.getNavigation().startMovingTo(player, 1.35);
            } else {
                ravager.getNavigation().stop();
            }
        }
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        UUID ravagerUuid = SUMMONED_RAVAGER.remove(uuid);
        if (ravagerUuid != null) {
            Entity entity = player.getEntityWorld().getEntity(ravagerUuid);
            if (entity != null) {
                entity.discard();
            }
        }
        removeCallItems(player);
        LAST_COOLDOWN_START.remove(uuid);
    }

    public static void onUnpossessUuid(UUID uuid) {
        SUMMONED_RAVAGER.remove(uuid);
        LAST_COOLDOWN_START.remove(uuid);
    }

    private static void handleUse(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        UUID ravagerUuid = SUMMONED_RAVAGER.get(uuid);
        if (ravagerUuid != null) {
            Entity entity = player.getEntityWorld().getEntity(ravagerUuid);
            if (entity != null) {
                entity.discard();
            }
            SUMMONED_RAVAGER.remove(uuid);
            LAST_COOLDOWN_START.put(uuid, (long) player.age);
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.8f);
            return;
        }

        long lastCooldown = LAST_COOLDOWN_START.getOrDefault(uuid, -1000L);
        if (player.age - lastCooldown < SUMMON_COOLDOWN_TICKS) {
            player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 1.0f, 0.6f);
            return;
        }

        RavagerEntity ravager = EntityType.RAVAGER.create(player.getEntityWorld(), SpawnReason.MOB_SUMMONED);
        if (ravager == null) return;

        Vec3d forward = player.getRotationVec(1.0f);
        double spawnX = player.getX() + forward.x * 3.0;
        double spawnZ = player.getZ() + forward.z * 3.0;
        BlockPos base = BlockPos.ofFloored(spawnX, player.getY(), spawnZ);
        BlockPos spawn = findSpawnPos(player.getEntityWorld(), ravager, base);

        ravager.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYaw(), 0.0f);
        ravager.setPersistent();
        player.getEntityWorld().spawnEntity(ravager);
        SUMMONED_RAVAGER.put(uuid, ravager.getUuid());

        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.0f, 1.0f);
    }

    private static BlockPos findSpawnPos(ServerWorld world, RavagerEntity ravager, BlockPos base) {
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos check = base.up(dy);
            if (world.isSpaceEmpty(ravager, ravager.getType().getDimensions().getBoxAt(
                    check.getX() + 0.5, check.getY(), check.getZ() + 0.5))) {
                return check;
            }
        }
        return base;
    }

    private static void tryTeleportToOwner(RavagerEntity ravager, ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        for (int i = 0; i < 10; i++) {
            int dx = ravager.getRandom().nextBetween(-3, 3);
            int dz = ravager.getRandom().nextBetween(-3, 3);
            BlockPos base = BlockPos.ofFloored(player.getX() + dx, player.getY(), player.getZ() + dz);
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos check = base.up(dy);
                BlockPos below = check.down();
                if (world.getBlockState(below).isSolidBlock(world, below)
                        && world.isSpaceEmpty(ravager, ravager.getType().getDimensions().getBoxAt(
                        check.getX() + 0.5, check.getY(), check.getZ() + 0.5))) {
                    ravager.refreshPositionAndAngles(check.getX() + 0.5, check.getY(), check.getZ() + 0.5,
                            ravager.getYaw(), ravager.getPitch());
                    ravager.getNavigation().stop();
                    return;
                }
            }
        }
    }

    private static void ensureCallItem(ServerPlayerEntity player) {
        if (hasCallItem(player)) return;
        ItemStack stack = new ItemStack(Items.BELL);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(CALL_NAME));
        player.getInventory().offerOrDrop(stack);
    }

    private static boolean hasCallItem(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (isCallItem(player.getInventory().getStack(i))) return true;
        }
        return isCallItem(player.getOffHandStack());
    }

    private static boolean isCallItem(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.isOf(Items.BELL)
                && stack.contains(DataComponentTypes.CUSTOM_NAME)
                && CALL_NAME.equals(stack.get(DataComponentTypes.CUSTOM_NAME).getString());
    }

    private static void removeCallItems(ServerPlayerEntity player) {
        for (int i = player.getInventory().size() - 1; i >= 0; i--) {
            if (isCallItem(player.getInventory().getStack(i))) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
        if (isCallItem(player.getOffHandStack())) {
            player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static boolean isEligible(PlayerEntity player) {
        return PillagerPossessionController.isPillagerPossessing(player)
                || VindicatorPossessionController.isVindicatorPossessing(player)
                || EvokerPossessionController.isEvokerPossessing(player);
    }
}
