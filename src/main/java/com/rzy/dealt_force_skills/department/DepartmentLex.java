package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Transient, nested-safe attribution for actual Lex damage and accepted deaths. */
public final class DepartmentLex {
    private DepartmentLex() { }
    private static final java.util.Set<LivingEntity> DEATHS=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final class Outcome {
        final ServerPlayer owner;final LivingEntity target;final DamageSource source;
        final String cast;final boolean reflect;float lost;
        Outcome(ServerPlayer owner,LivingEntity target,DamageSource source,String cast,boolean reflect) {
            this.owner=owner;this.target=target;this.source=source;this.cast=cast;this.reflect=reflect;
        }
    }
    private static final ThreadLocal<java.util.ArrayDeque<Outcome>> OUTCOMES=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static boolean damage(ServerPlayer owner,LivingEntity target,DamageSource source,float amount,
                                 boolean reflect,java.util.function.BooleanSupplier qualifyBurning) {
        var frames=OUTCOMES.get();var frame=new Outcome(owner,target,source,"",reflect);frames.push(frame);
        try {return com.rzy.dealt_force_skills.skill.SkillDamageHelper.hurtUnscaled(target,source,amount);}
        finally {
            frames.pop();if(frames.isEmpty())OUTCOMES.remove();
            if(Float.isFinite(frame.lost)&&frame.lost>0&&qualifyBurning.getAsBoolean())
                DepartmentTasks.signal(owner,"lex.burning.all_buffs_hit");
        }
    }
    public static void execute(ServerPlayer owner,LivingEntity target,String cast,Runnable action) {
        var frames=OUTCOMES.get();frames.push(new Outcome(owner,target,null,cast,false));
        try {action.run();}finally{frames.pop();if(frames.isEmpty())OUTCOMES.remove();}
    }
    public static void damaged(LivingEntity target,DamageSource source,float amount) {
        var frames=OUTCOMES.get();
        if(!frames.isEmpty()) {var frame=frames.peek();if(frame.target==target&&frame.source==source)frame.lost+=amount;}
        if(frames.isEmpty())OUTCOMES.remove();
    }
    public static void died(LivingEntity target,DamageSource source) {
        if(target.level().isClientSide||target.isAlive()||!DEATHS.add(target))return;
        ServerPlayer owner=source.getEntity() instanceof ServerPlayer player?player:null;
        var frames=OUTCOMES.get();
        if(!frames.isEmpty()) {
            var frame=frames.peek();
            if(frame.target==target) {
                if(frame.reflect&&frame.source==source)DepartmentTasks.signal(frame.owner,"lex.reflect.kill");
                if(!frame.cast.isEmpty()&&(source.getEntity()==frame.owner||source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL))) {
                    DepartmentTasks.scoped(frame.owner,"lex.hamu_all.cast_kills",1,frame.cast,target.getUUID().toString(),24000);
                    owner=frame.owner;
                }
            }
        }
        if(frames.isEmpty())OUTCOMES.remove();
        if(owner!=null&&owner!=target&&owner.isAlive()&&LexNinjiaStateManager.isLexNinjia(owner)) {
            if(LexNinjiaStateManager.isBerserkActive(owner))DepartmentTasks.signal(owner,"lex.night_hamu.kill");
            LexNinjiaStateManager.onKill(owner);
        }
    }
}
