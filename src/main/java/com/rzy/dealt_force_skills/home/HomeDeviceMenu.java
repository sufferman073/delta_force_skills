package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

final class HomeDeviceMenu extends ChestMenu {
    private final ServerPlayer user;
    private final HomeDevice device;
    private int page;
    private String nextEnergy;
    private HomeDeviceMenu(int id, ServerPlayer user, HomeDevice device) {
        super(MenuType.GENERIC_9x6, id, user.getInventory(), new SimpleContainer(54), 6);
        this.user = user; this.device = device; refresh();
    }
    static void open(ServerPlayer user, HomeDevice device) {
        user.openMenu(new SimpleMenuProvider((id, inventory, player) -> new HomeDeviceMenu(id, user, device), Component.literal("个人维度方块 · 配置")));
    }
    private static Component fluidName(net.minecraft.world.level.material.Fluid fluid) {
        String fallback = BuiltInRegistries.FLUID.getKey(fluid).getPath().replace('_', ' ');
        return Component.translatableWithFallback(fluid.getFluidType().getDescriptionId(new net.neoforged.neoforge.fluids.FluidStack(fluid, 1000)), fallback);
    }
    private void label(int slot, Item icon, String title) { label(slot, icon, Component.literal(title)); }
    private void label(int slot, Item icon, Component title) {
        ItemStack stack = new ItemStack(icon == null || icon == Items.AIR ? Items.GLASS_BOTTLE : icon);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, title);
        getContainer().setItem(slot, stack);
    }
    private void refresh() {
        getContainer().clearContent();
        label(4, Items.COMPASS, "等级 " + device.tier() + " · 普通右键往返家园");
        if (device.tier() == 0) return;
        label(10, Items.RED_DYE, "− 半径"); label(11, Items.SPYGLASS, "球形半径：" + device.radius + " / " + device.maxRadius()); label(12, Items.LIME_DYE, "+ 半径");
        label(14, Items.RED_DYE, "− 倍速"); label(15, Items.CLOCK, "总倍速：" + device.speed + " / " + device.maxSpeed() + "（0 关闭加速）"); label(16, Items.LIME_DYE, "+ 倍速");
        if (device.tier() < 4) return;
        HomeEnergy.Type selectedEnergy = device.tier() == 5 ? HomeEnergy.find(device.energyId) : null;
        Component energyName = selectedEnergy == null ? Component.translatable("home.dealt_force_skills.unavailable", device.energyId) : selectedEnergy.name();
        var price = Component.literal(device.payer == null ? "开启付费填充" : "关闭付费填充");
        if (device.tier() == 5 && device.energy) price.append(Component.translatable("home.dealt_force_skills.unit_cost", energyName));
        else price.append(Component.translatable("home.dealt_force_skills.fluid_fill_cost"));
        label(18, Items.BUCKET, price);
        label(19, Items.GOLD_INGOT, "你的余额：" + SpecialOperationsBaseManager.havocCoins(user));
        if (device.tier() < 5) return;
        var units = HomeEnergy.available();
        var next = device.energy && selectedEnergy != null ? units.get((units.indexOf(selectedEnergy) + 1) % units.size())
                : selectedEnergy != null ? selectedEnergy : HomeEnergy.find(HomeEnergy.FE);
        nextEnergy = next.id();
        label(21, device.energy ? Items.REDSTONE : device.selected.getBucket(), Component.translatable("home.dealt_force_skills.selected", device.energy ? energyName : fluidName(device.selected)));
        label(23, Items.REDSTONE_BLOCK, Component.translatable("home.dealt_force_skills.select_energy", next.name()));
        label(25, Items.ARROW, "上一页"); label(26, Items.ARROW, "下一页");
        for (int index = 0; index < 27 && page * 27 + index < HomeDevice.FluidsList.ALL.size(); index++) {
            var fluid = HomeDevice.FluidsList.ALL.get(page * 27 + index);
            label(27 + index, fluid.getBucket(), fluidName(fluid));
        }
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || button != 0 || (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) || device.tier() == 0) return;
        if (device.payer != null && !device.payer.equals(user.getUUID())) { user.displayClientMessage(Component.literal("仅开启付费填充的玩家可更改配置"), true); return; }
        int step = type == ClickType.QUICK_MOVE ? 4 : 1;
        switch (slot) {
            case 10, 12 -> device.configure(device.radius + (slot == 12 ? step : -step), device.speed);
            case 14, 16 -> device.configure(device.radius, device.speed + (slot == 16 ? step : -step));
            case 18 -> { if (device.tier() >= 4) device.payer = device.payer == null ? user.getUUID() : null; }
            case 23 -> { if (device.tier() == 5 && nextEnergy != null) { device.energyId = nextEnergy; device.energy = true; } }
            case 25 -> page = Math.max(0, page - 1);
            case 26 -> page = Math.min((HomeDevice.FluidsList.ALL.size() - 1) / 27, page + 1);
            default -> { int index = page * 27 + slot - 27;
                if (device.tier() == 5 && slot >= 27 && slot < 54 && index < HomeDevice.FluidsList.ALL.size()) { device.selected = HomeDevice.FluidsList.ALL.get(index); device.energy = false; }
            }
        }
        device.setChanged(); refresh(); broadcastChanges();
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player == user && !device.isRemoved() && user.level() == device.getLevel() && user.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(device.getBlockPos())) <= 64 && HomeManager.canInteract(user, device.getBlockPos());
    }
}
