package com.rzy.dealt_force_skills.compat.sophisticated;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;

public final class AutoSellUpgradeContainer extends UpgradeContainerBase<AutoSellUpgradeItem.Wrapper, AutoSellUpgradeContainer> {
    public static final UpgradeContainerType<AutoSellUpgradeItem.Wrapper, AutoSellUpgradeContainer> TYPE =
            new UpgradeContainerType<>(AutoSellUpgradeContainer::new);
    private final FilterLogicContainer<FilterLogic> filter;

    public AutoSellUpgradeContainer(Player player, int id, AutoSellUpgradeItem.Wrapper wrapper,
                                   UpgradeContainerType<AutoSellUpgradeItem.Wrapper, AutoSellUpgradeContainer> type) {
        super(player, id, wrapper, type);
        filter = new FilterLogicContainer<>(() -> upgradeWrapper.filter(), this, slots::add);
        if (player instanceof ServerPlayer serverPlayer) wrapper.bindOwner(serverPlayer);
    }

    public FilterLogicContainer<FilterLogic> filter() { return filter; }
    @Override public void handlePacket(CompoundTag data) {
        filter.handlePacket(data);
        if (upgradeWrapper.filter().isAllowList()) upgradeWrapper.filter().setAllowList(false);
    }

    public static void register() {
        UpgradeContainerRegistry.register(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "auto_sell_upgrade"), TYPE);
    }
}
