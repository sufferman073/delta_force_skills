package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Credit time saved by the drink only after the corresponding work succeeds. */
public final class DepartmentUseSpeed {
    private record Use(ItemStack item,long started,int duration,String signal,double saved) { }
    private static final class Repair {
        final ItemStack item;
        double guanshan,rose;
        Repair(ItemStack item){this.item=item;}
    }
    private static final java.util.Map<ServerPlayer,Use> USES=new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerPlayer,Repair> REPAIRS=new java.util.WeakHashMap<>();
    private DepartmentUseSpeed() { }
    private static String signal(ServerPlayer player) {
        if(DepartmentConsumables.from(player,ModEffects.LARGE_INTERACTION_SPEED.get(),"guanshan_nanyue"))return "drink.guanshan.time_saved";
        if(DepartmentConsumables.from(player,ModEffects.LARGE_INTERACTION_SPEED.get(),"wild_rose"))return "drink.rose.time_saved";
        return "";
    }
    private static double factor(ServerPlayer player) {
        var effect=player.getEffect(ModEffects.LARGE_INTERACTION_SPEED.get());
        return effect==null?1:1+2.0*Math.max(1,Math.min(100,effect.getAmplifier()+1.0));
    }
    private static int duration(int ticks,double rate) {
        if(!Double.isFinite(rate))rate=1;
        return Math.max(1,(int)Math.ceil(ticks/Math.max(0.05,Math.min(20,rate))));
    }
    public static void start(LivingEntity entity,ItemStack item,int ticks,double rate) {
        if(!(entity instanceof ServerPlayer player))return;
        USES.remove(player);
        if(ticks<=0||item.getItem() instanceof com.rzy.dealt_force_skills.item.RepairKitItem)return;
        String key=signal(player);if(key.isEmpty())return;
        int actual=duration(ticks,rate);double saved=duration(ticks,rate/factor(player))-actual;
        if(saved>0)USES.put(player,new Use(item,player.level().getGameTime(),actual,key,saved));
    }
    public static void finished(ServerPlayer player,ItemStack item) {
        Use use=USES.remove(player);
        if(use!=null&&use.item()==item&&player.level().getGameTime()-use.started()>=use.duration()-1)
            DepartmentTasks.signal(player,use.signal(),use.saved());
    }
    public static void repairStarted(ServerPlayer player,ItemStack item) {
        REPAIRS.put(player,new Repair(item));
    }
    public static void repairProgress(LivingEntity entity,ItemStack item,long elapsed) {
        if(!(entity instanceof ServerPlayer player)||elapsed<=0||player.getUseItem()!=item)return;
        Repair repair=REPAIRS.get(player);if(repair==null||repair.item!=item)return;
        String key=signal(player);double saved=elapsed*(factor(player)-1);
        if(key.equals("drink.guanshan.time_saved"))repair.guanshan+=saved;
        if(key.equals("drink.rose.time_saved"))repair.rose+=saved;
    }
    public static void repaired(ServerPlayer player,ItemStack item,int restored) {
        Repair repair=REPAIRS.get(player);if(repair==null||repair.item!=item||restored<=0)return;
        DepartmentTasks.signal(player,"drink.guanshan.time_saved",repair.guanshan);
        DepartmentTasks.signal(player,"drink.rose.time_saved",repair.rose);
        repair.guanshan=0;repair.rose=0;
    }
    public static void stopped(LivingEntity entity) {
        if(entity instanceof ServerPlayer player){USES.remove(player);REPAIRS.remove(player);}
    }
}
