package com.rzy.dealt_force_skills.raid.ui;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RaidPanelMenu extends ChestMenu {
    public enum Page { MAPS, PREP, STORAGE, EQUIPMENT, WAREHOUSE, PURCHASES, ENTITLEMENTS;
        public MenuType<RaidPanelMenu> type;
    }
    public static final int PREP_STORAGE_BASE = 90;
    public final Page pageKind;
    public static void openPanel(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.MenuProvider provider) {
        if (player.containerMenu instanceof RaidPanelMenu) player.doCloseContainer();
        player.openMenu(provider);
    }
    protected RaidPanelMenu(Page page, int id, Inventory inventory) {
        super(page.type, id, inventory, new SimpleContainer(54), 6);
        this.pageKind = page;
        if (page == Page.PREP || page == Page.PURCHASES) {
            SimpleContainer storage = new SimpleContainer(54);
            for (int index = 0; index < storage.getContainerSize(); index++) addSlot(new Slot(storage, index, 0, 0));
            if (page == Page.PREP) for (int index = 36; index < 41; index++) addSlot(new Slot(inventory, index, 0, 0));
        }
    }
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.MENU, helper -> {
            for (Page page : Page.values()) {
                page.type = new MenuType<>((id, inventory) -> new RaidPanelMenu(page, id, inventory), FeatureFlags.VANILLA_SET);
                helper.register(ResourceLocation.tryParse("dealt_force_skills:raid_" + page.name().toLowerCase(java.util.Locale.ROOT)), page.type);
            }
        });
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
