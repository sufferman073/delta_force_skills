package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Foundations plus purchased ninjutsu; uncapped experience remains at base level. */
public final class NativeLexSkills implements OperativeSkills {
    private final NativeLexArts arts=new NativeLexArts(this);
    public boolean plan(OperativeHost h,LivingEntity target){return !busy() && arts.choose(h,target);}
    public boolean beginArt(OperativeHost h,com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaArt art,LivingEntity target){return !busy() && arts.begin(h,art,target);}
    float resource(){return leicra;}
    float maximumLeicra(){return maxLeicra();}
    UUID handshakeTarget(){return handshake;}
    // Native operatives retain action cooldowns but do not exhaust the player resource budget.
    void spend(float n){ }
    void restoreResource(float n){leicra=Math.min(maxLeicra(),leicra+Math.max(0,n));}
    boolean seal(SkillSlot slot){int i=index(slot);float n=slot==SkillSlot.CORE?5:3;if(i<0)return false;spend(n);stacks[i]=Math.min(maxStacks(),stacks[i]+1);age[i]=0;return true;}
    private static final net.minecraft.resources.ResourceLocation SPEED=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_lex_hand_speed");
    private float leicra=maxLeicra();
    private final int[] stacks=new int[3], age=new int[3];
    private int combo, comboAge, delay, recovery;
    private SkillSlot previous, prepared;
    private UUID targetId, handshake;
    private boolean releasing;

    private static float config(String key,float fallback){float n=DealtForceConfig.floatValue("characters.lexninjia.lex_ninjia_state_manager."+key,fallback);return Float.isFinite(n)?Math.max(0,n):fallback;}
    private static float maxLeicra(){float n=DealtForceConfig.floatValue("experience_growth.lex_ninjia.base_max_leicra",200);return Float.isFinite(n)?Math.max(0,n):200;}
    private static int maxStacks(){return Math.max(1,Math.min(100,DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.max_foundation_stacks",10)));}
    private static int duration(){return Math.max(1,DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.stack_duration_ticks",200));}
    private static int expiry(){return Math.max(1,DealtForceConfig.intValue("characters.lexninjia.lex_ninjia_state_manager.input_expiry_ticks",600));}
    private static int index(SkillSlot slot){return switch(slot){case ACTIVE_1 -> 0;case ACTIVE_2 -> 1;case CORE -> 2;default -> -1;};}
    private static boolean valid(OperativeHost h,LivingEntity e,double range){return e!=null && e.isAlive() && e.level()==h.body().level() && h.isEnemy(e) && h.withinCombatRange(e.position()) && h.canSee(e) && h.body().distanceToSqr(e)<=range*range;}
    private static boolean available(OperativeHost h){return NativeRaptorSkills.available(h) && !OperativeSupport.downed(h.body());}
    private boolean equipment(OperativeHost h,SkillSlot slot){return slot==SkillSlot.ACTIVE_1?h.body().getMainHandItem().isEmpty():slot!=SkillSlot.ACTIVE_2 || MeleeWeaponCompat.isMeleeWeapon(h.body().getMainHandItem());}
    @Override public double score(OperativeHost h,SkillSlot slot,LivingEntity target){
        int i=index(slot);if(i<0 || busy() || recovery>0 || !available(h) || !equipment(h,slot)
                || OperativeElectronics.blocked(h,ModCharacters.LEX_NINJIA_ID,slot))return 0;
        return switch(slot){
            case ACTIVE_1 -> valid(h,target,8) && !target.getUUID().equals(handshake) && stacks[0]<maxStacks()
                    && (h.body().getHealth()<h.body().getMaxHealth()*.6F || target==h.body().getLastHurtByMob()) ? 88 : 0;
            case ACTIVE_2 -> valid(h,target,3) ? 80 : 0;
            case CORE -> valid(h,target,20) && stacks[2]<Math.min(maxStacks(),h.body().getHealth()<h.body().getMaxHealth()*.6F?10:3) ? 65 : 0;
            default -> 0;
        };
    }
    @Override public boolean start(OperativeHost h,SkillSlot slot,LivingEntity target){
        if(score(h,slot,target)<=0)return false;
        prepared=slot;targetId=target.getUUID();delay=2;h.aimAt(target);return true;
    }
    @Override public void tick(OperativeHost h){
        leicra=maxLeicra();
        if(recovery>0)recovery--;
        for(int i=0;i<3;i++)if(stacks[i]>0 && ++age[i]>duration()){stacks[i]--;age[i]=0;}
        if(combo>0 && ++comboAge>expiry()){combo=comboAge=0;previous=null;}
        if(handshake!=null && (!(h.serverLevel().getEntity(handshake) instanceof LivingEntity e) || !e.isAlive() || !h.isEnemy(e) || !h.withinCombatRange(e.position())))handshake=null;
        updateSpeed(h);
        arts.tick(h);
        if(!available(h)){cancel(h);return;}
        if(prepared==null)return;
        var slot=prepared;
        LivingEntity target=targetId!=null && h.serverLevel().getEntity(targetId) instanceof LivingEntity e?e:null;
        if(!valid(h,target,slot==SkillSlot.ACTIVE_2?3:slot==SkillSlot.ACTIVE_1?8:20) || !equipment(h,slot)
                || OperativeElectronics.blocked(h,ModCharacters.LEX_NINJIA_ID,slot)){cancel(h);return;}
        h.aimAt(target);if(--delay>0)return;
        float cost=slot==SkillSlot.CORE?5:3;
        spend(cost);int i=index(slot);stacks[i]=Math.min(maxStacks(),stacks[i]+1);age[i]=0;
        combo=previous==slot?combo+1:1;previous=slot;comboAge=0;
        updateSpeed(h);
        // Foundations form a real two-input combo; Harmony is a defensive foundation, not a free art.
        if(combo>=2 && slot!=SkillSlot.CORE){
            if(slot==SkillSlot.ACTIVE_1){handshake=target.getUUID();target.addEffect(new MobEffectInstance(MobEffects.GLOWING,300,0));h.body().addEffect(new MobEffectInstance(MobEffects.GLOWING,300,0));}
            else cut(h);
            combo=comboAge=0;previous=null;
        }
        h.serverLevel().playSound(null,h.body().blockPosition(),slot==SkillSlot.ACTIVE_2?net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP:net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,net.minecraft.sounds.SoundSource.NEUTRAL,.7F,1);
        prepared=null;targetId=null;recovery=4;
    }
    static void sound(OperativeHost h,String id){
        var sound=com.rzy.dealt_force_skills.registry.ModSounds.lexNinjiaSound(id);
        if(sound!=null)h.serverLevel().playSound(null,h.body().blockPosition(),sound,net.minecraft.sounds.SoundSource.NEUTRAL,1,1);
    }
    private void cut(OperativeHost h){
        double range=art("one_word_cut","range",3),width=art("one_word_cut","half_width",1),height=art("one_word_cut","half_height",1);
        var eye=h.body().getEyePosition();var look=h.body().getLookAngle();look=new Vec3(look.x,0,look.z).normalize();var right=new Vec3(-look.z,0,look.x);
        releasing=true;
        try{
            for(var e:h.serverLevel().getEntitiesOfClass(LivingEntity.class,h.body().getBoundingBox().inflate(Math.max(range,width),height,Math.max(range,width)),e->valid(h,e,Math.max(range,width)+2))){
                var d=e.getBoundingBox().getCenter().subtract(eye);double forward=d.dot(look);
                if(forward>0 && forward<=range && Math.abs(d.dot(right))<=width && Math.abs(d.y)<=height)
                    SkillDamageHelper.hurtUnscaled(e,SkillDamageHelper.trueDamage(h.serverLevel(),h.body(),h.body()),SkillDamageHelper.scale(h.body(),(float)Math.max(2,h.body().getAttributeValue(Attributes.ATTACK_DAMAGE))));
            }
        }finally{releasing=false;}
        h.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,h.body().getX(),h.body().getY()+1,h.body().getZ(),1,0,0,0,0);
    }
    private static double art(String art,String key,double fallback){double n=DealtForceConfig.doubleValue("characters.lex_ninjia.arts."+art+"."+key,fallback);return Double.isFinite(n)?Math.max(0,Math.min(32,n)):fallback;}
    public float incoming(OperativeHost h,DamageSource source,float amount){
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
        double multiplier=Math.max(0,1-stacks[2]*.02);
        if(source.getEntity()!=null && source.getEntity().getUUID().equals(handshake))multiplier*=art("handshake","incoming_damage_multiplier",.7);
        return arts.incoming(h,source,(float)(amount*multiplier));
    }
    public float outgoing(LivingEntity target,DamageSource source,float amount){
        if(releasing || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE))return amount;
        double multiplier=1+stacks[1]*.03;
        if(target.getUUID().equals(handshake))multiplier*=art("handshake","outgoing_non_art_damage_multiplier",.1);
        return (float)(amount*multiplier);
    }
    public float outgoing(OperativeHost h,LivingEntity target,DamageSource source,float amount){return arts.outgoing(h,target,source,outgoing(target,source,amount));}
    private void updateSpeed(OperativeHost h){var a=h.body().getAttribute(Attributes.MOVEMENT_SPEED);if(a==null)return;a.removeModifier(SPEED);if(stacks[0]>0)a.addTransientModifier(new AttributeModifier(SPEED,stacks[0]*.02,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    public boolean interdict(OperativeHost h,LivingEntity attacker,LivingEntity target,float amount){return arts.interdict(h,attacker,target,amount);}
    public void damagedTarget(OperativeHost h,LivingEntity target,float damage){arts.damagedTarget(h,target,damage);}
    @Override public com.rzy.dealt_force_skills.motion.TacticalMotion motion() {
        return arts.busy() || arts.charging() || prepared != null
                ? com.rzy.dealt_force_skills.motion.TacticalMotion.NINJA_CHARGE
                : com.rzy.dealt_force_skills.motion.TacticalMotion.NONE;
    }
    @Override public boolean locksMovement() { return arts.sleeping() || arts.charging(); }
    @Override public boolean locksWeapon() { return arts.sleeping() || arts.charging(); }
    @Override public boolean busy(){return prepared!=null || arts.busy() || arts.sleeping() || arts.charging();}
    @Override public void cancel(OperativeHost h){prepared=null;targetId=null;delay=0;arts.interrupt();}
    public void clear(OperativeHost h){cancel(h);arts.clear(h);var a=h.body().getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(SPEED);}
    @Override public CompoundTag save(){var t=new CompoundTag();t.putFloat("Leicra",leicra);for(int i=0;i<3;i++){t.putInt("Stack"+i,stacks[i]);t.putInt("Age"+i,age[i]);}t.putInt("Combo",combo);t.putInt("ComboAge",comboAge);if(previous!=null)t.putString("Previous",previous.name());if(handshake!=null)t.putUUID("Handshake",handshake);t.putInt("Recovery",recovery);t.put("Arts",arts.save());return t;}
    @Override public void load(CompoundTag t){
        float n=t.getFloat("Leicra");leicra=Float.isFinite(n)?Math.max(0,Math.min(maxLeicra(),n)):0;
        for(int i=0;i<3;i++){stacks[i]=Math.max(0,Math.min(maxStacks(),t.getInt("Stack"+i)));age[i]=Math.max(0,Math.min(duration(),t.getInt("Age"+i)));}
        combo=Math.max(0,Math.min(1,t.getInt("Combo")));comboAge=Math.max(0,Math.min(expiry(),t.getInt("ComboAge")));
        try{previous=SkillSlot.valueOf(t.getString("Previous"));if(index(previous)<0)previous=null;}catch(IllegalArgumentException e){previous=null;}
        if(previous==null)combo=0;
        handshake=t.hasUUID("Handshake")?t.getUUID("Handshake"):null;recovery=Math.max(0,Math.min(4,t.getInt("Recovery")));prepared=null;targetId=null;delay=0;
        arts.load(t.getCompound("Arts"));
    }
    @Override public String describe(){return "lex leicra="+leicra+",stacks="+java.util.Arrays.toString(stacks)+",combo="+previous+":"+combo+",handshake="+handshake+",preparing="+prepared+",art="+arts.describe();}
}
