package com.rzy.dealt_force_skills.department;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.List;

/** A task's state belongs exclusively to one player's saved compound. No shared mutable progress. */
public final class TaskProgress {
    public enum Status { AVAILABLE, ACTIVE, COMPLETED, COOLDOWN, ABANDONED }
    public record Goal(String key, double target, long windowTicks, boolean unique, boolean maximum) {
        public Goal {
            if(key==null||key.isBlank()||key.length()>160||!Double.isFinite(target)||target<=0||windowTicks<0)
                throw new IllegalArgumentException("Invalid task goal");
        }
    }
    private TaskProgress() { }

    public static Status status(CompoundTag state,long now) {
        if(state.getBoolean("Completed"))return Status.COMPLETED;
        if(state.getBoolean("Abandoned"))return Status.ABANDONED;
        if(state.getBoolean("Active"))return Status.ACTIVE;
        if(state.getLong("RetryAt")>now)return Status.COOLDOWN;
        return Status.AVAILABLE;
    }

    public static boolean accept(CompoundTag state,long now) {
        if(status(state,now)!=Status.AVAILABLE)return false;
        state.putBoolean("Active",true);state.putLong("AcceptedAt",now);
        state.put("Goals",new CompoundTag());return true;
    }

    public static void reset(CompoundTag state,long now,long cooldownTicks,boolean remainActive) {
        if(status(state,now)!=Status.ACTIVE)return;
        state.putBoolean("Active",remainActive);state.putLong("AcceptedAt",now);
        state.putLong("RetryAt",now+Math.max(0,cooldownTicks));state.put("Goals",new CompoundTag());
    }

    private static CompoundTag goalState(CompoundTag state,int index) {
        if(!state.contains("Goals",Tag.TAG_COMPOUND))state.put("Goals",new CompoundTag());
        CompoundTag goals=state.getCompound("Goals");String key=Integer.toString(index);
        if(!goals.contains(key,Tag.TAG_COMPOUND))goals.put(key,new CompoundTag());
        return goals.getCompound(key);
    }

    public static double progress(CompoundTag state,int index,Goal goal,long now) {
        CompoundTag data=goalState(state,index);
        if(goal.windowTicks()>0) {
            ListTag samples=data.getList("Samples",Tag.TAG_COMPOUND);
            // A window cannot survive clock rollback or include events before task acceptance.
            for(int i=samples.size()-1;i>=0;i--) {
                long at=samples.getCompound(i).getLong("At");
                if(at>now||at<state.getLong("AcceptedAt")||now-at>goal.windowTicks())samples.remove(i);
            }
            double result=0;
            java.util.Set<String> unique=new java.util.HashSet<>();
            for(int i=0;i<samples.size();i++) {
                CompoundTag sample=samples.getCompound(i);
                if(goal.unique()&&!unique.add(sample.getString("Token")))continue;
                double value=sample.getDouble("Value");
                if(Double.isFinite(value)&&value>0)result=goal.maximum()?Math.max(result,value):result+value;
            }
            return Math.min(goal.target(),result);
        }
        double value=data.getDouble("Value");
        return Double.isFinite(value)?Math.max(0,Math.min(goal.target(),value)):0;
    }

    /** Returns true only when a real event advances an accepted task. */
    public static boolean record(CompoundTag state,int index,Goal goal,String key,double amount,String token,long now) {
        if(status(state,now)!=Status.ACTIVE||!goal.key().equals(key)||!Double.isFinite(amount)||amount<=0
                ||now<state.getLong("AcceptedAt"))return false;
        double previous=progress(state,index,goal,now);
        if(previous>=goal.target()&&goal.windowTicks()==0)return false;
        CompoundTag data=goalState(state,index);
        if(goal.unique()&&(token==null||token.isBlank()||token.length()>160))return false;
        if(goal.windowTicks()>0) {
            ListTag samples=data.getList("Samples",Tag.TAG_COMPOUND);
            if(goal.unique()) {
                for(int i=samples.size()-1;i>=0;i--)if(token.equals(samples.getCompound(i).getString("Token")))samples.remove(i);
                while(samples.size()>=Math.ceil(goal.target()))samples.remove(0);
                amount=1;
            }
            // Combine same-tick non-unique samples; high-fire-rate events do not grow NBT per bullet.
            CompoundTag last=samples.isEmpty()?null:samples.getCompound(samples.size()-1);
            if(!goal.unique()&&last!=null&&last.getLong("At")==now) {
                double old=last.getDouble("Value");last.putDouble("Value",Math.min(goal.target(),goal.maximum()?Math.max(old,amount):old+amount));
            } else {
                CompoundTag sample=new CompoundTag();sample.putLong("At",now);sample.putDouble("Value",Math.min(goal.target(),amount));
                if(goal.unique())sample.putString("Token",token);samples.add(sample);
            }
            data.put("Samples",samples);
        } else {
            if(goal.unique()) {
                CompoundTag seen=data.getCompound("Seen");
                if(seen.getBoolean(token))return false;
                seen.putBoolean(token,true);data.put("Seen",seen);
            }
            data.putDouble("Value",Math.min(goal.target(),goal.maximum()?Math.max(previous,amount):previous+amount));
        }
        return progress(state,index,goal,now)>previous;
    }

    public static boolean satisfied(CompoundTag state,List<Goal> goals,long now) {
        if(status(state,now)!=Status.ACTIVE||goals.isEmpty())return false;
        for(int i=0;i<goals.size();i++)if(progress(state,i,goals.get(i),now)<goals.get(i).target())return false;
        return true;
    }

    /** Maximum accumulated within one server-owned cast/projectile, starting only after acceptance. */
    public static boolean recordScoped(CompoundTag state,int index,Goal goal,String key,double amount,
                                       String scope,String distinctTarget,long now,long lifetimeTicks) {
        if(status(state,now)!=Status.ACTIVE||now<state.getLong("AcceptedAt")||!goal.maximum()
                ||!goal.key().equals(key)||!Double.isFinite(amount)||amount<=0||scope==null||scope.isBlank()
                ||scope.length()>160||distinctTarget==null||distinctTarget.length()>160)return false;
        CompoundTag data=goalState(state,index),scopes=data.getCompound("Scopes");
        long lifetime=lifetimeTicks==Long.MAX_VALUE?Long.MAX_VALUE:Math.max(1,Math.min(24000,lifetimeTicks));
        for(String id:java.util.List.copyOf(scopes.getAllKeys())) {
            long at=scopes.getCompound(id).getLong("At");
            if(at>now||at<state.getLong("AcceptedAt")||now-at>lifetime)scopes.remove(id);
        }
        if(!scopes.contains(scope,Tag.TAG_COMPOUND)&&scopes.size()>=64) {
            String oldest=scopes.getAllKeys().stream().min(java.util.Comparator.comparingLong(id->scopes.getCompound(id).getLong("At"))).orElseThrow();
            scopes.remove(oldest);
        }
        CompoundTag sample=scopes.getCompound(scope);
        if(!distinctTarget.isEmpty()) {
            CompoundTag seen=sample.getCompound("Seen");
            if(seen.getBoolean(distinctTarget)||seen.size()>=Math.ceil(goal.target()))return false;
            seen.putBoolean(distinctTarget,true);sample.put("Seen",seen);
        }
        double total;
        if(goal.windowTicks()>0) {
            ListTag samples=sample.getList("Window",Tag.TAG_COMPOUND);
            for(int i=samples.size()-1;i>=0;i--)if(samples.getCompound(i).getLong("At")>now||now-samples.getCompound(i).getLong("At")>goal.windowTicks())samples.remove(i);
            CompoundTag last=samples.isEmpty()?null:samples.getCompound(samples.size()-1);
            if(last!=null&&last.getLong("At")==now)last.putDouble("Value",Math.min(goal.target(),last.getDouble("Value")+amount));
            else {var next=new CompoundTag();next.putLong("At",now);next.putDouble("Value",Math.min(goal.target(),amount));samples.add(next);}
            while(samples.size()>4096)samples.remove(0);
            sample.put("Window",samples);total=0;
            for(int i=0;i<samples.size();i++)total+=samples.getCompound(i).getDouble("Value");
            total=Math.min(goal.target(),total);
        }else total=Math.min(goal.target(),sample.getDouble("Value")+amount);
        sample.putDouble("Value",total);sample.putLong("At",now);scopes.put(scope,sample);data.put("Scopes",scopes);
        return record(state,index,goal,key,total,"",now);
    }

    /** Manager calls this before sending the uniquely keyed reward mail. */
    public static boolean complete(CompoundTag state,List<Goal> goals,long now) {
        if(!satisfied(state,goals,now))return false;
        state.putBoolean("Active",false);state.putBoolean("Completed",true);state.putLong("CompletedAt",now);return true;
    }
}
