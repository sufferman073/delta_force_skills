package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Shield credit waits for the real hurt call and the final event cancellation state. */
public final class DepartmentGambler {
    private static final class Hit {
        final LivingEntity target;
        boolean shield;
        java.util.function.BooleanSupplier resolved;
        Hit(LivingEntity target) { this.target=target; }
    }
    private static final ThreadLocal<java.util.ArrayDeque<Hit>> HITS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private DepartmentGambler() { }
    public static void beginHit(LivingEntity target) { HITS.get().push(new Hit(target)); }
    public static void shieldBlocked(ServerPlayer player) {
        var hits=HITS.get();
        if(!hits.isEmpty()&&hits.peek().target==player)hits.peek().shield=true;
        if(hits.isEmpty())HITS.remove();
    }
    public static void resolved(ServerPlayer player,java.util.function.BooleanSupplier resolved) {
        var hits=HITS.get();
        if(!hits.isEmpty()&&hits.peek().target==player)hits.peek().resolved=resolved;
        if(hits.isEmpty())HITS.remove();
    }
    public static void endHit() {
        var hits=HITS.get();
        if(hits.isEmpty()){HITS.remove();return;}
        var hit=hits.pop();
        if(hit.shield&&hit.target instanceof ServerPlayer player&&hit.resolved!=null&&hit.resolved.getAsBoolean())
            DepartmentTasks.signal(player,"gambler.shield.blocked");
        if(hits.isEmpty())HITS.remove();
    }
}
