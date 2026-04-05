package net.sam.samrequiemmod.possession.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.sam.samrequiemmod.item.ModItems;
import net.sam.samrequiemmod.possession.PossessionHurtSoundHelper;
import net.sam.samrequiemmod.possession.PossessionManager;
import net.sam.samrequiemmod.possession.zombie.ZombieTargetingState;
import net.minecraft.world.Difficulty;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;
import java.util.UUID;

public final class PandaPossessionController {

    private PandaPossessionController() {}

    public static boolean isPandaPossessing(PlayerEntity player) {
        return PossessionManager.getPossessedType(player) == EntityType.PANDA;
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!isPandaPossessing(serverPlayer)) return ActionResult.PASS;
            if (player.getMainHandStack().isOf(ModItems.POSSESSION_RELIC)) return ActionResult.PASS;
            if (!(entity instanceof LivingEntity target)) return ActionResult.FAIL;

            boolean damaged = target.damage(
                    (net.minecraft.server.world.ServerWorld) target.getEntityWorld(),
                    serverPlayer.getDamageSources().playerAttack(serverPlayer),
                    getMeleeDamage(serverPlayer.getEntityWorld().getDifficulty())
            );
            serverPlayer.swingHand(hand, true);
            if (damaged) {
                markProvoked(target, serverPlayer);
                serverPlayer.getEntityWorld().playSound(
                        null,
                        serverPlayer.getX(),
                        serverPlayer.getY(),
                        serverPlayer.getZ(),
                        SoundEvents.ENTITY_PANDA_BITE,
                        SoundCategory.PLAYERS,
                        1.0f,
                        getPitch(serverPlayer)
                );
            }
            return ActionResult.SUCCESS;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            if (!isPandaPossessing(player)) return true;
            if (net.sam.samrequiemmod.possession.PossessionDamageHelper.isHarmlessSlimeContact(source)) return true;

            PossessionHurtSoundHelper.playIfReady(player, SoundEvents.ENTITY_PANDA_HURT, getPitch(player));
            if (source.getAttacker() instanceof LivingEntity attacker) {
                markProvoked(attacker, player);
                rallyNearbyPandas(player, attacker);
            }
            return true;
        });
    }

    public static void tick(ServerPlayerEntity player) {
        if (!isPandaPossessing(player)) return;

        player.getHungerManager().setFoodLevel(7);
        player.getHungerManager().setSaturationLevel(0.0f);

        if (player.age % 140 == 0 && player.getRandom().nextFloat() < 0.35f) {
            player.getEntityWorld().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    BabyPassiveMobState.isServerBaby(player)
                            ? SoundEvents.ENTITY_PANDA_PRE_SNEEZE
                            : SoundEvents.ENTITY_PANDA_AMBIENT,
                    SoundCategory.PLAYERS,
                    1.0f,
                    getPitch(player)
            );
        }
    }

    public static void onUnpossess(ServerPlayerEntity player) {
        onUnpossessUuid(player.getUuid());
    }

    public static void onUnpossessUuid(UUID uuid) {
        PandaAppearanceState.clear(uuid);
    }

    public static boolean isPandaFood(ItemStack stack) {
        return stack.isOf(Items.BAMBOO);
    }

    public static float getPandaFoodHealing(ItemStack stack) {
        return isPandaFood(stack) ? 4.0f : 0.0f;
    }

    public static String getFoodErrorMessage() {
        return "§cAs a panda, you can only heal from bamboo.";
    }

    private static void rallyNearbyPandas(ServerPlayerEntity player, LivingEntity attacker) {
        List<PandaEntity> pandas = player.getEntityWorld().getEntitiesByClass(
                PandaEntity.class,
                player.getBoundingBox().expand(10.0),
                LivingEntity::isAlive
        );
        for (PandaEntity panda : pandas) {
            panda.setTarget(attacker);
            panda.setAttacker(attacker);
        }
    }

    private static void markProvoked(Entity entity, ServerPlayerEntity player) {
        if (entity instanceof MobEntity mob) {
            ZombieTargetingState.markProvoked(mob.getUuid(), player.getUuid());
            mob.setAttacker(player);
            if (mob.getTarget() == null) {
                mob.setTarget(player);
            }
        }
    }

    private static float getPitch(PlayerEntity player) {
        return BabyPassiveMobState.isBaby(player) ? 1.35f : 1.0f;
    }

    private static float getMeleeDamage(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 4.0f;
            case NORMAL -> 6.0f;
            case HARD -> 9.0f;
            default -> 6.0f;
        };
    }
}
