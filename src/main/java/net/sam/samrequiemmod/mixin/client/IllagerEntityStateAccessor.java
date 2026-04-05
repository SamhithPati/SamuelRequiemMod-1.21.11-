package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.mob.IllagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Exposes IllagerEntity state control via reflection.
 * Finds setState by searching all declared methods for one that takes
 * a single IllagerEntity.State parameter.
 */
@Mixin(IllagerEntity.class)
public abstract class IllagerEntityStateAccessor implements net.sam.samrequiemmod.client.IllagerStateSetter {

    @Unique private static java.lang.reflect.Method samrequiemmod$setStateMethod = null;
    @Unique private static boolean samrequiemmod$initialized = false;

    @Unique
    private static void samrequiemmod$init() {
        if (samrequiemmod$initialized) return;
        samrequiemmod$initialized = true;
        try {
            Class<?> cls = IllagerEntity.class;
            // Find the State inner enum
            Class<?> stateClass = IllagerEntity.State.class;
            // Find vanilla's hidden state setter, not our injected bridge method.
            for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (m.getName().startsWith("samrequiemmod$")) continue;
                if (params.length == 1 && params[0] == stateClass && m.getReturnType() == Void.TYPE) {
                    m.setAccessible(true);
                    samrequiemmod$setStateMethod = m;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Unique
    public void samrequiemmod$setState(IllagerEntity.State state) {
        samrequiemmod$init();
        if (samrequiemmod$setStateMethod == null) return;
        try {
            samrequiemmod$setStateMethod.invoke(this, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}






