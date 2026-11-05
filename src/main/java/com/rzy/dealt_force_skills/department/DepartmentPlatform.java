package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small loader/version boundary for saved item data and enchantment registries. */
public final class DepartmentPlatform {
    public record EnchantmentSpec(String id, int maximum, String kind) { }
    private DepartmentPlatform() { }

    public static net.minecraft.network.chat.Component itemName(ServerPlayer player,ItemStack stack) {
        var name=stack.getHoverName().copy();
        for(var entry:EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet())
            name.append(" · ").append(net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(),entry.getIntValue()));
        return name;
    }
    public static String encodeText(net.minecraft.network.chat.Component text) {
        return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,text).getOrThrow().toString();
    }
    public static net.minecraft.network.chat.Component decodeText(String text) {
        return net.minecraft.network.chat.ComponentSerialization.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,new com.google.gson.JsonParser().parse(text)).getOrThrow();
    }

    public static CompoundTag save(ServerPlayer player, ItemStack stack) { return (CompoundTag) stack.save(player.registryAccess()); }
    public static ItemStack load(ServerPlayer player, CompoundTag tag) { return ItemStack.parseOptional(player.registryAccess(),tag); }
    public static Map<String,Integer> enchantments(ServerPlayer player, ItemStack stack) {
        Map<String,Integer> result = new LinkedHashMap<>();
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet())
            entry.getKey().unwrapKey().ifPresent(key -> result.put(key.location().toString(), entry.getIntValue()));
        return result;
    }
    public static List<EnchantmentSpec> enchantments(ServerPlayer player) {
        List<EnchantmentSpec> result = new ArrayList<>();
        var registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for (var enchantment : registry.holders().toList())
            result.add(new EnchantmentSpec(enchantment.key().location().toString(),
                    Math.max(1, Math.min(64, enchantment.value().getMaxLevel())),
                    enchantment.is(EnchantmentTags.CURSE) ? "curse" : enchantment.is(EnchantmentTags.TREASURE) ? "treasure" : "normal"));
        return result;
    }
    public static ItemStack book(ServerPlayer player, Map<String,Integer> enchantments) {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        var registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        EnchantmentHelper.updateEnchantments(stack, mutable -> enchantments.forEach((id, level) -> {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null) registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, key)).ifPresent(holder -> mutable.set(holder, level));
        }));
        return stack;
    }
}
