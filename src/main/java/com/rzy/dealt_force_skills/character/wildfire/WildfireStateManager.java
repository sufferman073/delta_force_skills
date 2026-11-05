package com.rzy.dealt_force_skills.character.wildfire;

import com.rzy.dealt_force_skills.character.*;
import com.rzy.dealt_force_skills.character.electronics.ElectronicInterferenceManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import com.rzy.dealt_force_skills.entity.WildfireDeviceEntity;
import com.rzy.dealt_force_skills.network.*;
import com.rzy.dealt_force_skills.registry.*;
import com.rzy.dealt_force_skills.skill.*;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class WildfireStateManager {
    public static final String DATA="dealt_force_skills.wildfire";
    private static final ResourceLocation SPEED=ResourceLocation.fromNamespaceAndPath("dealt_force_skills","wildfire_speed");
    private static final ResourceLocation STEP=ResourceLocation.fromNamespaceAndPath("dealt_force_skills","wildfire_step");
    private static final Map<ServerPlayer,State> STATES=new WeakHashMap<>();
    private WildfireStateManager() { }
    private static final class State {
        int smoke,grenade,smokeRecharge,grenadeRecharge,cooldown,phase,prepare,age,tool,blocked,leapAge;
        long passiveUntil; Object level; Vec3 previous;
    }
    public static boolean isWildfire(Player player) {
        return player!=null&&CharacterSelectionManager.getSelectedCharacterId(player).filter(ModCharacters.WILDFIRE_ID::equals).isPresent();
    }
    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player,p->{
            State s=new State();CompoundTag data=p.getPersistentData().getCompound(DATA);
            s.smoke=data.contains("Smoke")?Math.min(WildfireConfig.maxCharges(true),data.getInt("Smoke")):WildfireConfig.maxCharges(true);
            s.grenade=data.contains("Grenade")?Math.min(WildfireConfig.maxCharges(false),data.getInt("Grenade")):WildfireConfig.maxCharges(false);
            s.smokeRecharge=Math.max(0,data.getInt("SmokeRecharge"));s.grenadeRecharge=Math.max(0,data.getInt("GrenadeRecharge"));
            s.cooldown=Math.max(0,data.getInt("Cooldown"));s.level=p.level();s.previous=p.position();return s;
        });
    }
    /** Supply restores one charge; the author's blessing fills both charge pools. */
    public static void refreshSkills(ServerPlayer player, boolean fill) {
        if (!isWildfire(player)) return;
        State s=state(player);
        int smokeMax=WildfireConfig.maxCharges(true),grenadeMax=WildfireConfig.maxCharges(false);
        s.smoke=fill?smokeMax:Math.min(smokeMax,Math.max(0,s.smoke)+1);
        s.grenade=fill?grenadeMax:Math.min(grenadeMax,Math.max(0,s.grenade)+1);
        if(s.smoke>=smokeMax)s.smokeRecharge=0;
        else if(s.smokeRecharge<=0)s.smokeRecharge=WildfireConfig.cooldown(true);
        if(s.grenade>=grenadeMax)s.grenadeRecharge=0;
        else if(s.grenadeRecharge<=0)s.grenadeRecharge=WildfireConfig.cooldown(false);
        s.cooldown=0;
        player.getPersistentData().put(DATA,snapshot(player));
        sync(player);
    }
    public static CompoundTag snapshot(ServerPlayer player) {
        State s=state(player);CompoundTag tag=new CompoundTag();
        tag.putInt("Smoke",s.smoke);tag.putInt("Grenade",s.grenade);
        tag.putInt("SmokeMax",WildfireConfig.maxCharges(true));tag.putInt("GrenadeMax",WildfireConfig.maxCharges(false));
        tag.putInt("SmokeRecharge",s.smokeRecharge);tag.putInt("GrenadeRecharge",s.grenadeRecharge);
        tag.putInt("Cooldown",s.cooldown);tag.putInt("Phase",s.phase);tag.putInt("Prepare",s.prepare);
        tag.putInt("CoreAge",s.age);tag.putInt("AccelerationTicks",WildfireConfig.ticks("core.acceleration_ticks",200));
        tag.putInt("Tool",s.tool);tag.putBoolean("Passive",passiveActive(player));
        tag.putDouble("Speed",forwardSpeed(player,s.age));return tag;
    }
    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingAndSelf(new S2C_WildfireState(player.getId(),snapshot(player)),player);
        HeldToolVisualSync.sync(player);
    }
    public static HeldToolVisual heldVisual(ServerPlayer player) {
        State s=STATES.get(player);return s==null?HeldToolVisual.NONE:s.tool==1?HeldToolVisual.STINGER_SMOKE_GRENADE:s.tool==2?HeldToolVisual.SHEPHERD_GRENADE:HeldToolVisual.NONE;
    }
    public static boolean useSkill(ServerPlayer player,SkillSlot slot,boolean alternate) {
        if(!isWildfire(player))return false;
        State s=state(player);
        if(s.phase!=0&&slot!=SkillSlot.CORE&&(slot!=SkillSlot.ACTIVE_1||alternate||s.phase==1))return false;
        if(slot==SkillSlot.CORE) {
            if(s.phase==1)return true;
            if(s.phase==2) {
                s.phase=3;s.leapAge=0;player.setDeltaMovement(player.getDeltaMovement().x,
                        leapVelocity(WildfireConfig.number("core.leap_height",3)),player.getDeltaMovement().z);
                player.hurtMarked=true;player.fallDistance=0;
                sound(player,ModSounds.WILDFIRE_CORE_LEAP.get(),1);sync(player);return true;
            }
            if(s.phase==3)return true;
            if(s.cooldown>0){cooldown(player);return true;}
            s.phase=1;s.prepare=WildfireConfig.ticks("core.prepare_ticks",40);s.cooldown=WildfireConfig.coreCooldown();
            stowTool(player,s);s.age=0;s.blocked=0;s.previous=player.position();player.stopUsingItem();
            sound(player,ModSounds.WILDFIRE_CORE_START.get(),1);sync(player);return true;
        }
        if(slot!=SkillSlot.ACTIVE_1&&slot!=SkillSlot.ACTIVE_2)return false;
        boolean smoke=slot==SkillSlot.ACTIVE_1;
        if((smoke?s.smoke:s.grenade)<=0){cooldown(player);return true;}
        if(alternate) {
            if(s.tool!=0&&s.tool!=(smoke?1:2))stowTool(player,s);
            s.tool=smoke?1:2;player.stopUsingItem();
            sound(player,smoke?ModSounds.WILDFIRE_SMOKE_EQUIP.get():ModSounds.WILDFIRE_GRENADE_EQUIP.get(),1);
            sync(player);return true;
        }
        if(smoke){WildfireDeviceEntity.smoke(player,player.position(),true);consume(s,true);sound(player,ModSounds.WILDFIRE_SMOKE_RING.get(),.85F);}
        else throwDevice(player,s,false,false);
        sync(player);return true;
    }
    /** action: 0 stow, 1 throw equipped tool, 2 cancel preparation. */
    public static void toolAction(ServerPlayer player,int action) {
        if(!isWildfire(player)||!player.isAlive()||player.isSpectator())return;
        State s=state(player);
        if(action==2) {cancelPreparation(player);return;}
        if(action==0) {stowTool(player,s);sync(player);return;}
        if(action!=1||s.tool==0||s.phase!=0)return;
        SkillSlot slot=s.tool==1?SkillSlot.ACTIVE_1:SkillSlot.ACTIVE_2;
        if(!SkillDispatcher.passesInputGuards(player,slot)||ElectronicInterferenceManager.tryBlockSkillUse(player,ModCharacters.WILDFIRE,slot))return;
        boolean smoke=s.tool==1;
        if((smoke?s.smoke:s.grenade)<=0)return;
        throwDevice(player,s,smoke,true);sync(player);
    }
    private static void throwDevice(ServerPlayer player,State s,boolean smoke,boolean high) {
        WildfireDeviceEntity device=new WildfireDeviceEntity(ModEntities.WILDFIRE_DEVICE.get(),player.level());
        device.configure(player,smoke?WildfireDeviceEntity.SMOKE_GRENADE:WildfireDeviceEntity.HE_GRENADE);
        Vec3 look=player.getLookAngle().normalize();device.setPos(player.getEyePosition().add(look.scale(.6)).add(0,-.1,0));
        device.setDeltaMovement(look.scale(WildfireConfig.number(high?"projectile.high_speed":"projectile.low_speed",high?1.55:1.05))
                .add(0,WildfireConfig.number(high?"projectile.high_lift":"projectile.low_lift",high?.3:.08),0));
        if(player.serverLevel().addFreshEntity(device))consume(s,smoke);
        s.tool=0;sound(player,smoke?ModSounds.WILDFIRE_SMOKE_THROW.get():ModSounds.WILDFIRE_GRENADE_THROW.get(),1);
    }
    private static void stowTool(ServerPlayer player,State s) {
        if(s.tool==2)sound(player,ModSounds.WILDFIRE_GRENADE_STOW.get(),1);
        s.tool=0;
    }
    private static void consume(State s,boolean smoke) {
        if(smoke){s.smoke--;if(s.smokeRecharge<=0)s.smokeRecharge=WildfireConfig.cooldown(true);}
        else{s.grenade--;if(s.grenadeRecharge<=0)s.grenadeRecharge=WildfireConfig.cooldown(false);}
    }
    public static void cancelPreparation(ServerPlayer player) {
        State s=STATES.get(player);if(s==null||s.phase!=1)return;
        s.cooldown=(int)Math.ceil(WildfireConfig.coreCooldown()*(1-WildfireConfig.fraction("core.cancel_refund",.9)));
        endCore(player,s);sync(player);
    }
    public static boolean blocksItems(Player player) {
        if(!(player instanceof ServerPlayer p))return false;
        State s=STATES.get(p);return s!=null&&isWildfire(p)&&(s.phase!=0||s.tool!=0);
    }
    public static boolean passiveActive(LivingEntity entity) {
        if(!(entity instanceof ServerPlayer player)||!isWildfire(player))return false;
        State s=state(player);
        if (ElectronicInterferenceManager.isElectronicSkill(ModCharacters.WILDFIRE,SkillSlot.PASSIVE)) {
            if (player.hasEffect(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE)
                    ||player.hasEffect(ModEffects.HACKCLAW_INTERFERENCE)
                    ||player.hasEffect(ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE)
                    ||ElectronicInterferenceManager.blockingField(player,ModCharacters.WILDFIRE,SkillSlot.PASSIVE)!=null) return false;
            double radius=Math.max(0,com.rzy.dealt_force_skills.config.DealtBossesConfig.doubleValue("accelerated_troops.interference_radius",20));
            if(radius>0&&!player.serverLevel().getEntitiesOfClass(com.rzy.dealt_force_skills.entity.AcceleratedTroopsEntity.class,
                    player.getBoundingBox().inflate(radius),e->e.isAlive()&&e.distanceToSqr(player)<=radius*radius).isEmpty())return false;
        }
        return player.getHealth()<player.getMaxHealth()*WildfireConfig.fraction("passive.health_threshold",.45)
                ||player.level().getGameTime()<s.passiveUntil;
    }
    public static double reloadMultiplier(LivingEntity entity) {
        boolean active=entity.level().isClientSide?entity.getPersistentData().getBoolean("dfs.wildfire.passive"):passiveActive(entity);
        return active?1+WildfireConfig.number("passive.reload_bonus",.5):1;
    }
    public static void onDamaged(LivingEntity entity) {
        if(entity instanceof ServerPlayer p&&isWildfire(p)) {
            state(p).passiveUntil=p.level().getGameTime()+WildfireConfig.ticks("passive.duration_ticks",60);
            updateAttributes(p,state(p));sync(p);
        }
    }
    public static float defend(ServerPlayer player,DamageSource source,float amount) {
        State s=STATES.get(player);if(s==null||s.phase<2)return amount;
        // Environmental damage is unaffected; known physical impacts use the existing hit estimator.
        if(source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                ||source.is(net.minecraft.world.damagesource.DamageTypes.FALL)||source.getEntity()==player)return amount;
        Double ratio=com.rzy.dealt_force_skills.event.CommonEvents.estimateEquipmentHitHeightRatio(player,source);
        if(ratio!=null&&ratio>=WildfireConfig.fraction("core.upper_body_min_ratio",.62))return 0;
        Vec3 origin=source.getSourcePosition();if(origin==null&&source.getEntity()!=null)origin=source.getEntity().position();
        if(origin!=null&&inFront(player,origin))return amount*(float)(1-WildfireConfig.fraction("core.front_damage_reduction",.75));
        return amount;
    }
    public static boolean inFront(Player player,Vec3 point) {
        Vec3 delta=point.subtract(player.position()).multiply(1,0,1).normalize();
        Vec3 look=player.getLookAngle().multiply(1,0,1).normalize();
        return delta.dot(look)>=Math.cos(Math.toRadians(Math.min(180,WildfireConfig.number("core.front_half_angle_degrees",90))));
    }
    public static double speedBonus(int age) {
        double start=WildfireConfig.number("core.initial_speed_bonus",.1),max=WildfireConfig.number("core.max_speed_bonus",2.5);
        return start+(max-start)*Math.min(1,Math.max(0,(double)age/WildfireConfig.ticks("core.acceleration_ticks",200)));
    }
    private static double forwardSpeed(ServerPlayer player,int age) {
        return WildfireConfig.number("core.base_forward_speed",.28)*(1+speedBonus(age))
                *(passiveActive(player)?1+WildfireConfig.number("passive.movement_bonus",.25):1);
    }
    /** Discrete vanilla gravity/drag simulation so the default leap reaches three blocks. */
    public static double leapVelocity(double height) {
        double low=0,high=20;
        for(int i=0;i<40;i++){double v=(low+high)/2,y=0,top=0;for(int t=0;t<500&&v>0;t++){y+=v;top=Math.max(top,y);v=(v-.08)*.98;}
            if(top<height)low=(low+high)/2;else high=(low+high)/2;}
        return (low+high)/2;
    }
    public static void tick(ServerPlayer player) {
        if(!isWildfire(player)||!player.isAlive()||player.isSpectator()) {clear(player);return;}
        State s=state(player);
        if(s.level!=player.level()){endCore(player,s);s.tool=0;s.level=player.level();}
        s.smoke=Math.min(s.smoke,WildfireConfig.maxCharges(true));s.grenade=Math.min(s.grenade,WildfireConfig.maxCharges(false));
        if(s.smoke<WildfireConfig.maxCharges(true)&&--s.smokeRecharge<=0){s.smoke++;s.smokeRecharge=s.smoke<WildfireConfig.maxCharges(true)?WildfireConfig.cooldown(true):0;}
        if(s.grenade<WildfireConfig.maxCharges(false)&&--s.grenadeRecharge<=0){s.grenade++;s.grenadeRecharge=s.grenade<WildfireConfig.maxCharges(false)?WildfireConfig.cooldown(false):0;}
        if(s.phase==0&&s.cooldown>0)s.cooldown--;
        if(s.phase!=0&&(player.hasEffect(ModEffects.STUN)||player.isPassenger()))endCore(player,s);
        if(s.phase==1&&--s.prepare<=0){s.phase=2;s.age=0;s.previous=player.position();}
        if(s.phase>=2) {
            player.stopUsingItem();s.age++;
            Vec3 forward=player.getLookAngle().multiply(1,0,1).normalize();
            double speed=forwardSpeed(player,s.age);
            Vec3 motion=forward.scale(speed);player.setDeltaMovement(motion.x,player.getDeltaMovement().y,motion.z);player.hurtMarked=true;
            if(s.phase==3){s.leapAge++;player.fallDistance=0;if(s.leapAge>2&&player.onGround()){impact(player,false);endCore(player,s);}}
            else {
                AABB next=player.getBoundingBox().move(motion.x,0,motion.z);
                boolean wall=!player.level().noCollision(player,next)
                        &&!player.level().noCollision(player,next.move(0,WildfireConfig.number("core.step_height_bonus",1)+.6,0));
                boolean hostile=!player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().expandTowards(forward.scale(.8)).inflate(.12),
                        target->target!=player&&target.isAlive()&&TargetingUtil.isHostileLivingFor(player,target)
                                &&inFront(player,target.position())).isEmpty();
                if(hostile){impact(player,true);endCore(player,s);}
                boolean stuck=wall&&player.position().distanceToSqr(s.previous)<.01;
                s.blocked=stuck?s.blocked+1:0;
                if(s.phase==2&&s.blocked>=WildfireConfig.ticks("core.blocked_ticks",3)){impact(player,true);endCore(player,s);}
            }
        }
        s.previous=player.position();updateAttributes(player,s);
        player.getPersistentData().put(DATA,snapshot(player));
        if(player.tickCount%2==0)sync(player);
    }
    private static void updateAttributes(ServerPlayer p,State s) {
        double move=(passiveActive(p)?WildfireConfig.number("passive.movement_bonus",.25):0);
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED),SPEED,move,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p.getAttribute(Attributes.STEP_HEIGHT),STEP,s.phase>=2?WildfireConfig.number("core.step_height_bonus",1):0,AttributeModifier.Operation.ADD_VALUE);
    }
    private static void modifier(AttributeInstance attribute,ResourceLocation id,double value,AttributeModifier.Operation operation) {
        if(attribute==null)return;var old=attribute.getModifier(id);
        if(old!=null&&old.amount()==value)return;
        if(old!=null)attribute.removeModifier(id);
        if(value!=0)attribute.addTransientModifier(new AttributeModifier(id,value,operation));
    }
    private static void endCore(ServerPlayer player,State s) {boolean active=s.phase!=0;s.phase=0;s.prepare=0;s.age=0;s.blocked=0;updateAttributes(player,s);if(active)sync(player);}
    public static void clear(ServerPlayer player) {
        State s=STATES.get(player);
        for(var level:player.server.getAllLevels())for(var entity:level.getAllEntities())
            if(entity instanceof WildfireDeviceEntity device&&player.getUUID().equals(device.ownerId()))device.discard();
        if(s==null)return;
        com.mojang.logging.LogUtils.getLogger().debug("Wildfire exit player={} phase={} tool={} packet=S2C_WildfireState(empty)",player.getUUID(),s.phase,s.tool);
        endCore(player,s);s.tool=0;player.getPersistentData().put(DATA,snapshot(player));
        CompoundTag clear=new CompoundTag();NetworkHandler.sendToTrackingAndSelf(new S2C_WildfireState(player.getId(),clear),player);
        NetworkHandler.sendToTrackingAndSelf(new S2C_SyncHeldToolVisual(player.getId(),0),player);
        modifier(player.getAttribute(Attributes.MOVEMENT_SPEED),SPEED,0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        STATES.remove(player);
    }
    public static void impact(ServerPlayer player,boolean frontOnly) {
        var level=player.serverLevel();Vec3 center=player.position();double radius=Math.min(64,WildfireConfig.number("core.impact_radius",4));
        DamageSource source=new WildfireDamageSource(level,player,player,true);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(radius))) {
            if(target==player||!target.isAlive()||!TargetingUtil.isHostileLivingFor(player,target)
                    ||target.position().distanceTo(center)>radius||frontOnly&&!inFront(player,target.position()))continue;
            target.invulnerableTime=0;target.hurt(source,(float)WildfireConfig.number("core.impact_damage",10));
            int ticks=WildfireConfig.ticks("core.silence_ticks",100);
            target.addEffect(new MobEffectInstance(ModEffects.STUN,ticks,0,false,true),player);
            // Match Sineva shield bash even when an equal/longer silence already exists.
            if(target.isAlive()) {
                SinevaKnockdownState.apply(player,target,ticks);
                if(target.hasEffect(ModEffects.STUN))onDamaged(player);
            }
        }
        int r=(int)Math.ceil(radius);
        for(BlockPos pos:BlockPos.betweenClosed(player.blockPosition().offset(-r,-r,-r),player.blockPosition().offset(r,r,r))) {
            if(Vec3.atCenterOf(pos).distanceTo(center)>radius||frontOnly&&!inFront(player,Vec3.atCenterOf(pos)))continue;
            var block=level.getBlockState(pos);
            if((block.getBlock() instanceof DoorBlock||block.getBlock() instanceof TrapDoorBlock)
                    &&block.hasProperty(BlockStateProperties.OPEN)&&!block.getValue(BlockStateProperties.OPEN))
                level.setBlock(pos,block.setValue(BlockStateProperties.OPEN,true),3);
        }
        SuperbWarfareCompat.damageVehicles(level,center,radius,SkillDamageHelper.vyronMagneticBomb(level,player,player),player,1F,false,
                vehicle->!frontOnly||inFront(player,vehicle.position()));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,center.x,center.y+.4,center.z,8,radius*.4,.3,radius*.4,0);
        sound(player,frontOnly?ModSounds.WILDFIRE_CORE_EXPLODE.get():ModSounds.WILDFIRE_CORE_LAND.get(),1);
    }
    private static void sound(ServerPlayer p,SoundEvent sound,float pitch) {p.level().playSound(null,p.blockPosition(),sound,SoundSource.PLAYERS,.85F,pitch);}
    private static void cooldown(ServerPlayer player) {SkillCooldownHelper.notifyCooldown(player,Component.translatable("message.dealt_force_skills.wildfire.cooldown"));}
}
