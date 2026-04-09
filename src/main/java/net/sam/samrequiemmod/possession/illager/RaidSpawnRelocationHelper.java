package net.sam.samrequiemmod.possession.illager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.village.raid.Raid;

import java.util.HashSet;
import java.util.Set;

public final class RaidSpawnRelocationHelper {

    private static final String RAID_SPAWN_CHECKED_TAG = "samrequiemmod_raid_spawn_checked";
    private static final int RELOCATION_CHECK_INTERVAL = 10;
    private static final double RAID_SEARCH_RADIUS = 80.0;

    private RaidSpawnRelocationHelper() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % RELOCATION_CHECK_INTERVAL != 0L) return;

            Set<BlockPos> seenRaidCenters = new HashSet<>();
            for (ServerPlayerEntity player : world.getPlayers()) {
                Raid raid = world.getRaidManager().getRaidAt(player.getBlockPos(), 9216);
                if (raid == null || !raid.isActive()) continue;
                if (!seenRaidCenters.add(raid.getCenter().toImmutable())) continue;

                Box raidBox = Box.of(raid.getCenter().toCenterPos(), RAID_SEARCH_RADIUS * 2.0, 96.0, RAID_SEARCH_RADIUS * 2.0);
                for (RaiderEntity raider : world.getEntitiesByClass(RaiderEntity.class, raidBox, RaiderEntity::isAlive)) {
                    if (raider.getCommandTags().contains(RAID_SPAWN_CHECKED_TAG)) continue;
                    if (!raider.hasActiveRaid() || raider.getRaid() != raid) continue;

                    raider.addCommandTag(RAID_SPAWN_CHECKED_TAG);

                    BlockPos betterSpawn = findVillageTeleportSpawn(world, raid, raider);
                    if (betterSpawn == null) continue;

                    raider.refreshPositionAndAngles(
                            betterSpawn.getX() + 0.5,
                            betterSpawn.getY(),
                            betterSpawn.getZ() + 0.5,
                            raider.getYaw(),
                            raider.getPitch()
                    );
                    raider.setOnGround(true);
                    raider.fallDistance = 0.0f;
                    raider.getNavigation().stop();
                    raider.velocityDirty = true;
                }
            }
        });
    }

    private static BlockPos findVillageTeleportSpawn(ServerWorld world, Raid raid, RaiderEntity raider) {
        BlockPos centerSurface = toSurface(world, raid.getCenter());
        int[][] offsets = {
                {0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {4, 0}, {-4, 0}, {0, 4}, {0, -4},
                {2, 2}, {2, -2}, {-2, 2}, {-2, -2},
                {6, 0}, {-6, 0}, {0, 6}, {0, -6},
                {4, 2}, {4, -2}, {-4, 2}, {-4, -2},
                {2, 4}, {2, -4}, {-2, 4}, {-2, -4}
        };

        int startIndex = Math.floorMod(raider.getUuid().hashCode(), offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            int[] offset = offsets[(startIndex + i) % offsets.length];
            BlockPos candidate = toSurface(world, centerSurface.add(offset[0], 0, offset[1]));
            if (isGoodRaidSpawn(world, raid, candidate)) {
                return candidate;
            }
        }

        for (int radius = 8; radius <= 24; radius += 4) {
            for (int angle = 0; angle < 360; angle += 15) {
                double radians = Math.toRadians(angle);
                int x = centerSurface.getX() + MathHelper.floor(Math.cos(radians) * radius);
                int z = centerSurface.getZ() + MathHelper.floor(Math.sin(radians) * radius);
                BlockPos candidate = toSurface(world, new BlockPos(x, centerSurface.getY(), z));

                if (!isGoodRaidSpawn(world, raid, candidate)) continue;
                return candidate;
            }
        }

        return null;
    }

    private static boolean isGoodRaidSpawn(ServerWorld world, Raid raid, BlockPos pos) {
        BlockPos surface = toSurface(world, pos);
        if (!world.isInBuildLimit(surface)) return false;
        if (!world.isSkyVisible(surface.up())) return false;
        if (!world.isNearOccupiedPointOfInterest(surface, 48)) return false;

        // If the actual raider position is significantly below the top surface,
        // it spawned in a cave or buried pocket and should be relocated.
        if (surface.getY() - pos.getY() > 3) return false;
        if (!world.isSkyVisible(pos.up())) return false;

        BlockState feetState = world.getBlockState(surface);
        BlockState groundState = world.getBlockState(surface.down());
        FluidState feetFluid = world.getFluidState(surface);
        FluidState groundFluid = world.getFluidState(surface.down());

        if (!feetState.isAir()) return false;
        if (!feetFluid.isEmpty() || !groundFluid.isEmpty()) return false;
        if (!groundState.blocksMovement()) return false;

        BlockPos centerSurface = toSurface(world, raid.getCenter());
        if (Math.abs(surface.getY() - centerSurface.getY()) > 8) return false;

        int waterSamples = 0;
        int steepSamples = 0;
        for (int i = 1; i <= 8; i++) {
            double progress = i / 8.0;
            int sampleX = MathHelper.floor(MathHelper.lerp(progress, centerSurface.getX(), surface.getX()));
            int sampleZ = MathHelper.floor(MathHelper.lerp(progress, centerSurface.getZ(), surface.getZ()));
            BlockPos sample = toSurface(world, new BlockPos(sampleX, centerSurface.getY(), sampleZ));

            if (!world.isSkyVisible(sample.up())) return false;
            if (!world.getFluidState(sample).isEmpty() || !world.getFluidState(sample.down()).isEmpty()) {
                waterSamples++;
            }
            if (Math.abs(sample.getY() - centerSurface.getY()) > 6) {
                steepSamples++;
            }
        }

        return waterSamples == 0 && steepSamples <= 1;
    }

    private static BlockPos toSurface(ServerWorld world, BlockPos pos) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
    }
}
