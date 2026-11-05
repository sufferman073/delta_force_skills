package com.rzy.dealt_force_skills.mixin;
import net.minecraft.world.item.DiggerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(DiggerItem.class)
public interface WorkbenchDiggerAccessor {
    @Accessor("blocks") net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> dfs$mineableBlocks();
}
