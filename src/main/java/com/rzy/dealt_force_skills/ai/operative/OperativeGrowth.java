package com.rzy.dealt_force_skills.ai.operative;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

/** Bounded purchased growth; experience and other uncapped progression stay at level zero. */
public final class OperativeGrowth {
    private static final net.minecraft.resources.ResourceLocation COMMANDER_SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "operative_commander_speed");
    private static final net.minecraft.resources.ResourceLocation ID=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","operative_bounded_growth");
    private OperativeGrowth() { }
    public static int level(com.rzy.dealt_force_skills.shop.UndeadShopEntry entry) { return entry.isUnlimited()?0:Math.max(0,entry.maxLevel()); }
    public static float healing() { return 1+level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.VITALITY)*.01F; }
    public static float energyCost() { return Math.max(0,1-level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.CRAFT)*.02F); }
    public static float undeadDamage() { return 1+level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.WEAPON_POLISH)*.1F+level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.STRENGTH)*.04F; }
    public static int gene() { return com.rzy.dealt_force_skills.character.commander.CommanderUpgrade.GENE.maxLevel(); }
    public static boolean commander(net.minecraft.world.entity.Entity entity) { return entity instanceof OperativeHost bot && bot.skills() instanceof NativeCommanderSkills; }
    public static double commanderEffect() { return 2*Math.pow(1.25,com.rzy.dealt_force_skills.character.commander.CommanderUpgrade.STRENGTH.maxLevel()); }
    public static int duration(net.minecraft.world.entity.Entity source,int base) { return commander(source) && base>0 ? (int)Math.min(Integer.MAX_VALUE,Math.ceil(base*commanderEffect())):base; }
    public static int amplifier(net.minecraft.world.entity.Entity source,int base) { return commander(source) && base>=0 ? duration(source,base+1)-1:base; }
    public static double radius(net.minecraft.world.entity.Entity source,double base) { return commander(source) && base>0?base*commanderEffect():base; }
    public static double falloff(net.minecraft.world.entity.Entity source,double base) { return commander(source) && base>0?base*.5:base; }
    /** Beyond the configured zero-damage boundary the visual blast has no collision damage. */
    public static double damageRadius(net.minecraft.world.entity.Entity source,double base,String profile) {
        double radius=radius(source,base);
        double rate=falloff(source,com.rzy.dealt_force_skills.config.SkillBalance.falloffPerBlock(profile,.1));
        double floor=com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(profile+".minimum_damage_multiplier",0);
        double full=Math.max(0,com.rzy.dealt_force_skills.config.DealtForceConfig.doubleValue(profile+".full_damage_radius",0));
        return floor>0 || rate<=0?radius:Math.min(radius,full+1/rate);
    }
    public static boolean branch(net.minecraft.world.entity.Entity source,com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade upgrade) {
        return commander(source) || source instanceof net.minecraft.world.entity.player.Player player
                && com.rzy.dealt_force_skills.character.commander.CommanderStateManager.hasBranchUpgrade(player,upgrade);
    }
    public static boolean hostile(LivingEntity source, LivingEntity target) {
        return source instanceof OperativeHost host ? host.isEnemy(target)
                : source instanceof net.minecraft.world.entity.player.Player player
                && com.rzy.dealt_force_skills.util.TargetingUtil.isHostileLivingFor(player, target);
    }
    public static int cooldownStep(OperativeHost h) { return h.body() instanceof OperativeHost bot && bot.skills() instanceof NativeCommanderSkills?(int)Math.round(1/Math.max(.1,.5-com.rzy.dealt_force_skills.character.commander.CommanderUpgrade.EFFICIENCY.maxLevel()*.1)):1; }
    public static float commanderStrength() { return (float)(2*Math.pow(1.25,com.rzy.dealt_force_skills.character.commander.CommanderUpgrade.STRENGTH.maxLevel())*(1+gene()*.1)); }
    public static void initialize(com.rzy.dealt_force_skills.entity.OperativeEntity bot) {
        OperativeSettings.apply(bot);
        if(bot.skills() instanceof NativeUndeadSkills)undead(bot);
        if(bot.skills() instanceof NativeCommanderSkills)applyCommanderGrowth(bot);
        bot.setHealth(bot.getMaxHealth());
    }
    public static void applyCommanderGrowth(LivingEntity body) {
        var speed = body.getAttribute(Attributes.MOVEMENT_SPEED);
        double amount = OperativeSettings.value("commander_speed_multiplier") - 1;
        if (body instanceof com.rzy.dealt_force_skills.entity.OperativeEntity && speed != null && (speed.getModifier(COMMANDER_SPEED) == null || speed.getModifier(COMMANDER_SPEED).amount() != amount)) {
            speed.removeModifier(COMMANDER_SPEED);
            speed.addTransientModifier(new AttributeModifier(COMMANDER_SPEED, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        apply(body,Attributes.MAX_HEALTH,gene()*.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(body,Attributes.MOVEMENT_SPEED,gene()*.04,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        apply(body,Attributes.LUCK,gene(),AttributeModifier.Operation.ADD_VALUE);
    }
    public static void undead(LivingEntity body) {
        apply(body, Attributes.MAX_HEALTH, level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.VITALITY)*.04);
        apply(body, Attributes.MOVEMENT_SPEED, level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.AGILITY)*.02);
        apply(body, Attributes.ARMOR, level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.ARMOR_FORGING)*.05);
        apply(body, Attributes.ARMOR_TOUGHNESS, level(com.rzy.dealt_force_skills.shop.UndeadShopEntry.ARMOR_FORGING)*.05);
    }
    private static void apply(LivingEntity body, net.minecraft.core.Holder<Attribute> attribute, double amount) {
        apply(body,attribute,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    private static void apply(LivingEntity body, net.minecraft.core.Holder<Attribute> attribute, double amount,AttributeModifier.Operation operation) {
        var instance = body.getAttribute(attribute);
        if (instance != null && instance.getModifier(ID) == null)
            instance.addTransientModifier(new AttributeModifier(ID, amount,
                    operation));
    }
    public static void clear(LivingEntity body) {
        if (body.getAttribute(Attributes.MOVEMENT_SPEED) != null) body.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(COMMANDER_SPEED);
        for (var attribute : java.util.List.of(Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED,
                Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,Attributes.LUCK)) {
            var instance = body.getAttribute(attribute);
            if (instance != null) instance.removeModifier(ID);
        }
        body.setHealth(Math.min(body.getHealth(), body.getMaxHealth()));
    }
}
