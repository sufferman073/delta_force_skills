package com.rzy.dealt_force_skills.raid.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.BooleanSupplier;

/** Paged, withdrawal-only real container view; no inventory copies own the loot. */
public final class RaidPageMenu extends ChestMenu {
    private final ServerPlayer owner;
    private final Container source;
    private final String title;
    private final BooleanSupplier validity;
    private final Runnable changed;
    private final int page;
    private RaidPageMenu(int id, Inventory inventory, Container source, String title, int page,
                         BooleanSupplier validity, Runnable changed) {
        super(MenuType.GENERIC_9x6, id, inventory, new SimpleContainer(54), 6);
        this.owner = (ServerPlayer) inventory.player; this.source = source; this.title = title;
        this.page = page; this.validity = validity; this.changed = changed;
        // ChestMenu creates fixed visual coordinates. Replace only its data slots with direct source slots.
        for (int i = 0; i < 45; i++) {
            int index = page * 45 + i;
            if (index >= source.getContainerSize()) continue;
            Slot previous = slots.get(i);
            Slot direct = new Slot(source, index, previous.x, previous.y) {
                @Override public boolean mayPlace(ItemStack item) { return false; }
                @Override public void setChanged() { super.setChanged(); changed.run(); }
            };
            direct.index = i; slots.set(i, direct);
        }
        getContainer().setItem(45, RaidUiItems.button(Items.ARROW, "上一页"));
        getContainer().setItem(49, RaidUiItems.button(Items.PAPER, "第 " + (page + 1) + " / " + pages(source) + " 页"));
        getContainer().setItem(53, RaidUiItems.button(Items.ARROW, "下一页"));
    }
    public static void open(ServerPlayer player, Container contents, String title,
                            BooleanSupplier stillValid, Runnable changed) {
        open(player, contents, title, 0, stillValid, changed);
    }
    private static void open(ServerPlayer player, Container contents, String title, int page,
                             BooleanSupplier validity, Runnable changed) {
        int selected = Math.max(0, Math.min(pages(contents) - 1, page));
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new RaidPageMenu(id, inventory, contents, title, selected, validity, changed), Component.literal(title)));
    }
    private static int pages(Container source) { return Math.max(1, (source.getContainerSize() + 44) / 45); }
    @Override public boolean stillValid(Player player) { return player == owner && validity.getAsBoolean(); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (slot >= 45 && slot < 54) {
            if (type == ClickType.PICKUP && (slot == 45 || slot == 53))
                open(owner, source, title, page + (slot == 45 ? -1 : 1), validity, changed);
            return;
        }
        if (slot >= 0 && slot < 45 && page * 45 + slot >= source.getContainerSize()) return;
        if (type == ClickType.CLONE || type == ClickType.SWAP || type == ClickType.QUICK_CRAFT || type == ClickType.PICKUP_ALL) return;
        super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= 45 || page * 45 + index >= source.getContainerSize()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy(), remaining = slot.getItem();
        if (!moveItemStackTo(remaining, 54, slots.size(), true)) return ItemStack.EMPTY;
        if (remaining.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, remaining); changed.run();
        return original;
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
}
