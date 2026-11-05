package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Server outcomes for Corps. Cast identity is transient and captured before damage callbacks. */
public final class DepartmentCorps {
    private static final java.util.Map<MobEffectInstance,String> CASTS=new java.util.WeakHashMap<>();
    public record Hit(ServerPlayer owner,String cast) { }
    private DepartmentCorps() { }
    public static void batonActivated(ServerPlayer player) {
        MobEffectInstance effect=player.getEffect(ModEffects.CORPS_FORCEFUL_BATON.get());
        if(effect!=null)CASTS.put(effect,java.util.UUID.randomUUID().toString());
    }
    public static Hit damageStarted(DamageSource source) {
        if(!(source.getEntity() instanceof ServerPlayer owner)||source.getDirectEntity()!=owner
                ||!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||!CorpsStateManager.isCorps(owner))return null;
        String cast=CASTS.get(owner.getEffect(ModEffects.CORPS_FORCEFUL_BATON.get()));
        return cast==null?null:new Hit(owner,cast);
    }
    public static void damageApplied(LivingEntity target,Hit hit,float amount) {
        if(target.level().isClientSide||!Float.isFinite(amount)||amount<=0)return;
        if(target instanceof ServerPlayer player)CorpsStateManager.onDamageTaken(player,amount);
        if(hit!=null&&target!=hit.owner())DepartmentTasks.scoped(hit.owner(),"corps.melee.cast_hits",1,hit.cast(),"",24000);
    }
    /** Invoked inside addEffect after team rules, before vanilla can merge or hide the effect. */
    public static boolean blockEffect(LivingEntity target,MobEffectInstance effect,Entity source) {
        if(!(target instanceof ServerPlayer player)
                ||com.rzy.dealt_force_skills.character.rover.RoverStateManager.isProtectedEffect(effect.getEffect())
                ||!CorpsStateManager.shouldDenyIncomingEffect(player,effect.getEffect()))return false;
        if(source!=player&&CorpsStateManager.isCorps(player))DepartmentTasks.signal(player,"corps.effect_immune");
        return true;
    }
}
