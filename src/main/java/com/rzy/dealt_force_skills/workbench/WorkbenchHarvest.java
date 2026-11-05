package com.rzy.dealt_force_skills.workbench;
import net.minecraft.world.item.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
public final class WorkbenchHarvest {
    private WorkbenchHarvest() { }
    public static boolean isTool(ItemStack stack) {
        return stack.canPerformAction(ToolActions.PICKAXE_DIG)||stack.canPerformAction(ToolActions.AXE_DIG)
            ||stack.canPerformAction(ToolActions.SHOVEL_DIG)||stack.canPerformAction(ToolActions.HOE_DIG);
    }
    public static int baseLevel(ItemStack stack) {
        if(stack.getItem() instanceof TieredItem item)return item.getTier().getLevel();
        // Call the item's native check: the stack check also applies workbench allocations.
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.OBSIDIAN.defaultBlockState()))return 3;
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.GOLD_ORE.defaultBlockState()))return 2;
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.IRON_ORE.defaultBlockState()))return 1;
        return 0;
    }
    private static boolean category(ItemStack stack,BlockState state,boolean original) {
        if(original)return true;
        if(stack.getItem() instanceof DiggerItem digger
            &&state.is(((com.rzy.dealt_force_skills.mixin.WorkbenchDiggerAccessor)digger).dfs$mineableBlocks()))return true;
        return (stack.canPerformAction(ToolActions.PICKAXE_DIG)&&state.is(BlockTags.MINEABLE_WITH_PICKAXE))
            ||(stack.canPerformAction(ToolActions.AXE_DIG)&&state.is(BlockTags.MINEABLE_WITH_AXE))
            ||(stack.canPerformAction(ToolActions.SHOVEL_DIG)&&state.is(BlockTags.MINEABLE_WITH_SHOVEL))
            ||(stack.canPerformAction(ToolActions.HOE_DIG)&&state.is(BlockTags.MINEABLE_WITH_HOE));
    }
    public static boolean correct(ItemStack stack,BlockState state,boolean original) {
        if(!(stack.getItem() instanceof DiggerItem)&&!isTool(stack))return original;
        int steps=WorkbenchData.steps(stack,WorkbenchStat.HARVEST_LEVEL);
        if(steps==0)return original;
        if(!category(stack,state,original))return false;
        Tier[] tiers={Tiers.WOOD,Tiers.STONE,Tiers.IRON,Tiers.DIAMOND,Tiers.NETHERITE};
        Tier originalTier=stack.getItem() instanceof TieredItem item?item.getTier():tiers[Math.min(4,baseLevel(WorkbenchData.clean(stack)))];
        // An existing rejection at an adequate tier is an independent tool restriction.
        if(!original&&net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(originalTier,state))return false;
        int target=Math.max(0,originalTier.getLevel()+steps);
        Tier chosen=originalTier;
        var sorted=net.minecraftforge.common.TierSortingRegistry.getSortedTiers();
        if(net.minecraftforge.common.TierSortingRegistry.isTierSorted(originalTier)) {
            int index=sorted.indexOf(originalTier),direction=Integer.signum(steps);
            for(int remaining=Math.abs(steps);remaining>0;remaining--) {
                int next=index+direction;
                while(next>=0&&next<sorted.size()&&sorted.get(next).getLevel()==sorted.get(index).getLevel())next+=direction;
                if(next<0||next>=sorted.size())break;
                index=next;
            }
            chosen=sorted.get(index);
        } else {
            chosen=tiers[Math.min(4,target)];
        }
        return net.minecraftforge.common.TierSortingRegistry.isCorrectTierForDrops(chosen,state);
    }
}
