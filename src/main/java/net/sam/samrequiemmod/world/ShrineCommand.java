package net.sam.samrequiemmod.world;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeKeys;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.Random;

public class ShrineCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("shrine")
                            .requires(src -> src.hasPermissionLevel(2))
                            .then(CommandManager.literal("locate")
                                    .executes(ctx -> locateShrine(ctx.getSource())))
                            .then(CommandManager.literal("generate")
                                    .executes(ctx -> forceGenerate(ctx.getSource())))
                            .then(CommandManager.literal("findbiome")
                                    .executes(ctx -> findBiome(ctx.getSource())))
            );
        });
    }

    private static int locateShrine(ServerCommandSource source) {
        var shrines = ShrineWorldGenerator.getPlacedShrines();
        if (shrines.isEmpty()) {
            source.sendFeedback(() -> Text.literal(
                    "§eNo shrines yet. Use /shrine findbiome to locate a Dark Forest, teleport there, then wait a few seconds."), false);
            return 0;
        }
        source.sendFeedback(() -> Text.literal("§dKnown shrine locations:"), false);
        for (BlockPos pos : shrines) {
            source.sendFeedback(() -> Text.literal(
                    "§7  → " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
        }
        return shrines.size();
    }

    private static int forceGenerate(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();

            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++)
                    for (int y = 1; y <= 9; y++)
                        world.setBlockState(pos.add(x, y, z),
                                net.minecraft.block.Blocks.AIR.getDefaultState(), 3);

            SoulTraderShrine.generate(world, pos, new Random());
            ShrineWorldGenerator.addPlacedShrine(pos);
            source.sendFeedback(() -> Text.literal(
                    "§aShrine generated at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), true);
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§cError: " + e.getMessage()), false);
        }
        return 1;
    }

    private static int findBiome(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = player.getBlockPos();

            source.sendFeedback(() -> Text.literal("§eSearching for nearest Dark Forest..."), false);

            // locateBiome in MC 1.21: (Predicate<RegistryEntry<Biome>>, BlockPos, int radius, int horizontalStep, int verticalStep)
            var result = world.locateBiome(
                    b -> b.matchesKey(BiomeKeys.DARK_FOREST),
                    pos, 6400, 32, 64
            );

            if (result == null) {
                // Try soul sand valley too
                var netherResult = source.getServer().getWorld(net.minecraft.world.World.NETHER);
                source.sendFeedback(() -> Text.literal(
                        "§cNo Dark Forest within 6400 blocks of spawn.\n" +
                                "§eTip: Use §f/shrine generate §eto force spawn one at your location,\n" +
                                "§eor explore further and run §f/shrine findbiome §eagain."), false);
            } else {
                BlockPos found = result.getFirst();
                double dist = Math.sqrt(found.getSquaredDistance(pos));
                source.sendFeedback(() -> Text.literal(
                        "§dNearest Dark Forest: §f" + found.getX() + ", " + found.getZ() +
                                " §7(" + (int)dist + " blocks away)\n" +
                                "§eTeleport there: §f/tp @s " + found.getX() + " ~ " + found.getZ() + "\n" +
                                "§eThen wait ~5 seconds and run §f/shrine locate"), false);
            }
        } catch (Exception e) {
            source.sendFeedback(() -> Text.literal("§cError: " + e.getMessage()), false);
            SamuelRequiemMod.LOGGER.error("[ShrineGen] findbiome error", e);
        }
        return 1;
    }
}