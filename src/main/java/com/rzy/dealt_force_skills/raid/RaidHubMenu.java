package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.raid.ui.RaidAssetMenus;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.Map;

/** Uses vanilla container synchronization; display buttons never transfer their ItemStacks. */
public final class RaidHubMenu {
    private RaidHubMenu() { }
    public static void open(ServerPlayer player) {
        if (RaidMatches.assetsBlocked(player)) { player.sendSystemMessage(Component.literal("物品恢复尚未完成，请使用 /raid recover。")); return; }
        com.rzy.dealt_force_skills.raid.ui.RaidPanelMenu.openPanel(player, new SimpleMenuProvider((id, inv, ignored) -> new Menu(id, player), Component.literal("绝密行动")));
    }
    private static final class Menu extends com.rzy.dealt_force_skills.raid.ui.RaidPanelMenu {
        final ServerPlayer owner;
        final Map<Integer, Runnable> actions = new HashMap<>();
        Menu(int id, ServerPlayer player) {
            super(Page.MAPS, id, player.getInventory());
            owner = player;
            button(49, Items.CLOCK, "刷新", () -> open(owner));
            var loading = RaidMatches.match(owner);
            if (loading != null && loading.phase == RaidMatchLedger.Phase.PREPARING) {
                button(13, Items.BARRIER, "取消进入并返回游戏", () -> { if (RaidMatches.cancelEntry(owner)) owner.closeContainer(); });
                return;
            }
            if (RaidMatches.belongs(owner)) {
                if (RaidMatches.participating(owner)) {
                    button(10, Items.CHEST, "副本储物", () -> { RaidPlayCommands.editable(owner, true); RaidAssetMenus.openStorage(owner); });
                    int slot = 19;
                    for (String marker : RaidMatches.conditionalIds(owner)) {
                        if (slot >= 45) break;
                        button(slot++, Items.ENDER_PEARL, "申请条件撤离 · " + marker, () -> {
                            if (!RaidMatches.conditional(owner, marker)) owner.sendSystemMessage(Component.literal("请到撤离区域，满足占格比例和付款条件后再申请。"));
                        });
                    }
                } else button(10, Items.ENDER_EYE, "切换队友观战视角", () -> RaidMatches.spectate(owner));
                return;
            }
            if (RaidLobbies.queued(owner)) { button(22, Items.CLOCK, "已录入名单 · 等待本局准备", () -> open(owner)); return; }
            button(0, Items.ARMOR_STAND, "行前备战 / 五套方案", () -> { RaidPlayCommands.editable(owner, false); RaidAssetMenus.openPrep(owner); });
            button(1, Items.CHEST, "安全箱 / 拓展格", () -> { RaidPlayCommands.editable(owner, true); RaidAssetMenus.openStorage(owner); });
            var draft = RaidLobbies.draft(owner);
            if (draft != null) {
                button(4, Items.MAP, RaidLobbies.mapName(draft.map()) + " · " + draft.members().size() + "/3人", () -> open(owner));
                button(8, Items.BARRIER, "取消待出发队伍", () -> { RaidLobbies.cancel(owner); open(owner); });
                if (draft.captain().equals(owner.getUUID())) {
                    button(7, Items.LIME_DYE, "整队准备完成 · 开始", () -> { RaidLobbies.ready(owner, RaidMatches.LOBBIES); if (RaidLobbies.draft(owner) != null) open(owner); });
                    int slot = 19;
                    for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(owner)) {
                        if (draft.members().contains(teammate.getUUID()) || slot >= 45) continue;
                        boolean invited = draft.invited().contains(teammate.getUUID());
                        button(slot++, Items.PLAYER_HEAD, (invited ? "取消邀请 · " : "邀请 · ") + teammate.getGameProfile().getName(), () -> {
                            if (invited) RaidLobbies.uninvite(owner, teammate.getUUID()); else RaidLobbies.invite(owner, teammate, RaidMatches.LOBBIES);
                            open(owner);
                        });
                    }
                }
                return;
            }
            button(10, Items.MAP, "零号大坝－绝密 · 关闭匹配", () -> create("zero_dam", false));
            button(11, Items.FILLED_MAP, "零号大坝－绝密 · 开启匹配", () -> create("zero_dam", true));
            button(13, Items.MAP, "航天基地－绝密 · 关闭匹配", () -> create("space_base", false));
            button(14, Items.FILLED_MAP, "航天基地－绝密 · 开启匹配", () -> create("space_base", true));
            int slot = 19;
            for (var invite : RaidLobbies.invitations(owner)) {
                if (slot >= 45) break;
                var captain = owner.server.getPlayerList().getPlayer(invite.captain());
                if (captain != null) button(slot++, Items.PLAYER_HEAD, "接受邀请 · " + captain.getGameProfile().getName(), () -> {
                    RaidLobbies.accept(owner, invite.captain(), RaidMatches.LOBBIES); open(owner);
                });
            }
            for (var lobby : RaidLobbies.open(owner.server)) {
                if (slot >= 45) break;
                button(slot++, Items.COMPASS, RaidLobbies.mapName(lobby.map()) + " · 余 " + (lobby.remainingTicks() + 19) / 20 + " 秒", () -> {
                    RaidLobbies.join(owner, lobby.id(), RaidMatches.LOBBIES); open(owner);
                });
            }
        }
        void create(String map, boolean matching) { RaidLobbies.create(owner, map, matching, RaidMatches.LOBBIES); open(owner); }
        void button(int slot, Item item, String label, Runnable action) {
            ItemStack stack = new ItemStack(item); stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(label));
            getContainer().setItem(slot, stack); actions.put(slot, action);
        }
        @Override public void clicked(int slot, int mouseButton, ClickType type, Player player) {
            if (player != owner || type != ClickType.PICKUP || RaidMatches.assetsBlocked(owner)) return;
            Runnable action = actions.get(slot);
            if (action != null) try { action.run(); }
            catch (RuntimeException failure) { owner.sendSystemMessage(Component.literal(failure.getMessage() == null ? "操作失败，请刷新重试。" : failure.getMessage())); }
        }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return player == owner && owner.isAlive() && !RaidMatches.assetsBlocked(owner); }
    }
}
