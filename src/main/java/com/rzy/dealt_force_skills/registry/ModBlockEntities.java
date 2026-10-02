package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.BladeWireBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedSpawnerBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAsaraArenaBlockEntity;
import com.rzy.dealt_force_skills.block.HvkEnchantInfusionBlockEntity;
import com.rzy.dealt_force_skills.block.GtiSpecialOperationsBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedTreasureCompassBlockEntity;
import com.rzy.dealt_force_skills.block.HvkClonePrototypeBlockEntity;
import com.rzy.dealt_force_skills.block.InterdimensionalBlockEntity;
import com.rzy.dealt_force_skills.block.QuickCoverBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DealtForceSkillsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BladeWireBlockEntity>> BLADE_WIRE =
            BLOCK_ENTITIES.register("blade_wire",
                    () -> BlockEntityType.Builder.of(
                            BladeWireBlockEntity::new,
                            ModBlocks.BLADE_WIRE.get(),
                            ModBlocks.BLADE_WIRE_CORE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuickCoverBlockEntity>> QUICK_COVER =
            BLOCK_ENTITIES.register("quick_cover",
                    () -> BlockEntityType.Builder.of(
                            QuickCoverBlockEntity::new,
                            ModBlocks.QUICK_COVER.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkAdvancedStandardTemplateConstructorBlockEntity>>
            HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR =
            BLOCK_ENTITIES.register("hvk_advanced_standard_template_constructor",
                    () -> BlockEntityType.Builder.of(
                            HvkAdvancedStandardTemplateConstructorBlockEntity::new,
                            ModBlocks.HVK_ADVANCED_STANDARD_TEMPLATE_CONSTRUCTOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkAdvancedDisassemblyBeamEmitterBlockEntity>>
            HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER =
            BLOCK_ENTITIES.register("hvk_advanced_disassembly_beam_emitter",
                    () -> BlockEntityType.Builder.of(
                            HvkAdvancedDisassemblyBeamEmitterBlockEntity::new,
                            ModBlocks.HVK_ADVANCED_DISASSEMBLY_BEAM_EMITTER.get()
                    ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkAdvancedSpawnerBlockEntity>> HVK_ADVANCED_SPAWNER =
            BLOCK_ENTITIES.register("hvk_advanced_spawner",
                    () -> BlockEntityType.Builder.of(HvkAdvancedSpawnerBlockEntity::new,
                            ModBlocks.HVK_ADVANCED_SPAWNER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkAsaraArenaBlockEntity>> HVK_ASARA_ARENA =
            BLOCK_ENTITIES.register("hvk_asara_arena",
                    () -> BlockEntityType.Builder.of(HvkAsaraArenaBlockEntity::new,
                            ModBlocks.HVK_ASARA_ARENA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkEnchantInfusionBlockEntity>> HVK_ENCHANT_INFUSION =
            BLOCK_ENTITIES.register("hvk_enchant_infusion",
                    () -> BlockEntityType.Builder.of(HvkEnchantInfusionBlockEntity::new,
                            ModBlocks.HVK_ENCHANT_INFUSION.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GtiSpecialOperationsBlockEntity>> GTI_SPECIAL_OPERATIONS =
            BLOCK_ENTITIES.register("gti_special_operations",
                    () -> BlockEntityType.Builder.of(GtiSpecialOperationsBlockEntity::new,
                            ModBlocks.GTI_SPECIAL_OPERATIONS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkClonePrototypeBlockEntity>>
            HVK_CLONE_PROTOTYPE =
            BLOCK_ENTITIES.register("hvk_clone_prototype",
                    () -> BlockEntityType.Builder.of(
                            HvkClonePrototypeBlockEntity::new,
                            ModBlocks.HVK_CLONE_PROTOTYPE.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HvkAdvancedTreasureCompassBlockEntity>>
            HVK_ADVANCED_TREASURE_COMPASS =
            BLOCK_ENTITIES.register("hvk_advanced_treasure_compass",
                    () -> BlockEntityType.Builder.of(
                            HvkAdvancedTreasureCompassBlockEntity::new,
                            ModBlocks.HVK_ADVANCED_TREASURE_COMPASS.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InterdimensionalBlockEntity>>
            INTERDIMENSIONAL_BLOCK =
            BLOCK_ENTITIES.register("interdimensional_block",
                    () -> BlockEntityType.Builder.of(
                            InterdimensionalBlockEntity::new,
                            ModBlocks.INTERDIMENSIONAL_BLOCK.get()
                    ).build(null));
}
