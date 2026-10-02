package mccfk.DeltaForce.java.client;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.screen.ClientScreenRegistry;
import mccfk.DeltaForce.java.client.screen.DfsConfigProvider;
import mccfk.DeltaForce.java.client.screen.DfsSkillHudScreen;
import mccfk.DeltaForce.java.client.screen.GroundItemHudScreen;
import mccfk.DeltaForce.java.client.screen.HudElementType;
import mccfk.DeltaForce.java.client.screen.LayoutEntry;
import mccfk.DeltaForce.java.client.screen.SettingsScreen;
import mccfk.DeltaForce.java.client.screen.StaminaBarHudScreen;
import mccfk.DeltaForce.java.item.creative.DeltaForceCreativeTabs;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * DeltaForce 客户端专用初始化
 * 与 DeltaForce 主类隔离，避免服务端类加载时触发 RuntimeDistCleaner
 */
public final class DeltaForceClientSetup {

    private DeltaForceClientSetup() {
    }

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        // 注册配置界面入口（NeoForge 模组列表"Config"按钮 → 打开设置界面）
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (minecraft, parent) -> new SettingsScreen(parent));

        // 注册自定义掉落物配置
        CustomDropConfig.bootstrap();
        DeltaForce.registerConfigProvider(DeltaForce.MODID, new CustomDropConfigProvider());

        // 注册核心模组配置（dealt_force_skills），联网时同步服务端配置
        DeltaForce.registerConfigProvider("dealt_force_skills", new DfsConfigProvider());

        // 注册设置界面UI状态配置（独立配置，不显示在设置界面中）
        SettingsUIConfig.bootstrap();

        // 注册DF创造模式标签页
        DeltaForceCreativeTabs.register(modEventBus);

        // 注册布局配置系统的可编辑界面
        registerLayoutScreens();
    }

    /** 向布局配置系统注册所有可编辑的界面/HUD条目及其预定义元素 */
    private static void registerLayoutScreens() {
        // 仅注册有真实视觉对应、可编辑位置的HUD元素
        // Screen类界面（主界面/设置/背包等）由Screen自身渲染，无需编辑器占位

        // DFS 技能按钮（真实渲染）
        ClientScreenRegistry.register("dfs.skill_hud",
                Component.translatable("screen.deltaforce.dfs_skill_buttons"),
                DfsSkillHudScreen.class,
                makeElements(
                        el("custom_box", "skill_buttons", 0.25f, 0.90f, 0f, 0f, 0.22f, 0.06f)));

        // DFS 体力条（真实渲染）
        ClientScreenRegistry.register("dfs.stamina_hud",
                Component.translatable("screen.deltaforce.dfs_stamina_bar"),
                StaminaBarHudScreen.class,
                makeElements(
                        el("health", "stamina_bar", 0.55f, 0.91f, 0f, 0f, 0.085f, 0.013f)));

        // 地面物品信息面板（真实渲染）
        ClientScreenRegistry.register("deltaforce.ground_item_hud",
                Component.translatable("screen.deltaforce.ground_item_panel"),
                GroundItemHudScreen.class,
                makeElements(
                        el("custom_box", "ground_item_panel", 0.72f, 0.11f, 0f, 0f, 0.18f, 0.15f)));
    }

    /** 创建单个默认布局元素 */
    private static LayoutEntry el(String typeId, String suffix,
            float anchorX, float anchorY,
            float offsetX, float offsetY,
            float relW, float relH) {
        HudElementType ht = HudElementType.fromId(typeId);
        LayoutEntry e = new LayoutEntry();
        e.id = typeId + "_" + suffix;
        // 直接用中文名称，避免语言管理器未加载时显示翻译键
        e.displayName = switch (typeId) {
            case "health" -> "血量条";
            case "custom_box" -> "区域";
            case "title_label" -> "标题";
            case "player_inventory" -> "背包";
            default -> ht.displayName().getString();
        };
        e.anchorX = anchorX;
        e.anchorY = anchorY;
        e.offsetX = offsetX;
        e.offsetY = offsetY;
        e.relWidth = relW;
        e.relHeight = relH;
        e.enabled = true;
        e.scaleX = 1.0f;
        e.scaleY = 1.0f;
        return e;
    }

    /** 创建默认元素列表 */
    @SafeVarargs
    private static List<LayoutEntry> makeElements(LayoutEntry... entries) {
        List<LayoutEntry> list = new ArrayList<>();
        for (LayoutEntry e : entries)
            list.add(e);
        return list;
    }

}