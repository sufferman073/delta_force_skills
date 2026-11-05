package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.saeed.SaeedGuardType;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.SaeedFireArrowEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Native roll, physical fire crossbow and temporary quartet; no player recruitment account. */
public final class NativeSaeedSkills implements OperativeSkills {
    private int rollCooldown, rollTicks, ammo = capacity(), recharge, shotCooldown;
    private long coreUntil, coreReadyAt;
    private Vec3 rollDirection = Vec3.ZERO;
    private final List<UUID> guards = new ArrayList<>();

    private static int integer(String key,int value) { return Math.max(0,DealtForceConfig.intValue("characters.saeed.saeed_state_manager."+key,value)); }
    private static double number(String key,double value) { return DealtForceConfig.doubleValue("characters.saeed.saeed_state_manager."+key,value); }
    public static int duration() { return Math.max(1,integer("core_duration_ticks",1800)); }
    private static int capacity() { return integer("fire_max_ammo",2); }
    private static int reload() { return Math.max(1,integer("fire_recharge_ticks",600)); }
    public boolean coreActive() { return coreUntil>0 && !guards.isEmpty(); }
    public boolean ownsGuard(UUID id,long deadline) { return coreActive() && guards.contains(id) && deadline==coreUntil; }

    @Override public void tick(OperativeHost host) {
        if(rollCooldown>0)rollCooldown--;
        if(shotCooldown>0)shotCooldown--;
        ammo=Math.min(ammo,capacity());
        if(ammo>=capacity())recharge=0;
        else if(recharge<=0)recharge=reload();
        else if(--recharge==0){ammo++;if(ammo<capacity())recharge=reload();}
        long now=host.serverLevel().getGameTime();
        if(coreUntil>now+duration() || coreUntil>0 && coreUntil<=now) clearGuards(host);
        coreReadyAt=Math.min(coreReadyAt,now+(long)duration()+integer("core_cooldown_ticks",2400));
        if(rollTicks<=0)return;
        if(!NativeRaptorSkills.available(host) || host.body().isInWaterOrBubble()
                || OperativeElectronics.blocked(host,ModCharacters.SAEED_ID,SkillSlot.ACTIVE_1)
                || !safeRoll(host,rollDirection)){cancel(host);return;}
        if(rollTicks==1){cancel(host);return;}
        rollTicks--;applyRollImpulse(host);
    }

    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(busy() || !NativeRaptorSkills.available(host) || OperativeSupport.downed(host.body())
                || OperativeElectronics.blocked(host,ModCharacters.SAEED_ID,slot))return 0;
        boolean threat=target!=null && target.level()==host.body().level() && host.isEnemy(target) && host.withinCombatRange(target.position());
        return switch(slot){
            case ACTIVE_1 -> rollCooldown==0 && host.body().onGround() && !host.body().isInWaterOrBubble()
                    && threat && (host.body().distanceToSqr(target)>=36 || host.body().getHealth()<host.body().getMaxHealth()*0.5F)
                    && safeRoll(host,rollVector(host,target))?65:0;
            case ACTIVE_2 -> ammo>0 && shotCooldown==0 && threat && shot(host,target)!=null?85:0;
            case CORE -> !coreActive() && host.serverLevel().getGameTime()>=coreReadyAt && threat
                    && host.body().distanceToSqr(target)<=48*48 && host.canSee(target)?110:0;
            case PASSIVE -> 0;
        };
    }

    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(score(host,slot,target)<=0)return false;
        if(slot==SkillSlot.ACTIVE_1){
            rollDirection=rollVector(host,target);rollTicks=Math.max(1,integer("roll_boost_ticks_total",9));
            rollCooldown=integer("roll_cooldown_ticks",240);applyRollImpulse(host);sound(host,ModSounds.SAEED_ROLL_START.get());return true;
        }
        if(slot==SkillSlot.ACTIVE_2){
            var shot=shot(host,target);if(shot==null)return false;
            var arrow=new SaeedFireArrowEntity(ModEntities.SAEED_FIRE_ARROW.get(),host.body().level(),host.body(),false);
            arrow.setPos(host.body().getEyePosition().add(shot.direction().scale(0.75)).add(0,-0.05,0));
            arrow.setDeltaMovement(shot.direction().scale(2.25));
            if(!host.serverLevel().addFreshEntity(arrow))return false;
            if(ammo==capacity())recharge=reload();ammo--;shotCooldown=20;host.aimAt(target);
            sound(host,ModSounds.SAEED_CROSSBOW_FIRE.get());return true;
        }
        if(slot!=SkillSlot.CORE)return false;
        var quartet=new ArrayList<NativeSaeedGuardEntity>();
        var boxes=new ArrayList<AABB>();
        for(var type:List.of(SaeedGuardType.IRON_RAIN,SaeedGuardType.FIREEYE,SaeedGuardType.SHARP_EAGLE,SaeedGuardType.THUNDER)){
            var guard=ModEntities.OPERATIVE_SAEED_GUARD.get().create(host.serverLevel());
            if(guard==null)return false;
            Vec3 point=spawnPoint(host,guard,boxes);
            if(point==null)return false;
            guard.configure(host,type,duration());guard.setPos(point);guard.setTarget(target);
            quartet.add(guard);boxes.add(guard.getBoundingBox());
        }
        for(var guard:quartet)if(!host.serverLevel().addFreshEntity(guard)){
            for(var added:quartet)added.discard();return false;
        }
        coreUntil=host.serverLevel().getGameTime()+duration();
        coreReadyAt=coreUntil+integer("core_cooldown_ticks",2400);
        for(var guard:quartet)guards.add(guard.getUUID());
        sound(host,ModSounds.SAEED_COMMAND_CALL.get());return true;
    }

    private void applyRollImpulse(OperativeHost host) {
        host.body().setPose(Pose.SWIMMING);host.body().refreshDimensions();
        Vec3 motion=rollDirection.scale(number("roll_boost_speed_per_tick",2.75));
        host.body().setDeltaMovement(motion.x,host.body().getDeltaMovement().y,motion.z);
        host.body().fallDistance=0;host.body().hurtMarked=true;
    }

    private static Vec3 spawnPoint(OperativeHost host,NativeSaeedGuardEntity guard,List<AABB> used) {
        double radius=Math.max(1.5,Math.min(6,number("guard_summon_radius",2.6)));
        for(int attempt=0;attempt<24;attempt++){
            double angle=Math.PI*2*attempt/24;
            Vec3 center=host.body().position().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
            for(int dy=1;dy>=-2;dy--){
                Vec3 point=new Vec3(center.x,BlockPos.containing(center).getY()+dy,center.z);
                AABB box=guard.getBoundingBox().move(point.subtract(guard.position()));
                if(!host.withinOrders(point) || !host.serverLevel().hasChunkAt(BlockPos.containing(point))
                        || box.minY<host.serverLevel().getMinBuildHeight() || box.maxY>host.serverLevel().getMaxBuildHeight()
                        || !host.serverLevel().getWorldBorder().isWithinBounds(box) || !host.serverLevel().noCollision(box)
                        || host.serverLevel().containsAnyLiquid(box) || used.stream().anyMatch(box::intersects))continue;
                BlockPos ground=BlockPos.containing(point).below();
                if(host.serverLevel().getBlockState(ground).isFaceSturdy(host.serverLevel(),ground,net.minecraft.core.Direction.UP))return point;
            }
        }
        return null;
    }
    private static OperativeBallistics.Shot shot(OperativeHost host,LivingEntity target) {
        return host.body().distanceToSqr(target)<=40*40?OperativeBallistics.direct(host,target,2.25,0.75,0.05,0.02,0.995,80):null;
    }
    private static Vec3 rollVector(OperativeHost host,LivingEntity target) {
        return OperativeTactics.displacement(host, target, number("roll_boost_speed_per_tick", 2.75) * Math.max(1, integer("roll_boost_ticks_total", 9)));
    }
    private static boolean safeRoll(OperativeHost host,Vec3 direction) {
        double speed=number("roll_boost_speed_per_tick",2.75);
        if(!Double.isFinite(speed) || speed<=0 || speed>8)return false;
        Vec3 delta=direction.scale(speed),next=host.body().position().add(delta);
        AABB swept=host.body().getBoundingBox().expandTowards(delta),box=host.body().getBoundingBox().move(delta);
        return host.withinOrders(next) && host.serverLevel().hasChunkAt(BlockPos.containing(next))
                && host.serverLevel().getWorldBorder().isWithinBounds(box)
                && host.serverLevel().noCollision(host.body(),swept) && !host.serverLevel().containsAnyLiquid(box)
                && !host.serverLevel().noCollision(host.body(),box.move(0,-1,0));
    }
    public void onKill(OperativeHost host,LivingEntity victim) {
        if(victim!=host.body() && !newAllegiance(host).isAlly(host.serverLevel(),victim)){
            rollCooldown=0;sound(host,ModSounds.SAEED_ROLL_END.get());
        }
    }
    private static OperativeAllegiance newAllegiance(OperativeHost host){var a=new OperativeAllegiance();a.capture(host.body());return a;}
    private static void sound(OperativeHost host,SoundEvent sound) {host.serverLevel().playSound(null,host.body().blockPosition(),sound,SoundSource.HOSTILE,0.85F,1);}
    @Override public boolean busy(){return rollTicks>0;}
    @Override public void cancel(OperativeHost host){
        if(rollTicks>0)host.body().setDeltaMovement(0,host.body().getDeltaMovement().y,0);
        rollTicks=0;
        if(host.body() instanceof com.rzy.dealt_force_skills.entity.OperativeEntity bot)bot.tryStanding();
    }
    private void clearGuards(OperativeHost host){
        for(UUID id:guards)if(host.serverLevel().getEntity(id) instanceof NativeSaeedGuardEntity guard)guard.discard();
        guards.clear();coreUntil=0;
    }
    public void clear(OperativeHost host){cancel(host);clearGuards(host);}
    @Override public CompoundTag save(){
        var tag=new CompoundTag();tag.putInt("RollCooldown",rollCooldown);tag.putInt("RollTicks",rollTicks);
        tag.putDouble("RollX",rollDirection.x);tag.putDouble("RollZ",rollDirection.z);
        tag.putInt("Ammo",ammo);tag.putInt("Recharge",recharge);tag.putInt("ShotCooldown",shotCooldown);
        tag.putLong("CoreUntil",coreUntil);tag.putLong("CoreReadyAt",coreReadyAt);
        var list=new ListTag();for(UUID id:guards){var entry=new CompoundTag();entry.putUUID("Id",id);list.add(entry);}tag.put("Guards",list);return tag;
    }
    @Override public void load(CompoundTag tag){
        rollCooldown=Math.max(0,Math.min(integer("roll_cooldown_ticks",240),tag.getInt("RollCooldown")));
        rollTicks=Math.max(0,Math.min(integer("roll_boost_ticks_total",9),tag.getInt("RollTicks")));
        Vec3 direction=new Vec3(tag.getDouble("RollX"),0,tag.getDouble("RollZ"));
        rollDirection=Double.isFinite(direction.lengthSqr()) && direction.lengthSqr()>1.0e-5?direction.normalize():Vec3.ZERO;
        if(rollDirection==Vec3.ZERO)rollTicks=0;
        ammo=tag.contains("Ammo")?Math.max(0,Math.min(capacity(),tag.getInt("Ammo"))):capacity();
        recharge=Math.max(0,Math.min(reload(),tag.getInt("Recharge")));shotCooldown=Math.max(0,Math.min(20,tag.getInt("ShotCooldown")));
        coreUntil=Math.max(0,tag.getLong("CoreUntil"));coreReadyAt=Math.max(0,tag.getLong("CoreReadyAt"));
        guards.clear();var list=tag.getList("Guards",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(4,list.size());i++)if(list.getCompound(i).hasUUID("Id")){
            var id=list.getCompound(i).getUUID("Id");if(!guards.contains(id))guards.add(id);
        }
        if(guards.isEmpty())coreUntil=0;
    }
    @Override public String describe(){return "Saeed roll="+rollCooldown+" arrow="+ammo+"/"+capacity()+" quartet="+guards.size();}

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeSaeedSkills();
        recharge = fresh.recharge;
        rollCooldown = fresh.rollCooldown;
        shotCooldown = fresh.shotCooldown;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return rollTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.ROLL : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return Math.max(1, integer("roll_boost_ticks_total", 9)); }
    @Override public Vec3 motionDirection() { return rollDirection; }

}
