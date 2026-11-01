package mccfk.DeltaForce.java.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.rzy.dealt_force_skills.client.ClientHudLayout;
import com.rzy.dealt_force_skills.client.StaminaBarRenderer;
import com.rzy.dealt_force_skills.client.config.ConfigLabelHelper;
import com.rzy.dealt_force_skills.config.ConfigAccess;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import com.rzy.dealt_force_skills.network.C2S_RequestConfigSnapshot;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.SettingsUIConfig;
import mccfk.DeltaForce.java.client.DFClientConfig;
import mccfk.DeltaForce.java.client.overlay.GroundItemOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * SettingsScreen 设置界面
 * 顶栏：模组标签页（贴图按钮）
 * 副栏：主分类（贴图按钮缩小版）
 * 子分类栏：副分类（贴图按钮更小版）
 * 内容区：配置项列表，右侧根据类型绘制不同控件
 * 底栏：保存/取消/退出，未保存退出时弹窗提示
 * 所有文本均通过本地化键名显示中文
 */
public class SettingsScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsScreen.class);

    private static final int BG_COLOR = 0xFF0E1819;
    private static final float REFERENCE_WIDTH = 960f;
    private static final float REFERENCE_HEIGHT = 540f;
    private static final int CAT_BAR_BG = 0x20222222;
    private static final int SUBCAT_BAR_BG = 0x181A1A1A;
    private static final int ENTRY_BG = 0x20AAAAAA;
    private static final int ENTRY_BG_HOVER = 0x40AAAAAA;
    private static final int ENTRY_ROW_HEIGHT_DEFAULT = 26;
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int SCROLL_BAR_COLOR = 0x60AAAAAA;
    private static final int SCROLL_BAR_HANDLE_COLOR = 0xAAFFFFFF;
    private static final int BOTTOM_BAR_HEIGHT = 20;
    private static final int BOTTOM_BAR_COLOR = 0x30333333;
    private static final int TOGGLE_ON_COLOR = 0xFF44AA44;
    private static final int TOGGLE_OFF_COLOR = 0xFFAA4444;
    private static final int SLIDER_BG = 0x40AAAAAA;
    private static final int SLIDER_HANDLE = 0xAAFFFFFF;
    private static final int INPUT_BG = 0x30111111;
    private static final int INPUT_BORDER = 0x60FFFFFF;

    private static final float UI_SCALE = 0.75F;
    private static final int TAB_BASE_WIDTH = 91;
    private static final int TAB_BASE_HEIGHT = 32;
    private static final int TAB_START_X_RATIO = 1;  // 标签栏距左边缘 = 屏幕宽度 * 1 / 100
    private static final int TAB_START_Y = 10;        // 标签栏距上边缘（固定值）
    private static final int BOTTOM_BTN_MARGIN_RATIO = 1;  // 底栏按钮距右边缘 = 屏幕宽度 * 1 / 100

    private static final ResourceLocation TABS_OFF = new ResourceLocation("deltaforce", "textures/gui/main_tabs_off.png");
    private static final ResourceLocation TABS_ON = new ResourceLocation("deltaforce", "textures/gui/main_tabs_on.png");

    // 界面设置（持久化）
    private static int entryRowHeight = ENTRY_ROW_HEIGHT_DEFAULT;
    private boolean showSettingsOverlay = false;

    // 已记录过的缺失翻译（避免日志重复）

    // 布局
    private int tabWidth, tabHeight, tabStartX, tabStartY;
    private int catBarY, catBarH = 16;
    private int subcatBarY, subcatBarH = 14;
    private int contentTop, contentBottom;
    private int refW, refH;
    private float uiScale = 1.0f;
    private int offsetX = 0;
    private int offsetY = 0;

    // 模组标签页
    private final List<String> loadedModIds = new ArrayList<>();
    private final List<Component> loadedModNames = new ArrayList<>();
    private int selectedModTab = 0;

    // 分类
    private final List<String> categories = new ArrayList<>();
    private int selectedCat = 0;

    // 子分类
    private final List<String> subCategories = new ArrayList<>();
    private int selectedSubcat = 0;

    // 滚动
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int subcatScrollOffset = 0;
    private int subcatMaxScroll = 0;
    private boolean isDraggingSubcatBar = false;
    private boolean isDraggingMainScrollBar = false;

    // 编辑状态
    private final Map<String, String> dirtyValues = new HashMap<>();
    private String focusedEditKey = null;
    private final Map<String, Boolean> sliderMode = new HashMap<>();

    // 拖拽锁定：记录当前正在拖拽的滑块路径，拖拽期间只响应此滑块
    private String draggingSliderPath = null;
    // 滑块拖拽起始值：拖拽过程中用起始值计算range，避免实时值变化导致range膨胀（正反馈溢出）
    private double sliderDragStartValue = 0;
    private boolean hasSliderDragStart = false;

    // 按键捕获（独立按键设置）
    private String capturingKeyPath = null;
    private final List<Integer> capturedKeys = new ArrayList<>(); // 组合键捕获列表（最多5键）

    // 搜索
    private String searchText = "";
    private boolean searchFocused = false;
    private String globalSearchText = "";
    private boolean globalSearchFocused = false;

    // 悬停延迟提示
    private String hoveredEntryPath = null;
    private IConfigProvider.ConfigEntry hoveredEntry = null;
    private long hoverStartTime = 0;
    private static final long HOVER_DELAY_MS = 1000;

    // 性能优化：翻译缓存（翻译不随运行时变化，永久缓存）
    private final Map<String, String> segmentTranslationCache = new HashMap<>();
    private final Map<String, Component> leafTranslationCache = new HashMap<>();
    private final Map<String, String> contextTranslationCache = new HashMap<>();

    // 性能优化：条目列表缓存（仅在分类/搜索变更时重新计算）
    private List<IConfigProvider.ConfigEntry> cachedEntries = null;
    private int cachedModTab = -1;
    private int cachedCat = -1;
    private int cachedSubcat = -1;
    private String cachedSearchText = null;
    private String cachedGlobalSearchText = null;

    // 预览HUD按钮
    private boolean isPreviewing = false;
    private int previewBtnX, previewBtnY, previewBtnW, previewBtnH;
    private long previewStartTime = 0; // 预览开始时间（用于品质/模型循环）
    private ItemStack currentPreviewItem = null;
    private List<ItemStack> previewItemPool = null; // 预览物品池（创造标签页物品）
    private long lastModelCycle = -1;
    private final Random previewRandom = new Random();

    // HUD布局拖拽（在预览模式下拖动技能按钮/体力条）
    private static final int HUD_DRAG_NONE = 0;
    private static final int HUD_DRAG_SKILLS = 1;
    private static final int HUD_DRAG_STAMINA = 2;
    private int hudDragTarget = HUD_DRAG_NONE;
    private double hudDragStartMouseX, hudDragStartMouseY;
    private int hudDragStartAnchorX, hudDragStartAnchorY;
    // 元素视觉边界（屏幕坐标），在renderHudLayoutPreview中计算
    private int hudSkillsVisX, hudSkillsVisY, hudSkillsVisW, hudSkillsVisH;
    private int hudStaminaVisX, hudStaminaVisY, hudStaminaVisW, hudStaminaVisH;

    // 底栏按钮
    private int saveBtnX, saveBtnY, saveBtnW, saveBtnH;
    private int cancelBtnX, cancelBtnY, cancelBtnW, cancelBtnH;
    private int exitBtnX, exitBtnY, exitBtnW, exitBtnH;
    private int undoBtnX, undoBtnY, undoBtnW, undoBtnH;
    private int redoBtnX, redoBtnY, redoBtnW, redoBtnH;

    // 撤销/重做历史（记录每次配置变更，用于逐步撤销）
    private final Deque<ChangeRecord> undoStack = new ArrayDeque<>();
    private final Deque<ChangeRecord> redoStack = new ArrayDeque<>();

    private boolean showUnsavedDialog = false;

    // 网络快照接收
    private static SettingsScreen active;
    private final Screen parent;

    public SettingsScreen() {
        this(null);
    }

    public SettingsScreen(Screen parent) {
        super(Component.translatable("screen.deltaforce.settings"));
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    public static void acceptServerSnapshot(List<ConfigEntryData> entries, boolean canEdit) {
        var provider = DeltaForce.getConfigProvider("dealt_force_skills");
        if (provider instanceof DfsConfigProvider dfs) {
            // 无论设置界面是否打开，都应更新DfsConfigProvider的权限状态
            // 防止玩家登录时设置界面未打开导致权限信息丢失
            dfs.acceptSnapshot(entries, canEdit);
        }
        if (active != null) {
            active.refreshCategories();
            active.invalidateEntriesCache();
        }
    }

    public static void acceptApplyResult(boolean success, int count, String code) {
        var provider = DeltaForce.getConfigProvider("dealt_force_skills");
        if (provider instanceof DfsConfigProvider dfs) {
            dfs.acceptApplyResult(success, count, code);
        }
        if (active != null) {
            active.dirtyValues.clear();
            active.invalidateEntriesCache();
        }
    }

    @Override
    protected void init() {
        super.init();
        active = this;
        // 参考画布按放大倍数缩小（960/倍数 x 540/倍数）再等比塞进 GUI 空间，
        // 这样界面尺寸 2 下字与控件等比放大，且画布不会超出屏幕（详见 UiCanvasScaleMath）。
        // 倍数来源：玩家没手动设置过时按当前界面尺寸自动选（>= 3 → 1.0，< 3 → 1.5），
        // 手动设置过则一直用玩家的值。
        DFClientConfig cfg = DFClientConfig.getInstance();
        double multiplier = cfg.resolveUiScale(Minecraft.getInstance().getWindow().getGuiScale());
        this.refW = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_WIDTH, multiplier);
        this.refH = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_HEIGHT, multiplier);
        this.uiScale = UiCanvasScaleMath.fitScale(this.width, this.height, this.refW, this.refH);
        this.offsetX = UiCanvasScaleMath.canvasOffset(this.width, this.refW, this.uiScale);
        this.offsetY = UiCanvasScaleMath.canvasOffset(this.height, this.refH, this.uiScale);
        // 根据屏幕大小计算位置（按比例，不额外缩放）
        this.tabWidth = (int) (TAB_BASE_WIDTH * UI_SCALE);
        this.tabHeight = (int) (TAB_BASE_HEIGHT * UI_SCALE);
        this.tabStartX = (int)(REFERENCE_WIDTH * TAB_START_X_RATIO / 100) + cfg.getTabXOffset();
        this.tabStartY = TAB_START_Y + cfg.getTabYOffset();

        // 恢复上次的界面状态
        this.selectedModTab = SettingsUIConfig.settingsTabIndex();
        this.scrollOffset = SettingsUIConfig.settingsScrollOffset();
        entryRowHeight = SettingsUIConfig.settingsRowHeight();

        loadedModIds.clear();
        loadedModNames.clear();
        ModList modList = ModList.get();

        for (String modId : DeltaForce.getConfigWhitelist()) {
            if (modList.isLoaded(modId)) {
                loadedModIds.add(modId);
                loadedModNames.add(Component.translatable("mod." + modId));
            }
        }

        if (selectedModTab >= loadedModNames.size()) {
            selectedModTab = 0;
        }

        this.catBarY = tabStartY + tabHeight + 4;
        this.subcatBarY = catBarY + catBarH + 2;
        this.contentTop = subcatBarY + subcatBarH + 4;
        this.contentBottom = this.refH - BOTTOM_BAR_HEIGHT - 4;

        refreshCategories();

        int barY = this.refH - BOTTOM_BAR_HEIGHT;
        int btnW = 60;
        int btnH = BOTTOM_BAR_HEIGHT;
        int rightMargin = this.refW * BOTTOM_BTN_MARGIN_RATIO / 100;
        exitBtnX = this.refW - rightMargin - btnW; exitBtnY = barY; exitBtnW = btnW; exitBtnH = btnH;
        cancelBtnX = exitBtnX - 6 - btnW; cancelBtnY = barY; cancelBtnW = btnW; cancelBtnH = btnH;
        saveBtnX = cancelBtnX - 6 - btnW; saveBtnY = barY; saveBtnW = btnW; saveBtnH = btnH;
        // 撤销/重做按钮（小按钮，放在保存按钮左侧）
        int smallBtnW = 24;
        redoBtnX = saveBtnX - 4 - smallBtnW; redoBtnY = barY; redoBtnW = smallBtnW; redoBtnH = btnH;
        undoBtnX = redoBtnX - 2 - smallBtnW; undoBtnY = barY; undoBtnW = smallBtnW; undoBtnH = btnH;
        previewBtnW = 50; previewBtnH = btnH;
        previewBtnX = undoBtnX - 6 - previewBtnW; previewBtnY = barY;

        showUnsavedDialog = false;
        // 每次进入设置界面清空撤销历史
        undoStack.clear();
        redoStack.clear();

        // 每次打开设置界面时向服务端请求最新配置权限快照
        // 确保玩家获得/失去OP后立即反映，无需重进服务器
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkHandler.sendToServer(new C2S_RequestConfigSnapshot());
        }
    }

    private void refreshCategories() {
        categories.clear();
        subCategories.clear();
        IConfigProvider provider = getCurrentProvider();
        if (provider != null) {
            categories.addAll(provider.getCategories());
            if (selectedCat >= categories.size()) selectedCat = 0;
            if (!categories.isEmpty()) {
                subCategories.addAll(provider.getSubCategories(categories.get(selectedCat)));
                if (selectedSubcat >= subCategories.size()) selectedSubcat = 0;
            }
        }
        scrollOffset = 0;
    }

    private List<IConfigProvider.ConfigEntry> getCurrentEntries() {
        IConfigProvider provider = getCurrentProvider();
        if (provider == null || categories.isEmpty() || subCategories.isEmpty()) {
            return List.of();
        }
        String cat = categories.get(selectedCat);
        String sub = subCategories.get(selectedSubcat);
        return provider.getEntries(cat, sub);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        float inv = 1.0f / this.uiScale;
        int smx = (int)((mouseX - this.offsetX) * inv);
        int smy = (int)((mouseY - this.offsetY) * inv);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.offsetX, this.offsetY, 0);
        guiGraphics.pose().scale(this.uiScale, this.uiScale, 1.0f);

        guiGraphics.fill(0, 0, refW, refH, BG_COLOR);

        renderTopBar(guiGraphics, smx, smy);
        renderCategoryBar(guiGraphics, smx, smy);
        renderSubCategoryBar(guiGraphics, smx, smy);
        renderEntries(guiGraphics, smx, smy);
        renderBottomBar(guiGraphics, smx, smy);

        // 状态信息（保存成功/失败/校验错误等）
        IConfigProvider currentProvider = getCurrentProvider();
        String status = currentProvider != null ? currentProvider.status() : "";
        if (!status.isBlank()) {
            guiGraphics.drawString(font, font.plainSubstrByWidth(status, Math.max(20, previewBtnX - 20)),
                    10, refH - 14, 0xFFE2B86C, false);
        }

        if (showSettingsOverlay) {
            renderSettingsOverlay(guiGraphics, smx, smy);
        }

        if (showUnsavedDialog) {
            renderUnsavedDialog(guiGraphics, smx, smy);
        }

        guiGraphics.pose().popPose();

        // 预览HUD（在屏幕坐标中渲染，与游戏内HUD一致）
        if (isPreviewing) {
            renderPreviewHud(guiGraphics, mouseX, mouseY);
        }
    }

    // ======================== 顶栏：模组标签页 ========================

    private void renderTopBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 设置按钮（齿轮图标，在最前面）
        int gearSize = tabHeight;
        int gearX = tabStartX;
        int gearY = tabStartY;
        boolean hoverGear = mouseX >= gearX && mouseX < gearX + gearSize && mouseY >= gearY && mouseY < gearY + gearSize;
        int gearBg = hoverGear ? 0x604488FF : 0x303366CC;
        guiGraphics.fill(gearX, gearY, gearX + gearSize, gearY + gearSize, gearBg);
        guiGraphics.renderOutline(gearX, gearY, gearSize, gearSize, 0x80FFFFFF);
        Component gearLabel = Component.literal("\u2699");
        int gearTextW = this.font.width(gearLabel);
        guiGraphics.drawString(this.font, gearLabel, gearX + (gearSize - gearTextW) / 2,
                gearY + (gearSize - this.font.lineHeight) / 2, 0xFFFFFFFF, false);

        int tabStartOffset = gearX + gearSize + 4;
        int tabCount = loadedModNames.size();
        for (int i = 0; i < tabCount; i++) {
            ResourceLocation tex = (i == selectedModTab) ? TABS_ON : TABS_OFF;
            int x = tabStartOffset + i * tabWidth;
            guiGraphics.blit(tex, x, tabStartY, 0, 0, tabWidth, tabHeight, tabWidth, tabHeight);

            Component tabTitle = loadedModNames.get(i);
            int textWidth = this.font.width(tabTitle);
            int textX = x + (tabWidth - textWidth) / 2;
            int textY = tabStartY + (tabHeight - this.font.lineHeight) / 2;
            int textColor = (i == selectedModTab) ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, tabTitle, textX, textY, textColor, false);
        }
    }

    // ======================== 副栏：主分类（贴图缩小版） ========================

    private void renderCategoryBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, catBarY, refW, catBarY + catBarH, CAT_BAR_BG);

        int x = 8;
        int gap = 3;
        for (int i = 0; i < categories.size(); i++) {
            String cat = categories.get(i);
            Component label = localizeCategory(getCurrentModId(), cat);
            int textW = this.font.width(label) + 8;
            int tabH = catBarH - 2;
            int tabY = catBarY + 1;

            ResourceLocation tex = (i == selectedCat) ? TABS_ON : TABS_OFF;
            guiGraphics.blit(tex, x, tabY, 0, 0, textW, tabH, textW, tabH);

            int textColor = (i == selectedCat) ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, label, x + 4, tabY + (tabH - this.font.lineHeight) / 2, textColor, false);
            x += textW + gap;
        }
    }

    // ======================== 子分类栏（贴图更小版，支持滚动） ========================

    private void renderSubCategoryBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (subCategories.isEmpty()) return;
        guiGraphics.fill(0, subcatBarY, refW, subcatBarY + subcatBarH, SUBCAT_BAR_BG);

        // 计算总宽度
        int totalWidth = 8;
        int gap = 2;
        for (String sub : subCategories) {
            Component label = localizeSubCategory(getCurrentModId(), sub);
            totalWidth += this.font.width(label) + 6 + gap;
        }

        // 计算最大滚动
        subcatMaxScroll = Math.max(0, totalWidth - refW);
        subcatScrollOffset = Math.max(0, Math.min(subcatScrollOffset, subcatMaxScroll));

        // 裁剪区域 (屏幕坐标)
        guiGraphics.enableScissor(
                offsetX, offsetY + (int)(subcatBarY * uiScale),
                offsetX + (int)(refW * uiScale), offsetY + (int)((subcatBarY + subcatBarH) * uiScale));

        int x = 8 - subcatScrollOffset;
        for (int i = 0; i < subCategories.size(); i++) {
            String sub = subCategories.get(i);
            Component label = localizeSubCategory(getCurrentModId(), sub);
            int textW = this.font.width(label) + 6;
            int tabH = subcatBarH - 2;
            int tabY = subcatBarY + 1;

            ResourceLocation tex = (i == selectedSubcat) ? TABS_ON : TABS_OFF;
            guiGraphics.blit(tex, x, tabY, 0, 0, textW, tabH, textW, tabH);

            int textColor = (i == selectedSubcat) ? 0xFFFFFFFF : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, label, x + 3, tabY + (tabH - this.font.lineHeight) / 2, textColor, false);
            x += textW + gap;
        }

        guiGraphics.disableScissor();

        // 绘制滚动条
        if (subcatMaxScroll > 0) {
            int scrollBarWidth = 4;
            int scrollBarY = subcatBarY + subcatBarH - 2;
            int scrollBarH = 2;
            int visibleWidth = refW - 16;
            int handleWidth = Math.max(20, visibleWidth * visibleWidth / totalWidth);
            int handleX = 8 + (subcatScrollOffset * (visibleWidth - handleWidth) / subcatMaxScroll);

            guiGraphics.fill(8, scrollBarY, 8 + visibleWidth, scrollBarY + scrollBarH, 0x40AAAAAA);
            guiGraphics.fill(handleX, scrollBarY, handleX + handleWidth, scrollBarY + scrollBarH, 0xAAFFFFFF);
        }
    }

    // ======================== 配置项列表 ========================

    private void renderEntries(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 性能优化：仅在分类/搜索变更时重新计算条目列表
        boolean needRecalc = cachedEntries == null
                || cachedModTab != selectedModTab
                || cachedCat != selectedCat
                || cachedSubcat != selectedSubcat
                || !java.util.Objects.equals(cachedSearchText, searchText)
                || !java.util.Objects.equals(cachedGlobalSearchText, globalSearchText);

        List<IConfigProvider.ConfigEntry> entries;
        if (needRecalc) {
            entries = getCurrentEntries();
            // 全局搜索过滤（搜索所有分类的条目）
            if (!globalSearchText.isEmpty()) {
                String lower = globalSearchText.toLowerCase();
                IConfigProvider provider = getCurrentProvider();
                entries = getAllModEntries(provider).stream()
                        .filter(e -> matchesSearch(e, lower))
                        .toList();
            }
            // 本地搜索过滤
            if (!searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                entries = entries.stream()
                        .filter(e -> matchesSearch(e, lower))
                        .toList();
            }
            // 层级过滤：未搜索时，隐藏父级关闭的子项
            if (searchText.isEmpty() && globalSearchText.isEmpty()) {
                entries = filterByParentVisibility(entries);
            }
            // 权限过滤：无编辑权限的配置项直接不显示，避免用户尝试修改后保存失败
            entries = filterByPermission(entries);
            cachedEntries = entries;
            cachedModTab = selectedModTab;
            cachedCat = selectedCat;
            cachedSubcat = selectedSubcat;
            cachedSearchText = searchText;
            cachedGlobalSearchText = globalSearchText;
        } else {
            entries = cachedEntries;
        }

        int searchBoxH = 18;
        int searchBoxW = 160;
        int searchBoxX = 14;

        // 全局搜索框（搜索整个模组的全部配置项）
        int globalSearchY = contentTop + 2;
        guiGraphics.fill(searchBoxX, globalSearchY, searchBoxX + searchBoxW, globalSearchY + searchBoxH, INPUT_BG);
        int gBorderColor = globalSearchFocused ? 0xFFFFFFFF : INPUT_BORDER;
        guiGraphics.renderOutline(searchBoxX, globalSearchY, searchBoxW, searchBoxH, gBorderColor);

        Component globalLabel = globalSearchText.isEmpty()
                ? Component.translatable("screen.deltaforce.settings.search_global")
                : Component.literal(globalSearchText);
        int gLabelColor = globalSearchText.isEmpty() ? 0x60FFFFFF : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, globalLabel, searchBoxX + 4, globalSearchY + 5, gLabelColor, false);

        if (globalSearchFocused) {
            int textW = this.font.width(Component.literal(globalSearchText));
            long t = System.currentTimeMillis() / 500 % 2;
            if (t == 0) {
                guiGraphics.fill(searchBoxX + 4 + textW, globalSearchY + 3, searchBoxX + 5 + textW, globalSearchY + searchBoxH - 3, 0xFFFFFFFF);
            }
        }

        // 本地搜索框（搜索当前分类/子分类）- 与全局搜索同一行
        int localSearchY = globalSearchY;
        int localSearchX = searchBoxX + searchBoxW + 6;
        int localSearchW = searchBoxW;

        guiGraphics.fill(localSearchX, localSearchY, localSearchX + localSearchW, localSearchY + searchBoxH, INPUT_BG);
        int lBorderColor = searchFocused ? 0xFFFFFFFF : INPUT_BORDER;
        guiGraphics.renderOutline(localSearchX, localSearchY, localSearchW, searchBoxH, lBorderColor);

        Component localLabel = searchText.isEmpty()
                ? Component.translatable("screen.deltaforce.settings.search_local")
                : Component.literal(searchText);
        int lLabelColor = searchText.isEmpty() ? 0x60FFFFFF : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, localLabel, localSearchX + 4, localSearchY + 5, lLabelColor, false);

        if (searchFocused) {
            int textW = this.font.width(Component.literal(searchText));
            long t = System.currentTimeMillis() / 500 % 2;
            if (t == 0) {
                guiGraphics.fill(localSearchX + 4 + textW, localSearchY + 3, localSearchX + 5 + textW, localSearchY + searchBoxH - 3, 0xFFFFFFFF);
            }
        }

        int listTop = localSearchY + searchBoxH + 4;
        int totalRows = entries.size();
        int contentHeight = contentBottom - listTop;
        maxScroll = Math.max(0, totalRows * entryRowHeight - contentHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        guiGraphics.enableScissor(
                offsetX, offsetY + (int)(listTop * uiScale),
                offsetX + (int)(refW * uiScale), offsetY + (int)(contentBottom * uiScale));

        String currentHovered = null;
        IConfigProvider.ConfigEntry currentHoveredEntry = null;
        for (int i = 0; i < totalRows; i++) {
            int rowY = listTop + i * entryRowHeight - scrollOffset;
            if (rowY + entryRowHeight < listTop || rowY > contentBottom) continue;

            IConfigProvider.ConfigEntry entry = entries.get(i);
            boolean hovered = mouseX >= 10 && mouseX < refW - SCROLL_BAR_WIDTH - 10
                    && mouseY >= rowY && mouseY < rowY + entryRowHeight;

            int rowBg = hovered ? ENTRY_BG_HOVER : (i % 2 == 0 ? ENTRY_BG : 0);
            if (rowBg != 0) {
                guiGraphics.fill(10, rowY, refW - SCROLL_BAR_WIDTH - 10, rowY + entryRowHeight, rowBg);
            }

            renderEntryRow(guiGraphics, mouseX, mouseY, entry, rowY, hovered);
            if (hovered) {
                currentHovered = entry.path();
                currentHoveredEntry = entry;
            }
        }

        guiGraphics.disableScissor();

        // 悬停延迟跟踪
        if (currentHovered != null) {
            if (!currentHovered.equals(hoveredEntryPath)) {
                hoveredEntryPath = currentHovered;
                hoveredEntry = currentHoveredEntry;
                hoverStartTime = System.currentTimeMillis();
            }
        } else {
            hoveredEntryPath = null;
            hoveredEntry = null;
            hoverStartTime = 0;
        }

        // 悬停延迟提示（在scissor外渲染，避免被裁剪）
        if (hoveredEntry != null && hoveredEntryPath != null
                && System.currentTimeMillis() - hoverStartTime > HOVER_DELAY_MS) {
            String desc = hoveredEntry.description();
            if (getCurrentProvider() instanceof DfsConfigProvider dfs && dfs.entry(hoveredEntry.path()) != null) {
                desc = ConfigLabelHelper.entryTooltip(dfs.entry(hoveredEntry.path())).getString();
            }
            if (desc == null || desc.isBlank()) {
                desc = buildAutoDescription(hoveredEntry);
            }
            // 构建tooltip行列表
            List<Component> tooltipLines = new ArrayList<>();
            if (desc != null && !desc.isBlank()) {
                String[] lines = desc.split("\n");
                for (String line : lines) {
                    tooltipLines.add(Component.literal(line));
                }
            }
            guiGraphics.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
        }

        // 滚动条
        if (totalRows > 0 && maxScroll > 0) {
            int sbX = refW - SCROLL_BAR_WIDTH - 2;
            guiGraphics.fill(sbX, listTop, sbX + SCROLL_BAR_WIDTH, contentBottom, SCROLL_BAR_COLOR);
            int handleH = Math.max(16, (int) ((float) contentHeight / (totalRows * entryRowHeight) * contentHeight));
            int handleY = listTop + (int) ((float) scrollOffset / maxScroll * (contentHeight - handleH));
            guiGraphics.fill(sbX, handleY, sbX + SCROLL_BAR_WIDTH, handleY + handleH, SCROLL_BAR_HANDLE_COLOR);
        }
    }

    /** 获取当前模组的所有配置项（跨所有分类） */
    private List<IConfigProvider.ConfigEntry> getAllModEntries(IConfigProvider provider) {
        if (provider == null) return List.of();
        List<IConfigProvider.ConfigEntry> all = new ArrayList<>();
        for (String cat : provider.getCategories()) {
            for (String sub : provider.getSubCategories(cat)) {
                all.addAll(provider.getEntries(cat, sub));
            }
        }
        return all;
    }

    /** 根据父级可见性过滤配置项：递归检查所有祖先，任一祖先关闭则隐藏 */
    private List<IConfigProvider.ConfigEntry> filterByParentVisibility(List<IConfigProvider.ConfigEntry> entries) {
        // 先收集所有条目的布尔值与父子关系
        Map<String, Boolean> boolValues = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        IConfigProvider provider = getCurrentProvider();
        for (IConfigProvider.ConfigEntry e : entries) {
            if (e.isBoolean()) {
                String val = dirtyValues.getOrDefault(e.path(),
                        provider != null ? provider.getValue(e.path()) : e.currentValue());
                boolValues.put(e.path(), "true".equalsIgnoreCase(val));
            }
            if (e.parentPath() != null) {
                parentMap.put(e.path(), e.parentPath());
            }
        }
        // 过滤：递归检查所有祖先，任一祖先关闭则隐藏
        return entries.stream()
                .filter(e -> isAncestorChainEnabled(e.path(), boolValues, parentMap))
                .toList();
    }

    /** 递归检查所有祖先是否均开启（每条链独立的visited防止循环引用） */
    private boolean isAncestorChainEnabled(String path, Map<String, Boolean> boolValues,
                                           Map<String, String> parentMap) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        return isAncestorChainEnabled(path, boolValues, parentMap, visited);
    }

    /** 递归检查（内部方法，带visited追踪） */
    private boolean isAncestorChainEnabled(String path, Map<String, Boolean> boolValues,
                                           Map<String, String> parentMap, java.util.Set<String> visited) {
        String parent = parentMap.get(path);
        if (parent == null) return true;
        if (!visited.add(parent)) return true; // 防止循环引用
        if (!boolValues.getOrDefault(parent, false)) return false;
        return isAncestorChainEnabled(parent, boolValues, parentMap, visited);
    }

    /** 权限过滤：移除当前用户无权编辑的配置项（无权限时不显示，避免修改失败） */
    private List<IConfigProvider.ConfigEntry> filterByPermission(List<IConfigProvider.ConfigEntry> entries) {
        IConfigProvider provider = getCurrentProvider();
        if (provider == null) return entries;
        return entries.stream()
                .filter(e -> provider.canEdit(e.path()))
                .toList();
    }

    /** 当配置项没有注释时，自动生成完整描述信息（移植自DFS ConfigLabelHelper） */
    private String buildAutoDescription(IConfigProvider.ConfigEntry entry) {
        String modId = getCurrentModId();
        List<String> lines = new ArrayList<>();
        
        // 第一行：本地化名称
        lines.add(localizeLeaf(modId, entry.path()).getString());
        
        // 第二行：参数作用
        lines.add(buildPurpose(modId, entry));
        
        // 第三行：文件
        lines.add("文件: " + modId + ".toml");
        
        // 第四行：键路径
        lines.add("键路径: " + entry.path());
        
        // 第五行：类型和当前值
        String currentValue = dirtyValues.getOrDefault(entry.path(), entry.currentValue());
        lines.add("类型: " + kindChinese(entry.kind().name()) + "  当前: " + currentValue);
        
        // 第六行：单位提示
        String unit = unitHint(entry.path());
        if (!unit.isEmpty()) {
            lines.add(unit);
        }
        
        // 如果有原始注释且不是"Controls "开头，也加上
        if (entry.description() != null && !entry.description().isBlank()
                && !entry.description().startsWith("Controls ")) {
            lines.add(entry.description());
        }
        
        return String.join("\n", lines);
    }

    /** 构建参数作用描述 */
    private String buildPurpose(String modId, IConfigProvider.ConfigEntry entry) {
        String leaf = getLeaf(entry.path());
        String[] parts = entry.path().split("\\.");
        String who = "";
        
        if (parts.length >= 2) {
            String first = parts[0];
            String second = parts[1];
            who = "「" + localizeSegment(modId, first) + " / " + localizeSegment(modId, second) + "」";
        } else if (parts.length == 1) {
            who = "「" + localizeSegment(modId, parts[0]) + "」";
        }
        
        String purpose = purposeForLeaf(leaf);
        StringBuilder sb = new StringBuilder();
        sb.append("参数作用: ");
        if (!who.isEmpty()) {
            sb.append("调整").append(who).append("的");
        } else {
            sb.append("调整");
        }
        sb.append(purpose).append("。");
        sb.append(" 生效方式: 保存后立即应用；也可用 /dealtreload 从磁盘重读。");
        return sb.toString();
    }

    /** 获取路径的最后一段 */
    private static String getLeaf(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }

    /** 类型中文名称 */
    private static String kindChinese(String kind) {
        return switch (kind) {
            case "BOOLEAN" -> "开关(true/false)";
            case "INTEGER" -> "整数";
            case "LONG" -> "长整数";
            case "FLOAT" -> "小数(float)";
            case "DOUBLE" -> "小数(double)";
            case "STRING" -> "文本";
            default -> kind;
        };
    }

    /** 单位提示 */
    private static String unitHint(String path) {
        String leaf = getLeaf(path).toLowerCase();
        if (leaf.endsWith("_ticks")) return "单位: tick（20 tick = 1 秒）";
        if (leaf.endsWith("_seconds")) return "单位: 秒";
        if (leaf.endsWith("_fraction")) return "单位: 比例（1.0 = 100%）";
        if (leaf.endsWith("_percent")) return "单位: 百分比（100 = 100%）";
        if (leaf.endsWith("_permille")) return "单位: 千分比（1000 = 100%）";
        if (leaf.endsWith("_multiplier")) return "单位: 倍率（1.0 = 不变）";
        if (leaf.endsWith("_radius") || leaf.endsWith("_range") || leaf.endsWith("_distance")) {
            return "单位: 方块";
        }
        if (leaf.endsWith("_damage") || leaf.equals("damage") || leaf.contains("heal")) {
            return "单位: 生命值（2点=1心）";
        }
        if (leaf.endsWith("_amplifier")) return "单位: 效果等级（0=I级）";
        if (leaf.equals("x_offset") || leaf.equals("y_offset")) return "单位: 像素";
        if (leaf.equals("button_scale")) return "单位: 倍率（0.25~3.0）";
        return "";
    }

    /** 根据叶子节点推断参数作用 */
    private static String purposeForLeaf(String leaf) {
        String lower = leaf.toLowerCase();
        if (lower.endsWith("_cooldown_ticks") || lower.equals("cooldown_ticks")) {
            return "冷却时间";
        }
        if (lower.endsWith("_recharge_ticks") || lower.equals("recharge_ticks")) {
            return "充能恢复时间";
        }
        if (lower.endsWith("_duration_ticks") || lower.equals("duration_ticks")) {
            return "持续时间";
        }
        if (lower.endsWith("_interval_ticks") || lower.equals("interval_ticks")) {
            return "触发/刷新间隔";
        }
        if (lower.endsWith("_delay_ticks") || lower.equals("delay_ticks")) {
            return "延迟时间";
        }
        if (lower.endsWith("_max_charges") || lower.equals("max_charges")) {
            return "最大充能次数";
        }
        if (lower.endsWith("_damage") || lower.equals("damage")) {
            return "造成的伤害数值";
        }
        if (lower.endsWith("_radius") || lower.equals("radius")) {
            return "作用半径（方块）";
        }
        if (lower.endsWith("_range") || lower.equals("range")) {
            return "作用/索敌范围（方块）";
        }
        if (lower.endsWith("_distance") || lower.equals("distance")) {
            return "距离相关参数（方块）";
        }
        if (lower.endsWith("_speed") || lower.equals("speed")) {
            return "速度相关参数";
        }
        if (lower.endsWith("_amplifier") || lower.equals("amplifier")) {
            return "药水/状态效果等级（0=I）";
        }
        if (lower.endsWith("_fraction") || lower.endsWith("_percent") || lower.endsWith("_permille")) {
            return "比例/百分比相关参数";
        }
        if (lower.endsWith("_multiplier")) {
            return "倍率（1.0=不变）";
        }
        if (lower.endsWith("_cost") || lower.contains("energy_cost") || lower.contains("health_cost")) {
            return "释放/维持消耗";
        }
        if (lower.equals("enabled")) {
            return "功能开关";
        }
        if (lower.equals("simple_mode")) {
            return "简易模式";
        }
        if (lower.equals("hotkey_detailed") || lower.contains("hotkey")) {
            return "快捷键显示详细信息";
        }
        if (lower.equals("x_offset") || lower.equals("y_offset")) {
            return "界面位置偏移（像素）";
        }
        if (lower.equals("button_scale")) {
            return "技能按钮缩放倍率";
        }
        return "相关参数";
    }

    private void renderEntryRow(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                IConfigProvider.ConfigEntry entry, int rowY, boolean hovered) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        boolean isModified = !currentValue.equals(entry.defaultValue());

        // 左侧：主名 + 小字路径上下文描述（参照DFS ConfigLabelHelper）
        Component keyText = localizeLeaf(getCurrentModId(), path);
        String context = localizeContext(getCurrentModId(), path);
        int keyColor = isModified ? 0xFFFFAA00 : 0xFFCCCCCC;
        int lineH = this.font.lineHeight;
        int mainY = rowY + (entryRowHeight - lineH * 2) / 2;
        // 根据嵌套层级缩进
        int indent = 14 + entry.nestLevel() * 16;
        guiGraphics.drawString(this.font, keyText, indent, mainY, keyColor, false);
        if (!context.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal(context), indent, mainY + lineH, 0x60AAAAAA, false);
        }

        // 客户端/服务端标记：绿色C=客户端本地配置，红色S=服务端同步配置
        // WIP条目追加黄色A标记（仅开发者可见），普通条目不显示
        // 无权限时完全不显示标记，避免误触导致保存失败
        IConfigProvider provider = getCurrentProvider();
        boolean isClient = true;
        boolean canEdit = true;
        if (provider != null) {
            isClient = provider.isClientLocal(path);
            canEdit = provider.canEdit(path);
            // 服务端配置且无权限 → 不显示 S 标记，避免用户误触后保存失败
            // 开发者始终显示标记（包括WIP黄A），以便查看和调试
            if (!isClient && !canEdit) {
                // 不绘制标记
            } else {
                String tag = isClient ? "C" : "S";
                int tagColor = isClient ? 0xFF55FF55 : 0xFFFF5555;
                int tagX = indent + this.font.width(keyText) + 4;
                guiGraphics.drawString(this.font, Component.literal(tag), tagX, mainY, tagColor, false);
            }
        }

        // 不可编辑的服务端条目不显示标记，避免用户困惑
        // 右侧控件区域 - 固定居中位置，仅与文本冲突时后移
        int widgetW = 120;
        int maxRight = refW - SCROLL_BAR_WIDTH - 14;
        int fixedCenter = (14 + maxRight) / 2;
        int widgetLeft = fixedCenter - widgetW / 2;
        int widgetRight = widgetLeft + widgetW;

        // 如果控件和名称重叠，将控件右移（取主名和上下文较宽者）
        int nameWidth = this.font.width(keyText);
        int contextWidth = context.isEmpty() ? 0 : this.font.width(Component.literal(context));
        int nameEnd = indent + Math.max(nameWidth, contextWidth) + 14;
        if (widgetLeft < nameEnd + 10) {
            widgetLeft = nameEnd + 10;
            widgetRight = widgetLeft + widgetW;
        }

        // 快捷键独立按键设置：特殊渲染
        if ("customdrop.hud.hotkey".equals(path)) {
            renderKeyBindingWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight);
            return;
        }

        switch (entry.kind()) {
            case BOOLEAN -> renderBooleanWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight);
            case INTEGER, LONG -> renderIntegerWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight, false);
            case FLOAT, DOUBLE -> renderIntegerWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight, true);
            case STRING -> renderStringWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight);
        }

        // 编辑光标（仅文本输入模式显示，滑块模式不显示）
        if (focusedEditKey != null && focusedEditKey.equals(path)) {
            boolean useSlider = sliderMode.getOrDefault(path, false);
            if (!useSlider) {
                // 数值型条目文本输入框右侧边界与widgetRight不同（需减去toggle按钮宽度22px），
                // 字符串型则相同，统一传入正确的右边界确保光标与文本对齐
                int cursorRight = entry.isNumeric() ? widgetRight - 18 - 4 : widgetRight;
                renderCursor(guiGraphics, entry, rowY, widgetLeft, cursorRight);
            }
        }

    }

    // ======================== 布尔开关控件 ========================

    private void renderBooleanWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                     IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        boolean isOn = "true".equalsIgnoreCase(currentValue);

        int btnW = 36;
        int btnH = 16;
        int btnX = widgetRight - btnW;
        int btnY = rowY + (entryRowHeight - btnH) / 2;

        boolean hoverBtn = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int bg = isOn ? TOGGLE_ON_COLOR : TOGGLE_OFF_COLOR;
        if (hoverBtn) bg = (bg & 0x00FFFFFF) | 0xCC000000;
        guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);

        Component label = isOn
                ? Component.translatable("screen.deltaforce.settings.on")
                : Component.translatable("screen.deltaforce.settings.off");
        int textW = this.font.width(label);
        guiGraphics.drawString(this.font, label, btnX + (btnW - textW) / 2,
                btnY + (btnH - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    // ======================== 按键绑定控件 ========================

    /** GLFW键码转可读名称 */
    private static String glfwKeyName(int keyCode) {
        if (keyCode == -1) return "无";
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null && !name.isEmpty()) return name.toUpperCase();
        return switch (keyCode) {
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE -> "Space";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER -> "Enter";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB -> "Tab";
            case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            default -> "Key" + keyCode;
        };
    }

    /** 解析组合键字符串（如 "73+341"）为键码列表 */
    private static List<Integer> parseComboKeys(String value) {
        List<Integer> keys = new ArrayList<>();
        if (value == null || value.isEmpty()) return keys;
        try {
            for (String part : value.split("\\+")) {
                keys.add(Integer.parseInt(part.trim()));
            }
        } catch (NumberFormatException ignored) {
            // 兼容旧格式：单个整数
            try {
                keys.add(Integer.parseInt(value));
            } catch (NumberFormatException ignored2) {}
        }
        return keys;
    }

    /** 组合键码列表转可读名称（如 "I+LCtrl"） */
    private static String glfwComboKeyName(List<Integer> keys) {
        if (keys == null || keys.isEmpty()) return "无";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append("+");
            int kc = keys.get(i);
            if (kc == -1) {
                sb.append("无");
            } else {
                sb.append(glfwKeyName(kc));
            }
        }
        return sb.toString();
    }

    private void renderKeyBindingWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                         IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        boolean isCapturing = path.equals(capturingKeyPath);

        int btnW = widgetRight - widgetLeft;
        int btnH = 16;
        int btnX = widgetLeft;
        int btnY = rowY + (entryRowHeight - btnH) / 2;

        boolean hoverBtn = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        int bg = isCapturing ? 0xFFFF8844 : (hoverBtn ? 0xFF4488FF : 0xFF3366CC);
        guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, bg);

        Component label;
        if (isCapturing) {
            if (capturedKeys.isEmpty()) {
                label = Component.translatable("screen.deltaforce.settings.pressing_key");
            } else {
                label = Component.literal("> " + glfwComboKeyName(capturedKeys) + " <");
            }
        } else {
            label = Component.literal(glfwComboKeyName(parseComboKeys(currentValue)));
        }
        int textW = this.font.width(label);
        guiGraphics.drawString(this.font, label, btnX + (btnW - textW) / 2,
                btnY + (btnH - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    // ======================== 数值控件（滑块+手动输入） ========================

    private void renderIntegerWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                     IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight,
                                     boolean isFloat) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        boolean useSlider = sliderMode.getOrDefault(path, false);

        int widgetW = widgetRight - widgetLeft;

        // 切换按钮（小按钮，在控件右侧）
        int toggleW = 18;
        int toggleH = 14;
        int toggleX = widgetRight - toggleW;
        int toggleY = rowY + (entryRowHeight - toggleH) / 2;

        boolean hoverToggle = mouseX >= toggleX && mouseX < toggleX + toggleW && mouseY >= toggleY && mouseY < toggleY + toggleH;
        int toggleBg = hoverToggle ? 0x604488FF : 0x403366CC;
        guiGraphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, toggleBg);
        guiGraphics.renderOutline(toggleX, toggleY, toggleW, toggleH, 0x80FFFFFF);

        Component toggleLabel = useSlider ? Component.literal("S") : Component.literal("N");
        int tw = this.font.width(toggleLabel);
        guiGraphics.drawString(this.font, toggleLabel, toggleX + (toggleW - tw) / 2, toggleY + 1, 0xFFFFFFFF, false);

        int contentRight = toggleX - 4;
        int contentLeft = widgetLeft;

        if (useSlider) {
            renderSliderWidget(guiGraphics, mouseX, mouseY, entry, rowY, contentLeft, contentRight, isFloat);
        } else {
            renderTextInputWidget(guiGraphics, mouseX, mouseY, entry, rowY, contentLeft, contentRight);
        }
    }

    private void renderSliderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                    IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight,
                                    boolean isFloat) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());

        int sliderW = 80;
        int sliderH = 10;
        int sliderX = widgetRight - sliderW - 4;
        int sliderY = rowY + (entryRowHeight - sliderH) / 2;

        double val;
        try { val = Double.parseDouble(currentValue); } catch (NumberFormatException e) { val = 0; }
        // 拖拽过程中使用起始值计算范围，与handleSliderDrag保持范围一致，避免滑块视觉位置偏差
        double rangeCalcValue = (hasSliderDragStart && path.equals(draggingSliderPath))
            ? sliderDragStartValue : val;
        double sliderMin = getSliderMin(entry, rangeCalcValue);
        double sliderMax = getSliderMax(entry, rangeCalcValue);
        double sliderRange = sliderMax - sliderMin;

        // 滑块背景
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, SLIDER_BG);

        if (sliderRange > 0) {
            double ratio = Math.max(0, Math.min(1, (val - sliderMin) / sliderRange));
            int handleX = sliderX + (int) (ratio * sliderW) - 3;

            // 如果最小值是负值，绘制中心线标记
            if (sliderMin < 0 && sliderMax > 0) {
                int centerX = sliderX + (int) ((0 - sliderMin) / sliderRange * sliderW);
                guiGraphics.fill(centerX, sliderY - 1, centerX + 1, sliderY + sliderH + 1, 0x80FFFFFF);
            }

            // 填充部分：对称滑块从中心填充到当前值，普通滑块从左侧填充到当前值
            int fillStart, fillEnd;
            if (sliderMin < 0 && sliderMax > 0) {
                // 对称滑块：中心线位置
                int centerX = sliderX + (int) ((0 - sliderMin) / sliderRange * sliderW);
                if (val >= 0) {
                    fillStart = centerX;
                    fillEnd = handleX + 3;
                } else {
                    fillStart = handleX + 3;
                    fillEnd = centerX;
                }
            } else {
                // 普通滑块：从左侧填充
                fillStart = sliderX;
                fillEnd = handleX + 3;
            }
            if (fillEnd > fillStart) {
                guiGraphics.fill(fillStart, sliderY, fillEnd, sliderY + sliderH, 0x6044AA44);
            } else if (fillStart > fillEnd) {
                guiGraphics.fill(fillEnd, sliderY, fillStart, sliderY + sliderH, 0x6044AA44);
            }

            // 滑块手柄
            guiGraphics.fill(handleX, sliderY - 2, handleX + 6, sliderY + sliderH + 2, SLIDER_HANDLE);
        }

        Component valText = Component.literal(formatNumericValue(val, isFloat));
        int textW = this.font.width(valText);
        guiGraphics.drawString(this.font, valText, sliderX - textW - 6,
                rowY + (entryRowHeight - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    /** 获取滑块最小值（支持负值范围，如 -1.0 ~ 1.0） */
    private double getSliderMin(IConfigProvider.ConfigEntry entry, double currentValue) {
        double defaultValue;
        try { defaultValue = Double.parseDouble(entry.defaultValue()); } catch (NumberFormatException e) { defaultValue = 0; }
        // 对于 scale 类型的配置项，范围固定为 -1.0 ~ 1.0
        String leaf = getLeaf(entry.path()).toLowerCase();
        if (leaf.equals("scale") || leaf.contains("scale")) {
            return -1.0;
        }
        // 偏移量配置项，固定范围 ±240
        if (leaf.contains("offset") || leaf.contains("_offset")) {
            return -240.0;
        }
        // 如果默认值或当前值为负，也允许负值范围
        if (defaultValue < 0 || currentValue < 0) {
            double absMax = Math.max(Math.abs(defaultValue), Math.abs(currentValue));
            if (absMax <= 0) absMax = 1.0;
            return -absMax;
        }
        return 0.0;
    }

    /** 获取滑块最大值（范围宽度 = max - min） */
    private double getSliderMax(IConfigProvider.ConfigEntry entry, double currentValue) {
        double defaultValue;
        try { defaultValue = Double.parseDouble(entry.defaultValue()); } catch (NumberFormatException e) { defaultValue = 0; }
        boolean isFloat = entry.kind() == IConfigProvider.ConfigValueKind.FLOAT
                || entry.kind() == IConfigProvider.ConfigValueKind.DOUBLE;
        // 对于 scale 类型的配置项，范围固定为 -1.0 ~ 1.0
        String leaf = getLeaf(entry.path()).toLowerCase();
        if (leaf.equals("scale") || leaf.contains("scale")) {
            return 1.0;
        }
        // 偏移量配置项，固定范围 ±240
        if (leaf.contains("offset") || leaf.contains("_offset")) {
            return 240.0;
        }
        double base = Math.max(Math.abs(defaultValue), Math.abs(currentValue));
        if (base <= 0) base = isFloat ? 1.0 : 100.0;
        double range = base * 2;
        if (isFloat) {
            range = Math.ceil(range * 10) / 10;
        } else {
            range = Math.ceil(range / 10) * 10;
        }
        return Math.max(range, isFloat ? 1.0 : 100.0);
    }

    /** 获取滑块范围宽度（max - min），用于兼容旧代码 */
    private double getSliderRange(IConfigProvider.ConfigEntry entry, double currentValue) {
        return getSliderMax(entry, currentValue) - getSliderMin(entry, currentValue);
    }

    /** 格式化数值显示，避免科学计数法(e+n)和int溢出 */
    private static String formatNumericValue(double val, boolean isFloat) {
        double abs = Math.abs(val);
        if (isFloat) {
            if (abs == 0) return "0.00";
            if (abs >= 1e12) return String.format("%.0f", val);
            if (abs >= 1e5) return String.format("%.1f", val);
            if (abs < 0.001) return String.format("%.6f", val);
            return String.format("%.2f", val);
        } else {
            // 整数：使用 long 避免溢出，(|int)| 约 2.1e9)
            if (abs > 9e18) return String.format("%.0f", val);
            long longVal = (long) val;
            if (longVal == val) return String.valueOf(longVal);
            return String.format("%.0f", val);
        }
    }

    private void renderTextInputWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                       IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        boolean isFocused = path.equals(focusedEditKey);

        int inputW = 80;
        int inputH = 16;
        int inputX = widgetRight - inputW;
        int inputY = rowY + (entryRowHeight - inputH) / 2;

        int borderColor = isFocused ? 0xFFFFFFFF : INPUT_BORDER;
        guiGraphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, INPUT_BG);
        guiGraphics.renderOutline(inputX, inputY, inputW, inputH, borderColor);

        Component valText = Component.literal(currentValue);
        guiGraphics.drawString(this.font, valText, inputX + 3, inputY + (inputH - this.font.lineHeight) / 2, 0xFFFFFFFF, false);
    }

    private void renderStringWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                    IConfigProvider.ConfigEntry entry, int rowY, int widgetLeft, int widgetRight) {
        renderTextInputWidget(guiGraphics, mouseX, mouseY, entry, rowY, widgetLeft, widgetRight);
    }

    // ======================== 编辑光标 ========================

    private void renderCursor(GuiGraphics guiGraphics, IConfigProvider.ConfigEntry entry, int rowY,
                              int widgetLeft, int widgetRight) {
        String path = entry.path();
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        int inputW = 80;
        int inputH = 16;
        int inputX = widgetRight - inputW;
        int inputY = rowY + (entryRowHeight - inputH) / 2;

        int textW = this.font.width(Component.literal(currentValue));
        int cursorX = inputX + 3 + textW;
        // 限制光标在输入框范围内
        int maxCursorX = inputX + inputW - 2;
        if (cursorX > maxCursorX) cursorX = maxCursorX;
        if (cursorX < inputX + 2) cursorX = inputX + 2;
        guiGraphics.fill(cursorX, inputY + 2, cursorX + 1, inputY + inputH - 2, 0xFFFFFFFF);
    }

    // ======================== 底栏 ========================

    private void renderBottomBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int barY = refH - BOTTOM_BAR_HEIGHT;
        guiGraphics.fill(0, barY, refW, refH, BOTTOM_BAR_COLOR);

        boolean hasChanges = hasUnsavedChanges();

        // 预览按钮（自定义掉落物HUD / HUD布局 标签页显示）
        if ("deltaforce".equals(getCurrentModId()) || "dealt_force_skills".equals(getCurrentModId())) {
            boolean hoverPreview = mouseX >= previewBtnX && mouseX < previewBtnX + previewBtnW
                    && mouseY >= previewBtnY && mouseY < previewBtnY + previewBtnH;
            int previewBg = isPreviewing ? 0xCC2288FF : hoverPreview ? 0xFF555555 : 0xFF444444;
            guiGraphics.fill(previewBtnX, previewBtnY, previewBtnX + previewBtnW, previewBtnY + previewBtnH, previewBg);
            guiGraphics.renderOutline(previewBtnX, previewBtnY, previewBtnW, previewBtnH, isPreviewing ? 0xFFFFFFFF : 0xFF888888);
            Component previewLabel = Component.translatable("config.deltaforce.button.preview");
            int previewLabelW = this.font.width(previewLabel);
            guiGraphics.drawString(this.font, previewLabel, previewBtnX + (previewBtnW - previewLabelW) / 2,
                    previewBtnY + (previewBtnH - 8) / 2, isPreviewing ? 0xFFFFFFFF : 0xFFCCCCCC);
        }

        drawBottomButton(guiGraphics, mouseX, mouseY, saveBtnX, saveBtnY, saveBtnW, saveBtnH,
                Component.translatable("screen.deltaforce.settings.save"), hasChanges);
        drawBottomButton(guiGraphics, mouseX, mouseY, cancelBtnX, cancelBtnY, cancelBtnW, cancelBtnH,
                Component.translatable("screen.deltaforce.settings.cancel"), hasChanges);
        drawBottomButton(guiGraphics, mouseX, mouseY, exitBtnX, exitBtnY, exitBtnW, exitBtnH,
                Component.translatable("screen.deltaforce.settings.exit"), true);

        // 撤销/重做按钮（带工具提示）
        boolean canUndo = !undoStack.isEmpty();
        boolean canRedo = !redoStack.isEmpty();
        drawBottomButton(guiGraphics, mouseX, mouseY, undoBtnX, undoBtnY, undoBtnW, undoBtnH,
                Component.literal("↩"), canUndo);
        drawBottomButton(guiGraphics, mouseX, mouseY, redoBtnX, redoBtnY, redoBtnW, redoBtnH,
                Component.literal("↪"), canRedo);
    }

    private void drawBottomButton(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  int x, int y, int w, int h, Component text, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int bgColor = enabled ? (hovered ? 0x60FFFFFF : 0x30FFFFFF) : 0x15FFFFFF;
        guiGraphics.fill(x, y, x + w, y + h, bgColor);

        int textWidth = this.font.width(text);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (h - this.font.lineHeight) / 2;
        int textColor = enabled ? 0xFFFFFFFF : 0x66FFFFFF;
        guiGraphics.drawString(this.font, text, textX, textY, textColor, false);
    }

    // ======================== 预览HUD ========================

    /** 获取当前配置值（优先使用脏值，否则使用已保存值） */
    private String getEffectiveValue(String path) {
        if (dirtyValues.containsKey(path)) {
            return dirtyValues.get(path);
        }
        IConfigProvider provider = getCurrentProvider();
        if (provider != null) {
            return provider.getValue(path);
        }
        return switch (path) {
            case "customdrop.hud.enabled" -> "true";
            case "customdrop.hud.scale" -> "0.0";
            case "customdrop.hud.x_offset" -> "0";
            case "customdrop.hud.y_offset" -> "0";
            case "customdrop.hud.simple_mode" -> "false";
            case "customdrop.hud.hotkey_detailed" -> "false";
            default -> "";
        };
    }

    /** 开始预览：HUD布局标签页仅切换状态，自定义掉落物标签页构建物品池 */
    private void startPreview() {
        if ("dealt_force_skills".equals(getCurrentModId())) {
            return;
        }
        previewStartTime = System.currentTimeMillis();
        lastModelCycle = -1;
        buildPreviewItemPool();
        pickNextPreviewItem();
    }

    /** 停止预览：清空物品池 */
    private void stopPreview() {
        previewItemPool = null;
        currentPreviewItem = null;
        previewStartTime = 0;
        hudDragTarget = HUD_DRAG_NONE;
    }

    /** 构建预览物品池（所有创意标签页的全部物品） */
    private void buildPreviewItemPool() {
        previewItemPool = new ArrayList<>();
        try {
            for (var tab : CreativeModeTabs.allTabs()) {
                var displayItems = tab.getDisplayItems();
                for (var stack : displayItems) {
                    if (!stack.isEmpty()) {
                        previewItemPool.add(stack.copy());
                    }
                }
            }
        } catch (Exception e) {
            // 回退：从物品注册表随机选取
            for (var item : BuiltInRegistries.ITEM) {
                previewItemPool.add(new ItemStack(item));
            }
        }
        if (previewItemPool.isEmpty()) {
            previewItemPool.add(ItemStack.EMPTY);
        }
    }

    /** 随机选取下一个预览物品 */
    private void pickNextPreviewItem() {
        if (previewItemPool == null || previewItemPool.isEmpty()) {
            currentPreviewItem = ItemStack.EMPTY;
            return;
        }
        int idx = previewRandom.nextInt(previewItemPool.size());
        currentPreviewItem = previewItemPool.get(idx).copy();
    }

    /** 获取当前品质（0-8循环，每秒切换） */
    private DfsItemQuality getPreviewQuality() {
        DfsItemQuality[] qualities = DfsItemQuality.values();
        if (qualities.length == 0) return DfsItemQuality.WHITE;
        long elapsed = System.currentTimeMillis() - previewStartTime;
        int idx = (int) ((elapsed / 1000) % qualities.length);
        return qualities[idx];
    }

    private void renderPreviewHud(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if ("dealt_force_skills".equals(getCurrentModId())) {
            renderHudLayoutPreview(guiGraphics, mouseX, mouseY);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // 读取当前配置值（含未保存的修改，预加载到内存）
        boolean enabled = "true".equalsIgnoreCase(getEffectiveValue("customdrop.hud.enabled"));
        if (!enabled) return; // HUD禁用时不渲染预览

        float scale = 0.0F;
        int xOffset = 0;
        int yOffset = 0;
        boolean simpleMode = "true".equalsIgnoreCase(getEffectiveValue("customdrop.hud.simple_mode"));

        try { scale = Float.parseFloat(getEffectiveValue("customdrop.hud.scale")); } catch (NumberFormatException ignored) {}
        try { xOffset = Integer.parseInt(getEffectiveValue("customdrop.hud.x_offset")); } catch (NumberFormatException ignored) {}
        try { yOffset = Integer.parseInt(getEffectiveValue("customdrop.hud.y_offset")); } catch (NumberFormatException ignored) {}

        // 每秒切换模型
        long modelCycle = (System.currentTimeMillis() - previewStartTime) / 1000;
        if (currentPreviewItem == null || modelCycle != lastModelCycle) {
            lastModelCycle = modelCycle;
            pickNextPreviewItem();
        }

        DfsItemQuality previewQuality = getPreviewQuality();
        Component itemName = Component.translatable("screen.deltaforce.settings.preview_item");
        Component qualityLine = Component.translatable("tooltip.dealt_force_skills.quality",
                Component.translatable("quality.dealt_force_skills." + previewQuality.key()));
        String description = Component.translatable("screen.deltaforce.settings.preview_description").getString();

        ItemStack renderStack = (currentPreviewItem != null && !currentPreviewItem.isEmpty())
                ? currentPreviewItem : ItemStack.EMPTY;

        // 直接调用自定义掉落物屏幕HUD渲染
        GroundItemOverlay.renderItemHud(guiGraphics, mc, renderStack, previewQuality,
                itemName, qualityLine, description, 0.8f, scale, scale, xOffset, yOffset,
                simpleMode, false);
    }

    // ======================== HUD布局预览（体力条 + 技能按钮 + 原版HUD） ========================

    private void renderHudLayoutPreview(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        // 绘制原版HUD模拟（快捷栏、血量、饥饿、护甲）
        renderMockVanillaHud(guiGraphics, mc, screenW, screenH);

        // 绘制体力条预览（含拖拽虚影）
        renderPreviewStamina(guiGraphics, screenW, screenH, mouseX, mouseY);

        // 绘制技能按钮占位（含拖拽虚影）
        renderPreviewSkillButtons(guiGraphics, mouseX, mouseY);
    }

    /** 模拟原版HUD：快捷栏、血量、饥饿、护甲、氧气 */
    private void renderMockVanillaHud(GuiGraphics guiGraphics, Minecraft mc, int screenW, int screenH) {
        int centerX = screenW / 2;
        int barBottom = screenH;

        // 快捷栏（9格，总宽182px，底部对齐）
        int hotbarW = 182;
        int hotbarH = 22;
        int hotbarX = centerX - hotbarW / 2;
        int hotbarY = barBottom - hotbarH;
        guiGraphics.fill(hotbarX, hotbarY, hotbarX + hotbarW, hotbarY + hotbarH, 0xAA101010);
        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + i * 20 + 1;
            int slotY = hotbarY + 3;
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 16, 0x40FFFFFF);
        }
        // 高亮选中格
        int selX = hotbarX + 4 * 20 + 1;
        guiGraphics.renderOutline(selX - 1, hotbarY + 2, 20, 18, 0xFFDDDDDD);

        // 血量条（10颗心，在快捷栏上方左侧）
        int heartY = hotbarY - 12;
        int armorY = heartY - 10;
        int heartsStartX = centerX - 91;
        for (int i = 0; i < 10; i++) {
            int hx = heartsStartX + i * 8;
            guiGraphics.fill(hx, heartY, hx + 7, heartY + 9, 0xFFFF3344); // 红心
        }
        // 护甲（半行，在心上方）
        for (int i = 0; i < 5; i++) {
            int ax = heartsStartX + i * 8;
            guiGraphics.fill(ax, armorY, ax + 7, armorY + 9, 0xFFAAAAAA);
        }

        // 饥饿条（在快捷栏上方右侧）
        int hungerY = hotbarY - 12;
        int hungerStartX = centerX + 91 - 80;
        for (int i = 0; i < 10; i++) {
            int hx = hungerStartX + i * 8;
            guiGraphics.fill(hx, hungerY, hx + 7, hungerY + 9, 0xFFBB7722);
        }

        // 模拟氧气条（水下气泡）
        int airY = hungerY - 10;
        for (int i = 0; i < 10; i++) {
            int ax = hungerStartX + i * 8;
            guiGraphics.fill(ax, airY, ax + 7, airY + 9, 0xFF4488FF);
        }
    }

    /** 体力条预览：模拟满体力状态，使用ClientHudLayout偏移/缩放 */
    private void renderPreviewStamina(GuiGraphics guiGraphics, int screenW, int screenH, int mouseX, int mouseY) {
        try {
            float scale = ClientHudLayout.staminaScale();
            float x = screenW / 2f + 91f - StaminaBarRenderer.WIDTH * scale + ClientHudLayout.staminaXOffset();
            float y = screenH - 48f + ClientHudLayout.staminaYOffset();

            // 记录视觉边界（屏幕坐标，用于命中检测）
            hudStaminaVisX = (int) x;
            hudStaminaVisY = (int) (y + StaminaBarRenderer.HEIGHT * (1.0F - scale));
            hudStaminaVisW = Math.round(StaminaBarRenderer.WIDTH * scale);
            hudStaminaVisH = Math.round(StaminaBarRenderer.HEIGHT * scale);

            // 拖拽时绘制半透明虚影
            if (hudDragTarget == HUD_DRAG_STAMINA) {
                int deltaX = mouseX - (int) hudDragStartMouseX;
                int deltaY = mouseY - (int) hudDragStartMouseY;
                int ghostX = hudDragStartAnchorX + deltaX;
                int ghostY = hudDragStartAnchorY + deltaY;
                guiGraphics.fill(ghostX, ghostY, ghostX + hudStaminaVisW, ghostY + hudStaminaVisH, 0x60FFFF44);
            }

            guiGraphics.pose().pushPose();
            try {
                guiGraphics.pose().translate(x, y + StaminaBarRenderer.HEIGHT * (1.0F - scale), 0.0F);
                guiGraphics.pose().scale(scale, scale, 1.0F);
                StaminaBarRenderer.render(guiGraphics, 80.0, false);
            } finally {
                guiGraphics.pose().popPose();
            }
        } catch (Exception e) {
            LOGGER.warn("预览体力条渲染失败", e);
        }
    }

    /** 技能按钮预览：绘制3个占位框，使用ClientHudLayout坐标 */
    private void renderPreviewSkillButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        try {
            int slot = 46;
            int gap = 4;
            int baseX = 8;
            int baseY = Math.max(8, guiGraphics.guiHeight() - 68);

            int x = ClientHudLayout.x(baseX);
            int y = ClientHudLayout.y(baseY);
            float bScale = ClientHudLayout.buttonScale();

            // 记录视觉边界（屏幕坐标，用于命中检测）
            // scaleButton的锚点变换：visualX = x(8) + (x - x(8)) * scale, visualY = y + 46*(1-scale)
            int anchorX = ClientHudLayout.x(8);
            hudSkillsVisX = Math.round(anchorX + (x - anchorX) * bScale);
            hudSkillsVisY = Math.round(y + 46 * (1.0F - bScale));
            hudSkillsVisW = Math.round((46 + gap) * 3 * bScale);
            hudSkillsVisH = Math.round(46 * bScale);

            // 拖拽时绘制半透明虚影
            if (hudDragTarget == HUD_DRAG_SKILLS) {
                int deltaX = mouseX - (int) hudDragStartMouseX;
                int deltaY = mouseY - (int) hudDragStartMouseY;
                int ghostX = hudDragStartAnchorX + deltaX;
                int ghostY = hudDragStartAnchorY + deltaY;
                int ghostSlotW = Math.round(46 * bScale);
                int ghostSlotH = Math.round(46 * bScale);
                int ghostSpacing = Math.round((46 + gap) * bScale);
                int[] colors = { 0x6067D6FF, 0x60FFC857, 0x608EE36A };
                for (int i = 0; i < 3; i++) {
                    int sx = ghostX + ghostSpacing * i;
                    guiGraphics.fill(sx, ghostY, sx + ghostSlotW, ghostY + ghostSlotH, colors[i]);
                }
            }

            int[] colors = { 0xAA67D6FF, 0xAAFFC857, 0xAA8EE36A };
            Component[] labels = {
                    Component.translatable("screen.deltaforce.settings.skill_core"),
                    Component.translatable("screen.deltaforce.settings.skill_2"),
                    Component.translatable("screen.deltaforce.settings.skill_1")
            };

            // 使用ClientHudLayout.buttonScale()缩放绘制
            ClientHudLayout.ButtonScale scaleObj = ClientHudLayout.scaleButton(guiGraphics, x, y, slot);
            try {
                for (int i = 0; i < 3; i++) {
                    int sx = x + (slot + gap) * i;
                    guiGraphics.fill(sx, y, sx + slot, y + slot, 0xAA101216);
                    guiGraphics.renderOutline(sx, y, slot, slot, colors[i]);
                    // 居中文字
                    Font font = Minecraft.getInstance().font;
                    int tw = font.width(labels[i]);
                    guiGraphics.drawString(font, labels[i],
                            sx + (slot - tw) / 2, y + (slot - 8) / 2, 0xFFCCCCCC, false);
                }
            } finally {
                scaleObj.close();
            }
        } catch (Exception e) {
            LOGGER.warn("预览技能按钮渲染失败", e);
        }
    }

    // ======================== HUD拖拽映射 ========================

    /** 拖拽松手时将屏幕像素偏移直接映射为配置偏移量并写入内存 */
    private void applyHudDrag(double mouseX, double mouseY) {
        if (hudDragTarget == HUD_DRAG_NONE) return;

        // 像素偏移直接=配置偏移量（x/y和stamina均已改用GUI坐标系，与体力条一致）
        int deltaX = (int) (mouseX - hudDragStartMouseX);
        int deltaY = (int) (mouseY - hudDragStartMouseY);
        int offsetDeltaX = deltaX;
        int offsetDeltaY = deltaY;

        int target = hudDragTarget;
        hudDragTarget = HUD_DRAG_NONE;

        if (offsetDeltaX == 0 && offsetDeltaY == 0) return;

        String xPath, yPath;
        int defaultX, defaultY;
        if (target == HUD_DRAG_SKILLS) {
            xPath = "HUD|global.x_offset";
            yPath = "HUD|global.y_offset";
            defaultX = 208;
            defaultY = -10;
        } else {
            xPath = "HUD|global.stamina_x_offset";
            yPath = "HUD|global.stamina_y_offset";
            defaultX = 0;
            defaultY = 0;
        }

        IConfigProvider provider = getCurrentProvider();
        if (provider == null) return;

        // 读取当前有效值（脏值优先，其次取provider实际值）
        String curX = dirtyValues.containsKey(xPath) ? dirtyValues.get(xPath) : provider.getValue(xPath);
        String curY = dirtyValues.containsKey(yPath) ? dirtyValues.get(yPath) : provider.getValue(yPath);
        int curXi = curX.isEmpty() ? defaultX : safeParseInt(curX, defaultX);
        int curYi = curY.isEmpty() ? defaultY : safeParseInt(curY, defaultY);

        String newX = String.valueOf(curXi + offsetDeltaX);
        String newY = String.valueOf(curYi + offsetDeltaY);

        // oldValue兜底用默认值，确保永不为null（否则applyRecord里null走dirty回读路径撤销失效）
        String oldX = curX.isEmpty() ? String.valueOf(defaultX) : curX;
        String oldY = curY.isEmpty() ? String.valueOf(defaultY) : curY;

        boolean changedX = !Objects.equals(oldX, newX);
        boolean changedY = !Objects.equals(oldY, newY);

        if (changedX) {
            undoStack.push(new ChangeRecord(xPath, oldX, newX));
        }
        if (changedY) {
            undoStack.push(new ChangeRecord(yPath, oldY, newY));
        }
        if (changedX || changedY) {
            redoStack.clear();
        }

        dirtyValues.put(xPath, newX);
        dirtyValues.put(yPath, newY);
        provider.setValue(xPath, newX);
        provider.setValue(yPath, newY);
        invalidateEntriesCache();
    }

    /** 将字符串安全解析为int，失败返回默认值 */
    private static int safeParseInt(String s, int defaultVal) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // ======================== 未保存弹窗 ========================

    private void renderUnsavedDialog(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, refW, refH, 0x80000000);

        int dialogW = 200;
        int dialogH = 60;
        int dialogX = (refW - dialogW) / 2;
        int dialogY = (refH - dialogH) / 2;

        guiGraphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xFF222222);
        guiGraphics.renderOutline(dialogX, dialogY, dialogW, dialogH, 0xFFFFFFFF);

        Component msg = Component.translatable("screen.deltaforce.settings.unsaved_warning");
        int msgW = this.font.width(msg);
        guiGraphics.drawString(this.font, msg, dialogX + (dialogW - msgW) / 2, dialogY + 8, 0xFFFF5555, false);

        int btnW = 50;
        int btnH = 14;
        int btnY = dialogY + dialogH - btnH - 6;
        int noBtnX = dialogX + dialogW / 2 - btnW - 4;
        int yesBtnX = dialogX + dialogW / 2 + 4;

        boolean hoverNo = mouseX >= noBtnX && mouseX < noBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        boolean hoverYes = mouseX >= yesBtnX && mouseX < yesBtnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

        guiGraphics.fill(noBtnX, btnY, noBtnX + btnW, btnY + btnH, hoverNo ? 0xFF555555 : 0xFF333333);
        guiGraphics.fill(yesBtnX, btnY, yesBtnX + btnW, btnY + btnH, hoverYes ? 0xFF884444 : 0xFF553333);

        Component noText = Component.translatable("screen.deltaforce.settings.no");
        Component yesText = Component.translatable("screen.deltaforce.settings.yes");
        guiGraphics.drawString(this.font, noText, noBtnX + (btnW - this.font.width(noText)) / 2, btnY + 2, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, yesText, yesBtnX + (btnW - this.font.width(yesText)) / 2, btnY + 2, 0xFFFF5555, false);
    }

    // ======================== 设置界面配置浮层 ========================

    /** 设置浮层尺寸（渲染与命中判定共用，改一处即可，避免滑块点击区错位）。 */
    private static final int SETTINGS_OVERLAY_W = 220;
    /** 高度按内容算：28（首行）+ 34（行距）+ 10（滑块）+ 2（手柄）+ 10（底边距）= 84。 */
    private static final int SETTINGS_OVERLAY_H = 84;
    /** 浮层内滑块的行间距（每个滑块占一行）。 */
    private static final int SETTINGS_OVERLAY_ROW_GAP = 34;
    /** 第一个滑块的纵坐标偏移（标题占 6~15，标签画在滑块上方 10 像素，28 起不压标题）。 */
    private static final int SETTINGS_OVERLAY_SLIDER_TOP = 28;

    private void renderSettingsOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int overlayW = SETTINGS_OVERLAY_W;
        int overlayH = SETTINGS_OVERLAY_H;
        int overlayX = (refW - overlayW) / 2;
        int overlayY = (refH - overlayH) / 2;

        guiGraphics.fill(0, 0, refW, refH, 0x60000000);
        guiGraphics.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH, 0xFF1A1A2E);
        guiGraphics.renderOutline(overlayX, overlayY, overlayW, overlayH, 0xFFFFFFFF);

        Component title = Component.translatable("screen.deltaforce.settings.display_config");
        int titleW = this.font.width(title);
        guiGraphics.drawString(this.font, title, overlayX + (overlayW - titleW) / 2, overlayY + 6, 0xFFFFFFFF, false);

        int sliderStartY = overlayY + SETTINGS_OVERLAY_SLIDER_TOP;
        int sliderLeft = overlayX + 16;
        int sliderW = overlayW - 48;
        int sliderH = 10;

        renderOverlaySlider(guiGraphics, mouseX, mouseY, overlayX, sliderStartY, sliderLeft, sliderW, sliderH,
                Component.translatable("screen.deltaforce.settings.row_height"), entryRowHeight, 20, 40, 1, "rowHeight");

        // 滑块显示「当前真正生效」的倍数：自动模式下随界面尺寸变化（>= 3 → 1.0，< 3 → 1.5），
        // 拖一下即切到手动模式并记住玩家的选择。
        renderOverlaySlider(guiGraphics, mouseX, mouseY, overlayX, sliderStartY + SETTINGS_OVERLAY_ROW_GAP,
                sliderLeft, sliderW, sliderH,
                Component.translatable("screen.deltaforce.settings.ui_scale"),
                (float) DFClientConfig.getInstance().resolveUiScale(Minecraft.getInstance().getWindow().getGuiScale()),
                (float) DFClientConfig.MIN_UI_SCALE, (float) DFClientConfig.MAX_UI_SCALE, 0.1F, "uiScale");

        // 关闭按钮
        int closeBtnW = 40;
        int closeBtnH = 14;
        int closeBtnX = overlayX + overlayW - closeBtnW - 8;
        int closeBtnY = overlayY + 4;
        boolean hoverClose = mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnH;
        guiGraphics.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + closeBtnH, hoverClose ? 0xFF884444 : 0xFF553333);
        Component closeText = Component.translatable("screen.deltaforce.settings.close");
        guiGraphics.drawString(this.font, closeText, closeBtnX + (closeBtnW - this.font.width(closeText)) / 2, closeBtnY + 2, 0xFFFFFFFF, false);
    }

    private void renderOverlaySlider(GuiGraphics guiGraphics, int mouseX, int mouseY,
                                     int overlayX, int sliderY, int sliderLeft, int sliderW, int sliderH,
                                     Component label, float value, float min, float max, float step, String id) {
        guiGraphics.drawString(this.font, label, sliderLeft, sliderY - 10, 0xFFCCCCCC, false);

        Component valText = Component.literal(id.equals("rowHeight") ? String.valueOf((int) value) : String.format("%.2f", value));
        int valW = this.font.width(valText);
        guiGraphics.drawString(this.font, valText, sliderLeft + sliderW + 4, sliderY - 1, 0xFFFFFFFF, false);

        guiGraphics.fill(sliderLeft, sliderY, sliderLeft + sliderW, sliderY + sliderH, SLIDER_BG);

        float ratio = (value - min) / (max - min);
        int handleX = sliderLeft + (int) (ratio * sliderW) - 3;
        guiGraphics.fill(sliderLeft, sliderY, handleX + 3, sliderY + sliderH, 0x6044AA44);
        guiGraphics.fill(handleX, sliderY - 2, handleX + 6, sliderY + sliderH + 2, SLIDER_HANDLE);
    }

    private boolean handleSettingsOverlayClick(double mouseX, double mouseY) {
        int overlayW = SETTINGS_OVERLAY_W;
        int overlayH = SETTINGS_OVERLAY_H;
        int overlayX = (refW - overlayW) / 2;
        int overlayY = (refH - overlayH) / 2;

        // 关闭按钮
        int closeBtnW = 40;
        int closeBtnH = 14;
        int closeBtnX = overlayX + overlayW - closeBtnW - 8;
        int closeBtnY = overlayY + 4;
        if (mouseX >= closeBtnX && mouseX < closeBtnX + closeBtnW && mouseY >= closeBtnY && mouseY < closeBtnY + closeBtnH) {
            showSettingsOverlay = false;
            return true;
        }

        // 点击浮层外部关闭
        if (mouseX < overlayX || mouseX > overlayX + overlayW || mouseY < overlayY || mouseY > overlayY + overlayH) {
            showSettingsOverlay = false;
            return true;
        }

        // 滑块点击处理
        int sliderStartY = overlayY + SETTINGS_OVERLAY_SLIDER_TOP;
        int sliderLeft = overlayX + 16;
        int sliderW = overlayW - 48;
        int sliderH = 10;

        if (handleOverlaySliderDrag(mouseX, mouseY, sliderStartY, sliderLeft, sliderW, sliderH, 20, 40, 1, "rowHeight")) return true;
        if (handleOverlaySliderDrag(mouseX, mouseY, sliderStartY + SETTINGS_OVERLAY_ROW_GAP, sliderLeft, sliderW, sliderH,
                (float) DFClientConfig.MIN_UI_SCALE, (float) DFClientConfig.MAX_UI_SCALE, 0.1F, "uiScale")) return true;

        return false;
    }

    private boolean handleOverlaySliderDrag(double mouseX, double mouseY, int sliderY, int sliderLeft, int sliderW, int sliderH,
                                            float min, float max, float step, String id) {
        if (mouseY >= sliderY - 4 && mouseY < sliderY + sliderH + 4 && mouseX >= sliderLeft && mouseX < sliderLeft + sliderW) {
            float ratio = (float) Math.max(0, Math.min(1, (mouseX - sliderLeft) / sliderW));
            float value = min + ratio * (max - min);
            value = Math.round(value / step) * step;
            value = Math.max(min, Math.min(max, value));

            switch (id) {
                case "rowHeight" -> entryRowHeight = Math.round(value);
                case "uiScale" -> {
                    DFClientConfig cfg = DFClientConfig.getInstance();
                    // 拖动过程中每帧都会进这里，只在数值真的变了才写盘，避免频繁 IO
                    boolean changed = Math.abs(cfg.getUiScale() - value) > 1.0E-6D || cfg.isUiScaleAuto();
                    if (!changed) {
                        return true;
                    }
                    cfg.setUiScale(value);
                    cfg.save();
                    // 立即按新倍数重算画布与适配系数，松手即可看到放大效果
                    double multiplier = cfg.resolveUiScale(Minecraft.getInstance().getWindow().getGuiScale());
                    this.refW = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_WIDTH, multiplier);
                    this.refH = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_HEIGHT, multiplier);
                    this.uiScale = UiCanvasScaleMath.fitScale(this.width, this.height, this.refW, this.refH);
                    this.offsetX = UiCanvasScaleMath.canvasOffset(this.width, this.refW, this.uiScale);
                    this.offsetY = UiCanvasScaleMath.canvasOffset(this.height, this.refH, this.uiScale);
                    // 画布尺寸变了，依赖 refW/refH 的布局都要重算（X 与 Y 都要，否则底栏按钮会飘）
                    this.contentBottom = this.refH - BOTTOM_BAR_HEIGHT - 4;
                    int barY = this.refH - BOTTOM_BAR_HEIGHT;
                    int btnW = 60;
                    int btnH = BOTTOM_BAR_HEIGHT;
                    int rightMargin = this.refW * BOTTOM_BTN_MARGIN_RATIO / 100;
                    exitBtnX = this.refW - rightMargin - btnW; exitBtnY = barY; exitBtnW = btnW; exitBtnH = btnH;
                    cancelBtnX = exitBtnX - 6 - btnW; cancelBtnY = barY; cancelBtnW = btnW; cancelBtnH = btnH;
                    saveBtnX = cancelBtnX - 6 - btnW; saveBtnY = barY; saveBtnW = btnW; saveBtnH = btnH;
                    int smallBtnW = 24;
                    redoBtnX = saveBtnX - 4 - smallBtnW; redoBtnY = barY; redoBtnW = smallBtnW; redoBtnH = btnH;
                    undoBtnX = redoBtnX - 2 - smallBtnW; undoBtnY = barY; undoBtnW = smallBtnW; undoBtnH = btnH;
                    previewBtnW = 50; previewBtnH = btnH;
                    previewBtnX = undoBtnX - 6 - previewBtnW; previewBtnY = barY;
                }
            }
            return true;
        }
        return false;
    }

    // ======================== 本地化辅助 ========================

    private Component localizePath(String modId, String path, String description) {
        // 先尝试完整路径翻译
        String fullKey = "config." + modId + "." + path;
        Component fullTranslated = Component.translatable(fullKey);
        if (!fullTranslated.getString().equals(fullKey)) {
            return fullTranslated;
        }
        // 没有完整翻译：逐段翻译每级路径，显示所有层级
        String[] parts = path.split("\\.");
        if (parts.length <= 2) {
            // 1-2段：全部显示
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(" > ");
                sb.append(localizeSegment(modId, parts[i]));
            }
            return Component.literal(sb.toString());
        }
        // 3段及以上：显示"分类 > 子分类 > 字段名"
        String firstPart = parts[0];
        String lastPart = parts[parts.length - 1];
        // 翻译分类段
        String catTranslated = localizeSegment(modId, firstPart);
        // 翻译字段段：优先从描述提取，其次从翻译键，最后回退到 humanize
        String fieldTranslated = null;
        if (description != null && !description.isBlank()) {
            fieldTranslated = extractFieldFromDescription(description);
        }
        if (fieldTranslated == null) {
            fieldTranslated = localizeSegment(modId, lastPart);
        }
        // 如果翻译结果和 humanize 一样（说明没有翻译），且与上一个段不同，尝试用描述中的其他信息
        if (fieldTranslated.equals(humanize(lastPart)) && description != null && !description.isBlank()) {
            // 描述可能直接就是字段说明
            String shortDesc = description.split("\\.")[0];
            if (shortDesc.length() > 0 && shortDesc.length() < 30) {
                fieldTranslated = shortDesc.trim();
            }
        }
        // 中间段：取第一个中间段作为子分类显示
        String middleTranslated = localizeSegment(modId, parts[1]);
        return Component.literal(catTranslated + " > " + middleTranslated + " > " + fieldTranslated);
    }

    /** 从描述中提取字段名，如 "Controls shop / General / Price. Default:..." -> "Price" */
    private static String extractFieldFromDescription(String description) {
        // 描述格式: "Controls {category} / {subcategory} / {Field Name}. Default:..."
        int lastSlash = description.lastIndexOf(" / ");
        if (lastSlash < 0) return null;
        int dotAfter = description.indexOf(".", lastSlash);
        if (dotAfter < 0) return null;
        return description.substring(lastSlash + 3, dotAfter).trim();
    }

    /** 翻译路径中的单个段，尝试多种键模式（结果缓存） */
    private String localizeSegment(String modId, String segment) {
        String cacheKey = modId + ":" + segment;
        String cached = segmentTranslationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 先尝试角色名翻译（character.{modId}.{id}.name）
        String charName = resolveCharacterName(modId, segment);
        if (charName != null) {
            segmentTranslationCache.put(cacheKey, charName);
            return charName;
        }
        // 尝试多种键模式
        String[] keys = {
            "config." + modId + "." + segment,
            "config." + modId + ".subcategory." + segment,
            "config." + modId + ".category." + segment,
        };
        for (String key : keys) {
            Component translated = Component.translatable(key);
            if (!translated.getString().equals(key)) {
                String result = translated.getString();
                segmentTranslationCache.put(cacheKey, result);
                return result;
            }
        }
        String fallback = humanize(segment);
        segmentTranslationCache.put(cacheKey, fallback);
        return fallback;
    }

    /** 解析角色名翻译，移植自DFS ConfigLabelHelper */
    private String resolveCharacterName(String modId, String configId) {
        if (configId == null || configId.isBlank()) {
            return null;
        }
        String langId = normalizeCharacterId(configId);
        String key = "character." + modId + "." + langId + ".name";
        Component translated = Component.translatable(key);
        if (!translated.getString().equals(key)) {
            return translated.getString();
        }
        // 尝试别名
        for (String alt : characterLangCandidates(configId)) {
            String altKey = "character." + modId + "." + alt + ".name";
            Component altTranslated = Component.translatable(altKey);
            if (!altTranslated.getString().equals(altKey)) {
                return altTranslated.getString();
            }
        }
        return null;
    }

    /** 规范化角色ID */
    private static String normalizeCharacterId(String id) {
        if (id == null) return "";
        String lower = id.toLowerCase();
        return switch (lower) {
            case "dwolf" -> "d_wolf";
            case "lexninjia" -> "lex_ninjia";
            case "n_two" -> "ntwo";
            case "nikaidou" -> "nikaidou_hiro";
            default -> lower;
        };
    }

    /** 角色ID别名候选列表 */
    private static List<String> characterLangCandidates(String configId) {
        String lower = configId.toLowerCase();
        List<String> list = new ArrayList<>();
        list.add(lower);
        list.add(normalizeCharacterId(lower));
        switch (lower) {
            case "dwolf" -> list.add("d_wolf");
            case "d_wolf" -> list.add("dwolf");
            case "lexninjia" -> list.add("lex_ninjia");
            case "lex_ninjia" -> list.add("lexninjia");
            case "ntwo", "n_two" -> {
                list.add("ntwo");
                list.add("n_two");
            }
            case "nikaidou" -> {
                list.add("nikaidou_hiro");
                list.add("nikaidou_hiro_witchification");
            }
            case "department" -> list.add("department");
            default -> {}
        }
        return list;
    }

    private static String humanize(String key) {
        String[] words = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /** 只翻译路径的叶子段（最后一段），返回主名（结果缓存） */
    private Component localizeLeaf(String modId, String path) {
        if ("dealt_force_skills".equals(modId)) {
            IConfigProvider provider = DeltaForce.getConfigProvider(modId);
            if (provider instanceof DfsConfigProvider dfs && dfs.entry(path) != null) {
                return ConfigLabelHelper.entryLabel(dfs.entry(path));
            }
        }
        String cacheKey = modId + "::leaf:" + path;
        Component cached = leafTranslationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 先尝试完整路径翻译
        String fullKey = "config." + modId + "." + path;
        Component fullTranslated = Component.translatable(fullKey);
        if (!fullTranslated.getString().equals(fullKey)) {
            leafTranslationCache.put(cacheKey, fullTranslated);
            return fullTranslated;
        }
        String[] parts = path.split("\\.");
        String lastPart = parts[parts.length - 1];
        // 尝试 leaf 级翻译
        String leafKey = "config." + modId + ".leaf." + lastPart;
        Component leafTranslated = Component.translatable(leafKey);
        if (!leafTranslated.getString().equals(leafKey)) {
            leafTranslationCache.put(cacheKey, leafTranslated);
            return leafTranslated;
        }
        Component result = Component.literal(localizeSegment(modId, lastPart));
        leafTranslationCache.put(cacheKey, result);
        return result;
    }

    /** 返回路径的上下文（除最后一段外的层级），用于小字描述（结果缓存） */
    private String localizeContext(String modId, String path) {
        if ("dealt_force_skills".equals(modId)) {
            IConfigProvider provider = DeltaForce.getConfigProvider(modId);
            if (provider instanceof DfsConfigProvider dfs && dfs.entry(path) != null) {
                ConfigEntryData entry = dfs.entry(path);
                return ConfigLabelHelper.fileBadge(entry.file()).getString()
                        + " / " + ConfigLabelHelper.subcategoryTitle(
                                ConfigAccess.categoryKey(entry),
                                ConfigAccess.subcategoryKey(entry)).getString();
            }
        }
        String cacheKey = modId + "::ctx:" + path;
        String cached = contextTranslationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String[] parts = path.split("\\.");
        if (parts.length <= 1) {
            contextTranslationCache.put(cacheKey, "");
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) sb.append(" > ");
            sb.append(localizeSegment(modId, parts[i]));
        }
        String result = sb.toString();
        contextTranslationCache.put(cacheKey, result);
        return result;
    }

    private Component localizeCategory(String modId, String cat) {
        if ("dealt_force_skills".equals(modId)) {
            return ConfigLabelHelper.categoryTitle(cat);
        }
        String cacheKey = modId + "::cat:" + cat;
        String cached = segmentTranslationCache.get(cacheKey);
        if (cached != null) {
            return Component.literal(cached);
        }
        String key = "config." + modId + ".category." + cat;
        Component translated = Component.translatable(key);
        if (translated.getString().equals(key)) {
            String fallback = humanize(cat);
            segmentTranslationCache.put(cacheKey, fallback);
            return Component.literal(fallback);
        }
        String result = translated.getString();
        segmentTranslationCache.put(cacheKey, result);
        return translated;
    }

    private Component localizeSubCategory(String modId, String sub) {
        if ("dealt_force_skills".equals(modId)) {
            return ConfigLabelHelper.subcategoryTitle(
                    categories.isEmpty() ? "" : categories.get(selectedCat), sub);
        }
        String cacheKey = modId + "::subcat:" + sub;
        String cached = segmentTranslationCache.get(cacheKey);
        if (cached != null) {
            return Component.literal(cached);
        }
        String key = "config." + modId + ".subcategory." + sub;
        Component translated = Component.translatable(key);
        if (translated.getString().equals(key)) {
            String fallback = humanize(sub);
            segmentTranslationCache.put(cacheKey, fallback);
            return Component.literal(fallback);
        }
        String result = translated.getString();
        segmentTranslationCache.put(cacheKey, result);
        return translated;
    }

    // ======================== 搜索匹配 ========================

    private boolean matchesSearch(IConfigProvider.ConfigEntry entry, String lower) {
        // 直接匹配显示名称（主名 + 上下文）
        String modId = getCurrentModId();
        String path = entry.path();
        String leafStr = localizeLeaf(modId, path).getString();
        String contextStr = localizeContext(modId, path);
        String displayStr = contextStr.isEmpty() ? leafStr : leafStr + " " + contextStr;
        if (displayStr.toLowerCase().contains(lower)) return true;

        // 匹配原始路径
        if (entry.path().toLowerCase().contains(lower)) return true;

        // 匹配拼音首字母（显示名称中每个汉字的拼音首字母）
        String pinyinInitials = PinyinHelper.getFullPinyinInitials(displayStr);
        if (pinyinInitials.toLowerCase().contains(lower)) return true;

        // 匹配拼音首字母（路径中的英文单词首字母）
        String[] parts = entry.path().split("\\.");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            String[] words = part.split("_");
            for (String word : words) {
                if (!word.isEmpty()) {
                    initials.append(word.charAt(0));
                }
            }
        }
        return initials.toString().toLowerCase().contains(lower);
    }

    // ======================== 辅助方法 ========================

    private IConfigProvider getCurrentProvider() {
        if (selectedModTab >= 0 && selectedModTab < loadedModIds.size()) {
            return DeltaForce.getConfigProvider(loadedModIds.get(selectedModTab));
        }
        return null;
    }

    private String getCurrentModId() {
        if (selectedModTab >= 0 && selectedModTab < loadedModIds.size()) {
            return loadedModIds.get(selectedModTab);
        }
        return "";
    }

    private boolean hasUnsavedChanges() {
        IConfigProvider provider = getCurrentProvider();
        return provider != null && provider.isDirty();
    }

    private void tryExit() {
        if (hasUnsavedChanges()) {
            showUnsavedDialog = true;
        } else {
            openMainScreen();
        }
    }

    private void discardAndExit() {
        IConfigProvider provider = getCurrentProvider();
        if (provider != null) provider.cancel();
        dirtyValues.clear();
        openMainScreen();
    }

    // ======================== 鼠标事件 ========================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // HUD布局拖拽优先处理（预览模式 + DFS标签页，使用屏幕坐标）
        if (isPreviewing && button == 0 && "dealt_force_skills".equals(getCurrentModId())) {
            // 检测是否点击了技能按钮区域
            if (mouseX >= hudSkillsVisX && mouseX < hudSkillsVisX + hudSkillsVisW
                    && mouseY >= hudSkillsVisY && mouseY < hudSkillsVisY + hudSkillsVisH) {
                hudDragTarget = HUD_DRAG_SKILLS;
                hudDragStartMouseX = mouseX;
                hudDragStartMouseY = mouseY;
                hudDragStartAnchorX = hudSkillsVisX;
                hudDragStartAnchorY = hudSkillsVisY;
                return true;
            }
            // 检测是否点击了体力条区域
            if (mouseX >= hudStaminaVisX && mouseX < hudStaminaVisX + hudStaminaVisW
                    && mouseY >= hudStaminaVisY && mouseY < hudStaminaVisY + hudStaminaVisH) {
                hudDragTarget = HUD_DRAG_STAMINA;
                hudDragStartMouseX = mouseX;
                hudDragStartMouseY = mouseY;
                hudDragStartAnchorX = hudStaminaVisX;
                hudDragStartAnchorY = hudStaminaVisY;
                return true;
            }
        }

        float inv = 1.0f / this.uiScale;
        int mx = (int)((mouseX - this.offsetX) * inv);
        int my = (int)((mouseY - this.offsetY) * inv);
        int w = refW;
        int h = refH;

        if (showUnsavedDialog) {
            int dialogW = 200, dialogH = 60;
            int dialogX = (w - dialogW) / 2, dialogY = (h - dialogH) / 2;
            int btnW = 50, btnH = 14, btnY = dialogY + dialogH - btnH - 6;
            int noBtnX = dialogX + dialogW / 2 - btnW - 4, yesBtnX = dialogX + dialogW / 2 + 4;

            if (mx >= noBtnX && mx < noBtnX + btnW && my >= btnY && my < btnY + btnH) {
                showUnsavedDialog = false;
                return true;
            }
            if (mx >= yesBtnX && mx < yesBtnX + btnW && my >= btnY && my < btnY + btnH) {
                showUnsavedDialog = false;
                discardAndExit();
                return true;
            }
            return true;
        }

        if (button == 0) {
            // 设置齿轮按钮
            int gearSize = tabHeight;
            if (mx >= tabStartX && mx < tabStartX + gearSize
                    && my >= tabStartY && my < tabStartY + gearSize) {
                showSettingsOverlay = !showSettingsOverlay;
                return true;
            }

            // 设置浮层内的点击
            if (showSettingsOverlay) {
                if (handleSettingsOverlayClick(mx, my)) {
                    return true;
                }
            }

            // 顶栏
            int tabStartOffset = tabStartX + gearSize + 4;
            for (int i = 0; i < loadedModNames.size(); i++) {
                int x = tabStartOffset + i * tabWidth;
                if (mx >= x && mx < x + tabWidth && my >= tabStartY && my < tabStartY + tabHeight) {
                    if (selectedModTab != i) {
                        selectedModTab = i;
                        refreshCategories();
                        saveScreenState();
                    }
                    return true;
                }
            }

            // 分类栏
            if (my >= catBarY && my < catBarY + catBarH) {
                int cx = 8;
                int gap = 3;
                for (int i = 0; i < categories.size(); i++) {
                    int textW = this.font.width(localizeCategory(getCurrentModId(), categories.get(i))) + 8;
                    int tabH = catBarH - 2;
                    int tabY = catBarY + 1;
                    if (mx >= cx && mx < cx + textW && my >= tabY && my < tabY + tabH) {
                        if (selectedCat != i) {
                            selectedCat = i;
                            IConfigProvider provider = getCurrentProvider();
                            subCategories.clear();
                            if (provider != null) {
                                subCategories.addAll(provider.getSubCategories(categories.get(selectedCat)));
                            }
                            selectedSubcat = 0;
                            scrollOffset = 0;
                        }
                        return true;
                    }
                    cx += textW + gap;
                }
            }

            // 子分类栏
            if (my >= subcatBarY && my < subcatBarY + subcatBarH) {
                // 检查是否点击了滚动条
                if (subcatMaxScroll > 0) {
                    int scrollBarY = subcatBarY + subcatBarH - 2;
                    int scrollBarH = 2;
                    int visibleWidth = w - 16;
                    int totalWidth = 8;
                    int gap = 2;
                    for (String sub : subCategories) {
                        Component label = localizeSubCategory(getCurrentModId(), sub);
                        totalWidth += this.font.width(label) + 6 + gap;
                    }
                    int handleWidth = Math.max(20, visibleWidth * visibleWidth / totalWidth);
                    int handleX = 8 + (subcatScrollOffset * (visibleWidth - handleWidth) / subcatMaxScroll);

                    if (my >= scrollBarY && my < scrollBarY + scrollBarH &&
                        mx >= handleX && mx < handleX + handleWidth) {
                        isDraggingSubcatBar = true;
                        return true;
                    }
                    // 点击轨道空白处，跳转到对应位置
                    if (my >= scrollBarY && my < scrollBarY + scrollBarH && mx >= 8 && mx < 8 + visibleWidth) {
                        int newHandleX = mx - handleWidth / 2;
                        newHandleX = Math.max(8, Math.min(newHandleX, 8 + visibleWidth - handleWidth));
                        subcatScrollOffset = (newHandleX - 8) * subcatMaxScroll / (visibleWidth - handleWidth);
                        subcatScrollOffset = Math.max(0, Math.min(subcatScrollOffset, subcatMaxScroll));
                        return true;
                    }
                }

                int sx = 8 - subcatScrollOffset;
                int gap = 2;
                for (int i = 0; i < subCategories.size(); i++) {
                    int textW = this.font.width(localizeSubCategory(getCurrentModId(), subCategories.get(i))) + 6;
                    int tabH = subcatBarH - 2;
                    int tabY = subcatBarY + 1;
                    if (mx >= sx && mx < sx + textW && my >= tabY && my < tabY + tabH) {
                        if (selectedSubcat != i) {
                            selectedSubcat = i;
                            scrollOffset = 0;
                        }
                        return true;
                    }
                    sx += textW + gap;
                }
            }

            // 底栏
            // 预览按钮（自定义掉落物HUD / HUD布局 标签页显示，开关模式）
            if (("deltaforce".equals(getCurrentModId()) || "dealt_force_skills".equals(getCurrentModId()))
                    && mx >= previewBtnX && mx < previewBtnX + previewBtnW
                    && my >= previewBtnY && my < previewBtnY + previewBtnH) {
                isPreviewing = !isPreviewing;
                if (isPreviewing) {
                    startPreview();
                } else {
                    stopPreview();
                }
                return true;
            }

            if (hasUnsavedChanges() && mx >= saveBtnX && mx < saveBtnX + saveBtnW && my >= saveBtnY && my < saveBtnY + saveBtnH) {
                IConfigProvider provider = getCurrentProvider();
                if (provider != null) provider.save();
                dirtyValues.clear();
                focusedEditKey = null;
                invalidateEntriesCache();
                return true;
            }
            if (hasUnsavedChanges() && mx >= cancelBtnX && mx < cancelBtnX + cancelBtnW && my >= cancelBtnY && my < cancelBtnY + cancelBtnH) {
                IConfigProvider provider = getCurrentProvider();
                if (provider != null) provider.cancel();
                dirtyValues.clear();
                focusedEditKey = null;
                invalidateEntriesCache();
                return true;
            }
            if (mx >= exitBtnX && mx < exitBtnX + exitBtnW && my >= exitBtnY && my < exitBtnY + exitBtnH) {
                tryExit();
                return true;
            }
            // 撤销按钮
            if (!undoStack.isEmpty() && mx >= undoBtnX && mx < undoBtnX + undoBtnW && my >= undoBtnY && my < undoBtnY + undoBtnH) {
                undo();
                return true;
            }
            // 重做按钮
            if (!redoStack.isEmpty() && mx >= redoBtnX && mx < redoBtnX + redoBtnW && my >= redoBtnY && my < redoBtnY + redoBtnH) {
                redo();
                return true;
            }

            // 全局搜索框点击
            int searchBoxH = 18;
            int searchBoxW = 160;
            int globalSearchY = contentTop + 2;
            if (mx >= 14 && mx < 14 + searchBoxW && my >= globalSearchY && my < globalSearchY + searchBoxH) {
                globalSearchFocused = true;
                searchFocused = false;
                focusedEditKey = null;
                return true;
            }
            // 本地搜索框点击
            int localSearchY = globalSearchY;
            int localSearchX = 14 + searchBoxW + 6;
            if (mx >= localSearchX && mx < localSearchX + searchBoxW && my >= localSearchY && my < localSearchY + searchBoxH) {
                searchFocused = true;
                globalSearchFocused = false;
                focusedEditKey = null;
                return true;
            }

            // 获取条目列表（提前，供滚动条和配置项共用）
            List<IConfigProvider.ConfigEntry> entries = cachedEntries != null ? cachedEntries : getCurrentEntries();

            // 主滚动条点击（侧条）：点击轨道跳转或点击滑块开始拖拽
            int listTop = localSearchY + searchBoxH + 4;
            int contentHeight = contentBottom - listTop;
            if (maxScroll > 0 && mx >= w - SCROLL_BAR_WIDTH - 4 && mx < w && my >= listTop && my < contentBottom) {
                int totalRows = entries.size();
                int handleH = Math.max(16, (int) ((float) contentHeight / (totalRows * entryRowHeight) * contentHeight));
                int handleY = listTop + (int) ((float) scrollOffset / maxScroll * (contentHeight - handleH));
                if (my >= handleY && my < handleY + handleH) {
                    isDraggingMainScrollBar = true;
                } else {
                    // 点击轨道空白处，跳转到对应位置
                    int targetScroll = (int) ((float) (my - listTop) / contentHeight * maxScroll);
                    scrollOffset = Math.max(0, Math.min(targetScroll, maxScroll));
                }
                return true;
            }

            // 配置项点击
            int maxRight = w - SCROLL_BAR_WIDTH - 14;
            int widgetW = 120;

            for (int i = 0; i < entries.size(); i++) {
                int rowY = listTop + i * entryRowHeight - scrollOffset;
                if (my < rowY || my >= rowY + entryRowHeight) continue;

                IConfigProvider.ConfigEntry entry = entries.get(i);
                String path = entry.path();

                // 固定居中计算，匹配渲染时的逻辑
                Component keyText = localizeLeaf(getCurrentModId(), path);
                String context = localizeContext(getCurrentModId(), path);
                int fixedCenter = (14 + maxRight) / 2;
                int adjLeft = fixedCenter - widgetW / 2;
                int adjRight = adjLeft + widgetW;

                // 根据嵌套层级缩进
                int indent = 14 + entry.nestLevel() * 16;
                // 如果控件和名称重叠，将控件右移（取主名和上下文较宽者）
                int nameWidth = this.font.width(keyText);
                int contextWidth = context.isEmpty() ? 0 : this.font.width(Component.literal(context));
                int nameEnd = indent + Math.max(nameWidth, contextWidth) + 14;
                if (adjLeft < nameEnd + 10) {
                    adjLeft = nameEnd + 10;
                    adjRight = adjLeft + widgetW;
                }

                // 切换按钮区域
                int toggleW = 18;
                int toggleH = 14;
                int toggleX = adjLeft + widgetW - toggleW;
                int toggleY = rowY + (entryRowHeight - toggleH) / 2;

                if (entry.isNumeric() && mx >= toggleX && mx < toggleX + toggleW
                        && my >= toggleY && my < toggleY + toggleH) {
                    boolean useSlider = sliderMode.getOrDefault(path, false);
                    sliderMode.put(path, !useSlider);
                    return true;
                }

                // 快捷键绑定按钮点击
                if ("customdrop.hud.hotkey".equals(path)) {
                    if (mx >= adjLeft && mx < adjRight) {
                        capturingKeyPath = path;
                        capturedKeys.clear();
                        focusedEditKey = null;
                        return true;
                    }
                }

                if (mx >= adjLeft && mx < adjLeft + widgetW) {
                    IConfigProvider provider = getCurrentProvider();
                    switch (entry.kind()) {
                        case BOOLEAN -> {
                            toggleBoolean(entry);
                            return true;
                        }
                        case INTEGER, LONG, FLOAT, DOUBLE -> {
                            if (provider != null && !provider.canEdit(path)) return true;
                            boolean useSlider = sliderMode.getOrDefault(path, false);
                            int contentRight = toggleX - 4;
                            if (useSlider && mx >= contentRight - 80 && mx < contentRight) {
                                handleSliderClick(mx, entry, contentRight);
                                return true;
                            }
                            focusedEditKey = path;
                            searchFocused = false;
                            globalSearchFocused = false;
                            return true;
                        }
                        case STRING -> {
                            if (provider != null && !provider.canEdit(path)) return true;
                            focusedEditKey = path;
                            searchFocused = false;
                            globalSearchFocused = false;
                            return true;
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleBoolean(IConfigProvider.ConfigEntry entry) {
        String path = entry.path();
        IConfigProvider provider = getCurrentProvider();
        if (provider != null && !provider.canEdit(path)) return;
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        String newValue = "true".equalsIgnoreCase(currentValue) ? "false" : "true";
        putDirty(path, newValue);
        if (provider != null) provider.setValue(path, newValue);
        invalidateEntriesCache();
    }

    private void handleSliderClick(double mouseX, IConfigProvider.ConfigEntry entry, int widgetRight) {
        String path = entry.path();
        IConfigProvider provider = getCurrentProvider();
        if (provider != null && !provider.canEdit(path)) return;

        int sliderW = 80;
        int sliderX = widgetRight - sliderW - 4;
        double ratio = (mouseX - sliderX) / sliderW;
        ratio = Math.max(0, Math.min(1, ratio));

        boolean isFloat = entry.kind() == IConfigProvider.ConfigValueKind.FLOAT
                || entry.kind() == IConfigProvider.ConfigValueKind.DOUBLE;
        String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
        double val;
        try { val = Double.parseDouble(currentValue); } catch (NumberFormatException e) { val = 0; }
        double sliderMin = getSliderMin(entry, val);
        double sliderMax = getSliderMax(entry, val);
        double sliderRange = sliderMax - sliderMin;
        double newVal = sliderMin + ratio * sliderRange;
        String newValue = formatNumericValue(newVal, isFloat);

        putDirty(path, newValue);
        if (provider != null) provider.setValue(path, newValue);

        // 设置拖拽锁定，并保存起始值用于后续range计算
        draggingSliderPath = path;
        sliderDragStartValue = val;
        hasSliderDragStart = true;
    }

    /** 滑块拖拽更新（不检查鼠标位置，直接根据X坐标计算值） */
    private void handleSliderDrag(double mouseX, IConfigProvider.ConfigEntry entry, int widgetRight) {
        String path = entry.path();
        IConfigProvider provider = getCurrentProvider();
        if (provider != null && !provider.canEdit(path)) return;

        int sliderW = 80;
        int sliderX = widgetRight - sliderW - 4;
        double ratio = (mouseX - sliderX) / sliderW;
        ratio = Math.max(0, Math.min(1, ratio));

        boolean isFloat = entry.kind() == IConfigProvider.ConfigValueKind.FLOAT
                || entry.kind() == IConfigProvider.ConfigValueKind.DOUBLE;
        // 用拖拽起始值计算range（而非实时变动值），避免正反馈导致range膨胀溢出
        double baseVal = hasSliderDragStart ? sliderDragStartValue : 0;
        if (!hasSliderDragStart) {
            String currentValue = dirtyValues.getOrDefault(path, entry.currentValue());
            try { baseVal = Double.parseDouble(currentValue); } catch (NumberFormatException e) { baseVal = 0; }
        }
        double sliderMin = getSliderMin(entry, baseVal);
        double sliderMax = getSliderMax(entry, baseVal);
        double sliderRange = sliderMax - sliderMin;
        double newVal = sliderMin + ratio * sliderRange;
        String newValue = formatNumericValue(newVal, isFloat);

        putDirtyQuiet(path, newValue);
        updateTopUndoTarget(path, newValue);
        if (provider != null) provider.setValue(path, newValue);
    }

    /** 使条目缓存失效（配置值变更时调用） */
    private void invalidateEntriesCache() {
        cachedEntries = null;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (showUnsavedDialog) return true;

        float inv = 1.0f / this.uiScale;
        int mx = (int)((mouseX - this.offsetX) * inv);
        int my = (int)((mouseY - this.offsetY) * inv);
        int w = refW;

        // 设置浮层滑块拖拽
        if (showSettingsOverlay) {
            if (handleSettingsOverlayClick(mx, my)) {
                return true;
            }
        }

        // 主滚动条拖拽（侧条）
        if (isDraggingMainScrollBar && maxScroll > 0) {
            int searchBoxH = 18;
            int listTop = contentTop + searchBoxH + 6;
            int contentHeight = contentBottom - listTop;
            int totalRows = cachedEntries != null ? cachedEntries.size() : 0;
            int handleH = Math.max(16, (int) ((float) contentHeight / (totalRows * entryRowHeight) * contentHeight));
            int newHandleY = my - handleH / 2;
            newHandleY = Math.max(listTop, Math.min(newHandleY, contentBottom - handleH));
            scrollOffset = (int) ((float) (newHandleY - listTop) / (contentHeight - handleH) * maxScroll);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }

        // 子分类栏滚动条拖拽
        if (isDraggingSubcatBar && subcatMaxScroll > 0) {
            int visibleWidth = w - 16;
            int totalWidth = 8;
            int gap = 2;
            for (String sub : subCategories) {
                Component label = localizeSubCategory(getCurrentModId(), sub);
                totalWidth += this.font.width(label) + 6 + gap;
            }
            int handleWidth = Math.max(20, visibleWidth * visibleWidth / totalWidth);
            int handleX = 8 + (subcatScrollOffset * (visibleWidth - handleWidth) / subcatMaxScroll);

            // 根据鼠标位置计算新的滚动偏移
            int newHandleX = mx - handleWidth / 2;
            newHandleX = Math.max(8, Math.min(newHandleX, 8 + visibleWidth - handleWidth));
            subcatScrollOffset = (newHandleX - 8) * subcatMaxScroll / (visibleWidth - handleWidth);
            subcatScrollOffset = Math.max(0, Math.min(subcatScrollOffset, subcatMaxScroll));
            return true;
        }

        if (!showUnsavedDialog) {
            List<IConfigProvider.ConfigEntry> entries = cachedEntries != null ? cachedEntries : getCurrentEntries();
            int searchBoxH = 18;
            int listTop = contentTop + searchBoxH + 6;
            int maxRight = w - SCROLL_BAR_WIDTH - 14;
            int widgetW = 120;

            // 拖拽锁定：如果正在拖拽某个滑块，只更新该滑块，不响应其他滑块区域
            if (draggingSliderPath != null) {
                for (int i = 0; i < entries.size(); i++) {
                    IConfigProvider.ConfigEntry entry = entries.get(i);
                    if (draggingSliderPath.equals(entry.path()) && entry.isNumeric()) {
                        boolean useSlider = sliderMode.getOrDefault(entry.path(), false);
                        if (useSlider) {
                            Component keyText = localizeLeaf(getCurrentModId(), entry.path());
                            String context = localizeContext(getCurrentModId(), entry.path());
                            int fixedCenter = (14 + maxRight) / 2;
                            int adjLeft = fixedCenter - widgetW / 2;
                            int adjRight = adjLeft + widgetW;

                            int indent = 14 + entry.nestLevel() * 16;
                            int nameWidth = this.font.width(keyText);
                            int contextWidth = context.isEmpty() ? 0 : this.font.width(Component.literal(context));
                            int nameEnd = indent + Math.max(nameWidth, contextWidth) + 14;
                            if (adjLeft < nameEnd + 10) {
                                adjLeft = nameEnd + 10;
                                adjRight = adjLeft + widgetW;
                            }
                            int toggleW = 18;
                            int contentRight = adjRight - toggleW - 4;
                            handleSliderDrag(mx, entry, contentRight);
                            return true;
                        }
                    }
                }
                return true;
            }

            for (int i = 0; i < entries.size(); i++) {
                int rowY = listTop + i * entryRowHeight - scrollOffset;
                if (my < rowY || my >= rowY + entryRowHeight) continue;

                IConfigProvider.ConfigEntry entry = entries.get(i);
                if (entry.isNumeric()) {
                    String path = entry.path();
                    boolean useSlider = sliderMode.getOrDefault(path, false);
                    if (useSlider) {
                        Component keyText = localizeLeaf(getCurrentModId(), path);
                        String context = localizeContext(getCurrentModId(), path);
                        int fixedCenter = (14 + maxRight) / 2;
                        int adjLeft = fixedCenter - widgetW / 2;
                        int adjRight = adjLeft + widgetW;

                        // 根据嵌套层级缩进
                        int indent = 14 + entry.nestLevel() * 16;
                        // 如果控件和名称重叠，将控件右移（取主名和上下文较宽者）
                        int nameWidth = this.font.width(keyText);
                        int contextWidth = context.isEmpty() ? 0 : this.font.width(Component.literal(context));
                        int nameEnd = indent + Math.max(nameWidth, contextWidth) + 14;
                        if (adjLeft < nameEnd + 10) {
                            adjLeft = nameEnd + 10;
                            adjRight = adjLeft + widgetW;
                        }
                        int toggleW = 18;
                        int contentRight = adjRight - toggleW - 4;
                        handleSliderClick(mx, entry, contentRight);
                        return true;
                    }
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // HUD布局拖拽松手：计算偏移差值并写入内存
        if (hudDragTarget != HUD_DRAG_NONE) {
            applyHudDrag(mouseX, mouseY);
            return true;
        }
        if (isDraggingSubcatBar) {
            isDraggingSubcatBar = false;
            return true;
        }
        if (isDraggingMainScrollBar) {
            isDraggingMainScrollBar = false;
            return true;
        }
        if (draggingSliderPath != null) {
            draggingSliderPath = null;
            hasSliderDragStart = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showUnsavedDialog) return true;
        if (showSettingsOverlay) return true;

        float inv = 1.0f / this.uiScale;
        int my = (int)((mouseY - this.offsetY) * inv);

        // 子分类栏滚动
        if (my >= subcatBarY && my < subcatBarY + subcatBarH && subcatMaxScroll > 0) {
            subcatScrollOffset = Math.max(0, Math.min(subcatScrollOffset - (int) (delta * 30), subcatMaxScroll));
            return true;
        }

        // 配置项列表滚动
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (delta * entryRowHeight), maxScroll));
        return true;
    }

    // ======================== 键盘事件 ========================

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // 组合键捕获：松手时提交
        if (capturingKeyPath != null && !capturedKeys.isEmpty()) {
            commitKeyBinding();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** 提交组合键绑定 */
    private void commitKeyBinding() {
        if (capturingKeyPath == null) return;
        IConfigProvider provider = getCurrentProvider();
        if (provider != null && !provider.canEdit(capturingKeyPath)) {
            capturedKeys.clear();
            capturingKeyPath = null;
            return;
        }
        if (provider != null) {
            String keyStr;
            if (capturedKeys.isEmpty()) {
                keyStr = "-1"; // 无绑定
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < capturedKeys.size(); i++) {
                    if (i > 0) sb.append("+");
                    sb.append(capturedKeys.get(i));
                }
                keyStr = sb.toString();
            }
            putDirty(capturingKeyPath, keyStr);
            provider.setValue(capturingKeyPath, keyStr);
        }
        capturedKeys.clear();
        capturingKeyPath = null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showUnsavedDialog) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                showUnsavedDialog = false;
                return true;
            }
            return true;
        }

        // Ctrl+Z 撤销 / Ctrl+Y 重做（全局快捷键）
        if ((modifiers & 2) != 0 && !undoStack.isEmpty() && keyCode == InputConstants.KEY_Z) {
            undo();
            return true;
        }
        if ((modifiers & 2) != 0 && !redoStack.isEmpty() && keyCode == InputConstants.KEY_Y) {
            redo();
            return true;
        }

        // 按键捕获模式：组合键（按住期间按键累积，松手时提交）
        if (capturingKeyPath != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                // ESC清空绑定
                capturedKeys.clear();
                capturedKeys.add(-1);
                commitKeyBinding();
                return true;
            }
            // 添加按键到组合列表（去重，最多5键）
            if (!capturedKeys.contains(keyCode) && capturedKeys.size() < 5) {
                capturedKeys.add(keyCode);
            }
            return true;
        }

        if (globalSearchFocused) {
            return handleSearchKey(keyCode, scanCode, modifiers, true);
        }

        if (searchFocused) {
            return handleSearchKey(keyCode, scanCode, modifiers, false);
        }

        if (focusedEditKey != null) {
            IConfigProvider provider = getCurrentProvider();
            if (provider != null && !provider.canEdit(focusedEditKey)) {
                focusedEditKey = null; // 不可编辑时退出编辑模式
                return true;
            }
            if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN
                    || keyCode == InputConstants.KEY_NUMPADENTER) {
                focusedEditKey = null;
                return true;
            }

            if (keyCode == InputConstants.KEY_BACKSPACE) {
                if (provider != null) {
                    String current = dirtyValues.getOrDefault(focusedEditKey, provider.getValue(focusedEditKey));
                    if (current == null) current = "";
                    if (!current.isEmpty()) {
                        String newValue = current.substring(0, current.length() - 1);
                        putDirty(focusedEditKey, newValue);
                        provider.setValue(focusedEditKey, newValue);
                    }
                }
                return true;
            }

            // 光标移动等导航键
            if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT
                    || keyCode == InputConstants.KEY_HOME || keyCode == InputConstants.KEY_END) {
                return true;
            }

            // 可打印字符不在此处理，返回false让GLFW触发charTyped事件（避免双字符）
            if (provider != null) {
                int key = -1;
                if (keyCode >= InputConstants.KEY_0 && keyCode <= InputConstants.KEY_9) key = keyCode;
                if (keyCode >= InputConstants.KEY_NUMPAD0 && keyCode <= InputConstants.KEY_NUMPAD9) key = keyCode;
                if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) key = keyCode;
                if (keyCode == InputConstants.KEY_PERIOD || keyCode == InputConstants.KEY_MINUS
                        || keyCode == InputConstants.KEY_APOSTROPHE || keyCode == InputConstants.KEY_SEMICOLON
                        || keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_EQUALS
                        || keyCode == InputConstants.KEY_SLASH || keyCode == InputConstants.KEY_BACKSLASH) {
                    key = keyCode;
                }
                if (key != -1) {
                    return false; // 让charTyped处理，避免双字符
                }
            }
            return true;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            // 仅从DFGUI_MAIN进入时才允许ESC返回；从NeoForge模组列表等外部进入时禁止ESC，避免回退到不存在的主界面导致崩溃
            if (!(parent instanceof DFGUI_MAIN)) return true;
            tryExit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private char getCharFromKey(int keyCode, int modifiers) {
        boolean shift = (modifiers & 1) != 0;
        if (keyCode >= InputConstants.KEY_0 && keyCode <= InputConstants.KEY_9) {
            if (shift) return ")!@#$%^&*(".charAt(keyCode - InputConstants.KEY_0);
            return (char) ('0' + (keyCode - InputConstants.KEY_0));
        }
        if (keyCode >= InputConstants.KEY_NUMPAD0 && keyCode <= InputConstants.KEY_NUMPAD9) {
            return (char) ('0' + (keyCode - InputConstants.KEY_NUMPAD0));
        }
        if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) {
            char c = (char) ('a' + (keyCode - InputConstants.KEY_A));
            return shift ? Character.toUpperCase(c) : c;
        }
        if (keyCode == InputConstants.KEY_PERIOD) return shift ? '>' : '.';
        if (keyCode == InputConstants.KEY_MINUS) return shift ? '_' : '-';
        if (keyCode == InputConstants.KEY_APOSTROPHE) return shift ? '"' : '\'';
        if (keyCode == InputConstants.KEY_SEMICOLON) return shift ? ':' : ';';
        if (keyCode == InputConstants.KEY_SPACE) return ' ';
        if (keyCode == InputConstants.KEY_EQUALS) return shift ? '+' : '=';
        if (keyCode == InputConstants.KEY_SLASH) return shift ? '?' : '/';
        if (keyCode == InputConstants.KEY_BACKSLASH) return shift ? '|' : '\\';
        return 0;
    }

    /** 处理搜索框键盘输入（仅处理特殊键，可打印字符返回false让GLFW触发charTyped） */
    private boolean handleSearchKey(int keyCode, int scanCode, int modifiers, boolean isGlobal) {
        if (keyCode == InputConstants.KEY_ESCAPE || keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (isGlobal) globalSearchFocused = false;
            else searchFocused = false;
            return true;
        }
        if (keyCode == InputConstants.KEY_BACKSPACE) {
            String text = isGlobal ? globalSearchText : searchText;
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                if (isGlobal) globalSearchText = text;
                else searchText = text;
            }
            scrollOffset = 0;
            return true;
        }
        if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT
                || keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN
                || keyCode == InputConstants.KEY_HOME || keyCode == InputConstants.KEY_END) {
            return true;
        }
        // 可打印字符：返回false让GLFW生成charTyped事件，实现即时响应
        return false;
    }

    /** 处理IME输入（中文等） */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showUnsavedDialog) return true;

        if (globalSearchFocused) {
            globalSearchText += codePoint;
            scrollOffset = 0;
            return true;
        }
        if (searchFocused) {
            searchText += codePoint;
            scrollOffset = 0;
            return true;
        }
        if (focusedEditKey != null) {
            IConfigProvider provider = getCurrentProvider();
            if (provider != null && !provider.canEdit(focusedEditKey)) {
                focusedEditKey = null;
                return true;
            }
            if (provider != null) {
                String current = dirtyValues.getOrDefault(focusedEditKey, provider.getValue(focusedEditKey));
                if (current == null) current = "";
                String newValue = current + codePoint;
                putDirty(focusedEditKey, newValue);
                provider.setValue(focusedEditKey, newValue);
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        active = null;
        // 清理预览和拖拽状态，防止泄露
        stopPreview();
        hudDragTarget = HUD_DRAG_NONE;
        // 持久化界面状态
        saveScreenState();
        super.removed();
    }

    /** 保存设置界面状态到文件 */
    private void saveScreenState() {
        SettingsUIConfig.setSettingsTabIndex(selectedModTab);
        SettingsUIConfig.setSettingsScrollOffset(scrollOffset);
        SettingsUIConfig.setSettingsRowHeight(entryRowHeight);
        SettingsUIConfig.flush();
    }

    private void openMainScreen() {
        undoStack.clear();
        redoStack.clear();
        Minecraft.getInstance().setScreen(new DFGUI_MAIN());
    }

    // ======================== 撤销/重做系统 ========================

    /** 单次配置变更记录（路径, 变更前值, 变更后值）；变更前值为null表示修改前为原始保存值 */
    private record ChangeRecord(String path, String oldValue, String newValue) {}

    /** 带撤销记录地写入脏值（所有独立配置值修改都应通过此方法） */
    private void putDirty(String path, String newValue) {
        String oldValue = dirtyValues.get(path);
        if (!Objects.equals(oldValue, newValue)) {
            undoStack.push(new ChangeRecord(path, oldValue, newValue));
            redoStack.clear();
        }
        dirtyValues.put(path, newValue);
    }

    /** 仅写入脏值，不记录撤销（用于滑块拖拽中间帧等连续操作） */
    private void putDirtyQuiet(String path, String newValue) {
        dirtyValues.put(path, newValue);
    }

    /** 更新撤销栈顶部记录的目标值（用于滑块拖拽中将整个拖拽会话合并为一步撤销） */
    private void updateTopUndoTarget(String path, String newValue) {
        if (!undoStack.isEmpty()) {
            ChangeRecord top = undoStack.peek();
            if (top.path().equals(path)) {
                undoStack.pop();
                undoStack.push(new ChangeRecord(path, top.oldValue(), newValue));
            }
        }
    }

    /** 应用一条变更记录到指定方向（undo=true恢复旧值，false恢复新值） */
    private void applyRecord(ChangeRecord record, boolean undoing) {
        String valueToApply = undoing ? record.oldValue() : record.newValue();
        IConfigProvider provider = getCurrentProvider();
        if (valueToApply == null) {
            dirtyValues.remove(record.path());
            if (provider != null) provider.setValue(record.path(), provider.getValue(record.path()));
        } else {
            dirtyValues.put(record.path(), valueToApply);
            if (provider != null) provider.setValue(record.path(), valueToApply);
        }
    }

    /** 撤销一步 */
    private void undo() {
        if (undoStack.isEmpty()) return;
        ChangeRecord record = undoStack.pop();
        redoStack.push(record);
        applyRecord(record, true);
        invalidateEntriesCache();
    }

    /** 重做一步 */
    private void redo() {
        if (redoStack.isEmpty()) return;
        ChangeRecord record = redoStack.pop();
        undoStack.push(record);
        applyRecord(record, false);
        invalidateEntriesCache();
    }
}