package com.rzy.dealt_force_skills.ai.operative;

import com.mojang.logging.LogUtils;
import com.rzy.dealt_force_skills.compat.maid.MaidSkillHost;
import com.rzy.dealt_force_skills.character.corps.CorpsStateManager;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Independent maze cells and persistent return tickets. Player duels retain their original arena. */
public final class NativeCorpsArena {
    public static final String KEY="dealt_force_skills.operative_corps_duel";
    private static final String DATA="dealt_force_skills_native_corps_arenas";
    private NativeCorpsArena() { }
    private static long now(MinecraftServer server){return server.overworld().getGameTime();}
    private static Allocations data(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(Allocations::load,Allocations::new,DATA);}
    public static LivingEntity find(MinecraftServer server,UUID id){if(id!=null)for(var level:server.getAllLevels()){var e=level.getEntity(id);if(e instanceof LivingEntity living)return living;}return null;}
    public static boolean hasTicket(LivingEntity body){return body.getPersistentData().contains(KEY,10);}
    public static boolean active(LivingEntity body){return hasTicket(body) && body.getPersistentData().getCompound(KEY).getLong("ExitAt")==0;}
    public static boolean owner(LivingEntity body){return hasTicket(body) && body.getPersistentData().getCompound(KEY).getBoolean("Owner");}
    public static Vec3 center(LivingEntity body){var t=body.getPersistentData().getCompound(KEY);return new Vec3(t.getInt("CenterX")+.5,80,t.getInt("CenterZ")+.5);}
    public static LivingEntity opponent(LivingEntity body){var t=body.getPersistentData().getCompound(KEY);return t.hasUUID("Partner")?find(body.getServer(),t.getUUID("Partner")):null;}
    public static int grace(LivingEntity body){return active(body)?(int)Math.max(0,Math.min(300,body.getPersistentData().getCompound(KEY).getLong("Started")+300-now(body.getServer()))):0;}
    public static boolean available(OperativeHost host){var server=host.serverLevel().getServer();var d=data(server);return !hasTicket(host.body()) && server.getLevel(CorpsStateManager.ALLEY_MAZE_LEVEL)!=null && d.next<65536 && d.leases.size()<16;}
    public static boolean start(OperativeHost host,LivingEntity target) {
        if(!available(host) || hasTicket(target) || host.body().isPassenger() || host.body().isVehicle() || target.isPassenger() || target.isVehicle())return false;
        var server=host.serverLevel().getServer();var arena=server.getLevel(CorpsStateManager.ALLEY_MAZE_LEVEL);var d=data(server);
        int slot=d.next++,x=4096+(slot%256)*128,z=4096+(slot/256)*128;UUID token=UUID.randomUUID();long started=now(server);
        var lease=new CompoundTag();lease.putInt("Slot",slot);lease.putLong("Until",started+4260);d.leases.put(token,lease);d.setDirty();force(arena,slot,true);build(arena,x,z,started);
        var a=ticket(host.body(),target,token,slot,x,z,started,true);var b=ticket(target,host.body(),token,slot,x,z,started,false);
        host.body().getPersistentData().put(KEY,a);target.getPersistentData().put(KEY,b);
        var moved=move(target,arena,new Vec3(x-34.5,80,z-34.5),45,0);
        if(moved==null){end(host.body(),true);return false;}
        moved.setGlowingTag(true);if(moved instanceof ServerPlayer player)player.setGameMode(GameType.ADVENTURE);
        var caster=move(host.body(),arena,new Vec3(x+34.5,80,z+34.5),-135,0);
        if(caster==null){end(moved,true);end(host.body(),true);return false;}
        if(caster instanceof net.minecraft.world.entity.Mob bot){bot.setTarget(moved);bot.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(Math.max(128,bot.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getBaseValue()));bot.getNavigation().setMaxVisitedNodesMultiplier(8);}
        return true;
    }
    private static CompoundTag ticket(LivingEntity body,LivingEntity partner,UUID token,int slot,int x,int z,long started,boolean owner) {
        var t=new CompoundTag();t.putUUID("Token",token);t.putUUID("Partner",partner.getUUID());t.putInt("Slot",slot);t.putInt("CenterX",x);t.putInt("CenterZ",z);t.putLong("Started",started);t.putLong("Until",started+4200);t.putBoolean("Owner",owner);
        var r=new CompoundTag();r.putString("Dimension",body.level().dimension().location().toString());r.putDouble("X",body.getX());r.putDouble("Y",body.getY());r.putDouble("Z",body.getZ());r.putFloat("Yaw",body.getYRot());r.putFloat("Pitch",body.getXRot());r.putBoolean("Glowing",body.isCurrentlyGlowing());
        if(body instanceof net.minecraft.world.entity.Mob bot)r.putDouble("FollowRange",bot.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).getBaseValue());
        if(body instanceof ServerPlayer player)r.putInt("GameMode",player.gameMode.getGameModeForPlayer().getId());t.put("Return",r);return t;
    }
    public static void end(LivingEntity body,boolean immediate) {
        if(!hasTicket(body))return;var t=body.getPersistentData().getCompound(KEY);long exit=now(body.getServer())+(immediate?0:60);
        exit=Math.max(1,exit);if(t.getLong("ExitAt")>0)exit=Math.min(exit,t.getLong("ExitAt"));t.putLong("ExitAt",exit);var partner=opponent(body);
        if(partner!=null && same(t,partner.getPersistentData().getCompound(KEY))){var other=partner.getPersistentData().getCompound(KEY);other.putLong("ExitAt",other.getLong("ExitAt")>0?Math.min(exit,other.getLong("ExitAt")):exit);}
        if(immediate){if(partner!=null && same(t,partner.getPersistentData().getCompound(KEY)))restore(partner);restore(body);}
    }
    private static boolean same(CompoundTag a,CompoundTag b){return a.hasUUID("Token") && b.hasUUID("Token") && a.getUUID("Token").equals(b.getUUID("Token"));}
    /** Retain selected threat intent across vanilla dimension clones, after normal hostility gates. */
    public static boolean selectedOpponent(LivingEntity body,LivingEntity target) {
        if(target==null || !body.isAlive() || !target.isAlive() || body.level()!=target.level()
                || !body.level().dimension().equals(CorpsStateManager.ALLEY_MAZE_LEVEL)
                || !active(body) || !owner(body) || !active(target) || owner(target))return false;
        var a=body.getPersistentData().getCompound(KEY);var b=target.getPersistentData().getCompound(KEY);
        long time=now(body.getServer());
        return validTicket(a,time) && validTicket(b,time) && time<a.getLong("Until") && time<b.getLong("Until")
                && same(a,b) && a.getUUID("Partner").equals(target.getUUID())
                && b.getUUID("Partner").equals(body.getUUID());
    }
    public static void tick(LivingEntity body) {
        if(!(body.level() instanceof ServerLevel level) || !hasTicket(body))return;var t=body.getPersistentData().getCompound(KEY);long time=now(level.getServer());
        if(!validTicket(t,time)){end(body,true);return;}
        if(t.getLong("ExitAt")>0){if(t.getLong("ExitAt")<=time)restore(body);return;}
        var partner=opponent(body);boolean together=partner!=null && partner.isAlive() && same(t,partner.getPersistentData().getCompound(KEY));
        Vec3 c=center(body);
        // A dimension clone is not UUID-addressable until its asynchronous entity chunk is ready.
        // Count absence only after that chunk is usable, then retain the normal 60-tick loss grace.
        var partnerSpawn=BlockPos.containing(c.add(owner(body)?-35:34,0,owner(body)?-35:34));
        if(together || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(partnerSpawn))
                || !level.isNaturalSpawningAllowed(partnerSpawn))t.remove("MissingSince");
        else if(!t.contains("MissingSince") || t.getLong("MissingSince")>time)t.putLong("MissingSince",time);
        if(!body.isAlive() || time>=t.getLong("Until") || !level.dimension().equals(CorpsStateManager.ALLEY_MAZE_LEVEL)
                || Math.abs(body.getX()-c.x)>49 || Math.abs(body.getZ()-c.z)>49 || body.getY()<77 || body.getY()>85
                || t.contains("MissingSince") && time-t.getLong("MissingSince")>=60){end(body,false);return;}
        if(owner(body) && grace(body)>0){body.setDeltaMovement(Vec3.ZERO);body.teleportTo(c.x+34,c.y,c.z+34);}
        if(owner(body) && body instanceof net.minecraft.world.entity.Mob bot)bot.getNavigation().setMaxVisitedNodesMultiplier(8);
        if(!owner(body))body.setGlowingTag(true);
    }
    private static boolean validTicket(CompoundTag t,long time) {
        int slot=t.getInt("Slot");return t.hasUUID("Token") && t.hasUUID("Partner") && slot>=0 && slot<65536
                && t.getInt("CenterX")==4096+(slot%256)*128 && t.getInt("CenterZ")==4096+(slot/256)*128
                && t.getLong("Started")<=time && t.getLong("Until")<=time+4200 && t.getLong("Until")>=t.getLong("Started")
                && t.getLong("ExitAt")<=time+60;
    }
    private static void restore(LivingEntity body) {
        if(!hasTicket(body) || !body.isAlive() && body instanceof ServerPlayer)return;var t=body.getPersistentData().getCompound(KEY);var r=t.getCompound("Return");var id=ResourceLocation.tryParse(r.getString("Dimension"));var server=body.getServer();
        var level=id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));double x=r.getDouble("X"),y=r.getDouble("Y"),z=r.getDouble("Z");
        if(level==null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || Math.abs(x)>29999900 || Math.abs(z)>29999900 || y<level.getMinBuildHeight() || y>level.getMaxBuildHeight()-2){level=server.overworld();var p=level.getSharedSpawnPos();x=p.getX()+.5;y=p.getY();z=p.getZ()+.5;}
        Vec3 destination=null,base=new Vec3(x,y,z);level.getChunk(BlockPos.containing(base));
        int[] offsets={0,-1,1,-2,2};
        for(int dy=0;dy<=4 && destination==null;dy++)for(int dx:offsets)for(int dz:offsets) {
            if(destination!=null)break;var candidate=base.add(dx,dy,dz);
            var box=body.getBoundingBox().move(candidate.subtract(body.position()));if(box.minY>=level.getMinBuildHeight() && box.maxY<=level.getMaxBuildHeight() && level.noCollision(body,box) && level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate))){destination=candidate;break;}
        }
        if(destination==null)return;
        var saved=t.copy();
        float yaw=Float.isFinite(r.getFloat("Yaw"))?r.getFloat("Yaw"):0,pitch=Float.isFinite(r.getFloat("Pitch"))?r.getFloat("Pitch"):0;
        var moved=body.isAlive()?move(body,level,destination,yaw,pitch):body;
        if(moved==null)return;
        moved.getPersistentData().remove(KEY);moved.setGlowingTag(r.getBoolean("Glowing"));
        if(moved instanceof ServerPlayer player)player.setGameMode(GameType.byId(r.getInt("GameMode")));
        if(moved instanceof net.minecraft.world.entity.Mob bot && r.contains("FollowRange",6)){double range=r.getDouble("FollowRange");bot.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(Double.isFinite(range)?Math.max(1,Math.min(2048,range)):24);bot.getNavigation().resetMaxVisitedNodesMultiplier();}
        if(saved.hasUUID("Token")){var d=data(server);var lease=d.leases.get(saved.getUUID("Token"));if(lease!=null){lease.putBoolean(saved.getBoolean("Owner")?"OwnerReturned":"TargetReturned",true);if(lease.getBoolean("OwnerReturned") && lease.getBoolean("TargetReturned"))lease.putLong("Until",Math.min(lease.getLong("Until"),now(server)+80));d.setDirty();}}
    }
    private static LivingEntity move(LivingEntity body,ServerLevel level,Vec3 destination,float yaw,float pitch) {
        if(body instanceof ServerPlayer player){player.teleportTo(level,destination.x,destination.y,destination.z,yaw,pitch);return player.level()==level?player:null;}
        LivingEntity moved=body;
        if(body.level()!=level){var teleporter=new FixedTeleporter(destination,yaw,pitch);var changed=body instanceof MaidSkillHost?vanillaMaidDimensionChange(body,level,destination,yaw,pitch):body.changeDimension(level,teleporter);if(!(changed instanceof LivingEntity living))return null;moved=living;}
        moved.teleportTo(destination.x,destination.y,destination.z);moved.setYRot(yaw);moved.setYHeadRot(yaw);moved.setXRot(pitch);moved.setDeltaMovement(Vec3.ZERO);return moved;
    }
    /** TLM overrides changeDimension with a private teleport routine that returns null. */
    private static Entity vanillaMaidDimensionChange(LivingEntity body,ServerLevel target,Vec3 destination,float yaw,float pitch) {
        try {
            if(!(body.level() instanceof ServerLevel) || body.isRemoved()) return null;
            var clone=body.getType().create(target);
            if(clone==null)return null;
            clone.restoreFrom(body);
            clone.setUUID(body.getUUID());
            clone.moveTo(destination.x,destination.y,destination.z,yaw,pitch);
            clone.setDeltaMovement(Vec3.ZERO);
            if(!target.addFreshEntity(clone)) {
                LogUtils.getLogger().warn("Touhou Little Maid dimension transfer was rejected for {}", body.getUUID());
                return null;
            }
            body.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            return clone;
        } catch(ThreadDeath|VirtualMachineError fatal) { throw fatal; }
        catch(RuntimeException|LinkageError error) {
            LogUtils.getLogger().warn("Skipping Touhou Little Maid dimension transfer for {}", body.getUUID(), error);
            return null;
        }
    }
    private record FixedTeleporter(Vec3 position,float yaw,float pitch) implements net.minecraftforge.common.util.ITeleporter {
        @Override public net.minecraft.world.level.portal.PortalInfo getPortalInfo(Entity entity,ServerLevel level,java.util.function.Function<ServerLevel,net.minecraft.world.level.portal.PortalInfo> fallback){return new net.minecraft.world.level.portal.PortalInfo(position,Vec3.ZERO,yaw,pitch);}
        @Override public Entity placeEntity(Entity entity,ServerLevel current,ServerLevel target,float rotation,java.util.function.Function<Boolean,Entity> reposition){var placed=reposition.apply(false);placed.teleportTo(position.x,position.y,position.z);return placed;}
    }
    private static void force(ServerLevel arena,int slot,boolean value){int x=4096+(slot%256)*128,z=4096+(slot/256)*128;for(int cx=(x-49)>>4;cx<=(x+49)>>4;cx++)for(int cz=(z-49)>>4;cz<=(z+49)>>4;cz++)arena.setChunkForced(cx,cz,value);}
    public static void tickLeases(MinecraftServer server) {
        if(now(server)%20!=0)return;var arena=server.getLevel(CorpsStateManager.ALLEY_MAZE_LEVEL);if(arena==null)return;var d=data(server);var iterator=d.leases.entrySet().iterator();
        while(iterator.hasNext()){var entry=iterator.next();if(entry.getValue().getLong("Until")<=now(server) || entry.getValue().getLong("Until")>now(server)+4260){force(arena,entry.getValue().getInt("Slot"),false);iterator.remove();d.setDirty();}}
    }
    private static void build(ServerLevel level,int cx,int cz,long seed) {
        var p=new BlockPos.MutableBlockPos();
        for(int x=-49;x<=49;x++)for(int z=-49;z<=49;z++) {
            boolean spawn=Math.abs(x-34)<4 && Math.abs(z-34)<4 || Math.abs(x+34)<4 && Math.abs(z+34)<4;
            boolean wall=Math.abs(x)==49 || Math.abs(z)==49 || !spawn && (Math.floorMod(x+49,8)==0 && Math.floorMod(z+seed,13)>2 || Math.floorMod(z+49,8)==0 && Math.floorMod(x-seed,11)>2);
            level.setBlock(p.set(cx+x,79,cz+z),(Math.floorMod(x+z,19)==0?Blocks.SEA_LANTERN:Math.floorMod(x+z,2)==0?Blocks.SMOOTH_STONE:Blocks.STONE_BRICKS).defaultBlockState(),2);
            for(int y=80;y<=84;y++)level.setBlock(p.set(cx+x,y,cz+z),(wall || y==84?Blocks.GRAY_CONCRETE:Blocks.AIR).defaultBlockState(),2);
        }
    }
    public static final class Allocations extends SavedData {
        private int next;
        private final Map<UUID,CompoundTag> leases=new HashMap<>();
        public static Allocations load(CompoundTag tag){var d=new Allocations();d.next=Math.max(0,Math.min(65536,tag.getInt("Next")));var entries=tag.getList("Leases",10);for(int i=0;i<Math.min(16,entries.size());i++){var e=entries.getCompound(i);int slot=e.getInt("Slot");if(e.hasUUID("Token") && slot>=0 && slot<65536){d.leases.put(e.getUUID("Token"),e.copy());d.next=Math.max(d.next,slot+1);}}return d;}
        @Override public CompoundTag save(CompoundTag tag){tag.putInt("Next",next);var entries=new ListTag();leases.forEach((token,lease)->{var e=lease.copy();e.putUUID("Token",token);entries.add(e);});tag.put("Leases",entries);return tag;}
    }
}
