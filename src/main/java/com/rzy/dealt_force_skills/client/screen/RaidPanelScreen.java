package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.raid.ui.RaidPanelMenu;
import com.rzy.dealt_force_skills.raid.ui.RaidUiItems;
import com.rzy.dealt_force_skills.raid.ui.RaidPanelMenu.Page;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import static com.rzy.dealt_force_skills.client.screen.DepartmentUiStyle.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="dealt_force_skills", value=net.minecraftforge.api.distmarker.Dist.CLIENT, bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class RaidPanelScreen extends Screen implements MenuAccess<RaidPanelMenu> {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void screens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> { for (Page page : Page.values()) net.minecraft.client.gui.screens.MenuScreens.register(page.type, RaidPanelScreen::new); });
    }
    private static final int TOP = 48, FOOT = 32, HOME = -1, DAM = -2, SPACE = -3, CLOSE = -4, START = -5;
    private record Hit(int x, int y, int width, int height, int slot) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
    private final RaidPanelMenu menu;
    private final boolean prepFromMaps;
    private final List<Hit> hits = new ArrayList<>();
    private int canvasWidth, canvasHeight, originX, originY, scroll, maxScroll, mouseCanvasX, mouseCanvasY;
    private float scale;
    private net.minecraft.client.gui.components.EditBox search;
    private String query = "";
    private int pressed = -1, pendingClick = -1;
    private long clickedAt;
    private double pressX, pressY;
    private boolean damMatch, spaceMatch, dragging;
    private ItemStack hover = ItemStack.EMPTY;
    private List<Component> uiHint = List.of();
    public RaidPanelScreen(RaidPanelMenu menu, Inventory inventory, Component title) {
        super(title); this.menu = menu;
        prepFromMaps = net.minecraft.client.Minecraft.getInstance().screen instanceof RaidPanelScreen parent
                && (parent.menu.pageKind == Page.MAPS || parent.prepFromMaps);
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof RaidPanelScreen previous && previous.menu.pageKind == menu.pageKind) {
            query = previous.search == null ? "" : previous.search.getValue(); scroll = previous.scroll; damMatch = previous.damMatch; spaceMatch = previous.spaceMatch;
        }
    }
    @Override public RaidPanelMenu getMenu() { return menu; }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() {
        var canvas = DepartmentCanvas.fit(width, height);
        canvasWidth = canvas.width(); canvasHeight = canvas.height(); scale = canvas.scale();
        originX = canvas.originX(); originY = canvas.originY();
        hits.clear(); dragging = false;
        if (menu.pageKind == Page.PURCHASES) {
            int boxWidth = Math.max(60, (int)((right()-left())*.34*scale));
            search = new net.minecraft.client.gui.components.EditBox(font, originX+(int)((right()-85)*scale)-boxWidth,
                    originY+(int)(15*scale), boxWidth, Math.max(12,(int)(19*scale)), Component.literal("搜索商品"));
            search.setHint(Component.literal("搜索商品 · 回车")); search.setMaxLength(80); search.setValue(query); addRenderableWidget(search);
        }
    }
    private int left() { return Math.max(15, (canvasWidth - 730) / 2); }
    private int right() { return canvasWidth - left() - 12; }
    private int bottom() { return canvasHeight - FOOT; }
    private ItemStack item(int slot) { return menu.getSlot(slot).getItem(); }
    private boolean has(int slot) { return !item(slot).isEmpty(); }
    private void text(GuiGraphics gui, String label, int x, int y, int width, int color) {
        gui.drawString(font, font.plainSubstrByWidth(label, Math.max(0, width)), x, y, color, false);
    }
    @Override public void render(GuiGraphics gui, int mx, int my, float partial) {
        gui.fill(0, 0, width, height, BG);
        mouseCanvasX = (int)Math.floor((mx-originX)/scale); mouseCanvasY = (int)Math.floor((my-originY)/scale);
        hits.clear(); hover = ItemStack.EMPTY; uiHint = List.of();
        gui.pose().pushPose(); gui.pose().translate(originX, originY, 0); gui.pose().scale(scale, scale, 1);
        int left = left(), right = right();
        text(gui, "特勤处  /  行动中心", left, 6, right-left, MUTED);
        text(gui, menu.pageKind == Page.MAPS ? "选择地图  /  行动队伍" : title.getString(), left, 23, right-left-86, TEXT);
        gui.fill(left, 39, right, 40, BORDER);
        if (menu.pageKind == Page.PREP) action(gui,right-182,13,104,"返回开始游戏",START);
        action(gui, right-72, 13, 72, "关闭", CLOSE);
        gui.enableScissor(originX + (int)(left*scale), originY + (int)(TOP*scale), originX + (int)Math.ceil(right*scale), originY + (int)Math.ceil(bottom()*scale));
        int start = TOP + 3 - scroll;
        int end = menu.pageKind == Page.MAPS ? maps(gui, left, right, start) : assets(gui, left, right, start);
        gui.disableScissor();
        maxScroll = Math.max(0, end + scroll + 5 - bottom());
        if (has(menu.pageKind == Page.MAPS ? 49 : 53)) scroll = Math.min(scroll, maxScroll);
        scrollbar(gui, right+3, TOP, bottom()-TOP, scroll, maxScroll);
        text(gui, maxScroll > 0 ? "滚轮或拖动右侧滑条浏览" : menu.pageKind == Page.MAPS ? "选择目的地后，可邀请队友一起出发" : "悬停查看详情与可用操作", left, bottom()+11, right-left, MUTED);
        gui.pose().popPose();
        if (search != null) search.render(gui,mx,my,partial);
        if (pressed >= 0 && Math.hypot(mx-pressX,my-pressY)>3 && has(pressed)) gui.renderItem(item(pressed),mx-8,my-8);
        if (!uiHint.isEmpty()) drawHint(gui, mx, my);
        else if (!hover.isEmpty()) gui.renderTooltip(font, hover, mx, my);
    }
    private void drawHint(GuiGraphics gui, int mx, int my) {
        int limit = Math.max(1, Math.min(260, width - 16));
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (Component line : uiHint) lines.addAll(font.split(line, limit));
        int hintWidth = lines.stream().mapToInt(font::width).max().orElse(0) + 12, hintHeight = lines.size() * (font.lineHeight + 2) + 8;
        int left = Math.max(2, Math.min(mx + 10, width - hintWidth - 2)), top = my + hintHeight + 14 < height ? my + 12 : Math.max(2, my - hintHeight - 4);
        gui.pose().pushPose(); gui.pose().translate(0, 0, 400);
        panel(gui, left, top, hintWidth, hintHeight, PANEL); gui.fill(left, top, left + 2, top + hintHeight, 0xFF67BBA8);
        for (int index = 0; index < lines.size(); index++) gui.drawString(font, lines.get(index), left + 6, top + 4 + index * (font.lineHeight + 2), index == 0 ? TEXT : MUTED, false);
        gui.pose().popPose();
    }
    private boolean hovered(Hit hit) {
        return hit.contains(mouseCanvasX, mouseCanvasY) && (hit.slot == CLOSE || hit.slot == START || mouseCanvasY >= TOP && mouseCanvasY < bottom());
    }
    private void action(GuiGraphics gui, int x, int y, int width, String label, int slot) {
        Hit hit = new Hit(x,y,width,22,slot); hits.add(hit);
        button(gui,font,x,y,width,Component.literal(font.plainSubstrByWidth(label,width-8)),hovered(hit));
        if (hovered(hit)) uiHint = slot >= 0 ? RaidUiItems.hint(item(slot)) : List.of(Component.literal(switch (slot) {
            case START -> "返回开始游戏界面";
            case HOME -> "进入家园 / 管理访问申请";
            case DAM, SPACE -> "点击切换是否参与匹配";
            default -> "关闭行动中心";
        }));
    }
    private int maps(GuiGraphics gui, int left, int right, int y) {
        boolean choose = has(10) && has(11) && has(13) && has(14);
        if (choose) {
            y = card(gui,left,right,y,"家园","私人区域与访问申请",HOME,HOME,false);
            y = card(gui,left,right,y,"零号大坝 · 绝密","行动上限 15 人",DAM,damMatch ? 11 : 10,damMatch);
            y = card(gui,left,right,y,"航天基地 · 绝密","行动上限 24 人",SPACE,spaceMatch ? 14 : 13,spaceMatch);
        }
        for (int slot = 0; slot < 54; slot++) {
            if (!has(slot) || slot == 0 || slot == 1 || choose && (slot==10 || slot==11 || slot==13 || slot==14)) continue;
            action(gui,left,y,right-left,item(slot).getHoverName().getString(),slot); y += 26;
        }
        return y;
    }
    private int card(GuiGraphics gui, int left, int right, int y, String name, String detail, int toggle, int slot, boolean enabled) {
        panel(gui,left,y,right-left,65,ROW); gui.fill(left,y,left+3,y+65,0xFF67BBA8);
        text(gui,name,left+12,y+8,right-left-152,TEXT); text(gui,detail,left+12,y+23,right-left-152,MUTED);
        if (toggle != HOME) action(gui,left+12,y+39,120,"匹配："+(enabled ? "开启" : "关闭"),toggle);
        action(gui,right-118,y+35,106,toggle == HOME ? "进入家园" : "组队进入",slot); return y+72;
    }
    private int assets(GuiGraphics gui,int left,int right,int start) {
        int gap=16, mid=left+(right-left)/2, cell=Math.min(28,(mid-left-gap)/9);
        int firstAction, next;
        switch (menu.pageKind) {
            case PREP -> {
                int contentY=start;
                if (has(50)) for (Component line : RaidUiItems.hint(item(50))) {
                    for (var wrapped : font.split(line,right-left)) { gui.drawString(font,wrapped,left,contentY,0xFFFFCE80,false); contentY+=12; }
                }
                start=contentY; next=grid(gui,left,start,cell,"备战物品",0,36);
                next=grid(gui,left,next,cell,"穿戴装备与副手",36,5); firstAction=41;
            }
            case STORAGE -> { next=start; firstAction=53; }
            case WAREHOUSE, PURCHASES -> {
                next=grid(gui,left,start,cell,menu.pageKind==Page.WAREHOUSE?"仓库库存":"可购商品",0,45); firstAction=45;
            }
            case EQUIPMENT -> { next=grid(gui,left,start,cell,"当前穿戴",0,5); firstAction=5; }
            default -> { next=start; firstAction=0; }
        }
        int inventoryEnd;
        if (menu.pageKind == Page.PURCHASES) {
            inventoryEnd=grid(gui,mid+4,start,cell,"备战物品背包",90,36);
            inventoryEnd=grid(gui,mid+4,inventoryEnd,cell,"备战装备与副手",126,5);
        } else {
            inventoryEnd=grid(gui,mid+4,start,cell,"随身背包",54,27);
            inventoryEnd=grid(gui,mid+4,inventoryEnd,cell,"快捷栏",81,9);
            if (menu.pageKind==Page.PREP) inventoryEnd=grid(gui,mid+4,inventoryEnd,cell,"随身装备与副手",144,5);
        }
        if (menu.pageKind == Page.PREP || menu.pageKind == Page.STORAGE) {
            int base = menu.pageKind == Page.PREP ? RaidPanelMenu.PREP_STORAGE_BASE : 0;
            int top = menu.pageKind == Page.PREP ? Math.max(next, inventoryEnd) + 5 : start;
            String owned = has(base + 51) ? item(base + 51).getHoverName().getString() : "正在读取储物权益…";
            int column = (right-left)/2-6;
            text(gui, "副本储物 / 扩容", left, top, column, TEXT);
            int row = top + 16;
            for (var line : font.split(Component.literal(owned), menu.pageKind == Page.PREP ? right-left : column)) {
                gui.drawString(font, line, left, row, MUTED, false); row += font.lineHeight + 2;
            }
            next = grid(gui, left, row + 4, cell, "安全箱 / 失败保护", base, 9);
            next = grid(gui, left, next, cell, has(base+52) ? item(base+52).getHoverName().getString() : "拓展容纳格", base+9, 36);
            for (int offset = 45; offset <= 46; offset++) if (has(base+offset)) {
                action(gui, left+(offset-45)*(column/2), next, column/2-2, item(base+offset).getHoverName().getString(), base+offset);
            }
            next += 26;
            int shop = menu.pageKind == Page.PREP ? row + 4 : inventoryEnd + 4;
            text(gui, "购买拓展格与安全箱", mid+4, shop, column, TEXT); shop += 16;
            for (int offset=47; offset<=50; offset++) if (has(base+offset)) {
                action(gui, mid+4, shop, column, item(base+offset).getHoverName().getString(), base+offset); shop+=26;
            }
            inventoryEnd = shop;
            if (menu.pageKind == Page.STORAGE) return Math.max(next, inventoryEnd);
        }
        int controls=Math.max(next,inventoryEnd)+5;
        text(gui,"操作",left,controls,right-left,TEXT); controls+=16;
        int index=0;
        for (int slot=firstAction;slot<53;slot++) if (has(slot)) {
            action(gui,(index%2==0?left:mid+4),controls+index/2*26,(right-left)/2-6,item(slot).getHoverName().getString(),slot); index++;
        }
        return controls+(index+1)/2*26;
    }
    private int grid(GuiGraphics gui,int left,int top,int cell,String label,int first,int count) {
        text(gui,label,left,top,9*cell,TEXT); int y=top+16;
        for (int index=0;index<count;index++) {
            boolean inventoryEquipment = count == 5 && (first == 36 || first == 126 || first == 144);
            int slot = first + (inventoryEquipment && index < 4 ? 3 - index : index);
            int x=left+index%9*cell,row=y+index/9*cell;
            Hit hit=new Hit(x,row,cell-1,cell-1,slot); hits.add(hit);
            ItemStack stack=item(slot);
            boolean control = (slot < 54 || slot >= RaidPanelMenu.PREP_STORAGE_BASE) && RaidUiItems.isControl(stack);
            boolean locked = control && stack.is(net.minecraft.world.item.Items.BLACK_STAINED_GLASS_PANE);
            panel(gui,x,row,cell-1,cell-1,hovered(hit)?GROUP:locked?PANEL:ROW);
            if (locked) gui.drawCenteredString(font,"×",x+cell/2,row+(cell-9)/2,MUTED);
            else if (!stack.isEmpty() && !control) {
                int ix=x+(cell-16)/2,iy=row+(cell-16)/2; gui.renderItem(stack,ix,iy); gui.renderItemDecorations(font,stack,ix,iy);
            }
            if (hovered(hit)) {
                if (control) uiHint = RaidUiItems.hint(stack); else hover = stack;
            }
        }
        return y+(count+8)/9*cell+10;
    }
    private void dragTo(double my) {
        double y=(my-originY)/scale; int height=bottom()-TOP, thumb=Math.max(16,height*height/(height+maxScroll));
        scroll=(int)(Math.max(0,Math.min(1,(y-TOP-thumb*.5)/Math.max(1,height-thumb)))*maxScroll);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if (search != null) { if (search.mouseClicked(mx,my,button)) { setFocused(search); return true; } search.setFocused(false); }
        double x=(mx-originX)/scale,y=(my-originY)/scale;
        if (button==0 && maxScroll>0 && x>=right()+2 && x<right()+11 && y>=TOP && y<bottom()) { dragging=true; dragTo(my); return true; }
        if (button!=0 && button!=1) return false;
        for (Hit hit:hits) if (hit.contains(x,y) && (hit.slot==CLOSE || hit.slot==START || y>=TOP && y<bottom())) {
            if (hit.slot<0 && button!=0) return true;
            if (button == 0 && !hasShiftDown() && itemSlot(hit.slot) && has(hit.slot) && !RaidUiItems.isControl(item(hit.slot))) {
                pressed=hit.slot; pressX=mx; pressY=my; return true;
            }
            switch(hit.slot) {
                case START -> returnToStart();
                case CLOSE -> onClose();
                case HOME -> {
                    if (minecraft.player != null && minecraft.player.containerMenu == menu) {
                        minecraft.player.connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(menu.containerId));
                        minecraft.player.containerMenu = minecraft.player.inventoryMenu;
                        minecraft.setScreen(HomeScreen.fromMapSelection());
                    }
                }
                case DAM -> damMatch=!damMatch;
                case SPACE -> spaceMatch=!spaceMatch;
                default -> { if (minecraft.player!=null && minecraft.gameMode!=null && minecraft.player.containerMenu==menu)
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId,hit.slot,button,hasShiftDown() && menu.pageKind!=Page.MAPS ? ClickType.QUICK_MOVE : ClickType.PICKUP,minecraft.player); }
            }
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) { if (!dragging) return false; dragTo(my); return true; }
    private boolean itemSlot(int slot) {
        if (menu.pageKind==Page.PREP) return slot>=0 && slot<41 || slot>=54 && slot<135 || slot>=144 && slot<149;
        return menu.pageKind==Page.STORAGE && (slot>=0 && slot<45 || slot>=54 && slot<90);
    }
    private void click(int slot, ClickType type) {
        if (minecraft.player!=null && minecraft.gameMode!=null && minecraft.player.containerMenu==menu)
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId,slot,0,type,minecraft.player);
    }
    @Override public void tick() {
        if (pendingClick>=0 && pressed<0 && net.minecraft.Util.getMillis()-clickedAt>250) {
            int slot=pendingClick; pendingClick=-1; click(slot,ClickType.PICKUP);
        }
    }
    @Override public boolean mouseReleased(double mx,double my,int button) {
        if (button==0 && pressed>=0) {
            int source=pressed; pressed=-1; double x=(mx-originX)/scale,y=(my-originY)/scale;
            for (Hit hit:hits) if (y>=TOP && y<bottom() && hit.contains(x,y) && itemSlot(hit.slot)) {
                if (source!=hit.slot) {
                    pendingClick=-1;
                    if (minecraft.player!=null && minecraft.player.containerMenu==menu) minecraft.player.connection.sendCommand("raid panel "+menu.containerId+" drop "+source+" "+hit.slot);
                } else if (pendingClick==source && net.minecraft.Util.getMillis()-clickedAt<=250) {
                    pendingClick=-1; click(source, ClickType.QUICK_MOVE);
                } else { pendingClick=source; clickedAt=net.minecraft.Util.getMillis(); }
                break;
            }
            return true;
        }
        boolean was=dragging; dragging=false; return was;
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta) { scroll=Math.max(0,Math.min(maxScroll,scroll-(int)Math.signum(delta)*30)); return true; }
    private void returnToStart() {
        if (minecraft.player!=null && minecraft.player.containerMenu==menu) {
            minecraft.player.connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(menu.containerId));
            minecraft.player.containerMenu=minecraft.player.inventoryMenu;
            mccfk.DeltaForce.java.client.screen.DFGUI_MAIN.openToStart();
        }
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if (key == 256 && (menu.pageKind == Page.PREP || menu.pageKind == Page.MAPS)) {
            if (menu.pageKind == Page.PREP && prepFromMaps && minecraft.player != null && minecraft.player.containerMenu == menu)
                minecraft.player.connection.sendCommand("raid");
            else returnToStart();
            return true;
        }
        if (search!=null && search.isFocused()) {
            if (key==257 || key==335) {
                if (minecraft.player != null && minecraft.player.containerMenu == menu) {
                    String text = search.getValue().strip().toLowerCase(java.util.Locale.ROOT);
                    java.util.BitSet matches = new java.util.BitSet();
                    if (!text.isEmpty()) for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                        int index = net.minecraft.world.item.Item.getId(item);
                        if (index >= 0 && index < 262144 && Component.translatable(item.getDescriptionId()).getString().toLowerCase(java.util.Locale.ROOT).contains(text)) matches.set(index);
                    }
                    com.rzy.dealt_force_skills.network.NetworkHandler.sendToServer(new com.rzy.dealt_force_skills.network.C2S_RaidShopSearch(menu.containerId, text, matches.toLongArray()));
                }
                return true;
            }
            if (key!=256) { search.keyPressed(key,scan,mods); return true; }
        }
        if (minecraft.options.keyInventory.matches(key,scan)) { onClose(); return true; } return super.keyPressed(key,scan,mods);
    }
    @Override public void onClose() { if (minecraft.player!=null) minecraft.player.closeContainer(); }
}
