package net.sam.samrequiemmod.possession;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public class PossessionData {
    private boolean possessing;
    @Nullable
    private EntityType<?> possessedType;

    public boolean isPossessing() {
        return possessing && possessedType != null;
    }

    @Nullable
    public EntityType<?> getPossessedType() {
        return possessedType;
    }

    public void setPossessedType(@Nullable EntityType<?> possessedType) {
        this.possessedType = possessedType;
        this.possessing = possessedType != null;
    }

    public void clear() {
        this.possessing = false;
        this.possessedType = null;
    }
}