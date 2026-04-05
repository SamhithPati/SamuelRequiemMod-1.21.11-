package net.sam.samrequiemmod.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.sam.samrequiemmod.entity.ModEntities;

public class SoulTraderShrinePiece extends StructurePiece {

    private final BlockPos centre;

    public SoulTraderShrinePiece(BlockPos centre) {
        super(ModStructures.SOUL_TRADER_SHRINE_PIECE, 0,
                new BlockBox(
                        centre.getX() - 5, centre.getY() - 1, centre.getZ() - 5,
                        centre.getX() + 5, centre.getY() + 10, centre.getZ() + 5
                ));
        this.centre = centre;
    }

    public SoulTraderShrinePiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.SOUL_TRADER_SHRINE_PIECE, nbt);
        this.centre = BlockPos.fromLong(nbt.getLong("Centre").orElse(0L));
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putLong("Centre", centre.asLong());
    }

    @Override
    public void generate(StructureWorldAccess world,
                         StructureAccessor structureAccessor,
                         ChunkGenerator chunkGenerator,
                         Random random,
                         BlockBox chunkBox,
                         ChunkPos chunkPos,
                         BlockPos pivot) {

        if (!chunkBox.contains(centre)) return;

        // Clear space
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++)
                for (int y = 1; y <= 9; y++)
                    world.setBlockState(centre.add(x, y, z),
                            Blocks.AIR.getDefaultState(), 3);

        java.util.Random jRandom = new java.util.Random(centre.asLong());
        SoulTraderShrine.generate(world, centre, jRandom);

        // Soul Trader spawns only after all 3 waves are defeated (handled by SoulTraderShrine tick)
    }
}





