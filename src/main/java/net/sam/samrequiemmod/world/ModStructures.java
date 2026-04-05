package net.sam.samrequiemmod.world;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.sam.samrequiemmod.SamuelRequiemMod;

public class ModStructures {

    public static final StructurePieceType SOUL_TRADER_SHRINE_PIECE =
            Registry.register(Registries.STRUCTURE_PIECE,
                    Identifier.of(SamuelRequiemMod.MOD_ID, "soul_trader_shrine_piece"),
                    SoulTraderShrinePiece::new);

    public static final StructureType<SoulTraderShrineStructure> SOUL_TRADER_SHRINE_TYPE =
            Registry.register(Registries.STRUCTURE_TYPE,
                    Identifier.of(SamuelRequiemMod.MOD_ID, "soul_trader_shrine"),
                    () -> SoulTraderShrineStructure.CODEC);

    public static void register() {
        SamuelRequiemMod.LOGGER.info("Registering structures for " + SamuelRequiemMod.MOD_ID);
    }
}





