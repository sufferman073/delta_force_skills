package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.client.ClientHudLayout;
import com.rzy.dealt_force_skills.client.config.ConfigLabelHelper;
import com.rzy.dealt_force_skills.config.ConfigAccess;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import com.rzy.dealt_force_skills.config.ConfigFileId;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import com.rzy.dealt_force_skills.config.ConfigValueKind;
import com.rzy.dealt_force_skills.network.C2S_ApplyConfigChanges;
import com.rzy.dealt_force_skills.network.C2S_RequestConfigSnapshot;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Graphical config UI: collapsible scrollable sidebar tree + searchable value list.
 */
public class DealtForceConfigScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 148;
    private static final int ROW_HEIGHT = 26;
    private static final int SIDEBAR_ROW = 18;
    private static final List<String> CATEGORY_ORDER = List.of(
            "hud", "characters", "rules", "bosses", "shop", "player", "gambler", "summons", "items", "other");

    private static DealtForceConfigScreen active;
    private static final List<ConfigEntryData> pendingServerEntries = new ArrayList<>();
    private static int expectedServerChunks = -1;
    private static boolean pendingCanEdit;

    private final Screen parent;
    private final Map<String, String> dirtyValues = new HashMap<>();
    private final Map<String, ConfigEntryData> entryByKey = new LinkedHashMap<>();
    private final List<ConfigEntryData> allEntries = new ArrayList<>();
    private final List<EditBox> valueFields = new ArrayList<>();
    private final Set<String> expandedCategories = new HashSet<>();

    private String selectedCategory = "hud";
    private String selectedSubcategory = "";
    private String statusMessage = "";
    private boolean canEditServer = false;
    private boolean awaitingServer = false;
    private boolean remoteDedicated = false;

    private EditBox searchBox;
    private EntryList entryList;
    private SidebarList sidebarList;
    private Button saveButton;
    private Button reloadButton;

    public DealtForceConfigScreen(Screen parent) {
        super(Component.translatable("config.dealt_force_skills.title"));
        this.parent = parent;
        // Default-expand HUD so first open is useful.
        this.expandedCategories.add("hud");
    }

    public static void acceptServerChunk(
            int chunkIndex,
            int totalChunks,
            boolean canEdit,
            boolean lastChunk,
            List<ConfigEntryData> entries) {
        if (chunkIndex == 0) {
            pendingServerEntries.clear();
            expectedServerChunks = totalChunks;
            pendingCanEdit = canEdit;
        }
        if (entries != null) {
            pendingServerEntries.addAll(entries);
        }
        if (lastChunk || (expectedServerChunks > 0 && chunkIndex + 1 >= expectedServerChunks)) {
            ConfigAccess.applyRuntimeSnapshot(List.copyOf(pendingServerEntries));
            mccfk.DeltaForce.java.client.screen.SettingsScreen.acceptServerSnapshot(
                    List.copyOf(pendingServerEntries), pendingCanEdit);
            if (active != null) {
                active.onServerSnapshot(List.copyOf(pendingServerEntries), pendingCanEdit);
            }
            pendingServerEntries.clear();
            expectedServerChunks = -1;
        }
    }

    public static void onApplyResult(boolean success, int appliedCount, String code) {
        mccfk.DeltaForce.java.client.screen.SettingsScreen.acceptApplyResult(success, appliedCount, code);
        if (active == null) {
            return;
        }
        if (success) {
            active.statusMessage = Component.translatable(
                    "config.dealt_force_skills.status.saved", appliedCount).getString();
            active.dirtyValues.clear();
            active.requestOrLoadEntries();
        } else {
            active.statusMessage = Component.translatable(
                    "config.dealt_force_skills.status.save_failed", code == null ? "?" : code).getString();
        }
    }

    @Override
    protected void init() {
        active = this;
        clearValueFields();

        int contentLeft = SIDEBAR_WIDTH + 10;
        int contentWidth = Math.max(120, width - contentLeft - 10);
        int listTop = 52;
        int listBottom = height - 38;

        searchBox = new EditBox(font, contentLeft, 28, contentWidth, 18,
                Component.translatable("config.dealt_force_skills.search"));
        searchBox.setHint(Component.translatable("config.dealt_force_skills.search"));
        searchBox.setResponder(text -> rebuildEntryList());
        addRenderableWidget(searchBox);

        sidebarList = new SidebarList(minecraft, SIDEBAR_WIDTH - 4, listBottom - 24, 24, SIDEBAR_ROW);
        sidebarList.setX(2);
        addWidget(sidebarList);

        entryList = new EntryList(minecraft, contentWidth, listBottom - listTop, listTop, ROW_HEIGHT);
        entryList.setX(contentLeft);
        addWidget(entryList);

        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width - 90, buttonY, 80, 20).build());
        saveButton = addRenderableWidget(Button.builder(
                        Component.translatable("config.dealt_force_skills.save"),
                        button -> saveChanges())
                .bounds(width - 180, buttonY, 84, 20).build());
        reloadButton = addRenderableWidget(Button.builder(
                        Component.translatable("config.dealt_force_skills.reload"),
                        button -> requestOrLoadEntries())
                .bounds(width - 270, buttonY, 84, 20).build());

        requestOrLoadEntries();
    }

    private void requestOrLoadEntries() {
        dirtyValues.clear();
        entryByKey.clear();
        allEntries.clear();

        for (ConfigEntryData entry : ClientHudLayout.snapshotEntries()) {
            putEntry(entry);
        }

        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        remoteDedicated = connection != null && !mc.hasSingleplayerServer();
        boolean inWorld = connection != null;
        boolean localOp = mc.player != null && mc.player.hasPermissions(2);

        if (!inWorld) {
            canEditServer = true;
            for (ConfigEntryData entry : ConfigAccess.snapshotAllServerFiles()) {
                putEntry(entry);
            }
            awaitingServer = false;
            statusMessage = Component.translatable("config.dealt_force_skills.status.local_menu").getString();
            rebuildSidebar();
            rebuildEntryList();
            return;
        }

        if (mc.hasSingleplayerServer() && localOp) {
            canEditServer = true;
            for (ConfigEntryData entry : ConfigAccess.snapshotAllServerFiles()) {
                putEntry(entry);
            }
            awaitingServer = false;
            statusMessage = Component.translatable("config.dealt_force_skills.status.singleplayer_op").getString();
            rebuildSidebar();
            rebuildEntryList();
            return;
        }

        if (localOp) {
            canEditServer = true;
            awaitingServer = true;
            statusMessage = Component.translatable("config.dealt_force_skills.status.loading_server").getString();
            NetworkHandler.sendToServer(new C2S_RequestConfigSnapshot());
            rebuildSidebar();
            rebuildEntryList();
            return;
        }

        canEditServer = false;
        awaitingServer = false;
        statusMessage = Component.translatable("config.dealt_force_skills.status.hud_only").getString();
        rebuildSidebar();
        rebuildEntryList();
    }

    private void onServerSnapshot(List<ConfigEntryData> serverEntries, boolean canEdit) {
        canEditServer = canEdit;
        awaitingServer = false;
        entryByKey.entrySet().removeIf(e -> e.getValue().file() != ConfigFileId.HUD);
        allEntries.removeIf(e -> e.file() != ConfigFileId.HUD);
        for (ConfigEntryData entry : serverEntries) {
            putEntry(entry);
        }
        statusMessage = canEdit
                ? Component.translatable("config.dealt_force_skills.status.server_op", serverEntries.size()).getString()
                : Component.translatable("config.dealt_force_skills.status.server_denied").getString();
        rebuildSidebar();
        rebuildEntryList();
    }

    private void putEntry(ConfigEntryData entry) {
        String key = keyOf(entry);
        entryByKey.put(key, entry);
        allEntries.removeIf(existing -> keyOf(existing).equals(key));
        allEntries.add(entry);
    }

    private static String keyOf(ConfigEntryData entry) {
        return entry.file().name() + "|" + entry.path();
    }

    private void clearValueFields() {
        for (EditBox field : valueFields) {
            removeWidget(field);
        }
        valueFields.clear();
    }

    private void rebuildSidebar() {
        if (sidebarList == null) {
            return;
        }
        sidebarList.reset();
        for (String category : presentCategories()) {
            sidebarList.append(new CategoryNode(category));
            if (expandedCategories.contains(category)) {
                sidebarList.append(new SubcategoryNode(category, ""));
                for (String sub : presentSubcategories(category)) {
                    sidebarList.append(new SubcategoryNode(category, sub));
                }
            }
        }
    }

    private void rebuildEntryList() {
        if (entryList == null) {
            return;
        }
        clearValueFields();
        entryList.reset();
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        List<ConfigEntryData> filtered = new ArrayList<>();
        for (ConfigEntryData entry : allEntries) {
            String category = ConfigAccess.categoryKey(entry);
            if (!selectedCategory.equals(category)) {
                continue;
            }
            if (!selectedSubcategory.isEmpty()
                    && !selectedSubcategory.equals(ConfigAccess.subcategoryKey(entry))) {
                continue;
            }
            if (!canEdit(entry)) {
                continue;
            }
            if (!query.isEmpty()) {
                String label = ConfigLabelHelper.entryLabel(entry).getString().toLowerCase(Locale.ROOT);
                String purpose = ConfigLabelHelper.entryTooltip(entry).getString().toLowerCase(Locale.ROOT);
                if (!entry.path().toLowerCase(Locale.ROOT).contains(query)
                        && !label.contains(query)
                        && !purpose.contains(query)
                        && !entry.file().fileName().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
            }
            filtered.add(entry);
        }
        filtered.sort(Comparator
                .comparing((ConfigEntryData e) -> ConfigAccess.subcategoryKey(e))
                .thenComparing(ConfigEntryData::path));

        boolean largeCategory = "characters".equals(selectedCategory)
                || "summons".equals(selectedCategory)
                || "items".equals(selectedCategory);
        if (selectedSubcategory.isEmpty() && largeCategory && filtered.size() > 40) {
            List<String> subs = presentSubcategories(selectedCategory);
            if (!subs.isEmpty()) {
                selectedSubcategory = subs.get(0);
                expandedCategories.add(selectedCategory);
                rebuildSidebar();
                filtered.clear();
                for (ConfigEntryData entry : allEntries) {
                    String category = ConfigAccess.categoryKey(entry);
                    if (!selectedCategory.equals(category)) {
                        continue;
                    }
                    if (!selectedSubcategory.equals(ConfigAccess.subcategoryKey(entry))) {
                        continue;
                    }
                    if (!canEdit(entry)) {
                        continue;
                    }
                    if (!query.isEmpty()) {
                        String label = ConfigLabelHelper.entryLabel(entry).getString().toLowerCase(Locale.ROOT);
                        String purpose = ConfigLabelHelper.entryTooltip(entry).getString().toLowerCase(Locale.ROOT);
                        if (!entry.path().toLowerCase(Locale.ROOT).contains(query)
                                && !label.contains(query)
                                && !purpose.contains(query)
                                && !entry.file().fileName().toLowerCase(Locale.ROOT).contains(query)) {
                            continue;
                        }
                    }
                    filtered.add(entry);
                }
                filtered.sort(Comparator
                        .comparing((ConfigEntryData e) -> ConfigAccess.subcategoryKey(e))
                        .thenComparing(ConfigEntryData::path));
                statusMessage = Component.translatable("config.dealt_force_skills.status.auto_sub",
                        ConfigLabelHelper.subcategoryTitle(selectedCategory, selectedSubcategory).getString()).getString();
            } else {
                entryList.append(new HeaderRow("_pick_sub"));
                statusMessage = Component.translatable("config.dealt_force_skills.status.pick_subcategory").getString();
                if (saveButton != null) {
                    saveButton.active = !dirtyValues.isEmpty();
                }
                if (reloadButton != null) {
                    reloadButton.active = !awaitingServer;
                }
                return;
            }
        }

        String lastSub = null;
        for (ConfigEntryData entry : filtered) {
            String sub = ConfigAccess.subcategoryKey(entry);
            if (!sub.equals(lastSub)) {
                entryList.append(new HeaderRow(sub));
                lastSub = sub;
            }
            entryList.append(new ValueRow(entry));
        }
        if (saveButton != null) {
            saveButton.active = !dirtyValues.isEmpty();
        }
        if (reloadButton != null) {
            reloadButton.active = !awaitingServer;
        }
    }

    private void saveChanges() {
        if (dirtyValues.isEmpty()) {
            statusMessage = Component.translatable("config.dealt_force_skills.status.no_changes").getString();
            return;
        }
        List<C2S_ApplyConfigChanges.Change> serverChanges = new ArrayList<>();
        int localHud = 0;
        int localServer = 0;
        Set<ConfigFileId> localTouched = new HashSet<>();
        // In an integrated world, use the packet too so reload runs on the server thread.
        boolean applyLocally = Minecraft.getInstance().getConnection() == null;

        for (Map.Entry<String, String> dirty : dirtyValues.entrySet()) {
            ConfigEntryData entry = entryByKey.get(dirty.getKey());
            if (entry == null) {
                continue;
            }
            String value = dirty.getValue();
            try {
                ConfigEntryData.parse(entry.kind(), value);
            } catch (RuntimeException error) {
                statusMessage = Component.translatable(
                        "config.dealt_force_skills.status.invalid", entry.path()).getString();
                return;
            }
            if (entry.file() == ConfigFileId.HUD) {
                Object parsed = ConfigEntryData.parse(entry.kind(), value);
                if (ClientHudLayout.setValue(entry.path(), parsed)) {
                    localHud++;
                }
                continue;
            }
            if (!canEditServer) {
                continue;
            }
            if (applyLocally) {
                if (ConfigAccess.applyChange(entry.file(), entry.path(), entry.kind(), value)) {
                    localServer++;
                    localTouched.add(entry.file());
                }
            } else {
                serverChanges.add(C2S_ApplyConfigChanges.Change.from(entry, value));
            }
        }

        if (localServer > 0) {
            for (ConfigFileId file : localTouched) {
                ConfigAccess.flush(file);
            }
            ConfigHotReload.reloadAll();
        }
        if (localHud > 0) {
            ClientHudLayout.flush();
            ClientHudLayout.forceReload();
        }
        if (!serverChanges.isEmpty()) {
            NetworkHandler.sendToServer(new C2S_ApplyConfigChanges(serverChanges));
            statusMessage = Component.translatable(
                    "config.dealt_force_skills.status.uploading", serverChanges.size()).getString();
            return;
        }

        dirtyValues.clear();
        statusMessage = Component.translatable(
                "config.dealt_force_skills.status.saved_local", localHud + localServer).getString();
        requestOrLoadEntries();
    }

    private List<String> presentCategories() {
        Set<String> present = new HashSet<>();
        for (ConfigEntryData entry : allEntries) {
            present.add(ConfigAccess.categoryKey(entry));
        }
        List<String> ordered = new ArrayList<>();
        for (String key : CATEGORY_ORDER) {
            if (present.contains(key)) {
                ordered.add(key);
            }
        }
        for (String key : present) {
            if (!ordered.contains(key)) {
                ordered.add(key);
            }
        }
        if (ordered.isEmpty()) {
            ordered.add("hud");
        }
        return ordered;
    }

    private List<String> presentSubcategories(String category) {
        Set<String> present = new HashSet<>();
        for (ConfigEntryData entry : allEntries) {
            if (category.equals(ConfigAccess.categoryKey(entry))) {
                present.add(ConfigAccess.subcategoryKey(entry));
            }
        }
        List<String> list = new ArrayList<>(present);
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    private void selectSubcategory(String category, String sub) {
        selectedCategory = category;
        selectedSubcategory = sub == null ? "" : sub;
        expandedCategories.add(category);
        rebuildSidebar();
        rebuildEntryList();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        // Sidebar background
        graphics.fill(0, 0, SIDEBAR_WIDTH, height, 0xE0101010);
        graphics.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 1, height, 0xFF404040);
        graphics.drawString(font, title, 8, 8, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("config.dealt_force_skills.sidebar.hint"),
                8, height - 12, 0x808080, false);

        if (sidebarList != null) {
            sidebarList.render(graphics, mouseX, mouseY, partialTick);
        }
        if (entryList != null) {
            entryList.render(graphics, mouseX, mouseY, partialTick);
            // Hide EditBoxes that scrolled out of the list viewport to prevent overlap.
            syncFieldVisibility();
            renderValueFields(graphics, mouseX, mouseY, partialTick);
        }
        // Status line (clip length)
        String status = statusMessage;
        int maxStatus = Math.max(40, width - SIDEBAR_WIDTH - 280);
        if (font.width(status) > maxStatus) {
            status = font.plainSubstrByWidth(status, maxStatus - 8) + "…";
        }
        graphics.drawString(font, status, SIDEBAR_WIDTH + 12, height - 42, 0xFFD0D0, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void syncFieldVisibility() {
        if (entryList == null) {
            return;
        }
        int top = entryList.viewportTop();
        int bottom = entryList.viewportBottom();
        for (EditBox field : valueFields) {
            int y = field.getY();
            boolean visible = y >= top + 2 && y + 16 <= bottom - 2;
            field.visible = visible;
            if (!visible) {
                field.active = false;
                if (getFocused() == field) {
                    setFocused(null);
                }
            }
        }
    }

    private void renderValueFields(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (EditBox field : valueFields) {
            if (field.visible) {
                field.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (EditBox field : valueFields) {
            if (field.visible && field.active
                    && mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                    && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight()) {
                setFocused(field);
                field.setFocused(true);
                return field.mouseClicked(mouseX, mouseY, button) || true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox box && box.isFocused()) {
            if (box.keyPressed(keyCode, scanCode, modifiers) || box.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (getFocused() instanceof EditBox box && box.isFocused() && box.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < SIDEBAR_WIDTH && sidebarList != null) {
            return sidebarList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (entryList != null && entryList.isMouseOver(mouseX, mouseY)) {
            return entryList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        active = null;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        super.removed();
    }

    private boolean canEdit(ConfigEntryData entry) {
        if (entry.file() == ConfigFileId.HUD) {
            return true;
        }
        return canEditServer;
    }


    private String currentValue(ConfigEntryData entry) {
        String key = keyOf(entry);
        if (dirtyValues.containsKey(key)) {
            return dirtyValues.get(key);
        }
        return entry.valueText();
    }

    // ---- Sidebar tree ----

    private abstract class SidebarRow extends ObjectSelectionList.Entry<SidebarRow> {
    }

    private final class CategoryNode extends SidebarRow {
        private final String category;

        private CategoryNode(String category) {
            this.category = category;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean selected = category.equals(selectedCategory) && selectedSubcategory.isEmpty();
            boolean expanded = expandedCategories.contains(category);
            int bg = selected ? 0xFF4060A0 : (hovering ? 0xFF303038 : 0x00000000);
            if (bg != 0) {
                graphics.fill(left, top, left + width, top + height, bg);
            }
            String arrow = expanded ? "▼ " : "▶ ";
            String label = arrow + ConfigLabelHelper.categoryTitle(category).getString();
            if (font.width(label) > width - 8) {
                label = font.plainSubstrByWidth(label, width - 14) + "…";
            }
            graphics.drawString(font, label, left + 4, top + 5, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (category.equals(selectedCategory)) {
                if (expandedCategories.contains(category)) {
                    expandedCategories.remove(category);
                } else {
                    expandedCategories.add(category);
                    List<String> subs = presentSubcategories(category);
                    selectedSubcategory = subs.isEmpty() ? "" : subs.get(0);
                }
                rebuildSidebar();
                rebuildEntryList();
            } else {
                selectedCategory = category;
                expandedCategories.add(category);
                List<String> subs = presentSubcategories(category);
                boolean large = "characters".equals(category) || "summons".equals(category) || "items".equals(category);
                selectedSubcategory = (large && !subs.isEmpty()) ? subs.get(0) : "";
                rebuildSidebar();
                rebuildEntryList();
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return ConfigLabelHelper.categoryTitle(category);
        }
    }

    private final class SubcategoryNode extends SidebarRow {
        private final String category;
        private final String sub; // empty = "全部"

        private SubcategoryNode(String category, String sub) {
            this.category = category;
            this.sub = sub;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            boolean selected = category.equals(selectedCategory)
                    && ((sub.isEmpty() && selectedSubcategory.isEmpty())
                    || sub.equals(selectedSubcategory));
            int bg = selected ? 0xFF305030 : (hovering ? 0xFF252528 : 0x00000000);
            if (bg != 0) {
                graphics.fill(left, top, left + width, top + height, bg);
            }
            Component title = sub.isEmpty()
                    ? Component.translatable("config.dealt_force_skills.sub.all")
                    : ConfigLabelHelper.subcategoryTitle(category, sub);
            String label = "  └ " + title.getString();
            if (font.width(label) > width - 10) {
                label = font.plainSubstrByWidth(label, width - 16) + "…";
            }
            graphics.drawString(font, label, left + 8, top + 5, selected ? 0xFFFFFF : 0xC8C8C8, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectSubcategory(category, sub);
            return true;
        }

        @Override
        public Component getNarration() {
            return sub.isEmpty()
                    ? Component.translatable("config.dealt_force_skills.sub.all")
                    : ConfigLabelHelper.subcategoryTitle(category, sub);
        }
    }

    private final class SidebarList extends ObjectSelectionList<SidebarRow> {
        private SidebarList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 8;
        }

        private void reset() {
            this.clearEntries();
        }

        private void append(SidebarRow row) {
            this.addEntry(row);
        }
    }

    // ---- Value list ----

    private abstract class Row extends ObjectSelectionList.Entry<Row> {
    }

    private final class HeaderRow extends Row {
        private final String subcategory;

        private HeaderRow(String subcategory) {
            this.subcategory = subcategory;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            Component title = "_pick_sub".equals(subcategory)
                    ? Component.translatable("config.dealt_force_skills.status.pick_subcategory")
                    : ConfigLabelHelper.subcategoryTitle(selectedCategory, subcategory);
            graphics.fill(left, top + 2, left + width - 4, top + height - 2, 0x40202020);
            graphics.drawString(font, title, left + 4, top + 8, 0xFFCC66, false);
        }

        @Override
        public Component getNarration() {
            return "_pick_sub".equals(subcategory)
                    ? Component.translatable("config.dealt_force_skills.status.pick_subcategory")
                    : ConfigLabelHelper.subcategoryTitle(selectedCategory, subcategory);
        }
    }

    private final class ValueRow extends Row {
        private final ConfigEntryData entry;
        private final EditBox field;

        private ValueRow(ConfigEntryData entry) {
            this.entry = entry;
            this.field = new EditBox(font, 0, 0, 90, 16, Component.literal(entry.path()));
            this.field.setMaxLength(256);
            this.field.setValue(currentValue(entry));
            this.field.setEditable(canEdit(entry));
            this.field.setResponder(text -> {
                if (!canEdit(entry)) {
                    return;
                }
                String key = keyOf(entry);
                if (text.equals(entry.valueText())) {
                    dirtyValues.remove(key);
                } else {
                    dirtyValues.put(key, text);
                }
                if (saveButton != null) {
                    saveButton.active = !dirtyValues.isEmpty();
                }
            });
            if (entry.kind() == ConfigValueKind.BOOLEAN) {
                this.field.setFilter(s -> s.isEmpty()
                        || "true".regionMatches(true, 0, s, 0, s.length())
                        || "false".regionMatches(true, 0, s, 0, s.length())
                        || "1".equals(s) || "0".equals(s));
            }
            this.field.visible = false;
            valueFields.add(field);
            // Value rows own rendering. Keeping fields out of Screen.renderables prevents old
            // rows from surviving a filtered or scrolled list rebuild.
            addWidget(field);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            Component label = ConfigLabelHelper.entryLabel(entry);
            int fieldW = 96;
            int labelWidth = Math.max(60, width - fieldW - 16);
            String text = label.getString();
            if (font.width(text) > labelWidth) {
                text = font.plainSubstrByWidth(text, labelWidth - 8) + "…";
            }
            int color = canEdit(entry) ? 0xFFFFFF : 0x888888;
            if (dirtyValues.containsKey(keyOf(entry))) {
                color = 0xFFD060;
            }
            graphics.drawString(font, text, left + 4, top + 5, color, false);
            // secondary path hint (very small)
            String pathHint = entry.path();
            if (font.width(pathHint) > labelWidth) {
                pathHint = font.plainSubstrByWidth(pathHint, labelWidth - 8) + "…";
            }
            graphics.drawString(font, pathHint, left + 4, top + 15, 0x707070, false);

            field.setX(left + width - fieldW - 6);
            field.setY(top + 5);
            field.setWidth(fieldW);
            field.visible = top >= entryList.viewportTop() - 4 && top + height <= entryList.viewportBottom() + 4;
            field.active = field.visible && canEdit(entry);

            if (hovering && mouseX < field.getX()) {
                setTooltipForNextRenderPass(ConfigLabelHelper.entryTooltip(entry));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (field.visible && canEdit(entry)
                    && mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                    && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight()) {
                DealtForceConfigScreen.this.setFocused(field);
                field.setFocused(true);
                field.setEditable(true);
                field.active = true;
                field.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            if (field.visible && field.mouseClicked(mouseX, mouseY, button)) {
                DealtForceConfigScreen.this.setFocused(field);
                field.setFocused(true);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return ConfigLabelHelper.entryLabel(entry);
        }
    }

    private final class EntryList extends ObjectSelectionList<Row> {
        private EntryList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return Math.max(200, this.getWidth() - 16);
        }

        private int viewportTop() { return this.getY(); }

        private int viewportBottom() { return this.getY() + this.height; }

        private void reset() {
            this.clearEntries();
            setScrollAmount(0);
        }

        private void append(Row row) {
            this.addEntry(row);
        }
    }
}