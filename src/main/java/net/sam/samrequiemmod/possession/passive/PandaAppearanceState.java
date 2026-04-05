package net.sam.samrequiemmod.possession.passive;

import net.minecraft.entity.passive.PandaEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PandaAppearanceState {

    private static final Map<UUID, PandaEntity.Gene> SERVER_MAIN_GENES = new ConcurrentHashMap<>();
    private static final Map<UUID, PandaEntity.Gene> SERVER_HIDDEN_GENES = new ConcurrentHashMap<>();
    private static final Map<UUID, PandaEntity.Gene> CLIENT_MAIN_GENES = new ConcurrentHashMap<>();
    private static final Map<UUID, PandaEntity.Gene> CLIENT_HIDDEN_GENES = new ConcurrentHashMap<>();

    private PandaAppearanceState() {}

    public static void setServerAppearance(UUID uuid, PandaEntity.Gene mainGene, PandaEntity.Gene hiddenGene) {
        SERVER_MAIN_GENES.put(uuid, mainGene);
        SERVER_HIDDEN_GENES.put(uuid, hiddenGene);
    }

    public static PandaEntity.Gene getServerMainGene(UUID uuid) {
        return SERVER_MAIN_GENES.getOrDefault(uuid, PandaEntity.Gene.NORMAL);
    }

    public static PandaEntity.Gene getServerHiddenGene(UUID uuid) {
        return SERVER_HIDDEN_GENES.getOrDefault(uuid, PandaEntity.Gene.NORMAL);
    }

    public static void setClientAppearance(UUID uuid, PandaEntity.Gene mainGene, PandaEntity.Gene hiddenGene) {
        CLIENT_MAIN_GENES.put(uuid, mainGene);
        CLIENT_HIDDEN_GENES.put(uuid, hiddenGene);
    }

    public static PandaEntity.Gene getClientMainGene(UUID uuid) {
        return CLIENT_MAIN_GENES.getOrDefault(uuid, PandaEntity.Gene.NORMAL);
    }

    public static PandaEntity.Gene getClientHiddenGene(UUID uuid) {
        return CLIENT_HIDDEN_GENES.getOrDefault(uuid, PandaEntity.Gene.NORMAL);
    }

    public static void clear(UUID uuid) {
        SERVER_MAIN_GENES.remove(uuid);
        SERVER_HIDDEN_GENES.remove(uuid);
        CLIENT_MAIN_GENES.remove(uuid);
        CLIENT_HIDDEN_GENES.remove(uuid);
    }

    public static void clearAllClient() {
        CLIENT_MAIN_GENES.clear();
        CLIENT_HIDDEN_GENES.clear();
    }
}
