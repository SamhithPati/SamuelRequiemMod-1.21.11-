package net.sam.samrequiemmod.possession;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import java.util.Optional;
import net.minecraft.command.argument.IdentifierArgumentType;
public final class PossessionCommand {
    private PossessionCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("possess")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("entity_id", IdentifierArgumentType.identifier())
                                    .suggests((context, builder) -> {
                                        net.minecraft.registry.Registries.ENTITY_TYPE.getIds().forEach(id ->
                                                builder.suggest(id.toString())
                                        );
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> possess(
                                            ctx.getSource(),
                                            IdentifierArgumentType.getIdentifier(ctx, "entity_id")
                                    )))
            );

            dispatcher.register(
                    CommandManager.literal("unpossess")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ctx -> unpossess(ctx.getSource()))
            );

            dispatcher.register(
                    CommandManager.literal("possessionstatus")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ctx -> status(ctx.getSource()))
            );
        });
    }

    private static int possess(ServerCommandSource source, Identifier id) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (!net.minecraft.registry.Registries.ENTITY_TYPE.containsId(id)) {
                source.sendError(Text.literal("§cUnknown entity type: " + id));
                return 0;
            }

            EntityType<?> type = net.minecraft.registry.Registries.ENTITY_TYPE.get(id);

            if (type == EntityType.PLAYER) {
                source.sendError(Text.literal("§cYou cannot possess a player entity type."));
                return 0;
            }

            if (!type.isSummonable()) {
                source.sendError(Text.literal("§cThat entity type is not summonable: " + id));
                return 0;
            }

            PossessionManager.startPossession(player, type);

            source.sendFeedback(
                    () -> Text.literal("§aYou are now possessing: §f" + id),
                    false
            );
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int unpossess(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (!PossessionManager.isPossessing(player)) {
                source.sendFeedback(() -> Text.literal("§eYou are not currently possessing anything."), false);
                return 0;
            }

            PossessionManager.clearPossession(player);
            source.sendFeedback(() -> Text.literal("§aPossession cleared."), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int status(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (!PossessionManager.isPossessing(player)) {
                source.sendFeedback(() -> Text.literal("§eCurrent possession: none"), false);
                return 0;
            }

            EntityType<?> type = PossessionManager.getPossessedType(player);
            source.sendFeedback(
                    () -> Text.literal("§dCurrent possession: §f" + EntityType.getId(type)),
                    false
            );
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}