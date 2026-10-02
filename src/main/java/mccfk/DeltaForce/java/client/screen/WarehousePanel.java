package mccfk.DeltaForce.java.client.screen;

import mccfk.DeltaForce.java.network.C2S_DfWarehouseAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 仓库页（DFGUI_MAIN 仓库标签页）
 * 左侧占半屏：盔甲区（头/甲、裤/鞋、副手/主手，格子放大）+ 下方物品栏（9列×4行，格子样式搬自特勤处）
 * 右侧占半屏：仓库网格（8列×15行）+ 最右滑动条
 * 交互：
 *   - 长按并拖动物品（物品栏↔仓库）到对方区域释放 → 移动整堆物品
 *   - 短按格子 → 变白闪一下
 *   - 滚轮滚动网格，滑动条可拖拽
 * 悬停格子显示物品 tooltip。
 */
public class WarehousePanel {

    private static final int SLOT = 20;                    // 物品栏/盔甲格子间距（仓库格子放大，见 gridSlot）
    private static final int GRID_INNER = 18;              // 物品栏格子内部绘制尺寸
    private static final int SCROLLBAR_W = 4;              // 滑动条宽度
    private static final int BAR_PADDING = 6;              // 滑动条与网格间距

    private static final int WIDE_ARMOR_SLOT = 40;
    private static final int WIDE_ARMOR_COLS = 2;

    private static final int INV_COLS = 9;                 // 物品栏列数
    private static final int INV_MAX_ROWS = 4;

    private static final int MIN_GRID_COLS = 8;            // 仓库网格最小列数
    private static final int GRID_ROWS = 15;               // 仓库网格行数（8×15=120）

    // 拖拽触发阈值
    private static final double DRAG_DISTANCE_SQ = 7.0 * 7.0;   // 移动超过7px
    private static final long DRAG_TIME_MS = 300;                // 或按住超过300ms
    private static final long DOUBLE_CLICK_MS = 250;

    private static final int SOURCE_INVENTORY = 0;
    private static final int SOURCE_WAREHOUSE = 1;
    private static final int SOURCE_ARMOR = 2;

    // 滚动状态
    private static int warehouseScrollRow = 0;             // 仓库滚动行
    private static boolean draggingWarehouseBar = false;   // 是否拖动仓库滑动条
    private static int itemScrollRow = 0;                  // 物品栏滚动行
    private static boolean draggingItemBar = false;        // 是否拖动物品栏滑动条

    // 按压/拖拽状态
    private static boolean pressing = false;
    private static int pressButton = -1;
    private static double pressX = 0, pressY = 0;
    private static long pressTime = 0;
    private static boolean pressOnSlot = false;
    private static int pressSource = -1;
    private static int pressIndex = -1;
    private static int lastClickSource = -1;
    private static int lastClickIndex = -1;
    private static long lastClickTime = 0L;
    private static boolean shiftSweep = false;
    private static final Set<Integer> SHIFT_SWEEP_SLOTS = new HashSet<>();

    private static boolean dragging = false;
    private static ItemStack dragStack = ItemStack.EMPTY;
    private static int dragSource = -1;
    private static int dragIndex = -1;

    // 短按闪白状态
    private static int flashSource = -1;
    private static int flashIndex = -1;
    private static long flashTime = 0;
    private static final long FLASH_DURATION_MS = 400;

    // 当前数据（render 时刷新，交互复用）
    private static int warehouseDataSize = 0;
    private static List<ItemStack> currentWarehouse = List.of();

    // 当前布局坐标（render 时计算，交互复用）
    private static int armorX, armorY;
    private static int invX, invY;
    private static int itemBarX;
    private static int gridX, gridY;
    private static int gridBarX;
    private static int gridRows;
    private static int gridCols;
    private static int gridSlot;                            // 仓库格子间距（放大铺满右半屏，layout 计算）
    private static int gridInner;                           // 仓库格子内部绘制尺寸
    private static int invSlot;                             // 物品栏格子间距（放大占左半屏，layout 计算）
    private static int invInner;                            // 物品栏格子内部绘制尺寸
    private static int topY;
    private static int armorSlot;
    private static int armorInner;
    private static int armorCols;
    private static int armorRows;
    private static int armorRowStride;
    private static int armorColumnStride;
    private static int invRows;
    private static boolean compactLayout;

    public static void resetInteractionState() {
        warehouseScrollRow = 0;
        itemScrollRow = 0;
        draggingWarehouseBar = false;
        draggingItemBar = false;
        pressing = false;
        pressButton = -1;
        pressOnSlot = false;
        pressSource = -1;
        pressIndex = -1;
        lastClickSource = -1;
        lastClickIndex = -1;
        lastClickTime = 0L;
        shiftSweep = false;
        SHIFT_SWEEP_SLOTS.clear();
        dragging = false;
        dragStack = ItemStack.EMPTY;
        dragSource = -1;
        dragIndex = -1;
        flashSource = -1;
        flashIndex = -1;
        warehouseDataSize = 0;
        currentWarehouse = List.of();
    }

    private WarehousePanel() {
    }

    // ========== 渲染 ==========

    public static void render(GuiGraphics g, int mouseX, int mouseY, int screenWidth, int screenHeight,
                              List<ItemStack> warehouseItems, int totalSlots, int page, String container) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) {
            return;
        }
        var font = mc.font;
        warehouseDataSize = warehouseItems.size();
        currentWarehouse = warehouseItems;

        layout(screenWidth, screenHeight);

        // --- 左半屏：盔甲区（格子放大）---
        drawSectionLabel(g, font, Component.translatable("warehouse.deltaforce.armor"), armorX, topY - 12);
        ItemStack head = player.getInventory().getArmor(3);
        ItemStack chest = player.getInventory().getArmor(2);
        ItemStack legs = player.getInventory().getArmor(1);
        ItemStack feet = player.getInventory().getArmor(0);
        ItemStack offhand = player.getOffhandItem();
        ItemStack mainhand = player.getMainHandItem();
        ItemStack[] slots = {head, chest, legs, feet, offhand, mainhand};
        String[] slotKeys = {
                "warehouse.deltaforce.slot.head", "warehouse.deltaforce.slot.chest",
                "warehouse.deltaforce.slot.legs", "warehouse.deltaforce.slot.feet",
                "warehouse.deltaforce.slot.offhand", "warehouse.deltaforce.slot.mainhand"
        };
        for (int i = 0; i < slots.length; i++) {
            int row = i / armorCols;
            int col = i % armorCols;
            int x = armorX + col * armorColumnStride;
            int y = armorY + row * armorRowStride;
            drawBigSlot(g, font, x, y, slots[i], mouseX, mouseY);
            if (!compactLayout) {
                String label = Component.translatable(slotKeys[i]).getString();
                int labelW = font.width(label);
                g.drawString(font, label, x + (armorSlot - labelW) / 2,
                        y + armorSlot + 2, 0xFFB8C2C8, false);
            }
        }

        // --- 左半屏：物品栏（盔甲区下方）---
        int invLabelY = armorY + armorRows * armorRowStride + (compactLayout ? 4 : 8);
        drawSectionLabel(g, font, Component.translatable("warehouse.deltaforce.inventory"), invX, invLabelY);
        invY = invLabelY + 12;
        int itemMax = itemMaxScroll(player.getInventory().items.size());
        if (itemScrollRow > itemMax) {
            itemScrollRow = itemMax;
        }
        if (itemScrollRow < 0) {
            itemScrollRow = 0;
        }
        drawStackGrid(g, font, player.getInventory().items, invX, invY, INV_COLS, invRows, itemScrollRow,
                SOURCE_INVENTORY, mouseX, mouseY, invSlot, invInner);
        drawScrollbar(g, itemBarX, invY, invRows * invSlot, invRows, itemScrollRow, itemMax);

        // --- 右半屏：仓库网格（标题含页码/页容器/页总格数）---
        drawSectionLabel(g, font, warehouseTitle(page, container, totalSlots), gridX, topY - 12);
        int gridMax = gridMaxScroll(warehouseItems.size());
        if (warehouseScrollRow > gridMax) {
            warehouseScrollRow = gridMax;
        }
        if (warehouseScrollRow < 0) {
            warehouseScrollRow = 0;
        }
        drawStackGrid(g, font, warehouseItems, gridX, gridY, gridCols, gridRows, warehouseScrollRow,
                SOURCE_WAREHOUSE, mouseX, mouseY, gridSlot, gridInner);
        drawScrollbar(g, gridBarX, gridY, gridRows * gridSlot, gridRows, warehouseScrollRow, gridMax);

        // --- 短按闪白效果 ---
        renderFlash(g, font);

        // --- 拖拽中的物品图标跟随鼠标 ---
        if (dragging && !dragStack.isEmpty()) {
            g.renderItem(dragStack, mouseX - 8, mouseY - 8);
            g.renderItemDecorations(font, dragStack, mouseX - 8, mouseY - 8);
            // 拖拽目标高亮：盔甲槽 / 仓库格 / 物品栏格
            int armorSlot = equipSlotAt(mouseX, mouseY);
            if (armorSlot >= 0) {
                int row = armorSlot / armorCols;
                int col = armorSlot % armorCols;
                int x = armorX + col * armorColumnStride;
                int y = armorY + row * armorRowStride;
                g.fill(x, y, x + armorInner, y + armorInner, 0x55FFFFFF);
            }
            int wh = gridIndexAt(mouseX, mouseY, gridX, gridY, gridCols, gridRows,
                    warehouseScrollRow, warehouseDataSize, gridSlot, gridInner);
            if (wh >= 0) {
                int row = wh / gridCols - warehouseScrollRow;
                int col = wh % gridCols;
                if (row >= 0 && row < gridRows) {
                    int x = gridX + col * gridSlot;
                    int y = gridY + row * gridSlot;
                    g.fill(x, y, x + gridInner, y + gridInner, 0x55FFFFFF);
                }
            }
            int inv = gridIndexAt(mouseX, mouseY, invX, invY, INV_COLS, invRows,
                    itemScrollRow, inventorySize(), invSlot, invInner);
            if (inv >= 0) {
                int row = inv / INV_COLS - itemScrollRow;
                int col = inv % INV_COLS;
                if (row >= 0 && row < invRows) {
                    int x = invX + col * invSlot;
                    int y = invY + row * invSlot;
                    g.fill(x, y, x + invInner, y + invInner, 0x55FFFFFF);
                }
            }
        }

        // --- 悬停物品提示（拖拽时不显示）---
        if (!dragging) {
            ItemStack hovered = hoveredStackAt(mouseX, mouseY, player.getInventory().items, warehouseItems);
            if (!hovered.isEmpty()) {
                g.renderTooltip(font, hovered, mouseX, mouseY);
            }
        }
    }

    private static void layout(int screenWidth, int screenHeight) {
        compactLayout = screenHeight < 360 || screenWidth < 620;
        topY = compactLayout ? 70 : 60;
        int leftX = compactLayout ? 8 : 30;
        armorSlot = compactLayout ? 20 : WIDE_ARMOR_SLOT;
        armorInner = armorSlot - 2;
        armorCols = WIDE_ARMOR_COLS;
        armorRows = (6 + armorCols - 1) / armorCols;
        armorRowStride = armorSlot + (compactLayout ? 2 : 10);
        armorColumnStride = armorSlot + (compactLayout ? 4 : 8);
        armorX = leftX;
        armorY = topY;
        invX = leftX;
        // 物品栏格子放大占左半屏宽度（左侧整体左移，右侧留出约两条滑动线宽度的空隙）
        int barSpace = SCROLLBAR_W + BAR_PADDING;
        int gap = 2 * (SCROLLBAR_W + BAR_PADDING);
        int leftAreaW = screenWidth / 2 - gap;
        invSlot = Math.max(16, (leftAreaW - leftX - barSpace) / INV_COLS);
        invInner = invSlot - 2;
        int invTop = armorY + armorRows * armorRowStride + (compactLayout ? 16 : 20);
        int contentBottom = Math.max(invTop + invSlot, screenHeight - 24);
        invRows = Math.max(1, Math.min(INV_MAX_ROWS, (contentBottom - invTop) / invSlot));
        itemBarX = invX + INV_COLS * invSlot + BAR_PADDING;
        // 仓库：8 列格子放大，右对齐铺满右半屏（从右缘向左渲染）
        gridCols = MIN_GRID_COLS;
        int halfX = screenWidth / 2;
        int rightMargin = 8;
        int availW = (screenWidth - rightMargin - barSpace) - halfX;
        gridSlot = Math.max(16, availW / gridCols);
        gridInner = gridSlot - 2;
        gridX = (screenWidth - rightMargin - barSpace) - gridCols * gridSlot;
        gridY = topY;
        gridBarX = gridX + gridCols * gridSlot + BAR_PADDING;
        gridRows = Math.min(GRID_ROWS, Math.max(1, (screenHeight - topY - 24) / gridSlot));
    }

    private static void drawSectionLabel(GuiGraphics g, Font font, Component label, int x, int y) {
        g.drawString(font, label, x, y, 0xFFB8C2C8, false);
    }

    private static Component warehouseTitle(int page, String container, int totalSlots) {
        return Component.translatable("warehouse.deltaforce.grid")
                .append(" · " + container + " · ")
                .append(Component.translatable("warehouse.deltaforce.page", page))
                .append(" (" + totalSlots + ")");
    }

    private static void drawBigSlot(GuiGraphics g, Font font, int x, int y, ItemStack stack, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, armorInner, armorInner);
        g.fill(x, y, x + armorInner, y + armorInner, hovered ? 0xAA3C4A51 : 0x88303A40);
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 1, y + 1);
            g.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    /**
     * 绘制一格一格样式的物品网格（搬自特勤处 drawStackGrid），附带短按闪白效果
     */
    private static void drawStackGrid(GuiGraphics g, Font font, List<ItemStack> stacks, int left, int top, int cols,
                                      int visibleRows, int firstRow, int source, int mouseX, int mouseY,
                                      int slot, int inner) {
        float scale = (float) inner / 18.0F;
        int start = firstRow * cols;
        int end = Math.min(stacks.size(), start + cols * visibleRows);
        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = left + local % cols * slot;
            int y = top + local / cols * slot;
            boolean hovered = inside(mouseX, mouseY, x, y, inner, inner);
            g.fill(x, y, x + inner, y + inner, hovered ? 0xAA3C4A51 : 0x88303A40);
            ItemStack stack = stacks.get(index);
            if (!stack.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x, y, 0);
                g.pose().scale(scale, scale, 1.0F);
                g.renderItem(stack, 1, 1);
                g.renderItemDecorations(font, stack, 1, 1);
                g.pose().popPose();
            }
            // 短按闪白
            if (flashSource == source && flashIndex == index) {
                g.fill(x, y, x + inner, y + inner, flashColor());
            }
        }
    }

    /**
     * 纵向滑动条：轨道 + 滑块。无滚动量时滑块占满整条轨道。
     */
    private static void drawScrollbar(GuiGraphics g, int x, int y, int trackH, int visibleRows,
                                      int scrollRow, int maxScroll) {
        g.fill(x, y, x + SCROLLBAR_W, y + trackH, 0xFF263139);
        if (maxScroll <= 0) {
            g.fill(x - 1, y, x + SCROLLBAR_W + 1, y + trackH, 0xFF8FA4AE);
            return;
        }
        int thumbH = Math.max(12, Math.round(trackH * (float) visibleRows / (visibleRows + maxScroll)));
        int travel = Math.max(1, trackH - thumbH);
        int thumbY = y + Math.round((float) scrollRow / maxScroll * travel);
        g.fill(x - 1, thumbY, x + SCROLLBAR_W + 1, thumbY + thumbH, 0xFF8FA4AE);
    }

    // ========== 短按闪白 ==========

    private static int flashColor() {
        long elapsed = System.currentTimeMillis() - flashTime;
        float remain = 1.0F - Math.max(0F, Math.min(1F, (float) elapsed / FLASH_DURATION_MS));
        int alpha = (int) (0x80 * remain);
        return (alpha << 24) | 0xFFFFFF;
    }

    private static void renderFlash(GuiGraphics g, Font font) {
        long elapsed = System.currentTimeMillis() - flashTime;
        if (flashIndex < 0 || elapsed > FLASH_DURATION_MS) {
            flashSource = -1;
            flashIndex = -1;
            return;
        }
        int color = flashColor();
        if (flashSource == SOURCE_INVENTORY) {
            int row = flashIndex / INV_COLS;
            int col = flashIndex % INV_COLS;
            if (row >= 0 && row < invRows) {
                int x = invX + col * invSlot;
                int y = invY + row * invSlot;
                g.fill(x, y, x + invInner, y + invInner, color);
            }
        } else if (flashSource == SOURCE_WAREHOUSE) {
            int row = flashIndex / gridCols - warehouseScrollRow;
            int col = flashIndex % gridCols;
            if (row >= 0 && row < gridRows) {
                int x = gridX + col * gridSlot;
                int y = gridY + row * gridSlot;
                g.fill(x, y, x + gridInner, y + gridInner, color);
            }
        } else if (flashSource == SOURCE_ARMOR) {
            int row = flashIndex / armorCols;
            int col = flashIndex % armorCols;
            int x = armorX + col * armorColumnStride;
            int y = armorY + row * armorRowStride;
            g.fill(x, y, x + armorInner, y + armorInner, color);
        }
    }

    // ========== 悬停 tooltip ==========

    private static ItemStack hoveredStackAt(int mouseX, int mouseY, List<ItemStack> invItems, List<ItemStack> warehouseItems) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            ItemStack[] slots = {
                    player.getInventory().getArmor(3), player.getInventory().getArmor(2),
                    player.getInventory().getArmor(1), player.getInventory().getArmor(0),
                    player.getOffhandItem(), player.getMainHandItem()
            };
            for (int i = 0; i < slots.length; i++) {
                int row = i / armorCols;
                int col = i % armorCols;
                int x = armorX + col * armorColumnStride;
                int y = armorY + row * armorRowStride;
                if (!slots[i].isEmpty() && inside(mouseX, mouseY, x, y, armorInner, armorInner)) {
                    return slots[i];
                }
            }
        }
        ItemStack inv = gridStackAt(mouseX, mouseY, invX, invY, INV_COLS, invRows, itemScrollRow, invItems, invSlot, invInner);
        if (!inv.isEmpty()) {
            return inv;
        }
        return gridStackAt(mouseX, mouseY, gridX, gridY, gridCols, gridRows, warehouseScrollRow, warehouseItems, gridSlot, gridInner);
    }

    private static ItemStack gridStackAt(int mouseX, int mouseY, int left, int top, int cols,
                                         int visibleRows, int firstRow, List<ItemStack> stacks,
                                         int slot, int inner) {
        int start = firstRow * cols;
        int end = Math.min(stacks.size(), start + cols * visibleRows);
        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = left + local % cols * slot;
            int y = top + local / cols * slot;
            if (inside(mouseX, mouseY, x, y, inner, inner)) {
                return stacks.get(index);
            }
        }
        return ItemStack.EMPTY;
    }

    // ========== 交互 ==========

    /**
     * 鼠标按下：记录起始格子与时间，用于区分短按/长按拖拽；点击滑动条启动拖动。
     */
    public static boolean handleMousePressed(int mouseX, int mouseY, int screenWidth, int screenHeight, int button) {
        layout(screenWidth, screenHeight);
        var player = Minecraft.getInstance().player;
        pressing = true;
        pressButton = button;
        pressX = mouseX;
        pressY = mouseY;
        pressTime = System.currentTimeMillis();
        pressOnSlot = false;
        pressSource = -1;
        pressIndex = -1;
        dragging = false;
        if (player == null) {
            return false;
        }
        if (button == 0) {
            // 盔甲槽（头/甲/裤/鞋/副手/主手）
            int armorSlot = equipSlotAt(mouseX, mouseY);
            if (armorSlot >= 0 && !armorStackAt(armorSlot).isEmpty()) {
                pressSource = SOURCE_ARMOR;
                pressIndex = armorSlot;
                pressOnSlot = true;
                return true;
            }
            // 物品栏格子
            int inv = gridIndexAt(mouseX, mouseY, invX, invY, INV_COLS, invRows, itemScrollRow,
                    player.getInventory().items.size(), invSlot, invInner);
            if (inv >= 0 && !player.getInventory().getItem(inv).isEmpty()) {
                long now = System.currentTimeMillis();
                if (!Screen.hasShiftDown() && lastClickSource == SOURCE_INVENTORY
                        && inv == lastClickIndex && now - lastClickTime <= DOUBLE_CLICK_MS) {
                    sendAutoDeposit(inv);
                    lastClickSource = -1;
                    lastClickIndex = -1;
                    lastClickTime = 0L;
                    pressing = false;
                    return true;
                }
                lastClickSource = Screen.hasShiftDown() ? -1 : SOURCE_INVENTORY;
                lastClickIndex = Screen.hasShiftDown() ? -1 : inv;
                lastClickTime = Screen.hasShiftDown() ? 0L : now;
                pressSource = SOURCE_INVENTORY;
                pressIndex = inv;
                pressOnSlot = true;
                return true;
            }
            // 仓库格子
            int wh = gridIndexAt(mouseX, mouseY, gridX, gridY, gridCols, gridRows,
                    warehouseScrollRow, warehouseDataSize, gridSlot, gridInner);
            if (wh >= 0 && wh < currentWarehouse.size() && !currentWarehouse.get(wh).isEmpty()) {
                long now = System.currentTimeMillis();
                if (!Screen.hasShiftDown() && lastClickSource == SOURCE_WAREHOUSE
                        && wh == lastClickIndex && now - lastClickTime <= DOUBLE_CLICK_MS) {
                    sendAutoWithdraw(wh);
                    lastClickSource = -1;
                    lastClickIndex = -1;
                    lastClickTime = 0L;
                    pressing = false;
                    return true;
                }
                lastClickSource = Screen.hasShiftDown() ? -1 : SOURCE_WAREHOUSE;
                lastClickIndex = Screen.hasShiftDown() ? -1 : wh;
                lastClickTime = Screen.hasShiftDown() ? 0L : now;
                pressSource = SOURCE_WAREHOUSE;
                pressIndex = wh;
                pressOnSlot = true;
                return true;
            }
            // 滑动条
            int itemMax = itemMaxScroll(inventorySize());
            if (itemMax > 0 && inside(mouseX, mouseY, itemBarX - 4, invY, SCROLLBAR_W + 10, invRows * invSlot)) {
                draggingItemBar = true;
                setItemScrollFromMouse(mouseY, itemMax);
                return true;
            }
            int gridMax = gridMaxScroll(warehouseDataSize);
            if (gridMax > 0 && inside(mouseX, mouseY, gridBarX - 4, gridY, SCROLLBAR_W + 10, gridRows * gridSlot)) {
                draggingWarehouseBar = true;
                setWarehouseScrollFromMouse(mouseY, gridMax);
                return true;
            }
        }
        // 未命中任何面板内容（顶部标签/底栏按钮等），不拦截，交由主界面处理标签切换
        pressing = false;
        return false;
    }

    /**
     * 鼠标拖动：长按/移动距离足够则抓起物品进入拖拽状态；滑动条跟随。
     */
    public static void handleMouseDragged(int mouseX, int mouseY) {
        if (draggingWarehouseBar) {
            setWarehouseScrollFromMouse(mouseY, gridMaxScroll(warehouseDataSize));
            return;
        }
        if (draggingItemBar) {
            setItemScrollFromMouse(mouseY, itemMaxScroll(inventorySize()));
            return;
        }
        if (pressing && pressOnSlot
                && (pressSource == SOURCE_INVENTORY || pressSource == SOURCE_WAREHOUSE)
                && (shiftSweep || Screen.hasShiftDown())) {
            shiftSweep = true;
            if (pressSource == SOURCE_INVENTORY) {
                depositSweepSlot(pressIndex);
                int hovered = gridIndexAt(mouseX, mouseY, invX, invY, INV_COLS, invRows,
                        itemScrollRow, inventorySize(), invSlot, invInner);
                depositSweepSlot(hovered);
            } else {
                withdrawSweepSlot(pressIndex);
                int hovered = gridIndexAt(mouseX, mouseY, gridX, gridY, gridCols, gridRows,
                        warehouseScrollRow, warehouseDataSize, gridSlot, gridInner);
                withdrawSweepSlot(hovered);
            }
            return;
        }
        if (!pressing || !pressOnSlot || dragging) {
            return;
        }
        double dx = mouseX - pressX;
        double dy = mouseY - pressY;
        boolean movedEnough = dx * dx + dy * dy > DRAG_DISTANCE_SQ;
        boolean heldLong = System.currentTimeMillis() - pressTime > DRAG_TIME_MS;
        if (movedEnough || heldLong) {
            // 抓起物品
            dragging = true;
            dragSource = pressSource;
            dragIndex = pressIndex;
            dragStack = stackAt(pressSource, pressIndex).copy();
        }
    }

    /**
     * 鼠标释放：拖拽中释放到对方区域则移动整堆；短按触发闪白。
     */
    public static void handleMouseReleased(int mouseX, int mouseY, int button) {
        if (shiftSweep && button == 0) {
            shiftSweep = false;
            SHIFT_SWEEP_SLOTS.clear();
        } else if (dragging && button == 0) {
            dragging = false;
            if (!dragStack.isEmpty()) {
                int armorSlot = equipSlotAt(mouseX, mouseY);
                int wh = gridIndexAt(mouseX, mouseY, gridX, gridY, gridCols, gridRows,
                        warehouseScrollRow, warehouseDataSize, gridSlot, gridInner);
                int inv = gridIndexAt(mouseX, mouseY, invX, invY, INV_COLS, invRows,
                        itemScrollRow, inventorySize(), invSlot, invInner);
                switch (dragSource) {
                    case SOURCE_INVENTORY -> {
                        // 物品栏 → 装备槽 / 仓库指定格 / 物品栏另一格
                        if (armorSlot >= 0) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.EQUIP, dragIndex, armorSlot, dragStack.getCount()));
                        } else if (wh >= 0 && wh != dragIndex) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.DEPOSIT, dragIndex, wh, dragStack.getCount()));
                        } else if (inv >= 0 && inv != dragIndex) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.MOVE_INV, dragIndex, inv, dragStack.getCount()));
                        }
                    }
                    case SOURCE_WAREHOUSE -> {
                        // 仓库 → 装备槽 / 物品栏指定格 / 仓库另一格
                        if (armorSlot >= 0) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.EQUIP_WH, dragIndex, armorSlot, dragStack.getCount()));
                        } else if (inv >= 0 && inv != dragIndex) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.WITHDRAW, dragIndex, inv, dragStack.getCount()));
                        } else if (wh >= 0 && wh != dragIndex) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.MOVE_WH, dragIndex, wh, dragStack.getCount()));
                        }
                    }
                    case SOURCE_ARMOR -> {
                        // 装备 → 物品栏指定格 或 仓库指定格
                        if (inv >= 0) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.UNEQUIP, dragIndex, inv, 0));
                        } else if (wh >= 0) {
                            PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                                    C2S_DfWarehouseAction.Action.UNEQUIP_WH, dragIndex, wh, 0));
                        }
                    }
                }
            }
            dragStack = ItemStack.EMPTY;
        } else if (pressing && pressOnSlot && button == pressButton) {
            // 短按：变白闪一下
            flashSource = pressSource;
            flashIndex = pressIndex;
            flashTime = System.currentTimeMillis();
        }
        pressing = false;
        dragSource = -1;
        dragIndex = -1;
        draggingWarehouseBar = false;
        draggingItemBar = false;
    }

    private static void depositSweepSlot(int inventorySlot) {
        if (inventorySlot >= 0 && SHIFT_SWEEP_SLOTS.add(inventorySlot)) {
            sendAutoDeposit(inventorySlot);
        }
    }

    private static void sendAutoDeposit(int inventorySlot) {
        ItemStack stack = stackAt(SOURCE_INVENTORY, inventorySlot);
        if (stack.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                C2S_DfWarehouseAction.Action.DEPOSIT_AUTO, inventorySlot, -1, stack.getCount()));
        flashSource = SOURCE_INVENTORY;
        flashIndex = inventorySlot;
        flashTime = System.currentTimeMillis();
    }

    private static void withdrawSweepSlot(int warehouseSlot) {
        if (warehouseSlot >= 0 && SHIFT_SWEEP_SLOTS.add(warehouseSlot)) {
            sendAutoWithdraw(warehouseSlot);
        }
    }

    private static void sendAutoWithdraw(int warehouseSlot) {
        ItemStack stack = stackAt(SOURCE_WAREHOUSE, warehouseSlot);
        if (stack.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new C2S_DfWarehouseAction(
                C2S_DfWarehouseAction.Action.WITHDRAW_AUTO, warehouseSlot, -1, stack.getCount()));
        flashSource = SOURCE_WAREHOUSE;
        flashIndex = warehouseSlot;
        flashTime = System.currentTimeMillis();
    }

    public static void handleMouseScrolled(double mouseX, double mouseY, double scrollY,
                                           int screenWidth, int screenHeight) {
        layout(screenWidth, screenHeight);
        if (inside(mouseX, mouseY, gridX, gridY, gridCols * gridSlot, gridRows * gridSlot)) {
            int max = gridMaxScroll(warehouseDataSize);
            if (max > 0) {
                warehouseScrollRow = Math.max(0, Math.min(max,
                        warehouseScrollRow - (int) Math.signum(scrollY)));
            }
        } else if (inside(mouseX, mouseY, invX, invY, INV_COLS * invSlot, invRows * invSlot)) {
            int max = itemMaxScroll(inventorySize());
            if (max > 0) {
                itemScrollRow = Math.max(0, Math.min(max,
                        itemScrollRow - (int) Math.signum(scrollY)));
            }
        }
    }

    private static void setWarehouseScrollFromMouse(double mouseY, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int trackH = gridRows * gridSlot;
        int thumbH = Math.max(12, Math.round(trackH * (float) gridRows / (gridRows + maxScroll)));
        int travel = Math.max(1, trackH - thumbH);
        float ratio = (float) (mouseY - gridY - thumbH / 2.0) / travel;
        warehouseScrollRow = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));
    }

    private static void setItemScrollFromMouse(double mouseY, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int trackH = invRows * invSlot;
        int thumbH = Math.max(12, Math.round(trackH * (float) invRows / (invRows + maxScroll)));
        int travel = Math.max(1, trackH - thumbH);
        float ratio = (float) (mouseY - invY - thumbH / 2.0) / travel;
        itemScrollRow = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));
    }

    private static ItemStack stackAt(int source, int index) {
        var player = Minecraft.getInstance().player;
        if (source == SOURCE_INVENTORY && player != null && index >= 0 && index < player.getInventory().items.size()) {
            return player.getInventory().getItem(index);
        }
        if (source == SOURCE_WAREHOUSE && index >= 0 && index < currentWarehouse.size()) {
            return currentWarehouse.get(index);
        }
        if (source == SOURCE_ARMOR && player != null && index >= 0 && index < 6) {
            return armorStackAt(index);
        }
        return ItemStack.EMPTY;
    }

    // ========== 工具 ==========

    /**
     * 读取盔甲槽物品（0头 1甲 2裤 3鞋 4副手 5主手）
     */
    private static ItemStack armorStackAt(int slotIndex) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }
        return switch (slotIndex) {
            case 0 -> player.getInventory().getArmor(3);
            case 1 -> player.getInventory().getArmor(2);
            case 2 -> player.getInventory().getArmor(1);
            case 3 -> player.getInventory().getArmor(0);
            case 4 -> player.getOffhandItem();
            case 5 -> player.getMainHandItem();
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * 命中盔甲槽则返回槽位索引（0头 1甲 2裤 3鞋 4副手 5主手），否则 -1
     */
    private static int equipSlotAt(int mouseX, int mouseY) {
        for (int i = 0; i < 6; i++) {
            int row = i / armorCols;
            int col = i % armorCols;
            int x = armorX + col * armorColumnStride;
            int y = armorY + row * armorRowStride;
            if (inside(mouseX, mouseY, x, y, armorInner, armorInner)) {
                return i;
            }
        }
        return -1;
    }

    private static int gridIndexAt(int mouseX, int mouseY, int left, int top, int cols,
                                   int visibleRows, int firstRow, int size, int slot, int inner) {
        int start = firstRow * cols;
        int end = Math.min(size, start + cols * visibleRows);
        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = left + local % cols * slot;
            int y = top + local / cols * slot;
            if (inside(mouseX, mouseY, x, y, inner, inner)) {
                return index;
            }
        }
        return -1;
    }

    private static int inventorySize() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : player.getInventory().items.size();
    }

    private static int itemMaxScroll(int size) {
        return Math.max(0, (size + INV_COLS - 1) / INV_COLS - invRows);
    }

    private static int gridMaxScroll(int size) {
        return Math.max(0, (size + gridCols - 1) / gridCols - gridRows);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
