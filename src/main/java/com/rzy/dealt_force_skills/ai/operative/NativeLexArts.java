package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.boss.BossCombatRules;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaArt;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaComboInput;
import com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaSchool;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.rzy.dealt_force_skills.util.MeleeWeaponCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import java.util.EnumMap;
import java.util.UUID;

/** Server-side combo input/release runtime. Purchased arts and finite mind expansions are unlocked. */
final class NativeLexArts {
    private final NativeLexSkills skills;
    private LexNinjiaArt plan;
    private UUID targetId;
    private int input, delay, cadence;
    private final EnumMap<LexNinjiaArt,Integer> cooldowns=new EnumMap<>(LexNinjiaArt.class);
    private final EnumMap<LexNinjiaArt,Integer> effects=new EnumMap<>(LexNinjiaArt.class);
    private int reflect, sharpen, peas, swordCharge, fertilizer;
    private float overflow;
    private Vec3 swordOrigin;
    private UUID beast, deathGod;
    private boolean pact, doubleHand, whiteCrane;
    private ItemStack previousOffhand=ItemStack.EMPTY;
    private net.minecraft.core.HolderLookup.Provider registryProvider;
    private CompoundTag pendingOffhand;
    private record Slash(UUID target,int remaining,float damage,int delay,int interval) { }
    private final java.util.List<Slash> slashes=new java.util.ArrayList<>();
    private boolean emitting;
    NativeLexArts(NativeLexSkills skills){this.skills=skills;}
    boolean busy(){return plan!=null;}
    boolean sleeping(){return active(LexNinjiaArt.GOOD_SLEEP);}
    boolean charging(){return swordCharge>0 || active(LexNinjiaArt.HAM_KILL_ALL);}
    private boolean active(LexNinjiaArt a){return effects.getOrDefault(a,0)>0;}
    private static double value(LexNinjiaArt a,String key,double fallback){
        double n=com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue("characters.lex_ninjia.arts."+a.id()+"."+key,fallback);
        return Double.isFinite(n)?Math.max(0,n):fallback;
    }
    private void duration(LexNinjiaArt a,int ticks){effects.put(a,Math.max(1,Math.min(12000,(int)value(a,"duration_ticks",ticks))));}
    private boolean valid(OperativeHost h,LivingEntity t,double range){return t!=null && t.isAlive() && t.level()==h.body().level() && h.isEnemy(t) && h.withinCombatRange(t.position()) && h.canSee(t) && h.body().distanceToSqr(t)<=range*range;}
    private java.util.List<LivingEntity> enemies(OperativeHost h,double r){return h.serverLevel().getEntitiesOfClass(LivingEntity.class,h.body().getBoundingBox().inflate(r),e->valid(h,e,r));}
    private float foundationCost(LexNinjiaArt a){float n=0;for(var i:a.combo())n+=i==LexNinjiaComboInput.HARMONY?5:i==LexNinjiaComboInput.HAND || i==LexNinjiaComboInput.BLADE?3:0;return n;}
    private boolean allowed(OperativeHost h,LexNinjiaArt a,LivingEntity t){return allowed(h,a,t,true);}
    private boolean allowed(OperativeHost h,LexNinjiaArt a,LivingEntity t,boolean starting){
        if(a==LexNinjiaArt.ONE_WORD_CUT || a==LexNinjiaArt.HANDSHAKE || cooldowns.getOrDefault(a,0)>0)return false;
        if((a.school()==LexNinjiaSchool.BLADE || a.combo().contains(LexNinjiaComboInput.BLADE)) && !MeleeWeaponCompat.isMeleeWeapon(h.body().getMainHandItem()))return false;
        for(var seal:a.combo()){
            var slot=seal==LexNinjiaComboInput.HAND?SkillSlot.ACTIVE_1:seal==LexNinjiaComboInput.BLADE?SkillSlot.ACTIVE_2:seal==LexNinjiaComboInput.HARMONY?SkillSlot.CORE:null;
            if(slot!=null && OperativeElectronics.blocked(h,ModCharacters.LEX_NINJIA_ID,slot))return false;
        }
        if(a.hamForbidden() && a!=LexNinjiaArt.HAM_FRIEND && !active(LexNinjiaArt.HAM_FRIEND))return false;
        if(a==LexNinjiaArt.SHADOW_SMOKE && !active(LexNinjiaArt.SHADOW_BLADE))return false;
        if(a==LexNinjiaArt.NO_NAME_BLADE && t!=h.body().getLastHurtByMob())return false;
        if(a==LexNinjiaArt.ONE_DEATH_HAND && (t==null || !t.getUUID().equals(skills.handshakeTarget())))return false;
        return t==h.body() && restorative(a) || valid(h,t,a.releaseTrigger()==com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaReleaseTrigger.LEFT_CLICK?3:24);
    }
    boolean choose(OperativeHost h,LivingEntity target){
        if(busy() || sleeping() || cadence>0 || !NativeRaptorSkills.available(h))return false;
        LexNinjiaArt selected=null;double best=Double.POSITIVE_INFINITY;int bestUrgency=-1;
        boolean lowHealth=h.body().getHealth()<h.body().getMaxHealth()*.5F;
        if(!valid(h,target,24)){if(!lowHealth)return false;target=h.body();}
        boolean protectedInCombat=java.util.List.of(LexNinjiaArt.FD_HAND,LexNinjiaArt.REFLECT_HAND,
                LexNinjiaArt.SHIELD_GUARD,LexNinjiaArt.DEATH_FLAME_SMOKE,
                LexNinjiaArt.NO_ONE_RETALIATES,LexNinjiaArt.SAND_WALL).stream().anyMatch(this::active);
        for(var art:LexNinjiaArt.values())if(allowed(h,art,target)){
            double score=score(h,art,target);
            if (score <= 0) continue;
            int urgency=lowHealth && restorative(art)?2:!protectedInCombat && protective(art)?1:0;
            // Emergency recovery and missing protection win; keep variety within the selected tier.
            double priority = -Math.log(Math.max(1.0E-9, h.body().getRandom().nextDouble())) / (score * score * score);
            if (urgency>bestUrgency || urgency==bestUrgency && priority<best) {
                bestUrgency=urgency;best=priority;selected=art;
            }
        }
        return selected!=null && begin(h,selected,target);
    }
    private static boolean restorative(LexNinjiaArt art){return switch(art){
        case HAMBURGER,LOTUS_BOX_FOOD,RETURN_HAND,SOLDIER_PILL,NANO_SNICKERS,MILK_FRUIT_SHAKE,
                SHRIMP_HAND,ROAST_MEAT_RICE,MC_NUGGETS -> true;default -> false;};}
    private static boolean protective(LexNinjiaArt art){return switch(art){
        case FD_HAND,REFLECT_HAND,SHIELD_GUARD,DEATH_FLAME_SMOKE,NO_ONE_RETALIATES,SAND_WALL -> true;
        default -> false;};}
    private static int cooldown(LexNinjiaArt art){return art.cookRecipe()?300:50;}
    private double score(OperativeHost h,LexNinjiaArt a,LivingEntity t){
        double d=h.body().distanceTo(t),hp=h.body().getHealth()/h.body().getMaxHealth();int group=enemies(h,8).size();
        if(active(a) || a==LexNinjiaArt.DOUBLE_LUOHAN && doubleHand || a==LexNinjiaArt.WHITE_CRANE && whiteCrane
                || a==LexNinjiaArt.HAM_BEAST && beast!=null || a==LexNinjiaArt.DEATH_GOD_HAND && deathGod!=null)return 0;
        return switch(a){
            case HAMBURGER -> hp<.55?145:0;
            case LOTUS_BOX_FOOD -> hp<.75?125:0;
            case MILK_BEER -> h.body().getActiveEffects().stream().anyMatch(e->!e.getEffect().value().isBeneficial())?122:0;
            case MILK_FRUIT_SHAKE -> hp<.5 || !h.body().hasEffect(MobEffects.DAMAGE_RESISTANCE)?92:0;
            case ROAST_MEAT_RICE,SHRIMP_HAND -> !h.body().hasEffect(MobEffects.DAMAGE_BOOST) && d<8?100:0;
            case MC_NUGGETS -> !h.body().hasEffect(MobEffects.HEALTH_BOOST) && hp>.7 && d>4?80:0;
            case HOT_DRINK -> !h.body().hasEffect(MobEffects.DAMAGE_BOOST) && d>8?78:0;
            case SOLDIER_PILL,NANO_SNICKERS -> hp<.5?110:0;
            case COLD_COPPER -> 0;
            case BIG_PORTION -> (skills.resource()<60 || fertilizer==0 && d>4) && hp>.8?120:0;
            case GOOD_SLEEP -> (skills.resource()<40 || hp<.55) && d>12 && h.body().hurtTime==0?115:0;
            case RETURN_HAND -> hp<.6?110:0;
            case FD_HAND -> hp<.5?108:0;
            case DEATH_FLAME_SMOKE -> hp<.4?112:0;
            case REFLECT_HAND -> t.getMainHandItem().getItem() instanceof com.tacz.guns.api.item.IGun?100:0;
            case SHIELD_GUARD -> 92;
            case SHADOW_SMOKE -> hp<.6 && d<5?103:0;
            case FLASH_CUT_HAND -> d<3?(!t.getMainHandItem().isEmpty()?96:65):0;
            case STOP_HAND -> d<3 && !t.hasEffect(ModEffects.RAPTOR_ACTION_PAUSE)?94:0;
            case ION_HAND -> d>5 && d<16?84:0;
            case SPIN_ION_HAND -> d<6?(group>=3?98:75):0;
            case TEN_METER_SWORD -> d<10?88:0;
            case IRON_SWORD_RAIN -> group>=2?100:t.getHealth()>30?76:0;
            case DEATH_GOD_HAND -> t.getHealth()>40?101:0;
            case ALL_HANDS -> d<=16?(group>=3 || hp<.5?121:85):0;
            case NO_NAME_BLADE,ONE_DEATH_HAND -> d<3 && BossCombatRules.canInstantKill(t)?105:0;
            case FIRE_FIST,CLIFF_FALL_BLADE -> d<3?78:0;
            case LUOHAN_HAND -> d<3?(doubleHand || whiteCrane?160:78):0;
            case BURNING_BLADE -> d<3?87:0;
            case SNAKE_POISON_HAND -> d<4 && t.getHealth()>10?93:0;
            case ARASHI_CUT,SHADOW_CLONE_CROSS -> d<5?(group>=2?89:70):0;
            case SHADOW_BLADE -> d<7?86:0;
            case SHARPEN -> sharpen<5 && d<12?100:0;
            case PEA_SHOOTER -> peas<5?70:0;
            case DOUBLE_LUOHAN,WHITE_CRANE -> d<6?112:0;
            case ONE_BLADE_TAUNT -> hp>.75?72:0;
            case NO_ONE_RETALIATES,SAND_WALL -> hp<.7?(group>=3?95:80):0;
            case HAM_FRIEND -> hp>.6 && !active(LexNinjiaArt.HAM_FRIEND)?100:0;
            case HAM_BERSERK -> hp>.7 && d<4?91:0;
            case HAM_SHADOW_KICK -> hp>.65 && d<6?90:0;
            case HAM_BEAST -> hp>.85 && t.getHealth()>30?80:0;
            case HAM_KILL_ALL -> hp<.15 && group>=5?125:0;
            default -> 0;
        };
    }
    boolean begin(OperativeHost h,LexNinjiaArt art,LivingEntity target){
        if(busy() || charging() || sleeping() || !NativeRaptorSkills.available(h) || !allowed(h,art,target))return false;
        plan=art;targetId=target.getUUID();input=0;delay=1;return true;
    }
    void tick(OperativeHost h){
        registryProvider=h.serverLevel().registryAccess();resolveOffhand(h);
        cooldowns.replaceAll((a,n)->Math.max(0,n-1));
        if(cadence>0)cadence--;
        for(var a:LexNinjiaArt.values()){
            int n=effects.getOrDefault(a,0);if(n>0){effects.put(a,n-1);if(n==1){
                if(a==LexNinjiaArt.GOOD_SLEEP){h.body().heal(h.body().getMaxHealth());skills.restoreResource(skills.maximumLeicra()*.5F);}
                if(a==LexNinjiaArt.SHIELD_GUARD)restoreShield(h);
                if(a==LexNinjiaArt.DEATH_FLAME_SMOKE && overflow>0){float debt=overflow+1;overflow=0;selfDamage(h,debt);}
                if(a==LexNinjiaArt.HAM_KILL_ALL){for(var e:enemies(h,100))execute(h,e);self(h,(float)value(a,"self_damage_max_health_fraction",4));}
            }}
        }
        if(fertilizer>0)fertilizer--;
        if(active(LexNinjiaArt.DEATH_FLAME_SMOKE) || active(LexNinjiaArt.RETURN_HAND))
            for(var effect:java.util.List.copyOf(h.body().getActiveEffects()))if(!effect.getEffect().value().isBeneficial())h.body().removeEffect(effect.getEffect());
        if(active(LexNinjiaArt.DEATH_FLAME_SMOKE))h.body().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,30,0));
        if(!h.parallelNativeAi() && active(LexNinjiaArt.SHIELD_GUARD) && h.body() instanceof Mob mob && mob.getTarget()!=null && h.isEnemy(mob.getTarget()))h.body().startUsingItem(InteractionHand.OFF_HAND);
        if(active(LexNinjiaArt.RETURN_HAND) && h.body().tickCount%20==0)h.body().heal((float)(h.body().getMaxHealth()*value(LexNinjiaArt.RETURN_HAND,"heal_max_health_fraction_per_second",.04)));
        if(active(LexNinjiaArt.IRON_SWORD_RAIN) && h.body().tickCount%6==0)for(var e:enemies(h,40))hit(h,e,melee(h)*.9F);
        if(active(LexNinjiaArt.HAM_SHADOW_KICK) && h.body().getDeltaMovement().horizontalDistanceSqr()>.005 && h.body().tickCount%10==0){self(h,(float)value(LexNinjiaArt.HAM_SHADOW_KICK,"tick_self_health_cost_fraction",.025));for(var e:enemies(h,7)){hit(h,e,melee(h)*.7F);e.addEffect(new MobEffectInstance(ModEffects.STUN,20,0),h.body());}}
        for(int i=slashes.size()-1;i>=0;i--){var s=slashes.get(i);if(s.delay()>1){slashes.set(i,new Slash(s.target(),s.remaining(),s.damage(),s.delay()-1,s.interval()));continue;}
            slashes.remove(i);var e=h.serverLevel().getEntity(s.target());if(e instanceof LivingEntity t && valid(h,t,10)){hit(h,t,s.damage());if(s.remaining()>1)slashes.add(new Slash(s.target(),s.remaining()-1,s.damage(),s.interval(),s.interval()));}}
        if(swordCharge>0){
            if(swordOrigin==null || !h.parallelNativeAi() && h.body().position().distanceToSqr(swordOrigin)>.25 || !NativeRaptorSkills.available(h) || !MeleeWeaponCompat.isMeleeWeapon(h.body().getMainHandItem()))swordCharge=0;
            else if(--swordCharge==0)front(h,66,6,6,melee(h)*5);
        }
        if(beast!=null && h.serverLevel().getEntity(beast)!=null && !h.serverLevel().getEntity(beast).isAlive())beast=null;
        if(deathGod!=null && (h.serverLevel().getEntity(deathGod)==null || !h.serverLevel().getEntity(deathGod).isAlive()))deathGod=null;
        if(plan==null)return;
        LivingEntity target=targetId!=null && h.serverLevel().getEntity(targetId) instanceof LivingEntity t?t:null;
        if(!NativeRaptorSkills.available(h) || !(target==h.body() && restorative(plan)) && !valid(h,target,24) || OperativeSupport.downed(h.body())){cancel();return;}
        if(target!=h.body())h.aimAt(target);if(--delay>0)return;delay=1;
        if(input<plan.combo().size()){
            var seal=plan.combo().get(input++);
            SkillSlot slot=switch(seal){case HAND->SkillSlot.ACTIVE_1;case BLADE->SkillSlot.ACTIVE_2;case HARMONY->SkillSlot.CORE;default->null;};
            if(slot!=null){
                if(OperativeElectronics.blocked(h,ModCharacters.LEX_NINJIA_ID,slot)){cancel();return;}
                // The hand is empty only for the input, restored synchronously even on cancellation/error.
                ItemStack held=h.body().getMainHandItem();
                try{if(slot==SkillSlot.ACTIVE_1)h.body().setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                    if(slot==SkillSlot.ACTIVE_2 && !MeleeWeaponCompat.isMeleeWeapon(held) || !skills.seal(slot)){cancel();return;}
                }finally{if(slot==SkillSlot.ACTIVE_1)h.body().setItemSlot(EquipmentSlot.MAINHAND,held);}
            }
            h.serverLevel().sendParticles(ParticleTypes.ENCHANT,h.body().getX(),h.body().getEyeY(),h.body().getZ(),3,.2,.15,.2,0);
            return;
        }
        var released=plan;
        if(!allowed(h,released,target,false)){cancel();return;}
        skills.spend(released.leicraCost());cancel();cadence=1;cooldowns.put(released,cooldown(released));
        NativeLexSkills.sound(h,released.soundId());
        if(released.hamForbidden() && released!=LexNinjiaArt.HAM_FRIEND)effects.remove(LexNinjiaArt.HAM_FRIEND);
        ItemStack held=h.body().getMainHandItem();
        try{if(released.school()==LexNinjiaSchool.HAND)h.body().setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);release(h,released,target);}
        finally{if(released.school()==LexNinjiaSchool.HAND)h.body().setItemSlot(EquipmentSlot.MAINHAND,held);}
        // Ordinary attacks continue during the next seal sequence, consuming released buffs.
    }
    private float melee(OperativeHost h){return (float)Math.max(2,h.body().getAttributeValue(Attributes.ATTACK_DAMAGE));}
    private void hit(OperativeHost h,LivingEntity target,float amount){if(!valid(h,target,100))return;emitting=true;try{target.invulnerableTime=0;SkillDamageHelper.hurtUnscaled(target,SkillDamageHelper.trueDamage(h.serverLevel(),h.body(),h.body()),SkillDamageHelper.scale(h.body(),amount));}finally{emitting=false;}}
    private void selfDamage(OperativeHost h,float damage){emitting=true;try{h.body().invulnerableTime=0;SkillDamageHelper.hurtUnscaled(h.body(),SkillDamageHelper.trueDamage(h.serverLevel(),h.body(),h.body()),damage);}finally{emitting=false;}}
    private void self(OperativeHost h,float fraction){selfDamage(h,h.body().getMaxHealth()*fraction);}
    private void queue(LivingEntity t,int count,float damage){queue(t,count,damage,6);}
    private void queue(LivingEntity t,int count,float damage,int interval){if(slashes.size()<64)slashes.add(new Slash(t.getUUID(),count,damage,interval,interval));}
    private float hand(float damage){if(doubleHand){doubleHand=false;return damage*2;}return damage;}
    private void execute(OperativeHost h,LivingEntity t){if(BossCombatRules.canInstantKill(t))hit(h,t,Math.max(1000,t.getMaxHealth()*20));}
    private void front(OperativeHost h,double range,double width,double height,float damage){var look=h.body().getLookAngle().multiply(1,0,1).normalize();var right=new Vec3(-look.z,0,look.x);
        for(var e:enemies(h,Math.hypot(range,width))){var delta=e.getBoundingBox().getCenter().subtract(h.body().getEyePosition());double ahead=delta.dot(look);if(ahead>0 && ahead<=range && Math.abs(delta.dot(right))<=width && Math.abs(delta.y)<=height)hit(h,e,damage);}}
    private static final net.minecraft.resources.ResourceLocation COOK_ABSORPTION=
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_lex_cook_absorption");
    private static void addCookAbsorption(OperativeHost h,float previous,float saturation){
        var attribute=h.body().getAttribute(Attributes.MAX_ABSORPTION);
        if(attribute!=null && saturation>0){
            var existing=attribute.getModifier(COOK_ABSORPTION);
            double capacity=Math.min(2048,(existing==null?0:existing.amount())+saturation);
            attribute.removeModifier(COOK_ABSORPTION);
            attribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    COOK_ABSORPTION,capacity,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        h.body().setAbsorptionAmount(Math.min(2048,previous+saturation));
    }
    private void release(OperativeHost h,LexNinjiaArt a,LivingEntity t){
        if(a.cookRecipe()){
            float previousAbsorption=h.body().getAbsorptionAmount();
            com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager.applyCookFoodEffect(h.body(),a);
            int nutrition=switch(a){case SOLDIER_PILL,MILK_FRUIT_SHAKE->16;case NANO_SNICKERS,HAMBURGER,LOTUS_BOX_FOOD->20;
                case SHRIMP_HAND->10;case ROAST_MEAT_RICE->100;case MC_NUGGETS->9;case COLD_COPPER->1000;default->0;};
            float saturationModifier=switch(a){case MILK_FRUIT_SHAKE,LOTUS_BOX_FOOD->1.5F;case SHRIMP_HAND->.9F;
                case ROAST_MEAT_RICE->10;case MC_NUGGETS->.8F;case COLD_COPPER->100;default->1;};
            h.body().heal(nutrition);
            // FoodData's nutrition * modifier * 2 saturation, converted directly for NPCs without hunger.
            float saturation=nutrition*saturationModifier*2;
            addCookAbsorption(h,Math.max(previousAbsorption,h.body().getAbsorptionAmount()),saturation);
            return;
        }
        switch(a){
            case FIRE_FIST -> {if(valid(h,t,3)){hit(h,t,(float)(value(a,"flat_damage",10)+t.getMaxHealth()*value(a,"max_health_burn_fraction",.01)));t.igniteForSeconds(3);}}
            case LUOHAN_HAND -> {if(valid(h,t,3)){hit(h,t,hand((float)value(a,"flat_damage",12)));if(whiteCrane){whiteCrane=false;t.addEffect(new MobEffectInstance(MobEffects.LEVITATION,40,0),h.body());}}}
            case FLASH_CUT_HAND -> {if(valid(h,t,3))t.addEffect(new MobEffectInstance(ModEffects.TEMPEST_DISARMED,70,0),h.body());}
            case STOP_HAND -> {if(valid(h,t,3)){t.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE,80,0),h.body());t.addEffect(new MobEffectInstance(ModEffects.NOX_DELAYED_WOUND,200,0),h.body());}}
            case CLIFF_FALL_BLADE -> {if(valid(h,t,3))t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,120,10));}
            case NO_NAME_BLADE,ONE_DEATH_HAND -> {if(valid(h,t,3))execute(h,t);}
            case BURNING_BLADE -> {duration(a,100);queue(t,4,melee(h));}
            case TEN_METER_SWORD -> {swordCharge=60;swordOrigin=h.body().position();if(!h.parallelNativeAi() && h.body() instanceof Mob mob)mob.getNavigation().stop();}
            case SHARPEN -> sharpen=20;
            case REFLECT_HAND -> {duration(a,1200);reflect=4;}
            case PEA_SHOOTER -> {duration(a,300);peas=Math.min(5,peas+1);}
            case BIG_PORTION -> {self(h,(float)value(a,"self_health_cost_fraction",.1));if(h.body().isAlive()){skills.restoreResource(skills.maximumLeicra()*.5F);fertilizer=600;}}
            case GOOD_SLEEP -> duration(a,200);
            case ION_HAND -> {if(valid(h,t,24)){var p=h.body().position().add(h.body().getLookAngle().normalize().scale(1.5));if(h.serverLevel().noCollision(t,t.getBoundingBox().move(p.subtract(t.position())))){t.teleportTo(p.x,h.body().getY(),p.z);t.setDeltaMovement(h.body().position().subtract(t.position()).normalize().scale(1.2));t.hurtMarked=true;}}}
            case SPIN_ION_HAND -> {front(h,4,2,2,hand(6));var dash=h.body().getLookAngle().normalize().scale(.4);h.body().push(dash.x,.1,dash.z);}
            case SHADOW_SMOKE -> {h.body().addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,80,0));for(var e:enemies(h,6))if(e instanceof Mob mob)mob.setTarget(null);}
            case ONE_BLADE_TAUNT -> {for(var e:enemies(h,12))if(e instanceof Mob mob)mob.setTarget(h.body());}
            case SHIELD_GUARD -> {if(!active(a))previousOffhand=h.body().getOffhandItem().copy();var shield=new ItemStack(Items.SHIELD);shield.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,new net.minecraft.world.item.component.Unbreakable(true));var marker=new CompoundTag();marker.putBoolean("OperativeLexShield",true);shield.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(marker));h.body().setItemSlot(EquipmentSlot.OFFHAND,shield);duration(a,600);if(!h.parallelNativeAi())h.body().startUsingItem(InteractionHand.OFF_HAND);}
            case SAND_WALL -> {NativeLexTerrain.create(h);duration(a,600);}
            case DOUBLE_LUOHAN -> doubleHand=true;
            case WHITE_CRANE -> whiteCrane=true;
            case FD_HAND -> duration(a,40);
            case ARASHI_CUT -> duration(a,300);
            case SHADOW_BLADE -> duration(a,240);
            case SHADOW_CLONE_CROSS -> duration(a,2400);
            case IRON_SWORD_RAIN -> duration(a,120);
            case DEATH_FLAME_SMOKE -> duration(a,200);
            case RETURN_HAND -> duration(a,280);
            case SNAKE_POISON_HAND -> duration(a,400);
            case NO_ONE_RETALIATES -> duration(a,1200);
            case DEATH_GOD_HAND -> summonDeathGod(h,t);
            case ALL_HANDS -> {
                if(valid(h,t,16)){hit(h,t,22);t.addEffect(new MobEffectInstance(ModEffects.RAPTOR_ACTION_PAUSE,80,0),h.body());t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,120,10),h.body());
                    String key="dealt_force_skills.lex_ninjia.snake_poison_stacks";t.getPersistentData().putInt(key,Math.min(9,t.getPersistentData().getInt(key)+3));}
                duration(LexNinjiaArt.FD_HAND,40);peas=5;duration(LexNinjiaArt.PEA_SHOOTER,300);duration(LexNinjiaArt.RETURN_HAND,280);NativeLexTerrain.create(h);duration(LexNinjiaArt.SAND_WALL,600);
            }
            case HAM_FRIEND -> {duration(a,12000);pact=true;}
            case HAM_BERSERK -> {self(h,(float)value(a,"self_health_cost_fraction",.2));if(h.body().isAlive())duration(a,600);}
            case HAM_SHADOW_KICK -> {self(h,(float)value(a,"release_self_health_cost_fraction",.05));if(h.body().isAlive())duration(a,300);}
            case HAM_BEAST -> {self(h,(float)value(a,"self_health_cost_fraction",.5));if(h.body().isAlive())summonBeast(h);}
            case HAM_KILL_ALL -> {duration(a,100);h.body().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,4));}
            default -> throw new IllegalStateException("Unsupported native art "+a);
        }
        h.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,t.getX(),t.getY()+1,t.getZ(),6,.5,.4,.5,0);
    }
    float incoming(OperativeHost h,DamageSource source,float amount){
        if(emitting)return amount;
        // Companions block with the skill shield without replacing native gun/item use.
        if(h.parallelNativeAi() && active(LexNinjiaArt.SHIELD_GUARD)
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)
                && source.getSourcePosition()!=null) {
            var incoming=h.body().position().subtract(source.getSourcePosition()).multiply(1,0,1).normalize();
            if(incoming.dot(h.body().getViewVector(1).multiply(1,0,1))<0)return 0;
        }
        if(active(LexNinjiaArt.FD_HAND)){effects.remove(LexNinjiaArt.FD_HAND);return 0;}
        if(active(LexNinjiaArt.REFLECT_HAND) && reflect>0 && source.getDirectEntity() instanceof Projectile){reflect--;if(source.getEntity() instanceof LivingEntity t)hit(h,t,amount);return 0;}
        if(pact)amount*=.75F;
        if(active(LexNinjiaArt.HAM_BERSERK) || active(LexNinjiaArt.HAM_SHADOW_KICK))amount*=.55F;
        if(sleeping())amount*=.2F;
        if(active(LexNinjiaArt.DEATH_FLAME_SMOKE) && amount>=h.body().getHealth()){float taken=Math.max(0,h.body().getHealth()-1);overflow+=Math.max(0,amount-taken);amount=taken;}
        return amount;
    }
    float outgoing(OperativeHost h,LivingEntity t,DamageSource source,float amount){
        if(emitting || !h.isEnemy(t) || source.is(SkillDamageHelper.TRUE_SKILL_DAMAGE))return amount;
        amount*=1+sharpen*.01F;if(sharpen>0)sharpen--;
        if(pact)amount*=1.5F;
        if(active(LexNinjiaArt.HAM_BERSERK))amount*=3;
        if(active(LexNinjiaArt.BURNING_BLADE)){hit(h,t,t.getMaxHealth()*.025F);t.igniteForSeconds(3);}
        if(active(LexNinjiaArt.SHADOW_BLADE))hit(h,t,t.getMaxHealth()*.005F);
        if(active(LexNinjiaArt.ARASHI_CUT))queue(t,2,amount*.6F,4);
        if(active(LexNinjiaArt.SHADOW_CLONE_CROSS)){amount/=3;queue(t,2,amount);}
        if(active(LexNinjiaArt.PEA_SHOOTER))hit(h,t,peas*(fertilizer>0?2.5F:1));
        if(active(LexNinjiaArt.SNAKE_POISON_HAND)){String key="dealt_force_skills.lex_ninjia.snake_poison_stacks";t.getPersistentData().putInt(key,Math.min(9,t.getPersistentData().getInt(key)+(int)value(LexNinjiaArt.SNAKE_POISON_HAND,"max_triggers",3)));}
        if(whiteCrane && h.body().getMainHandItem().isEmpty()){whiteCrane=false;t.addEffect(new MobEffectInstance(MobEffects.LEVITATION,40,0),h.body());}
        return amount;
    }
    void cancel(){plan=null;targetId=null;input=delay=0;}
    void damagedTarget(OperativeHost h,LivingEntity target,float damage){
        if(damage>0 && h.isEnemy(target) && active(LexNinjiaArt.HAM_BERSERK))h.body().heal(damage*.5F);
    }
    void interrupt(){cancel();swordCharge=0;swordOrigin=null;}
    private void resolveOffhand(OperativeHost h){if(pendingOffhand!=null){previousOffhand=ItemStack.parseOptional(h.serverLevel().registryAccess(),pendingOffhand);pendingOffhand=null;}}
    private void restoreShield(OperativeHost h){
        resolveOffhand(h);
        var off=h.body().getOffhandItem();
        if(off.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("OperativeLexShield")){if(h.body().getUsedItemHand()==InteractionHand.OFF_HAND)h.body().stopUsingItem();h.body().setItemSlot(EquipmentSlot.OFFHAND,previousOffhand);}
        previousOffhand=ItemStack.EMPTY;
    }
    private void summonBeast(OperativeHost h){
        if(beast!=null && h.serverLevel().getEntity(beast)!=null)return;
        var wolf=EntityType.WOLF.create(h.serverLevel());if(wolf==null)return;
        wolf.setPos(h.body().position());wolf.setPersistenceRequired();
        wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(150);wolf.setHealth(150);
        wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,1500,99));wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,1500,2));
        var allegiance=new OperativeAllegiance();allegiance.capture(h.body());NativeDepartmentSkills.tagSummon(wolf,allegiance);
        wolf.getPersistentData().getCompound(NativeDepartmentSkills.SUMMON).putBoolean("LexBeast",true);
        wolf.getPersistentData().getCompound(NativeDepartmentSkills.SUMMON).putLong("Expires",h.serverLevel().getGameTime()+1500);
        if(h.serverLevel().addFreshEntity(wolf))beast=wolf.getUUID();
    }
    private void summonDeathGod(OperativeHost h,LivingEntity target){
        if(deathGod!=null && h.serverLevel().getEntity(deathGod)!=null)return;
        var skeleton=EntityType.SKELETON.create(h.serverLevel());if(skeleton==null)return;
        Vec3 spawn=h.body().position().add(1,0,1);
        if(!h.withinOrders(spawn) || !h.serverLevel().noCollision(skeleton,skeleton.getBoundingBox().move(spawn)))spawn=h.body().position();
        skeleton.setPos(spawn);skeleton.setPersistenceRequired();
        skeleton.setCustomName(net.minecraft.network.chat.Component.translatable("entity.dealt_force_skills.lex_ninjia.death_god"));
        skeleton.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);skeleton.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1000);skeleton.setHealth(100);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));
        skeleton.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));skeleton.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
        skeleton.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.LEATHER_LEGGINGS));skeleton.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
        var allegiance=new OperativeAllegiance();allegiance.capture(h.body());NativeDepartmentSkills.tagSummon(skeleton,allegiance);
        skeleton.getPersistentData().getCompound(NativeDepartmentSkills.SUMMON).putBoolean("LexSummon",true);skeleton.setTarget(target);
        if(h.serverLevel().addFreshEntity(skeleton))deathGod=skeleton.getUUID();
    }
    boolean interdict(OperativeHost h,LivingEntity attacker,LivingEntity target,float damage){
        double radius=value(LexNinjiaArt.NO_ONE_RETALIATES,"radius",45);
        if(!active(LexNinjiaArt.NO_ONE_RETALIATES) || attacker.distanceToSqr(h.body())>radius*radius || target.distanceToSqr(h.body())>radius*radius)return false;
        if(attacker==h.body()){effects.put(LexNinjiaArt.NO_ONE_RETALIATES,Math.max(0,effects.get(LexNinjiaArt.NO_ONE_RETALIATES)-(int)value(LexNinjiaArt.NO_ONE_RETALIATES,"caster_attack_duration_penalty_ticks",200)));return false;}
        emitting=true;
        try{attacker.invulnerableTime=0;SkillDamageHelper.hurtUnscaled(attacker,SkillDamageHelper.shakehandsReflect(h.serverLevel(),h.body(),h.body()),Math.max(0,damage)*(float)value(LexNinjiaArt.NO_ONE_RETALIATES,"reflect_multiplier",2));}
        finally{emitting=false;}
        return true;
    }
    void clear(OperativeHost h){
        interrupt();restoreShield(h);NativeLexTerrain.expire(h);
        if(overflow>0 && h.body().isAlive()){float debt=overflow+1;overflow=0;selfDamage(h,debt);}
        if(beast!=null && h.serverLevel().getEntity(beast)!=null)h.serverLevel().getEntity(beast).discard();
        if(deathGod!=null && h.serverLevel().getEntity(deathGod)!=null)h.serverLevel().getEntity(deathGod).discard();
        deathGod=null;beast=null;effects.clear();slashes.clear();overflow=0;pact=doubleHand=whiteCrane=false;
    }
    CompoundTag save(){var tag=new CompoundTag();for(var a:LexNinjiaArt.values()){if(active(a))tag.putInt("Effect_"+a.name(),effects.get(a));if(cooldowns.getOrDefault(a,0)>0)tag.putInt("Cooldown_"+a.name(),cooldowns.get(a));}
        tag.putInt("Reflect",reflect);tag.putInt("Sharpen",sharpen);tag.putInt("Peas",peas);tag.putInt("Cadence",cadence);
        tag.putFloat("Overflow",overflow);tag.putBoolean("Pact",pact);tag.putBoolean("DoubleHand",doubleHand);tag.putBoolean("WhiteCrane",whiteCrane);tag.putInt("Fertilizer",fertilizer);
        if(deathGod!=null)tag.putUUID("DeathGod",deathGod);
        if(beast!=null)tag.putUUID("Beast",beast);if(pendingOffhand!=null)tag.put("PreviousOffhand",pendingOffhand.copy());else if(!previousOffhand.isEmpty() && registryProvider!=null)tag.put("PreviousOffhand",previousOffhand.save(registryProvider));return tag;}
    void load(CompoundTag tag){interrupt();effects.clear();cooldowns.clear();slashes.clear();for(var a:LexNinjiaArt.values()){effects.put(a,Math.max(0,Math.min(12000,tag.getInt("Effect_"+a.name()))));cooldowns.put(a,Math.max(0,Math.min(cooldown(a),tag.getInt("Cooldown_"+a.name()))));}
        reflect=Math.max(0,Math.min(4,tag.getInt("Reflect")));sharpen=Math.max(0,Math.min(20,tag.getInt("Sharpen")));peas=Math.max(0,Math.min(5,tag.getInt("Peas")));cadence=Math.max(0,Math.min(40,tag.getInt("Cadence")));
        float debt=tag.getFloat("Overflow");overflow=Float.isFinite(debt)?Math.max(0,Math.min(1000000,debt)):0;
        pact=tag.getBoolean("Pact");doubleHand=tag.getBoolean("DoubleHand");whiteCrane=tag.getBoolean("WhiteCrane");fertilizer=Math.max(0,Math.min(600,tag.getInt("Fertilizer")));
        deathGod=tag.hasUUID("DeathGod")?tag.getUUID("DeathGod"):null;
        beast=tag.hasUUID("Beast")?tag.getUUID("Beast"):null;previousOffhand=ItemStack.EMPTY;pendingOffhand=tag.contains("PreviousOffhand",10)?tag.getCompound("PreviousOffhand").copy():null;}
    String describe(){return plan==null?"none":plan.id()+":"+input+"/"+plan.combo().size();}
}
