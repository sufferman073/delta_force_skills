package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.CraftabilityStatus;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeDetail;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeTreeLine;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeView;
import com.rzy.dealt_force_skills.network.C2S_HvkConstructorCraft;
import com.rzy.dealt_force_skills.network.C2S_HvkConstructorDetail;
import com.rzy.dealt_force_skills.network.C2S_HvkConstructorQuery;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HvkConstructorScreen extends Screen {
    private static final int PADDING = 12;
    private static final int HEADER_HEIGHT = 54;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 3;
    private static final int TREE_ROW_HEIGHT = 22;
    private static final int SCROLL_STEP = 24;
    private static final int TREE_TOP_OFFSET = 82;
    private static final int TREE_BOTTOM_RESERVED = 32;
    private static final int SEARCH_DEBOUNCE_TICKS = 16;
    private static final int DETAIL_REQUEST_DELAY_TICKS = 8;

    private final BlockPos pos;
    private String query;
    private int page;
    private int totalRecipes;
    private List<RecipeView> recipes;
    private ResourceLocation selectedId;
    private RecipeDetail detail;
    private boolean searchFocused;
    private int recipeScroll;
    private int detailScroll;
    private int pendingSearchTicks = -1;
    private int pendingSearchPage;
    private int pendingDetailTicks = -1;
    private ResourceLocation pendingDetailId;
    private int pendingDetailRequestId;
    private int latestDetailRequestId;
    private boolean detailChecking;
    private final boolean disassembler;
    private final Set<Integer> collapsedTreeRows = new HashSet<>();
    private final Map<Integer, Integer> disassemblyChoiceIndexes = new HashMap<>();

    public HvkConstructorScreen(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes) {
        this(pos, query, page, totalRecipes, recipes, false);
    }

    public HvkConstructorScreen(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes,
                                boolean disassembler) {
        super(Component.translatable(screenKey(disassembler, "title")));
        this.pos = pos;
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
        this.totalRecipes = Math.max(0, totalRecipes);
        this.recipes = recipes == null ? List.of() : List.copyOf(recipes);
        this.disassembler = disassembler;
        this.selectedId = this.recipes.isEmpty() ? null : this.recipes.get(0).id();
        this.detail = null;
        collapseAllTreeRows();
    }

    public static void open(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes) {
        open(pos, query, page, totalRecipes, recipes, false);
    }

    public static void open(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes,
                            boolean disassembler) {
        HvkConstructorScreen screen = new HvkConstructorScreen(pos, query, page, totalRecipes, recipes, disassembler);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void updateRecipes(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes) {
        updateRecipes(pos, query, page, totalRecipes, recipes, false);
    }

    public static void updateRecipes(BlockPos pos, String query, int page, int totalRecipes, List<RecipeView> recipes,
                                     boolean disassembler) {
        if (Minecraft.getInstance().screen instanceof HvkConstructorScreen screen
                && screen.pos.equals(pos)
                && screen.disassembler == disassembler) {
            if (!normalizeQuery(query).equals(normalizeQuery(screen.query))) {
                return;
            }
            screen.setRecipes(query, page, totalRecipes, recipes);
        }
    }

    public static void updateDetail(BlockPos pos, int requestId, RecipeDetail detail) {
        if (Minecraft.getInstance().screen instanceof HvkConstructorScreen screen
                && screen.pos.equals(pos)
                && detail != null
                && detail.id().equals(screen.selectedId)) {
            if (requestId > 0) {
                if (requestId != screen.latestDetailRequestId) {
                    return;
                }
                screen.detailChecking = !isFinalDetail(detail);
            } else if (screen.latestDetailRequestId > 0 || screen.detailChecking) {
                return;
            }
            boolean firstDetail = screen.detail == null
                    || !screen.detail.id().equals(detail.id())
                    || screen.detail.tree().isEmpty();
            screen.detail = detail;
            if (firstDetail) {
                screen.detailScroll = 0;
                screen.disassemblyChoiceIndexes.clear();
                screen.collapseAllTreeRows();
            }
        }
    }

    private static boolean isFinalDetail(RecipeDetail detail) {
        if (detail == null) {
            return false;
        }
        return switch (detail.status()) {
            case CRAFTABLE, MISSING, TRUNCATED, ERROR -> true;
            case BASIC_DETAIL, PLANNING -> false;
        };
    }

    private void setRecipes(String query, int page, int totalRecipes, List<RecipeView> recipes) {
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
        this.totalRecipes = Math.max(0, totalRecipes);
        this.recipes = recipes == null ? List.of() : List.copyOf(recipes);
        if (selectedId == null || this.recipes.stream().noneMatch(recipe -> recipe.id().equals(selectedId))) {
            selectedId = this.recipes.isEmpty() ? null : this.recipes.get(0).id();
            detail = null;
            detailChecking = false;
            invalidateDetailRequests();
            disassemblyChoiceIndexes.clear();
        }
        recipeScroll = 0;
        detailScroll = 0;
        pendingDetailTicks = -1;
        pendingDetailId = null;
        pendingDetailRequestId = 0;
        collapseAllTreeRows();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (pendingSearchTicks >= 0 && --pendingSearchTicks <= 0) {
            int page = pendingSearchPage;
            pendingSearchTicks = -1;
            requestPageNow(page);
        }
        if (pendingDetailTicks >= 0 && --pendingDetailTicks <= 0) {
            ResourceLocation detailId = pendingDetailId;
            pendingDetailTicks = -1;
            pendingDetailId = null;
            if (detailId != null && detailId.equals(selectedId)) {
                requestSelectedDetailNow(detailId, pendingDetailRequestId);
            }
        }
        if (detail != null && detail.crafting()) {
            int nextTick = Math.min(detail.craftIntervalTicks(), detail.craftTicks() + 1);
            if (nextTick != detail.craftTicks()) {
                detail = new RecipeDetail(detail.id(), detail.tree(), detail.missing(),
                        detail.queuedCrafts(), detail.queuedCompleted(), nextTick, detail.craftIntervalTicks(),
                        detail.truncated(), detail.truncationReason(), detail.status());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xAA000000);
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left, top, left + panelWidth(), top + panelHeight(), 0xE012171D);
        graphics.fill(left, top, left + panelWidth(), top + 1, 0xFFFFD34D);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        drawSearch(graphics, mouseX, mouseY);
        drawRecipeList(graphics, mouseX, mouseY);
        drawDetail(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderHoveredTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            searchFocused = inside(mouseX, mouseY, searchLeft(), searchTop(), searchWidth(), 18);
            int pageDelta = pageButtonAt(mouseX, mouseY);
            if (pageDelta != 0) {
                requestPage(page + pageDelta);
                return true;
            }
            RecipeView row = recipeAt(mouseX, mouseY);
            if (row != null) {
                selectedId = row.id();
                detail = null;
                detailChecking = true;
                detailScroll = 0;
                disassemblyChoiceIndexes.clear();
                collapseAllTreeRows();
                scheduleSelectedDetail();
                return true;
            }
            TreeRow treeRow = treeRowAt(mouseX, mouseY);
            if (cycleDisassemblyChoice(treeRow)) {
                return true;
            }
            if (treeRow != null && treeRow.hasChildren()) {
                if (treeRow.collapsed()) {
                    collapsedTreeRows.remove(treeRow.sourceIndex());
                } else {
                    collapsedTreeRows.add(treeRow.sourceIndex());
                }
                detailScroll = clamp(detailScroll, 0, maxDetailScroll());
                return true;
            }
            int craftCount = craftButtonAt(mouseX, mouseY);
            RecipeView selected = selectedRecipe();
            if (selected != null && craftCount > 0 && canCraftSelected(selected)) {
                NetworkHandler.sendToServer(new C2S_HvkConstructorCraft(pos, selected.id(), craftCount,
                        selectedDisassemblyOutputs(selected)));
                return true;
            }
            if (searchFocused) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scheduleRecipes();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                requestRecipesNow();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && !Character.isISOControl(codePoint) && query.length() < 80) {
            query += codePoint;
            scheduleRecipes();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, listLeft(), listTop(), listWidth(), listHeight())) {
            recipeScroll = clamp(recipeScroll - (int) Math.round(scrollY * SCROLL_STEP), 0, maxRecipeScroll());
            return true;
        }
        if (inside(mouseX, mouseY, treeLeft(), treeTop(), treeWidth(), treeHeight())) {
            detailScroll = clamp(detailScroll - (int) Math.round(scrollY * SCROLL_STEP), 0, maxDetailScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void requestRecipes() {
        scheduleRecipes();
    }

    private void scheduleRecipes() {
        pendingSearchPage = 0;
        pendingSearchTicks = SEARCH_DEBOUNCE_TICKS;
        pendingDetailTicks = -1;
        pendingDetailId = null;
        pendingDetailRequestId = 0;
        detail = null;
        detailChecking = false;
        disassemblyChoiceIndexes.clear();
        invalidateDetailRequests();
        detailScroll = 0;
        collapseAllTreeRows();
    }

    private void requestRecipesNow() {
        requestPage(0);
    }

    private void requestPage(int requestedPage) {
        pendingSearchTicks = -1;
        requestPageNow(requestedPage);
    }

    private void requestPageNow(int requestedPage) {
        NetworkHandler.sendToServer(new C2S_HvkConstructorQuery(pos, query, clamp(requestedPage, 0, maxPage())));
    }

    private void scheduleSelectedDetail() {
        if (selectedId != null) {
            pendingDetailId = selectedId;
            pendingDetailRequestId = nextDetailRequestId();
            detailChecking = true;
            pendingDetailTicks = DETAIL_REQUEST_DELAY_TICKS;
        }
    }

    private int nextDetailRequestId() {
        if (latestDetailRequestId == Integer.MAX_VALUE) {
            latestDetailRequestId = 0;
        }
        return ++latestDetailRequestId;
    }

    private void invalidateDetailRequests() {
        if (latestDetailRequestId == Integer.MAX_VALUE) {
            latestDetailRequestId = 0;
        }
        latestDetailRequestId++;
    }

    private void requestSelectedDetailNow(ResourceLocation detailId, int requestId) {
        NetworkHandler.sendToServer(new C2S_HvkConstructorDetail(pos, detailId, requestId));
    }

    private String screenKey(String leaf) {
        return screenKey(disassembler, leaf);
    }

    private static String screenKey(boolean disassembler, String leaf) {
        return "screen.dealt_force_skills." + (disassembler ? "hvk_disassembler." : "hvk_constructor.") + leaf;
    }

    private void drawSearch(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = searchLeft();
        int y = searchTop();
        graphics.fill(x, y, x + searchWidth(), y + 18, 0xAA111820);
        graphics.fill(x, y, x + searchWidth(), y + 1, searchFocused ? 0xFFFFD34D : 0x66304050);
        Component text = query.isBlank()
                ? Component.translatable(screenKey("search"))
                : Component.literal(query);
        graphics.drawString(font, fit(text, searchWidth() - 10), x + 5, y + 5,
                query.isBlank() ? 0xFF7D8791 : 0xFFFFFF);
        Component resultText = Component.translatable(screenKey("results"),
                recipes.size(), totalRecipes, page + 1, maxPage() + 1);
        graphics.drawString(font, fit(resultText, searchWidth() - 94), x, y + 24, 0xFFBFC7D0);
        drawPageButton(graphics, pageButtonLeft(), y + 21, -1, mouseX, mouseY);
        drawPageButton(graphics, pageButtonLeft() + 44, y + 21, 1, mouseX, mouseY);
    }

    private void drawPageButton(GuiGraphics graphics, int x, int y, int delta, int mouseX, int mouseY) {
        boolean enabled = delta < 0 ? page > 0 : page < maxPage();
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, 40, 14);
        graphics.fill(x, y, x + 40, y + 14, enabled ? (hovered ? 0xCC3D4A36 : 0xAA26333D) : 0x55304050);
        graphics.fill(x, y, x + 40, y + 1, hovered ? 0xFFFFD34D : 0x66304050);
        graphics.drawCenteredString(font, Component.translatable(delta < 0
                ? screenKey("previous")
                : screenKey("next")), x + 20, y + 3, enabled ? 0xFFFFFF : 0xFF7D8791);
    }

    private void drawRecipeList(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = listLeft();
        int top = listTop();
        graphics.fill(left - 1, top - 1, left + listWidth() + 1, top + listHeight() + 1, 0x5523333F);
        graphics.enableScissor(left, top, left + listWidth(), top + listHeight());
        int y = top - recipeScroll;
        for (RecipeView recipe : recipes) {
            drawRecipeRow(graphics, recipe, left, y, mouseX, mouseY);
            y += ROW_HEIGHT + ROW_GAP;
        }
        graphics.disableScissor();
        drawRecipeScrollbar(graphics);
    }

    private void drawRecipeRow(GuiGraphics graphics, RecipeView recipe, int x, int y, int mouseX, int mouseY) {
        if (y + ROW_HEIGHT < listTop() || y > listTop() + listHeight()) {
            return;
        }
        boolean selected = recipe.id().equals(selectedId);
        boolean hovered = inside(mouseX, mouseY, x, y, listWidth(), ROW_HEIGHT);
        int fill = selected ? 0xCC314A35 : hovered ? 0xAA24313D : 0x8818242F;
        int edge = selected ? 0xFFFFD34D : hovered ? 0xFF7D8791 : 0x66304050;
        graphics.fill(x, y, x + listWidth(), y + ROW_HEIGHT, fill);
        graphics.fill(x, y, x + listWidth(), y + 1, edge);
        graphics.renderItem(recipe.result(), x + 5, y + 7);
        graphics.renderItemDecorations(font, recipe.result(), x + 5, y + 7);
        graphics.drawString(font, fit(recipe.result().getHoverName(), listWidth() - 34), x + 28, y + 5, 0xFFFFFF);
        graphics.drawString(font, fit(Component.literal(recipe.id().toString()), listWidth() - 34),
                x + 28, y + 18, 0xFF8D98A5);
    }

    private void drawDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = detailLeft();
        int y = detailTop();
        int w = detailWidth();
        int h = detailHeight();
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x5523333F);
        RecipeView selected = selectedRecipe();
        if (selected == null) {
            graphics.drawCenteredString(font, Component.translatable(screenKey("no_recipe")),
                    x + w / 2, y + h / 2 - 4, 0xFFBFC7D0);
            return;
        }

        graphics.drawString(font, Component.translatable(screenKey("target")),
                x + PADDING, y + 10, 0xFFFFD34D);
        graphics.renderItem(selected.result(), x + PADDING, y + 25);
        graphics.renderItemDecorations(font, selected.result(), x + PADDING, y + 25);
        graphics.drawString(font, fit(selected.result().getHoverName(), w - 48),
                x + PADDING + 24, y + 25, 0xFFFFFF);
        graphics.drawString(font, fit(Component.literal(selected.id().toString()), w - 48),
                x + PADDING + 24, y + 38, 0xFF8D98A5);

        List<RecipeTreeLine> lines = detailLines(selected);
        List<TreeRow> visibleRows = visibleDetailRows(selected);
        String treeCount = visibleRows.size() == lines.size()
                ? Integer.toString(lines.size())
                : visibleRows.size() + "/" + lines.size();
        graphics.drawString(font, fit(missingText(selected), w - PADDING * 2),
                x + PADDING, treeTop() - 38, missingColor(selected));
        graphics.drawString(font, fit(progressText(selected), w - PADDING * 2),
                x + PADDING, treeTop() - 26, progressColor(selected));
        graphics.drawString(font, fit(treeText(selected, treeCount), w - PADDING * 2),
                x + PADDING, treeTop() - 14, detail != null && detail.truncated() ? 0xFFFFA64D : 0xFFFFD34D);
        drawRecipeTree(graphics, selected, visibleRows);
        drawCraftButtons(graphics, mouseX, mouseY);
    }

    private Component missingText(RecipeView selected) {
        if (detailChecking) {
            return Component.translatable(screenKey("status_checking"));
        }
        if (detail == null || !detail.id().equals(selected.id())) {
            return Component.translatable(screenKey("status_unknown"));
        }
        if (detail.status() == CraftabilityStatus.BASIC_DETAIL) {
            return Component.translatable(screenKey("status_basic_detail"));
        }
        if (detail.status() == CraftabilityStatus.PLANNING) {
            return Component.translatable(screenKey("status_checking"));
        }
        if (detail.status() == CraftabilityStatus.CRAFTABLE) {
            return Component.translatable(screenKey("missing_ready"));
        }
        if (detail.status() == CraftabilityStatus.TRUNCATED) {
            return Component.translatable(screenKey("status_truncated"));
        }
        if (detail.status() == CraftabilityStatus.ERROR) {
            return Component.translatable(screenKey("status_error"));
        }
        List<ItemStack> missing = validStacks(detail.missing());
        if (missing.isEmpty()) {
            return Component.translatable(screenKey("missing_unknown"));
        }
        return Component.translatable(screenKey("missing_detail"),
                stackSummary(missing, 3));
    }

    private int missingColor(RecipeView selected) {
        if (detailChecking || detail == null || !detail.id().equals(selected.id())) {
            return 0xFFFFD34D;
        }
        return switch (detail.status()) {
            case CRAFTABLE -> 0xFF9FE870;
            case MISSING, ERROR -> 0xFFFF6B6B;
            case TRUNCATED -> 0xFFFFA64D;
            default -> 0xFFFFD34D;
        };
    }

    private Component progressText(RecipeView selected) {
        if (detail == null || !detail.id().equals(selected.id()) || !detail.crafting()) {
            return Component.translatable(screenKey("progress_idle"));
        }
        int totalTicks = Math.max(1, detail.queuedCrafts() * detail.craftIntervalTicks());
        int doneTicks = detail.queuedCompleted() * detail.craftIntervalTicks() + detail.craftTicks();
        int percent = clamp((int) Math.floor(doneTicks * 100.0D / totalTicks), 0, 100);
        return Component.translatable(screenKey("progress_active"),
                detail.queuedCompleted(), detail.queuedCrafts(), percent);
    }

    private int progressColor(RecipeView selected) {
        return detail != null && detail.id().equals(selected.id()) && detail.crafting()
                ? 0xFFFFD34D
                : 0xFF8D98A5;
    }

    private Component treeText(RecipeView selected, String treeCount) {
        if (detail != null && detail.id().equals(selected.id())
                && (detail.truncated() || detail.status() == CraftabilityStatus.TRUNCATED)) {
            return Component.translatable(screenKey("tree_truncated"), treeCount);
        }
        return Component.translatable(screenKey("tree"), treeCount);
    }

    private static String stackSummary(List<ItemStack> stacks, int maxEntries) {
        if (stacks == null || stacks.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        int limit = Math.max(1, maxEntries);
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0 || shown >= limit) {
                continue;
            }
            if (shown > 0) {
                builder.append(", ");
            }
            builder.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
            shown++;
        }
        int validCount = validStacks(stacks).size();
        if (validCount > shown) {
            builder.append(", +").append(validCount - shown);
        }
        return builder.toString();
    }

    private static List<ItemStack> validStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty() && stack.getCount() > 0) {
                valid.add(stack);
            }
        }
        return List.copyOf(valid);
    }

    private void drawRecipeTree(GuiGraphics graphics, RecipeView selected, List<TreeRow> rows) {
        int x = treeLeft();
        int y = treeTop();
        int w = treeWidth();
        int h = treeHeight();
        graphics.fill(x, y, x + w, y + h, 0x66111820);
        graphics.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < rows.size(); i++) {
            TreeRow row = rows.get(i);
            RecipeTreeLine line = displayLine(row.sourceIndex(), row.line());
            int rowY = y + 4 + i * TREE_ROW_HEIGHT - detailScroll;
            if (rowY + 18 < y || rowY > y + h) {
                continue;
            }
            int indent = Math.min(84, line.depth() * 14);
            int toggleX = x + 5 + indent;
            int iconX = x + 17 + indent;
            int textX = iconX + 22;
            int color = line.missing() ? 0xFFFF6B6B : (line.raw() ? 0xFFBFC7D0 : 0xFFFFD34D);
            if (row.hasChildren()) {
                graphics.drawCenteredString(font, Component.literal(row.collapsed() ? "+" : "-"),
                        toggleX + 4, rowY + 5, 0xFFFFD34D);
            }
            graphics.fill(iconX - 1, rowY - 1, iconX + 17, rowY + 17, line.missing() ? 0x884F2020 : 0x8840525F);
            graphics.renderItem(line.stack(), iconX, rowY);
            graphics.renderItemDecorations(font, line.stack(), iconX, rowY);
            graphics.drawString(font, fit(line.stack().getHoverName(), x + w - textX - 6), textX, rowY + 1, color);
            if (line.depth() == 0 && selected.id().equals(detail == null ? null : detail.id())) {
                graphics.drawString(font, fit(Component.literal(selected.id().toString()), x + w - textX - 6),
                        textX, rowY + 12, 0xFF8D98A5);
            }
        }
        graphics.disableScissor();
        drawDetailScrollbar(graphics);
    }

    private void drawCraftButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        RecipeView selected = selectedRecipe();
        boolean enabled = selected != null && canCraftSelected(selected);
        int[] counts = actionCounts();
        int totalGap = Math.max(0, counts.length - 1) * 4;
        int buttonWidth = Math.max(disassembler ? 132 : 56,
                (detailWidth() - PADDING * 2 - totalGap) / Math.max(1, counts.length));
        int x = detailLeft() + PADDING;
        int y = detailTop() + detailHeight() - 30;
        for (int i = 0; i < counts.length; i++) {
            int buttonX = x + i * (buttonWidth + 4);
            boolean hovered = enabled && inside(mouseX, mouseY, buttonX, y, buttonWidth, 20);
            graphics.fill(buttonX, y, buttonX + buttonWidth, y + 20,
                    enabled ? (hovered ? 0xCC3D4A36 : 0xAA26333D) : 0x55304050);
            graphics.fill(buttonX, y, buttonX + buttonWidth, y + 1,
                    enabled && hovered ? 0xFFFFD34D : 0x66304050);
            graphics.drawCenteredString(font, Component.translatable(
                    screenKey("craft"), counts[i]),
                    buttonX + buttonWidth / 2, y + 6, enabled ? 0xFFFFFF : 0xFF7D8791);
        }
    }

    private void drawRecipeScrollbar(GuiGraphics graphics) {
        drawScrollbar(graphics, listLeft() + listWidth() - 3, listTop(), listHeight(),
                recipeFullHeight(), recipeScroll, maxRecipeScroll());
    }

    private void drawDetailScrollbar(GuiGraphics graphics) {
        drawScrollbar(graphics, treeLeft() + treeWidth() - 3, treeTop(), treeHeight(),
                detailFullHeight(), detailScroll, maxDetailScroll());
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int y, int height, int fullHeight, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        graphics.fill(x, y, x + 2, y + height, 0x55304050);
        int thumbHeight = Math.max(12, height * height / Math.max(height, fullHeight));
        int thumbTravel = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) Math.round(thumbTravel * (scroll / (double) maxScroll));
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xAAFFD34D);
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        RecipeView selected = selectedRecipe();
        if (!disassembler && selected != null && isMissingSummaryHovered(mouseX, mouseY)
                && detail != null && detail.status() == CraftabilityStatus.TRUNCATED) {
            graphics.renderTooltip(font, font.split(Component.translatable(screenKey("truncated_help")), 260), mouseX, mouseY);
            return;
        }
        if (selected != null && isMissingSummaryHovered(mouseX, mouseY)
                && detail != null && detail.id().equals(selected.id())
                && detail.status() == CraftabilityStatus.MISSING && !detail.missing().isEmpty()) {
            graphics.renderComponentTooltip(font, missingMaterialTooltip(detail.missing()), mouseX, mouseY);
            return;
        }
        TreeRow treeRow = treeRowAt(mouseX, mouseY);
        if (treeRow != null && renderTreeAlternativesTooltip(graphics,
                displayLine(treeRow.sourceIndex(), treeRow.line()), mouseX, mouseY)) {
            return;
        }
        ItemStack hovered = stackAt(mouseX, mouseY);
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private boolean renderTreeAlternativesTooltip(GuiGraphics graphics, RecipeTreeLine line, int mouseX, int mouseY) {
        List<Component> lines = alternativeTooltipLines(line);
        if (lines.isEmpty()) {
            return false;
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    private ItemStack stackAt(double mouseX, double mouseY) {
        RecipeView row = recipeAt(mouseX, mouseY);
        if (row != null) {
            return row.result();
        }
        RecipeView selected = selectedRecipe();
        if (selected == null) {
            return ItemStack.EMPTY;
        }
        if (inside(mouseX, mouseY, detailLeft() + PADDING, detailTop() + 25, 18, 18)) {
            return selected.result();
        }
        TreeRow treeRow = treeRowAt(mouseX, mouseY);
        if (treeRow != null) {
            return selectedLineStack(treeRow.sourceIndex(), treeRow.line());
        }
        return ItemStack.EMPTY;
    }

    private RecipeView recipeAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, listLeft(), listTop(), listWidth(), listHeight())) {
            return null;
        }
        int y = listTop() - recipeScroll;
        for (RecipeView recipe : recipes) {
            if (inside(mouseX, mouseY, listLeft(), y, listWidth(), ROW_HEIGHT)) {
                return recipe;
            }
            y += ROW_HEIGHT + ROW_GAP;
        }
        return null;
    }

    private int treeLineIndexAt(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, treeLeft(), treeTop(), treeWidth(), treeHeight())) {
            return -1;
        }
        int y = (int) mouseY - treeTop() - 4 + detailScroll;
        if (y < 0) {
            return -1;
        }
        return y / TREE_ROW_HEIGHT;
    }

    private TreeRow treeRowAt(double mouseX, double mouseY) {
        RecipeView selected = selectedRecipe();
        if (selected == null) {
            return null;
        }
        int index = treeLineIndexAt(mouseX, mouseY);
        List<TreeRow> rows = visibleDetailRows(selected);
        if (index >= 0 && index < rows.size()) {
            return rows.get(index);
        }
        return null;
    }

    private int craftButtonAt(double mouseX, double mouseY) {
        RecipeView selected = selectedRecipe();
        if (selected == null || !canCraftSelected(selected)) {
            return 0;
        }
        int[] counts = actionCounts();
        int totalGap = Math.max(0, counts.length - 1) * 4;
        int buttonWidth = Math.max(disassembler ? 132 : 56,
                (detailWidth() - PADDING * 2 - totalGap) / Math.max(1, counts.length));
        int x = detailLeft() + PADDING;
        int y = detailTop() + detailHeight() - 30;
        for (int i = 0; i < counts.length; i++) {
            int buttonX = x + i * (buttonWidth + 4);
            if (inside(mouseX, mouseY, buttonX, y, buttonWidth, 20)) {
                return counts[i];
            }
        }
        return 0;
    }

    private int[] actionCounts() {
        return disassembler ? new int[]{1} : new int[]{1, 10, 64};
    }

    private boolean canCraftSelected(RecipeView selected) {
        return !detailChecking
                && detail != null
                && detail.id().equals(selected.id())
                && detail.status() == CraftabilityStatus.CRAFTABLE;
    }

    private int pageButtonAt(double mouseX, double mouseY) {
        int y = searchTop() + 21;
        if (page > 0 && inside(mouseX, mouseY, pageButtonLeft(), y, 40, 14)) {
            return -1;
        }
        if (page < maxPage() && inside(mouseX, mouseY, pageButtonLeft() + 44, y, 40, 14)) {
            return 1;
        }
        return 0;
    }

    private RecipeView selectedRecipe() {
        if (selectedId == null) {
            return null;
        }
        for (RecipeView recipe : recipes) {
            if (recipe.id().equals(selectedId)) {
                return recipe;
            }
        }
        return null;
    }

    private List<RecipeTreeLine> detailLines(RecipeView selected) {
        if (!disassembler && detail != null && detail.id().equals(selected.id()) && !detail.missing().isEmpty()) {
            List<RecipeTreeLine> missing = new ArrayList<>();
            missing.add(new RecipeTreeLine(selected.result(), 0, false, false));
            for (ItemStack stack : detail.missing()) missing.add(new RecipeTreeLine(stack, 0, true, true));
            return missing;
        }
        if (detail != null && detail.id().equals(selected.id()) && !detail.tree().isEmpty()) {
            List<RecipeTreeLine> ordered = new ArrayList<>();
            for (RecipeTreeLine line : detail.tree()) {
                if (line != null && line.stack() != null && !line.stack().isEmpty()
                        && line.stack().getCount() > 0) {
                    ordered.add(line);
                }
            }
            if (ordered.size() > 2) {
                List<RecipeTreeLine> requirements = new ArrayList<>(ordered.subList(1, ordered.size()));
                requirements.sort(java.util.Comparator.comparing(RecipeTreeLine::missing).reversed());
                ordered.subList(1, ordered.size()).clear();
                ordered.addAll(requirements);
            }
            return ordered;
        }
        List<RecipeTreeLine> fallback = new ArrayList<>();
        fallback.add(new RecipeTreeLine(selected.result(), 0, false, false));
        for (ItemStack ingredient : validStacks(selected.ingredients())) {
            fallback.add(new RecipeTreeLine(ingredient, 1, true, false));
        }
        return fallback;
    }

    private List<TreeRow> visibleDetailRows(RecipeView selected) {
        List<RecipeTreeLine> lines = detailLines(selected);
        List<TreeRow> rows = new ArrayList<>();
        int hiddenDepth = -1;
        for (int i = 0; i < lines.size(); i++) {
            RecipeTreeLine line = lines.get(i);
            if (hiddenDepth >= 0) {
                if (line.depth() > hiddenDepth) {
                    continue;
                }
                hiddenDepth = -1;
            }
            boolean hasChildren = i + 1 < lines.size() && lines.get(i + 1).depth() > line.depth();
            boolean collapsed = hasChildren && collapsedTreeRows.contains(i);
            rows.add(new TreeRow(i, line, hasChildren, collapsed));
            if (collapsed) {
                hiddenDepth = line.depth();
            }
        }
        return rows;
    }

    private boolean cycleDisassemblyChoice(TreeRow row) {
        if (!disassembler || row == null) {
            return false;
        }
        RecipeTreeLine line = row.line();
        if (line.depth() != 1 || line.alternatives().size() <= 1) {
            return false;
        }
        int current = disassemblyChoiceIndexes.getOrDefault(row.sourceIndex(), defaultChoiceIndex(line));
        int next = (current + 1) % line.alternatives().size();
        disassemblyChoiceIndexes.put(row.sourceIndex(), next);
        return true;
    }

    private RecipeTreeLine displayLine(int sourceIndex, RecipeTreeLine line) {
        if (!disassembler || line == null || line.depth() != 1 || line.alternatives().isEmpty()) {
            return line;
        }
        return new RecipeTreeLine(selectedLineStack(sourceIndex, line), line.depth(), line.raw(), line.missing(),
                line.alternatives());
    }

    private ItemStack selectedLineStack(int sourceIndex, RecipeTreeLine line) {
        if (!disassembler || line == null || line.depth() != 1 || line.alternatives().isEmpty()) {
            return line == null ? ItemStack.EMPTY : line.stack();
        }
        int index = clamp(disassemblyChoiceIndexes.getOrDefault(sourceIndex, defaultChoiceIndex(line)), 0,
                line.alternatives().size() - 1);
        return line.alternatives().get(index);
    }

    private int defaultChoiceIndex(RecipeTreeLine line) {
        if (line == null || line.alternatives().isEmpty() || line.stack().isEmpty()) {
            return 0;
        }
        for (int i = 0; i < line.alternatives().size(); i++) {
            ItemStack alternative = line.alternatives().get(i);
            if (!alternative.isEmpty() && alternative.getItem() == line.stack().getItem()) {
                return i;
            }
        }
        return 0;
    }

    private List<ResourceLocation> selectedDisassemblyOutputs(RecipeView selected) {
        if (!disassembler || selected == null) {
            return List.of();
        }
        List<ResourceLocation> outputs = new ArrayList<>();
        List<RecipeTreeLine> lines = detailLines(selected);
        for (int i = 0; i < lines.size(); i++) {
            RecipeTreeLine line = lines.get(i);
            if (line.depth() == 1 && line.raw()) {
                ItemStack selectedStack = selectedLineStack(i, line);
                if (!selectedStack.isEmpty()) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(selectedStack.getItem());
                    if (key != null) {
                        outputs.add(key);
                    }
                }
            }
        }
        return outputs;
    }

    private void collapseAllTreeRows() {
        collapsedTreeRows.clear();
        RecipeView selected = selectedRecipe();
        if (selected == null) {
            return;
        }
        List<RecipeTreeLine> lines = detailLines(selected);
        for (int i = 0; i + 1 < lines.size(); i++) {
            if (lines.get(i + 1).depth() > lines.get(i).depth()) {
                collapsedTreeRows.add(i);
            }
        }
    }

    private List<Component> alternativeTooltipLines(RecipeTreeLine line) {
        if (line == null || line.alternatives().size() <= 1) {
            return List.of();
        }
        List<Component> lines = new ArrayList<>();
        lines.add(line.stack().getHoverName());
        lines.add(Component.literal(""));
        for (ItemStack alternative : line.alternatives()) {
            if (!alternative.isEmpty() && alternative.getItem() != line.stack().getItem()) {
                lines.add(stackNameWithCount(alternative));
            }
        }
        return lines.size() <= 2 ? List.of() : lines;
    }

    private boolean isMissingSummaryHovered(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, detailLeft() + PADDING, treeTop() - 40,
                Math.max(1, detailWidth() - PADDING * 2), 12);
    }

    private List<Component> missingMaterialTooltip(List<ItemStack> missing) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(screenKey("missing")));
        for (ItemStack stack : missing) {
            if (!stack.isEmpty()) {
                lines.add(stackNameWithCount(stack).copy().withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        return lines;
    }

    private Component stackNameWithCount(ItemStack stack) {
        if (stack.getCount() > 1) {
            return Component.literal(stack.getCount() + "x ").append(stack.getHoverName());
        }
        return stack.getHoverName();
    }

    private int maxPage() {
        if (totalRecipes <= 0) {
            return 0;
        }
        return (totalRecipes - 1) / HvkAdvancedStandardTemplateConstructorBlockEntity.RECIPE_PAGE_SIZE;
    }

    private int recipeFullHeight() {
        return Math.max(0, recipes.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP);
    }

    private int maxRecipeScroll() {
        return Math.max(0, recipeFullHeight() - listHeight());
    }

    private int detailFullHeight() {
        RecipeView selected = selectedRecipe();
        if (selected == null) {
            return 0;
        }
        return visibleDetailRows(selected).size() * TREE_ROW_HEIGHT + 8;
    }

    private int maxDetailScroll() {
        return Math.max(0, detailFullHeight() - treeHeight());
    }

    private int panelWidth() {
        return Math.max(1, Math.min(840, width - 24));
    }

    private int panelHeight() {
        return Math.max(1, Math.min(520, height - 24));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return Math.max(8, (height - panelHeight()) / 2);
    }

    private int searchLeft() {
        return panelLeft() + PADDING;
    }

    private int searchTop() {
        return panelTop() + 30;
    }

    private int searchWidth() {
        return listWidth();
    }

    private int pageButtonLeft() {
        return searchLeft() + searchWidth() - 84;
    }

    private int listLeft() {
        return panelLeft() + PADDING;
    }

    private int listTop() {
        return panelTop() + HEADER_HEIGHT + 18;
    }

    private int listWidth() {
        return Math.max(190, Math.min(270, panelWidth() / 3));
    }

    private int listHeight() {
        return Math.max(40, panelTop() + panelHeight() - listTop() - PADDING);
    }

    private int detailLeft() {
        return listLeft() + listWidth() + 12;
    }

    private int detailTop() {
        return panelTop() + HEADER_HEIGHT;
    }

    private int detailWidth() {
        return Math.max(160, panelLeft() + panelWidth() - detailLeft() - PADDING);
    }

    private int detailHeight() {
        return Math.max(80, panelTop() + panelHeight() - detailTop() - PADDING);
    }

    private int treeLeft() {
        return detailLeft() + PADDING;
    }

    private int treeTop() {
        return detailTop() + TREE_TOP_OFFSET;
    }

    private int treeWidth() {
        return Math.max(40, detailWidth() - PADDING * 2);
    }

    private int treeHeight() {
        return Math.max(24, detailTop() + detailHeight() - TREE_BOTTOM_RESERVED - treeTop());
    }

    private Component fit(Component component, int maxWidth) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            return component;
        }
        return Component.literal(font.plainSubstrByWidth(text, Math.max(8, maxWidth - font.width("..."))) + "...");
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private record TreeRow(int sourceIndex, RecipeTreeLine line, boolean hasChildren, boolean collapsed) {
    }
}
