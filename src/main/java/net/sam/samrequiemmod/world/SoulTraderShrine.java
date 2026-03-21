package net.sam.samrequiemmod.world;

import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.sam.samrequiemmod.SamuelRequiemMod;
import net.sam.samrequiemmod.entity.ModEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedurally generates the Soul Trader Shrine and manages
 * the 3-wave mob encounter that must be cleared before the
 * Soul Trader spawns.
 */
public class SoulTraderShrine {

    // Tracks active shrines: shrine centre → wave state
    private static final java.util.Map<ShrineKey, WaveState> ACTIVE_SHRINES = new java.util.HashMap<>();
    private static final java.util.Set<ShrineKey> COMPLETED_SHRINES = new java.util.HashSet<>();
    private record ShrineKey(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey, BlockPos pos) {}
    // ── Structure generation ──────────────────────────────────────────────────

    /**
     * Places the shrine centred at the given position.
     * The centre block is where the Soul Trader will eventually stand.
     */
    public static void generate(StructureWorldAccess world, BlockPos centre, Random random) {
        buildFoundation(world, centre);
        buildPillars(world, centre);
        buildAltar(world, centre);
        buildRoof(world, centre);
        placeDecoration(world, centre, random);

        // Wave tracking is handled by autoRegisterNearbyStructures in tick()
    }

    private static void buildFoundation(StructureWorldAccess world, BlockPos c) {
        // 9x9 blackstone base, 2 layers deep
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.setBlockState(c.add(x, -1, z), Blocks.POLISHED_BLACKSTONE.getDefaultState(), 3);
                world.setBlockState(c.add(x, -2, z), Blocks.BLACKSTONE.getDefaultState(), 3);
                // Ground level floor
                world.setBlockState(c.add(x, 0, z), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 3);
            }
        }
        // Cracked/broken edges for creepy effect
        world.setBlockState(c.add(-4, 0, -4), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 3);
        world.setBlockState(c.add( 4, 0, -4), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 3);
        world.setBlockState(c.add(-4, 0,  4), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 3);
        world.setBlockState(c.add( 4, 0,  4), Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 3);
        // Soul sand ring inside the foundation
        for (int x = -3; x <= 3; x++) {
            world.setBlockState(c.add(x, 0, -3), Blocks.SOUL_SAND.getDefaultState(), 3);
            world.setBlockState(c.add(x, 0,  3), Blocks.SOUL_SAND.getDefaultState(), 3);
        }
        for (int z = -2; z <= 2; z++) {
            world.setBlockState(c.add(-3, 0, z), Blocks.SOUL_SAND.getDefaultState(), 3);
            world.setBlockState(c.add( 3, 0, z), Blocks.SOUL_SAND.getDefaultState(), 3);
        }
    }

    private static void buildPillars(StructureWorldAccess world, BlockPos c) {
        // Four corner pillars, 5 blocks tall
        int[][] corners = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] corner : corners) {
            int px = corner[0], pz = corner[1];
            for (int y = 1; y <= 5; y++) {
                world.setBlockState(c.add(px, y, pz), Blocks.BASALT.getDefaultState(), 3);
            }
            // Skull on top of each pillar
            world.setBlockState(c.add(px, 6, pz),
                    Blocks.SKELETON_SKULL.getDefaultState(), 3);
            // Soul lantern at mid-pillar
            world.setBlockState(c.add(px, 3, pz), Blocks.SOUL_LANTERN.getDefaultState(), 3);
        }
    }

    private static void buildAltar(StructureWorldAccess world, BlockPos c) {
        // Centre altar — 3x3 raised platform at y=1
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlockState(c.add(x, 1, z), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 3);
            }
        }
        // Centre altar block slightly raised
        world.setBlockState(c.add(0, 2, 0), Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState(), 3);
        // Soul fire on top of altar — the merchant will stand here when summoned
        world.setBlockState(c.add(0, 3, 0), Blocks.SOUL_FIRE.getDefaultState(), 3);
        // Crying obsidian ring around altar
        world.setBlockState(c.add(-1, 1, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), 3);
        world.setBlockState(c.add( 1, 1, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), 3);
        world.setBlockState(c.add( 0, 1,-1), Blocks.CRYING_OBSIDIAN.getDefaultState(), 3);
        world.setBlockState(c.add( 0, 1, 1), Blocks.CRYING_OBSIDIAN.getDefaultState(), 3);
    }

    private static void buildRoof(StructureWorldAccess world, BlockPos c) {
        // Dark oak slab roof connecting the four pillars at y=6
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // Outer frame
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    world.setBlockState(c.add(x, 6, z),
                            Blocks.DARK_OAK_SLAB.getDefaultState(), 3);
                }
            }
        }
        // Inner roof (y=7 centre)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlockState(c.add(x, 7, z),
                        Blocks.DARK_OAK_PLANKS.getDefaultState(), 3);
            }
        }
        // Central glowstone cluster in roof for eerie lighting
        world.setBlockState(c.add(0, 7, 0), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(c.add(1, 7, 0), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(c.add(-1,7, 0), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(c.add(0, 7, 1), Blocks.GLOWSTONE.getDefaultState(), 3);
        world.setBlockState(c.add(0, 7,-1), Blocks.GLOWSTONE.getDefaultState(), 3);
    }

    private static void placeDecoration(StructureWorldAccess world, BlockPos c, Random random) {
        // Wall skulls on the inside of each pillar face
        // North wall skulls
        world.setBlockState(c.add(-1, 4, -3),
                Blocks.SKELETON_SKULL.getDefaultState(), 3);
        world.setBlockState(c.add( 1, 4, -3),
                Blocks.SKELETON_SKULL.getDefaultState(), 3);
        // South wall skulls
        world.setBlockState(c.add(-1, 4,  3),
                Blocks.SKELETON_SKULL.getDefaultState(), 3);
        world.setBlockState(c.add( 1, 4,  3),
                Blocks.SKELETON_SKULL.getDefaultState(), 3);

        // Soul torches scattered around foundation
        world.setBlockState(c.add(-2, 1, -2), Blocks.SOUL_TORCH.getDefaultState(), 3);
        world.setBlockState(c.add( 2, 1, -2), Blocks.SOUL_TORCH.getDefaultState(), 3);
        world.setBlockState(c.add(-2, 1,  2), Blocks.SOUL_TORCH.getDefaultState(), 3);
        world.setBlockState(c.add( 2, 1,  2), Blocks.SOUL_TORCH.getDefaultState(), 3);

        // Chains hanging from roof corners
        world.setBlockState(c.add(-2, 6, -2), Blocks.CHAIN.getDefaultState(), 3);
        world.setBlockState(c.add( 2, 6, -2), Blocks.CHAIN.getDefaultState(), 3);
        world.setBlockState(c.add(-2, 6,  2), Blocks.CHAIN.getDefaultState(), 3);
        world.setBlockState(c.add( 2, 6,  2), Blocks.CHAIN.getDefaultState(), 3);

        // Random bone blocks scattered on soul sand ring
        int[] boneOffsets = {-2, 2};
        for (int ox : boneOffsets) {
            if (random.nextBoolean()) {
                world.setBlockState(c.add(ox, 1, -3), Blocks.BONE_BLOCK.getDefaultState(), 3);
            }
            if (random.nextBoolean()) {
                world.setBlockState(c.add(ox, 1,  3), Blocks.BONE_BLOCK.getDefaultState(), 3);
            }
        }
    }

    // ── Wave system ───────────────────────────────────────────────────────────

    private static void autoRegisterNearbyStructures(ServerWorld world) {
        for (var player : world.getPlayers()) {
            ChunkPos playerChunk = player.getChunkPos();
            for (int cx = -3; cx <= 3; cx++) {
                for (int cz = -3; cz <= 3; cz++) {
                    var chunk = world.getChunk(playerChunk.x + cx, playerChunk.z + cz,
                            net.minecraft.world.chunk.ChunkStatus.STRUCTURE_STARTS, false);
                    if (chunk == null) continue;

                    var starts = chunk.getStructureStarts();
                    for (StructureStart start : starts.values()) {
                        if (start == StructureStart.DEFAULT) continue;

                        // Use instanceof to identify our structure — avoids registry lookup issues
                        if (!(start.getStructure() instanceof SoulTraderShrineStructure)) continue;

                        BlockPos centre = start.getBoundingBox().getCenter().toImmutable();
                        ShrineKey key = new ShrineKey(world.getRegistryKey(), centre);

                        if (!ACTIVE_SHRINES.containsKey(key) && !COMPLETED_SHRINES.contains(key)) {
                            ACTIVE_SHRINES.put(key, new WaveState(world.getRegistryKey(), centre));
                            SamuelRequiemMod.LOGGER.info(
                                    "[ShrineWave] Registered shrine at {} in {}",
                                    centre, world.getRegistryKey().getValue()
                            );
                        }
                    }
                }
            }
        }
    }

    public static void tick(ServerWorld world) {
        autoRegisterNearbyStructures(world);
        List<ShrineKey> toRemove = new ArrayList<>();

        for (java.util.Map.Entry<ShrineKey, WaveState> entry : ACTIVE_SHRINES.entrySet()) {
            ShrineKey key = entry.getKey();
            WaveState state = entry.getValue();

            if (!key.worldKey().equals(world.getRegistryKey())) {
                continue;
            }

            BlockPos centre = key.pos();

            // Step 1: Wait for player to arrive
            if (!state.started) {
                boolean playerNear = false;
                for (var player : world.getPlayers()) {
                    if (player.getBlockPos().isWithinDistance(centre, 16)) {
                        playerNear = true;
                        break;
                    }
                }
                if (playerNear) {
                    state.started = true;
                    state.wave = 1;
                    state.waitingForDeath = false;
                    state.checkCooldown = 0;
                    startWave(world, centre, state);
                }
                continue;
            }

            // Step 2: Wave in progress — wait for mobs to die
            if (state.waitingForDeath) {
                if (state.checkCooldown > 0) {
                    state.checkCooldown--;
                    continue;
                }

                int stillAlive = 0;
                java.util.List<java.util.UUID> stillTracked = new java.util.ArrayList<>();

                for (java.util.UUID uuid : state.spawnedMobs) {
                    net.minecraft.entity.Entity entity = world.getEntity(uuid);
                    if (entity instanceof net.minecraft.entity.mob.MobEntity mob && mob.isAlive()) {
                        stillAlive++;
                        stillTracked.add(uuid);
                    }
                }

                state.spawnedMobs.clear();
                state.spawnedMobs.addAll(stillTracked);

                net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info(
                        "[Wave] Wave {} check: {} alive (tracked={})",
                        state.wave, stillAlive, state.spawnedMobs.size()
                );

                if (stillAlive > 0) continue;

                state.waitingForDeath = false;

                net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info(
                        "[Wave] Wave {} cleared at {}", state.wave, centre
                );

                if (state.wave < 3) {
                    state.wave++;
                    startWave(world, centre, state);
                } else {
                    net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info(
                            "[Wave] All 3 waves done, spawning trader"
                    );
                    spawnSoulTrader(world, centre);
                    COMPLETED_SHRINES.add(key);
                    toRemove.add(key);
                    broadcastNearby(world, centre, "§5§lThe Soul Trader emerges from the shadows...");
                }
            }
        }

        toRemove.forEach(ACTIVE_SHRINES::remove);
    }

    private static void startWave(ServerWorld world, BlockPos centre, WaveState state) {
        state.spawnedMobs.clear();
        state.waitingForDeath = false; // will be set to true after spawning
        state.checkCooldown = 20; // 5 second grace period so mobs fully load before checking
        int[][] waveComposition = {
                {3, 2},  // Wave 1: 3 zombies, 2 skeletons
                {4, 3},  // Wave 2: 4 zombies, 3 skeletons
                {5, 4},  // Wave 3: 5 zombies, 4 skeletons
        };
        int[] comp = waveComposition[state.wave - 1];
        int zombies = comp[0], skeletons = comp[1];

        net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info("[Wave] Starting wave {} at {}", state.wave, centre);
        broadcastNearby(world, centre,
                "§c§lWave " + state.wave + " begins! Defeat the guardians of the shrine!");

        // Spawn mobs at centre Y+2 — inside the shrine which is cleared to air.
        // This guarantees they spawn in valid air space and won't die from environment.
        int sy = centre.getY() + 2;
        BlockPos[] spawnPoints = {
                new BlockPos(centre.getX() - 3, sy, centre.getZ()),
                new BlockPos(centre.getX() + 3, sy, centre.getZ()),
                new BlockPos(centre.getX(), sy, centre.getZ() - 3),
                new BlockPos(centre.getX(), sy, centre.getZ() + 3),
                new BlockPos(centre.getX() - 2, sy, centre.getZ() - 2),
                new BlockPos(centre.getX() + 2, sy, centre.getZ() - 2),
                new BlockPos(centre.getX() - 2, sy, centre.getZ() + 2),
                new BlockPos(centre.getX() + 2, sy, centre.getZ() + 2)
        };

        int spawnIdx = 0;
        for (int i = 0; i < zombies; i++) {
            BlockPos sp = spawnPoints[spawnIdx % spawnPoints.length];
            spawnIdx++;
            ZombieEntity zombie = EntityType.ZOMBIE.create(world);
            if (zombie != null) {
                zombie.refreshPositionAndAngles(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0, 0);
                zombie.equipStack(EquipmentSlot.HEAD,     new ItemStack(Items.IRON_HELMET));
                zombie.equipStack(EquipmentSlot.CHEST,    new ItemStack(Items.IRON_CHESTPLATE));
                zombie.equipStack(EquipmentSlot.LEGS,     new ItemStack(Items.IRON_LEGGINGS));
                zombie.equipStack(EquipmentSlot.FEET,     new ItemStack(Items.IRON_BOOTS));
                zombie.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                zombie.initialize(world, world.getLocalDifficulty(sp), SpawnReason.STRUCTURE, null);
                zombie.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
                zombie.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.RESISTANCE, 100, 4, false, false));
                zombie.setHealth(zombie.getMaxHealth());
                zombie.setPersistent();

                boolean spawned = world.spawnEntity(zombie);
                if (spawned) {
                    state.spawnedMobs.add(zombie.getUuid());
                    SamuelRequiemMod.LOGGER.info("[Wave] Spawned zombie for wave {} at {}", state.wave, sp);
                } else {
                    SamuelRequiemMod.LOGGER.warn("[Wave] FAILED to spawn zombie for wave {} at {}", state.wave, sp);
                }
            }
        }

        for (int i = 0; i < skeletons; i++) {
            BlockPos sp = spawnPoints[spawnIdx % spawnPoints.length];
            spawnIdx++;
            SkeletonEntity skeleton = EntityType.SKELETON.create(world);
            if (skeleton != null) {
                skeleton.refreshPositionAndAngles(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0, 0);
                skeleton.equipStack(EquipmentSlot.HEAD,     new ItemStack(Items.IRON_HELMET));
                skeleton.equipStack(EquipmentSlot.CHEST,    new ItemStack(Items.IRON_CHESTPLATE));
                skeleton.equipStack(EquipmentSlot.LEGS,     new ItemStack(Items.IRON_LEGGINGS));
                skeleton.equipStack(EquipmentSlot.FEET,     new ItemStack(Items.IRON_BOOTS));
                skeleton.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.initialize(world, world.getLocalDifficulty(sp), SpawnReason.STRUCTURE, null);
                skeleton.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
                skeleton.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.RESISTANCE, 100, 4, false, false));
                skeleton.setHealth(skeleton.getMaxHealth());
                skeleton.setPersistent();

                boolean spawned = world.spawnEntity(skeleton);
                if (spawned) {
                    state.spawnedMobs.add(skeleton.getUuid());
                    SamuelRequiemMod.LOGGER.info("[Wave] Spawned skeleton for wave {} at {}", state.wave, sp);
                } else {
                    SamuelRequiemMod.LOGGER.warn("[Wave] FAILED to spawn skeleton for wave {} at {}", state.wave, sp);
                }
            }
        }
        if (state.spawnedMobs.isEmpty()) {
            SamuelRequiemMod.LOGGER.warn("[Wave] Wave {} spawned 0 mobs at {}", state.wave, centre);
            state.waitingForDeath = false;
            return;
        }

        state.waitingForDeath = true;
    }

    private static void spawnSoulTrader(ServerWorld world, BlockPos centre) {
        net.sam.samrequiemmod.SamuelRequiemMod.LOGGER.info("[Wave] spawnSoulTrader called at {}", centre);
        // Spawn on the altar (centre + 3 high)
        BlockPos spawnPos = centre.add(0, 3, 0);
        // Clear the soul fire first
        world.setBlockState(spawnPos, Blocks.AIR.getDefaultState(), 3);

        var trader = ModEntities.CORRUPTED_MERCHANT.create(world);
        if (trader != null) {
            trader.refreshPositionAndAngles(
                    spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            trader.setPersistent();
            world.spawnEntity(trader);
        }
    }

    private static void broadcastNearby(ServerWorld world, BlockPos centre, String message) {
        for (var player : world.getPlayers()) {
            if (player.getBlockPos().isWithinDistance(centre, 30)) {
                player.sendMessage(net.minecraft.text.Text.literal(message), false);
            }
        }
    }

    public static void registerShrine(ServerWorld world, BlockPos centre) {
        ShrineKey key = new ShrineKey(world.getRegistryKey(), centre.toImmutable());
        ACTIVE_SHRINES.put(key, new WaveState(world.getRegistryKey(), centre.toImmutable()));
    }

    // ── Wave state ────────────────────────────────────────────────────────────

    private static class WaveState {
        final net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey;
        final BlockPos centre;
        int wave = 0;
        boolean started = false;
        boolean waitingForDeath = false;
        int checkCooldown = 0;
        final List<java.util.UUID> spawnedMobs = new ArrayList<>();

        WaveState(net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey, BlockPos centre) {
            this.worldKey = worldKey;
            this.centre = centre;
        }
    }
}