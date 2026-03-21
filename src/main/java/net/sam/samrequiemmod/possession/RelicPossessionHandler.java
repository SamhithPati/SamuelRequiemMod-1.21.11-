package net.sam.samrequiemmod.possession;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedNetworking;
import net.sam.samrequiemmod.possession.drowned.BabyDrownedState;
import net.sam.samrequiemmod.possession.husk.BabyHuskNetworking;
import net.sam.samrequiemmod.possession.husk.BabyHuskState;
import net.sam.samrequiemmod.possession.zombie.BabyZombieNetworking;
import net.sam.samrequiemmod.possession.zombie.BabyZombieState;

public final class RelicPossessionHandler {

    private RelicPossessionHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!player.getStackInHand(hand).isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity livingTarget)) return ActionResult.PASS;
            if (!(entity instanceof MobEntity)) return ActionResult.PASS;

            if (entity instanceof PlayerEntity) {
                serverPlayer.sendMessage(Text.literal("§cYou cannot possess players."), true);
                return ActionResult.FAIL;
            }

            if (!entity.isAlive()) return ActionResult.FAIL;

            if (PossessionManager.isPossessing(serverPlayer)) {
                serverPlayer.sendMessage(Text.literal("§eYou are already possessing something."), true);
                return ActionResult.FAIL;
            }

            EntityType<?> type = livingTarget.getType();
            // Pillager: allowed mob type
            if (type == EntityType.PILLAGER) {
                float mobHealth = livingTarget.getHealth();
                PossessionManager.startPossession(serverPlayer, type, mobHealth);
                PossessionNetworking.broadcastPossessionSync(serverPlayer, type);
                entity.discard();
                return ActionResult.SUCCESS;
            }

            // Detect baby zombie (EntityType.ZOMBIE + isBaby)
            boolean isBabyZombie = (type == EntityType.ZOMBIE)
                    && (entity instanceof ZombieEntity zombie)
                    && zombie.isBaby();

            // Detect baby husk (EntityType.HUSK + isBaby)
            // HuskEntity extends ZombieEntity, so we check HuskEntity first
            boolean isBabyHusk = (type == EntityType.HUSK)
                    && (entity instanceof HuskEntity husk)
                    && husk.isBaby();

            // Detect baby drowned
            boolean isBabyDrowned = (type == EntityType.DROWNED)
                    && (entity instanceof DrownedEntity drowned)
                    && drowned.isBaby();

            // Set baby states BEFORE startPossession so PossessionEffects picks up the correct profile
            if (isBabyZombie)  BabyZombieState.setServerBaby(serverPlayer.getUuid(), true);
            if (isBabyHusk)    BabyHuskState.setServerBaby(serverPlayer.getUuid(), true);
            if (isBabyDrowned) BabyDrownedState.setServerBaby(serverPlayer.getUuid(), true);

            // Start possession — pass mob's current health so player health updates to match
            float mobHealth = livingTarget.getHealth();
            PossessionManager.startPossession(serverPlayer, type, mobHealth);

            // Broadcast baby states to clients after possession starts
            if (isBabyZombie)  BabyZombieNetworking.broadcastBabyZombieSync(serverPlayer, true);
            if (isBabyHusk)    BabyHuskNetworking.broadcastBabyHuskSync(serverPlayer, true);
            if (isBabyDrowned) BabyDrownedNetworking.broadcast(serverPlayer, true);

            entity.discard();

            serverPlayer.sendMessage(
                    Text.literal("§5You used the Possession Relic on §f"
                            + livingTarget.getType().getName().getString()
                            + (isBabyZombie || isBabyHusk || isBabyDrowned ? " §7(Baby)" : "")),
                    true
            );

            return ActionResult.SUCCESS;
        });
    }
}