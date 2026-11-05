package com.rzy.dealt_force_skills.department;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Records only reveal packets the server actually sends to this viewer. */
public final class DepartmentRecon {
    private static final String KEY="dealt_force_skills.department_reveals";
    private DepartmentRecon() { }
    public interface Packet { void recordDepartmentReveal(ServerPlayer viewer); }
    public static void mark(ServerPlayer viewer,int entityId,int ticks) {
        if(viewer==null||ticks<=0||!(viewer.serverLevel().getEntity(entityId) instanceof LivingEntity target)||target==viewer)return;
        var data=viewer.getPersistentData();
        if(!data.contains(KEY))data.put(KEY,new CompoundTag());
        var known=data.getCompound(KEY);long now=viewer.server.overworld().getGameTime();
        for(String id:java.util.List.copyOf(known.getAllKeys()))if(known.getLong(id)<=now)known.remove(id);
        String id=target.getUUID().toString();
        if(!known.contains(id)&&known.size()>=256) {
            String oldest=known.getAllKeys().stream().min(java.util.Comparator.comparingLong(known::getLong)).orElse(null);
            if(oldest!=null)known.remove(oldest);
        }
        known.putLong(id,Math.max(known.getLong(id),now+(long)ticks));
    }
    public static void clear(ServerPlayer viewer) { viewer.getPersistentData().remove(KEY); }
    public static boolean visible(ServerPlayer viewer,LivingEntity target) {
        return viewer.getPersistentData().getCompound(KEY).getLong(target.getUUID().toString())>viewer.server.overworld().getGameTime();
    }
    public static void morseAffected(LivingEntity owner,LivingEntity target,boolean bounced,boolean flash) {
        if(!(owner instanceof ServerPlayer player)||target==owner)return;
        if(bounced)DepartmentTasks.signal(player,flash?"morse.flash.bounce_hit":"morse.stun.bounce_hit");
        if(visible(player,target))DepartmentTasks.signal(player,"morse.grenade.revealed_hit");
    }
}
