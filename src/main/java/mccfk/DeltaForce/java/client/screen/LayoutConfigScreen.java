package mccfk.DeltaForce.java.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * LayoutConfigScreen - 自定义布局配置界面
 * 布局: 左侧预览区 | 右侧界面列表 | 预览下方工具栏 | 底栏
 * 工具栏含缩放/X/Y滑块 + 预保存按钮
 * 每界面有预定义元素，只能拖动改位置/缩放，不能添加/删除
 * 退出时若存在未提交的预保存修改，弹出确认对话框
 */
public class LayoutConfigScreen extends Screen {

    // 颜色常量
    private static final int BG_COLOR = 0xFF0E1819;
    private static final float REF_WIDTH = 960f;
    private static final float REF_HEIGHT = 540f;
    private static final int BOTTOM_BAR_H = 20;
    private static final int BOTTOM_BAR_COLOR = 0x30333333;
    private static final int PREVIEW_BG = 0x18000000;
    private static final int PREVIEW_BORDER = 0xFF555555;
    private static final int TOOLBAR_COLOR = 0x181E1E1E;
    private static final int TOOLBAR_SEP = 0xFF444444;
    private static final int HANDLE_COLOR = 0xCCFFFFFF;
    private static final int HANDLE_SIZE = 6;
    private static final int MIN_WIDGET = 16;
    private static final int SLIDER_BAR_H = 8;
    private static final int SLIDER_HANDLE_W = 6;

    // ====================== 颜色与渲染常量 ======================

    // 缩放/位移
    private float uiScale;
    private int offX, offY;
    private int refW, refH;

    // 预览窗口
    private int prevX, prevY, prevW, prevH;
    // 当前预览窗口实际宽高比
    private float previewAspect;

    // 工具栏 (约1/5窗口高)
    private int toolbarY, toolbarH;
    private int toolbarX, toolbarW;
    private int lineY;

    // 底栏按钮
    private int saveBtnX, saveBtnY, saveBtnW, saveBtnH;
    private int cancelBtnX, cancelBtnY, cancelBtnW, cancelBtnH;
    private int undoBtnX, undoBtnY, undoBtnW, undoBtnH;
    private int exitBtnX, exitBtnY, exitBtnW, exitBtnH;

    // 右侧列表
    private final List<ClientScreenRegistry.Entry> screenEntries = new ArrayList<>();
    private int listAreaX, listAreaY, listAreaW, listAreaH;
    private int listScrollY, listMaxScroll;
    private static final int ENTRY_H = 18;
    private static final int CFG_BTN_W = 40;

    // ====================== 编辑状态 ======================

    private ClientScreenRegistry.Entry editingEntry;
    private LayoutDataManager.LayoutContainer editLayout;
    private final List<LayoutEntry> editElementList = new ArrayList<>();

    // 预保存缓冲区
    private LayoutDataManager.LayoutContainer preSaveBuffer;
    private boolean hasUnsavedPreSaves;
    // 预保存按钮闪烁计时
    private long preSaveFlashStart;

    // 当前选中的控件
    private LayoutEntry selectedWidget;

    // 正在编辑的真实Screen实例（非null时用真渲染替代模拟绘制）
    private Screen editingScreen;

    // 拖拽状态
    private LayoutEntry dragWidget;
    private int dragHandle = -1; // -1=整体拖拽, 0-7=缩放手柄
    private int dragStartMX, dragStartMY;
    private float dragStartRelX, dragStartRelY;
    private float dragStartScaleX, dragStartScaleY;

    // 滑块交互状态 (-1=无, 0=scaleX, 1=scaleY, 2=posX, 3=posY)
    private int activeSlider = -1;

    // 撤销快照
    private String undoSsnapJson;

    // ====================== 构造 ======================

    public static void open() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new LayoutConfigScreen());
    }

    public LayoutConfigScreen() {
        super(Component.translatable("screen.deltaforce.layout_config"));
    }

    @Override
    protected void init() {
        this.uiScale = Math.min(this.width / REF_WIDTH, this.height / REF_HEIGHT);
        this.offX = (int) ((this.width - REF_WIDTH * this.uiScale) / 2);
        this.offY = (int) ((this.height - REF_HEIGHT * this.uiScale) / 2);
        this.refW = (int) REF_WIDTH;
        this.refH = (int) REF_HEIGHT;

        // 底栏按钮
        int barY = this.refH - BOTTOM_BAR_H;
        int btnW = 60, btnH = BOTTOM_BAR_H, gap = 6;
        int rm = (int) (REF_WIDTH * 0.01f);
        exitBtnX = refW - rm - btnW;  exitBtnY = barY; exitBtnW = btnW; exitBtnH = btnH;
        undoBtnX = exitBtnX - gap - btnW; undoBtnY = barY; undoBtnW = btnW; undoBtnH = btnH;
        cancelBtnX = undoBtnX - gap - btnW; cancelBtnY = barY; cancelBtnW = btnW; cancelBtnH = btnH;
        saveBtnX = cancelBtnX - gap - btnW; saveBtnY = barY; saveBtnW = btnW; saveBtnH = btnH;

        calcAreas();
        refreshScreenList();
    }

    // ====================== 布局计算 ======================

    /**
     * 计算所有区域：预览窗口(保持屏幕比例)、工具栏(约1/5窗口高)、右侧列表
     */
    private void calcAreas() {
        // 右侧列表始终需要宽度
        listAreaW = 170;
        listAreaX = refW - listAreaW - 10;
        listAreaY = 10;
        listAreaH = refH - BOTTOM_BAR_H - listAreaY;

        // 可用空间: 左侧预览+工具栏最大宽度为列表左侧-20
        int leftMaxW = listAreaX - 20;
        int totalLeftH = refH - BOTTOM_BAR_H - 10;

        // 工具栏约占剩余高度的 1/5
        this.toolbarH = Math.max(70, totalLeftH / 5);
        int maxPreviewH = totalLeftH - toolbarH - 6;

        // 真实屏幕比例
        float screenAspect = (float) this.width / Math.max(1, this.height);

        // 预览窗口保持真实屏幕比例
        this.prevX = 10;
        this.prevY = 10;
        this.prevW = leftMaxW;
        this.prevH = (int) (prevW / screenAspect);
        if (prevH > maxPreviewH) {
            prevH = maxPreviewH;
            prevW = (int) (prevH * screenAspect);
        }
        this.previewAspect = (float) prevW / Math.max(1, prevH);

        // 工具栏位置: 预览窗口下方
        this.lineY = prevY + prevH + 2;
        this.toolbarY = lineY + 2;
        this.toolbarX = prevX;
        this.toolbarW = leftMaxW;

        layoutListArea();
    }

    private void layoutListArea() {
        listMaxScroll = Math.max(0, screenEntries.size() * ENTRY_H - listAreaH);
        if (listScrollY > listMaxScroll) listScrollY = listMaxScroll;
        if (listScrollY < 0) listScrollY = 0;
    }

    // ====================== 屏幕列表收集 ======================

    private void refreshScreenList() {
        screenEntries.clear();
        screenEntries.addAll(ClientScreenRegistry.getAll());
        screenEntries.sort(Comparator.comparing(
                e -> e.displayName().getString().toLowerCase()));
    }

    // ====================== 编辑模式 ======================

    private void startEditing(ClientScreenRegistry.Entry entry) {
        this.editingEntry = entry;
        String modId = LayoutDataManager.extractModId(entry.id());
        String screenName = LayoutDataManager.extractScreenName(entry.id());
        this.editLayout = LayoutDataManager.load(modId, screenName);
        if (this.editLayout == null)
            this.editLayout = new LayoutDataManager.LayoutContainer();
        this.preSaveBuffer = null;
        this.hasUnsavedPreSaves = false;
        this.preSaveFlashStart = 0;
        this.selectedWidget = null;
        this.activeSlider = -1;
        // 确保该界面所有预定义元素都存在（从保存的数据恢复，或使用默认值）
        ensureDefaultElements(entry);
        refreshEditElementList();
        createUndoSnapshot();

        // 创建真实Screen实例用于预览渲染
        this.editingScreen = null;
        if (entry.hasScreenClass()) {
            try {
                int tabIndex = getDfGuiTabIndex(entry.id());
                Screen s = (Screen) entry.screenClass()
                        .getDeclaredConstructor().newInstance();
                // 对DFGUI_MAIN子页设置指定标签页
                if (s instanceof DFGUI_MAIN df && tabIndex >= 0) {
                    setSelectedTab(df, tabIndex);
                }
                // 用预览区尺寸初始化Screen
                s.width = prevW;
                s.height = prevH;
                s.init(minecraft, prevW, prevH);
                this.editingScreen = s;
            } catch (Exception ex) {
                // 反射失败则退回到模拟渲染
            }
        }
    }

    /** 获取DFGUI_MAIN子页的标签索引，非主界面返回-1 */
    private static int getDfGuiTabIndex(String entryId) {
        return switch (entryId) {
            case "dfgui_main" -> 0;
            case "dfgui_main_start" -> 0;
            case "dfgui_main_warehouse" -> 1;
            case "dfgui_main_operator" -> 2;
            case "dfgui_main_department" -> 3;
            case "dfgui_main_exchange" -> 4;
            case "dfgui_main_logistics" -> 5;
            case "dfgui_main_gunsmith" -> 6;
            default -> -1;
        };
    }

    /** 通过反射设置DFGUI_MAIN的selectedTab */
    private static void setSelectedTab(DFGUI_MAIN screen, int tab) {
        try {
            java.lang.reflect.Field f = DFGUI_MAIN.class
                    .getDeclaredField("selectedTab");
            f.setAccessible(true);
            f.setInt(screen, tab);
        } catch (Exception ignored) {}
    }

    /** 确保当前编辑界面有所有预定义元素，加载不存在则用默认值添加 */
    private void ensureDefaultElements(ClientScreenRegistry.Entry entry) {
        if (entry.defaultElements() == null) return;
        for (LayoutEntry def : entry.defaultElements()) {
            if (!editLayout.elements.containsKey(def.id)) {
                LayoutEntry copy = new LayoutEntry();
                copy.id = def.id;
                copy.displayName = def.displayName;
                copy.anchorX = def.anchorX;
                copy.anchorY = def.anchorY;
                copy.offsetX = def.offsetX;
                copy.offsetY = def.offsetY;
                copy.relWidth = def.relWidth;
                copy.relHeight = def.relHeight;
                copy.scaleX = def.scaleX;
                copy.scaleY = def.scaleY;
                copy.enabled = true;
                editLayout.elements.put(def.id, copy);
            }
        }
    }

    private void stopEditing() {
        this.editingEntry = null;
        this.editLayout = null;
        this.editElementList.clear();
        this.preSaveBuffer = null;
        this.hasUnsavedPreSaves = false;
        this.preSaveFlashStart = 0;
        this.selectedWidget = null;
        this.activeSlider = -1;
        this.dragWidget = null;
        this.dragHandle = -1;
        this.undoSsnapJson = null;
        this.editingScreen = null;
    }

    private void refreshEditElementList() {
        editElementList.clear();
        if (editLayout != null)
            editElementList.addAll(editLayout.elements.values());
    }

    // ====================== 预保存 ======================

    private void preSaveToBuffer() {
        if (editLayout == null) return;
        preSaveBuffer = deepCopy(editLayout);
        hasUnsavedPreSaves = true;
        preSaveFlashStart = System.currentTimeMillis();
    }

    private LayoutDataManager.LayoutContainer deepCopy(
            LayoutDataManager.LayoutContainer src) {
        var gson = new com.google.gson.Gson();
        return gson.fromJson(gson.toJson(src),
                LayoutDataManager.LayoutContainer.class);
    }

    // ====================== 保存 / 撤销 ======================

    private void commitSave() {
        if (editingEntry == null || editLayout == null) return;
        String modId = LayoutDataManager.extractModId(editingEntry.id());
        String screenName = LayoutDataManager.extractScreenName(
                editingEntry.id());
        LayoutDataManager.save(modId, screenName, editLayout);
        preSaveBuffer = null;
        hasUnsavedPreSaves = false;
        preSaveFlashStart = 0;
        createUndoSnapshot();
    }

    private void createUndoSnapshot() {
        undoSsnapJson = jsonEncode(editLayout);
    }

    private String jsonEncode(LayoutDataManager.LayoutContainer c) {
        return new com.google.gson.Gson().toJson(c);
    }

    private void undoSave() {
        if (undoSsnapJson == null || editLayout == null) return;
        LayoutDataManager.LayoutContainer prev = new com.google.gson.Gson()
                .fromJson(undoSsnapJson,
                        LayoutDataManager.LayoutContainer.class);
        if (prev != null) {
            editLayout.elements.clear();
            editLayout.elements.putAll(prev.elements);
            refreshEditElementList();
            selectedWidget = null;
            createUndoSnapshot();
        }
    }

    // ====================== 退出确认 ======================

    private void tryExit() {
        if (editingEntry != null && hasUnsavedPreSaves) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    result -> {
                        if (result) {
                            commitSave();
                            Minecraft.getInstance().setScreen(null);
                        } else {
                            Minecraft.getInstance().setScreen(null);
                        }
                    },
                    Component.translatable(
                            "gui.deltaforce.exit_confirm.title"),
                    Component.translatable(
                            "gui.deltaforce.exit_confirm.message"),
                    Component.translatable(
                            "gui.deltaforce.exit_confirm.save_quit"),
                    Component.translatable(
                            "gui.deltaforce.exit_confirm.discard_quit")));
        } else {
            onClose();
        }
    }

    @Override
    public void onClose() {
        stopEditing();
        super.onClose();
    }

    // ====================== 渲染主入口 ======================

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, BG_COLOR);
        g.pose().pushPose();
        g.pose().translate(offX, offY, 0);
        g.pose().scale(uiScale, uiScale, 1f);
        int smx = (int) ((mx - offX) / uiScale);
        int smy = (int) ((my - offY) / uiScale);

        // 预览区底色
        g.fill(prevX, prevY, prevX + prevW, prevY + prevH, PREVIEW_BG);

        if (editingScreen != null) {
            // 按LayoutEntry变换渲染Screen
            renderTransformedScreen(g, smx, smy, pt);
        } else if (editingEntry != null) {
            // 无Screen类的HUD条目：模拟绘制
            renderEditWidgets(g, smx, smy);
        }

        // 预览区边框
        g.renderOutline(prevX, prevY, prevW, prevH, PREVIEW_BORDER);

        renderToolbar(g, smx, smy);
        renderScreenList(g, smx, smy);
        renderBottomBar(g, smx, smy);

        g.pose().popPose();
    }

    /**
     * 按LayoutEntry的relX/relY/scaleX/scaleY变换渲染Screen
     * 每个元素 = 一个独立的Screen渲染实例，位置/缩放由LayoutEntry控制
     */
    private void renderTransformedScreen(GuiGraphics g, int smx, int smy, float pt) {
        if (editingScreen == null || editElementList.isEmpty()) return;

        for (LayoutEntry e : editElementList) {
            if (!e.enabled) continue;
            boolean isSel = (e == selectedWidget);

            // 计算元素在预览区的绝对位置和尺寸
            int ex = prevX + (int) (e.relX() * prevW);
            int ey = prevY + (int) (e.relY() * prevH);
            int ew = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
            int eh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));

            g.pose().pushPose();
            // 平移到元素位置
            g.pose().translate(ex, ey, 0);
            // 按scaleX/scaleY缩放
            g.pose().scale(e.scaleX, e.scaleY, 1f);

            // 临时设置Screen尺寸为元素的基础尺寸（未缩放前）
            int origW = editingScreen.width;
            int origH = editingScreen.height;
            int baseW = Math.max(1, (int) (e.relWidth * prevW));
            int baseH = Math.max(1, (int) (e.relHeight * prevH));
            editingScreen.width = baseW;
            editingScreen.height = baseH;

            // 渲染Screen
            editingScreen.render(g, 0, 0, pt);

            // 恢复Screen原始尺寸
            editingScreen.width = origW;
            editingScreen.height = origH;

            g.pose().popPose();

            // 绘制元素轮廓框（选中=亮黄，未选中=暗灰）
            int borderColor = isSel ? 0xFFEECC00 : 0x44666666;
            g.renderOutline(ex, ey, ew, eh, borderColor);
            if (isSel) {
                g.renderOutline(ex - 1, ey - 1, ew + 2, eh + 2, 0xAAEECC00);
                g.fill(ex, ey, ex + ew, ey + eh, 0x180088FF);
            }

            // 名称标签
            if (this.font != null) {
                String name = e.displayName;
                int nw = this.font.width(name);
                int labelX = ex + Math.max(0, (ew - nw) / 2);
                int labelY = ey - this.font.lineHeight - 1;
                if (labelY < prevY) labelY = prevY;
                g.fill(labelX - 2, labelY, labelX + nw + 2,
                        labelY + this.font.lineHeight + 1, 0xCC101216);
                int lc = isSel ? 0xFFFFDD55 : 0xFF888888;
                g.drawString(this.font, name, labelX, labelY, lc, false);
            }

            // 缩放手柄（选中或拖拽中）
            if (isSel) {
                int hs = HANDLE_SIZE;
                drawHandle(g, smx, smy, e, 0, ex, ey, hs);
                drawHandle(g, smx, smy, e, 1, ex + ew / 2 - hs / 2, ey, hs);
                drawHandle(g, smx, smy, e, 2, ex + ew - hs, ey, hs);
                drawHandle(g, smx, smy, e, 3, ex + ew - hs, ey + eh / 2 - hs / 2, hs);
                drawHandle(g, smx, smy, e, 4, ex + ew - hs, ey + eh - hs, hs);
                drawHandle(g, smx, smy, e, 5, ex + ew / 2 - hs / 2, ey + eh - hs, hs);
                drawHandle(g, smx, smy, e, 6, ex, ey + eh - hs, hs);
                drawHandle(g, smx, smy, e, 7, ex, ey + eh / 2 - hs / 2, hs);
            }
        }
    }

    /** 
     * 模拟路径：在模拟渲染元素之上叠加边框和拖拽手柄
     * 仅用于无Screen类的HUD条目
     */
    private void renderElementOverlay(GuiGraphics g, int smx, int smy) {
        if (editElementList.isEmpty()) return;
        for (LayoutEntry e : editElementList) {
            if (!e.enabled) continue;
            int wx = prevX + (int) (e.relX() * prevW);
            int wy = prevY + (int) (e.relY() * prevH);
            int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
            int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));

            boolean isSelected = (e == selectedWidget);
            boolean isDragging = (e == dragWidget);

            if (isSelected || isDragging) {
                g.fill(wx, wy, wx + ww, wy + wh, 0x2200AAFF);
            }
            if (isDragging) {
                g.renderOutline(wx - 1, wy - 1, ww + 2, wh + 2, 0xFFFFFF00);
            } else if (isSelected) {
                g.renderOutline(wx - 1, wy - 1, ww + 2, wh + 2, 0xFFFFAA00);
            } else {
                g.renderOutline(wx, wy, ww, wh, 0x22FFFFFF);
            }

            if (isSelected || isDragging) {
                int hs = HANDLE_SIZE;
                drawHandle(g, smx, smy, e, 0, wx, wy, hs);
                drawHandle(g, smx, smy, e, 1, wx + ww / 2 - hs / 2, wy, hs);
                drawHandle(g, smx, smy, e, 2, wx + ww - hs, wy, hs);
                drawHandle(g, smx, smy, e, 3, wx + ww - hs,
                        wy + wh / 2 - hs / 2, hs);
                drawHandle(g, smx, smy, e, 4, wx + ww - hs, wy + wh - hs, hs);
                drawHandle(g, smx, smy, e, 5, wx + ww / 2 - hs / 2,
                        wy + wh - hs, hs);
                drawHandle(g, smx, smy, e, 6, wx, wy + wh - hs, hs);
                drawHandle(g, smx, smy, e, 7, wx, wy + wh / 2 - hs / 2, hs);
            }
        }
    }

    // ====================== 编辑控件渲染（HUD无Screen类时用） ======================

    private void renderEditWidgets(GuiGraphics g, int smx, int smy) {
        for (LayoutEntry e : editElementList) {
            if (!e.enabled) continue;
            renderOneWidget(g, smx, smy, e);
        }
        if (editElementList.isEmpty()) {
            Component hint = Component.translatable(
                    "gui.deltaforce.layout.no_elements");
            int hw = this.font.width(hint);
            g.fill(prevX + (prevW - hw) / 2 - 4,
                    prevY + prevH / 2 - this.font.lineHeight - 2,
                    prevX + (prevW + hw) / 2 + 4,
                    prevY + prevH / 2 + 4,
                    0x20000000);
            g.drawString(this.font, hint,
                    prevX + (prevW - hw) / 2,
                    prevY + prevH / 2 - this.font.lineHeight / 2,
                    0x88FFFFFF, false);
        }
    }

    /**
     * 渲染单个控件，使用真实的HUD元素外观而非纯色块
     */
    private void renderOneWidget(GuiGraphics g, int smx, int smy,
            LayoutEntry e) {
        int wx = prevX + (int) (e.relX() * prevW);
        int wy = prevY + (int) (e.relY() * prevH);
        int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
        int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));

        boolean isSelected = (e == selectedWidget);
        boolean isDragging = (e == dragWidget);

        // 判断元素类型进行真实渲染
        HudElementType elemType = HudElementType.fromId(extractTypeId(e.id));
        renderElementByType(g, wx, wy, ww, wh, elemType, isSelected,
                isDragging, e);

        // 选中/拖拽高亮边框
        if (isDragging) {
            g.renderOutline(wx - 1, wy - 1, ww + 2, wh + 2, 0xFFFFFF00);
        } else if (isSelected) {
            g.renderOutline(wx - 1, wy - 1, ww + 2, wh + 2, 0xFFFFAA00);
        } else {
            g.renderOutline(wx, wy, ww, wh, 0x88FFFFFF);
        }

        // 缩放手柄（仅选中时绘制）
        if (isSelected || isDragging) {
            int hs = HANDLE_SIZE;
            drawHandle(g, smx, smy, e, 0, wx, wy, hs);
            drawHandle(g, smx, smy, e, 1, wx + ww / 2 - hs / 2, wy, hs);
            drawHandle(g, smx, smy, e, 2, wx + ww - hs, wy, hs);
            drawHandle(g, smx, smy, e, 3, wx + ww - hs,
                    wy + wh / 2 - hs / 2, hs);
            drawHandle(g, smx, smy, e, 4, wx + ww - hs, wy + wh - hs, hs);
            drawHandle(g, smx, smy, e, 5, wx + ww / 2 - hs / 2,
                    wy + wh - hs, hs);
            drawHandle(g, smx, smy, e, 6, wx, wy + wh - hs, hs);
            drawHandle(g, smx, smy, e, 7, wx, wy + wh / 2 - hs / 2, hs);
        }
    }

    /**
     * 从元素ID中提取类型前缀（如 "hotbar_12345" -> "hotbar"）
     */
    private String extractTypeId(String elementId) {
        if (elementId == null) return "custom_box";
        int idx = elementId.lastIndexOf('_');
        if (idx > 0) {
            String prefix = elementId.substring(0, idx);
            // 验证是否为已知类型
            for (HudElementType t : HudElementType.values()) {
                if (t.id.equals(prefix)) return prefix;
            }
        }
        return elementId;
    }

    /** 
     * 按HUD元素类型绘制真实外观
     */
    private void renderElementByType(GuiGraphics g, int wx, int wy, int ww,
            int wh, HudElementType type, boolean selected, boolean dragging,
            LayoutEntry entry) {
        // 半透明背景底色
        int alpha = dragging ? 0x60 : selected ? 0x50 : 0x35;
        int bg = (type.color & 0x00FFFFFF) | (alpha << 24);
        g.fill(wx, wy, wx + ww, wy + wh, bg);

        switch (type) {
            case HOTBAR:
                renderHotbar(g, wx, wy, ww, wh);
                break;
            case HEALTH:
                renderHearts(g, wx, wy, ww, wh, 0xFFFF4444);
                break;
            case ARMOR:
                renderHearts(g, wx, wy, ww, wh, 0xFF888888);
                break;
            case HUNGER:
                renderHearts(g, wx, wy, ww, wh, 0xFFCC8844);
                break;
            case CROSSHAIR:
                renderCrosshair(g, wx, wy, ww, wh);
                break;
            case EXPERIENCE:
                renderProgressBar(g, wx, wy, ww, wh, 0xFF44FF44);
                break;
            case BOSSBAR:
                renderProgressBar(g, wx, wy, ww, wh, 0xFF880088);
                break;
            case CHAT:
                renderChatPreview(g, wx, wy, ww, wh);
                break;
            case SCOREBOARD:
                renderScoreboardPreview(g, wx, wy, ww, wh);
                break;
            case PLAYER_LIST:
                renderPlayerListPreview(g, wx, wy, ww, wh);
                break;
            case ACTIONBAR:
                renderActionBarPreview(g, wx, wy, ww, wh);
                break;
            case CONTAINER_SLOTS:
                renderSlotGrid(g, wx, wy, ww, wh, 0xFF665544);
                break;
            case PLAYER_INVENTORY:
                renderSlotGrid(g, wx, wy, ww, wh, 0xFF776655);
                break;
            case TITLE_LABEL:
                renderTitlePreview(g, wx, wy, ww, wh);
                break;
            case CUSTOM_BOX:
            default:
                renderSmartCustomBox(g, wx, wy, ww, wh, entry);
                break;
        }
    }

    /** 
     * 根据元素ID后缀智能渲染 — 不再画"自定义框"而是按实际元素含义绘制
     */
    private void renderSmartCustomBox(GuiGraphics g, int x, int y, int w, int h,
            LayoutEntry entry) {
        String lowerId = entry.id != null ? entry.id.toLowerCase() : "";
        // 根据ID中的关键字判定渲染风格
        if (lowerId.contains("button") || lowerId.contains("btn")) {
            renderButtonGroup(g, x, y, w, h, lowerId);
        } else if (lowerId.contains("bar") || lowerId.contains("底栏") 
                || lowerId.contains("bottom")) {
            renderBarStrip(g, x, y, w, h);
        } else if (lowerId.contains("title") || lowerId.contains("label")
                || lowerId.contains("标题")) {
            renderTitlePreview(g, x, y, w, h);
        } else {
            // 真正的无特征自定义框 — 画虚线框以示区别
            renderGenericPlaceholder(g, x, y, w, h, entry);
        }
    }

    /** 按钮组外观：多个圆角矩形排列 */
    private void renderButtonGroup(GuiGraphics g, int x, int y, int w, int h,
            String idHint) {
        // 根据区域大小决定绘制几个按钮
        int btnCount = 3;
        if (idHint.contains("mode") || idHint.contains("模式"))
            btnCount = 4;
        int gap = 2;
        int btnW = (w - (btnCount + 1) * gap) / btnCount;
        if (btnW < 6) btnW = 6;
        int btnH = Math.min(h - 4, btnW * 2 / 3);
        int startX = x + (w - (btnCount * btnW + (btnCount - 1) * gap)) / 2;
        int btnY = y + (h - btnH) / 2;
        for (int i = 0; i < btnCount; i++) {
            int bx = startX + i * (btnW + gap);
            if (bx + btnW > x + w) break;
            // 按钮底色
            g.fill(bx, btnY, bx + btnW, btnY + btnH, 0xAA444444);
            g.renderOutline(bx, btnY, btnW, btnH, 0xFF777777);
            // 按钮高光边
            g.fill(bx + 1, btnY + 1, bx + btnW - 1, btnY + 2, 0x30FFFFFF);
        }
    }

    /** 横条/底栏外观 */
    private void renderBarStrip(GuiGraphics g, int x, int y, int w, int h) {
        int barH = Math.max(4, h);
        int barY = y + (h - barH) / 2;
        // 主体
        g.fill(x, barY, x + w, barY + barH, 0xAA222222);
        // 上沿高光
        g.fill(x, barY, x + w, barY + 1, 0x40FFFFFF);
        // 下沿暗边
        g.fill(x, barY + barH - 1, x + w, barY + barH, 0x40000000);
        // 中间装饰短线
        if (w > 20) {
            int segW = w / 4;
            for (int s = 0; s < 4; s++) {
                int sx = x + s * segW + segW / 4;
                g.fill(sx, barY + barH / 2 - 1, sx + segW / 2, 
                        barY + barH / 2 + 1, 0x18FFFFFF);
            }
        }
    }

    /** 无特征占位框 — 虚线框提示 */  
    private void renderGenericPlaceholder(GuiGraphics g, int x, int y, int w, 
            int h, LayoutEntry entry) {
        // 虚线边框
        for (int i = x + 1; i < x + w - 1; i += 4) {
            g.fill(i, y + 1, Math.min(i + 2, x + w - 1), y + 2, 0x44FFFFFF);
            g.fill(i, y + h - 2, Math.min(i + 2, x + w - 1), y + h - 1, 
                    0x44FFFFFF);
        }
        for (int i = y + 1; i < y + h - 1; i += 4) {
            g.fill(x + 1, i, x + 2, Math.min(i + 2, y + h - 1), 0x44FFFFFF);
            g.fill(x + w - 2, i, x + w - 1, Math.min(i + 2, y + h - 1), 
                    0x44FFFFFF);
        }
        // 显示名称
        String label = entry.displayName != null ? entry.displayName 
                : (entry.id != null ? entry.id : "?");
        int tw = this.font.width(label);
        if (w > tw + 6 && h > this.font.lineHeight + 4) {
            g.fill(x + (w - tw) / 2 - 2, y + (h - this.font.lineHeight) / 2 - 1,
                    x + (w + tw) / 2 + 2, 
                    y + (h + this.font.lineHeight) / 2 + 1, 0x88000000);
            g.drawString(this.font, label,
                    x + (w - tw) / 2, y + (h - this.font.lineHeight) / 2,
                    0x88CCCCCC, false);
        }
    }

    // ---------- 各类型真实渲染实现 ----------

    /** 快捷栏: 多个槽位 */
    private void renderHotbar(GuiGraphics g, int x, int y, int w, int h) {
        int slots = 9;
        int slotW = w / slots;
        int slotH = Math.min(h, slotW);
        int startY = y + (h - slotH) / 2;
        int selectedSlot = 4;
        for (int i = 0; i < slots; i++) {
            int sx = x + i * slotW + 1;
            int sy = startY + 1;
            int sw = slotW - 2;
            int sh = slotH - 2;
            if (sw < 2 || sh < 2) continue;
            if (i == selectedSlot) {
                g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, 0xFFFFFFFF);
            }
            g.fill(sx, sy, sx + sw, sy + sh, 0x8B000000);
            // 小图标
            int inner = Math.min(sw, sh) / 2;
            if (inner >= 2) {
                g.fill(sx + (sw - inner) / 2, sy + (sh - inner) / 2,
                        sx + (sw + inner) / 2, sy + (sh + inner) / 2,
                        0x55888888);
            }
        }
    }

    /** 心形指示器（生命/盔甲/饥饿通用） */
    private void renderHearts(GuiGraphics g, int x, int y, int w, int h,
            int color) {
        int heartSize = Math.min(h, Math.max(3, w / 10));
        int maxHearts = w / (heartSize + 1);
        int hearts = Math.max(1, maxHearts * 3 / 4);
        int startX = x + (w - hearts * (heartSize + 1)) / 2;
        int rowH = heartSize + 2;
        int rowCount = h / rowH;
        for (int row = 0; row < Math.min(rowCount, 2); row++) {
            for (int i = 0; i < hearts; i++) {
                int hx = startX + i * (heartSize + 1);
                int hy = y + 2 + row * rowH;
                if (hx + heartSize <= x + w && hy + heartSize <= y + h) {
                    if (i < hearts * 3 / 4) {
                        g.fill(hx, hy, hx + heartSize, hy + heartSize,
                                color | 0xFF000000);
                    } else {
                        g.fill(hx, hy, hx + heartSize, hy + heartSize,
                                0x66444444);
                    }
                }
            }
        }
    }

    /** 准星 */
    private void renderCrosshair(GuiGraphics g, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int len = Math.min(w, h) / 3;
        int thick = Math.max(1, len / 4);
        // 十字线
        g.fill(cx - thick / 2, cy - len, cx + thick / 2, cy - thick,
                0xCC00FF00);
        g.fill(cx - thick / 2, cy + thick, cx + thick / 2, cy + len,
                0xCC00FF00);
        g.fill(cx - len, cy - thick / 2, cx - thick, cy + thick / 2,
                0xCC00FF00);
        g.fill(cx + thick, cy - thick / 2, cx + len, cy + thick / 2,
                0xCC00FF00);
        // 中心点
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFF00);
    }

    /** 经验条/Boss条: 进度条 */
    private void renderProgressBar(GuiGraphics g, int x, int y, int w, int h,
            int color) {
        // 条形外观
        int barH = Math.max(3, h / 3);
        int barY = y + (h - barH) / 2;
        g.fill(x + 1, barY, x + w - 1, barY + barH, 0xFF000000);
        // 填充部分
        int fillW = (int) ((w - 2) * 0.6f);
        g.fill(x + 1, barY, x + 1 + fillW, barY + barH, color | 0xFF000000);
        // 条纹
        for (int i = x + 5; i < x + fillW; i += 6) {
            g.fill(i, barY + 1, Math.min(i + 2, x + fillW), barY + barH - 1,
                    0x30FFFFFF);
        }
    }

    /** 聊天框预览 */
    private void renderChatPreview(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0x80000000);
        // 虚线分隔
        for (int i = x + 2; i < x + w - 2; i += 4) {
            g.fill(i, y + h - 16, Math.min(i + 2, x + w - 2), y + h - 15,
                    0x44FFFFFF);
        }
        // 模拟聊天行
        int lineH = Math.max(4, (h - 20) / 5);
        for (int i = 0; i < 4 && (y + 4 + i * (lineH + 1) + lineH <= y + h
                - 16); i++) {
            int ly = y + 4 + i * (lineH + 1);
            int lw = (int) ((w - 8) * (0.5f + 0.5f * (4 - i) / 4f));
            g.fill(x + 4, ly, x + 4 + lw, ly + lineH, 0x30FFFFFF);
        }
        // 输入栏
        g.fill(x + 4, y + h - 12, x + w - 4, y + h - 4, 0x20FFFFFF);
    }

    /** 计分板预览 */
    private void renderScoreboardPreview(GuiGraphics g, int x, int y, int w,
            int h) {
        g.fill(x, y, x + w, y + h, 0x88000000);
        // 标题
        g.fill(x, y, x + w, y + Math.min(10, h / 4), 0xAA552200);
        // 行
        int rowH = Math.max(3, (h - 12) / 6);
        for (int i = 0; i < 5; i++) {
            int ry = y + 12 + i * (rowH + 1);
            if (ry + rowH > y + h) break;
            g.fill(x + 2, ry, x + w - 2, ry + rowH,
                    (i % 2 == 0) ? 0x18000000 : 0x08000000);
        }
    }

    /** 玩家列表预览 */
    private void renderPlayerListPreview(GuiGraphics g, int x, int y, int w,
            int h) {
        g.fill(x, y, x + w, y + h, 0x88000000);
        int rowH = Math.max(3, h / 8);
        for (int i = 0; i < 7; i++) {
            int ry = y + 2 + i * (rowH + 1);
            if (ry + rowH > y + h) break;
            // 头像占位
            g.fill(x + 4, ry, x + 4 + rowH, ry + rowH, 0x40666666);
            // 名字占位
            int nameLen = (int) ((w - rowH - 12) * (0.4f + 0.3f * Math.random()));
            g.fill(x + rowH + 8, ry + rowH / 2 - 1, x + rowH + 8 + nameLen,
                    ry + rowH / 2 + 1, 0x30FFFFFF);
        }
    }

    /** 动作栏预览 */
    private void renderActionBarPreview(GuiGraphics g, int x, int y, int w,
            int h) {
        int barW = (int) (w * 0.6f);
        int barH = Math.max(3, h / 2);
        int barX = x + (w - barW) / 2;
        int barY = y + (h - barH) / 2;
        g.fill(barX, barY, barX + barW, barY + barH, 0xAA222222);
        g.fill(barX + 4, barY + 1, barX + barW - 4, barY + barH - 1,
                0x40FFFFFF);
    }

    /** 槽位网格（容器/背包） */
    private void renderSlotGrid(GuiGraphics g, int x, int y, int w, int h,
            int bgColor) {
        int slotSize = Math.min(14, Math.min(w / 9, h / 4));
        int cols = Math.max(1, w / (slotSize + 2));
        int rows = Math.max(1, h / (slotSize + 2));
        int gridW = cols * (slotSize + 2);
        int gridH = rows * (slotSize + 2);
        int sx = x + (w - gridW) / 2;
        int sy = y + (h - gridH) / 2;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slotX = sx + c * (slotSize + 2);
                int slotY = sy + r * (slotSize + 2);
                g.fill(slotX, slotY, slotX + slotSize, slotY + slotSize,
                        bgColor | 0xFF000000);
                g.renderOutline(slotX, slotY, slotSize, slotSize,
                        0xFF555555);
            }
        }
    }

    /** 标题预览 */
    private void renderTitlePreview(GuiGraphics g, int x, int y, int w,
            int h) {
        int centerY = y + h / 2;
        int barH = Math.max(2, h / 3);
        g.fill(x + 4, centerY - barH / 2, x + w - 4, centerY + barH / 2,
                0x60777777);
        // 装饰线
        g.fill(x + 8, centerY - barH / 2, x + w - 8,
                centerY - barH / 2 + 1, 0x40CCCCCC);
        g.fill(x + 8, centerY + barH / 2 - 1, x + w - 8,
                centerY + barH / 2, 0x40CCCCCC);
    }

    // ---------- 缩放手柄 ----------

    private void drawHandle(GuiGraphics g, int smx, int smy, LayoutEntry e,
            int hidx, int hx, int hy, int hs) {
        boolean hover = smx >= hx && smx < hx + hs && smy >= hy
                && smy < hy + hs;
        g.fill(hx, hy, hx + hs, hy + hs,
                hover ? 0xFFFFFF00 : HANDLE_COLOR);
    }

    // ====================== 工具栏 ======================

    private void renderToolbar(GuiGraphics g, int smx, int smy) {
        if (editingEntry == null) return;

        // 分隔线
        g.fill(toolbarX, lineY, toolbarX + toolbarW, lineY + 1, TOOLBAR_SEP);

        // 工具栏背景
        g.fill(toolbarX, toolbarY, toolbarX + toolbarW,
                toolbarY + toolbarH, TOOLBAR_COLOR);

        int pad = 6;
        int innerX = toolbarX + pad;
        int innerW = toolbarW - pad * 2;
        int innerY = toolbarY + pad;
        int innerH = toolbarH - pad * 2;

        // 布局：左侧约70%为滑块区（2行×2列=4滑块），右侧约30%为元素选择+预保存
        int sliderAreaW = innerW * 7 / 10;
        int rightAreaX = innerX + sliderAreaW + 4;
        int rightAreaW = innerW - sliderAreaW - 4;

        // === 左侧：二行二列滑块 (scaleX, scaleY / posX, posY) ===
        int colW = (sliderAreaW - 8) / 2;
        int col1 = innerX;
        int col2 = col1 + colW + 8;
        int rowH = (innerH - 4) / 2 - 2;
        int row1Y = innerY;
        int row2Y = innerY + rowH + 4;

        // 预计算选中值
        float sxVal = selectedWidget != null
                ? (selectedWidget.scaleX - 0.3f) / 2.7f
                : 0.5f;
        float syVal = selectedWidget != null
                ? (selectedWidget.scaleY - 0.3f) / 2.7f
                : 0.5f;
        float xVal = selectedWidget != null
                ? selectedWidget.relX()
                : 0.5f;
        float yVal = selectedWidget != null
                ? selectedWidget.relY()
                : 0.5f;

        String sxStr = selectedWidget != null
                ? String.format("x%.1f", selectedWidget.scaleX) : "-";
        String syStr = selectedWidget != null
                ? String.format("x%.1f", selectedWidget.scaleY) : "-";
        String xStr = selectedWidget != null
                ? String.format("%.2f", selectedWidget.relX()) : "-";
        String yStr = selectedWidget != null
                ? String.format("%.2f", selectedWidget.relY()) : "-";

        // Row1: scaleX, scaleY
        int labelY1 = row1Y;
        drawToolbarLabel(g, Component.literal("X缩放"), sxStr, col1, labelY1, colW);
        drawToolbarLabel(g, Component.literal("Y缩放"), syStr, col2, labelY1, colW);
        int sliderY1 = labelY1 + this.font.lineHeight + 1;
        renderInteractiveSlider(g, smx, smy, col1, sliderY1, colW, SLIDER_BAR_H,
                sxVal, 0);
        renderInteractiveSlider(g, smx, smy, col2, sliderY1, colW, SLIDER_BAR_H,
                syVal, 1);

        // Row2: posX, posY
        int labelY2 = row2Y;
        drawToolbarLabel(g, Component.translatable("gui.deltaforce.layout.pos_x"),
                xStr, col1, labelY2, colW);
        drawToolbarLabel(g, Component.translatable("gui.deltaforce.layout.pos_y"),
                yStr, col2, labelY2, colW);
        int sliderY2 = labelY2 + this.font.lineHeight + 1;
        renderInteractiveSlider(g, smx, smy, col1, sliderY2, colW, SLIDER_BAR_H,
                xVal, 2);
        renderInteractiveSlider(g, smx, smy, col2, sliderY2, colW, SLIDER_BAR_H,
                yVal, 3);

        // === 右侧：当前选中 + 预保存按钮 ===
        int rightCY = innerY + innerH / 2 - 8;

        // 当前选中元素名称
        int selLabelH = this.font.lineHeight + 2;
        String selLabel = selectedWidget != null
                ? "当前: " + selectedWidget.displayName
                : "未选中元素";
        int slw = this.font.width(selLabel);
        g.drawString(this.font, selLabel,
                rightAreaX + (rightAreaW - Math.min(slw, rightAreaW)) / 2,
                rightCY - selLabelH, 0xFFAACCEE, false);

        // 预保存按钮
        int psBtnW = Math.min(80, rightAreaW);
        int psBtnH = 16;
        int psBtnX = rightAreaX + (rightAreaW - psBtnW) / 2;
        int psBtnY = rightCY + 2;
        boolean psHover = smx >= psBtnX && smx < psBtnX + psBtnW
                && smy >= psBtnY && smy < psBtnY + psBtnH;
        int psBg;
        if (hasUnsavedPreSaves) {
            long elapsed = System.currentTimeMillis() - preSaveFlashStart;
            boolean flash = elapsed < 2000 && (elapsed / 300) % 2 == 0;
            psBg = flash ? (psHover ? 0xC0FFAA00 : 0x90FFAA00)
                    : (psHover ? 0x80FF8800 : 0x50FF8800);
        } else {
            psBg = psHover ? 0x603366CC : 0x403366CC;
        }
        g.fill(psBtnX, psBtnY, psBtnX + psBtnW, psBtnY + psBtnH, psBg);
        String psStr = hasUnsavedPreSaves ? "◆预保存" : "预保存";
        int psw3 = this.font.width(psStr);
        g.drawString(this.font, psStr,
                psBtnX + (psBtnW - psw3) / 2,
                psBtnY + (psBtnH - this.font.lineHeight) / 2,
                0xFFFFFFFF, false);

        // 底部信息行
        if (selectedWidget != null) {
            int infoY = innerY + innerH - this.font.lineHeight - 2;
            String info = String.format(
                    "位:(%.3f,%.3f) 尺:%.3fx%.3f 缩XY:%.2f/%.2f",
                    selectedWidget.relX(), selectedWidget.relY(),
                    selectedWidget.relWidth, selectedWidget.relHeight,
                    selectedWidget.scaleX, selectedWidget.scaleY);
            int iw = this.font.width(info);
            if (iw > innerW) {
                info = String.format("X:%.3f Y:%.3f W:%.3f H:%.3f SX:%.2f SY:%.2f",
                        selectedWidget.relX(), selectedWidget.relY(),
                        selectedWidget.relWidth, selectedWidget.relHeight,
                        selectedWidget.scaleX, selectedWidget.scaleY);
            }
            g.drawString(this.font, info, innerX, infoY, 0x88CCCCCC, false);
        }
    }

    private void drawToolbarLabel(GuiGraphics g, Component label,
            String value, int x, int y, int maxW) {
        String text = label.getString() + ": " + value;
        int tw = this.font.width(text);
        if (tw > maxW) text = label.getString();
        g.drawString(this.font, text, x, y, 0xFFCCCCCC, false);
    }

    /**
     * 可交互的滑块渲染
     */
    private void renderInteractiveSlider(GuiGraphics g, int smx, int smy,
            int x, int y, int w, int h, float val, int sliderIndex) {
        boolean isActive = (activeSlider == sliderIndex);

        // 轨道背景
        g.fill(x, y, x + w, y + h, 0xFF222222);
        // 已填充部分
        int fillW = (int) (val * w);
        int fillColor = isActive ? 0xFF5577AA : 0xFF444466;
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + h, fillColor);
        }
        // 刻度线
        for (int i = 1; i < 4; i++) {
            int tx = x + w * i / 4;
            g.fill(tx, y, tx + 1, y + h, 0x30FFFFFF);
        }
        // 滑块手柄
        int handleX = x + fillW - SLIDER_HANDLE_W / 2;
        handleX = Math.max(x, Math.min(x + w - SLIDER_HANDLE_W, handleX));
        g.fill(handleX, y - 1, handleX + SLIDER_HANDLE_W, y + h + 1,
                isActive ? 0xFFFFFF00 : 0xFF999999);

        // 当前值标签
        String valStr;
        if (sliderIndex == 0 || sliderIndex == 1) {
            valStr = String.format("%.1f", 0.3f + val * 2.7f);
        } else {
            valStr = String.format("%.2f", val);
        }
        int vw = this.font.width(valStr);
        g.drawString(this.font, valStr,
                x + w + 2, y + (h - this.font.lineHeight) / 2,
                0xFF888888, false);
    }

    // ====================== 右侧列表 ======================

    private void renderScreenList(GuiGraphics g, int mx, int my) {
        g.fill(listAreaX, listAreaY, listAreaX + listAreaW,
                listAreaY + listAreaH, 0x18000000);
        int visS = listScrollY / ENTRY_H;
        int visE = Math.min(screenEntries.size(),
                (listScrollY + listAreaH) / ENTRY_H + 1);
        int oY = listAreaY - (listScrollY % ENTRY_H);
        for (int i = visS; i < visE; i++) {
            int y = oY + (i - visS) * ENTRY_H;
            renderScreenEntry(g, mx, my, screenEntries.get(i), y);
        }
    }

    private void renderScreenEntry(GuiGraphics g, int mx, int my,
            ClientScreenRegistry.Entry entry, int y) {
        boolean hover = mx >= listAreaX && mx < listAreaX + listAreaW
                && my >= y && my < y + ENTRY_H;
        boolean editing = editingEntry != null
                && editingEntry.id().equals(entry.id());
        if (editing)
            g.fill(listAreaX, y, listAreaX + listAreaW, y + ENTRY_H,
                    0x3000AAFF);
        else if (hover)
            g.fill(listAreaX, y, listAreaX + listAreaW, y + ENTRY_H,
                    0x20FFFFFF);

        String name = entry.displayName().getString();
        if (name.equals(entry.id()) && entry.id().startsWith("container."))
            name = entry.id().substring("container.".length());
        else if (name.equals(entry.id()) && entry.id().contains("."))
            name = entry.id().substring(entry.id().lastIndexOf('.') + 1);
        int mc = (listAreaW - CFG_BTN_W - 6)
                / Math.max(1, this.font.width("W"));
        if (name.length() > mc)
            name = name.substring(0, mc - 1) + "...";
        g.drawString(this.font, name, listAreaX + 4,
                y + (ENTRY_H - this.font.lineHeight) / 2,
                0xFFCCCCCC, false);

        int bx = listAreaX + listAreaW - CFG_BTN_W - 4, by = y + 2,
                bh = ENTRY_H - 4;
        boolean bhvr = mx >= bx && mx < bx + CFG_BTN_W && my >= by
                && my < by + bh;
        g.fill(bx, by, bx + CFG_BTN_W, by + bh,
                bhvr ? 0x603366CC : 0x403366CC);
        Component bl = Component
                .translatable("screen.deltaforce.layout_config.configure");
        int bw = this.font.width(bl);
        g.drawString(this.font, bl, bx + (CFG_BTN_W - bw) / 2,
                by + (bh - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    // ====================== 底栏 ======================

    private void renderBottomBar(GuiGraphics g, int mx, int my) {
        int barY = refH - BOTTOM_BAR_H;
        g.fill(0, barY, refW, refH, BOTTOM_BAR_COLOR);
        boolean he = editingEntry != null;
        drawBottomBtn(g, mx, my, saveBtnX, saveBtnY, saveBtnW, saveBtnH,
                Component.translatable("screen.deltaforce.settings.save"), he);
        drawBottomBtn(g, mx, my, cancelBtnX, cancelBtnY, cancelBtnW,
                cancelBtnH,
                Component.translatable("screen.deltaforce.settings.cancel"),
                true);
        drawBottomBtn(g, mx, my, undoBtnX, undoBtnY, undoBtnW, undoBtnH,
                Component.translatable("screen.deltaforce.settings.undo"), he);
        drawBottomBtn(g, mx, my, exitBtnX, exitBtnY, exitBtnW, exitBtnH,
                Component.translatable("screen.deltaforce.settings.exit"),
                true);
    }

    private void drawBottomBtn(GuiGraphics g, int mx, int my,
            int x, int y, int w, int h, Component t, boolean ena) {
        boolean hv = ena && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h,
                ena ? (hv ? 0x60FFFFFF : 0x30FFFFFF) : 0x15FFFFFF);
        int tw = this.font.width(t);
        g.drawString(this.font, t, x + (w - tw) / 2,
                y + (h - this.font.lineHeight) / 2,
                ena ? 0xFFFFFFFF : 0x66FFFFFF, false);
    }

    // ====================== 鼠标事件 ======================

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);
        int smx = (int) ((mx - offX) / uiScale);
        int smy = (int) ((my - offY) / uiScale);

        // 底栏
        if (smy >= refH - BOTTOM_BAR_H) {
            if (inRect(smx, smy, saveBtnX, saveBtnY, saveBtnW, saveBtnH)) {
                if (editingEntry != null) commitSave();
                return true;
            }
            if (inRect(smx, smy, cancelBtnX, cancelBtnY, cancelBtnW,
                    cancelBtnH)) {
                if (editingEntry != null) {
                    stopEditing();
                    return true;
                }
                tryExit();
                return true;
            }
            if (inRect(smx, smy, undoBtnX, undoBtnY, undoBtnW, undoBtnH)) {
                undoSave();
                return true;
            }
            if (inRect(smx, smy, exitBtnX, exitBtnY, exitBtnW, exitBtnH)) {
                tryExit();
                return true;
            }
        }

        // 工具栏按钮和滑块
        if (editingEntry != null && smy >= toolbarY
                && smy < toolbarY + toolbarH) {
            int pad = 6;
            int innerX = toolbarX + pad;
            int innerW = toolbarW - pad * 2;
            int innerY = toolbarY + pad;
            int innerH = toolbarH - pad * 2;
            int sliderAreaW = innerW * 7 / 10;
            int rightAreaX = innerX + sliderAreaW + 4;
            int rightAreaW = innerW - sliderAreaW - 4;

            // 检查滑块点击
            int sliderResult = checkSliderClick(smx, smy);
            if (sliderResult >= 0) {
                activeSlider = sliderResult;
                applySliderValue(smx, smy, sliderResult);
                return true;
            }

            // 预保存按钮
            int rightCY = innerY + innerH / 2 - 8;
            int psBtnW = Math.min(80, rightAreaW);
            int psBtnH = 16;
            int psBtnX = rightAreaX + (rightAreaW - psBtnW) / 2;
            int psBtnY = rightCY + 2;
            if (inRect(smx, smy, psBtnX, psBtnY, psBtnW, psBtnH)) {
                preSaveToBuffer();
                return true;
            }
            return true;
        }

        // 右侧列表"配置此页"
        ClientScreenRegistry.Entry le = findListEntry(smx, smy);
        if (le != null) {
            int idx = screenEntries.indexOf(le);
            int y = listAreaY + idx * ENTRY_H - listScrollY;
            int bx = listAreaX + listAreaW - CFG_BTN_W - 4, bh = ENTRY_H - 4;
            if (smx >= bx && smx < bx + CFG_BTN_W && smy >= y + 2
                    && smy < y + 2 + bh) {
                startEditing(le);
                return true;
            }
        }

        // 预览区控件 — 真渲染模式下点击元素可选中/拖拽
        if (editingEntry != null && inPreview(smx, smy)) {
            if (editingScreen != null) {
                // 先检测缩放手柄
                for (LayoutEntry e : editElementList) {
                    int wx = prevX + (int) (e.relX() * prevW);
                    int wy = prevY + (int) (e.relY() * prevH);
                    int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
                    int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));
                    int h = hitHandle(smx, smy, wx, wy, ww, wh);
                    if (h >= 0) {
                        dragWidget = e;
                        dragHandle = h;
                        dragStartMX = smx;
                        dragStartMY = smy;
                        dragStartRelX = e.relX();
                        dragStartRelY = e.relY();
                        dragStartScaleX = e.scaleX;
                        dragStartScaleY = e.scaleY;
                        selectedWidget = e;
                        return true;
                    }
                }
                // 点击元素体：直接开始位置拖拽
                LayoutEntry clicked = findElementAtPosition(smx, smy);
                if (clicked != null) {
                    dragWidget = clicked;
                    dragHandle = -1;
                    dragStartMX = smx;
                    dragStartMY = smy;
                    dragStartRelX = clicked.relX();
                    dragStartRelY = clicked.relY();
                    dragStartScaleX = clicked.scaleX;
                    dragStartScaleY = clicked.scaleY;
                    selectedWidget = clicked;
                } else {
                    selectedWidget = null;
                }
                return true;
            }
            return handlePreviewClick(smx, smy);
        }

        // 点击空白取消选中
        if (editingEntry != null) {
            selectedWidget = null;
        }

        return super.mouseClicked(mx, my, btn);
    }

    /** 在预览区像素坐标下查找命中的元素（从后往前=上层优先） */
    private LayoutEntry findElementAtPosition(int smx, int smy) {
        for (int i = editElementList.size() - 1; i >= 0; i--) {
            LayoutEntry e = editElementList.get(i);
            if (!e.enabled) continue;
            int wx = prevX + (int) (e.relX() * prevW);
            int wy = prevY + (int) (e.relY() * prevH);
            int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
            int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));
            if (smx >= wx && smx < wx + ww && smy >= wy && smy < wy + wh) {
                return e;
            }
        }
        return null;
    }

    /** 开始拖拽指定元素 */
    private void startDragElement(LayoutEntry e, int smx, int smy) {
        dragWidget = e;
        dragHandle = -1;
        dragStartMX = smx;
        dragStartMY = smy;
        dragStartRelX = e.relX();
        dragStartRelY = e.relY();
        dragStartScaleX = e.scaleX;
        dragStartScaleY = e.scaleY;
    }

    /**
     * 检查是否点击了滑块区域，返回滑块索引(-1=未命中, 0=缩放, 1=X, 2=Y)
     */
    private int checkSliderClick(int smx, int smy) {
        int pad = 6;
        int innerX = toolbarX + pad;
        int innerW = toolbarW - pad * 2;
        int sliderAreaW = innerW * 7 / 10;
        int innerY = toolbarY + pad;
        int innerH = toolbarH - pad * 2;
        int colW = (sliderAreaW - 8) / 2;
        int rowH = (innerH - 4) / 2 - 2;
        int row1Y = innerY;
        int row2Y = innerY + rowH + 4;
        int col1 = innerX;
        int col2 = col1 + colW + 8;
        int labH = this.font.lineHeight;
        int sliderY1 = row1Y + labH + 1;
        int sliderY2 = row2Y + labH + 1;
        int sh = SLIDER_BAR_H;

        // Row1: scaleX(0) / scaleY(1)
        if (smy >= sliderY1 - 2 && smy <= sliderY1 + sh + 2) {
            if (smx >= col1 && smx < col1 + colW + 20) return 0;
            if (smx >= col2 && smx < col2 + colW + 20) return 1;
        }
        // Row2: posX(2) / posY(3)
        if (smy >= sliderY2 - 2 && smy <= sliderY2 + sh + 2) {
            if (smx >= col1 && smx < col1 + colW + 20) return 2;
            if (smx >= col2 && smx < col2 + colW + 20) return 3;
        }
        return -1;
    }

    /**
     * 根据滑块索引和鼠标位置更新选中控件属性
     */
    private void applySliderValue(int smx, int smy, int sliderIndex) {
        if (selectedWidget == null) return;
        int pad = 6;
        int innerX = toolbarX + pad;
        int innerW = toolbarW - pad * 2;
        int sliderAreaW = innerW * 7 / 10;
        int colW = (sliderAreaW - 8) / 2;

        int col1 = innerX;
        int col2 = col1 + colW + 8;

        int sliderX;
        int sliderW;
        switch (sliderIndex) {
            case 0: case 2: sliderX = col1; sliderW = colW; break;
            case 1: case 3: sliderX = col2; sliderW = colW; break;
            default: return;
        }

        float t = clamp((smx - sliderX) / (float) Math.max(1, sliderW), 0f, 1f);

        switch (sliderIndex) {
            case 0:
                // scaleX: 0.3 ~ 3.0
                selectedWidget.scaleX = 0.3f + t * 2.7f;
                break;
            case 1:
                // scaleY: 0.3 ~ 3.0
                selectedWidget.scaleY = 0.3f + t * 2.7f;
                break;
            case 2:
                // X位置: 0.0 ~ 1.0
                selectedWidget.offsetX = t - selectedWidget.anchorX;
                break;
            case 3:
                // Y位置: 0.0 ~ 1.0
                selectedWidget.offsetY = t - selectedWidget.anchorY;
                break;
        }
    }

    // ====================== 预览区交互 ======================

    private boolean inPreview(int mx, int my) {
        return mx >= prevX && mx < prevX + prevW && my >= prevY
                && my < prevY + prevH;
    }

    private boolean handlePreviewClick(int smx, int smy) {
        // 手柄优先
        for (LayoutEntry e : editElementList) {
            int wx = prevX + (int) (e.relX() * prevW);
            int wy = prevY + (int) (e.relY() * prevH);
            int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
            int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));
            int h = hitHandle(smx, smy, wx, wy, ww, wh);
            if (h >= 0) {
                dragWidget = e;
                dragHandle = h;
                dragStartMX = smx;
                dragStartMY = smy;
                dragStartRelX = e.relX();
                dragStartRelY = e.relY();
                dragStartScaleX = e.scaleX;
                dragStartScaleY = e.scaleY;
                selectedWidget = e;
                return true;
            }
        }
        // 控件体
        for (LayoutEntry e : editElementList) {
            int wx = prevX + (int) (e.relX() * prevW);
            int wy = prevY + (int) (e.relY() * prevH);
            int ww = Math.max(MIN_WIDGET, (int) (e.relWidth * prevW * e.scaleX));
            int wh = Math.max(MIN_WIDGET, (int) (e.relHeight * prevH * e.scaleY));
            if (smx >= wx && smx < wx + ww && smy >= wy && smy < wy + wh) {
                dragWidget = e;
                dragHandle = -1;
                dragStartMX = smx;
                dragStartMY = smy;
                dragStartRelX = e.relX();
                dragStartRelY = e.relY();
                dragStartScaleX = e.scaleX;
                dragStartScaleY = e.scaleY;
                selectedWidget = e;
                return true;
            }
        }
        selectedWidget = null;
        return false;
    }

    private int hitHandle(int mx, int my, int wx, int wy, int ww, int wh) {
        int hs = HANDLE_SIZE;
        if (mx >= wx && mx < wx + hs && my >= wy && my < wy + hs)
            return 0;
        if (mx >= wx + ww - hs && mx < wx + ww && my >= wy && my < wy + hs)
            return 2;
        if (mx >= wx + ww - hs && mx < wx + ww && my >= wy + wh - hs
                && my < wy + wh)
            return 4;
        if (mx >= wx && mx < wx + hs && my >= wy + wh - hs && my < wy + wh)
            return 6;
        if (mx >= wx + ww / 2 - hs / 2 && mx < wx + ww / 2 + hs / 2
                && my >= wy && my < wy + hs)
            return 1;
        if (mx >= wx + ww - hs && mx < wx + ww
                && my >= wy + wh / 2 - hs / 2 && my < wy + wh / 2 + hs / 2)
            return 3;
        if (mx >= wx + ww / 2 - hs / 2 && mx < wx + ww / 2 + hs / 2
                && my >= wy + wh - hs && my < wy + wh)
            return 5;
        if (mx >= wx && mx < wx + hs && my >= wy + wh / 2 - hs / 2
                && my < wy + wh / 2 + hs / 2)
            return 7;
        return -1;
    }

    // ====================== 拖拽 ======================

    @Override
    public boolean mouseDragged(double mx, double my, int btn,
            double dx, double dy) {
        int smx = (int) ((mx - offX) / uiScale);
        int smy = (int) ((my - offY) / uiScale);

        // 滑块拖拽
        if (activeSlider >= 0) {
            applySliderValue(smx, smy, activeSlider);
            return true;
        }

        // 控件拖拽
        if (dragWidget == null)
            return super.mouseDragged(mx, my, btn, dx, dy);

        float rdx = (smx - dragStartMX) / (float) Math.max(1, prevW);
        float rdy = (smy - dragStartMY) / (float) Math.max(1, prevH);
        int h = dragHandle;
        float nrX = dragStartRelX, nrY = dragStartRelY;
        float nsX = dragStartScaleX, nsY = dragStartScaleY;
        float minS = MIN_WIDGET / ((float) Math.max(1, prevW) * Math.max(0.001f, dragWidget.relWidth));

        if (h == -1) {
            // 整体拖拽：移动位置，缩放不变
            nrX = clamp(dragStartRelX + rdx, 0f, 1f - dragWidget.relWidth * nsX);
            nrY = clamp(dragStartRelY + rdy, 0f, 1f - dragWidget.relHeight * nsY);
        } else {
            // 手柄缩放：改变scaleX/scaleY，relWidth/relHeight保持不动
            // 右边缘手柄 (2,3,4)：水平拉伸，位置不变，scaleX改变
            if (h == 2 || h == 3 || h == 4) {
                nsX = Math.max(minS, dragStartScaleX + rdx / Math.max(0.001f, dragWidget.relWidth));
            }
            // 左边缘手柄 (0,6,7)：水平拉伸，位置改变，scaleX改变
            if (h == 0 || h == 6 || h == 7) {
                float dxRel = rdx;
                nsX = Math.max(minS, dragStartScaleX - dxRel / Math.max(0.001f, dragWidget.relWidth));
                nrX = dragStartRelX + dragWidget.relWidth * (dragStartScaleX - nsX);
                if (nrX < 0) { nsX = dragStartScaleX + dragStartRelX / Math.max(0.001f, dragWidget.relWidth); nrX = 0; }
            }
            // 下边缘手柄 (4,5,6)：垂直拉伸，位置不变，scaleY改变
            if (h == 4 || h == 5 || h == 6) {
                nsY = Math.max(minS, dragStartScaleY + rdy / Math.max(0.001f, dragWidget.relHeight));
            }
            // 上边缘手柄 (0,1,2)：垂直拉伸，位置改变，scaleY改变
            if (h == 0 || h == 1 || h == 2) {
                float dyRel = rdy;
                nsY = Math.max(minS, dragStartScaleY - dyRel / Math.max(0.001f, dragWidget.relHeight));
                nrY = dragStartRelY + dragWidget.relHeight * (dragStartScaleY - nsY);
                if (nrY < 0) { nsY = dragStartScaleY + dragStartRelY / Math.max(0.001f, dragWidget.relHeight); nrY = 0; }
            }
            // 限制最大比例不超出屏幕
            if (nrX + dragWidget.relWidth * nsX > 1.0f) nsX = (1.0f - nrX) / Math.max(0.001f, dragWidget.relWidth);
            if (nrY + dragWidget.relHeight * nsY > 1.0f) nsY = (1.0f - nrY) / Math.max(0.001f, dragWidget.relHeight);
        }
        dragWidget.offsetX = nrX - dragWidget.anchorX;
        dragWidget.offsetY = nrY - dragWidget.anchorY;
        dragWidget.scaleX = nsX;
        dragWidget.scaleY = nsY;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (activeSlider >= 0) {
            activeSlider = -1;
            return true;
        }
        if (dragWidget != null) {
            dragWidget = null;
            dragHandle = -1;
            return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx,
            double sy) {
        int smx = (int) ((mx - offX) / uiScale);
        int smy = (int) ((my - offY) / uiScale);
        // 右侧列表滚动
        if (smx >= listAreaX && smx < listAreaX + listAreaW
                && smy >= listAreaY && smy < listAreaY + listAreaH) {
            listScrollY = Math.max(0,
                    Math.min(listMaxScroll, listScrollY - (int) (sy * ENTRY_H)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // ====================== 键盘 ======================

    @Override
    public boolean keyPressed(int kc, int sc, int m) {
        if (kc == 256) { // Escape
            if (editingEntry != null) {
                stopEditing();
                return true;
            }
            tryExit();
            return true;
        }
        return super.keyPressed(kc, sc, m);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ====================== 辅助 ======================

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private ClientScreenRegistry.Entry findListEntry(int smx, int smy) {
        if (smx < listAreaX || smx >= listAreaX + listAreaW
                || smy < listAreaY || smy >= listAreaY + listAreaH)
            return null;
        int idx = (smy - listAreaY + listScrollY) / ENTRY_H;
        if (idx < 0 || idx >= screenEntries.size())
            return null;
        return screenEntries.get(idx);
    }

    private static float clamp(float v, float mi, float ma) {
        return v < mi ? mi : Math.min(v, ma);
    }
}