package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;

public final class HvkGeneSample {
    public static final String TAG_ENTITY = "HvkGeneSampleEntity";

    private HvkGeneSample() {
    }

    public static ItemStack create(ResourceLocation entityId, String entityName) {
        ItemStack stack = new ItemStack(ModItems.GLASS_SYRINGE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_ENTITY, entityId == null ? "minecraft:pig" : entityId.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.dealt_force_skills.hvk_gene_sample",
                entityName == null || entityName.isBlank() ? tag.getString(TAG_ENTITY) : entityName)
                .withStyle(ChatFormatting.AQUA));
        return stack;
    }

    public static boolean isFor(ItemStack stack, ResourceLocation entityId) {
        ResourceLocation stored = entityId(stack);
        return stored != null && stored.equals(entityId);
    }

    public static ResourceLocation entityId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != ModItems.GLASS_SYRINGE.get()) {
            return null;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String value = tag.getString(TAG_ENTITY);
        if (value.isBlank()) {
            return null;
        }
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
