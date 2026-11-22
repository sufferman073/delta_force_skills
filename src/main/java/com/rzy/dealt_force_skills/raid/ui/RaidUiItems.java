package com.rzy.dealt_force_skills.raid.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import java.util.ArrayList;
import java.util.List;

public final class RaidUiItems {
    private RaidUiItems() { }
    static ItemStack button(Item item, String title, String... lines) {
        ItemStack display = describe(new ItemStack(item), title, lines);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, display, tag -> tag.putBoolean("RaidUiControl", true));
        return display;
    }
    public static boolean isControl(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains("RaidUiControl");
    }
    public static List<Component> hint(ItemStack stack) {
        var lines = new ArrayList<Component>(); lines.add(stack.getHoverName());
        lines.addAll(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        return lines;
    }
    static ItemStack describe(ItemStack original, String title, String... lines) {
        ItemStack display = original.copy(); display.set(DataComponents.CUSTOM_NAME, Component.literal(title));
        List<Component> lore = new ArrayList<>();
        for (String line : lines) lore.add(Component.literal(line));
        display.set(DataComponents.LORE, new ItemLore(lore));
        return display;
    }
}
