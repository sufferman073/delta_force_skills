package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.config.DealtBaseConfig;
import com.rzy.dealt_force_skills.network.C2S_SpecialOperationsBaseAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SpecialOperationsBaseSnapshot;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class SpecialOperationsBaseScreen extends Screen {
    private static boolean nextOpenUsesGtiAccess;
    private static final int SLOT = 20;
    private static final int BUYBACK_ROW_HEIGHT = 28;
    private static final int MAX_REDEEM_COUNT = 9999;
    private S2C_SpecialOperationsBaseSnapshot snapshot;
    private final boolean gtiAccess;
    // 嵌入模式：作为主机面内的面板显示，不打开独立界面（不绘制全屏背景）
    private final boolean embedded;
    private Tab tab = Tab.FACILITIES;
    private String sellSource = "INVENTORY";
    private String buybackSearch = "";
    private String buybackCount = "1";
   private String haffExchangeAmount = "10";
    private String specialExchangeAmount = "1";
    private EditBox buybackSearchBox;
    private EditBox buybackCountBox;
    private EditBox haffExchangeBox;
    private EditBox specialExchangeBox;
    private int scrollRow;
    // 界面尺寸补偿系数：独立打开时按界面尺寸把面板整体放大；嵌入模式由宿主缩放，恒为 1.0
    private float scaleFactor = 1.0F;
    // 特勤处内部仓库已删除，移除 draggingWarehouseScrollbar 字段

    private enum Tab {
        FACILITIES("facilities"),
        SELL("sell"),
        BUYBACK("buyback"),
        EXCHANGE("exchange");

        private final String id;

        Tab(String id) {
            this.id = id;
        }
    }

    private SpecialOperationsBaseScreen(S2C_SpecialOperationsBaseSnapshot snapshot) {
        this(snapshot, false);
    }

    private SpecialOperationsBaseScreen(S2C_SpecialOperationsBaseSnapshot snapshot, boolean embedded) {
        super(Component.translatable("screen.dealt_force_skills.base.title"));
        this.snapshot = snapshot;
        this.gtiAccess = nextOpenUsesGtiAccess;
        nextOpenUsesGtiAccess = false;
        this.embedded = embedded;
    }

    /** 创建嵌入模式实例（在主机面内以面板形式显示，不打开独立界面） */
    public static SpecialOperationsBaseScreen createEmbedded(S2C_SpecialOperationsBaseSnapshot snapshot) {
        return new SpecialOperationsBaseScreen(snapshot, true);
    }

    /** 嵌入模式下刷新数据并保持当前标签页（供主机面调用） */
    public void acceptSnapshot(S2C_SpecialOperationsBaseSnapshot snapshot) {
        this.snapshot = snapshot;
        refreshWidgets();
    }

    public static void prepareGtiAccess() { nextOpenUsesGtiAccess = true; }

    public static void clearPendingGtiAccess() { nextOpenUsesGtiAccess = false; }

    public static void accept(S2C_SpecialOperationsBaseSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SpecialOperationsBaseScreen screen) {
            screen.snapshot = snapshot;
            screen.refreshWidgets();
        } else {
            minecraft.setScreen(new SpecialOperationsBaseScreen(snapshot));
        }
    }

    @Override
    protected void init() {
        // 独立打开时按界面尺寸补偿（界面尺寸 2 → 1.5 倍），嵌入模式由宿主统一缩放，恒为 1.0
        scaleFactor = ScreenScale.factor(embedded);
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        int left = panelLeft();
        int tabWidth = Math.max(1, (panelWidth() - 16) / Tab.values().length);
        for (int i = 0; i < Tab.values().length; i++) {
            Tab value = Tab.values()[i];
            addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.base.tab." + value.id),
                            button -> {
                                tab = value;
                                scrollRow = 0;
                                refreshWidgets();
                            })
                    .bounds(left + 8 + i * tabWidth, panelTop() + 30, tabWidth - 3, 20).build());
        }
        if (tab == Tab.FACILITIES) {
            addFacilityButtons();
        } else if (tab == Tab.SELL) {
            addSellSourceButtons();
        } else if (tab == Tab.BUYBACK) {
            addBuybackInputs();
        } else if (tab == Tab.EXCHANGE) {
            addExchangeButtons();
        }
    }

    private void addFacilityButtons() {
        int x = panelLeft() + panelWidth() - 88;
        int y = panelTop() + 62;
        int end = Math.min(snapshot.facilities().size(), scrollRow + visibleFacilityRows());
        for (int index = scrollRow; index < end; index++) {
            S2C_SpecialOperationsBaseSnapshot.FacilityView facility = snapshot.facilities().get(index);
            boolean maxed = facility.level() >= facility.maxLevel();
            Button button = Button.builder(Component.translatable(maxed
                                    ? "screen.dealt_force_skills.base.max"
                                    : "screen.dealt_force_skills.base.upgrade"),
                            ignored -> send(new C2S_SpecialOperationsBaseAction(
                                    C2S_SpecialOperationsBaseAction.Action.UPGRADE,
                                    facility.id(), 0, 0, 0L, snapshot.warehousePage())))
                    .bounds(x, y, 72, 20).build();
            button.active = !maxed && facility.allowed();
            addRenderableWidget(button);
            y += 50;
        }
    }

    private void addWarehouseButtons() {
        int y = panelTop() + panelHeight() - 29;
        Button previous = Button.builder(Component.literal("<"), ignored -> requestPage(snapshot.warehousePage() - 1))
                .bounds(panelLeft() + 12, y, 24, 20).build();
        previous.active = snapshot.warehousePage() > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal(">"), ignored -> requestPage(snapshot.warehousePage() + 1))
                .bounds(panelLeft() + 42, y, 24, 20).build();
        next.active = snapshot.warehousePage() + 1 < snapshot.warehousePages();
        addRenderableWidget(next);
    }

    private void addSellSourceButtons() {
        // 特勤处内部仓库已删除，只保留 INVENTORY 和 CONTAINER
        String[] sources = {"INVENTORY", "CONTAINER"};
        int x = panelLeft() + 12;
        int buttonWidth = Math.max(1, (panelWidth() - 30) / sources.length);
        for (String source : sources) {
            Button button = Button.builder(Component.translatable(
                            "screen.dealt_force_skills.base.source." + source.toLowerCase()), ignored -> {
                        sellSource = source;
                        scrollRow = 0;
                        refreshWidgets();
                    }).bounds(x, panelTop() + 57, buttonWidth, 20).build();
            button.active = !source.equals(sellSource);
            addRenderableWidget(button);
            x += buttonWidth + 6;
        }
    }

    private void addExchangeButtons() {
        haffExchangeBox = addAmountBox(panelTop() + 86, haffExchangeAmount,
                value -> haffExchangeAmount = value);
        specialExchangeBox = addAmountBox(panelTop() + 144, specialExchangeAmount,
                value -> specialExchangeAmount = value);
        addExchangeButton("haff_coins", panelTop() + 86, () -> haffExchangeAmount);
        addExchangeButton("current_special", panelTop() + 144, () -> specialExchangeAmount);
    }

    private void addBuybackInputs() {
        int countWidth = 64;
        int searchWidth = Math.max(60, panelWidth() - countWidth - 98);
        buybackSearchBox = new EditBox(font, panelLeft() + 12, panelTop() + 57, searchWidth, 20,
                Component.translatable("screen.dealt_force_skills.base.search"));
        buybackSearchBox.setMaxLength(80);
        buybackSearchBox.setValue(buybackSearch);
        buybackSearchBox.setResponder(value -> {
            buybackSearch = value;
            scrollRow = 0;
        });
        addRenderableWidget(buybackSearchBox);

        buybackCountBox = new EditBox(font, panelLeft() + panelWidth() - countWidth - 12, panelTop() + 57,
                countWidth, 20, Component.translatable("screen.dealt_force_skills.base.quantity"));
        buybackCountBox.setMaxLength(4);
        buybackCountBox.setFilter(this::validRedeemCountText);
        buybackCountBox.setValue(buybackCount);
        buybackCountBox.setResponder(value -> buybackCount = value);
        buybackCountBox.active = ModGameRules.isRebuyEnabled(Minecraft.getInstance().level)
                && (gtiAccess ? DealtBaseConfig.gtiRedemptionEnabled() : DealtBaseConfig.redemptionEnabled());
        addRenderableWidget(buybackCountBox);
    }

    private EditBox addAmountBox(int y, String value, java.util.function.Consumer<String> responder) {
        int controlY = compactLayout() ? y + 14 : y;
        int boxX = compactLayout() ? panelLeft() + 12 : panelLeft() + 170;
        int buttonWidth = Math.max(54, Math.min(82, panelWidth() / 4));
        int buttonX = panelLeft() + panelWidth() - buttonWidth - 12;
        int boxWidth = Math.max(40, Math.min(120, buttonX - boxX - 6));
        EditBox box = new EditBox(font, boxX, controlY, boxWidth, 20,
                Component.translatable("screen.dealt_force_skills.base.quantity"));
        box.setMaxLength(18);
        box.setFilter(this::validLongAmountText);
        box.setValue(value);
        box.setResponder(responder);
        addRenderableWidget(box);
        return box;
    }

    private void addExchangeButton(String target, int y, java.util.function.Supplier<String> amount) {
        int controlY = compactLayout() ? y + 14 : y;
        int buttonWidth = Math.max(54, Math.min(82, panelWidth() / 4));
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.base.exchange.action"),
                        ignored -> {
                            long parsed = parsePositiveLong(amount.get());
                            if (parsed > 0L) {
                                send(new C2S_SpecialOperationsBaseAction(
                                        C2S_SpecialOperationsBaseAction.Action.EXCHANGE,
                                        target, 0, 0, parsed, snapshot.warehousePage()));
                            }
                        })
                .bounds(panelLeft() + panelWidth() - buttonWidth - 12, controlY, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!embedded) {
            // 全屏遮罩必须在缩放之外绘制：GuiGraphics#fill 走 pose，但遮罩需要铺满真实屏幕
            graphics.fill(0, 0, width, height, 0xAA000000);
        }
        float factor = scaleFactor;
        ScreenScale.push(graphics, factor);
        int localMouseX = ScreenScale.toLocal(mouseX, factor);
        int localMouseY = ScreenScale.toLocal(mouseY, factor);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xEE10171C);
        graphics.fill(left, top, left + panelWidth(), top + 2, 0xFFE2B84B);
        graphics.drawString(font, fit(title.getString(), Math.max(40, panelWidth() - 20)), left + 10, top + 11, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.havoc_coins", snapshot.havocCoins()),
                compactLayout() ? left + 10 : left + panelWidth() - 150,
                compactLayout() ? top + 20 : top + 11, 0xFFE2B84B);
        switch (tab) {
            case FACILITIES -> renderFacilities(graphics);
            case SELL -> renderSell(graphics, localMouseX, localMouseY);
            case BUYBACK -> renderBuyback(graphics, localMouseX, localMouseY);
            case EXCHANGE -> renderExchange(graphics);
        }
        super.render(graphics, localMouseX, localMouseY, partialTick);
        if (tab == Tab.BUYBACK && buybackSearchBox != null && buybackSearchBox.getValue().isEmpty()
                && !buybackSearchBox.isFocused()) {
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.search"),
                    buybackSearchBox.getX() + 4, buybackSearchBox.getY() + 6, 0xFF777777);
        }
        renderItemTooltip(graphics, localMouseX, localMouseY);
        ScreenScale.pop(graphics);
    }

    private void renderFacilities(GuiGraphics graphics) {
        int x = panelLeft() + 14;
        int y = panelTop() + 64;
        int end = Math.min(snapshot.facilities().size(), scrollRow + visibleFacilityRows());
        for (int index = scrollRow; index < end; index++) {
            S2C_SpecialOperationsBaseSnapshot.FacilityView facility = snapshot.facilities().get(index);
            int buttonX = panelLeft() + panelWidth() - 88;
            graphics.fill(x - 4, y - 4, panelLeft() + panelWidth() - 12, y + 39, 0x88202B32);
            Component facilityName = Component.translatable("screen.dealt_force_skills.base.facility." + facility.id());
            graphics.drawString(font, fit(facilityName.getString(), Math.max(40, buttonX - x - 4)), x, y, 0xFFFFFF);
            // 仓库设施显示格数而不是等级
            if ("warehouse".equals(facility.id())) {
                int slots = snapshot.warehouseSlotsPerPage();
                graphics.drawString(font, Component.literal(slots + "格"), x, y + 13, 0xFFB8C2C8);
            } else {
                graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.level",
                        facility.level(), facility.maxLevel()), x, y + 13, 0xFFB8C2C8);
            }
            boolean maxed = facility.level() >= facility.maxLevel();
            if (!maxed) {
                int detailX = compactLayout() ? x + Math.min(80, Math.max(54, panelWidth() / 4)) : x + 170;
                boolean missingConditions = hasMissingConditions(facility);
                Component next = Component.translatable("screen.dealt_force_skills.base.next",
                        facility.nextLevel(), facility.nextCost());
                graphics.drawString(font, compactLayout()
                                ? fit(next.getString(), Math.max(28, buttonX - detailX - 4)) : next.getString(),
                        detailX, compactLayout() ? y + 13 : y,
                        missingConditions ? 0xFFCC7777 : 0xFFE2B84B);
                graphics.drawString(font, fit(facilityConditionSummary(facility).getString(), facilityConditionWidth()),
                        compactLayout() ? x : detailX, compactLayout() ? y + 26 : y + 13,
                        missingConditions ? 0xFFFF6B6B : 0xFF7FC9A4);
            }
            if (!compactLayout() || maxed) {
                String effect = facilityEffect(facility.id(), maxed ? facility.level() : facility.nextLevel()).getString();
                graphics.drawString(font, "training_center".equals(facility.id())
                                ? fit(effect, Math.max(1, panelWidth() - 28)) : effect,
                        x, y + 26, 0xFF7FC9A4);
            }
            y += 50;
        }
    }

    // 特勤处内部仓库已删除，移除 renderWarehouse/renderWarehouseScrollbar/addWarehouseButtons 方法

    private void renderSell(GuiGraphics graphics, int mouseX, int mouseY) {
        List<ItemStack> stacks = sellStacks();
        int left = panelLeft() + 14;
        int top = panelTop() + 88;
        int cols = Math.max(1, (panelWidth() - 28) / SLOT);
        int visibleRows = Math.max(1, (panelHeight() - 105) / SLOT);
        drawStackGrid(graphics, stacks, left, top, cols, visibleRows, scrollRow, mouseX, mouseY);
    }

    private void renderBuyback(GuiGraphics graphics, int mouseX, int mouseY) {
        List<S2C_SpecialOperationsBaseSnapshot.BuybackView> buyback = filteredBuyback();
        int left = panelLeft() + 14;
        int top = panelTop() + 84;
        int buttonX = panelLeft() + panelWidth() - 88;
        Component quantityLabel = Component.translatable("screen.dealt_force_skills.base.quantity");
        graphics.drawString(font, quantityLabel,
                panelLeft() + panelWidth() - 82 - font.width(quantityLabel), panelTop() + 63, 0xFFB8C2C8);
        int index = Math.min(scrollRow, Math.max(0, buyback.size() - visibleBuybackRows()));
        int visibleRows = visibleBuybackRows();
        for (int row = 0; row < visibleRows; row++) {
            if (index >= buyback.size()) {
                break;
            }
            S2C_SpecialOperationsBaseSnapshot.BuybackView view = buyback.get(index++);
            int y = top + row * BUYBACK_ROW_HEIGHT;
            ItemStack stack = view.stack();
            if (stack.isEmpty()) {
                continue;
            }
            graphics.fill(left, y, panelLeft() + panelWidth() - 14, y + 24, 0x88202B32);
            graphics.renderItem(stack, left + 4, y + 4);
            graphics.drawString(font, fit(stack.getHoverName().getString(), Math.max(40, buttonX - left - 34)),
                    left + 26, y + 3, 0xFFFFFF);
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.buyback.price", view.price()),
                    left + 26, y + 14, 0xFFE2B84B);
            boolean enabled = ModGameRules.isRebuyEnabled(Minecraft.getInstance().level);
            boolean hovered = enabled && inside(mouseX, mouseY, buttonX, y + 2, 74, 20);
            graphics.fill(buttonX, y + 2, buttonX + 74, y + 22,
                    enabled ? (hovered ? 0xFF4B6860 : 0xFF354A45) : 0xFF000000);
            graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.redeem"),
                    buttonX + 12, y + 8, enabled ? 0xFFFFFFFF : 0xFF606060);
        }
    }

    private void renderExchange(GuiGraphics graphics) {
        int x = panelLeft() + 24;
        int y = panelTop() + 91;
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.exchange.haff"), x, y, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.exchange.special"), x, y + 58, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.dealt_force_skills.base.exchange.rate"), x,
                compactLayout() ? panelTop() + panelHeight() - 16 : y + 126, 0xFFB8C2C8);
    }

    private void drawStackGrid(GuiGraphics graphics, List<ItemStack> stacks, int left, int top, int cols,
                               int visibleRows, int firstRow, int mouseX, int mouseY) {
        int start = firstRow * cols;
        int end = Math.min(stacks.size(), start + cols * visibleRows);
        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = left + local % cols * SLOT;
            int y = top + local / cols * SLOT;
            boolean hovered = inside(mouseX, mouseY, x, y, 18, 18);
            graphics.fill(x, y, x + 18, y + 18, hovered ? 0xAA3C4A51 : 0x88303A40);
            ItemStack stack = stacks.get(index);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 1, y + 1);
                graphics.renderItemDecorations(font, stack, x + 1, y + 1);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double localX = ScreenScale.toLocalCoordinate(mouseX, scaleFactor);
        double localY = ScreenScale.toLocalCoordinate(mouseY, scaleFactor);
        if (tab == Tab.SELL) {
            int cols = Math.max(1, (panelWidth() - 28) / SLOT);
            List<ItemStack> stacks = sellStacks();
            int index = gridIndex(localX, localY, panelLeft() + 14, panelTop() + 88, cols,
                    Math.max(1, (panelHeight() - 105) / SLOT), scrollRow, stacks.size());
            if (index >= 0 && !stacks.get(index).isEmpty()) {
                // 特勤处内部仓库已删除，WAREHOUSE 源不再支持
                send(new C2S_SpecialOperationsBaseAction(C2S_SpecialOperationsBaseAction.Action.SELL,
                        sellSource, index, button == 1 ? 1 : stacks.get(index).getCount(), 0L, snapshot.warehousePage()));
                return true;
            }
        } else if (tab == Tab.BUYBACK) {
            if (button == 0) {
                int row = (int) (localY - (panelTop() + 84)) / BUYBACK_ROW_HEIGHT;
                List<S2C_SpecialOperationsBaseSnapshot.BuybackView> buyback = filteredBuyback();
                int index = row + scrollRow;
                int buttonX = panelLeft() + panelWidth() - 88;
                if (ModGameRules.isRebuyEnabled(Minecraft.getInstance().level)
                        && row >= 0 && row < visibleBuybackRows() && index >= 0 && index < buyback.size()
                        && inside(localX, localY, buttonX,
                        panelTop() + 84 + row * BUYBACK_ROW_HEIGHT + 2, 74, 20)) {
                    int count = parseRedeemCount();
                    if (count > 0) {
                        send(new C2S_SpecialOperationsBaseAction(C2S_SpecialOperationsBaseAction.Action.REDEEM,
                                "", buyback.get(index).soldIndex(), count, 0L, snapshot.warehousePage()));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(localX, localY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int total = switch (tab) {
            case FACILITIES -> snapshot.facilities().size();
            case SELL -> sellStacks().size();
            case BUYBACK -> filteredBuyback().size();
            default -> 0;
        };
        int cols = tab == Tab.FACILITIES || tab == Tab.BUYBACK ? 1
                : Math.max(1, (panelWidth() - 28) / SLOT);
        int visible = tab == Tab.FACILITIES ? visibleFacilityRows() : tab == Tab.BUYBACK ? visibleBuybackRows()
                : Math.max(1, (panelHeight() - 105) / SLOT);
        int max = Math.max(0, (total + cols - 1) / cols - visible);
        if (max > 0) {
            scrollRow = Math.max(0, Math.min(max, scrollRow - (int) Math.signum(delta)));
            if (tab == Tab.FACILITIES) {
                refreshWidgets();
            }
            return true;
        }
        return super.mouseScrolled(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 特勤处内部仓库已删除，移除 draggingWarehouseScrollbar 逻辑
        return super.mouseDragged(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), button,
                ScreenScale.toLocalDelta(dragX, scaleFactor), ScreenScale.toLocalDelta(dragY, scaleFactor));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // 特勤处内部仓库已删除，移除 draggingWarehouseScrollbar 逻辑
        return super.mouseReleased(ScreenScale.toLocalCoordinate(mouseX, scaleFactor),
                ScreenScale.toLocalCoordinate(mouseY, scaleFactor), button);
    }

    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tab == Tab.FACILITIES) {
            S2C_SpecialOperationsBaseSnapshot.FacilityView facility = facilityConditionAt(mouseX, mouseY);
            if (facility != null && hasMissingConditions(facility)) {
                ScreenScale.renderTextTooltip(graphics, font, facilityConditionTooltip(facility),
                        mouseX, mouseY, localWidth(), localHeight());
                return;
            }
            int y = panelTop() + 64;
            int end = Math.min(snapshot.facilities().size(), scrollRow + visibleFacilityRows());
            for (int index = scrollRow; index < end; index++, y += 50) {
                var hovered = snapshot.facilities().get(index);
                if ("training_center".equals(hovered.id())
                        && inside(mouseX, mouseY, panelLeft() + 14, y, Math.max(1, panelWidth() - 112), 38)) {
                    int level = hovered.level() >= hovered.maxLevel() ? hovered.level() : hovered.nextLevel();
                    ScreenScale.renderTooltipLines(graphics, font,
                            font.split(facilityEffect(hovered.id(), level), Math.max(1, Math.min(300, localWidth() - 24))),
                            mouseX, mouseY, localWidth(), localHeight());
                    return;
                }
            }
        } else if (tab == Tab.SELL) {
            List<ItemStack> stacks = sellStacks();
            int cols = Math.max(1, (panelWidth() - 28) / SLOT);
            int index = gridIndex(mouseX, mouseY, panelLeft() + 14, panelTop() + 88, cols,
                    Math.max(1, (panelHeight() - 105) / SLOT), scrollRow, stacks.size());
            if (index >= 0 && !stacks.get(index).isEmpty()) {
                ScreenScale.renderItemTooltip(graphics, font, stacks.get(index), mouseX, mouseY,
                        localWidth(), localHeight());
            }
        } else if (tab == Tab.BUYBACK) {
            int row = (mouseY - (panelTop() + 84)) / BUYBACK_ROW_HEIGHT;
            List<S2C_SpecialOperationsBaseSnapshot.BuybackView> buyback = filteredBuyback();
            int index = row + scrollRow;
            if (row >= 0 && row < visibleBuybackRows() && index >= 0 && index < buyback.size()
                    && inside(mouseX, mouseY, panelLeft() + 18,
                    panelTop() + 84 + row * BUYBACK_ROW_HEIGHT + 3, 18, 18)) {
                ItemStack hovered = buyback.get(index).stack();
                if (!hovered.isEmpty()) {
                    ScreenScale.renderItemTooltip(graphics, font, hovered, mouseX, mouseY,
                            localWidth(), localHeight());
                }
            }
        }
    }

    private List<S2C_SpecialOperationsBaseSnapshot.BuybackView> filteredBuyback() {
        return snapshot.buyback().stream().filter(view -> {
            ItemStack stack = view.stack();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return !stack.isEmpty() && JustEnoughCharactersCompat.matches(
                    stack.getHoverName().getString() + " " + (id == null ? "" : id), buybackSearch);
        }).toList();
    }

    private int parseRedeemCount() {
        try {
            int value = Integer.parseInt(buybackCount);
            return value >= 1 && value <= MAX_REDEEM_COUNT ? value : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private boolean validRedeemCountText(String value) {
        return value.isEmpty() || value.chars().allMatch(Character::isDigit)
                && value.length() <= 4 && parseBoundedInt(value, MAX_REDEEM_COUNT);
    }

    private boolean validLongAmountText(String value) {
        return value.isEmpty() || value.chars().allMatch(Character::isDigit) && parsePositiveLong(value) > 0L;
    }

    private boolean parseBoundedInt(String value, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 1 && parsed <= max;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0L ? parsed : -1L;
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private List<ItemStack> sellStacks() {
        // 特勤处内部仓库已删除，只保留 INVENTORY 和 CONTAINER
        return switch (sellSource) {
            case "CONTAINER" -> snapshot.container();
            default -> snapshot.inventory();
        };
    }

    private int gridIndex(double mouseX, double mouseY, int left, int top, int cols, int visibleRows,
                          int firstRow, int size) {
        if (!inside(mouseX, mouseY, left, top, cols * SLOT, visibleRows * SLOT)) {
            return -1;
        }
        int col = (int) (mouseX - left) / SLOT;
        int row = (int) (mouseY - top) / SLOT;
        int index = (row + firstRow) * cols + col;
        return index >= 0 && index < size ? index : -1;
    }

    private void requestPage(int page) {
        scrollRow = 0;
        send(C2S_SpecialOperationsBaseAction.page(page));
    }

    private void send(C2S_SpecialOperationsBaseAction action) {
        NetworkHandler.sendToServer(action);
    }

    private Item item(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText);
        return id == null ? null : ForgeRegistries.ITEMS.getValue(id);
    }

    private boolean hasMissingConditions(S2C_SpecialOperationsBaseSnapshot.FacilityView facility) {
        return facility.materials().stream().anyMatch(material -> material.missingCount() > 0)
                || !facility.allowed() && facility.requiredWarehouseLevel() > 0;
    }

    private Component facilityConditionSummary(S2C_SpecialOperationsBaseSnapshot.FacilityView facility) {
        List<String> missing = new ArrayList<>();
        for (S2C_SpecialOperationsBaseSnapshot.MaterialView material : facility.materials()) {
            Item item = item(material.itemId());
            if (item != null && material.missingCount() > 0) {
                missing.add(item.getDescription().getString() + "x" + material.missingCount());
            }
        }
        if (!facility.allowed() && facility.requiredWarehouseLevel() > 0) {
            missing.add(Component.translatable("screen.dealt_force_skills.base.requires_warehouse_level",
                    facility.requiredWarehouseLevel()).getString());
        }
        return missing.isEmpty()
                ? Component.translatable("screen.dealt_force_skills.base.conditions_met")
                : Component.translatable("screen.dealt_force_skills.base.missing_summary", String.join("  ", missing));
    }

    private List<Component> facilityConditionTooltip(S2C_SpecialOperationsBaseSnapshot.FacilityView facility) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.dealt_force_skills.base.missing_conditions"));
        for (S2C_SpecialOperationsBaseSnapshot.MaterialView material : facility.materials()) {
            Item item = item(material.itemId());
            if (item != null && material.missingCount() > 0) {
                lines.add(Component.literal(material.missingCount() + "x ").append(item.getDescription())
                        .withStyle(ChatFormatting.RED));
            }
        }
        if (!facility.allowed() && facility.requiredWarehouseLevel() > 0) {
            lines.add(Component.translatable("screen.dealt_force_skills.base.requires_warehouse_level",
                    facility.requiredWarehouseLevel()).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private S2C_SpecialOperationsBaseSnapshot.FacilityView facilityConditionAt(double mouseX, double mouseY) {
        int x = compactLayout() ? panelLeft() + 14 : panelLeft() + 184;
        int y = panelTop() + 64;
        int end = Math.min(snapshot.facilities().size(), scrollRow + visibleFacilityRows());
        for (int index = scrollRow; index < end; index++) {
            S2C_SpecialOperationsBaseSnapshot.FacilityView facility = snapshot.facilities().get(index);
            if (facility.level() < facility.maxLevel()
                    && inside(mouseX, mouseY, x, y + (compactLayout() ? 26 : 11), facilityConditionWidth(), 12)) {
                return facility;
            }
            y += 50;
        }
        return null;
    }

    private int facilityConditionWidth() {
        return Math.max(1, panelWidth() - (compactLayout() ? 110 : 290));
    }

    private String fit(String value, int maxWidth) {
        return font.width(value) <= maxWidth ? value
                : font.plainSubstrByWidth(value, Math.max(8, maxWidth - font.width("..."))) + "...";
    }

    private Component facilityEffect(String id, int level) {
        return switch (id) {
            case "command_center" -> Component.translatable("screen.dealt_force_skills.base.effect.command_center",
                    level * 20, level * 10);
            case "shooting_range" -> Component.translatable("screen.dealt_force_skills.base.effect.shooting_range",
                    level * 3, level * 5);
            case "training_center" -> Component.translatable("screen.dealt_force_skills.base.effect.training_center",
                    level * 7, level * 3, level * 5);
            case "diving_center" -> Component.translatable("screen.dealt_force_skills.base.effect.diving_center",
                    level * 30, level * 25, level * 15);
            case "decontamination_room" -> Component.translatable("screen.dealt_force_skills.base.effect.decontamination_room",
                    level * 25, level, level * 30, level >= 3 ? 1 : 0);
            case "warehouse" -> Component.translatable("screen.dealt_force_skills.base.effect.warehouse",
                    level <= 0 ? 0 : new int[]{0, 27, 54, 108, 216, 312, 468, 624, 1060, 1440, 1960}[level]);
            default -> Component.empty();
        };
    }

    private int panelWidth() {
        return Math.max(1, Math.min(720, localWidth() - 20));
    }

    private int panelHeight() {
        // 嵌入模式：面板避开主机面顶部标签栏与底部底栏
        return embedded ? Math.max(1, Math.min(430, localHeight() - 68))
                        : Math.max(1, Math.min(430, localHeight() - 20));
    }

    private int panelLeft() {
        return (localWidth() - panelWidth()) / 2;
    }

    private int panelTop() {
        return embedded ? 42 : (localHeight() - panelHeight()) / 2;
    }

    /** 虚拟布局宽度：独立打开时为真实 GUI 宽 / 缩放系数，嵌入模式等于真实 GUI 宽 */
    private int localWidth() {
        return ScreenScale.localWidth(width, scaleFactor);
    }

    /** 虚拟布局高度：独立打开时为真实 GUI 高 / 缩放系数，嵌入模式等于真实 GUI 高 */
    private int localHeight() {
        return ScreenScale.localHeight(height, scaleFactor);
    }

    private boolean compactLayout() {
        return panelWidth() < 620;
    }

    private int visibleFacilityRows() {
        return Math.max(1, (panelHeight() - 68) / 50);
    }

    private int visibleBuybackRows() {
        return Math.max(1, (panelHeight() - 94) / BUYBACK_ROW_HEIGHT);
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        super.onClose();
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
