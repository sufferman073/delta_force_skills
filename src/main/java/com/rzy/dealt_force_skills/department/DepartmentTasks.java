package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Server-only entry points. Client packets may accept/submit, never report progress. */
public final class DepartmentTasks {
    private DepartmentTasks() { }
    private static final class Catalog { static final DepartmentTracker TRACKER=new DepartmentTracker(DepartmentCatalog.load()); }
    public static DepartmentTracker tracker() { return Catalog.TRACKER; }
    public static void signal(Entity owner,String key) { signal(owner,key,1); }
    public static void signal(Entity owner,String key,double amount) {
        if(owner instanceof ServerPlayer player)tracker().record(player,"signal",key,amount,"");
    }
    public static void signal(Entity owner,String key,double amount,String token) {
        if(owner instanceof ServerPlayer player)tracker().record(player,"signal",key,amount,token);
    }
    public static void skillUsed(ServerPlayer player,String characterId,com.rzy.dealt_force_skills.character.SkillSlot slot) {
        if(slot==com.rzy.dealt_force_skills.character.SkillSlot.PASSIVE)return;
        int colon=characterId.indexOf(':'),slash=characterId.indexOf('/');
        signal(player,"skill.used");
        if(slash>colon) {
            signal(player,"skill.branch."+characterId.substring(colon+1,slash));
            signal(player,"skill.role."+characterId.substring(slash+1));
        }
    }

    public static void scoped(Entity owner,String key,double amount,String scope,String distinctTarget,long lifetimeTicks) {
        if(owner instanceof ServerPlayer player)tracker().recordScoped(player,key,amount,scope,distinctTarget,lifetimeTicks);
    }

    /** Only call once an activation really occurs. A handled/cooldown response is not success. */
    public static void activated(ServerPlayer player,String outcome) {
        var context=ACTIVATION.get();
        if(context!=null&&context.player==player&&!context.reported) {
            context.reported=true;signal(player,context.key);
        }
        com.rzy.dealt_force_skills.character.CharacterSelectionManager.getSelectedCharacterId(player)
                .ifPresent(id->skillUsed(player,id,com.rzy.dealt_force_skills.character.SkillSlot.ACTIVE_1));
        if(!outcome.isEmpty())signal(player,outcome);
    }
    private static final ThreadLocal<Activation> ACTIVATION=new ThreadLocal<>();
    private static final class Activation {
        final ServerPlayer player;final String key;boolean reported;
        Activation(ServerPlayer player,String key){this.player=player;this.key=key;}
    }
    /** Preserve the original handled result; only a nested real activation can grant progress. */
    public static boolean observeActivation(ServerPlayer player,String key,java.util.function.BooleanSupplier action) {
        if(key.isEmpty())return action.getAsBoolean();
        Activation previous=ACTIVATION.get();ACTIVATION.set(new Activation(player,key));
        try{return action.getAsBoolean();}
        finally{if(previous==null)ACTIVATION.remove();else ACTIVATION.set(previous);}
    }
    public static boolean allowRequest(ServerPlayer player,boolean mutation) {
        // Browsing is read-only; allow a small burst so consecutive selections are never silently dropped.
        if(!mutation)return com.rzy.dealt_force_skills.mail.MailboxManager.allowRequest(player);
        var data=player.getPersistentData();long now=player.server.overworld().getGameTime();
        String key=DepartmentTracker.KEY+(mutation?".mutationTick":".viewTick");
        if(data.contains(key)) {
            long last=data.getLong(key);
            if(now>=last&&now-last<(mutation?20:5))return false;
        }
        data.putLong(key,now);return true;
    }
}
