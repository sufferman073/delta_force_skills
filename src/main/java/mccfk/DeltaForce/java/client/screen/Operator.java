package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import com.rzy.dealt_force_skills.network.C2S_SelectCharacter;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 干员选择栏（特战干员界面底栏上方）
 * 干员定义已统一合并到 friend 模组角色目录（ModCharacters），本界面直接使用 friend 角色。
 * 布局：按 textures/gui/operator 下的文件夹分组（原版四类 + boss/特殊/瓦罗兰特），
 * 分类标题 + 细线 → 该分类干员图片 | 分类标题 + 细线 → 该分类干员图片 ...
 * 点击头像先变白（待确认），再点一次才确认选择；点击其他干员会切换待确认高亮。
 * 选中干员头像右上角画圆点；支持滚轮与拖拽滑动查看超出屏幕的干员。
 * 底部固定滑动示意条显示滚动位置。
 * 干员头像贴图路径：deltaforce:textures/gui/operator/&lt;文件夹&gt;/&lt;角色短id&gt;.png
 */
public class Operator {

    // === 干员栏布局常量 ===
    private static final int BAR_HEIGHT = 84;             // 干员栏总高度
    private static final int BAR_LEFT = 20;               // 左边缘留白
    private static final int BAR_RIGHT = 20;              // 右边缘留白
    private static final int SLIDER_H = 3;                // 滑动示意条高度
    private static final int SLIDER_BOTTOM_OFFSET = 6;    // 示意条距底栏顶部间距

    // === 组标题行 ===
    private static final int GROUP_HEADER_H = 16;         // 组标题行高度（含细线）
    private static final int CONTENT_TOP = 18;            // 干员内容区距栏顶（标题行之下）

    // === 干员块（仅头像，无名字）===
    private static final int OP_ICON_W = 46;              // 头像绘制宽（原图132×1.4≈46）
    private static final int OP_ICON_H = 60;              // 头像绘制高（原图173×1.4≈60）
    private static final int OP_BLOCK_W = 54;             // 干员块宽（头像+左右间距）

    // === 分隔线 ===
    private static final int DIVIDER_W = 2;

    // === 分组定义（文件夹名 + 标题翻译键）===
    private record GroupDef(String folder, String titleKey) {
    }

    // === 分组顺序（对应 textures/gui/operator 下的文件夹：原版四类 + boss/特殊/瓦罗兰特）===
    private static final List<GroupDef> GROUP_ORDER = List.of(
            new GroupDef("assault", "character.dealt_force_skills.role.assault"),
            new GroupDef("support", "character.dealt_force_skills.role.support"),
            new GroupDef("engineer", "character.dealt_force_skills.role.engineer"),
            new GroupDef("recon", "character.dealt_force_skills.role.recon"),
            new GroupDef("boss", "character.dealt_force_skills.role.boss"),
            new GroupDef("special", "character.dealt_force_skills.role.special"),
            new GroupDef("valorant", "character.dealt_force_skills.role.valorant")
    );

    // === 角色预览大图资源（角色id → 原始分辨率）===
    private static final Map<String, PreviewAsset> PREVIEWS = Map.ofEntries(
            Map.entry("dealt_force_skills:assault/wildfire", new PreviewAsset(454, 288)),
            Map.entry("dealt_force_skills:special/corps", new PreviewAsset(538, 288)),
            Map.entry("dealt_force_skills:assault/d_wolf", new PreviewAsset(279, 288)),
            Map.entry("dealt_force_skills:assault/nox", new PreviewAsset(382, 288)),
            Map.entry("dealt_force_skills:assault/tempest", new PreviewAsset(244, 288)),
            Map.entry("dealt_force_skills:assault/vyron", new PreviewAsset(273, 288)),
            Map.entry("dealt_force_skills:boss/commander", new PreviewAsset(399, 288)),
            Map.entry("dealt_force_skills:boss/ghroth", new PreviewAsset(570, 288)),
            Map.entry("dealt_force_skills:boss/saeed", new PreviewAsset(254, 288)),
            Map.entry("dealt_force_skills:engineer/gizmo", new PreviewAsset(364, 288)),
            Map.entry("dealt_force_skills:engineer/n_two", new PreviewAsset(294, 288)),
            Map.entry("dealt_force_skills:engineer/shepherd", new PreviewAsset(230, 288)),
            Map.entry("dealt_force_skills:engineer/sineva", new PreviewAsset(254, 230)),
            Map.entry("dealt_force_skills:engineer/uluru", new PreviewAsset(514, 288)),
            Map.entry("dealt_force_skills:recon/hackclaw", new PreviewAsset(438, 288)),
            Map.entry("dealt_force_skills:recon/luna", new PreviewAsset(568, 288)),
            Map.entry("dealt_force_skills:recon/morse", new PreviewAsset(290, 288)),
            Map.entry("dealt_force_skills:recon/raptor", new PreviewAsset(515, 288)),
            Map.entry("dealt_force_skills:special/catdad", new PreviewAsset(371, 288)),
            Map.entry("dealt_force_skills:special/department_of_transportation", new PreviewAsset(631, 288)),
            Map.entry("dealt_force_skills:special/gambler", new PreviewAsset(675, 288)),
            Map.entry("dealt_force_skills:special/lex_ninjia", new PreviewAsset(442, 288)),
            Map.entry("dealt_force_skills:special/manba", new PreviewAsset(182, 288)),
            Map.entry("dealt_force_skills:special/nikaidou_hiro", new PreviewAsset(512, 288)),
            Map.entry("dealt_force_skills:special/nikaidou_hiro_witchification", new PreviewAsset(521, 288)),
            Map.entry("dealt_force_skills:special/undead", new PreviewAsset(567, 288)),
            Map.entry("dealt_force_skills:support/stinger", new PreviewAsset(223, 288)),
            Map.entry("dealt_force_skills:support/rover", new PreviewAsset(226, 228)),
            Map.entry("dealt_force_skills:support/toxik", new PreviewAsset(284, 288)),
            Map.entry("dealt_force_skills:support/vlinder", new PreviewAsset(236, 288)),
            Map.entry("dealt_force_skills:valorant/chamber", new PreviewAsset(580, 288))
    );

    // 待确认选择的干员ID：第一次点击头像变白，再点一次才确认选择
    private static String pendingCharacterId = null;

    // 点击当前已选干员的“闪白”截止时间（毫秒）：点击当前干员时短暂变白后自动恢复
    private static long currentSelectedFlashUntil = 0L;

    // === 滚动状态（客户端内存，跨打开保留）===
    private static int scrollOffset = 0;                  // 当前滚动偏移（像素）
    private static int maxScroll = 0;                     // 最大可滚动范围
    private static int lastDragX = -1;                    // 拖拽起始X（-1表示未拖拽）
    private static int dragStartScroll = 0;               // 拖拽开始时的滚动偏移

    // 悬浮的角色ID（hover 高亮用）
    private static String hoveredOperatorId = null;

    // === 干员介绍文本 ===
    private static final Map<String, IntroText> introCache = new HashMap<>();
    private static boolean detailExpanded = false;
    private static String lastDetailCharacterId = null;

    // 详情按钮区域（由渲染时计算，供点击检测用）
    private static int detailBtnX = 0;
    private static int detailBtnY = 0;
    private static int detailBtnW = 60;
    private static int detailBtnH = 20;

    // 介绍文本滚动
    private static int introScrollOffset = 0;
    private static int introMaxScroll = 0;
    private static int introTextPanelX = 0;
    private static int introTextPanelY = 0;
    private static int introTextPanelW = 0;
    private static int introTextPanelH = 0;
    private static final int INTRO_SCROLL_BAR_W = 4;
    private static final int INTRO_SCROLL_BAR_COLOR = 0x60AAAAAA;
    private static final int INTRO_SCROLL_HANDLE_COLOR = 0xAAFFFFFF;

    // 介绍文本面板布局常量
    private static final int INTRO_PANEL_LEFT_RATIO = 9;  // 左面板占屏幕宽度 9/20
    private static final int INTRO_PANEL_LEFT_MARGIN_RATIO = 2;  // 左面板距左边缘 = 屏幕宽度 * 2 / 100
    private static final int INTRO_PANEL_RIGHT_MARGIN_RATIO = 2;  // 右面板距右边缘 = 屏幕宽度 * 2 / 100
    private static final int INTRO_PANEL_TOP = 40;        // 面板距上边缘（固定值，避开标签栏）
    private static final int INTRO_PANEL_BOTTOM_MARGIN = 8;
    private static final int INTRO_PANEL_PADDING = 12;
    private static final int INTRO_PANEL_BG = 0xC0101A24;
    private static final int INTRO_BTN_BG = 0xFF2A3550;
    private static final int INTRO_BTN_HOVER = 0xFF3D4F6F;

    /**
     * 介绍文本数据结构
     */
    private record IntroText(String brief, String full) {}

    /**
     * 不死人子角色介绍数据结构
     */
    private record SubIntroText(String fileKey, String name, String brief, String full) {}

    /** 不死人子角色文件名列表 */
    private static final List<String> UNDEAD_SUB_FILES = List.of(
            "hunter", "explorer", "scholar", "rogue", "warrior", "knight"
    );

    /** 不死人子角色介绍缓存 */
    private static List<SubIntroText> undeadSubIntros = null;

    /** 不死人子角色详情展开状态（fileKey → 是否展开） */
    private static final Map<String, Boolean> undeadSubDetailExpanded = new HashMap<>();

    /** 不死人子角色详情按钮区域列表（供点击检测） */
    private static final List<UndeadDetailBtn> undeadDetailBtns = new ArrayList<>();

    private static class UndeadDetailBtn {
        int x, y, w, h;
        String fileKey;
    }

    private Operator() {
    }

    /**
     * 渲染干员选择栏（仅特战干员界面调用）
     */
    public static void render(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        var font = Minecraft.getInstance().font;
        int barTop = screenHeight - 20 - BAR_HEIGHT;
        int viewW = screenWidth - BAR_LEFT - BAR_RIGHT;
        int contentY = barTop + CONTENT_TOP;
        hoveredOperatorId = null;

        // 点击当前已选干员的闪白计时到期后自动恢复（仅短暂变白，不进入待确认流程）
        if (pendingCharacterId != null
                && ClientCharacterSelectionState.isSelectedCharacter(pendingCharacterId)
                && System.currentTimeMillis() >= currentSelectedFlashUntil) {
            pendingCharacterId = null;
        }

        List<DisplayGroup> groups = displayGroups();

        // 计算内容总宽：每组(组标题+该组干员+组间分隔线)
        int contentW = 0;
        for (DisplayGroup group : groups) {
            contentW += GROUP_HEADER_H + group.characters().size() * OP_BLOCK_W + DIVIDER_W;
        }
        contentW = Math.max(0, contentW - DIVIDER_W); // 尾部不留分隔线
        contentW += BAR_RIGHT; // 尾部余量

        // 滚动范围与边界钳制
        maxScroll = Math.max(0, contentW - viewW);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        // === 第一步：绘制干员栏所有内容（不受边界限制）===
        int x = BAR_LEFT - scrollOffset;
        for (int t = 0; t < groups.size(); t++) {
            DisplayGroup group = groups.get(t);
            List<CharacterDefinition> chars = group.characters();

            // 计算该分类总宽：组标题 + 该组干员（不含分隔线）
            int groupW = GROUP_HEADER_H + chars.size() * OP_BLOCK_W;

            // 组标题行：分类名居中于组标题块 + 下方细线
            int groupStartX = x;
            int groupEndX = x + groupW;
            drawGroupHeaderLabel(guiGraphics, font, group.label(),
                    x, barTop, groupStartX, groupEndX, screenWidth);
            x += GROUP_HEADER_H;

            // 该组干员（头像）
            for (CharacterDefinition character : chars) {
                int blockX = x;
                drawOperator(guiGraphics, font, blockX, contentY, character,
                        ClientCharacterSelectionState.isSelectedCharacter(character.id()));
                // hover 检测（仅头像区域）
                int iconX = blockX + (OP_BLOCK_W - OP_ICON_W) / 2;
                if (mouseX >= iconX && mouseX < iconX + OP_ICON_W &&
                    mouseY >= contentY && mouseY < contentY + OP_ICON_H) {
                    hoveredOperatorId = character.id();
                }
                x += OP_BLOCK_W;
            }

            // 组间分隔线
            if (t < groups.size() - 1) {
                int dividerY = contentY + 4;
                guiGraphics.fill(x, dividerY, x + DIVIDER_W, dividerY + OP_ICON_H - 4, 0x33FFFFFF);
                x += DIVIDER_W;
            }
        }

        // === 第二步：绘制左右遮罩（覆盖超出边界的部分）===
        // 左遮罩：从屏幕左边缘到 BAR_LEFT
        if (BAR_LEFT > 0) {
            guiGraphics.fill(0, barTop, BAR_LEFT, barTop + BAR_HEIGHT, 0xFF0E1819);  // 使用背景色
        }
        // 右遮罩：从 screenWidth - BAR_RIGHT 到屏幕右边缘
        int rightMaskX = screenWidth - BAR_RIGHT;
        if (rightMaskX < screenWidth) {
            guiGraphics.fill(rightMaskX, barTop, screenWidth, barTop + BAR_HEIGHT, 0xFF0E1819);  // 使用背景色
        }

        // 底部滑动示意条（在遮罩之上）
        drawSlider(guiGraphics, screenWidth, screenHeight, viewW, contentW);
        if(hoveredOperatorId!=null) {
            CharacterDefinition hovered=getOperatorAt(mouseX,mouseY,screenWidth,screenHeight);
            if(hovered!=null&&!ClientCharacterSelectionState.canSelect(hovered)) {
                var description=ClientCharacterSelectionState.blockedDescription(hovered);
                guiGraphics.renderTooltip(font,font.split(description,300),mouseX,mouseY);
            }
        }
    }

    /**
     * 绘制组标题行（通用）：给定标题组件 + 下方白色细线
     */
    private static void drawGroupHeaderLabel(GuiGraphics g, Font font, Component label, int x, int barTop, int groupStartX, int groupEndX, int screenWidth) {
        int labelX = x + (GROUP_HEADER_H - font.width(label)) / 2;
        int labelY = barTop + (GROUP_HEADER_H - font.lineHeight) / 2;
        g.drawString(font, label, labelX, labelY, 0xFFFFFFFF, false);

        // 细线（从组起始位置横跨到该分类最右干员的右边缘，不包含分隔线）
        int lineY = barTop + GROUP_HEADER_H - 2;
        // 裁剪：只显示在可见区域内的部分（被左右遮罩覆盖）
        int clipStart = Math.max(groupStartX, BAR_LEFT);
        int clipEnd = Math.min(groupEndX, screenWidth - BAR_RIGHT);
        if (clipEnd > clipStart) {
            g.fill(clipStart, lineY, clipEnd, lineY + 1, 0xCCFFFFFF);
        }
    }

    /**
     * 绘制单个干员：头像贴图 + 待确认变白 + 选中圆点
     * 无对应贴图时回退为深色底框 + 居中角色名
     */
    private static void drawOperator(GuiGraphics g, Font font, int x, int contentY, CharacterDefinition character, boolean selected) {
        int iconX = x + (OP_BLOCK_W - OP_ICON_W) / 2;
        ResourceLocation tex = operatorTexture(character);
        if (tex != null) {
            g.blit(tex, iconX, contentY, 0, 0, OP_ICON_W, OP_ICON_H, OP_ICON_W, OP_ICON_H);
        } else {
            // 无贴图回退：深色底框 + 细边框 + 居中角色名
            g.fill(iconX, contentY, iconX + OP_ICON_W, contentY + OP_ICON_H, 0xFF1B222A);
            g.fill(iconX, contentY, iconX + OP_ICON_W, contentY + 1, 0x33FFFFFF);
            g.fill(iconX, contentY + OP_ICON_H - 1, iconX + OP_ICON_W, contentY + OP_ICON_H, 0x33FFFFFF);
            g.fill(iconX, contentY, iconX + 1, contentY + OP_ICON_H, 0x33FFFFFF);
            g.fill(iconX + OP_ICON_W - 1, contentY, iconX + OP_ICON_W, contentY + OP_ICON_H, 0x33FFFFFF);
            String name = font.plainSubstrByWidth(character.displayName().getString(), OP_ICON_W - 6);
            g.drawCenteredString(font, name, iconX + OP_ICON_W / 2,
                    contentY + OP_ICON_H / 2 - font.lineHeight / 2, 0xFFFFFFFF);
        }

        boolean selectable = ClientCharacterSelectionState.canSelect(character);

        // 待确认高亮：变白一点（第一次点击后）
        if (selectable && character.id().equals(pendingCharacterId)) {
            g.fill(iconX, contentY, iconX + OP_ICON_W, contentY + OP_ICON_H, 0x70FFFFFF);
        }

        // hover 高亮
        if (selectable && character.id().equals(hoveredOperatorId)) {
            g.fill(iconX, contentY, iconX + OP_ICON_W, contentY + OP_ICON_H, 0x30FFFFFF);
        }

        // 选中圆点（头像右上角：外圈深色 + 内绿色）
        if (selected) {
            int dotSize = 12;
            int dotX = iconX + OP_ICON_W - dotSize - 4;
            int dotY = contentY + 4;
            g.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, 0xFF202020);
            int inner = 8;
            int inX = dotX + (dotSize - inner) / 2;
            int inY = dotY + (dotSize - inner) / 2;
            g.fill(inX, inY, inX + inner, inY + inner, 0xFF00FF00);  // 绿色
        }

        if (!selectable) {
            g.fill(iconX, contentY, iconX + OP_ICON_W, contentY + OP_ICON_H, 0xD0000000);
        }
    }

    /**
     * 绘制底部滑动示意条（轨道+滑块）
     */
    private static void drawSlider(GuiGraphics g, int screenWidth, int screenHeight, int viewW, int contentW) {
        int trackX = BAR_LEFT;
        int trackY = screenHeight - 20 - SLIDER_BOTTOM_OFFSET;
        int trackW = viewW;

        // 轨道
        g.fill(trackX, trackY, trackX + trackW, trackY + SLIDER_H, 0x33FFFFFF);

        // 滑块
        int thumbW = (maxScroll <= 0) ? trackW : Math.max(24, trackW * trackW / contentW);
        int thumbX = (maxScroll <= 0) ? trackX
                : trackX + (int) ((float) scrollOffset / maxScroll * (trackW - thumbW));
        g.fill(thumbX, trackY, thumbX + thumbW, trackY + SLIDER_H, 0xCCFFFFFF);
    }

    // ========== 数据源 ==========

    /**
     * 按 textures/gui/operator 文件夹分组：原版四类 + boss/特殊/瓦罗兰特。
     * 数据包/附加包自定义目录（不在内置分组中）追加在最后。
     * 仅显示有对应贴图的角色（无贴图的不显示）。
     */
    private static List<DisplayGroup> displayGroups() {
        List<CharacterDefinition> all = ClientCharacterSelectionState.selectionCatalog().stream()
                .flatMap(branch -> branch.characters().stream())
                .toList();

        List<DisplayGroup> groups = new ArrayList<>();
        Set<String> knownFolders = new LinkedHashSet<>();
        for (GroupDef def : GROUP_ORDER) {
            knownFolders.add(def.folder);
            // 只保留有贴图的角色
            List<CharacterDefinition> chars = all.stream()
                    .filter(c -> folderOf(c).equals(def.folder))
                    .filter(c -> operatorTexture(c) != null)
                    .toList();
            if (!chars.isEmpty()) {
                groups.add(new DisplayGroup(Component.translatable(def.titleKey), chars));
            }
        }

        // 数据包/附加包自定义目录（不在内置分组中）追加在最后
        for (String folder : all.stream().map(Operator::folderOf).distinct().toList()) {
            if (knownFolders.contains(folder)) {
                continue;
            }
            // 只保留有贴图的角色
            List<CharacterDefinition> chars = all.stream()
                    .filter(c -> folderOf(c).equals(folder))
                    .filter(c -> operatorTexture(c) != null)
                    .toList();
            if (!chars.isEmpty()) {
                groups.add(new DisplayGroup(Component.literal(folder), chars));
            }
        }
        return groups;
    }

    /**
     * 从角色 id 提取贴图文件夹名。
     * 角色 id 形如 "dealt_force_skills:assault/d_wolf" → "assault"
     */
    private static String folderOf(CharacterDefinition character) {
        String id = character.id();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return path.contains("/") ? path.substring(0, path.indexOf('/')) : "";
    }

    /**
     * 根据角色 id 生成干员头像贴图路径，贴图不存在时返回 null。
     * 角色 id 形如 "dealt_force_skills:assault/d_wolf" → deltaforce:textures/gui/operator/assault/d_wolf.png
     */
    private static ResourceLocation operatorTexture(CharacterDefinition character) {
        String id = character.id();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                "deltaforce", "textures/gui/operator/" + path + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(tex).isPresent() ? tex : null;
    }

    // ========== 交互 ==========

    /**
     * 滚轮滑动（scrollY>0 向上滚 → 内容右移，看更靠前的干员）
     */
    public static void handleMouseScrolled(double scrollY) {
        if (maxScroll <= 0) return;
        scrollOffset -= (int) (scrollY * 24);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    /**
     * 按下左键：第一次点击干员头像变白（待确认），再次点击同一干员才确认选择；
     * 点击当前已选干员时只变白闪一下，不触发切换（避免误消耗浮木）；
     * 生存模式下确认会弹出消耗浮木的确认窗；点击其他干员时切换待确认高亮；
     * 未命中干员则在干员栏区域内启动拖拽。
     *
     * @return 是否命中干员栏区域（命中则吞掉点击）
     */
    public static boolean handleMouseClicked(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int barTop = screenHeight - 20 - BAR_HEIGHT;
        if (mouseY >= barTop && mouseY < screenHeight - 20) {
            // 点击干员头像 → 待确认机制
            CharacterDefinition clicked = getOperatorAt(mouseX, mouseY, screenWidth, screenHeight);
            if(clicked!=null&&!ClientCharacterSelectionState.canSelect(clicked)) {
                Minecraft.getInstance().player.displayClientMessage(ClientCharacterSelectionState.blockedDescription(clicked),false);
                return true;
            }
            if (clicked != null && ClientCharacterSelectionState.canSelect(clicked)) {
                if (ClientCharacterSelectionState.isSelectedCharacter(clicked.id())) {
                    // 点击当前已选干员：只变白闪一下，不进入待确认/确认流程
                    pendingCharacterId = clicked.id();
                    currentSelectedFlashUntil = System.currentTimeMillis() + 300L;
                    return true;
                }
                if (clicked.id().equals(pendingCharacterId)) {
                    // 再次点击同一干员：确认选择
                    confirmSelection(clicked.id());
                } else {
                    // 第一次点击：变白待确认（点击别的干员时自动切换高亮）
                    pendingCharacterId = clicked.id();
                }
                return true;
            }
            // 未命中干员：启动拖拽
            lastDragX = mouseX;
            dragStartScroll = scrollOffset;
            return true;
        }
        lastDragX = -1;
        return false;
    }

    /**
     * 确认选择：首次选择直接提交；生存模式重选才检查背包浮木并弹出确认窗。
     */
    private static void confirmSelection(String characterId) {
        var player = Minecraft.getInstance().player;
        if (player != null && !player.getAbilities().instabuild
                && !ClientCharacterSelectionState.hasFreeReselection()
                && ClientCharacterSelectionState.hasSelectedCharacter()) {
            // 生存模式重选：需要浮木，无浮木也弹窗提示
            if (hasDriftwood(player)) {
                OperatorSwitchConfirmScreen.open(characterId);
            } else {
                OperatorSwitchConfirmScreen.openMissing();
            }
        } else {
            // 首次选择或创造模式：直接交给服务端校验
            NetworkHandler.sendToServer(new C2S_SelectCharacter(characterId));
            pendingCharacterId = null;
        }
    }

    /**
     * 背包中是否存在浮木
     */
    private static boolean hasDriftwood(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.DRIFTWOOD.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清除待确认状态（弹窗关闭时调用）
     */
    public static void clearPending() {
        pendingCharacterId = null;
    }

    /**
     * 根据鼠标位置查找点击到的干员（与渲染布局一致），未命中返回null
     */
    private static CharacterDefinition getOperatorAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        var font = Minecraft.getInstance().font;
        int barTop = screenHeight - 20 - BAR_HEIGHT;
        int contentY = barTop + CONTENT_TOP;
        int rightEdge = screenWidth - BAR_RIGHT;
        int x = BAR_LEFT - scrollOffset;

        List<DisplayGroup> groups = displayGroups();
        for (int t = 0; t < groups.size(); t++) {
            x += GROUP_HEADER_H;
            List<CharacterDefinition> chars = groups.get(t).characters();
            for (CharacterDefinition character : chars) {
                int blockX = x;
                // 右边界：干员块右边缘不超过 rightEdge 才绘制
                if (blockX + OP_BLOCK_W > BAR_LEFT && blockX + OP_BLOCK_W <= rightEdge) {
                    int iconX = blockX + (OP_BLOCK_W - OP_ICON_W) / 2;
                    if (mouseX >= iconX && mouseX < iconX + OP_ICON_W &&
                        mouseY >= contentY && mouseY < contentY + OP_ICON_H) {
                        return character;
                    }
                }
                x += OP_BLOCK_W;
            }
            if (t < groups.size() - 1) {
                x += DIVIDER_W;
            }
        }
        return null;
    }

    /**
     * 拖拽滑动：按住左键横向拖动，内容跟随鼠标移动
     */
    public static void handleMouseDragged(int mouseX) {
        if (lastDragX < 0) return;
        scrollOffset = dragStartScroll - (mouseX - lastDragX);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    /**
     * 松开鼠标：结束拖拽
     */
    public static void handleMouseReleased() {
        lastDragX = -1;
    }

    /**
     * 获取当前悬浮的角色ID（供外部显示详情用，无则返回null）
     */
    public static String getHoveredOperatorId() {
        return hoveredOperatorId;
    }

    /**
     * 渲染干员详情面板：左边介绍图 + 右边介绍文本 + 详情按钮
     * 不死人特殊处理：右边显示多个子角色，每个子角色有名字、介绍和详情按钮
     */
    public static void renderOperatorDetail(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight,
            int offsetX, int offsetY, float uiScale) {
        String characterId = pendingCharacterId != null && !pendingCharacterId.isBlank()
                ? pendingCharacterId
                : ClientCharacterSelectionState.selectedCharacterId();
        if (characterId == null || characterId.isBlank()) {
            return;
        }

        // 检测角色切换，重置详情展开状态和滚动
        if (!characterId.equals(lastDetailCharacterId)) {
            lastDetailCharacterId = characterId;
            detailExpanded = false;
            introScrollOffset = 0;
            undeadSubDetailExpanded.clear();
            undeadDetailBtns.clear();
        }

        // 判断是否为不死人
        boolean isUndead = characterId.endsWith("/undead");

        PreviewAsset asset = PREVIEWS.get(characterId);
        if (asset == null) asset = new PreviewAsset(288, 288);
        String path = characterId.substring(characterId.indexOf(':') + 1);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                "deltaforce", "textures/gui/operator_preview/" + path + ".png");
        boolean hasPreview = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();

        int barTop = screenHeight - 20 - Operator.BAR_HEIGHT;
        int previewTop = screenWidth < 640 ? 68 : INTRO_PANEL_TOP;
        int previewBottom = barTop - INTRO_PANEL_BOTTOM_MARGIN;
        int maxHeight = Math.max(0, previewBottom - previewTop);
        if (maxHeight == 0) {
            return;
        }

        // 左面板宽度（介绍图区域）
        int leftPanelWidth = screenWidth * INTRO_PANEL_LEFT_RATIO / 20;
        int leftPanelX = screenWidth * INTRO_PANEL_LEFT_MARGIN_RATIO / 100;
        int leftPanelRight = leftPanelX + leftPanelWidth;

        // 计算介绍图缩放
        int leftMaxW = Math.max(0, leftPanelWidth - screenWidth * INTRO_PANEL_LEFT_MARGIN_RATIO / 100);
        float scale = Math.min((float) leftMaxW / asset.width(), (float) maxHeight / asset.height());
        int drawWidth = Math.max(1, Math.round(asset.width() * scale));
        int drawHeight = Math.max(1, Math.round(asset.height() * scale));
        int drawX = leftPanelX + (leftPanelWidth - drawWidth) / 2;
        int drawY = previewTop + (maxHeight - drawHeight) / 2;

        // 绘制介绍图
        if (hasPreview) {
            graphics.pose().pushPose();
            graphics.pose().translate(drawX, drawY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.blit(texture, 0, 0, 0, 0,
                    asset.width(), asset.height(), asset.width(), asset.height());
            graphics.pose().popPose();
        }

        // 右面板
        int rightPanelX = leftPanelRight + 16;
        int rightPanelW = screenWidth - rightPanelX - screenWidth * INTRO_PANEL_RIGHT_MARGIN_RATIO / 100;
        if (rightPanelW < 100) {
            return;
        }

        // 文本面板背景
        introTextPanelX = rightPanelX;
        introTextPanelY = previewTop;
        introTextPanelW = rightPanelW;
        introTextPanelH = maxHeight;
        graphics.fill(introTextPanelX, introTextPanelY, introTextPanelX + introTextPanelW, introTextPanelY + introTextPanelH, INTRO_PANEL_BG);

        if (isUndead) {
            renderUndeadSubIntroPanel(graphics, mouseX, mouseY, offsetX, offsetY, uiScale);
        } else {
            renderNormalIntroPanel(graphics, mouseX, mouseY, characterId, offsetX, offsetY, uiScale);
        }
    }

    /**
     * 渲染普通干员介绍面板（右面板）
     */
    private static void renderNormalIntroPanel(GuiGraphics graphics, int mouseX, int mouseY,
            String characterId, int offsetX, int offsetY, float uiScale) {
        IntroText intro = loadIntroText(characterId);
        if (intro == null) {
            return;
        }
        String text = detailExpanded ? intro.full() : intro.brief();
        if (text == null || text.isBlank()) {
            return;
        }

        // 文本可用区域
        int textX = introTextPanelX + INTRO_PANEL_PADDING;
        int textMaxW = introTextPanelW - INTRO_PANEL_PADDING * 2;

        // 按钮区域（底部）
        int btnAreaH = detailBtnH + 8;
        int textMaxH = introTextPanelH - INTRO_PANEL_PADDING * 2 - btnAreaH;
        if (textMaxH <= 0 || textMaxW <= 0) return;

        // 计算详情按钮位置
        detailBtnX = introTextPanelX + (introTextPanelW - detailBtnW) / 2;
        detailBtnY = introTextPanelY + introTextPanelH - INTRO_PANEL_PADDING - detailBtnH;

        // 渲染文本（自动换行+滚动）
        if (textMaxW > 0 && textMaxH > 0) {
            var font = Minecraft.getInstance().font;
            List<FormattedCharSequence> lines = font.split(Component.literal(text), textMaxW);
            int lineHeight = font.lineHeight + 1;
            int totalTextH = lines.size() * lineHeight;
            introMaxScroll = Math.max(0, totalTextH - textMaxH);

            // 滚动边界钳制
            if (introScrollOffset > introMaxScroll) introScrollOffset = introMaxScroll;
            if (introScrollOffset < 0) introScrollOffset = 0;

            // 裁剪区域
            graphics.enableScissor(
                    offsetX + (int)(introTextPanelX * uiScale),
                    offsetY + (int)((introTextPanelY + INTRO_PANEL_PADDING) * uiScale),
                    offsetX + (int)((introTextPanelX + introTextPanelW) * uiScale),
                    offsetY + (int)((introTextPanelY + INTRO_PANEL_PADDING + textMaxH) * uiScale));

            int textBaseY = textMaxH > 0 ? introTextPanelY + INTRO_PANEL_PADDING - introScrollOffset : introTextPanelY + INTRO_PANEL_PADDING;
            for (int i = 0; i < lines.size(); i++) {
                int lineY = textBaseY + i * lineHeight;
                if (lineY + font.lineHeight < introTextPanelY + INTRO_PANEL_PADDING) continue;
                if (lineY > introTextPanelY + INTRO_PANEL_PADDING + textMaxH) break;
                graphics.drawString(font, lines.get(i), textX, lineY, 0xFFD0D0D0, false);
            }

            graphics.disableScissor();

            // 绘制滚动条
            if (introMaxScroll > 0) {
                int scrollBarX = introTextPanelX + introTextPanelW - INTRO_SCROLL_BAR_W - 3;
                int scrollBarY = introTextPanelY + INTRO_PANEL_PADDING;
                int scrollBarH = textMaxH;
                graphics.fill(scrollBarX, scrollBarY, scrollBarX + INTRO_SCROLL_BAR_W, scrollBarY + scrollBarH, INTRO_SCROLL_BAR_COLOR);

                int handleH = Math.max(12, scrollBarH * scrollBarH / (totalTextH + textMaxH));
                int handleY = scrollBarY + (int)((float) introScrollOffset / introMaxScroll * (scrollBarH - handleH));
                graphics.fill(scrollBarX, handleY, scrollBarX + INTRO_SCROLL_BAR_W, handleY + handleH, INTRO_SCROLL_HANDLE_COLOR);
            }
        }

        // 绘制详情按钮
        boolean btnHovered = mouseX >= detailBtnX && mouseX < detailBtnX + detailBtnW
                && mouseY >= detailBtnY && mouseY < detailBtnY + detailBtnH;
        int btnColor = btnHovered ? INTRO_BTN_HOVER : INTRO_BTN_BG;
        graphics.fill(detailBtnX, detailBtnY, detailBtnX + detailBtnW, detailBtnY + detailBtnH, btnColor);

        // 按钮文字
        String btnText = Component.translatable(detailExpanded ? "screen.deltaforce.operator.collapse" : "screen.deltaforce.operator.details").getString();
        var font = Minecraft.getInstance().font;
        int btnTextW = font.width(btnText);
        graphics.drawString(font, btnText,
                detailBtnX + (detailBtnW - btnTextW) / 2,
                detailBtnY + (detailBtnH - font.lineHeight) / 2,
                0xFFFFFFFF, false);
    }

    /**
     * 渲染不死人子角色介绍面板（右面板）：多个子角色，每个有名字、介绍和详情按钮
     */
    private static void renderUndeadSubIntroPanel(GuiGraphics graphics, int mouseX, int mouseY,
            int offsetX, int offsetY, float uiScale) {
        List<SubIntroText> subIntros = loadUndeadSubIntros();
        if (subIntros.isEmpty()) {
            return;
        }

        var font = Minecraft.getInstance().font;
        int textX = introTextPanelX + INTRO_PANEL_PADDING;
        int textMaxW = introTextPanelW - INTRO_PANEL_PADDING * 2 - INTRO_SCROLL_BAR_W - 4;
        int lineHeight = font.lineHeight + 1;

        // 子角色详情按钮尺寸
        int subBtnW = 40;
        int subBtnH = 16;
        int subBtnMargin = 4;

        // 计算每个子角色条目的高度和总内容高度
        int nameLineH = lineHeight + 2; // 名字行高
        int separatorH = 6; // 分隔线高度

        // 先计算总内容高度
        int totalContentH = 0;
        int[] entryHeights = new int[subIntros.size()];
        for (int i = 0; i < subIntros.size(); i++) {
            SubIntroText sub = subIntros.get(i);
            boolean expanded = undeadSubDetailExpanded.getOrDefault(sub.fileKey(), false);
            String text = expanded ? sub.full() : sub.brief();
            List<FormattedCharSequence> lines = font.split(Component.literal(text), textMaxW);
            int textH = lines.size() * lineHeight;
            int entryH = nameLineH + textH + subBtnH + subBtnMargin + separatorH;
            entryHeights[i] = entryH;
            totalContentH += entryH;
        }

        // 滚动范围
        int visibleH = introTextPanelH - INTRO_PANEL_PADDING * 2;
        introMaxScroll = Math.max(0, totalContentH - visibleH);
        if (introScrollOffset > introMaxScroll) introScrollOffset = introMaxScroll;
        if (introScrollOffset < 0) introScrollOffset = 0;

        // 裁剪区域
        int scissorTop = offsetY + (int)((introTextPanelY + INTRO_PANEL_PADDING) * uiScale);
        int scissorBottom = offsetY + (int)((introTextPanelY + INTRO_PANEL_PADDING + visibleH) * uiScale);
        graphics.enableScissor(
                offsetX + (int)(introTextPanelX * uiScale),
                scissorTop,
                offsetX + (int)((introTextPanelX + introTextPanelW) * uiScale),
                scissorBottom);

        // 清空之前的按钮区域
        undeadDetailBtns.clear();

        // 绘制每个子角色条目
        int currentY = introTextPanelY + INTRO_PANEL_PADDING - introScrollOffset;
        for (int i = 0; i < subIntros.size(); i++) {
            SubIntroText sub = subIntros.get(i);
            boolean expanded = undeadSubDetailExpanded.getOrDefault(sub.fileKey(), false);
            String text = expanded ? sub.full() : sub.brief();
            List<FormattedCharSequence> lines = font.split(Component.literal(text), textMaxW);
            int textH = lines.size() * lineHeight;

            // 如果整个条目都在可见区域上方，跳过
            if (currentY + entryHeights[i] < introTextPanelY + INTRO_PANEL_PADDING) {
                currentY += entryHeights[i];
                continue;
            }
            // 如果整个条目都在可见区域下方，停止绘制
            if (currentY > introTextPanelY + INTRO_PANEL_PADDING + visibleH) {
                break;
            }

            // 绘制子角色名字（金色高亮）
            int nameY = currentY;
            if (nameY + font.lineHeight >= introTextPanelY + INTRO_PANEL_PADDING
                    && nameY <= introTextPanelY + INTRO_PANEL_PADDING + visibleH) {
                graphics.drawString(font, sub.name(), textX, nameY, 0xFFFFD700, false);
            }
            currentY += nameLineH;

            // 绘制介绍文本
            for (int j = 0; j < lines.size(); j++) {
                int lineY = currentY + j * lineHeight;
                if (lineY + font.lineHeight < introTextPanelY + INTRO_PANEL_PADDING) continue;
                if (lineY > introTextPanelY + INTRO_PANEL_PADDING + visibleH) break;
                graphics.drawString(font, lines.get(j), textX, lineY, 0xFFD0D0D0, false);
            }
            currentY += textH;

            // 绘制详情按钮
            int btnX = introTextPanelX + (introTextPanelW - subBtnW) / 2;
            int btnY = currentY + subBtnMargin;
            boolean btnHovered = mouseX >= btnX && mouseX < btnX + subBtnW
                    && mouseY >= btnY && mouseY < btnY + subBtnH;
            int btnColor = btnHovered ? INTRO_BTN_HOVER : INTRO_BTN_BG;
            graphics.fill(btnX, btnY, btnX + subBtnW, btnY + subBtnH, btnColor);

            String btnText = expanded ? "收起" : "详情";
            int btnTextW = font.width(btnText);
            graphics.drawString(font, btnText,
                    btnX + (subBtnW - btnTextW) / 2,
                    btnY + (subBtnH - font.lineHeight) / 2,
                    0xFFFFFFFF, false);

            // 记录按钮区域（用于点击检测）
            UndeadDetailBtn btn = new UndeadDetailBtn();
            btn.x = btnX; btn.y = btnY; btn.w = subBtnW; btn.h = subBtnH;
            btn.fileKey = sub.fileKey();
            undeadDetailBtns.add(btn);

            currentY += subBtnH + subBtnMargin;

            // 绘制分隔线
            int sepY = currentY;
            if (sepY >= introTextPanelY + INTRO_PANEL_PADDING
                    && sepY <= introTextPanelY + INTRO_PANEL_PADDING + visibleH) {
                graphics.fill(textX, sepY, introTextPanelX + introTextPanelW - INTRO_PANEL_PADDING, sepY + 1, 0x33FFFFFF);
            }
            currentY += separatorH;
        }

        graphics.disableScissor();

        // 绘制滚动条
        if (introMaxScroll > 0) {
            int scrollBarX = introTextPanelX + introTextPanelW - INTRO_SCROLL_BAR_W - 3;
            int scrollBarY = introTextPanelY + INTRO_PANEL_PADDING;
            int scrollBarH = visibleH;
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + INTRO_SCROLL_BAR_W, scrollBarY + scrollBarH, INTRO_SCROLL_BAR_COLOR);

            int handleH = Math.max(12, scrollBarH * scrollBarH / (totalContentH + visibleH));
            int handleY = scrollBarY + (int)((float) introScrollOffset / introMaxScroll * (scrollBarH - handleH));
            graphics.fill(scrollBarX, handleY, scrollBarX + INTRO_SCROLL_BAR_W, handleY + handleH, INTRO_SCROLL_HANDLE_COLOR);
        }
    }

    /**
     * 处理详情按钮点击（包括不死人子角色详情按钮）
     * @return 是否命中按钮
     */
    public static boolean handleDetailButtonClick(int mouseX, int mouseY) {
        // 先检查不死人子角色详情按钮
        for (UndeadDetailBtn btn : undeadDetailBtns) {
            if (mouseX >= btn.x && mouseX < btn.x + btn.w
                    && mouseY >= btn.y && mouseY < btn.y + btn.h) {
                boolean current = undeadSubDetailExpanded.getOrDefault(btn.fileKey, false);
                undeadSubDetailExpanded.put(btn.fileKey, !current);
                introScrollOffset = 0;
                return true;
            }
        }

        // 普通详情按钮
        if (detailBtnX <= 0) {
            return false;
        }
        if (mouseX >= detailBtnX && mouseX < detailBtnX + detailBtnW
                && mouseY >= detailBtnY && mouseY < detailBtnY + detailBtnH) {
            detailExpanded = !detailExpanded;
            introScrollOffset = 0;
            return true;
        }
        return false;
    }

    /**
     * 处理介绍面板鼠标滚轮滚动
     */
    public static void handleIntroScroll(double scrollY) {
        if (introMaxScroll <= 0) return;
        introScrollOffset -= (int)(scrollY * 24);
        if (introScrollOffset < 0) introScrollOffset = 0;
        if (introScrollOffset > introMaxScroll) introScrollOffset = introMaxScroll;
    }

    /**
     * 检测鼠标是否在介绍文本面板内
     */
    public static boolean isInIntroPanel(int mouseX, int mouseY) {
        return mouseX >= introTextPanelX && mouseX < introTextPanelX + introTextPanelW
                && mouseY >= introTextPanelY && mouseY < introTextPanelY + introTextPanelH;
    }

    /**
     * 根据角色ID获取介绍文件路径名
     */
    private static String introFileName(String characterId) {
        if (characterId == null) return null;
        String path = characterId.contains(":") ? characterId.substring(characterId.indexOf(':') + 1) : characterId;
        if (path.endsWith("/undead")) {
            return null; // 不死人由 loadUndeadSubIntros 单独处理
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 加载并解析干员介绍文本
     */
    private static IntroText loadIntroText(String characterId) {
        if (introCache.containsKey(characterId)) {
            return introCache.get(characterId);
        }
        String fileName = introFileName(characterId);
        if (fileName == null) {
            introCache.put(characterId, null);
            return null;
        }
        try {
            ResourceLocation res = ResourceLocation.fromNamespaceAndPath(
                    "dealt_force_skills", "operator_introduction/" + fileName + ".txt");
            var opt = Minecraft.getInstance().getResourceManager().getResource(res);
            if (opt.isEmpty()) {
                introCache.put(characterId, null);
                return null;
            }
            String content;
            try (var reader = new BufferedReader(new InputStreamReader(opt.get().open(), StandardCharsets.UTF_8))) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }
            String brief = extractTag(content, "introduction");
            String full = extractTag(content, "introduction_all");
            IntroText result = new IntroText(brief, full);
            introCache.put(characterId, result);
            return result;
        } catch (Exception e) {
            introCache.put(characterId, null);
            return null;
        }
    }

    /**
     * 加载不死人所有子角色介绍文本
     */
    private static List<SubIntroText> loadUndeadSubIntros() {
        if (undeadSubIntros != null) {
            return undeadSubIntros;
        }
        List<SubIntroText> result = new ArrayList<>();
        for (String fileKey : UNDEAD_SUB_FILES) {
            try {
                ResourceLocation res = ResourceLocation.fromNamespaceAndPath(
                        "dealt_force_skills", "operator_introduction/undead/" + fileKey + ".txt");
                var opt = Minecraft.getInstance().getResourceManager().getResource(res);
                if (opt.isEmpty()) {
                    continue;
                }
                String content;
                try (var reader = new BufferedReader(new InputStreamReader(opt.get().open(), StandardCharsets.UTF_8))) {
                    content = reader.lines().collect(Collectors.joining("\n"));
                }
                String briefRaw = extractTag(content, "introduction");
                String fullRaw = extractTag(content, "introduction_all");
                if (briefRaw == null && fullRaw == null) {
                    continue;
                }
                // 提取子角色名称（从brief第二行，跳过公共提示行）
                String name = extractUndeadSubName(briefRaw, fullRaw);
                // 清理brief文本：移除公共提示行和名称行
                String brief = cleanUndeadSubText(briefRaw);
                String full = cleanUndeadSubText(fullRaw);
                result.add(new SubIntroText(fileKey, name, brief, full));
            } catch (Exception e) {
                // 跳过无法加载的文件
            }
        }
        undeadSubIntros = result;
        return result;
    }

    /**
     * 从不死人子角色文本中提取名称
     */
    private static String extractUndeadSubName(String briefRaw, String fullRaw) {
        // 从brief中查找以"不死人子角色-"开头的行
        if (briefRaw != null) {
            for (String line : briefRaw.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("不死人子角色-")) {
                    return trimmed.substring("不死人子角色-".length());
                }
            }
        }
        // 回退：从full中查找
        if (fullRaw != null) {
            for (String line : fullRaw.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("不死人子角色-")) {
                    return trimmed.substring("不死人子角色-".length());
                }
            }
        }
        return "???";
    }

    /**
     * 清理不死人子角色文本：移除公共提示行和名称行
     */
    private static String cleanUndeadSubText(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            // 跳过公共提示行
            if (trimmed.startsWith("（非创造模式下")) {
                continue;
            }
            // 跳过名称行
            if (trimmed.startsWith("不死人子角色-")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString().trim();
    }

    /**
     * 从XML标签中提取内容
     */
    private static String extractTag(String content, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = content.indexOf(open);
        int end = content.indexOf(close);
        if (start >= 0 && end > start) {
            return content.substring(start + open.length(), end).trim();
        }
        return null;
    }

    /**
     * 渲染分组（标题 + 角色列表）
     */
    private record DisplayGroup(Component label, List<CharacterDefinition> characters) {
    }

    /**
     * 角色预览大图原始分辨率
     */
    private record PreviewAsset(int width, int height) {
    }
}