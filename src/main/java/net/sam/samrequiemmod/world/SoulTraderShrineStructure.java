package net.sam.samrequiemmod.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.Heightmap;

import java.util.Optional;

public class SoulTraderShrineStructure extends Structure {

    public static final MapCodec<SoulTraderShrineStructure> CODEC =
            createCodec(SoulTraderShrineStructure::new);

    public SoulTraderShrineStructure(Config config) {
        super(config);
    }

    @Override
    public Optional<StructurePosition> getStructurePosition(Context context) {
        BlockPos pos = context.chunkPos().getCenterAtY(0);

        // Use OCEAN_FLOOR_WG for Nether so we get the floor, not the bedrock ceiling
        Heightmap.Type heightmapType = Heightmap.Type.OCEAN_FLOOR_WG;

        int surfaceY = context.chunkGenerator().getHeightInGround(
                pos.getX(), pos.getZ(),
                heightmapType,
                context.world(),
                context.noiseConfig()
        );

        // In the Nether clamp to a reasonable floor range (32-90)
        if (surfaceY < 32 || surfaceY > 90) return Optional.empty();

        BlockPos centre = new BlockPos(pos.getX(), surfaceY, pos.getZ());

        // Reject water: if world surface Y is higher than ocean floor Y,
        // the surface is covered by water — skip this position
        int worldSurfaceY = context.chunkGenerator().getHeightInGround(
                pos.getX(), pos.getZ(),
                Heightmap.Type.WORLD_SURFACE_WG,
                context.world(),
                context.noiseConfig()
        );
        if (worldSurfaceY > surfaceY + 1) return Optional.empty();

        return Optional.of(new StructurePosition(centre, collector ->
                collector.addPiece(new SoulTraderShrinePiece(centre))
        ));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.SOUL_TRADER_SHRINE_TYPE;
    }
}