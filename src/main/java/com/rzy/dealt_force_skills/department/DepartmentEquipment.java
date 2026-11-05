package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

/** Equipment outcomes wait for the final damage event or successful drop spawning. */
public final class DepartmentEquipment {
    private DepartmentEquipment() { }
    private static final class Hit {
        final LivingEntity target;
        java.util.function.BooleanSupplier accepted;
        final java.util.List<Runnable> outcomes=new java.util.ArrayList<>();
        Hit(LivingEntity target){this.target=target;}
    }
    private static final ThreadLocal<java.util.ArrayDeque<Hit>> HITS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static void beginHit(LivingEntity target){HITS.get().push(new Hit(target));}
    public static void resolved(LivingEntity target,java.util.function.BooleanSupplier accepted) {
        var hits=HITS.get();if(!hits.isEmpty()&&hits.peek().target==target)hits.peek().accepted=accepted;
        if(hits.isEmpty())HITS.remove();
    }
    public static void record(ServerPlayer owner,String key,double amount) {
        if(!Double.isFinite(amount)||amount<=0)return;
        Runnable outcome=key.equals("armor.gti.destroyed_absorbing")
                ?DepartmentTasks.tracker().captureEquipmentDestruction(owner)
                :()->DepartmentTasks.signal(owner,key,amount);
        var hits=HITS.get();
        if(hits.isEmpty()){HITS.remove();outcome.run();}else hits.peek().outcomes.add(outcome);
    }
    public static void endHit() {
        var hits=HITS.get();if(hits.isEmpty()){HITS.remove();return;}
        var hit=hits.pop();if(hits.isEmpty())HITS.remove();
        if(hit.accepted!=null&&hit.accepted.getAsBoolean())hit.outcomes.forEach(Runnable::run);
    }
    private record Bonus(java.lang.ref.WeakReference<ServerPlayer> owner,java.lang.ref.WeakReference<ItemEntity> drop,int count) { }
    private static final java.util.Map<LivingEntity,java.util.List<Bonus>> DROPS=new java.util.WeakHashMap<>();
    public static void bonus(LivingEntity target,ServerPlayer owner,ItemEntity drop) {
        DROPS.computeIfAbsent(target,unused->new java.util.ArrayList<>()).add(new Bonus(new java.lang.ref.WeakReference<>(owner),new java.lang.ref.WeakReference<>(drop),drop.getItem().getCount()));
    }
    /** die TAIL runs after the loader dispatches and spawns the final drop collection. */
    public static void died(LivingEntity target) {
        var bonuses=DROPS.remove(target);if(bonuses==null||target.isAlive())return;
        for(var bonus:bonuses) {
            var owner=bonus.owner.get();var drop=bonus.drop.get();
            if(owner!=null&&drop!=null&&target.level() instanceof net.minecraft.server.level.ServerLevel level
                    &&level.getEntity(drop.getUUID())==drop)
                DepartmentTasks.signal(owner,"armor.gti.bonus_loot",Math.min(bonus.count,drop.getItem().getCount()));
        }
    }
}
