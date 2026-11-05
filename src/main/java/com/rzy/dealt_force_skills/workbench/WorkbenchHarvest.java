package com.rzy.dealt_force_skills.workbench;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
public final class WorkbenchHarvest {
    private WorkbenchHarvest() { }
    private static final Tier[] TIERS={Tiers.WOOD,Tiers.STONE,Tiers.IRON,Tiers.DIAMOND,Tiers.NETHERITE};
    public static boolean isTool(ItemStack stack) {
        return stack.has(DataComponents.TOOL)||stack.canPerformAction(ItemAbilities.PICKAXE_DIG)
            ||stack.canPerformAction(ItemAbilities.AXE_DIG)||stack.canPerformAction(ItemAbilities.SHOVEL_DIG)
            ||stack.canPerformAction(ItemAbilities.HOE_DIG);
    }
    private static TagKey<Block> tierTag(ItemStack stack) {
        if(stack.getItem() instanceof TieredItem item)return item.getTier().getIncorrectBlocksForDrops();
        var tool=stack.get(DataComponents.TOOL);
        if(tool!=null)for(var rule:tool.rules())
            if(rule.correctForDrops().isPresent()&&!rule.correctForDrops().get())
                for(var tier:new Tier[]{Tiers.WOOD,Tiers.GOLD,Tiers.STONE,Tiers.IRON,Tiers.DIAMOND,Tiers.NETHERITE})
                    if(rule.blocks().unwrapKey().map(tier.getIncorrectBlocksForDrops()::equals).orElse(false))return tier.getIncorrectBlocksForDrops();
        return null;
    }
    public static int baseLevel(ItemStack stack) {
        var tag=tierTag(stack);
        if(tag!=null) {
            for(int i=TIERS.length-1;i>=0;i--)if(tag.equals(TIERS[i].getIncorrectBlocksForDrops()))return i;
            if(!Blocks.OBSIDIAN.defaultBlockState().is(tag))return 3;
            if(!Blocks.GOLD_ORE.defaultBlockState().is(tag))return 2;
            if(!Blocks.IRON_ORE.defaultBlockState().is(tag))return 1;
            return 0;
        }
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.OBSIDIAN.defaultBlockState()))return 3;
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.GOLD_ORE.defaultBlockState()))return 2;
        if(stack.getItem().isCorrectToolForDrops(stack,Blocks.IRON_ORE.defaultBlockState()))return 1;
        return 0;
    }
    private static boolean actionCategory(ItemStack stack,BlockState state) {
        return (stack.canPerformAction(ItemAbilities.PICKAXE_DIG)&&state.is(BlockTags.MINEABLE_WITH_PICKAXE))
            ||(stack.canPerformAction(ItemAbilities.AXE_DIG)&&state.is(BlockTags.MINEABLE_WITH_AXE))
            ||(stack.canPerformAction(ItemAbilities.SHOVEL_DIG)&&state.is(BlockTags.MINEABLE_WITH_SHOVEL))
            ||(stack.canPerformAction(ItemAbilities.HOE_DIG)&&state.is(BlockTags.MINEABLE_WITH_HOE));
    }
    public static boolean correct(ItemStack stack,BlockState state,boolean original) {
        if(!isTool(stack))return original;
        int steps=WorkbenchData.steps(stack,WorkbenchStat.HARVEST_LEVEL);
        if(steps==0)return original;
        var ownTag=tierTag(stack);
        boolean category=original||actionCategory(stack,state);
        var tool=stack.get(DataComponents.TOOL);
        if(tool!=null)for(var rule:tool.rules()) {
            if(rule.correctForDrops().isEmpty()||!state.is(rule.blocks()))continue;
            if(ownTag!=null&&rule.blocks().unwrapKey().map(ownTag::equals).orElse(false))continue;
            // Respect the component's first matching category/custom restriction.
            if(!rule.correctForDrops().get())return false;
            category=true;
            break;
        }
        if(!category)return false;
        int base=baseLevel(WorkbenchData.clean(stack));
        if(!original&&(ownTag!=null?!state.is(ownTag):!state.is(TIERS[base].getIncorrectBlocksForDrops())))return false;
        int level=Math.max(0,Math.min(4,base+steps));
        return !state.is(TIERS[level].getIncorrectBlocksForDrops());
    }
}
