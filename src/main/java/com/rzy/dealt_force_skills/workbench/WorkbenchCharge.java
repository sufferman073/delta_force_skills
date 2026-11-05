package com.rzy.dealt_force_skills.workbench;
import net.minecraft.world.item.ItemStack;
public final class WorkbenchCharge {
    private WorkbenchCharge() { }
    public static int elapsed(ItemStack stack,int ticks,int original) {
        int steps=WorkbenchData.steps(stack,WorkbenchStat.CHARGE_TIME);
        if(steps==0||ticks<=0)return ticks;
        int duration=Math.max(1,original-steps*2);
        return (int)Math.min(Integer.MAX_VALUE,(long)ticks*original/duration);
    }
}
