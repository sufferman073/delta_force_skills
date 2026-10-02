package mccfk.DeltaForce.java.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.character.CharacterBranchPackManager;
import com.rzy.dealt_force_skills.network.C2S_SpecialOperationsBaseAction;
import com.rzy.dealt_force_skills.network.C2S_StartPageAction;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_StartPageSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * StartGameScreen - 开始游戏子界面
 * 在屏幕中央绘制玩家模型（类似背包界面但不跟随鼠标）
 * 底部绘制功能按钮：左侧原有7按钮 + 右侧新增：排行榜→招募→社交→部署×3
 */
public class StartGameScreen {

    // 按钮贴图路径
    private static final ResourceLocation BTN_BUTTON     = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/button.png"); // 128x48
    private static final ResourceLocation BTN_CHAT       = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/chat.png"); // 64x64
    private static final ResourceLocation BTN_COLLECTION = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/collection.png"); // 64x64
    private static final ResourceLocation BTN_SHOP       = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/shop.png"); // 64x64
    private static final ResourceLocation BTN_MARKET     = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/market.png"); // 64x64
    private static final ResourceLocation BTN_PASS       = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/pass.png"); // 64x64
    private static final ResourceLocation BTN_EVENT      = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/event.png"); // 64x64

    private static final ResourceLocation[] BTN_TEXTURES = {
        BTN_BUTTON, BTN_CHAT, BTN_COLLECTION, BTN_SHOP, BTN_MARKET, BTN_PASS, BTN_EVENT
    };

    // === 右侧新增按钮贴图 ===
    private static final ResourceLocation BTN_LEADERBOARD = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/leaderboard.png");
    private static final ResourceLocation BTN_RECRUIT     = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/recruit.png");
    private static final ResourceLocation BTN_SOCIAL      = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/social.png");
    private static final ResourceLocation BTN_PLAYER_SLOTS = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/player_slots.png");

    // === 右上角信息区贴图 ===
    private static final ResourceLocation ICON_SETTINGS   = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/settings.png");
    private static final ResourceLocation ICON_MAIL       = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/mail.png");
    private static final ResourceLocation ICON_NETWORK    = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/network_latency.png");
    private static final ResourceLocation ICON_DELTA_COIN = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/coin/delta_coin.png");
    private static final ResourceLocation ICON_HAVOC_COIN = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/coin/havoc_coin.png");

    // 从左到右：排行榜 → 招募 → 社交 → 玩家头像1（队友） → 玩家头像2（队友） → 玩家头像3（我的头像）
    private static final ResourceLocation[] NEW_BTN_TEXTURES = {
        BTN_LEADERBOARD, BTN_RECRUIT, BTN_SOCIAL, BTN_PLAYER_SLOTS, BTN_PLAYER_SLOTS, BTN_PLAYER_SLOTS
    };
    private static final int NEW_BTN_COUNT = 6;

    private static final int BTN_COUNT = 7;
    private static final int BAR_HEIGHT = 20;
    private static final int FIRST_X_RATIO = 3;     // 首个按钮距左边缘 = 屏幕宽度 * 3 / 100
    private static final int[] GAP_PATTERN = {7, 6, 6};

    private static final int BUTTON_TEX_W = 128;
    private static final int BUTTON_TEX_H = 48;
    private static final int ICON_TEX_SIZE = 64;

    private static final int BUTTON_DRAW_W = 80;
    private static final int BUTTON_DRAW_H = 20;
    private static final int ICON_DRAW_SIZE = 20;

    private static final float BUTTON_TEX_SCALE = 0.5F;
    private static final float ICON_TEX_SCALE = 0.45F;

    // === 右侧按钮布局常量 ===
    private static final int MARGIN_RIGHT_RATIO = 3;  // 距右边缘 = 屏幕宽度 * 3 / 100
    private static final int GAP_DEPLOY = 3;
    private static final int GAP_SOCIAL_DEPLOY = 7;
    private static final int GAP_RECRUIT_SOCIAL = 6;
    private static final int GAP_LEADERBOARD_RECRUIT = 6;
    private static final int SOCIAL_DRAW_W = 30;
    private static final float DEPLOY_TEX_SCALE = ICON_TEX_SCALE * 0.5F;  // 部署贴图缩小0.5倍

    // === 玩法模式面板贴图（主界面入口，点击打开玩法选择界面）===
    private static final ResourceLocation BTN_PRE_BATTLE_REP = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/pre_battle_rep.png");
    private static final ResourceLocation BTN_NO_TARGET = ResourceLocation.fromNamespaceAndPath("deltaforce", "textures/gui/start_game/no_target.png");

    // 原始尺寸
    private static final int PRE_BATTLE_REP_ORIG_W = 496;
    private static final int PRE_BATTLE_REP_ORIG_H = 57;
    private static final int NO_TARGET_ORIG_W = 496;
    private static final int NO_TARGET_ORIG_H = 190;

    // 缩放比例（0.4倍）
    private static final float MODE_PANEL_SCALE = 0.4F;

    // 实际绘制尺寸
    private static final int PRE_BATTLE_REP_W = (int)(PRE_BATTLE_REP_ORIG_W * MODE_PANEL_SCALE);
    private static final int PRE_BATTLE_REP_H = (int)(PRE_BATTLE_REP_ORIG_H * MODE_PANEL_SCALE);
    private static final int NO_TARGET_W = (int)(NO_TARGET_ORIG_W * MODE_PANEL_SCALE);
    private static final int NO_TARGET_H = (int)(NO_TARGET_ORIG_H * MODE_PANEL_SCALE);

    private static final int MODE_PANEL_MARGIN_RIGHT_RATIO = 2;  // 距右边缘 = 屏幕宽度 * 2 / 100
    private static final int PRE_BATTLE_REP_BOTTOM_OFFSET = 10;
    private static final int MODE_PANEL_GAP = 1;

    // === 右上角信息区布局常量 ===
    private static final int TOP_RIGHT_MARGIN_X_RATIO = 1;  // 距右边缘 = 屏幕宽度 * 1 / 100
    private static final int TOP_RIGHT_MARGIN_Y = 10;        // 距上边缘（固定值，窄窗口自动切换为40）
    private static final int TOP_RIGHT_ICON_SIZE = 20;       // 图标绘制尺寸
    private static final int TOP_RIGHT_GAP = 4;              // 元素间距
    private static final int TOP_RIGHT_HAVOC_SHIFT = 15;         // 哈夫币数量/图标整体右移基准像素
    private static final int TOP_RIGHT_HAVOC_ICON_LEFT = 10;     // 哈夫币图标在基准上再往左像素
    private static final int TOP_RIGHT_HAVOC_TEXT_LEFT = 13;     // 哈夫币数量文本在基准上再往左像素
    private static final int OPERATOR_TAB_INDEX = 2;    // 特战干员标签索引（货币显示在该标签页隐藏）

    private static S2C_StartPageSnapshot snapshot = emptySnapshot();
    private static PanelMode panelMode = PanelMode.NONE;
    private static final List<PanelButton> panelButtons = new ArrayList<>();
    private static int panelScroll;
    private static int panelMaxScroll;
    private static UUID contextMember;
    private static int contextX;
    private static int contextY;

    private enum PanelMode { NONE, ROSTER, SQUAD, INVITE }
    private enum PanelAction {
        CLOSE, CREATE, JOIN, INVITE, ACCEPT_INVITE, DECLINE_INVITE,
        ACCEPT_JOIN, DECLINE_JOIN, LEAVE, KICK, PROMOTE
    }
    private record PanelButton(int x, int y, int width, int height, PanelAction action,
                               UUID playerId, String teamName) {
    }

    /**
     * 获取全部在线玩家数（供其他界面读取）
     */
    public static int getOnlineFriendsCount() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null ? connection.getOnlinePlayers().size() : 0;
    }

    public static void acceptSnapshot(S2C_StartPageSnapshot value) {
        snapshot = value == null ? emptySnapshot() : value;
        panelScroll = Math.min(panelScroll, Math.max(0, panelMaxScroll));
    }

    public static void requestSnapshot() {
        NetworkHandler.sendToServer(C2S_StartPageAction.refresh());
    }

    public static void resetSessionState() {
        snapshot = emptySnapshot();
        panelMode = PanelMode.NONE;
        panelButtons.clear();
        panelScroll = 0;
        panelMaxScroll = 0;
        contextMember = null;
    }

    public static void closeOverlayPanels() {
        panelMode = PanelMode.NONE;
        panelButtons.clear();
        panelScroll = 0;
        panelMaxScroll = 0;
        contextMember = null;
    }

    /**
     * 在指定区域绘制"制作中"红叉遮罩，悬浮时显示"制作中"提示
     */
    public static void renderWipOverlay(GuiGraphics guiGraphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        // 红色描边，表示"框架已预留、功能开发中"
        int border = Math.max(1, Math.min(w, h) / 16);
        // 上边
        guiGraphics.fill(x, y, x + w, y + border, 0xFFFF3333);
        // 下边
        guiGraphics.fill(x, y + h - border, x + w, y + h, 0xFFFF3333);
        // 左边
        guiGraphics.fill(x, y, x + border, y + h, 0xFFFF3333);
        // 右边
        guiGraphics.fill(x + w - border, y, x + w, y + h, 0xFFFF3333);
        // 悬浮提示
        if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
            Component tip = Component.translatable("deltaforce.wip.making");
            guiGraphics.renderTooltip(Minecraft.getInstance().font, tip, mouseX, mouseY);
        }
    }

    /**
     * 渲染底栏按钮（左侧原有按钮仅开始游戏显示 + 右侧新增按钮常显）
     * @param guiGraphics GUI上下文
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param screenWidth 屏幕宽
     * @param screenHeight 屏幕高
     * @param selectedTab 当前选中的标签页索引
     * @return 悬浮的按钮索引(-1表示无)
     */
    public static int renderBottomBarButtons(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight, int selectedTab) {
        int btnY = screenHeight - BAR_HEIGHT;
        int hovered = -1;
        var font = Minecraft.getInstance().font;

        boolean showLeftButtons = (selectedTab == 0); // 左侧按钮仅开始游戏标签页显示

        // === 左侧原有按钮（仅开始游戏标签页显示，从左到右）===
        if (showLeftButtons) {
            int currentX = screenWidth * FIRST_X_RATIO / 100;
            int leftButtonCount = screenWidth < 430 ? 1 : BTN_COUNT;
            for (int i = 0; i < leftButtonCount; i++) {
                int x = currentX;
                int y = btnY;
                int drawW, drawH, texSrcW, texSrcH;

                if (i == 0) {
                    drawW = BUTTON_DRAW_W;
                    drawH = BUTTON_DRAW_H;
                    texSrcW = (int)(BUTTON_TEX_W * BUTTON_TEX_SCALE);
                    texSrcH = (int)(BUTTON_TEX_H * BUTTON_TEX_SCALE);
                } else {
                    drawW = ICON_DRAW_SIZE;
                    drawH = ICON_DRAW_SIZE;
                    texSrcW = (int)(ICON_TEX_SIZE * ICON_TEX_SCALE);
                    texSrcH = (int)(ICON_TEX_SIZE * ICON_TEX_SCALE);
                }

                int texX = x + (drawW - texSrcW) / 2;
                int texY = y + (drawH - texSrcH) / 2;

                guiGraphics.blit(BTN_TEXTURES[i], texX, texY, 0, 0, texSrcW, texSrcH, texSrcW, texSrcH);

                if (i == 0) {
                    Component text = Component.literal("进入特勤处");
                    int textWidth = font.width(text);
                    float textScale = 0.7F;
                    int scaledTextWidth = (int)(textWidth * textScale);
                    int textX = x + drawW - scaledTextWidth - 15;
                    int textY = y + (drawH - (int)(font.lineHeight * textScale)) / 2;

                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(textX, textY, 0);
                    guiGraphics.pose().scale(textScale, textScale, 1.0F);
                    guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);
                    guiGraphics.pose().popPose();
                }

                if (mouseX >= x && mouseX < x + drawW && mouseY >= y && mouseY < y + drawH) {
                    hovered = i;
                }

                // 按钮1-6无实际功能，绘制红叉遮罩
                if (i > 0) {
                    renderWipOverlay(guiGraphics, x, y, drawW, drawH, mouseX, mouseY);
                }

                currentX = x + drawW + GAP_PATTERN[i % GAP_PATTERN.length];
            }
        }

        // === 右侧新增按钮（常显；从右往左：部署3→部署2→部署1→社交→招募→排行榜）===
        int rightEdge = screenWidth - screenWidth * MARGIN_RIGHT_RATIO / 100;
        // 从右到左计算新按钮位置
        int[] nx = new int[NEW_BTN_COUNT];
        int[] nw = new int[NEW_BTN_COUNT];

        // 部署3、部署2、部署1 按钮大小与普通图标一致(20x20)，仅贴图显示缩小
        nx[5] = rightEdge - ICON_DRAW_SIZE;                               // 部署3
        nw[5] = ICON_DRAW_SIZE;
        nx[4] = nx[5] - GAP_DEPLOY - ICON_DRAW_SIZE;                      // 部署2
        nw[4] = ICON_DRAW_SIZE;
        nx[3] = nx[4] - GAP_DEPLOY - ICON_DRAW_SIZE;                      // 部署1
        nw[3] = ICON_DRAW_SIZE;
        nx[2] = nx[3] - GAP_SOCIAL_DEPLOY - SOCIAL_DRAW_W;                 // 社交
        nw[2] = SOCIAL_DRAW_W;
        nx[1] = nx[2] - GAP_RECRUIT_SOCIAL - ICON_DRAW_SIZE;               // 招募
        nw[1] = ICON_DRAW_SIZE;
        nx[0] = nx[1] - GAP_LEADERBOARD_RECRUIT - ICON_DRAW_SIZE;          // 排行榜
        nw[0] = ICON_DRAW_SIZE;

        for (int i = 0; i < NEW_BTN_COUNT; i++) {
            int x = nx[i];
            int drawW = nw[i];
            int drawH = ICON_DRAW_SIZE;
            float texScale = (i >= 3) ? DEPLOY_TEX_SCALE : ICON_TEX_SCALE;
            int texSrc = (int)(ICON_TEX_SIZE * texScale);
            int y = btnY;

            int texX = x + (drawW - texSrc) / 2;
            int texY = y + (drawH - texSrc) / 2;

            guiGraphics.blit(NEW_BTN_TEXTURES[i], texX, texY, 0, 0, texSrc, texSrc, texSrc, texSrc);

            // 社交按钮(新按钮索引2)内部靠右侧显示好友在线数变量
            if (i == 2) {
                Component friendsText = Component.literal(String.valueOf(getOnlineFriendsCount()));
                int textWidth = font.width(friendsText);
                float textScale = 0.55F;
                int scaledTextWidth = (int)(textWidth * textScale);
                int textX = x + drawW - scaledTextWidth - 2;   // 靠右2px
                int textY = y + (drawH - (int)(font.lineHeight * textScale)) / 2; // 垂直居中

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(textX, textY, 0);
                guiGraphics.pose().scale(textScale, textScale, 1.0F);
                guiGraphics.drawString(font, friendsText, 0, 0, 0xFFFFFFFF, false);
                guiGraphics.pose().popPose();
            }

            if (mouseX >= x && mouseX < x + drawW && mouseY >= y && mouseY < y + drawH) {
                hovered = i + 100;
            }

            if (i >= 3) {
                int memberIndex = i - 3;
                if (memberIndex < snapshot.members().size()) {
                    renderMemberHead(guiGraphics, snapshot.members().get(memberIndex), x + 2, y + 2, drawH - 4);
                }
            }
        }

        // 绘制悬浮白色视觉反馈
        if (hovered >= 0) {
            if (hovered < 100) {
                int hx = screenWidth * FIRST_X_RATIO / 100;
                int leftButtonCount = screenWidth < 430 ? 1 : BTN_COUNT;
                for (int i = 0; i < leftButtonCount; i++) {
                    int hw = (i == 0) ? BUTTON_DRAW_W : ICON_DRAW_SIZE;
                    if (i == hovered) {
                        guiGraphics.fill(hx, btnY, hx + hw, btnY + ICON_DRAW_SIZE, 0x80FFFFFF);
                        break;
                    }
                    hx += hw + GAP_PATTERN[i % GAP_PATTERN.length];
                }
            } else {
                int ni = hovered - 100;
                guiGraphics.fill(nx[ni], btnY, nx[ni] + nw[ni], btnY + ICON_DRAW_SIZE, 0x80FFFFFF);
            }
        }

        if (hovered >= 103 && hovered <= 105) {
            int memberIndex = hovered - 103;
            if (memberIndex < snapshot.members().size()) {
                S2C_StartPageSnapshot.MemberEntry member = snapshot.members().get(memberIndex);
                renderSimpleTooltip(guiGraphics, font,
                        member.name() + (member.online() ? "" : "（离线）"), mouseX, mouseY, screenWidth, screenHeight);
            }
        }

        return hovered;
    }

    public static void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        panelButtons.clear();
        if (panelMode != PanelMode.NONE) {
            int[] bounds = panelBounds(screenWidth, screenHeight);
            int x = bounds[0], y = bounds[1], width = bounds[2], height = bounds[3];
            Font font = Minecraft.getInstance().font;
            guiGraphics.fill(x, y, x + width, y + height, 0xFF14191C);
            guiGraphics.fill(x, y, x + width, y + 1, 0xFF58C8D8);
            guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF29383D);
            guiGraphics.fill(x, y, x + 1, y + height, 0xFF29383D);
            guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF29383D);
            String title = switch (panelMode) {
                case ROSTER -> "在线干员状态";
                case SQUAD -> "长期生存队伍";
                case INVITE -> "邀请在线玩家";
                default -> "";
            };
            guiGraphics.drawString(font, title, x + 9, y + 8, 0xFFFFFFFF, false);
            addPanelButton(guiGraphics, font, x + width - 20, y + 5, 15, 15, "×", PanelAction.CLOSE, null, "");
            switch (panelMode) {
                case ROSTER -> renderRosterPanel(guiGraphics, font, x, y, width, height);
                case SQUAD -> renderSquadPanel(guiGraphics, font, x, y, width, height);
                case INVITE -> renderInvitePanel(guiGraphics, font, x, y, width, height);
                default -> {
                }
            }
            renderPanelScrollbar(guiGraphics, x, y, width, height);
        }
        renderContextMenu(guiGraphics, screenWidth, screenHeight);
    }

    public static void renderOverlayOnTop(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
        try {
            renderOverlay(guiGraphics, mouseX, mouseY, screenWidth, screenHeight);
            guiGraphics.flush();
        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    public static boolean hasOpenOverlay() {
        return panelMode != PanelMode.NONE || contextMember != null;
    }

    public static boolean handleOverlayClick(int mouseX, int mouseY, int button, int screenWidth, int screenHeight) {
        for (PanelButton hit : List.copyOf(panelButtons)) {
            if (button == 0 && contains(hit, mouseX, mouseY)) {
                dispatchPanelAction(hit);
                return true;
            }
        }
        if (contextMember != null) contextMember = null;
        if (panelMode != PanelMode.NONE) {
            int[] bounds = panelBounds(screenWidth, screenHeight);
            return mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3];
        }
        return false;
    }

    public static boolean handleBottomButtonClick(int mouseX, int mouseY, int button,
                                                  int screenWidth, int screenHeight, int selectedTab) {
        int hit = bottomButtonAt(mouseX, mouseY, screenWidth, screenHeight, selectedTab);
        if (hit < 0) return false;
        if (hit == 0 && button == 0) {
            panelMode = PanelMode.NONE;
            DFGUI_MAIN.openToLogistics();
            return true;
        }
        if (hit == 100 && button == 0) { togglePanel(PanelMode.ROSTER); return true; }
        if (hit == 101 && button == 0) { togglePanel(PanelMode.SQUAD); return true; }
        if (hit == 102 && button == 0) { togglePanel(PanelMode.INVITE); return true; }
        if (hit >= 103 && hit <= 105) {
            int memberIndex = hit - 103;
            if (memberIndex < snapshot.members().size()) {
                S2C_StartPageSnapshot.MemberEntry member = snapshot.members().get(memberIndex);
                if (button == 1 && isLocalLeader() && !isLocalPlayer(member.playerId())) {
                    contextMember = member.playerId(); contextX = mouseX; contextY = mouseY;
                } else if (button == 0) {
                    togglePanel(PanelMode.SQUAD);
                }
            }
            return true;
        }
        return true;
    }

    public static boolean handlePanelScroll(double delta) {
        if (panelMode == PanelMode.NONE || delta == 0.0D) return false;
        panelScroll = Math.max(0, Math.min(panelMaxScroll, panelScroll + (delta < 0.0D ? 1 : -1)));
        return true;
    }

    private static void renderRosterPanel(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height) {
        int bodyY = y + 29, rowHeight = 22;
        int visible = Math.max(1, (height - 36) / rowHeight);
        panelMaxScroll = Math.max(0, snapshot.roster().size() - visible);
        panelScroll = Math.min(panelScroll, panelMaxScroll);
        int nameX = x + 8;
        int characterX = x + Math.max(56, width * 28 / 100);
        int statusX = x + Math.max(112, width * 55 / 100);
        int recordX = Math.min(x + width - 48, x + Math.max(164, width * 76 / 100));
        guiGraphics.drawString(font, "玩家", nameX, bodyY, 0xFF8FA5AB, false);
        guiGraphics.drawString(font, "干员", characterX, bodyY, 0xFF8FA5AB, false);
        guiGraphics.drawString(font, "生命 / 护甲", statusX, bodyY, 0xFF8FA5AB, false);
        guiGraphics.drawString(font, "游玩 / 死亡", recordX, bodyY, 0xFF8FA5AB, false);
        bodyY += 12;
        for (int index = panelScroll; index < snapshot.roster().size() && index < panelScroll + visible; index++) {
            S2C_StartPageSnapshot.RosterEntry entry = snapshot.roster().get(index);
            int rowY = bodyY + (index - panelScroll) * rowHeight;
            if (((index - panelScroll) & 1) == 0) guiGraphics.fill(x + 5, rowY - 2, x + width - 5, rowY + 18, 0x382B363A);
            guiGraphics.drawString(font, fit(font, entry.name(), Math.max(54, characterX - nameX - 5)), nameX, rowY + 3, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, fit(font, characterName(entry.characterId()), Math.max(54, statusX - characterX - 5)), characterX, rowY + 3, 0xFFB9E4EA, false);
            String health = Math.round(entry.health()) + "/" + Math.round(entry.maxHealth()) + "  A" + entry.armor();
            guiGraphics.drawString(font, fit(font, health, Math.max(54, recordX - statusX - 5)), statusX, rowY + 3,
                    healthColor(entry.health(), entry.maxHealth()), false);
            guiGraphics.drawString(font, fit(font, formatPlayTime(entry.playTimeTicks()) + "  D" + entry.deaths(),
                    Math.max(32, x + width - recordX - 7)), recordX, rowY + 3, 0xFFD0D5D6, false);
        }
    }

    private static void renderSquadPanel(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height) {
        int bodyY = y + 48, rowHeight = 24;
        int visible = Math.max(1, (height - 56) / rowHeight);
        if (snapshot.teamName().isEmpty()) {
            addPanelButton(guiGraphics, font, x + width - 86, y + 25, 76, 17, "创建队伍", PanelAction.CREATE, null, "");
            int total = snapshot.invitations().size() + snapshot.teams().size();
            panelMaxScroll = Math.max(0, total - visible);
            panelScroll = Math.min(panelScroll, panelMaxScroll);
            if (total == 0) {
                guiGraphics.drawString(font, "暂无可加入的队伍，创建后可邀请在线玩家。", x + 9, bodyY + 18, 0xFF9DA9AC, false);
                return;
            }
            for (int item = panelScroll; item < total && item < panelScroll + visible; item++) {
                int rowY = bodyY + (item - panelScroll) * rowHeight;
                guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + 21, 0x3A2A3438);
                if (item < snapshot.invitations().size()) {
                    S2C_StartPageSnapshot.InviteEntry invite = snapshot.invitations().get(item);
                    guiGraphics.drawString(font, fit(font, "邀请：" + invite.teamName() + " / " + invite.leaderName(), Math.max(60, width - 132)), x + 10, rowY + 6, 0xFFFFD36A, false);
                    addPanelButton(guiGraphics, font, x + width - 112, rowY + 3, 48, 16, "接受", PanelAction.ACCEPT_INVITE, null, invite.teamName());
                    addPanelButton(guiGraphics, font, x + width - 59, rowY + 3, 48, 16, "拒绝", PanelAction.DECLINE_INVITE, null, invite.teamName());
                } else {
                    S2C_StartPageSnapshot.TeamEntry team = snapshot.teams().get(item - snapshot.invitations().size());
                    guiGraphics.drawString(font, fit(font, team.name() + "  队长 " + team.leaderName() + "  " + team.memberCount() + "/3", Math.max(60, width - 88)), x + 10, rowY + 6, 0xFFFFFFFF, false);
                    addPanelButton(guiGraphics, font, x + width - 76, rowY + 3, 65, 16,
                            team.joinRequested() ? "已申请" : "申请加入", PanelAction.JOIN, null, team.name());
                }
            }
            return;
        }
        guiGraphics.drawString(font, fit(font, snapshot.teamName() + "  " + snapshot.members().size() + "/3", Math.max(80, width - 110)), x + 9, y + 29, 0xFFB9E4EA, false);
        addPanelButton(guiGraphics, font, x + width - 68, y + 25, 58, 17, "退出队伍", PanelAction.LEAVE, null, "");
        int total = snapshot.members().size() + snapshot.joinRequests().size();
        panelMaxScroll = Math.max(0, total - visible);
        panelScroll = Math.min(panelScroll, panelMaxScroll);
        for (int item = panelScroll; item < total && item < panelScroll + visible; item++) {
            int rowY = bodyY + (item - panelScroll) * rowHeight;
            guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + 21, 0x3A2A3438);
            if (item < snapshot.members().size()) {
                S2C_StartPageSnapshot.MemberEntry member = snapshot.members().get(item);
                boolean leader = member.playerId().equals(snapshot.leaderId());
                guiGraphics.drawString(font, (leader ? "[队长] " : "") + member.name(), x + 10, rowY + 6, leader ? 0xFFFFD36A : 0xFFFFFFFF, false);
                String state = member.online() ? "在线" : "离线";
                guiGraphics.drawString(font, state, x + width - font.width(state) - 12, rowY + 6, member.online() ? 0xFF7BE09B : 0xFF7B8588, false);
            } else {
                S2C_StartPageSnapshot.JoinRequestEntry request = snapshot.joinRequests().get(item - snapshot.members().size());
                guiGraphics.drawString(font, fit(font, "加入申请：" + request.name(), Math.max(60, width - 132)), x + 10, rowY + 6, 0xFF8FDCE7, false);
                addPanelButton(guiGraphics, font, x + width - 112, rowY + 3, 48, 16, "同意", PanelAction.ACCEPT_JOIN, request.playerId(), "");
                addPanelButton(guiGraphics, font, x + width - 59, rowY + 3, 48, 16, "拒绝", PanelAction.DECLINE_JOIN, request.playerId(), "");
            }
        }
    }

    private static void renderPanelScrollbar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (panelMaxScroll <= 0) return;
        int trackX = x + width - 4;
        int trackY = y + 25;
        int trackHeight = Math.max(12, height - 30);
        int thumbHeight = Math.max(12, trackHeight / (panelMaxScroll + 1));
        int thumbY = trackY + Math.round((trackHeight - thumbHeight)
                * (panelScroll / (float) panelMaxScroll));
        guiGraphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0x553B484C);
        guiGraphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xCC8FA5AB);
    }

    private static void renderInvitePanel(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height) {
        int bodyY = y + 31;
        if (snapshot.teamName().isEmpty()) {
            panelMaxScroll = 0; guiGraphics.drawString(font, "请先在旗帜按钮中创建或加入队伍。", x + 9, bodyY + 12, 0xFF9DA9AC, false); return;
        }
        if (!isLocalLeader()) {
            panelMaxScroll = 0; guiGraphics.drawString(font, "只有队长可以邀请玩家。", x + 9, bodyY + 12, 0xFFFFB067, false); return;
        }
        if (snapshot.members().size() >= 3) {
            panelMaxScroll = 0; guiGraphics.drawString(font, "队伍已满（3/3）。", x + 9, bodyY + 12, 0xFFFFB067, false); return;
        }
        List<S2C_StartPageSnapshot.RosterEntry> candidates = snapshot.roster().stream()
                .filter(entry -> !isLocalPlayer(entry.playerId()))
                .filter(entry -> snapshot.members().stream().noneMatch(member -> member.playerId().equals(entry.playerId()))).toList();
        int rowHeight = 23, visible = Math.max(1, (height - 40) / rowHeight);
        panelMaxScroll = Math.max(0, candidates.size() - visible);
        panelScroll = Math.min(panelScroll, panelMaxScroll);
        if (candidates.isEmpty()) {
            guiGraphics.drawString(font, "当前没有可邀请的在线玩家。", x + 9, bodyY + 12, 0xFF9DA9AC, false); return;
        }
        for (int index = panelScroll; index < candidates.size() && index < panelScroll + visible; index++) {
            S2C_StartPageSnapshot.RosterEntry entry = candidates.get(index);
            int rowY = bodyY + (index - panelScroll) * rowHeight;
            guiGraphics.fill(x + 6, rowY, x + width - 6, rowY + 20, 0x3A2A3438);
            guiGraphics.drawString(font, fit(font, entry.name() + "  " + characterName(entry.characterId()), Math.max(60, width - 88)), x + 10, rowY + 6, 0xFFFFFFFF, false);
            addPanelButton(guiGraphics, font, x + width - 67, rowY + 2, 56, 16, "邀请", PanelAction.INVITE, entry.playerId(), "");
        }
    }

    private static void renderContextMenu(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (contextMember == null || snapshot.members().stream().noneMatch(member -> member.playerId().equals(contextMember))) {
            contextMember = null; return;
        }
        Font font = Minecraft.getInstance().font;
        int width = 92, height = 40;
        int x = Math.max(4, Math.min(contextX, screenWidth - width - 4));
        int y = Math.max(4, Math.min(contextY - height, screenHeight - BAR_HEIGHT - height - 2));
        guiGraphics.fill(x, y, x + width, y + height, 0xF2181D20);
        addPanelButton(guiGraphics, font, x + 4, y + 4, width - 8, 14, "设为队长", PanelAction.PROMOTE, contextMember, "");
        addPanelButton(guiGraphics, font, x + 4, y + 22, width - 8, 14, "踢出队伍", PanelAction.KICK, contextMember, "");
    }

    private static void addPanelButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                       String label, PanelAction action, UUID playerId, String teamName) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF263237);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF58C8D8);
        String fitted = fit(font, label, width - 4);
        guiGraphics.drawString(font, fitted, x + Math.max(2, (width - font.width(fitted)) / 2), y + Math.max(2, (height - font.lineHeight) / 2), 0xFFFFFFFF, false);
        panelButtons.add(new PanelButton(x, y, width, height, action, playerId, teamName));
    }

    private static void dispatchPanelAction(PanelButton hit) {
        contextMember = null;
        if (hit.action == PanelAction.CLOSE) { panelMode = PanelMode.NONE; return; }
        C2S_StartPageAction.Action action = switch (hit.action) {
            case CREATE -> C2S_StartPageAction.Action.CREATE;
            case JOIN -> C2S_StartPageAction.Action.REQUEST_JOIN;
            case INVITE -> C2S_StartPageAction.Action.INVITE;
            case ACCEPT_INVITE -> C2S_StartPageAction.Action.ACCEPT_INVITE;
            case DECLINE_INVITE -> C2S_StartPageAction.Action.DECLINE_INVITE;
            case ACCEPT_JOIN -> C2S_StartPageAction.Action.ACCEPT_JOIN;
            case DECLINE_JOIN -> C2S_StartPageAction.Action.DECLINE_JOIN;
            case LEAVE -> C2S_StartPageAction.Action.LEAVE;
            case KICK -> C2S_StartPageAction.Action.KICK;
            case PROMOTE -> C2S_StartPageAction.Action.PROMOTE;
            default -> C2S_StartPageAction.Action.REFRESH;
        };
        NetworkHandler.sendToServer(new C2S_StartPageAction(action, hit.playerId, hit.teamName));
    }

    private static void togglePanel(PanelMode target) {
        panelMode = panelMode == target ? PanelMode.NONE : target;
        panelScroll = 0; contextMember = null;
        if (panelMode != PanelMode.NONE) requestSnapshot();
    }

    private static int bottomButtonAt(int mouseX, int mouseY, int screenWidth, int screenHeight, int selectedTab) {
        int buttonY = screenHeight - BAR_HEIGHT;
        if (mouseY < buttonY || mouseY >= buttonY + BAR_HEIGHT) return -1;
        if (selectedTab == 0) {
            int currentX = screenWidth * FIRST_X_RATIO / 100;
            int leftButtonCount = screenWidth < 430 ? 1 : BTN_COUNT;
            for (int i = 0; i < leftButtonCount; i++) {
                int width = i == 0 ? BUTTON_DRAW_W : ICON_DRAW_SIZE;
                if (mouseX >= currentX && mouseX < currentX + width) return i;
                currentX += width + GAP_PATTERN[i % GAP_PATTERN.length];
            }
        }
        for (int i = 0; i < NEW_BTN_COUNT; i++) {
            int x = newButtonX(i, screenWidth), width = i == 2 ? SOCIAL_DRAW_W : ICON_DRAW_SIZE;
            if (mouseX >= x && mouseX < x + width) return i + 100;
        }
        return -1;
    }

    private static int newButtonX(int index, int screenWidth) {
        int rightEdge = screenWidth - screenWidth * MARGIN_RIGHT_RATIO / 100;
        int slot3 = rightEdge - ICON_DRAW_SIZE;
        int slot2 = slot3 - GAP_DEPLOY - ICON_DRAW_SIZE;
        int slot1 = slot2 - GAP_DEPLOY - ICON_DRAW_SIZE;
        int social = slot1 - GAP_SOCIAL_DEPLOY - SOCIAL_DRAW_W;
        int recruit = social - GAP_RECRUIT_SOCIAL - ICON_DRAW_SIZE;
        int roster = recruit - GAP_LEADERBOARD_RECRUIT - ICON_DRAW_SIZE;
        return switch (index) { case 0 -> roster; case 1 -> recruit; case 2 -> social; case 3 -> slot1; case 4 -> slot2; default -> slot3; };
    }

    private static void renderMemberHead(GuiGraphics guiGraphics, S2C_StartPageSnapshot.MemberEntry member, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(member.playerId());
        guiGraphics.fill(x, y, x + size, y + size, member.online() ? 0xFF31464D : 0xFF2B3032);
        if (info != null) {
            ResourceLocation skin = info.getSkin().texture();
            float scale = size / 8.0F;
            guiGraphics.pose().pushPose(); guiGraphics.pose().translate(x, y, 0.0F); guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.blit(skin, 0, 0, 8, 8, 8, 8, 64, 64);
            guiGraphics.blit(skin, 0, 0, 40, 8, 8, 8, 64, 64);
            guiGraphics.pose().popPose();
        } else {
            String initial = member.name().isEmpty() ? "?" : member.name().substring(0, 1).toUpperCase();
            Font font = minecraft.font;
            guiGraphics.drawString(font, initial, x + (size - font.width(initial)) / 2, y + (size - font.lineHeight) / 2, 0xFFFFFFFF, false);
        }
        if (!member.online()) guiGraphics.fill(x, y, x + size, y + size, 0x78000000);
        int border = member.playerId().equals(snapshot.leaderId()) ? 0xFFFFD36A : 0xFF718087;
        guiGraphics.fill(x, y, x + size, y + 1, border); guiGraphics.fill(x, y + size - 1, x + size, y + size, border);
        guiGraphics.fill(x, y, x + 1, y + size, border); guiGraphics.fill(x + size - 1, y, x + size, y + size, border);
    }

    private static void renderSimpleTooltip(GuiGraphics guiGraphics, Font font, String text, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int width = font.width(text) + 8;
        int x = Math.max(3, Math.min(mouseX + 8, screenWidth - width - 3));
        int y = Math.max(3, Math.min(mouseY - 15, screenHeight - font.lineHeight - 7));
        guiGraphics.fill(x, y, x + width, y + font.lineHeight + 5, 0xF0101416);
        guiGraphics.drawString(font, text, x + 4, y + 3, 0xFFFFFFFF, false);
    }

    private static int[] panelBounds(int screenWidth, int screenHeight) {
        int xMargin = Math.min(16, Math.max(4, screenWidth / 20));
        int width = Math.max(120, Math.min(430, screenWidth - xMargin * 2));
        width = Math.min(width, Math.max(1, screenWidth - 4));
        int y = Math.min(32, Math.max(4, screenHeight / 9));
        int height = Math.max(50, Math.min(230, screenHeight - BAR_HEIGHT - y - 4));
        height = Math.min(height, Math.max(1, screenHeight - y - 2));
        return new int[]{(screenWidth - width) / 2, y, width, height};
    }

    private static boolean contains(PanelButton button, int mouseX, int mouseY) {
        return mouseX >= button.x && mouseX < button.x + button.width && mouseY >= button.y && mouseY < button.y + button.height;
    }

    private static boolean isLocalLeader() {
        return snapshot.leaderId() != null && Minecraft.getInstance().player != null && snapshot.leaderId().equals(Minecraft.getInstance().player.getUUID());
    }

    private static boolean isLocalPlayer(UUID playerId) {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(playerId);
    }

    private static String characterName(String characterId) {
        if (characterId == null || characterId.isBlank()) return "未选择";
        return CharacterBranchPackManager.findCharacter(characterId).map(character -> character.displayName().getString()).orElseGet(() -> {
            int separator = characterId.indexOf(':'); return separator >= 0 ? characterId.substring(separator + 1) : characterId;
        });
    }

    private static int healthColor(float health, float maxHealth) {
        float ratio = maxHealth <= 0.0F ? 0.0F : health / maxHealth;
        return ratio > 0.66F ? 0xFF7BE09B : ratio > 0.33F ? 0xFFFFD36A : 0xFFFF7777;
    }

    private static String formatPlayTime(long ticks) {
        long minutes = Math.max(0L, ticks) / 1200L; return (minutes / 60L) + "h" + (minutes % 60L) + "m";
    }

    private static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String ellipsis = "..."; return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }

    private static S2C_StartPageSnapshot emptySnapshot() {
        return new S2C_StartPageSnapshot(List.of(), "", null, List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 检测底栏按钮点击(仅碰撞,无反馈)
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param screenWidth 屏幕宽
     * @param screenHeight 屏幕高
     * @param selectedTab 当前选中的标签页索引
     * @return 是否点到按钮
     */
    public static boolean isButtonClicked(int mouseX, int mouseY, int screenWidth, int screenHeight, int selectedTab) {
        int btnY = screenHeight - BAR_HEIGHT;
        if (mouseY < btnY || mouseY >= btnY + BAR_HEIGHT) return false;

        // 检查左侧原有按钮（仅开始游戏标签页）
        if (selectedTab == 0) {
            int currentX = screenWidth * FIRST_X_RATIO / 100;
            for (int i = 0; i < BTN_COUNT; i++) {
                int drawW = (i == 0) ? BUTTON_DRAW_W : ICON_DRAW_SIZE;
                if (mouseX >= currentX && mouseX < currentX + drawW) {
                    return true;
                }
                currentX += drawW + GAP_PATTERN[i % GAP_PATTERN.length];
            }
        }

        // 检查右侧新按钮（常显）
        int rightEdge = screenWidth - screenWidth * MARGIN_RIGHT_RATIO / 100;
        int[] cx = new int[NEW_BTN_COUNT];
        int[] cw = new int[NEW_BTN_COUNT];

        cx[5] = rightEdge - ICON_DRAW_SIZE;
        cw[5] = ICON_DRAW_SIZE;
        cx[4] = cx[5] - GAP_DEPLOY - ICON_DRAW_SIZE;
        cw[4] = ICON_DRAW_SIZE;
        cx[3] = cx[4] - GAP_DEPLOY - ICON_DRAW_SIZE;
        cw[3] = ICON_DRAW_SIZE;
        cx[2] = cx[3] - GAP_SOCIAL_DEPLOY - SOCIAL_DRAW_W;
        cw[2] = SOCIAL_DRAW_W;
        cx[1] = cx[2] - GAP_RECRUIT_SOCIAL - ICON_DRAW_SIZE;
        cw[1] = ICON_DRAW_SIZE;
        cx[0] = cx[1] - GAP_LEADERBOARD_RECRUIT - ICON_DRAW_SIZE;
        cw[0] = ICON_DRAW_SIZE;

        for (int i = 0; i < NEW_BTN_COUNT; i++) {
            if (mouseX >= cx[i] && mouseX < cx[i] + cw[i] && mouseY >= btnY && mouseY < btnY + ICON_DRAW_SIZE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 渲染右上角信息区（从右上角往左依次绘制）：
     * settings → mail → (货币:三角币数量(缩写)→delta_coin图标→哈夫币数量(右移15px)→havoc_coin图标,特战干员界面隐藏) → (网络延迟图标,仅开始游戏,位于哈夫币左侧)
     * settings/mail 常显；鼠标悬浮货币区域/网络图标时在其下方绘制等宽详情窗口
     * @param guiGraphics GUI上下文
     * @param mouseX 鼠标X
     * @param mouseY 鼠标Y
     * @param screenWidth 屏幕宽
     * @param screenHeight 屏幕高
     * @param selectedTab 当前选中的标签页索引
     * @return 悬浮区域标识(0=无, 1=settings, 2=mail, 3=货币区域, 4=网络延迟)
     */
    public static int renderTopRightInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight, int selectedTab) {
        var font = Minecraft.getInstance().font;
        int y = screenWidth < 640 ? 40 : TOP_RIGHT_MARGIN_Y;
        int rightEdge = screenWidth - screenWidth * TOP_RIGHT_MARGIN_X_RATIO / 100;
        int hovered = 0;

        boolean showNetwork = (selectedTab == 0);                // 网络验证仅在开始游戏显示
        boolean showCoins = (selectedTab != OPERATOR_TAB_INDEX); // 货币显示仅在特战干员界面隐藏

        // 获取玩家两种货币余额
        long deltaBalance = 0L;
        long havocBalance = 0L;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            String name = player.getName().getString();
            deltaBalance = EeconomyManager.getInstance().getBalance(name, CurrencyType.DELTA_COIN);
            havocBalance = EeconomyManager.getInstance().getBalance(name, CurrencyType.HAVOC_COIN);
        }

        // 数量文本（缩写显示，显示范围按实际文本宽度）
        String deltaText = formatAbbreviated(deltaBalance);
        String havocText = formatAbbreviated(havocBalance);
        int textY = y + (TOP_RIGHT_ICON_SIZE - font.lineHeight) / 2;

        // 1. settings（常显，最右侧）
        int settingsX = rightEdge - TOP_RIGHT_ICON_SIZE;
        guiGraphics.blit(ICON_SETTINGS, settingsX, y, 0, 0, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE);

        // 2. mail（常显，往左）- 无实际功能
        int mailX = settingsX - TOP_RIGHT_GAP - TOP_RIGHT_ICON_SIZE;
        guiGraphics.blit(ICON_MAIL, mailX, y, 0, 0, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE);
        renderWipOverlay(guiGraphics, mailX, y, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, mouseX, mouseY);

        // 当前已绘制区域的最左位置
        int cursorX = mailX;

        // 3. 货币区域（特战干员界面隐藏）
        int deltaTextW = 0, deltaTextX = 0, deltaCoinX = 0, havocCoinX = 0;
        if (showCoins) {
            deltaTextW = font.width(deltaText);
            int havocTextW = font.width(havocText);

            // 三角币数量文本（往左）
            deltaTextX = cursorX - TOP_RIGHT_GAP - deltaTextW;
            guiGraphics.drawString(font, deltaText, deltaTextX, textY, 0xFFFFFFFF, false);

            // delta_coin 图标（往左）
            deltaCoinX = deltaTextX - TOP_RIGHT_GAP - TOP_RIGHT_ICON_SIZE;
            guiGraphics.blit(ICON_DELTA_COIN, deltaCoinX, y, 0, 0, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE);

            // 哈夫币数量文本（基准右移15px基础上再往左13px）
            int havocTextX = deltaCoinX - TOP_RIGHT_GAP - havocTextW + TOP_RIGHT_HAVOC_SHIFT - TOP_RIGHT_HAVOC_TEXT_LEFT;
            guiGraphics.drawString(font, havocText, havocTextX, textY, 0xFFFFFFFF, false);

            // havoc_coin 图标（跟随文本，图标再往左10px；因文本已左移13px故补偿回13px，使图标净左移10px）
            havocCoinX = havocTextX - TOP_RIGHT_GAP - TOP_RIGHT_ICON_SIZE + TOP_RIGHT_HAVOC_TEXT_LEFT - TOP_RIGHT_HAVOC_ICON_LEFT;
            guiGraphics.blit(ICON_HAVOC_COIN, havocCoinX, y, 0, 0, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE);
        }

        // 4. 网络延迟图标（仅开始游戏，位于货币区域(哈夫币)左侧）
        int networkX = -1;
        if (showNetwork) {
            int baseX = showCoins ? havocCoinX : mailX;
            networkX = baseX - TOP_RIGHT_GAP - TOP_RIGHT_ICON_SIZE;
            guiGraphics.blit(ICON_NETWORK, networkX, y, 0, 0, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE, TOP_RIGHT_ICON_SIZE);
        }

        // hover 检测
        if (mouseY >= y && mouseY < y + TOP_RIGHT_ICON_SIZE) {
            if (mouseX >= settingsX && mouseX < settingsX + TOP_RIGHT_ICON_SIZE) {
                hovered = 1;
            } else if (mouseX >= mailX && mouseX < mailX + TOP_RIGHT_ICON_SIZE) {
                hovered = 2;
            } else if (showCoins && mouseX >= havocCoinX && mouseX < deltaTextX + deltaTextW) {
                hovered = 3; // 货币显示区域
            } else if (showNetwork && networkX >= 0 && mouseX >= networkX && mouseX < networkX + TOP_RIGHT_ICON_SIZE) {
                hovered = 4; // 网络延迟图标
            }
        }

        // settings/mail 悬浮白色视觉反馈（与底栏按钮一致）
        if (hovered == 1) {
            guiGraphics.fill(settingsX, y, settingsX + TOP_RIGHT_ICON_SIZE, y + TOP_RIGHT_ICON_SIZE, 0x80FFFFFF);
        } else if (hovered == 2) {
            guiGraphics.fill(mailX, y, mailX + TOP_RIGHT_ICON_SIZE, y + TOP_RIGHT_ICON_SIZE, 0x80FFFFFF);
        }

        // 货币区域悬浮：下方绘制等宽详情窗口
        if (hovered == 3 && showCoins) {
            renderCoinTooltip(guiGraphics, font, deltaBalance, havocBalance, havocCoinX, deltaTextX + deltaTextW, y + TOP_RIGHT_ICON_SIZE);
        }

        // 网络延迟悬浮：下方绘制等宽详情窗口
        if (hovered == 4 && showNetwork && networkX >= 0) {
            renderNetworkTooltip(guiGraphics, font, networkX, y + TOP_RIGHT_ICON_SIZE);
        }

        return hovered;
    }

    /**
     * 数量缩写（最小为K）：<1000K(100万) 一直千分位显示(XXX,XXX,XXX)，>=1000K(100万) 用K，>=1000M(10亿) 用M，>=1000B(1万亿) 用B，保留1位小数并去除末尾.0
     */
    public static String formatAbbreviated(long value) {
        if (value >= 1_000_000_000_000L) {
            return trimTrailingZero(String.format(java.util.Locale.US, "%.1f", value / 1_000_000_000.0)) + "B";
        }
        if (value >= 1_000_000_000L) {
            return trimTrailingZero(String.format(java.util.Locale.US, "%.1f", value / 1_000_000.0)) + "M";
        }
        if (value >= 1_000_000L) {
            return trimTrailingZero(String.format(java.util.Locale.US, "%.1f", value / 1000.0)) + "K";
        }
        return formatCommaSeparated(value);
    }

    private static String trimTrailingZero(String s) {
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    /**
     * 千分位格式（如 1,234,567）
     */
    public static String formatCommaSeparated(long value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

    /**
     * 渲染货币悬浮等宽小窗口（位于货币显示区域下方）
     * 内容：三角币图标+名称+千分位数量 / 哈夫币图标+名称+千分位数量
     * @param regionLeft 货币区域左缘
     * @param regionRight 货币区域右缘
     * @param topY 货币区域底部Y
     */
    private static void renderCoinTooltip(GuiGraphics guiGraphics, Font font, long deltaBalance, long havocBalance, int regionLeft, int regionRight, int topY) {
        Component deltaName = Component.translatable("currency.deltaforce.delta_coin");
        Component havocName = Component.translatable("currency.deltaforce.havoc_coin");
        String deltaQty = formatCommaSeparated(deltaBalance);
        String havocQty = formatCommaSeparated(havocBalance);

        int iconDraw = 12;
        int pad = 4;
        int rowGap = 2;
        int lineH = font.lineHeight;
        int rowH = Math.max(iconDraw, lineH);
        int textStartX = pad + iconDraw + 3; // 名称与数量统一缩进起点

        // 等宽窗口：取名称行(图标+名称)与数量行的最大宽度
        int contentW = Math.max(
            iconDraw + 3 + Math.max(font.width(deltaName), font.width(havocName)),
            Math.max(font.width(deltaQty), font.width(havocQty)));
        int w = contentW + pad * 2;
        int h = pad * 2 + rowH * 4 + rowGap * 3; // 4行内容

        int x = regionLeft;
        int y = topY + 4;
        // 防止窗口超出货币区域右缘
        if (x + w > regionRight) {
            x = Math.max(regionRight - w, 0);
        }

        // 背景 + 边框
        guiGraphics.fill(x, y, x + w, y + h, 0xE6101819);
        guiGraphics.fill(x, y, x + w, y + 1, 0xFF3C4A4B);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFF3C4A4B);
        guiGraphics.fill(x, y, x + 1, y + h, 0xFF3C4A4B);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFF3C4A4B);

        // 行1：三角币图标 + 名称
        int cy = y + pad;
        guiGraphics.blit(ICON_DELTA_COIN, x + pad, cy, 0, 0, iconDraw, iconDraw, iconDraw, iconDraw);
        guiGraphics.drawString(font, deltaName, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFFFFF, false);
        cy += rowH + rowGap;

        // 行2：三角币数量（千分位，不缩写）
        guiGraphics.drawString(font, deltaQty, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFE08A, false);
        cy += rowH + rowGap;

        // 行3：哈夫币图标 + 名称
        guiGraphics.blit(ICON_HAVOC_COIN, x + pad, cy, 0, 0, iconDraw, iconDraw, iconDraw, iconDraw);
        guiGraphics.drawString(font, havocName, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFFFFF, false);
        cy += rowH + rowGap;

        // 行4：哈夫币数量（千分位，不缩写）
        guiGraphics.drawString(font, havocQty, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFE08A, false);
    }

    /**
     * 渲染网络延迟悬浮等宽小窗口（位于网络图标下方）
     * 内容：网络图标+名称 / 延迟值(ms)
     * @param iconLeft 网络图标左缘X
     * @param topY 网络图标底部Y
     */
    private static void renderNetworkTooltip(GuiGraphics guiGraphics, Font font, int iconLeft, int topY) {
        Component name = Component.translatable("deltaforce.network.latency");
        String latencyText = getLatencyText();

        int iconDraw = 12;
        int pad = 4;
        int rowGap = 2;
        int lineH = font.lineHeight;
        int rowH = Math.max(iconDraw, lineH);
        int textStartX = pad + iconDraw + 3; // 名称与数值统一缩进起点

        // 等宽窗口：取名称行与数值行的最大宽度
        int contentW = Math.max(
            iconDraw + 3 + font.width(name),
            font.width(latencyText));
        int w = contentW + pad * 2;
        int h = pad * 2 + rowH * 2 + rowGap; // 2行内容

        int x = iconLeft;
        int y = topY + 4;
        // 防止窗口超出屏幕右缘
        int guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        if (x + w > guiWidth) {
            x = Math.max(guiWidth - w, 0);
        }

        // 背景 + 边框
        guiGraphics.fill(x, y, x + w, y + h, 0xE6101819);
        guiGraphics.fill(x, y, x + w, y + 1, 0xFF3C4A4B);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, 0xFF3C4A4B);
        guiGraphics.fill(x, y, x + 1, y + h, 0xFF3C4A4B);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, 0xFF3C4A4B);

        // 行1：网络图标 + 名称
        int cy = y + pad;
        guiGraphics.blit(ICON_NETWORK, x + pad, cy, 0, 0, iconDraw, iconDraw, iconDraw, iconDraw);
        guiGraphics.drawString(font, name, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFFFFF, false);
        cy += rowH + rowGap;

        // 行2：延迟值（ms）
        guiGraphics.drawString(font, latencyText, x + textStartX, cy + (rowH - lineH) / 2, 0xFFFFE08A, false);
    }

    /**
     * 获取网络延迟文本（单位 ms），无连接或离线时返回 "--"
     */
    private static String getLatencyText() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.getConnection() == null) {
            return "--";
        }
        var info = mc.getConnection().getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() + " ms" : "--";
    }

    /**
     * 检测右上角 settings/mail 图标是否被点击（空触发，与底栏按钮一致）
     */
    public static boolean isTopRightIconClicked(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int y = screenWidth < 640 ? 40 : TOP_RIGHT_MARGIN_Y;
        int rightEdge = screenWidth - screenWidth * TOP_RIGHT_MARGIN_X_RATIO / 100;
        int settingsX = rightEdge - TOP_RIGHT_ICON_SIZE;
        int mailX = settingsX - TOP_RIGHT_GAP - TOP_RIGHT_ICON_SIZE;
        if (mouseY >= y && mouseY < y + TOP_RIGHT_ICON_SIZE) {
            if (mouseX >= settingsX && mouseX < settingsX + TOP_RIGHT_ICON_SIZE) return true;
            if (mouseX >= mailX && mouseX < mailX + TOP_RIGHT_ICON_SIZE) return true;
        }
        return false;
    }

    /**
     * 检测右上角 settings 图标是否被点击（用于打开设置界面）
     */
    public static boolean isSettingsIconClicked(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int y = screenWidth < 640 ? 40 : TOP_RIGHT_MARGIN_Y;
        int rightEdge = screenWidth - screenWidth * TOP_RIGHT_MARGIN_X_RATIO / 100;
        int settingsX = rightEdge - TOP_RIGHT_ICON_SIZE;
        if (mouseY >= y && mouseY < y + TOP_RIGHT_ICON_SIZE) {
            if (mouseX >= settingsX && mouseX < settingsX + TOP_RIGHT_ICON_SIZE) return true;
        }
        return false;
    }

    /**
     * 渲染玩法模式面板（pre_battle_rep、no_target）
     * 位于底栏上方右侧，点击打开玩法选择界面
     * @return 悬浮区域标识(-1=无, -2=pre_battle_rep, -3=no_target)
     */
    public static int renderModePanels(GuiGraphics guiGraphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int barY = screenHeight - BAR_HEIGHT;
        int hovered = -1;

        // pre_battle_rep（缩放到0.4倍）
        int preX = screenWidth - screenWidth * MODE_PANEL_MARGIN_RIGHT_RATIO / 100 - PRE_BATTLE_REP_W;
        int preBottom = barY - PRE_BATTLE_REP_BOTTOM_OFFSET;
        int preY = preBottom - PRE_BATTLE_REP_H;

        // 使用PoseStack进行缩放渲染
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(preX, preY, 0);
        guiGraphics.pose().scale(MODE_PANEL_SCALE, MODE_PANEL_SCALE, 1.0F);
        guiGraphics.blit(BTN_PRE_BATTLE_REP, 0, 0, 0, 0, PRE_BATTLE_REP_ORIG_W, PRE_BATTLE_REP_ORIG_H, PRE_BATTLE_REP_ORIG_W, PRE_BATTLE_REP_ORIG_H);
        guiGraphics.pose().popPose();

        if (mouseX >= preX && mouseX < preX + PRE_BATTLE_REP_W && mouseY >= preY && mouseY < preY + PRE_BATTLE_REP_H) {
            hovered = -2;
        }
        // 玩法模式面板无实际功能，绘制红叉遮罩
        renderWipOverlay(guiGraphics, preX, preY, PRE_BATTLE_REP_W, PRE_BATTLE_REP_H, mouseX, mouseY);

        // no_target（缩放到0.4倍）
        int noX = preX;
        int noBottom = preY - MODE_PANEL_GAP;
        int noY = noBottom - NO_TARGET_H;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(noX, noY, 0);
        guiGraphics.pose().scale(MODE_PANEL_SCALE, MODE_PANEL_SCALE, 1.0F);
        guiGraphics.blit(BTN_NO_TARGET, 0, 0, 0, 0, NO_TARGET_ORIG_W, NO_TARGET_ORIG_H, NO_TARGET_ORIG_W, NO_TARGET_ORIG_H);
        guiGraphics.pose().popPose();

        if (mouseX >= noX && mouseX < noX + NO_TARGET_W && mouseY >= noY && mouseY < noY + NO_TARGET_H) {
            hovered = -3;
        }
        // no_target 玩法面板无实际功能，绘制红叉遮罩
        renderWipOverlay(guiGraphics, noX, noY, NO_TARGET_W, NO_TARGET_H, mouseX, mouseY);

        return hovered;
    }

    /**
     * 检测 pre_battle_rep 是否被点击
     */
    public static boolean isPreBattleRepClicked(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int barY = screenHeight - BAR_HEIGHT;
        int preX = screenWidth - screenWidth * MODE_PANEL_MARGIN_RIGHT_RATIO / 100 - PRE_BATTLE_REP_W;
        int preBottom = barY - PRE_BATTLE_REP_BOTTOM_OFFSET;
        int preY = preBottom - PRE_BATTLE_REP_H;
        return mouseX >= preX && mouseX < preX + PRE_BATTLE_REP_W && mouseY >= preY && mouseY < preY + PRE_BATTLE_REP_H;
    }

    /**
     * 检测 no_target 是否被点击
     */
    public static boolean isNoTargetClicked(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int barY = screenHeight - BAR_HEIGHT;
        int preX = screenWidth - screenWidth * MODE_PANEL_MARGIN_RIGHT_RATIO / 100 - PRE_BATTLE_REP_W;
        int preBottom = barY - PRE_BATTLE_REP_BOTTOM_OFFSET;
        int preY = preBottom - PRE_BATTLE_REP_H;
        int noY = preY - MODE_PANEL_GAP - NO_TARGET_H;
        return mouseX >= preX && mouseX < preX + NO_TARGET_W && mouseY >= noY && mouseY < noY + NO_TARGET_H;
    }

    /**
     * 渲染玩家模型到指定位置
     * @param guiGraphics GUI图形上下文
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     */
    public static void renderPlayerModel(GuiGraphics guiGraphics, int centerX, int centerY) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float modelScale = Math.max(36.0F, Math.min(100.0F, (screenHeight - BAR_HEIGHT) * 0.35F));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX - modelScale * 0.75F, centerY + modelScale * 1.55F, 100.0F);
        guiGraphics.pose().scale(modelScale, modelScale, modelScale);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(0.0F));

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);

        float oldYaw = player.getYRot();
        float oldPitch = player.getXRot();
        float oldBodyYaw = player.yBodyRot;
        float oldHeadYaw = player.yHeadRot;

        try {
            player.setYRot(0.0F);
            player.setXRot(0.0F);
            player.yBodyRot = 0.0F;
            player.yHeadRot = 0.0F;
            dispatcher.render(player, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, guiGraphics.pose(),
                guiGraphics.bufferSource(), 15728880);
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
            player.yBodyRot = oldBodyYaw;
            player.yHeadRot = oldHeadYaw;
            dispatcher.setRenderShadow(true);
        }

        guiGraphics.flush();
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }
}