package com.rzy.dealt_force_skills.raid.ui;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RaidUiItems {
    private RaidUiItems() { }
    static ItemStack button(Item item, String title, String... lines) {
        ItemStack display = describe(new ItemStack(item), title, lines);
        display.getOrCreateTag().putBoolean("RaidUiControl", true);
        return display;
    }
    public static boolean isControl(ItemStack stack) { return stack.hasTag() && stack.getTag().getBoolean("RaidUiControl"); }
    public static java.util.List<Component> hint(ItemStack stack) {
        var lines = new java.util.ArrayList<Component>(); lines.add(stack.getHoverName());
        var display = stack.getTagElement("display");
        if (display != null) {
            var lore = display.getList("Lore", net.minecraft.nbt.Tag.TAG_STRING);
            for (int index = 0; index < lore.size(); index++) {
                Component line = Component.Serializer.fromJson(lore.getString(index));
                if (line != null) lines.add(line);
            }
        }
        return lines;
    }
    static ItemStack describe(ItemStack original, String title, String... lines) {
        ItemStack display = original.copy(); display.setHoverName(Component.literal(title));
        ListTag lore = new ListTag();
        for (String line : lines) lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        display.getOrCreateTagElement("display").put("Lore", lore);
        return display;
    }
}
