package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Measures existing mechanics; it never changes damage or control durations. */
public final class DepartmentManba {
    private DepartmentManba() { }
    public static final class Silence {
        private final java.lang.ref.WeakReference<ServerPlayer> owner;
        private String scope=java.util.UUID.randomUUID().toString();
        private long last=Long.MIN_VALUE;
        public Silence(ServerPlayer player){owner=new java.lang.ref.WeakReference<>(player);}
    }
    public static Silence merge(Silence previous,Silence incoming) {
        return previous!=null&&incoming!=null&&previous.owner.get()==incoming.owner.get()?previous:incoming;
    }
    public static void tickSilence(MobEffectInstance effect,LivingEntity target) {
        if(target.level().isClientSide||!target.isAlive()||effect.getDuration()==0)return;
        Silence silence=((DepartmentEffectSource)effect).dfs$silence();
        if(silence==null)return;
        ServerPlayer owner=silence.owner.get();long now=target.level().getGameTime();
        if(owner==null||!owner.isAlive()||owner.isRemoved()||owner.level()!=target.level())return;
        if(silence.last==now)return;
        if(silence.last!=now-1)silence.scope=java.util.UUID.randomUUID().toString();
        silence.last=now;
        DepartmentTasks.scoped(owner,"manba.elbow.continuous_silence",1,silence.scope,"",24000);
    }

    private record Reduction(ServerPlayer owner,double amount,boolean complete,java.util.function.BooleanSupplier resolved) { }
    private static final class Hit {
        final LivingEntity target;final float health;
        final java.util.List<Reduction> reductions=new java.util.ArrayList<>();
        Hit(LivingEntity entity){target=entity;health=entity.getHealth()+entity.getAbsorptionAmount();}
    }
    private static final ThreadLocal<java.util.ArrayDeque<Hit>> HITS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static void beginHit(LivingEntity target) { HITS.get().push(new Hit(target)); }
    public static void reduced(LivingEntity target,ServerPlayer owner,float before,float after,java.util.function.BooleanSupplier resolved) {
        var hits=HITS.get();
        if(!hits.isEmpty()&&hits.peek().target==target&&Float.isFinite(before)&&Float.isFinite(after)&&before>after&&after>=0)
            hits.peek().reductions.add(new Reduction(owner,before-after,after==0,resolved));
        if(hits.isEmpty())HITS.remove();
    }
    public static void endHit() {
        var hits=HITS.get();if(hits.isEmpty()){HITS.remove();return;}
        var hit=hits.pop();
        boolean damaged=hit.health>hit.target.getHealth()+hit.target.getAbsorptionAmount();
        for(var reduction:hit.reductions)if(reduction.resolved().getAsBoolean()&&(damaged||reduction.complete()))
            DepartmentTasks.signal(reduction.owner(),"manba.passive.damage_reduced",reduction.amount());
        if(hits.isEmpty())HITS.remove();
    }
}
