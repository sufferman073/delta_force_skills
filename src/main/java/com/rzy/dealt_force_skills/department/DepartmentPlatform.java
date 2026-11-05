package com.rzy.dealt_force_skills.department;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        EnchantmentHelper.getEnchantments(stack).forEach((enchantment,level)->name.append(" · ").append(enchantment.getFullname(level)));
        return name;
    }
    public static String encodeText(net.minecraft.network.chat.Component text) { return net.minecraft.network.chat.Component.Serializer.toJson(text); }
    public static net.minecraft.network.chat.Component decodeText(String text) { return java.util.Objects.requireNonNull(net.minecraft.network.chat.Component.Serializer.fromJson(text)); }

    public static CompoundTag save(ServerPlayer player, ItemStack stack) { return stack.save(new CompoundTag()); }
    public static ItemStack load(ServerPlayer player, CompoundTag tag) { return ItemStack.of(tag); }
    public static Map<String,Integer> enchantments(ServerPlayer player, ItemStack stack) {
        Map<String,Integer> result = new LinkedHashMap<>();
        EnchantmentHelper.getEnchantments(stack).forEach((enchantment, level) ->
                result.put(BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString(), level));
        return result;
    }
    public static List<EnchantmentSpec> enchantments(ServerPlayer player) {
        List<EnchantmentSpec> result = new ArrayList<>();
        for (var enchantment : BuiltInRegistries.ENCHANTMENT)
            result.add(new EnchantmentSpec(BuiltInRegistries.ENCHANTMENT.getKey(enchantment).toString(),
                    Math.max(1, Math.min(64, enchantment.getMaxLevel())),
                    enchantment.isCurse() ? "curse" : enchantment.isTreasureOnly() ? "treasure" : "normal"));
        return result;
    }
    public static ItemStack book(ServerPlayer player, Map<String,Integer> enchantments) {
        ItemStack stack = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        Map<net.minecraft.world.item.enchantment.Enchantment,Integer> values = new LinkedHashMap<>();
        enchantments.forEach((id,level) -> {
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key != null && BuiltInRegistries.ENCHANTMENT.containsKey(key)) values.put(BuiltInRegistries.ENCHANTMENT.get(key), level);
        });
        EnchantmentHelper.setEnchantments(values, stack);
        return stack;
    }
}
