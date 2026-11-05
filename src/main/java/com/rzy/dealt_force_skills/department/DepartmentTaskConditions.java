package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Short effect leases: task cancellation must not cleanse effects supplied by another source. */
public final class DepartmentTaskConditions {
    private static final Map<ServerPlayer,List<Lease>> LEASES=new WeakHashMap<>();
    private record Lease(MobEffectInstance applied,MobEffectInstance previous,long since,int amplifier) { }
    private DepartmentTaskConditions() { }

    public static void refresh(ServerPlayer player,DepartmentTracker tracker) {
        if(!player.isAlive() || !tracker.hasPolicy(player,"contact_effects")) { clear(player);return; }
        var leases=LEASES.computeIfAbsent(player,ignored->new ArrayList<>());
        var effects=List.of(new MobEffectInstance(MobEffects.DARKNESS,80,0),
                new MobEffectInstance(MobEffects.BLINDNESS,80,0),
                new MobEffectInstance(MobEffects.CONFUSION,80,2),
                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,80,1));
        long tick=player.server.overworld().getGameTime();
        for(var required:effects) {
            var existing=player.getEffect(required.getEffect());
            Lease lease=leases.stream().filter(l->l.applied().getEffect().equals(required.getEffect())).findFirst().orElse(null);
            if(lease!=null && (existing!=lease.applied() || existing.getAmplifier()!=lease.amplifier() || existing.getDuration()>80)) {
                leases.remove(lease);lease=null;
            }
            if(existing!=null && existing.getAmplifier()>=required.getAmplifier()
                    && (existing.isInfiniteDuration() || existing.getDuration()>40))continue;
            var previous=lease==null && existing!=null?new MobEffectInstance(existing):lease==null?null:lease.previous();
            long since=lease==null?tick:lease.since();
            player.addEffect(required);
            if(lease!=null)leases.remove(lease);
            var applied=player.getEffect(required.getEffect());
            if(applied!=null && applied.getAmplifier()==required.getAmplifier())
                leases.add(new Lease(applied,previous,since,required.getAmplifier()));
        }
    }

    public static void clear(ServerPlayer player) {
        var leases=LEASES.remove(player);
        if(leases==null)return;
        long tick=player.server.overworld().getGameTime();
        for(var lease:leases) {
            var current=player.getEffect(lease.applied().getEffect());
            if(current!=lease.applied() || current.getAmplifier()!=lease.amplifier() || current.getDuration()>80)continue;
            player.removeEffect(current.getEffect());
            var previous=lease.previous();
            if(previous!=null) {
                int remaining=previous.isInfiniteDuration()?-1:(int)Math.max(0,previous.getDuration()-Math.max(0,tick-lease.since()));
                if(remaining!=0)player.addEffect(new MobEffectInstance(previous.getEffect(),remaining,previous.getAmplifier(),
                        previous.isAmbient(),previous.isVisible(),previous.showIcon()));
            }
        }
    }
}
