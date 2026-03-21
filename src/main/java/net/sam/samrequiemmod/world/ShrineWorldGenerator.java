package net.sam.samrequiemmod.world;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.sam.samrequiemmod.SamuelRequiemMod;

import java.util.*;

public class ShrineWorldGenerator {

    // Regions that have already been given a shrine attempt
    private static final Set<Long> ATTEMPTED_REGIONS = new HashSet<>();
    private static final List<BlockPos> PLACED_SHRINES = new ArrayList<>();

    // One shrine attempt per region (32x32 chunks = 512x512 blocks)
    private static final int REGION_CHUNKS = 32;

    public static List<BlockPos> getPlacedShrines() {
        return Collections.unmodifiableList(PLACED_SHRINES);
    }

    public static void addPlacedShrine(BlockPos pos) {
        PLACED_SHRINES.add(pos.toImmutable());
    }

    public static void register() {
        // Wave system tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                SoulTraderShrine.tick(world);
            }
        });

        // Every time ANY chunk loads, check if it belongs to a region
        // that hasn't had a shrine attempt yet
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!(world instanceof ServerWorld serverWorld)) return;
            boolean isOverworld = serverWorld.getRegistryKey().equals(World.OVERWORLD);
            boolean isNether    = serverWorld.getRegistryKey().equals(World.NETHER);
            if (!isOverworld && !isNether) return;

            ChunkPos chunkPos = chunk.getPos();
            // Convert chunk coords to region coords
            int regionX = Math.floorDiv(chunkPos.x, REGION_CHUNKS);
            int regionZ = Math.floorDiv(chunkPos.z, REGION_CHUNKS);

            long dimBit = isNether ? (1L << 62) : 0L;
            long key = dimBit | ((long)(regionX + 100000) << 20) | (long)(regionZ + 100000);

            if (ATTEMPTED_REGIONS.contains(key)) return;
            ATTEMPTED_REGIONS.add(key);

            // Schedule on the server thread (chunk load may fire off-thread)
            serverWorld.getServer().execute(() ->
                    tryGenerateInRegion(serverWorld, regionX, regionZ, isNether, chunkPos)
            );
        });
    }

    private static void tryGenerateInRegion(ServerWorld world,
                                            int regionX, int regionZ,
                                            boolean isNether,
                                            ChunkPos triggerChunk) {
        long seed = (long) regionX * 341873128712L
                + (long) regionZ * 132897987541L
                + world.getSeed() ^ 0x9AC2DEAD;
        Random random = new Random(seed);

        // 50% chance this region gets a shrine
        if (random.nextBoolean()) return;

        int sampleY = isNether ? 50 : 64;

        // Try 20 positions across the whole region to find a valid biome
        // Focus positions near the trigger chunk for better hit rate
        for (int attempt = 0; attempt < 20; attempt++) {
            // Mix: half attempts near trigger chunk, half random across region
            int chunkX, chunkZ;
            if (attempt < 10) {
                // Near the trigger chunk (within 8 chunks)
                chunkX = triggerChunk.x + random.nextInt(17) - 8;
                chunkZ = triggerChunk.z + random.nextInt(17) - 8;
            } else {
                // Anywhere in the region
                chunkX = regionX * REGION_CHUNKS + random.nextInt(REGION_CHUNKS);
                chunkZ = regionZ * REGION_CHUNKS + random.nextInt(REGION_CHUNKS);
            }

            int worldX = chunkX * 16 + random.nextInt(16);
            int worldZ = chunkZ * 16 + random.nextInt(16);

            // Check biome — works without chunk being loaded
            var biome = world.getBiome(new BlockPos(worldX, sampleY, worldZ));
            boolean validBiome =
                    biome.matchesKey(BiomeKeys.DARK_FOREST)      ||
                            biome.matchesKey(BiomeKeys.DEEP_DARK)        ||
                            biome.matchesKey(BiomeKeys.SOUL_SAND_VALLEY);

            if (!validBiome) continue;

            // Get surface Y
            int surfaceY;
            if (isNether) {
                surfaceY = 35;
                for (int y = 110; y > 30; y--) {
                    var state = world.getBlockState(new BlockPos(worldX, y, worldZ));
                    if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                        surfaceY = y + 1;
                        break;
                    }
                }
            } else {
                surfaceY = world.getTopY(
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
            }

            if (surfaceY < 30) continue;

            // Reject water
            var surface = world.getBlockState(
                    new BlockPos(worldX, surfaceY - 1, worldZ));
            if (surface.isOf(Blocks.WATER) || surface.getFluidState().isStill()) continue;

            BlockPos centre = new BlockPos(worldX, surfaceY, worldZ);

            // Clear space
            for (int x = -4; x <= 4; x++)
                for (int z = -4; z <= 4; z++)
                    for (int y = 1; y <= 9; y++)
                        world.setBlockState(centre.add(x, y, z),
                                Blocks.AIR.getDefaultState(), 3);

            PLACED_SHRINES.add(centre.toImmutable());
            SoulTraderShrine.generate(world, centre, random);
            SamuelRequiemMod.LOGGER.info("[ShrineGen] Shrine placed at {} in {}",
                    centre, world.getRegistryKey().getValue());
            return;
        }
    }
}