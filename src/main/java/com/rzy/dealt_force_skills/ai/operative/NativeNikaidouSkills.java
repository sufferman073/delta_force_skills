package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/** Native base Hiro: physical weapon geometry and the committed core's real cost. */
public final class NativeNikaidouSkills implements OperativeSkills {
    private int correctionCooldown, correctionTicks, coreCooldown, coreAge, decayClock;
    private int doomedTicks, riftStacks, riftTicks, attackCooldown, strikeTicks, sequence;
    private boolean hotIron, core, spawnedSkeleton, finishing;
    private UUID strikeTarget;

    private static int integer(String key, int fallback) {
        return DealtForceConfig.intValue("characters.nikaidou.nikaidou_hiro_state_manager." + key, fallback);
    }
    private static double decimal(String key, double fallback) {
        return DealtForceConfig.doubleValue("characters.nikaidou.nikaidou_hiro_state_manager." + key, fallback);
    }
    private static boolean available(OperativeHost host) { return NativeRaptorSkills.available(host); }
    private static boolean valid(OperativeHost host, LivingEntity target, double range) {
        return target != null && target.level() == host.body().level() && target.isAlive() && host.isEnemy(target) && host.withinCombatRange(target.position())
                && host.body().distanceToSqr(target) <= range * range && host.canSee(target);
    }
    private boolean blocked(OperativeHost host, SkillSlot slot) {
        return !available(host) || OperativeElectronics.blocked(host, ModCharacters.NIKAIDOU_HIRO_ID, slot);
    }
    private boolean wantsCore(OperativeHost host, LivingEntity target) {
        if (core || doomedTicks > 0 || coreCooldown > 0 || !valid(host, target, 8)) return false;
        return host.body().getHealth() <= host.body().getMaxHealth() * .55F
                || host.serverLevel().getEntitiesOfClass(LivingEntity.class, host.body().getBoundingBox().inflate(5), e -> valid(host,e,5)).size() >= 3;
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (blocked(host,slot) || busy()) return 0;
        return switch(slot) {
            case ACTIVE_1 -> correctionCooldown == 0 && correctionTicks == 0 && valid(host,target,16) ? 72 : 0;
            case ACTIVE_2 -> !core && doomedTicks == 0 && (hotIron ? wantsCore(host,target) : valid(host,target,8)) ? 78 : 0;
            case CORE -> !hotIron && wantsCore(host,target) ? 100 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (score(host,slot,target) <= 0) return false;
        switch(slot) {
            case ACTIVE_1 -> {
                correctionCooldown = Math.max(1,NikaidouHiroStateManager.CORRECTION_COOLDOWN_TICKS);
                correctionTicks = Math.max(1,NikaidouHiroStateManager.CORRECTION_DURATION_TICKS);
                host.body().addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_CORRECTION,correctionTicks,integer("effect.nikaidou_correction.0.amplifier",0)));
                sound(host,ModSounds.NIKAIDOU_CORRECTION_START.get());
            }
            case ACTIVE_2 -> {hotIron = !hotIron;sound(host,ModSounds.NIKAIDOU_HOT_IRON_TOGGLE.get());}
            case CORE -> {
                core = true;coreAge = decayClock = 0;spawnedSkeleton = false;
                coreCooldown = Math.max(1,NikaidouHiroStateManager.CORE_COOLDOWN_TICKS);
                host.body().setHealth(host.body().getMaxHealth());syncCore(host);
                sound(host,ModSounds.NIKAIDOU_CORE_START.get());
            }
            default -> {return false;}
        }
        return true;
    }
    public boolean weaponActive() {return hotIron || core || doomedTicks > 0;}
    public boolean attack(OperativeHost host, LivingEntity target) {
        double range = decimal(core || doomedTicks > 0 ? "ritual_sword_attack_range" : "hot_iron_attack_range",3);
        if (!weaponActive() || busy() || attackCooldown > 0 || blocked(host,SkillSlot.ACTIVE_2) || !valid(host,target,range+1)) return false;
        host.aimAt(target);strikeTarget = target.getUUID();
        boolean sword = core || doomedTicks > 0;
        SkillModelVisual visual = sword ? switch(sequence % 3) {
            case 1 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_RIGHT_TO_LEFT;
            case 2 -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD_DIAGONAL;
            default -> SkillModelVisual.NIKAIDOU_RITUAL_SWORD;
        } : (sequence % 2 == 0 ? SkillModelVisual.NIKAIDOU_HOT_IRON : SkillModelVisual.NIKAIDOU_HOT_IRON_OVERHEAD);
        sequence = (sequence+1)%6;
        strikeTicks = Math.max(1,visual.impactTick());
        if (host instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost)
            com.rzy.dealt_force_skills.network.NetworkHandler.sendToTrackingAndSelf(
                    new com.rzy.dealt_force_skills.network.S2C_SkillModelVisual(host.body().getId(), visual.ordinal(), visual.durationTicks()), host.body());
        attackCooldown = Math.max(1,integer(sword ? "ritual_sword_attack_cooldown_ticks" : "hot_iron_attack_cooldown_ticks",sword?13:23));
        host.body().swing(InteractionHand.MAIN_HAND);return true;
    }
    @Override public double meleeIntervalTicks(OperativeHost host) {
        return weaponActive() ? Math.max(1,integer(core || doomedTicks>0 ? "ritual_sword_attack_cooldown_ticks" : "hot_iron_attack_cooldown_ticks",core || doomedTicks>0 ? 13 : 23)) : 20;
    }
    private void strike(OperativeHost host) {
        LivingEntity selected = strikeTarget == null ? null : host.serverLevel().getEntity(strikeTarget) instanceof LivingEntity living ? living : null;
        strikeTarget = null;
        boolean sword = core || doomedTicks > 0;
        double range = decimal(sword ? "ritual_sword_attack_range" : "hot_iron_attack_range",3);
        if (!weaponActive() || blocked(host,SkillSlot.ACTIVE_2) || !valid(host,selected,range+1)) return;
        Vec3 eye = host.body().getEyePosition(), look = host.body().getLookAngle().normalize();
        if (!sword) {
            double radius = DealtForceConfig.doubleValue("characters.nikaidou_hiro.melee_ray_inflate",.28);
            Vec3 end = eye.add(look.scale(range));
            var hit = host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().expandTowards(look.scale(range)).inflate(radius), e -> valid(host,e,range+1))
                    .stream().filter(e -> e.getBoundingBox().inflate(radius).clip(eye,end).isPresent())
                    .min(Comparator.comparingDouble(e -> eye.distanceToSqr(e.getBoundingBox().inflate(radius).clip(eye,end).orElse(e.position())))).orElse(null);
            if (hit != null) hit(host,hit,15,false);
        } else {
            double inflate = DealtForceConfig.doubleValue("characters.nikaidou_hiro.sweep_hitbox_inflate",.35);
            Vec3 flat = new Vec3(look.x,0,look.z).normalize(), end = eye.add(look.scale(range+inflate));
            for (var target : host.serverLevel().getEntitiesOfClass(LivingEntity.class,host.body().getBoundingBox().inflate(range+1.25,1.75,range+1.25), e -> valid(host,e,range+2))) {
                var direct = target.getBoundingBox().inflate(inflate).clip(eye,end);
                Vec3 reference = direct.orElse(target.getBoundingBox().getCenter()), offset = reference.subtract(eye), horizontal = new Vec3(offset.x,0,offset.z);
                double distance = horizontal.length();
                if (distance <= .05 || distance > range+target.getBbWidth()*.5+inflate || direct.isEmpty() && horizontal.normalize().dot(flat)<Math.cos(Math.toRadians(55))
                        || reference.y < host.body().getY()-.65 || reference.y > host.body().getY()+host.body().getBbHeight()+.85) continue;
                hit(host,target,45,true);
            }
        }
    }
    private static void hit(OperativeHost host, LivingEntity target, float damage, boolean sword) {
        if (SkillDamageHelper.hurt(target,SkillDamageHelper.nikaidouWeapon(host.serverLevel(),host.body(),host.body()),host.body(),damage)) {
            host.serverLevel().sendParticles(sword ? net.minecraft.core.particles.ParticleTypes.CRIT : net.minecraft.core.particles.ParticleTypes.FLAME,
                    target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),8,.2,.3,.2,.01);
            host.serverLevel().playSound(null,target.blockPosition(),sword?ModSounds.NIKAIDOU_RITUAL_SWORD_HIT.get():ModSounds.NIKAIDOU_HOT_IRON_HIT.get(),SoundSource.NEUTRAL,.85F,1);
        }
    }
    @Override public void tick(OperativeHost host) {
        if (correctionCooldown>0)correctionCooldown--;if(coreCooldown>0)coreCooldown--;if(attackCooldown>0)attackCooldown--;
        if (riftTicks>0 && --riftTicks==0){riftStacks=0;host.body().removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);}
        if (correctionTicks>0 && --correctionTicks==0)host.body().removeEffect(ModEffects.NIKAIDOU_CORRECTION);
        if (!available(host)) cancel(host);
        if (doomedTicks>0) {
            host.body().clearFire();host.body().setHealth(Math.max(1,host.body().getHealth()));
            if (--doomedTicks==0) {finishDoomed(host);return;}
            if (!host.body().hasEffect(ModEffects.NIKAIDOU_DOOMED))host.body().addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_DOOMED,doomedTicks,integer("effect.nikaidou_doomed.4.amplifier",0)));
        } else if (core) {
            coreAge = Math.min(72000,coreAge+1);if(!host.parallelNativeAi()){host.body().stopUsingItem();host.body().setSprinting(false);}syncCore(host);
            if (++decayClock >= Math.max(1,integer("core_decay_interval_ticks",5))) {
                double rampSeconds = Math.max(.05,decimal("core_decay_ramp_seconds",10)), max = Math.max(0,decimal("core_decay_max_per_second",.15));
                double ratio = Math.min(max,decimal("core_decay_initial_per_second",.001)+Math.min(1,coreAge/20D/rampSeconds)*(max-decimal("core_decay_initial_per_second",.001)));
                float damage = (float)(host.body().getMaxHealth()*Math.max(0,ratio)*decayClock/20D);decayClock=0;
                if (host.body().getHealth()-damage<=.01F)startDoomed(host);else host.body().setHealth(host.body().getHealth()-damage);
            }
        }
        if (!host.body().isAlive() || !available(host))return;
        if (strikeTicks>0) {if (--strikeTicks==0)strike(host);}
        else if (weaponActive() && host.body() instanceof Mob mob && mob.getTarget()!=null)attack(host,mob.getTarget());
    }
    private void syncCore(OperativeHost host) {
        if (!host.body().hasEffect(ModEffects.NIKAIDOU_CORE))host.body().addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_CORE,72000,integer("effect.nikaidou_core.1.amplifier",0)));
    }
    private void startDoomed(OperativeHost host) {
        if(doomedTicks>0 || finishing)return;
        core=false;hotIron=false;doomedTicks=Math.max(1,NikaidouHiroStateManager.DOOMED_TICKS);
        host.body().setHealth(Math.max(1,host.body().getHealth()));host.body().removeEffect(ModEffects.NIKAIDOU_CORE);
        host.body().addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_DOOMED,doomedTicks,integer("effect.nikaidou_doomed.4.amplifier",0)));
        sound(host,ModSounds.NIKAIDOU_DOOM_TRIGGER.get());
    }
    public float incoming(OperativeHost host, DamageSource source, float amount) {
        if (finishing || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || amount<=0)return amount;
        if (doomedTicks>0)return 0;
        float adjusted = source.is(SkillDamageHelper.NIKAIDOU_DECAY) ? amount : amount*(core?.1F:hotIron?.4F:1);
        if(core && adjusted>=host.body().getHealth()){startDoomed(host);return 0;}
        return adjusted;
    }
    public void onKill(OperativeHost host, LivingEntity victim) {
        var allegiance = new OperativeAllegiance();allegiance.capture(host.body());
        if (victim == host.body() || allegiance.isAlly(host.body(),victim) || finishing)return;
        riftStacks=Math.min(Math.max(0,NikaidouHiroStateManager.MAX_RIFT_STACKS),riftStacks+1);
        riftTicks=Math.max(1,NikaidouHiroStateManager.RIFT_DURATION_TICKS);correctionCooldown=0;
        if(riftStacks>0)host.body().addEffect(new MobEffectInstance(ModEffects.NIKAIDOU_RIFT_STACKS,riftTicks,Math.max(0,riftStacks*Math.max(1,integer("rift_effect_levels_per_stack",2))-1)));
        sound(host,ModSounds.NIKAIDOU_RIFT_REFRESH.get());
    }
    private void finishDoomed(OperativeHost host) {
        spawnSkeleton(host);finishing=true;clearEffects(host);
        // One real terminal damage attempt; event cancellation is respected.
        host.body().hurt(host.body().damageSources().genericKill(),Float.MAX_VALUE);
    }
    public void onDeath(OperativeHost host) {if(core || doomedTicks>0 || finishing)spawnSkeleton(host);}
    private void spawnSkeleton(OperativeHost host) {
        if(spawnedSkeleton)return;spawnedSkeleton=true;
        var skeleton=EntityType.WITHER_SKELETON.create(host.serverLevel());if(skeleton==null)return;
        skeleton.moveTo(host.body().getX(),host.body().getY(),host.body().getZ(),host.body().getYRot(),0);
        var health=skeleton.getAttribute(Attributes.MAX_HEALTH);if(health!=null)health.setBaseValue(1000);skeleton.setHealth(skeleton.getMaxHealth());
        skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,integer("effect.damage_resistance.5.duration_ticks",72000),integer("effect.damage_resistance.5.amplifier",2)));
        skeleton.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,integer("effect.damage_boost.6.duration_ticks",72000),integer("effect.damage_boost.6.amplifier",99)));
        skeleton.setPersistenceRequired();var allegiance=new OperativeAllegiance();allegiance.capture(host.body());NativeDepartmentSkills.tagSummon(skeleton,allegiance);
        host.serverLevel().addFreshEntity(skeleton);sound(host,ModSounds.NIKAIDOU_DOOM_DEATH.get());
    }
    @Override public boolean busy(){return strikeTicks>0;}
    @Override public void cancel(OperativeHost host){strikeTicks=0;strikeTarget=null;hotIron=false;correctionTicks=0;host.body().removeEffect(ModEffects.NIKAIDOU_CORRECTION);}
    public void clear(OperativeHost host){cancel(host);core=false;doomedTicks=riftTicks=riftStacks=0;clearEffects(host);}
    private static void clearEffects(OperativeHost host){host.body().removeEffect(ModEffects.NIKAIDOU_CORE);host.body().removeEffect(ModEffects.NIKAIDOU_DOOMED);host.body().removeEffect(ModEffects.NIKAIDOU_RIFT_STACKS);host.body().removeEffect(ModEffects.NIKAIDOU_CORRECTION);}
    @Override public CompoundTag save(){
        var tag=new CompoundTag();tag.putInt("CorrectionCooldown",correctionCooldown);tag.putInt("CorrectionTicks",correctionTicks);tag.putInt("CoreCooldown",coreCooldown);tag.putInt("CoreAge",coreAge);tag.putInt("DecayClock",decayClock);
        tag.putInt("DoomedTicks",doomedTicks);tag.putInt("RiftStacks",riftStacks);tag.putInt("RiftTicks",riftTicks);tag.putInt("AttackCooldown",attackCooldown);tag.putInt("StrikeTicks",strikeTicks);tag.putInt("Sequence",sequence);
        tag.putBoolean("HotIron",hotIron);tag.putBoolean("Core",core);tag.putBoolean("SpawnedSkeleton",spawnedSkeleton);tag.putBoolean("Finishing",finishing);if(strikeTarget!=null)tag.putUUID("StrikeTarget",strikeTarget);return tag;
    }
    @Override public void load(CompoundTag tag){
        correctionCooldown=bound(tag,"CorrectionCooldown",NikaidouHiroStateManager.CORRECTION_COOLDOWN_TICKS);correctionTicks=bound(tag,"CorrectionTicks",NikaidouHiroStateManager.CORRECTION_DURATION_TICKS);coreCooldown=bound(tag,"CoreCooldown",NikaidouHiroStateManager.CORE_COOLDOWN_TICKS);
        coreAge=bound(tag,"CoreAge",72000);decayClock=bound(tag,"DecayClock",Math.max(1,integer("core_decay_interval_ticks",5))-1);doomedTicks=bound(tag,"DoomedTicks",NikaidouHiroStateManager.DOOMED_TICKS);
        riftTicks=bound(tag,"RiftTicks",NikaidouHiroStateManager.RIFT_DURATION_TICKS);riftStacks=riftTicks>0?bound(tag,"RiftStacks",NikaidouHiroStateManager.MAX_RIFT_STACKS):0;
        finishing=tag.getBoolean("Finishing");core=!finishing && doomedTicks==0 && tag.getBoolean("Core");hotIron=!finishing && !core && doomedTicks==0 && tag.getBoolean("HotIron");spawnedSkeleton=tag.getBoolean("SpawnedSkeleton");
        attackCooldown=bound(tag,"AttackCooldown",Math.max(integer("hot_iron_attack_cooldown_ticks",23),integer("ritual_sword_attack_cooldown_ticks",13)));strikeTicks=bound(tag,"StrikeTicks",3);sequence=bound(tag,"Sequence",5);
        strikeTarget=tag.hasUUID("StrikeTarget")?tag.getUUID("StrikeTarget"):null;if(strikeTarget==null || !weaponActive())strikeTicks=0;
    }
    private static int bound(CompoundTag tag,String key,int max){return Math.max(0,Math.min(Math.max(0,max),tag.getInt(key)));}
    private static void sound(OperativeHost host,SoundEvent sound){host.serverLevel().playSound(null,host.body().blockPosition(),sound,SoundSource.NEUTRAL,.85F,1);}
    @Override public String describe(){return "iron="+hotIron+",core="+core+",doomed="+doomedTicks+",rift="+riftStacks+",correction="+correctionCooldown;}
    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeNikaidouSkills();
        attackCooldown = fresh.attackCooldown;
        coreCooldown = fresh.coreCooldown;
        correctionCooldown = fresh.correctionCooldown;
    }
}
