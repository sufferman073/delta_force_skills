package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.home.HomeDimensions;
import com.rzy.dealt_force_skills.home.HomeManager;
import com.rzy.dealt_force_skills.network.C2S_HomeAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.UUID;

/** Server snapshots drive the list; refreshes never replace the current screen. */
public final class HomeScreen extends Screen {
    private static final UUID NONE = new UUID(0, 0);
    private final Screen parent;
    private HomeManager.Snapshot snapshot = new HomeManager.Snapshot(List.of(), List.of(), false, "", false, true, true, false, false);
    private int offset, refresh;
    private boolean visitors;
    private String status = "loading";
    public HomeScreen(Screen parent) { super(tr("title")); this.parent = parent; }
    private static Component tr(String key, Object... args) { return Component.translatable("home.dealt_force_skills." + key, args); }
    private int left() { return Math.max(8, (width - 720) / 2); }
    private int right() { return width - left(); }
    private int visibleRows() { return Math.max(1, (height - 126) / 68); }
    private int count() { return visitors ? snapshot.people().size() : snapshot.regions().size(); }
    private int maxOffset() { return Math.max(0, count() - visibleRows()); }
    @Override protected void init() { widgets(); send(HomeManager.OPEN, NONE, 0); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void tick() { if (++refresh >= 40) { refresh = 0; send(HomeManager.OPEN, NONE, 0); } }
    private void send(int action, UUID target, int amount) {
        NetworkHandler.sendToServer(new C2S_HomeAction(action, target, amount));
        if (action != HomeManager.OPEN) status = "waiting";
    }
    public static void addDimension(UUID owner) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.levels().add(HomeDimensions.key(owner));
    }
    public static void accept(HomeManager.Snapshot value) {
        if (!(Minecraft.getInstance().screen instanceof HomeScreen screen)) return;
        screen.snapshot = value;
        if (!value.status().isEmpty()) screen.status = value.status();
        else if (screen.status.equals("loading")) screen.status = "";
        if (value.close()) { Minecraft.getInstance().setScreen(null); return; }
        screen.offset = Math.min(screen.offset, screen.maxOffset()); screen.widgets();
    }
    private Button button(Component label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(DepartmentUiStyle.widget(label, x, y, w, action));
    }
    private void widgets() {
        clearWidgets(); int l = left(), r = right();
        button(tr("regions"), l, 30, 104, () -> { visitors = false; offset = 0; widgets(); });
        button(tr("visitors", snapshot.people().size()), l + 110, 30, 128, () -> { visitors = true; offset = 0; widgets(); });
        button(Component.translatable("gui.back"), l, height - 30, 74, this::onClose);
        Button back = button(tr("return"), r - 136, height - 30, 136, () -> send(HomeManager.RETURN, NONE, 0)); back.active = snapshot.inHome();
        int end = Math.min(count(), offset + visibleRows());
        for (int index = offset; index < end; index++) {
            int y = 62 + (index - offset) * 68;
            if (visitors) {
                var p = snapshot.people().get(index);
                button(tr(p.approved() ? "revoke" : "approve"), r - 164, y + 31, 74,
                        () -> send(p.approved() ? HomeManager.REVOKE : HomeManager.APPROVE, p.id(), 0));
                if (!p.approved()) button(tr("decline"), r - 84, y + 31, 74, () -> send(HomeManager.DECLINE, p.id(), 0));
            } else {
                var row = snapshot.regions().get(index);
                boolean mayEnter = row.own() || row.allowed();
                Button enter = button(tr(mayEnter ? "enter" : row.pending() ? "pending" : "request"), r - 112, y + 31, 102,
                        () -> send(mayEnter ? HomeManager.ENTER : HomeManager.REQUEST, row.owner(), 0));
                enter.active = snapshot.enabled() && (mayEnter || row.created() && !row.pending());
                if (row.own()) {
                    int w = Math.min(156, Math.max(60, (r - l - 134) / 2));
                    Button target = button(tr("carry_target", tr(snapshot.targeted() ? "allow" : "forbid")), l + 10, y + 31, w,
                            () -> send(HomeManager.SET_TARGETED, row.owner(), snapshot.targeted() ? 0 : 1));
                    Button nearby = button(tr("carry_nearby", tr(snapshot.nearby() ? "allow" : "forbid")), l + 16 + w, y + 31, w,
                            () -> send(HomeManager.SET_NEARBY, row.owner(), snapshot.nearby() ? 0 : 1));
                    target.active = nearby.active = snapshot.enabled() && snapshot.transportAllowed();
                    target.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("carry_target_help")));
                    nearby.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tr("carry_nearby_help")));
                }
            }
        }
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, DepartmentUiStyle.BG);
        g.drawCenteredString(font, title, width / 2, 10, DepartmentUiStyle.TEXT);
        int l = left(), r = right(), end = Math.min(count(), offset + visibleRows());
        for (int i = offset; i < end; i++) {
            int y = 62 + (i - offset) * 68;
            DepartmentUiStyle.panel(g, l, y, r - l, 62, DepartmentUiStyle.ROW);
            Component name, detail;
            if (visitors) {
                var p = snapshot.people().get(i); name = Component.literal(p.name()); detail = tr(p.approved() ? "guest" : "application");
            } else {
                var row = snapshot.regions().get(i); name = row.own() ? tr("own") : tr("named", row.name());
                detail = tr(row.own() ? "size" : row.created() ? "size_guest" : "uncreated", row.size(), row.size());
            }
            g.drawString(font, font.plainSubstrByWidth(name.getString(), r - l - 20), l + 10, y + 5, DepartmentUiStyle.TEXT, false);
            g.drawString(font, font.plainSubstrByWidth(detail.getString(), r - l - 20), l + 10, y + 18, DepartmentUiStyle.MUTED, false);
        }
        if (count() == 0) g.drawCenteredString(font, tr(visitors ? "no_visitors" : "loading"), width / 2, 80, DepartmentUiStyle.MUTED);
        DepartmentUiStyle.scrollbar(g, r + 2, 62, visibleRows() * 68, offset * 68, maxOffset() * 68);
        if (!snapshot.enabled()) status = "disabled";
        else if (status.equals("disabled")) status = "";
        if (!status.isEmpty()) g.drawString(font, font.plainSubstrByWidth(tr(status).getString(), r - l), l, height - 46, status.equals("denied") ? 0xFFFF8B82 : DepartmentUiStyle.MUTED, false);
        super.render(g, mx, my, partial);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double delta) {
        if (y < 58 || y >= height - 48) return false;
        offset = Math.max(0, Math.min(maxOffset(), offset - (int) Math.signum(delta))); widgets(); return true;
    }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
