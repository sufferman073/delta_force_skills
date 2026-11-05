package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Pending auto-scaling belongs to the incoming effect object, including nested additions. */
public final class DepartmentEffects {
    private record Pending(java.lang.ref.WeakReference<MobEffectInstance> effect,java.lang.ref.WeakReference<ServerPlayer> owner) { }
    private static final java.util.List<Pending> SCALED=new java.util.ArrayList<>();
    private DepartmentEffects() { }
    public static void scaled(MobEffectInstance incoming,ServerPlayer owner) {
        if(owner==null)return;
        SCALED.removeIf(p->p.effect().get()==null||p.owner().get()==null||p.effect().get()==incoming);
        if(SCALED.size()>=512)SCALED.remove(0);
        SCALED.add(new Pending(new java.lang.ref.WeakReference<>(incoming),new java.lang.ref.WeakReference<>(owner)));
    }
    public static void applied(MobEffectInstance incoming,boolean accepted) {
        ServerPlayer owner=null;
        for(var iterator=SCALED.iterator();iterator.hasNext();) {
            var pending=iterator.next();var effect=pending.effect().get();
            if(effect==incoming){owner=pending.owner().get();iterator.remove();}
            else if(effect==null||pending.owner().get()==null)iterator.remove();
        }
        if(accepted&&owner!=null)DepartmentTasks.signal(owner,"toxik.passive");
    }
}
