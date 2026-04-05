package net.sam.samrequiemmod.mixin.client;

import net.minecraft.entity.mob.EvokerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Exposes spell animation control via reflection.
 * Finds setCurrentSpell by searching all declared methods for one that takes
 * a single enum parameter — avoids hardcoding the obfuscated method name.
 */
@Mixin(EvokerEntity.class)
public abstract class EvokerEntitySpellAccessor implements net.sam.samrequiemmod.client.EvokerSpellSetter {

    @Unique private static java.lang.reflect.Method samrequiemmod$method = null;
    @Unique private static Object[] samrequiemmod$spells = null;
    @Unique private static boolean samrequiemmod$initialized = false;

    @Unique
    private static void samrequiemmod$init() {
        if (samrequiemmod$initialized) return;
        samrequiemmod$initialized = true;
        try {
            Class<?> cls = net.minecraft.entity.mob.SpellcastingIllagerEntity.class;
            // Find the inner Spell enum
            Class<?> spellClass = null;
            for (Class<?> inner : cls.getDeclaredClasses()) {
                if (inner.isEnum()) { spellClass = inner; break; }
            }
            if (spellClass == null) return;
            samrequiemmod$spells = spellClass.getEnumConstants();
            // Find setCurrentSpell by scanning all declared methods for one taking a single Spell param
            for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == spellClass) {
                    m.setAccessible(true);
                    samrequiemmod$method = m;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Unique
    public void samrequiemmod$setSpellByOrdinal(int ordinal) {
        samrequiemmod$init();
        if (samrequiemmod$method == null || samrequiemmod$spells == null) return;
        if (ordinal < 0 || ordinal >= samrequiemmod$spells.length) return;
        try {
            samrequiemmod$method.invoke(this, samrequiemmod$spells[ordinal]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}





