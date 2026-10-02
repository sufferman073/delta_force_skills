package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    /**
     * Blocks that are not vanilla-climbable ({@link net.minecraft.tags.BlockTags#CLIMBABLE}) on their own,
     * but are eligible to be turned into climbable "chains" by the HVK Universal Glue item (fences, fence
     * gates and chains). Combine with {@code BlockTags.CLIMBABLE} for the full eligibility check.
     */
    public static final TagKey<Block> HVK_CLIMBABLE = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "hvk_climbable"));

    private ModBlockTags() {
    }
}
