package com.rzy.dealt_force_skills.compat.maid;

import java.lang.reflect.Method;
import net.minecraft.world.entity.LivingEntity;

/** Optional public task API only; the maid continues to own her brain and gun operator. */
final class MaidNativeTasks {
    private record Access(Method task, Method home, Class<?> melee) { }
    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override protected Access computeValue(Class<?> type) {
            try {
                return new Access(type.getMethod("getTask"), type.getMethod("isHomeModeEnable"),
                        Class.forName("com.github.tartaricacid.touhoulittlemaid.entity.task.TaskAttack", false, type.getClassLoader()));
            } catch (ReflectiveOperationException error) { return new Access(null, null, null); }
        }
    };
    private MaidNativeTasks() { }
    static boolean melee(LivingEntity maid) {
        Access access = ACCESS.get(maid.getClass());
        try { return access.task != null && access.melee.isInstance(access.task.invoke(maid)); }
        catch (ReflectiveOperationException error) { return false; }
    }
    static boolean home(LivingEntity maid) {
        Access access = ACCESS.get(maid.getClass());
        try { return access.home != null && Boolean.TRUE.equals(access.home.invoke(maid)); }
        catch (ReflectiveOperationException error) { return false; }
    }
}
