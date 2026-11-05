package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.undead.UndeadProfession;
import com.rzy.dealt_force_skills.character.undead.UndeadStateManager;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.skill.SkillModelVisual;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** All six professions, bounded upgrades and contextual profession switching. */
public final class NativeUndeadSkills implements OperativeSkills {
    private float energy = maxEnergy(), shield;
    private int coreCooldown, shieldTicks, parryTicks, chargeTicks, windup;
    private SkillSlot prepared;
    private UUID chargeTarget;
    private Vec3 detachedChargePosition;
    private UndeadProfession profession = UndeadProfession.KNIGHT;
    private final int[] professionCooldowns = new int[UndeadProfession.values().length];
    private final int[] actionCooldowns = new int[UndeadProfession.values().length * 3];
    private int switchCooldown, imbalance, activeCooldown, bloodlust, stealth, space, meditation, ritual, reload;
    private boolean might, scatter, splittingDamage;
    private CompoundTag records = new CompoundTag();

    private static float value(String key, float fallback) {
        float n = DealtForceConfig.floatValue("characters.undead.undead_state_manager." + key, fallback);
        return Float.isFinite(n) ? Math.max(0, n) : fallback;
    }
    private static float maxEnergy() { return value("base_max_energy", 100) + OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.INTELLIGENCE)*10; }
    private static int cooldown() { return Math.max(1, (int)Math.ceil(DealtForceConfig.intValue("characters.undead.undead_skills.cooldown.0.cooldown_ticks", 1800) * Math.max(0,1-OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.CRAFT)*.05))); }
    private static float cost(SkillSlot slot) {
        return switch(slot) {
            case ACTIVE_1 -> 20 * OperativeGrowth.energyCost();
            case ACTIVE_2 -> DealtForceConfig.floatValue("characters.undead.undead_skills.energy.1.energy_cost", 10) * OperativeGrowth.energyCost();
            case CORE -> DealtForceConfig.floatValue("characters.undead.undead_skills.energy.2.energy_cost", 60) * OperativeGrowth.energyCost();
            default -> Float.POSITIVE_INFINITY;
        };
    }
    private static boolean valid(OperativeHost host, LivingEntity target, double range) {
        return target != null && target.isAlive() && target.level() == host.body().level() && host.isEnemy(target)
                && host.withinCombatRange(target.position()) && host.canSee(target) && host.body().distanceToSqr(target) <= range * range;
    }
    private static boolean available(OperativeHost host) { return NativeRaptorSkills.available(host) && !OperativeSupport.downed(host.body()); }
    private static boolean safeStep(OperativeHost host, Vec3 step) {
        var box = host.body().getBoundingBox();
        return host.body().onGround() && host.withinOrders(host.body().position().add(step))
                && host.serverLevel().noCollision(host.body(), box.expandTowards(step).deflate(.001))
                && !host.serverLevel().noCollision(host.body(), box.move(step).move(0,-1,0));
    }
    @Override public double score(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if (imbalance > 0 || activeCooldown > 0 || slot == SkillSlot.PASSIVE || actionCooldowns[actionIndex(slot)] > 0) return 0;
        if (profession != UndeadProfession.KNIGHT) return professionScore(host, slot, target);
        if (!available(host) || busy() || OperativeElectronics.blocked(host, ModCharacters.UNDEAD_ID, slot)
                || !Float.isFinite(cost(slot))) return 0;
        return switch(slot) {
            case ACTIVE_1 -> valid(host,target,16) && host.body().distanceToSqr(target)>1
                    && safeStep(host, horizontal(host,target).scale(1.25)) ? 78 : 0;
            case ACTIVE_2 -> valid(host,target,4) && (target.getLastHurtByMob()==host.body()
                    || host.body().getHealth()<host.body().getMaxHealth()*.6F) ? 85 : 0;
            case CORE -> coreCooldown==0 && shieldTicks==0 && valid(host,target,20)
                    && host.body().getHealth()<host.body().getMaxHealth()*.65F ? 100 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost host, SkillSlot slot, LivingEntity target) {
        if(score(host,slot,target)<=0)return false;
        if (profession != UndeadProfession.KNIGHT) {
            prepared = slot; chargeTarget = target == null ? null : target.getUUID();
            windup = host.parallelNativeAi() ? 1 : 3;
            if (host.parallelNativeAi()) tickProfession(host);
            return true;
        }
        prepared=slot;chargeTarget=target.getUUID();host.aimAt(target);
        windup=switch(slot){case ACTIVE_1 -> SkillModelVisual.UNDEAD_KNIGHT_CHARGE.impactTick();
            case ACTIVE_2 -> SkillModelVisual.UNDEAD_KNIGHT_BARRIER.impactTick();
            default -> SkillModelVisual.UNDEAD_KNIGHT_WALL.impactTick();};
        if (host.parallelNativeAi()) { windup = 1; prepareKnight(host, target); }
        return true;
    }
    @Override public void tick(OperativeHost host) {
        OperativeGrowth.undead(host.body());
        for (int i=0;i<professionCooldowns.length;i++) if(professionCooldowns[i]>0)professionCooldowns[i]--;
        for (int i=0;i<actionCooldowns.length;i++) if(actionCooldowns[i]>0)actionCooldowns[i]--;
        if(switchCooldown>0)switchCooldown--;
        if(imbalance>0)imbalance--;
        if(activeCooldown>0)activeCooldown--;
        if(reload>0)reload--;
        if(coreCooldown>0)coreCooldown--;
        energy=Math.min(maxEnergy(),energy+Math.max(maxEnergy()*.05F,value("base_energy_regen_per_tick",.2F)*(1+OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.INTELLIGENCE)*.02F)));
        if(shieldTicks>0 && --shieldTicks==0)shield=0;
        if(parryTicks>0)parryTicks--;
        tickEffects(host);
        if(!available(host)){cancel(host);return;}
        if(switchCooldown==0 && !busy() && host.body() instanceof net.minecraft.world.entity.Mob mob) {
            LivingEntity enemy=mob.getTarget();
            if(enemy!=null && valid(host,enemy,32)) selectProfession(host, preferred(host,enemy));
        }
        if(profession != UndeadProfession.KNIGHT) { tickProfession(host); return; }
        LivingEntity target=chargeTarget!=null && host.serverLevel().getEntity(chargeTarget) instanceof LivingEntity living?living:null;
        if(prepared!=null){prepareKnight(host,target);if(prepared!=null)return;}
        if(parryTicks>0 && !host.parallelNativeAi())host.body().setDeltaMovement(Vec3.ZERO);
        if(chargeTicks<=0)return;
        if(!(host.parallelNativeAi() ? NativeRaptorSkills.committedTarget(host,target) : valid(host,target,20)) || !host.parallelNativeAi() && host.body().horizontalCollision
                || OperativeElectronics.blocked(host,ModCharacters.UNDEAD_ID,SkillSlot.ACTIVE_1)){cancel(host);return;}
        host.aimAt(target);
        Vec3 position=detachedChargePosition!=null?detachedChargePosition:host.body().position();
        var chargeBox=host.body().getBoundingBox().move(position.subtract(host.body().position()));
        if(chargeBox.inflate(.65).intersects(target.getBoundingBox())){
            float before=target.getHealth();
            SkillDamageHelper.hurt(target,host.body().damageSources().mobAttack(host.body()),host.body(),8.4F);
            if(target.getHealth()<before)target.addEffect(new MobEffectInstance(ModEffects.STUN,
                    DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.3.duration_ticks",30),0),host.body());
            stopCharge(host);return;
        }
        var step=target.position().subtract(position).multiply(1,0,1).normalize().scale(1.25);
        if(detachedChargePosition!=null){
            Vec3 next=position.add(step);
            if(!host.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(next))
                    || !host.serverLevel().noCollision(host.body(),chargeBox.expandTowards(step).deflate(.001))
                    || host.serverLevel().noCollision(host.body(),chargeBox.move(step).move(0,-1,0))){stopCharge(host);return;}
            detachedChargePosition=next;
        }else{
            if(!safeStep(host,step)){stopCharge(host);return;}
            host.body().setDeltaMovement(step.x,host.body().getDeltaMovement().y,step.z);host.body().hurtMarked=true;
        }
        host.serverLevel().sendParticles(ParticleTypes.END_ROD,position.x,position.y+.8,position.z,2,.2,.25,.2,0);
        if(--chargeTicks==0)stopCharge(host);
    }
    private void prepareKnight(OperativeHost host, LivingEntity target) {
        if(prepared!=null){
            if(!valid(host,target,prepared==SkillSlot.ACTIVE_2?4:20)
                    || OperativeElectronics.blocked(host,ModCharacters.UNDEAD_ID,prepared)){cancel(host);return;}
            host.aimAt(target);
            if(--windup>0)return;
            var slot=prepared;prepared=null;
            float spend=Math.max(0,cost(slot));
            if(!Float.isFinite(spend)){cancel(host);return;}
            if(slot==SkillSlot.ACTIVE_1 && !safeStep(host,horizontal(host,target).scale(1.25))){cancel(host);return;}
            energy=Math.max(0,energy-spend);
            commit(slot);
            switch(slot){
                case ACTIVE_1 -> {chargeTicks=30;detachedChargePosition=host.parallelNativeAi()?host.body().position():null;}
                case ACTIVE_2 -> {parryTicks=16;chargeTarget=null;}
                case CORE -> {shield=50;shieldTicks=400;coreCooldown=cooldown();chargeTarget=null;}
                default -> { }
            }
            host.serverLevel().playSound(null,host.body().blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.NEUTRAL,.8F,1);
        }
    }
    private static Vec3 horizontal(OperativeHost host,LivingEntity target){var d=target.position().subtract(host.body().position());return new Vec3(d.x,0,d.z).normalize();}
    private void stopCharge(OperativeHost host){chargeTicks=0;chargeTarget=null;detachedChargePosition=null;if(!host.parallelNativeAi())host.body().setDeltaMovement(0,host.body().getDeltaMovement().y,0);}
    public float incoming(OperativeHost host,DamageSource source,float amount){
        if(amount<=0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        if(splittingDamage)return amount;
        if(stealth>0 || space>0)return 0;
        if(profession==UndeadProfession.HUNTER && source.is(DamageTypes.FALL))return 0;
        if(host.body().getRandom().nextFloat()<Math.min(.95F,OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.AGILITY)*.005F))return 0;
        if(!OperativeSupport.downed(host.body()) && parryTicks>0){
            parryTicks=0;energy=Math.min(maxEnergy(),energy+15);
            if(source.getEntity() instanceof LivingEntity attacker && host.isEnemy(attacker))attacker.addEffect(new MobEffectInstance(ModEffects.STUN,
                    DealtForceConfig.intValue("characters.undead.undead_state_manager.effect.stun.2.duration_ticks",60),0),host.body());
            return 0;
        }
        if(!OperativeSupport.downed(host.body()) && shield>0 && shieldTicks>0){
            shield=Math.max(0,shield-amount);if(shield==0)shieldTicks=0;return 0;
        }
        if(profession==UndeadProfession.KNIGHT)amount=Math.min(amount*.7F,host.body().getMaxHealth()*.45F)
                *(1-.15F*(1-host.body().getHealth()/host.body().getMaxHealth()));
        if(profession==UndeadProfession.SCHOLAR)amount*=source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.WITCH_RESISTANT_TO) || source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.getMsgId().contains("magic") || source.typeHolder().unwrapKey().map(k->k.location().getNamespace().equals("dealt_force_skills")).orElse(false)?2:.5F;
        if(source.getEntity() instanceof LivingEntity attacker)amount*=Math.max(0,1-records.getInt(type(attacker))*(attacker instanceof Player?.15F:.49F));
        return amount*Math.max(0,1-OperativeGrowth.level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.ARMOR_FORGING)*.03F);
    }
    @Override public boolean locksMovement() { return imbalance>0 || chargeTicks>0 || parryTicks>0 || meditation>0; }
    @Override public boolean locksWeapon() { return prepared!=null || imbalance>0 || chargeTicks>0 || parryTicks>0 || meditation>0 || space>0; }
    @Override public boolean busy(){return prepared!=null || chargeTicks>0 || parryTicks>0 || meditation>0;}
    @Override public void cancel(OperativeHost host){prepared=null;windup=parryTicks=meditation=ritual=0;if(chargeTicks>0)stopCharge(host);chargeTarget=null;}
    @Override public CompoundTag save(){var t=new CompoundTag();t.putFloat("Energy",energy);t.putFloat("Shield",shield);t.putInt("ShieldTicks",shieldTicks);t.putInt("CoreCooldown",coreCooldown);
        t.putIntArray("ActionCooldowns",actionCooldowns);t.putInt("Ritual",ritual);t.putString("Profession",profession.name());t.putIntArray("ProfessionCooldowns",professionCooldowns);t.putInt("SwitchCooldown",switchCooldown);t.putInt("Imbalance",imbalance);t.putInt("ActiveCooldown",activeCooldown);
        t.putInt("Bloodlust",bloodlust);t.putInt("Stealth",stealth);t.putInt("Space",space);t.putInt("Reload",reload);t.putBoolean("Might",might);t.putBoolean("Scatter",scatter);t.put("Records",records.copy());return t;}
    @Override public void load(CompoundTag t){
        energy=finite(t,"Energy",maxEnergy());shield=finite(t,"Shield",50);
        shieldTicks=Math.max(0,Math.min(400,t.getInt("ShieldTicks")));if(shieldTicks==0)shield=0;
        coreCooldown=Math.max(0,Math.min(cooldown(),t.getInt("CoreCooldown")));
        prepared=null;chargeTarget=null;detachedChargePosition=null;windup=chargeTicks=parryTicks=0;
        try{profession=UndeadProfession.valueOf(t.getString("Profession"));}catch(IllegalArgumentException ignored){profession=UndeadProfession.KNIGHT;}
        int[] cds=t.getIntArray("ProfessionCooldowns");for(int i=0;i<professionCooldowns.length;i++)professionCooldowns[i]=i<cds.length?Math.max(0,Math.min(7200,cds[i])):0;
        int[] actions=t.getIntArray("ActionCooldowns");for(int i=0;i<actionCooldowns.length;i++)actionCooldowns[i]=i<actions.length?Math.max(0,Math.min(1200,actions[i])):0;
        ritual=bounded(t,"Ritual",100);switchCooldown=bounded(t,"SwitchCooldown",12);imbalance=bounded(t,"Imbalance",2);activeCooldown=bounded(t,"ActiveCooldown",6);
        bloodlust=bounded(t,"Bloodlust",300);stealth=bounded(t,"Stealth",80);space=bounded(t,"Space",600);reload=bounded(t,"Reload",16);might=t.getBoolean("Might");scatter=t.getBoolean("Scatter");
        records=new CompoundTag();for(String k:t.getCompound("Records").getAllKeys())if(records.size()<10)records.putInt(k,bounded(t.getCompound("Records"),k,2));
    }
    private static float finite(CompoundTag t,String key,float max){float n=t.getFloat(key);return Float.isFinite(n)?Math.max(0,Math.min(max,n)):0;}
    @Override public String describe(){return profession+" energy="+energy+",shield="+shield+"/"+shieldTicks+",core="+coreCooldown+",charge="+chargeTicks+",parry="+parryTicks+",preparing="+prepared+",imbalance="+imbalance;}

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeUndeadSkills();
        coreCooldown = fresh.coreCooldown;
        java.util.Arrays.fill(professionCooldowns,0);java.util.Arrays.fill(actionCooldowns,0);reload=0;
    }

    private static int bounded(CompoundTag t,String key,int max){return Math.max(0,Math.min(max,t.getInt(key)));}
    private static String type(LivingEntity e){return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();}
    private java.util.List<LivingEntity> enemies(OperativeHost h,double radius){return h.serverLevel().getEntitiesOfClass(LivingEntity.class,h.body().getBoundingBox().inflate(radius),e->valid(h,e,radius));}
    private UndeadProfession preferred(OperativeHost h,LivingEntity target){
        UndeadProfession original=profession, best=profession;double bestScore=0;
        try {
            for(var candidate:UndeadProfession.values()) {
                profession=candidate;
                for(var slot:new SkillSlot[]{SkillSlot.ACTIVE_1,SkillSlot.ACTIVE_2,SkillSlot.CORE}) {
                    double score=score(h,slot,target);
                    if(score>0 && candidate==original)score+=3;
                    if(score>bestScore){bestScore=score;best=candidate;}
                }
            }
        } finally {profession=original;}
        return best;
    }
    public boolean selectProfession(OperativeHost h,UndeadProfession next){
        if(next==null || next==profession || switchCooldown>0 || busy())return false;
        profession=next;imbalance=2;switchCooldown=12;
        return true;
    }
    private int actionIndex(SkillSlot slot){return profession.ordinal()*3+(slot==SkillSlot.ACTIVE_1?0:slot==SkillSlot.ACTIVE_2?1:2);}
    private void commit(SkillSlot slot){
        activeCooldown=6;
        actionCooldowns[actionIndex(slot)]=switch(profession){
            case KNIGHT -> slot==SkillSlot.CORE?400:slot==SkillSlot.ACTIVE_2?60:80;
            case WARRIOR -> slot==SkillSlot.ACTIVE_1?60:200;
            case EXPLORER -> slot==SkillSlot.CORE?600:100;
            case ROGUE -> slot==SkillSlot.CORE?320:100;
            case SCHOLAR -> slot==SkillSlot.ACTIVE_1?160:slot==SkillSlot.ACTIVE_2?120:80;
            case HUNTER -> slot==SkillSlot.CORE?160:100;
        };
    }
    private float professionCost(SkillSlot slot){
        float n=switch(profession){
            case WARRIOR -> slot==SkillSlot.ACTIVE_1?30:slot==SkillSlot.CORE?50:0;
            case EXPLORER -> slot==SkillSlot.ACTIVE_1?20:slot==SkillSlot.ACTIVE_2?10:100;
            case ROGUE -> slot==SkillSlot.ACTIVE_1?15:slot==SkillSlot.ACTIVE_2?30:50;
            case SCHOLAR -> slot==SkillSlot.CORE?37.5F:20;
            case HUNTER -> slot==SkillSlot.ACTIVE_1?20:0;
            default -> 0;
        };return n*OperativeGrowth.energyCost();
    }
    private double professionScore(OperativeHost h,SkillSlot slot,LivingEntity target){
        if(!available(h) || busy() || OperativeElectronics.blocked(h,ModCharacters.UNDEAD_ID,slot)
                || slot==SkillSlot.PASSIVE)return 0;
        double d=target==null?Double.MAX_VALUE:h.body().distanceToSqr(target);
        boolean visible=valid(h,target,24),core=professionCooldowns[profession.ordinal()]==0;
        float hp=h.body().getHealth()/h.body().getMaxHealth();
        return switch(profession){
            case WARRIOR -> switch(slot){case ACTIVE_1 -> visible && d<=16?80:0;case ACTIVE_2 -> visible && !might && energy>50?65:0;case CORE -> visible && core && bloodlust==0 && hp>.4 && d<36?92:0;default->0;};
            case EXPLORER -> switch(slot){case ACTIVE_1 -> visible && d>9?75:0;case ACTIVE_2 -> energy<maxEnergy()*.5 && (!visible || d>64)?95:0;case CORE -> visible && core && space==0 && hp<.55?110:0;default->0;};
            case ROGUE -> switch(slot){case ACTIVE_1 -> visible && stealth==0 && d>9?82:0;case ACTIVE_2 -> visible && d<16 && !target.hasEffect(ModEffects.STUN)?84:0;case CORE -> visible && d<16 && core && (stealth>0 || target.hasEffect(ModEffects.STUN)) && (target instanceof Player || com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target))?102:0;default->0;};
            case SCHOLAR -> switch(slot){case ACTIVE_1 -> hp<.7?120:0;case ACTIVE_2 -> visible && d<25 && ritual==0?88:0;case CORE -> visible && records.getInt(type(target))<2 && (records.contains(type(target)) || records.size()<10)?72:0;default->0;};
            case HUNTER -> switch(slot){case ACTIVE_1 -> visible && d>25 && d<225?78:0;case ACTIVE_2 -> energy<maxEnergy()*.5 && hp>.55 && reload==0?98:0;case CORE -> visible && !scatter && d<25 && energy>25?90:0;default->0;};
            default->0;
        };
    }
    private void tickEffects(OperativeHost h){
        if(bloodlust>0 && --bloodlust%20==0)selfCost(h,.02F);
        if(stealth>0 && --stealth==0)h.body().removeEffect(MobEffects.INVISIBILITY);
        if(space>0){space--;h.body().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,12,0,false,false));}
        if(might){if(energy<.19F)might=false;else energy-=.19F;}
        if(meditation>0){meditation--;energy=Math.min(maxEnergy(),energy+.5F);h.body().heal(h.body().getMaxHealth()*.05F/20*OperativeGrowth.healing());
            if(!enemies(h,6).isEmpty() || h.body().hurtTime>0)meditation=0;}
        if(ritual>0){ritual--;if(h.body().tickCount%5==0)for(var e:enemies(h,3)){
            e.setDeltaMovement(e.getDeltaMovement().add(h.body().position().subtract(e.position()).normalize().scale(.08)));
            e.hurtMarked=true;e.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,12,0),h.body());
            if(h.body().tickCount%20==0)hit(h,e,4);
        }if(h.body().tickCount%20==0)h.body().heal(h.body().getMaxHealth()*.06F*OperativeGrowth.healing());}
        if(scatter){if(energy<.19F || enemies(h,6).isEmpty())scatter=false;else{energy-=.19F;
            if(h.body().tickCount%5==0)for(var e:enemies(h,4))hit(h,e,4);}}
    }
    private void tickProfession(OperativeHost h){
        if(prepared==null)return;
        var slot=prepared;
        LivingEntity target=chargeTarget!=null && h.serverLevel().getEntity(chargeTarget) instanceof LivingEntity e?e:null;
        if(OperativeElectronics.blocked(h,ModCharacters.UNDEAD_ID,slot)
                || !releaseValid(h,slot,target)) {cancel(h);return;}
        if(target!=null)h.aimAt(target);if(--windup>0)return;
        prepared=null;chargeTarget=null;energy=Math.max(0,energy-professionCost(slot));commit(slot);
        if(slot==SkillSlot.CORE)professionCooldowns[profession.ordinal()]=switch(profession){case WARRIOR->480;case ROGUE->320;default->1;};
        switch(profession){
            case WARRIOR -> {if(slot==SkillSlot.ACTIVE_1){if(valid(h,target,4))hit(h,target,15);}else if(slot==SkillSlot.ACTIVE_2)might=true;else bloodlust=300;}
            case EXPLORER -> {if(slot==SkillSlot.ACTIVE_1 && target!=null){for(var e:h.serverLevel().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(5),e->valid(h,e,30))){hit(h,e,4);e.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ELECTROMAGNETIC_INTERFERENCE,160,0),h.body());e.addEffect(new MobEffectInstance(MobEffects.GLOWING,160,0));}}else if(slot==SkillSlot.ACTIVE_2){meditation=30;h.body().addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,1));}else if(slot==SkillSlot.CORE)space=600;}
            case ROGUE -> {if(slot==SkillSlot.ACTIVE_1){stealth=80;h.body().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,80,0));h.body().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,80,1));}
                else if(valid(h,target,4)){if(slot==SkillSlot.ACTIVE_2){hit(h,target,1);target.addEffect(new MobEffectInstance(ModEffects.STUN,100,0),h.body());}
                else if(stealth>0 || target.hasEffect(ModEffects.STUN)){if(target instanceof Player)hit(h,target,10);else if(com.rzy.dealt_force_skills.boss.BossCombatRules.canInstantKill(target))hit(h,target,target.getMaxHealth()*4);}}}
            case SCHOLAR -> {if(slot==SkillSlot.ACTIVE_1)h.body().heal(h.body().getMaxHealth()*.75F*OperativeGrowth.healing());else if(slot==SkillSlot.ACTIVE_2)ritual=100;
                else if(target!=null && records.getInt(type(target))<2 && (records.contains(type(target)) || records.size()<10))records.putInt(type(target),records.getInt(type(target))+1);}
            case HUNTER -> {if(slot==SkillSlot.ACTIVE_1){var tnt=new PrimedTnt(h.serverLevel(),h.body().getX(),h.body().getEyeY()-.25,h.body().getZ(),h.body());tnt.setFuse(40);tnt.getPersistentData().putUUID(UndeadStateManager.HUNTER_RETREAT_OWNER,h.body().getUUID());tnt.setDeltaMovement(h.body().getLookAngle().scale(.9).add(0,.2,0));h.serverLevel().addFreshEntity(tnt);}
                else if(slot==SkillSlot.ACTIVE_2){selfCost(h,.1F);energy=Math.min(maxEnergy(),energy+50);reload=16;}else scatter=true;}
            default -> { }
        }
        h.serverLevel().sendParticles(ParticleTypes.ENCHANT,h.body().getX(),h.body().getY()+1,h.body().getZ(),12,.4,.5,.4,.01);
    }
    private void selfCost(OperativeHost h,float fraction){splittingDamage=true;try{h.body().hurt(h.body().damageSources().magic(),h.body().getMaxHealth()*fraction);}finally{splittingDamage=false;}}
    private void hit(OperativeHost h,LivingEntity target,float damage){target.invulnerableTime=0;SkillDamageHelper.hurt(target,h.body().damageSources().mobAttack(h.body()),h.body(),damage);}
    private boolean releaseValid(OperativeHost h,SkillSlot slot,LivingEntity target){
        if(slot==SkillSlot.CORE && professionCooldowns[profession.ordinal()]>0)return false;
        return switch(profession){
            case WARRIOR -> valid(h,target,slot==SkillSlot.ACTIVE_1?4:24);
            case ROGUE -> valid(h,target,slot==SkillSlot.ACTIVE_1?24:4) && (slot!=SkillSlot.CORE || stealth>0 || target.hasEffect(ModEffects.STUN));
            case EXPLORER,HUNTER -> slot==SkillSlot.ACTIVE_2 || valid(h,target,24);
            case SCHOLAR -> slot==SkillSlot.ACTIVE_1 || valid(h,target,slot==SkillSlot.ACTIVE_2?5:24);
            default -> false;
        };
    }
    public void damagedTarget(OperativeHost h,LivingEntity target,float damage){
        if(damage>0 && h.isEnemy(target) && profession==UndeadProfession.WARRIOR)
            h.body().heal(damage*(bloodlust>0?.3F:.1F)*OperativeGrowth.healing());
    }
    public float outgoing(OperativeHost h,LivingEntity target,DamageSource source,float amount){
        if(splittingDamage || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE) || !h.isEnemy(target))return amount;
        amount*=OperativeGrowth.undeadDamage();
        if(profession==UndeadProfession.WARRIOR){if(might)amount*=1.25F;if(bloodlust>0)amount*=2;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,2),h.body());
            if(might){splittingDamage=true;try{target.hurt(SkillDamageHelper.trueDamage(h.serverLevel(),h.body(),h.body()),amount*.5F);}finally{splittingDamage=false;}amount*=.5F;}
        }else if(profession==UndeadProfession.ROGUE){stealth=0;h.body().removeEffect(MobEffects.INVISIBILITY);
            if(target.getLookAngle().dot(h.body().position().subtract(target.position()).normalize())<-.5)amount*=2;
            if(target.hasEffect(ModEffects.STUN)){amount*=3;target.removeEffect(ModEffects.STUN);}
        }else if(profession==UndeadProfession.SCHOLAR && target instanceof Player)amount*=1+records.getInt(type(target))*.15F;
        return amount;
    }
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return chargeTicks > 0 ? com.rzy.dealt_force_skills.motion.TacticalMotion.CHARGE : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public int motionDuration() { return 30; }
    @Override public Vec3 motionDirection() { return Vec3.ZERO; }

}
