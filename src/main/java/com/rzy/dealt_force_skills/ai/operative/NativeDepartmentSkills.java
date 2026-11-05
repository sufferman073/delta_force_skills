package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.DepartmentExplosiveTrapEntity;
import com.rzy.dealt_force_skills.entity.DepartmentOverheatLaserEntity;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Native traffic-booth defenses, physical laser/traps and the staged contingency execution. */
public final class NativeDepartmentSkills implements OperativeSkills {
    public static final String SUMMON = "dealt_force_skills.native_department";
    private int laserCooldown, coreCooldown, countdown, ascend, reduction = integer("initial_reduction_percent",75), vulnerability;
    private boolean firstFatal = true, secondFatal, concealmentReady, concealing, previousInvisible, previousSilent;
    private long revealUntil;
    private Vec3 detachedCorePosition;
    private final int[] trapCooldowns = new int[Math.max(0,Math.min(16,DepartmentOfTransportationStateManager.TRAP_MAX_CHARGES))];
    private final List<UUID> victims = new ArrayList<>();
    private final Map<UUID,Integer> revealed = new HashMap<>();
    private static int integer(String key,int fallback) {return Math.max(0,DealtForceConfig.intValue("characters.department.department_of_transportation_state_manager."+key,fallback));}
    private static double decimal(String key,double fallback) {return Math.max(0,DealtForceConfig.doubleValue("characters.department.department_of_transportation_state_manager."+key,fallback));}
    private static boolean valid(OperativeHost host,LivingEntity target,double range) {
        return target!=null && target.isAlive() && target.level()==host.body().level() && host.isEnemy(target)
                && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target)<=range*range && host.canSee(target);
    }
    private static boolean blocked(OperativeHost host,SkillSlot slot) {return OperativeElectronics.blocked(host,ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID,slot);}
    public boolean wantsLaser(OperativeHost host) { return laserCooldown == 0 && !busy() && !blocked(host, SkillSlot.ACTIVE_1); }
    private int readyTrap() {for(int i=0;i<trapCooldowns.length;i++)if(trapCooldowns[i]==0)return i;return -1;}
    private static Vec3 trapPosition(OperativeHost host,LivingEntity target) {
        Vec3 direction=target.position().subtract(host.body().position()).multiply(1,0,1).normalize();
        Vec3 pos=host.body().position().add(direction.scale(Math.min(2,host.body().distanceTo(target))));
        var floor=net.minecraft.core.BlockPos.containing(pos).below();
        return host.withinOrders(pos) && host.serverLevel().getBlockState(floor).isFaceSturdy(host.serverLevel(),floor,Direction.UP)
                && host.serverLevel().noCollision(new AABB(pos.x-.2,pos.y,pos.z-.2,pos.x+.2,pos.y+.4,pos.z+.2)) ? pos:null;
    }
    private static int trapCount(OperativeHost host) {
        return host.serverLevel().getEntitiesOfClass(DepartmentExplosiveTrapEntity.class,host.body().getBoundingBox().inflate(56),e->e.isOwnedBy(host.body().getUUID())).size();
    }
    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(!NativeRaptorSkills.available(host) || busy() || blocked(host,slot))return 0;
        return switch(slot) {
            case ACTIVE_1 -> laserCooldown==0 && valid(host,target,14) ? 100:0;
            case ACTIVE_2 -> readyTrap()>=0 && valid(host,target,6) && trapCount(host)<DepartmentOfTransportationStateManager.TRAP_ACTIVE_LIMIT && trapPosition(host,target)!=null ? 90:0;
            case CORE -> coreCooldown==0 && hasExecutableEnemy(host) ? 110:0;
            case PASSIVE -> 0;
        };
    }
    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target) {
        if(score(host,slot,target)<=0)return false;
        if(target!=null)host.aimAt(target);
        switch(slot) {
            case ACTIVE_1 -> {
                Vec3 look=target.getBoundingBox().getCenter().subtract(host.body().getEyePosition()).normalize();
                var laser=new DepartmentOverheatLaserEntity(ModEntities.DEPARTMENT_OVERHEAT_LASER.get(),host.serverLevel(),host.body());
                laser.setPos(host.body().getEyePosition().add(look.scale(.55)).add(0,-.08,0));laser.setDeltaMovement(look.scale(1.8));
                if(!host.serverLevel().addFreshEntity(laser))return false;
                laserCooldown=DepartmentOfTransportationStateManager.LASER_COOLDOWN_TICKS;
                host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_LASER_SHOOT.get(),SoundSource.NEUTRAL,.85F,1);
            }
            case ACTIVE_2 -> {
                int index=readyTrap();Vec3 pos=trapPosition(host,target);if(index<0 || pos==null)return false;
                var trap=new DepartmentExplosiveTrapEntity(ModEntities.DEPARTMENT_EXPLOSIVE_TRAP.get(),host.serverLevel(),host.body(),Direction.UP,index,
                        host.serverLevel().getGameTime()+DepartmentOfTransportationStateManager.TRAP_COOLDOWN_TICKS);
                trap.setPos(pos);if(!host.serverLevel().addFreshEntity(trap))return false;
                trapCooldowns[index]=DepartmentOfTransportationStateManager.TRAP_COOLDOWN_TICKS;
            }
            case CORE -> {
                coreCooldown=DepartmentOfTransportationStateManager.CORE_COOLDOWN_TICKS;
                countdown=Math.max(1,DepartmentOfTransportationStateManager.CORE_COUNTDOWN_TICKS);ascend=0;victims.clear();
                detachedCorePosition=host.parallelNativeAi()?host.body().position():null;
                if(!host.parallelNativeAi() && host.body() instanceof Mob mob)mob.getNavigation().stop();
                for(var enemy:host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(6),host::isEnemy))
                    if(enemy.distanceToSqr(host.body())<=36)enemy.addEffect(new MobEffectInstance(ModEffects.STUN.get(),80,0),host.body());
                host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_CORE_CAST.get(),SoundSource.NEUTRAL,1,1);
                host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_CORE_COUNTDOWN.get(),SoundSource.NEUTRAL,.7F,1);
            }
            default -> {return false;}
        }
        recover();offense(host);return true;
    }
    private void recover() {
        int recovery=integer("skill_vulnerability_recovery_percent",3);
        if(vulnerability>recovery)vulnerability-=recovery;
        else if(vulnerability>0){reduction=Math.min(integer("initial_reduction_percent",75),reduction+integer("skill_reduction_recovery_percent",5)-vulnerability);vulnerability=0;}
        else reduction=Math.min(integer("initial_reduction_percent",75),reduction+integer("skill_reduction_recovery_percent",5));
    }
    public void offense(OperativeHost host) {if(concealmentReady){revealUntil=host.serverLevel().getGameTime()+integer("concealment_break_ticks",60);unconceal(host);}}
    public boolean concealed(OperativeHost host) {
        double range=decimal("concealment_proximity",1.5);
        return concealmentReady && host.serverLevel().getGameTime()>=revealUntil
                && host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range),e->e!=host.body() && e.isAlive() && e.distanceToSqr(host.body())<=range*range).isEmpty();
    }
    @Override public void tick(OperativeHost host) {
        if(laserCooldown>0)laserCooldown--;if(coreCooldown>0)coreCooldown--;
        for(int i=0;i<trapCooldowns.length;i++)if(trapCooldowns[i]>0)trapCooldowns[i]--;
        revealed.replaceAll((id,ticks)->ticks-1);revealed.values().removeIf(ticks->ticks<=0);
        if(!NativeRaptorSkills.available(host)){cancel(host);return;}
        if(revealUntil>host.serverLevel().getGameTime()+integer("concealment_break_ticks",60))revealUntil=host.serverLevel().getGameTime()+integer("concealment_break_ticks",60);
        if(concealmentReady){
            double range=decimal("concealment_proximity",1.5);
            if(!host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range),e->e!=host.body() && e.isAlive() && e.distanceToSqr(host.body())<=range*range).isEmpty())offense(host);
            if(concealed(host)){
                if(!concealing){previousInvisible=host.body().isInvisible();previousSilent=host.body().isSilent();concealing=true;}
                host.body().setInvisible(true);host.body().setSilent(true);
                host.body().addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_CONCEALMENT.get(),40,0,true,false));
                for(var mob:host.serverLevel().getEntitiesOfClass(Mob.class,host.body().getBoundingBox().inflate(32),e->e.getTarget()==host.body()))mob.setTarget(null);
            }else unconceal(host);
        }
        syncVulnerability(host);
        if(!busy())return;
        if(blocked(host,SkillSlot.CORE)){cancelCore(host);return;}
        if(!host.parallelNativeAi() && host.body() instanceof Mob mob)mob.getNavigation().stop();
        float yaw=host.body().getYRot()+(countdown>0?18:22);host.body().setYRot(yaw);host.body().setYHeadRot(yaw);host.body().yBodyRot=yaw;
        if(host.parallelNativeAi() && detachedCorePosition==null)detachedCorePosition=host.body().position();
        Vec3 corePosition=detachedCorePosition==null?host.body().position():detachedCorePosition;
        if(countdown>0){
            if(!host.parallelNativeAi())host.body().setDeltaMovement(0,Math.min(0,host.body().getDeltaMovement().y),0);
            if(--countdown==0){
                ascend=Math.max(1,DepartmentOfTransportationStateManager.CORE_ASCEND_TICKS);
                double range=decimal("core_calibration_radius",24);
                for(var enemy:host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().move(corePosition.subtract(host.body().position())).inflate(range),e->e.isAlive() && host.isEnemy(e) && host.withinCombatRange(e.position()) && calibrated(e)).stream().limit(32).toList()){
                    victims.add(enemy.getUUID());enemy.addEffect(new MobEffectInstance(ModEffects.STUN.get(),ascend+10,0),host.body());
                }
            }
            return;
        }
        if(detachedCorePosition!=null)detachedCorePosition=corePosition.add(0,.26,0);
        else{host.body().setDeltaMovement(host.body().getDeltaMovement().multiply(.2,0,.2).add(0,.26,0));host.body().hurtMarked=true;}
        victims.removeIf(id->!(host.serverLevel().getEntity(id) instanceof LivingEntity target) || !validVictim(host,target));
        for(UUID id:victims)if(host.serverLevel().getEntity(id) instanceof LivingEntity target){target.setDeltaMovement(target.getDeltaMovement().multiply(.2,0,.2).add(0,.23,0));target.hurtMarked=true;}
        if(--ascend>0)return;
        var due=List.copyOf(victims);victims.clear();
        host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_CORE_RELEASE.get(),SoundSource.NEUTRAL,1,1);
        burst(host,corePosition,host.body(),5,24,80,SkillDamageHelper.departmentCore(host.serverLevel(),host.body(),host.body()));
        for(UUID id:due)if(host.serverLevel().getEntity(id) instanceof LivingEntity target && validVictim(host,target)){
            burst(host,target.position(),target,4,18,60,SkillDamageHelper.departmentCore(host.serverLevel(),target,host.body()));
            execute(host,target);target.removeEffect(ModEffects.DEPARTMENT_CALIBRATION.get());
        }
        execute(host,host.body());
    }
    private static boolean hasExecutableEnemy(OperativeHost host) {
        double range=decimal("core_calibration_radius",24);
        return !host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range),
                e->e.isAlive() && host.isEnemy(e) && host.withinCombatRange(e.position()) && calibrated(e)).isEmpty();
    }
    private static boolean calibrated(LivingEntity target) {var effect=target.getEffect(ModEffects.DEPARTMENT_CALIBRATION.get());return effect!=null && effect.getAmplifier()>=integer("core_execution_min_amplifier",2);}
    private static boolean validVictim(OperativeHost host,LivingEntity target) {return target.isAlive() && target.level()==host.body().level() && host.isEnemy(target);}
    private static void execute(OperativeHost host,LivingEntity target) {
        DepartmentOfTransportationStateManager.applyCoreExecution(host.serverLevel(),target,host.body(),target!=host.body());
    }
    private static void burst(OperativeHost host,Vec3 center,Entity skip,double radius,float damage,int stun,DamageSource source) {
        host.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,center.x,center.y,center.z,1,0,0,0,0);
        for(var target:host.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius),e->e!=skip && host.isEnemy(e))){
            if(target.getBoundingBox().getCenter().distanceToSqr(center)>radius*radius || !host.withinCombatRange(target.position()))continue;
            target.invulnerableTime=0;SkillDamageHelper.hurt(target,source,host.body(),damage);
            target.setDeltaMovement(target.getDeltaMovement().add(target.position().subtract(center).normalize().scale(.45)));target.hurtMarked=true;
            target.addEffect(new MobEffectInstance(ModEffects.STUN.get(),stun,0),host.body());
        }
    }
    public float incoming(OperativeHost host,DamageSource source,float amount) {
        if(amount<=0 || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(concealed(host))return 0;
        float adjusted=amount*(reduction>0?Math.max(0,1-reduction*(float)decimal("passive_percent_multiplier",.01)):1+vulnerability*(float)decimal("passive_percent_multiplier",.01));
        if(reduction>0)reduction--;else vulnerability=Math.min(integer("max_vulnerability_percent",90),vulnerability+1);
        if(adjusted>=host.body().getHealth()){
            if(firstFatal){
                firstFatal=false;secondFatal=true;host.body().setHealth(host.body().getMaxHealth());
                burst(host,host.body().position(),host.body(),5,12,60,SkillDamageHelper.departmentPassiveBlast(host.serverLevel(),host.body(),host.body()));
                host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_PASSIVE_FIRST_FATAL.get(),SoundSource.NEUTRAL,1,1);return 0;
            }
            if(secondFatal){secondFatal=false;concealmentReady=true;host.body().setHealth(1);host.body().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,1));
                host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.DEPARTMENT_PASSIVE_SECOND_FATAL.get(),SoundSource.NEUTRAL,.95F,1);return 0;}
        }
        return adjusted;
    }
    private void syncVulnerability(OperativeHost host) {
        var effect=host.body().getEffect(ModEffects.DEPARTMENT_VULNERABLE.get());
        if(vulnerability==0){if(effect!=null)host.body().removeEffect(ModEffects.DEPARTMENT_VULNERABLE.get());}
        else if(effect==null || effect.getAmplifier()!=vulnerability-1)host.body().addEffect(new MobEffectInstance(ModEffects.DEPARTMENT_VULNERABLE.get(),72000,vulnerability-1));
    }
    private void unconceal(OperativeHost host) {
        if(concealing){host.body().setInvisible(previousInvisible);host.body().setSilent(previousSilent);host.body().removeEffect(ModEffects.DEPARTMENT_CONCEALMENT.get());concealing=false;}
    }
    private void cancelCore(OperativeHost host) {
        if(busy() && !host.parallelNativeAi())host.body().setDeltaMovement(host.body().getDeltaMovement().multiply(1,0,1));
        for(UUID id:victims)if(host.serverLevel().getEntity(id) instanceof LivingEntity target)target.setDeltaMovement(target.getDeltaMovement().multiply(1,0,1));
        countdown=ascend=0;victims.clear();detachedCorePosition=null;
    }
    @Override public boolean busy(){return countdown>0 || ascend>0;}
    @Override public void cancel(OperativeHost host){cancelCore(host);unconceal(host);host.body().removeEffect(ModEffects.DEPARTMENT_VULNERABLE.get());}
    @Override public void remember(LivingEntity target,int ticks){if(ticks>0 && revealed.size()<128)revealed.merge(target.getUUID(),ticks,Math::max);}
    @Override public boolean isRevealed(LivingEntity target){return target!=null && revealed.getOrDefault(target.getUUID(),0)>0;}
    @Override public CompoundTag save(){
        var tag=new CompoundTag();
        if(busy() && detachedCorePosition!=null){tag.putDouble("DetachedCoreX",detachedCorePosition.x);tag.putDouble("DetachedCoreY",detachedCorePosition.y);tag.putDouble("DetachedCoreZ",detachedCorePosition.z);}
        tag.putInt("LaserCooldown",laserCooldown);tag.putInt("CoreCooldown",coreCooldown);tag.putInt("Countdown",countdown);tag.putInt("Ascend",ascend);
        tag.putInt("Reduction",reduction);tag.putInt("Vulnerability",vulnerability);tag.putBoolean("FirstFatal",firstFatal);tag.putBoolean("SecondFatal",secondFatal);tag.putBoolean("ConcealmentReady",concealmentReady);
        tag.putLong("RevealUntil",revealUntil);tag.putBoolean("Concealing",concealing);tag.putBoolean("PreviousInvisible",previousInvisible);tag.putBoolean("PreviousSilent",previousSilent);
        tag.putIntArray("TrapCooldowns",trapCooldowns);var list=new ListTag();for(UUID id:victims){var entry=new CompoundTag();entry.putUUID("Target",id);list.add(entry);}tag.put("Victims",list);return tag;
    }
    @Override public void load(CompoundTag tag){
        laserCooldown=bound(tag,"LaserCooldown",DepartmentOfTransportationStateManager.LASER_COOLDOWN_TICKS);coreCooldown=bound(tag,"CoreCooldown",DepartmentOfTransportationStateManager.CORE_COOLDOWN_TICKS);
        countdown=bound(tag,"Countdown",DepartmentOfTransportationStateManager.CORE_COUNTDOWN_TICKS);ascend=countdown>0?0:bound(tag,"Ascend",DepartmentOfTransportationStateManager.CORE_ASCEND_TICKS);
        reduction=tag.contains("Reduction")?bound(tag,"Reduction",integer("initial_reduction_percent",75)):integer("initial_reduction_percent",75);vulnerability=reduction>0?0:bound(tag,"Vulnerability",integer("max_vulnerability_percent",90));
        firstFatal=!tag.contains("FirstFatal") || tag.getBoolean("FirstFatal");secondFatal=!firstFatal && tag.getBoolean("SecondFatal");concealmentReady=!firstFatal && !secondFatal && tag.getBoolean("ConcealmentReady");
        revealUntil=Math.max(0,tag.getLong("RevealUntil"));concealing=tag.getBoolean("Concealing");previousInvisible=tag.getBoolean("PreviousInvisible");previousSilent=tag.getBoolean("PreviousSilent");
        var cooldowns=tag.getIntArray("TrapCooldowns");for(int i=0;i<trapCooldowns.length;i++)trapCooldowns[i]=i<cooldowns.length?Math.max(0,Math.min(DepartmentOfTransportationStateManager.TRAP_COOLDOWN_TICKS,cooldowns[i])):0;
        victims.clear();if(ascend>0){var list=tag.getList("Victims",10);for(int i=0;i<Math.min(32,list.size());i++)if(list.getCompound(i).hasUUID("Target")){UUID id=list.getCompound(i).getUUID("Target");if(!victims.contains(id))victims.add(id);}}
        revealed.clear();detachedCorePosition=null;
        if(busy() && tag.contains("DetachedCoreX",6) && tag.contains("DetachedCoreY",6) && tag.contains("DetachedCoreZ",6)){
            double x=tag.getDouble("DetachedCoreX"),y=tag.getDouble("DetachedCoreY"),z=tag.getDouble("DetachedCoreZ");
            if(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z))detachedCorePosition=new Vec3(x,y,z);
        }
    }
    private static int bound(CompoundTag tag,String key,int max){return Math.max(0,Math.min(max,tag.getInt(key)));}
    @Override public String describe(){return "laser="+laserCooldown+",core="+countdown+"/"+ascend+",reduction="+reduction+",vulnerability="+vulnerability;}

    public static OperativeAllegiance summonAllegiance(Entity entity){var result=new OperativeAllegiance();if(entity!=null)result.load(entity.getPersistentData().getCompound(SUMMON),1200);return result;}
    public static boolean nativeSummon(Entity entity){return entity!=null && entity.getPersistentData().getCompound(SUMMON).getBoolean("OperativeSource");}
    public static boolean summonAllied(Entity source,LivingEntity target){return nativeSummon(source) && source.level() instanceof ServerLevel level && summonAllegiance(source).isAlly(level,target);}
    public static void spawnCreeper(ServerLevel level,Vec3 pos,OperativeAllegiance allegiance){
        var creeper=EntityType.CREEPER.create(level);if(creeper==null)return;
        creeper.setPos(pos);creeper.setPersistenceRequired();creeper.getPersistentData().putBoolean(DepartmentOfTransportationStateManager.CHARGED_CREEPER_TAG,true);tagSummon(creeper,allegiance);
        var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt!=null){bolt.setVisualOnly(true);creeper.thunderHit(level,bolt);}
        level.addFreshEntity(creeper);
    }
    public static void tagSummon(Mob mob,OperativeAllegiance allegiance){
        var tag=new CompoundTag();allegiance.save(tag,0);tag.putLong("Expires",mob.level().getGameTime()+1200);mob.getPersistentData().put(SUMMON,tag);
        if(allegiance.caster()!=null)mob.getPersistentData().putUUID(DepartmentOfTransportationStateManager.SUMMON_OWNER,allegiance.caster());
        for(var slot:EquipmentSlot.values())mob.setDropChance(slot,0);
    }
    public static void spawnBaby(ServerLevel level,Entity creeper){
        var zombie=EntityType.ZOMBIE.create(level);if(zombie==null)return;
        zombie.setBaby(true);zombie.setPersistenceRequired();zombie.setPos(creeper.position());
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));zombie.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
        zombie.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));zombie.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));zombie.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        tagSummon(zombie,summonAllegiance(creeper));var speed=zombie.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.setBaseValue(speed.getBaseValue()*1.25);
        level.addFreshEntity(zombie);level.playSound(null,zombie.blockPosition(),ModSounds.DEPARTMENT_BABY_ZOMBIE_EJECT.get(),SoundSource.HOSTILE,.9F,1.05F);
    }
    public static void tickSummon(Mob mob){
        if(!(mob.level() instanceof ServerLevel level) || !nativeSummon(mob))return;
        var tag=mob.getPersistentData().getCompound(SUMMON);var allegiance=summonAllegiance(mob);
        int maxLife=tag.getBoolean("LexBeast")?1500:1200;
        if(tag.getLong("Expires")>level.getGameTime()+maxLife)tag.putLong("Expires",level.getGameTime()+maxLife);
        if(tag.getLong("Expires")<=level.getGameTime()){mob.discard();return;}
        if(tag.getBoolean("LexBeast") || tag.getBoolean("LexSummon")){
            var root=allegiance.caster()==null?null:level.getEntity(allegiance.caster());
            if(root!=null && (!(root instanceof OperativeHost bot) || !root.isAlive() || !(bot.skills() instanceof NativeLexSkills))){mob.discard();return;}
            if(root==null){mob.setTarget(null);mob.getNavigation().stop();return;}
        }
        if(mob.getTarget()!=null && !allegiance.isEnemy(level,mob.getTarget()))mob.setTarget(null);
        if(mob.tickCount%10==0 && !mob.isNoAi())mob.setTarget(level.getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(16),e->e!=mob && !summonAllied(e,mob) && allegiance.isEnemy(level,e))
                .stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null));
        if(mob.getLastHurtByMob()!=null && allegiance.isAlly(level,mob.getLastHurtByMob()))mob.setLastHurtByMob(null);
    }

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeDepartmentSkills();
        coreCooldown = fresh.coreCooldown;
        laserCooldown = fresh.laserCooldown;
    }
}
