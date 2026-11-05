package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Accepted outcomes only; pending copies wait for the original hit to finish. */
public final class DepartmentGhroth {
    private DepartmentGhroth() { }
    private static final class StarsHit {
        final LivingEntity target;final DamageSource source;float lost;
        StarsHit(LivingEntity target,DamageSource source){this.target=target;this.source=source;}
    }
    private static final ThreadLocal<java.util.ArrayDeque<StarsHit>> STARS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static boolean stars(ServerPlayer owner,LivingEntity target,DamageSource source,float amount) {
        var hits=STARS.get();var hit=new StarsHit(target,source);hits.push(hit);
        try {return com.rzy.dealt_force_skills.skill.SkillDamageHelper.hurtUnscaled(target,source,amount);}
        finally {
            hits.pop();if(hits.isEmpty())STARS.remove();
            if(Float.isFinite(hit.lost)&&hit.lost>0)DepartmentTasks.signal(owner,"ghroth.active1.damage",hit.lost);
        }
    }
    public static void damaged(LivingEntity target,DamageSource source,float amount) {
        var hits=STARS.get();
        if(!hits.isEmpty()) {var hit=hits.peek();if(hit.target==target&&hit.source==source)hit.lost+=amount;}
        if(hits.isEmpty())STARS.remove();
    }
    private static final java.util.Set<LivingEntity> DEATHS=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final class Hit {
        final LivingEntity target;final float health;
        ServerPlayer dodgeOwner,noonOwner;
        java.util.function.BooleanSupplier dodgeResolved,noonResolved;
        java.util.function.IntConsumer copies;
        boolean full;
        Hit(LivingEntity target){this.target=target;health=target.getHealth()+target.getAbsorptionAmount();}
    }
    private static final ThreadLocal<java.util.ArrayDeque<Hit>> HITS=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static void beginHit(LivingEntity target){HITS.get().push(new Hit(target));}
    public static void dodged(ServerPlayer player,java.util.function.BooleanSupplier resolved) {
        var hits=HITS.get();
        if(!hits.isEmpty()&&hits.peek().target==player){hits.peek().dodgeOwner=player;hits.peek().dodgeResolved=resolved;}
        if(hits.isEmpty())HITS.remove();
    }
    public static void noon(LivingEntity target,ServerPlayer owner,java.util.function.BooleanSupplier resolved,
                            java.util.function.IntConsumer applyCopies) {
        if(!GhrothStateManager.isNoonActive(owner)||GhrothStateManager.noonDamageCopies(owner)<=0)return;
        var hits=HITS.get();
        if(!hits.isEmpty()&&hits.peek().target==target) {
            var hit=hits.peek();hit.noonOwner=owner;hit.noonResolved=resolved;hit.copies=applyCopies;
            hit.full=GhrothStateManager.isNoonFullyCharged(owner);
        }
        if(hits.isEmpty())HITS.remove();
    }
    public static void endHit() {
        var hits=HITS.get();if(hits.isEmpty()){HITS.remove();return;}
        var hit=hits.pop();if(hits.isEmpty())HITS.remove();
        if(hit.dodgeOwner!=null&&hit.dodgeResolved.getAsBoolean())DepartmentTasks.signal(hit.dodgeOwner,"ghroth.dodged");
        float lost=hit.health-hit.target.getHealth()-hit.target.getAbsorptionAmount();
        if(hit.noonOwner!=null&&Float.isFinite(lost)&&lost>0&&hit.noonResolved.getAsBoolean()) {
            int copies=GhrothStateManager.consumeNoonDamageCopiesForHit(hit.noonOwner);
            if(copies>0){if(hit.full)DepartmentTasks.signal(hit.noonOwner,"ghroth.core.fully_charged_hit");hit.copies.accept(copies);}
        }
    }
    public static void died(LivingEntity target,DamageSource source) {
        if(target.level().isClientSide||target.isAlive()||!DEATHS.add(target))return;
        if(source.getEntity() instanceof ServerPlayer owner&&owner!=target&&GhrothStateManager.isGhroth(owner))
            GhrothStateManager.recordJusticeKill(owner,target);
    }
}
