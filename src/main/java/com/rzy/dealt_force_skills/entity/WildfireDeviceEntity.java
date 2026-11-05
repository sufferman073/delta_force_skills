package com.rzy.dealt_force_skills.entity;

import com.rzy.dealt_force_skills.character.wildfire.*;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

/** Smoke fields and their two bouncing delivery devices share one tracked entity type. */
public final class WildfireDeviceEntity extends Entity implements ItemSupplier, SmokeCloudModelState {
    public static final int SMOKE_GRENADE=0, HE_GRENADE=1, CLOUD=2, RING=3;
    private static final EntityDataAccessor<CompoundTag> DATA=SynchedEntityData.defineId(WildfireDeviceEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private UUID ownerId;
    private boolean resting, countdownPlayed;
    private final java.util.List<UUID> ringClouds=new java.util.ArrayList<>();
    private boolean ringCloudsCreated;
    public WildfireDeviceEntity(EntityType<? extends WildfireDeviceEntity> type,Level level){super(type,level);noCulling=true;}
    @Override protected void defineSynchedData(){entityData.define(DATA,new CompoundTag());}
    public int mode(){return entityData.get(DATA).getInt("Mode");}
    public double radius(){return entityData.get(DATA).getDouble("Radius");}
    public double width(){return entityData.get(DATA).getDouble("Width");}
    public double height(){return entityData.get(DATA).getDouble("Height");}
    public UUID ownerId(){return ownerId;}
    @Override public int smokeLifetimeTicks(){return Math.max(1,entityData.get(DATA).getInt("Life"));}
    @Override public boolean shouldRenderSmokeModel(){return mode()==CLOUD;}
    @Override public ItemStack getItem(){return new ItemStack(mode()==HE_GRENADE?Items.SLIME_BALL:Items.GRAY_DYE);}
    public void configure(ServerPlayer owner,int mode){
        ownerId=owner.getUUID();CompoundTag tag=new CompoundTag();tag.putInt("Mode",mode);
        tag.putInt("Life",WildfireConfig.ticks(mode==HE_GRENADE?"grenade.fuse_ticks":"projectile.smoke_max_flight_ticks",mode==HE_GRENADE?60:100));
        entityData.set(DATA,tag);
    }
    public static WildfireDeviceEntity smoke(ServerPlayer owner,Vec3 position,boolean ring){
        WildfireDeviceEntity cloud=new WildfireDeviceEntity(ModEntities.WILDFIRE_DEVICE.get(),owner.level());
        cloud.ownerId=owner.getUUID();CompoundTag tag=new CompoundTag();tag.putInt("Mode",ring?RING:CLOUD);
        tag.putInt("Life",WildfireConfig.ticks("smoke.duration_ticks",160));
        double front=WildfireConfig.number("smoke.ring_front",5),back=WildfireConfig.number("smoke.ring_back",1);
        tag.putDouble("Radius",Math.min(64,ring?(front+back)/2:WildfireConfig.number("smoke.radius",5)));
        // Keep the caster in the clear central opening, including asymmetric rings.
        tag.putDouble("Width",Math.min(WildfireConfig.number("smoke.ring_width",.8),Math.min(front,back)*.8));
        tag.putDouble("Height",Math.min(64,WildfireConfig.number("smoke.height",3)));
        cloud.entityData.set(DATA,tag);
        if(ring)position=position.add(owner.getLookAngle().multiply(1,0,1).normalize().scale((front-back)/2));
        cloud.setPos(position);cloud.noPhysics=true;owner.serverLevel().addFreshEntity(cloud);return cloud;
    }
    private void createRingClouds(){
        if(!(level() instanceof ServerLevel level)||ringCloudsCreated)return;
        ringCloudsCreated=true;
        // Position full, existing smoke clouds outside the clear opening. No model scaling or clipping.
        double inner=(WildfireConfig.number("smoke.ring_front",5)+WildfireConfig.number("smoke.ring_back",1))/2;
        double nativeExtent=4.5,centers=inner+nativeExtent;
        CompoundTag data=entityData.get(DATA).copy();data.putDouble("Radius",centers+nativeExtent);
        data.putDouble("Width",nativeExtent*2);entityData.set(DATA,data);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI*2/12;
            DWolfSmokeCloudEntity smoke=new DWolfSmokeCloudEntity(ModEntities.D_WOLF_SMOKE_CLOUD.get(),level);
            smoke.setPos(position().add(Math.cos(angle)*centers,0,Math.sin(angle)*centers));
            smoke.setLifeTicks(Math.max(1,smokeLifetimeTicks()-tickCount));smoke.setOperativeLifetime(true);
            var link=smoke.getPersistentData();link.putUUID("dfs.wildfire.parent",getUUID());
            link.putLong("dfs.wildfire.parentPos",blockPosition().asLong());
            link.putLong("dfs.wildfire.expiry",level.getGameTime()+Math.max(1,smokeLifetimeTicks()-tickCount));
            if(level.addFreshEntity(smoke))ringClouds.add(smoke.getUUID());
        }
    }
    @Override public void remove(RemovalReason reason){
        if(reason.shouldDestroy()&&level() instanceof ServerLevel level)
            for(UUID id:ringClouds){Entity cloud=level.getEntity(id);if(cloud instanceof DWolfSmokeCloudEntity)cloud.discard();}
        super.remove(reason);
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(mode()>=CLOUD){if(mode()==RING)createRingClouds();suppressTargets();if(tickCount>=smokeLifetimeTicks())discard();return;}
        if(mode()==HE_GRENADE&&tickCount>=smokeLifetimeTicks()){explode();return;}
        if(mode()==HE_GRENADE&&!countdownPlayed&&tickCount>=Math.max(1,smokeLifetimeTicks()-60)){
            countdownPlayed=true;
            // The entity-bound packet follows the grenade and stops when it is removed.
            level().playSound(null,this,ModSounds.WILDFIRE_GRENADE_COUNTDOWN.get(),SoundSource.PLAYERS,.85F,1);
        }
        if(mode()==SMOKE_GRENADE&&tickCount>=smokeLifetimeTicks()){burst();return;}
        if(resting)return;
        Vec3 velocity=getDeltaMovement();Vec3 start=position();
        BlockHitResult hit=level().clip(new ClipContext(start,start.add(velocity),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(hit.getType()==HitResult.Type.BLOCK){
            Vec3 normal=Vec3.atLowerCornerOf(hit.getDirection().getNormal());setPos(hit.getLocation().add(normal.scale(.04)));
            if(hit.getDirection()==net.minecraft.core.Direction.UP){
                if(mode()==SMOKE_GRENADE){burst();return;}resting=true;setDeltaMovement(Vec3.ZERO);
            }else{
                setDeltaMovement(velocity.subtract(normal.scale(2*velocity.dot(normal))).scale(WildfireConfig.fraction("projectile.bounce_factor",.7)));
                if(mode()==SMOKE_GRENADE)level().playSound(null,blockPosition(),ModSounds.WILDFIRE_SMOKE_BOUNCE.get(),SoundSource.PLAYERS,.65F,1);
            }
        }else {setPos(start.add(velocity));setDeltaMovement(velocity.scale(WildfireConfig.fraction("projectile.drag",.985)).add(0,-WildfireConfig.number("projectile.gravity",.045),0));}
        if(getY()<level().getMinBuildHeight()-32)discard();
    }
    private void burst(){
        if(level() instanceof ServerLevel level&&ownerId!=null&&level.getPlayerByUUID(ownerId) instanceof ServerPlayer owner)
            smoke(owner,position(),false);
        level().playSound(null,blockPosition(),ModSounds.WILDFIRE_SMOKE_TRIGGER.get(),SoundSource.PLAYERS,1,1);discard();
    }
    public static float grenadeDamage(double distance){
        double radius=Math.min(64,WildfireConfig.number("grenade.radius",7));
        return distance>radius?0:(float)(WildfireConfig.number("grenade.damage",50)*Math.max(0,1-distance*WildfireConfig.fraction("grenade.falloff_per_block",.1)));
    }
    public void explode(){
        if(!(level() instanceof ServerLevel level))return;
        Entity owner=ownerId==null?null:level.getEntity(ownerId);double radius=Math.min(64,WildfireConfig.number("grenade.radius",7));
        WildfireDamageSource source=new WildfireDamageSource(level,this,owner,false);
        for(Entity target:level.getEntities(this,getBoundingBox().inflate(radius),e->e.isAlive()&&e.distanceToSqr(this)<=radius*radius)){
            if(target instanceof LivingEntity living&&target!=owner&&!TargetingUtil.isHostileLivingFor(owner,living))continue;
            float damage=grenadeDamage(target.position().distanceTo(position()));if(damage<=0)continue;
            if(target instanceof LivingEntity living)living.invulnerableTime=0;target.hurt(source,damage);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,getX(),getY(),getZ(),1,0,0,0,0);
        level.playSound(null,blockPosition(),ModSounds.WILDFIRE_GRENADE_EXPLODE.get(),SoundSource.PLAYERS,2,1);discard();
    }
    public boolean containsSmoke(Vec3 point){
        if(mode()<CLOUD)return false;double y=point.y-getY();if(y<0||y>height())return false;
        double d=point.subtract(position()).multiply(1,0,1).length();
        return d<=radius()&&(mode()!=RING||d>=Math.max(0,radius()-width()));
    }
    public boolean blocksSight(Vec3 from,Vec3 to){
        if(containsSmoke(from)&&containsSmoke(to))return false;
        double length=from.distanceTo(to);if(length<.001)return false;
        // Exact horizontal circle crossings plus vertical slab crossings avoid holes in thin rings.
        Vec3 delta=to.subtract(from);java.util.List<Double> cuts=new java.util.ArrayList<>(java.util.List.of(0D,1D));
        double a=delta.x*delta.x+delta.z*delta.z,b=2*((from.x-getX())*delta.x+(from.z-getZ())*delta.z);
        if(a>1E-10)for(double r:new double[]{radius(),Math.max(0,radius()-width())}){
            double c=(from.x-getX())*(from.x-getX())+(from.z-getZ())*(from.z-getZ())-r*r,d=b*b-4*a*c;
            if(d>=0){cuts.add(Math.max(0,Math.min(1,(-b-Math.sqrt(d))/(2*a))));cuts.add(Math.max(0,Math.min(1,(-b+Math.sqrt(d))/(2*a))));}
        }
        if(Math.abs(delta.y)>1E-10){cuts.add(Math.max(0,Math.min(1,(getY()-from.y)/delta.y)));cuts.add(Math.max(0,Math.min(1,(getY()+height()-from.y)/delta.y)));}
        cuts.sort(Double::compare);for(int i=1;i<cuts.size();i++)if(containsSmoke(from.add(delta.scale((cuts.get(i-1)+cuts.get(i))/2))))return true;
        return false;
    }
    public boolean conceals(LivingEntity target){return mode()>=CLOUD&&target.getUUID().equals(ownerId);}
    private void suppressTargets(){
        if(!(level() instanceof ServerLevel level)||ownerId==null||!(level.getEntity(ownerId) instanceof LivingEntity owner)||!owner.isAlive())return;
        double range=Math.min(128,WildfireConfig.number("smoke.target_suppression_range",40));
        for(Mob mob:level.getEntitiesOfClass(Mob.class,owner.getBoundingBox().inflate(range),m->m.getTarget()==owner))
            if(blocksSight(mob.getEyePosition(),owner.getEyePosition())){mob.setTarget(null);mob.getNavigation().stop();}
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        tag.put("Data",entityData.get(DATA).copy());if(ownerId!=null)tag.putUUID("Owner",ownerId);tag.putInt("Age",tickCount);tag.putBoolean("Resting",resting);tag.putBoolean("CountdownPlayed",countdownPlayed);
        tag.putBoolean("RingCloudsCreated",ringCloudsCreated);var ids=new net.minecraft.nbt.ListTag();
        for(UUID id:ringClouds){var entry=new CompoundTag();entry.putUUID("Id",id);ids.add(entry);}tag.put("RingClouds",ids);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        entityData.set(DATA,tag.getCompound("Data"));ownerId=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;tickCount=Math.max(0,tag.getInt("Age"));resting=tag.getBoolean("Resting");countdownPlayed=tag.getBoolean("CountdownPlayed");noPhysics=mode()>=CLOUD;
        ringCloudsCreated=tag.getBoolean("RingCloudsCreated");ringClouds.clear();var ids=tag.getList("RingClouds",net.minecraft.nbt.Tag.TAG_COMPOUND);
        for(int i=0;i<ids.size();i++){var entry=ids.getCompound(i);if(entry.hasUUID("Id"))ringClouds.add(entry.getUUID("Id"));}
    }
    @Override public boolean isPickable(){return false;}
    @Override public boolean isAttackable(){return false;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<512*512;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
