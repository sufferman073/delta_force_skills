package com.rzy.dealt_force_skills.workbench;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import java.util.*;

/** Only final signed allocations are saved: there is no bank of reusable modification points. */
public final class WorkbenchData {
    private WorkbenchData() { }
    public static final String KEY="dealt_force_skills.workbench";
    public static CompoundTag tag(ItemStack stack) {
        var root=stack.getTag();return root==null?new CompoundTag():root.getCompound(KEY);
    }
    public static ItemStack clean(ItemStack stack) {
        var copy=stack.copy();if(copy.hasTag())copy.getTag().remove(KEY);return copy;
    }
    public static void write(ItemStack stack,int[] allocation) {
        if(!valid(stack,allocation))throw new IllegalArgumentException("Invalid workbench allocation");
        var data=new CompoundTag();data.putInt("Version",1);var steps=new CompoundTag();
        for(var stat:WorkbenchStat.values())if(allocation[stat.ordinal()]!=0)steps.putInt(stat.key(),allocation[stat.ordinal()]);
        data.put("Steps",steps);stack.getOrCreateTag().put(KEY,data);
    }
    public static int steps(ItemStack stack,WorkbenchStat stat) {
        var data=tag(stack);
        if(data.getInt("Version")!=1||data.getCompound("Steps").getInt(stat.key())==0)return 0;
        int[] allocation=rawAllocation(stack);
        return valid(stack,allocation)?allocation[stat.ordinal()]:0;
    }
    private static int[] rawAllocation(ItemStack stack) {
        var data=tag(stack).getCompound("Steps");var out=new int[WorkbenchStat.values().length];
        for(var stat:WorkbenchStat.values())out[stat.ordinal()]=data.getInt(stat.key());return out;
    }
    public static int[] allocation(ItemStack stack) {
        int[] values=rawAllocation(stack);
        return tag(stack).getInt("Version")==1&&valid(stack,values)?values:new int[WorkbenchStat.values().length];
    }
    public static Set<String> types(ItemStack stack) {
        Set<String> out=new LinkedHashSet<>();var item=stack.getItem();
        if(item instanceof ArmorItem)out.add("armor");
        if(item instanceof ShieldItem)out.add("shield");
        if(item instanceof FishingRodItem)out.add("fishing");
        if(item instanceof DiggerItem||item instanceof ShearsItem||WorkbenchHarvest.isTool(stack))out.add("tool");
        if(item instanceof SwordItem)out.add("sword");
        if(item instanceof AxeItem)out.add("axe");
        if(item instanceof TridentItem)out.add("trident");
        if(item instanceof BowItem)out.add("bow");
        if(item instanceof CrossbowItem)out.add("crossbow");
        String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        if(item.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("mace")||id.equals("mace"))out.add("mace");
        if(WorkbenchPlatform.taczLoaded()&&WorkbenchTacz.isGun(stack))out.add("gun");
        boolean weapon=false;
        for(var entry:stack.getTags().toList()) {
            String path=entry.location().getPath();
            if(path.equals("weapons")||path.startsWith("weapons/"))weapon=true;
            for(String type:List.of("sword","axe","trident","bow","crossbow","mace","tool","armor","shield","fishing"))
                if(path.equals(type+"s")||path.equals("weapons/"+type+"s")||path.equals("tools/"+type+"s"))out.add(type);
        }
        if(weapon&&Collections.disjoint(out,Set.of("sword","axe","trident","bow","crossbow","mace","gun")))out.add("sword");
        if(out.contains("tool")||out.contains("sword")||out.contains("axe")||out.contains("trident")||out.contains("mace"))out.add("melee");
        if(out.contains("bow")||out.contains("crossbow")||out.contains("trident")){out.add("projectile");out.add("charged");}
        if(!out.isEmpty())out.add("all");return Collections.unmodifiableSet(out);
    }
    public static boolean available(ItemStack stack,WorkbenchStat stat) {
        if(!types(stack).contains(stat.group))return false;
        if(stat==WorkbenchStat.DURABILITY)return clean(stack).getMaxDamage()>0;
        if(stat==WorkbenchStat.PIERCING)return WorkbenchPlatform.enchantment(stack,"piercing")>0;
        if(stat==WorkbenchStat.MULTISHOT)return WorkbenchPlatform.enchantment(stack,"multishot")>0;
        if(stat==WorkbenchStat.IMPALING)return WorkbenchPlatform.enchantment(stack,"impaling")>0;
        return true;
    }
    public static double baseline(ItemStack stack,WorkbenchStat stat) {
        var original=clean(stack);var types=types(original);
        return switch(stat) {
            case DURABILITY -> original.getMaxDamage();
            case DAMAGE,ATTACK_SPEED,ENTITY_REACH,BLOCK_REACH,ARMOR,TOUGHNESS,KNOCKBACK -> WorkbenchPlatform.attribute(original,stat);
            case CRITICAL -> 1.5;
            case CHARGE_TIME -> types.contains("crossbow")?WorkbenchPlatform.crossbowTicks(original)/20.0:types.contains("trident")?.5:1;
            case MINING_SPEED -> original.getItem() instanceof TieredItem tiered?tiered.getTier().getSpeed():1;
            case HARVEST_LEVEL -> WorkbenchPlatform.harvestLevel(original);
            case PIERCING -> WorkbenchPlatform.enchantment(original,"piercing");
            case MULTISHOT -> 3;
            case IMPALING -> Math.max(1,WorkbenchPlatform.enchantment(original,"impaling"));
            case FISH_LURE -> WorkbenchPlatform.enchantment(original,"lure");
            case FISH_LUCK -> WorkbenchPlatform.enchantment(original,"luck_of_the_sea");
            case GUN_DAMAGE,GUN_RPM,GUN_RANGE,GUN_SPEED,GUN_WEIGHT,GUN_ADS,GUN_HEADSHOT,GUN_ARMOR -> WorkbenchPlatform.taczLoaded()?WorkbenchTacz.baseline(original,stat):0;
            default -> 1;
        };
    }
    public static int balance(int[] allocation) {
        if(allocation==null||allocation.length!=WorkbenchStat.values().length)return Integer.MIN_VALUE;
        long balance=0;for(int value:allocation){if(value < -1024||value>1024)return Integer.MIN_VALUE;balance+=value<0?-2L*value:-value;}
        return (int)balance;
    }
    public static boolean valid(ItemStack stack,int[] allocation) {
        if(stack.isEmpty()||stack.getCount()!=1||balance(allocation)<0)return false;
        for(var stat:WorkbenchStat.values()) {
            int value=allocation[stat.ordinal()];if(value==0)continue;
            if(!available(stack,stat))return false;
            double base=baseline(stack,stat);
            if(!Double.isFinite(base)||base<0||value < -stat.sacrificeLimit(base)||value>stat.improvementLimit(base))return false;
        }
        return !types(stack).isEmpty();
    }
    public static double factor(ItemStack stack,WorkbenchStat stat) {
        return Math.max(.05,Math.min(100,1+stat.change(1,steps(stack,stat))));
    }
}
