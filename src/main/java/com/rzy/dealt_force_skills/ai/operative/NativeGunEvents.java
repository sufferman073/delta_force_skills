package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.chamber.ChamberGunKind;
import com.rzy.dealt_force_skills.character.chamber.ChamberTaczEnhancement;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.skill.SkillDamageHelper;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bullet identity survives the last round being stowed; late allies are protected at impact. */
@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class NativeGunEvents {
    private static final String KIND="dealt_force_skills.operative_chamber_gun";
    private static final String HEALTH="dealt_force_skills.operative_chamber_hit_health";
    private NativeGunEvents(){ }
    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide || !(event.getEntity() instanceof EntityKineticBullet bullet)
                || !(bullet.getOwner() instanceof OperativeHost bot) || !(bot.skills() instanceof NativeChamberSkills skills))return;
        var kind=ChamberTaczEnhancement.kind(bot.body().getMainHandItem());
        if(kind!=ChamberGunKind.NONE && skills.equipped()==kind)bullet.getPersistentData().putString(KIND,kind.name());
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void protect(EntityHurtByGunEvent.Pre event){
        if(!(event.getAttacker() instanceof OperativeHost bot) || bot.body().level().isClientSide
                || !(event.getHurtEntity() instanceof LivingEntity target))return;
        if(bot instanceof com.rzy.dealt_force_skills.compat.maid.MaidSkillHost && bot.skills()==null)return;
        if(!bot.isEnemy(target)){event.setCanceled(true);return;}
        if(!(bot.skills() instanceof NativeChamberSkills) || !event.getBullet().getPersistentData().contains(KIND))return;
        event.setHeadshotMultiplier(event.getHeadshotMultiplier()*5);
        if(ChamberGunKind.TOUR_DE_FORCE.name().equals(event.getBullet().getPersistentData().getString(KIND))){
            var source=SkillDamageHelper.trueDamage(bot.serverLevel(),event.getBullet(),bot.body());
            event.setDamageSource(GunDamageSourcePart.NON_ARMOR_PIERCING,source);
            event.setDamageSource(GunDamageSourcePart.ARMOR_PIERCING,source);
            event.getBullet().getPersistentData().putFloat(HEALTH,target.getHealth()+target.getAbsorptionAmount());
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(EntityHurtByGunEvent.Post event){
        if(event.getHurtEntity() instanceof LivingEntity target)finishHit(event.getAttacker(),target,event.getBullet(),event.isHeadShot());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void killed(EntityKillByGunEvent event){
        // TaCZ emits Kill instead of Hurt.Post when this bullet is lethal.
        finishHit(event.getAttacker(),event.getKilledEntity(),event.getBullet(),event.isHeadShot());
    }
    private static void finishHit(LivingEntity attacker,LivingEntity target,net.minecraft.world.entity.Entity bullet,boolean headshot){
        if(attacker instanceof OperativeEntity bot) OperativeHitFeedback.emit(bot,target,bullet,headshot);
        if(attacker instanceof net.minecraft.server.level.ServerPlayer && target.isAlive()) attacker.setLastHurtMob(target);
        if(attacker instanceof OperativeHost bot && !bot.body().level().isClientSide
                && bot.skills() instanceof NativeChamberSkills skills
                && ChamberGunKind.TOUR_DE_FORCE.name().equals(bullet.getPersistentData().getString(KIND))
                && bullet.getPersistentData().getFloat(HEALTH)>target.getHealth()+target.getAbsorptionAmount())skills.sniperHit(bot,target);
        bullet.getPersistentData().remove(HEALTH);
    }
    @SubscribeEvent
    public static void drops(LivingDropsEvent event){
        if(event.getEntity() instanceof OperativeEntity)
            for(var item:event.getDrops()) OperativeGun.preparePlayerTransfer(item.getItem());
        if(event.getEntity() instanceof OperativeEntity bot && bot.skills() instanceof NativeChamberSkills chamber){
            event.getDrops().removeIf(item->ChamberTaczEnhancement.isChamberWeapon(item.getItem()));
            var carried=chamber.takeCarried(bot);
            if(!carried.isEmpty())event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(bot.level(),bot.getX(),bot.getY(),bot.getZ(),carried));
        }
    }
}
