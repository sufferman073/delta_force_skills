package com.rzy.dealt_force_skills.workbench;

/** One signed step always costs one point; a negative step yields two points. */
public enum WorkbenchStat {
    DURABILITY("耐久度",.01,true,false,"all"),
    DAMAGE("攻击伤害",.5,false,false,"melee"),
    ATTACK_SPEED("攻击速度",.2,false,false,"melee"),
    ENTITY_REACH("攻击距离",.2,false,false,"melee"),
    CRITICAL("暴击倍率",.1,false,false,"melee"),
    CHARGE_TIME("蓄力/装填/拉弓时间",.1,false,true,"charged"),
    MINING_SPEED("破坏效率",.05,true,false,"tool"),
    BLOCK_REACH("交互距离",.2,false,false,"tool"),
    HARVEST_LEVEL("采集等级",1,false,false,"tool"),
    SWEEP("横扫伤害",.1,true,false,"sword"),
    SMASH("下落增伤",.1,true,false,"mace"),
    BOW_DAMAGE("基础命中伤害",.1,true,false,"bow"),
    BOW_FULL_DAMAGE("满蓄力命中伤害",.1,true,false,"bow"),
    PROJECTILE_SPEED("弹射物速度",.1,true,false,"projectile"),
    PIERCING("穿透目标",1,false,false,"crossbow"),
    MULTISHOT("多重射击个数",1,false,false,"crossbow"),
    IMPALING("穿刺增伤",.5,true,false,"trident"),
    SHIELD_COOLDOWN("破盾冷却",.1,true,true,"shield"),
    FISH_PULL("拉扯力度",.1,true,false,"fishing"),
    FISH_LURE("饵钓等级",1,false,false,"fishing"),
    FISH_LUCK("战利品品质",1,false,false,"fishing"),
    ARMOR("护甲值",1,false,false,"armor"),
    TOUGHNESS("护甲韧性",1,false,false,"armor"),
    KNOCKBACK("击退抗性",.1,false,false,"armor"),
    GUN_DAMAGE("枪械伤害",.5,false,false,"gun"),
    GUN_RPM("射速",.05,true,false,"gun"),
    GUN_RANGE("有效射程",.05,true,false,"gun"),
    GUN_SPEED("弹速",.05,true,false,"gun"),
    GUN_WEIGHT("重量",.05,true,true,"gun"),
    GUN_ADS("开镜时间",.05,true,true,"gun"),
    GUN_HEADSHOT("爆头倍率",.1,true,false,"gun"),
    GUN_ARMOR("护甲穿透倍率",.05,true,false,"gun");
    public final String title,group;
    public final double step;
    public final boolean relative,inverse;
    WorkbenchStat(String title,double step,boolean relative,boolean inverse,String group) {
        this.title=title;this.step=step;this.relative=relative;this.inverse=inverse;this.group=group;
    }
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
    public double change(double baseline,int steps){return (inverse?-1:1)*steps*step*(relative?baseline:1);}
    public int sacrificeLimit(double baseline) {
        if(!Double.isFinite(baseline)||baseline<=0)return 0;
        return Math.min(1024,(int)Math.floor((relative?.75:baseline*.75)/step+1.0e-8));
    }
    public int improvementLimit(double baseline) {
        if(this==PIERCING)return Math.max(0,127-(int)baseline);
        // Limit entity fan-out; the UI shows the exact permitted range.
        if(this==MULTISHOT)return 16;
        if(this==KNOCKBACK)return Math.max(0,(int)Math.floor((1-baseline)/step+1.0e-8));
        if(this==GUN_ARMOR)return baseline>0?Math.max(0,(int)Math.floor((1/baseline-1)/step+1.0e-8)):0;
        if(this==GUN_RPM)return baseline>0?Math.max(0,Math.min(1024,(int)Math.floor((6000/baseline-1)/step+1.0e-8))):0;
        if(!inverse)return 1024;
        return Math.max(0,Math.min(1024,(int)Math.floor((relative?.95:Math.max(0,baseline-.05))/step+1.0e-8)));
    }
}
