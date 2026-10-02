package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.BladeWireBlock;
import com.rzy.dealt_force_skills.block.HvkClonePrototypeBlock;
import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlock;
import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlock;
import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlock;
import com.rzy.dealt_force_skills.block.HvkEnchantInfusionBlock;
import com.rzy.dealt_force_skills.block.GtiSpecialOperationsBlock;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlock;
import com.rzy.dealt_force_skills.block.HvkAdvancedTreasureCompassBlock;
import com.rzy.dealt_force_skills.block.InterdimensionalBlock;
import com.rzy.dealt_force_skills.block.QuickCoverBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DealtForceSkillsMod.MODID);

    public static final DeferredHolder<Block, Block> BLADE_WIRE =
            BLOCKS.register("blade_wire", () -> new BladeWireBlock(false));

    public static final DeferredHolder<Block, Block> BLADE_WIRE_CORE =
            BLOCKS.register("blade_wire_core", () -> new BladeWireBlock(true));

    public static final DeferredHolder<Block, Block> QUICK_COVER =
            BLOCKS.register("quick_cover", QuickCoverBlock::new);

    public static final DeferredHolder<Block, Block> HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR =
            BLOCKS.register("hvk_advanced_standard_template_constructor",
                    HvkAdvancedStandardTemplateConstructorBlock::new);

    public static final DeferredHolder<Block, Block> HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER =
            BLOCKS.register("hvk_advanced_disassembly_beam_emitter",
                    HvkAdvancedDisassemblyBeamEmitterBlock::new);
    public static final DeferredHolder<Block, Block> HVK_ADVANCED_SPAWNER =
            BLOCKS.register("hvk_advanced_spawner", HvkAdvancedSpawnerBlock::new);
    public static final DeferredHolder<Block, Block> HVK_ASARA_ARENA =
            BLOCKS.register("hvk_asara_arena", HvkAsaraArenaBlock::new);
    public static final DeferredHolder<Block, Block> HVK_ENCHANT_INFUSION =
            BLOCKS.register("hvk_enchant_infusion", HvkEnchantInfusionBlock::new);
    public static final DeferredHolder<Block, Block> GTI_SPECIAL_OPERATIONS =
            BLOCKS.register("gti_special_operations", GtiSpecialOperationsBlock::new);

    public static final DeferredHolder<Block, Block> HVK_CLONE_PROTOTYPE =
            BLOCKS.register("hvk_clone_prototype", HvkClonePrototypeBlock::new);

    public static final DeferredHolder<Block, Block> HVK_ADVANCED_TREASURE_COMPASS =
            BLOCKS.register("hvk_advanced_treasure_compass", HvkAdvancedTreasureCompassBlock::new);

    public static final DeferredHolder<Block, Block> INTERDIMENSIONAL_BLOCK =
            BLOCKS.register("interdimensional_block", InterdimensionalBlock::new);
}
