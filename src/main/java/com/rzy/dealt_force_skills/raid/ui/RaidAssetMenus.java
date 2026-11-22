package com.rzy.dealt_force_skills.raid.ui;

import com.rzy.dealt_force_skills.raid.assets.RaidAssetService;
import com.rzy.dealt_force_skills.raid.assets.RaidItemCodec;
import com.rzy.dealt_force_skills.raid.assets.RaidPlayerAssets;
import com.rzy.dealt_force_skills.raid.assets.RaidPurchases;
import com.rzy.dealt_force_skills.raid.rules.SafeBoxRules;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-owned selections and transactions for the raid equipment screens. */
public final class RaidAssetMenus {
    private enum View { PREP, STORAGE, WAREHOUSE, EQUIPMENT, PURCHASES }
    private RaidAssetMenus() { }
    public static void openPrep(ServerPlayer player) { open(player, View.PREP, new Selection(), 0); }
    public static void openStorage(ServerPlayer player) {
        if (RaidAssetService.isActive(player.getServer(), player.getUUID())) open(player, View.STORAGE, new Selection(), 0);
        else openPrep(player);
    }
    public static void openPurchases(ServerPlayer player) { open(player, View.PURCHASES, new Selection(), 0); }
    private static final class Selection {
        String query = "";
        java.util.BitSet localMatches = new java.util.BitSet();
        List<String> missing = List.of();
        int plan;
        int target = -1;
        RaidPlayerAssets.Store targetStore = RaidPlayerAssets.Store.PREP;
        RaidPurchases.Source shop = RaidPurchases.Source.REDEMPTION;
    }
    private static void open(ServerPlayer player, View view, Selection selection, int page) {
        if (RaidAssetService.pending(player)) { message(player, "物品正在恢复，请稍后再试。"); return; }
        boolean active = RaidAssetService.isActive(player.getServer(), player.getUUID());
        if (active && view != View.STORAGE) { message(player, "对局中只能使用副本储物。"); return; }
        String title = switch (view) {
            case PREP -> "副本备战 · 方案 " + (selection.plan + 1);
            case STORAGE -> "副本储物 · 安全箱 / 拓展格";
            case WAREHOUSE -> "仓库 → 备战 · 第 " + (page + 1) + " 页";
            case EQUIPMENT -> "穿戴装备 → 备战";
            case PURCHASES -> "直购备战 · " + (selection.shop == RaidPurchases.Source.REDEMPTION ? "特勤处赎回" : "交易行");
        };
        RaidPanelMenu.openPanel(player, new SimpleMenuProvider((id, inventory, ignored) -> new Menu(id, inventory, view, selection, Math.max(0, page)), Component.literal(title)));
    }
    private static void message(ServerPlayer player, String text) { player.displayClientMessage(Component.literal(text), false); }

    public static void search(ServerPlayer player, int menuId, String text) {
        search(player, menuId, text, new java.util.BitSet());
    }
    public static void search(ServerPlayer player, int menuId, String text, java.util.BitSet matches) {
        if (player != null && text.length() <= 80 && player.containerMenu instanceof Menu menu && menu.containerId == menuId
                && menu.view == View.PURCHASES && menu.stillValid(player)) {
            menu.selection.localMatches = matches;
            menu.selection.query = text.strip().toLowerCase(java.util.Locale.ROOT);
            open(player, menu.view, menu.selection, 0);
        }
    }
    public static void drop(ServerPlayer player, int menuId, int source, int destination) {
        if (!(player.containerMenu instanceof Menu menu) || menu.containerId != menuId || !menu.stillValid(player)
                || menu.view != View.PREP && menu.view != View.STORAGE || RaidPlayerAssets.revision(player) != menu.revision) return;
        var from = menu.address(source); var to = menu.address(destination);
        if (from != null && to != null && from.store != to.store) menu.transfer(from.store, from.index, to.store, to.index, false);
    }
    private record Address(RaidPlayerAssets.Store store, int index) {}
    private static final class Menu extends RaidPanelMenu {
        private Address address(int slot) {
            if (slot >= 54 && slot < 90 || view == View.PREP && slot >= 144 && slot < 149)
                return new Address(RaidPlayerAssets.Store.INVENTORY, getSlot(slot).getContainerSlot());
            if (view == View.PREP && slot >= 0 && slot < 41) return new Address(RaidPlayerAssets.Store.PREP, slot);
            int offset = slot - (view == View.PREP ? PREP_STORAGE_BASE : 0);
            if (offset >= 0 && offset < 9) return new Address(RaidPlayerAssets.Store.SAFE, offset);
            if (offset >= 9 && offset < 45 && page*36+offset-9 < RaidPlayerAssets.MAX_EXTRA_SLOTS) return new Address(RaidPlayerAssets.Store.EXTRA, page*36+offset-9);
            return null;
        }
        private final ServerPlayer owner;
        private final View view;
        private final Selection selection;
        private final int page;
        private final Map<Integer, Action> actions = new HashMap<>();
        private final long revision;
        private final CompoundTag profile;
        private final boolean active;
        private interface Action { void run(int button, boolean shift); }
        Menu(int id, Inventory inventory, View view, Selection selection, int page) {
            super(Page.valueOf(view.name()), id, inventory);
            this.owner = (ServerPlayer) inventory.player; this.view = view; this.selection = selection; this.page = page;
            this.profile = RaidPlayerAssets.snapshot(owner); this.revision = profile.getLong("Revision");
            this.active = RaidAssetService.isActive(owner.getServer(), owner.getUUID());
            render();
        }
        private void render() {
            for (int i = 0; i < 54; i++) getContainer().setItem(i, ItemStack.EMPTY);
            switch (view) {
                case PREP -> { prep(); storage(PREP_STORAGE_BASE); } case STORAGE -> storage(0); case WAREHOUSE -> warehouse();
                case EQUIPMENT -> equipment(); case PURCHASES -> purchases();
            }
            button(53, Items.BARRIER, "关闭", (mouse, shift) -> owner.closeContainer());
        }
        private void button(int slot, net.minecraft.world.item.Item icon, String title, Action action, String... lines) {
            getSlot(slot).set(RaidUiItems.button(icon, title, lines)); actions.put(slot, action);
        }
        private List<ItemStack> items(RaidPlayerAssets.Store store) {
            return switch (store) {
                case INVENTORY -> RaidItemCodec.inventory(owner);
                case WAREHOUSE -> DfWarehouseManager.loadWarehouse(owner);
                case PREP -> RaidItemCodec.readList(owner.getServer(), profile.getList("PREP", Tag.TAG_COMPOUND), 41);
                case SAFE -> RaidItemCodec.readList(owner.getServer(), profile.getList("SAFE", Tag.TAG_COMPOUND), 9);
                case EXTRA -> RaidItemCodec.readList(owner.getServer(), profile.getList("EXTRA", Tag.TAG_COMPOUND), 75);
            };
        }
        private void prep() {
            List<ItemStack> prep = items(RaidPlayerAssets.Store.PREP);
            for (int i = 0; i < 41; i++) physical(i, RaidPlayerAssets.Store.PREP, i, prep.get(i), slotName(i));
            button(41, Items.COMPASS, "放入位置：" + targetLabel(),
                    (mouse, shift) -> { selection.target = -1; selection.targetStore = RaidPlayerAssets.Store.PREP; refresh(); }, "选择备战、安全箱或拓展格后，点击背包放入；点击此处恢复自动备战");
            button(42, Items.WRITABLE_BOOK, "当前方案 " + (selection.plan + 1) + " / 5", (mouse, shift) -> { selection.plan = (selection.plan + 1) % 5; refresh(); }, "点击切换方案");
            button(43, Items.PAPER, "保存当前方案", (mouse, shift) -> {
                if (RaidPlayerAssets.savePlan(owner, revision, selection.plan)) message(owner, "已保存方案 " + (selection.plan + 1) + " 的物品需求。"); refresh();
            }, "覆盖所选方案；只保存需求，不复制物品");
            button(44, Items.CHEST, "从背包与仓库补全", (mouse, shift) -> result(RaidPurchases.fillPlan(owner, revision, selection.plan, false)), "按整套需求检查，缺少时列出清单");
            button(45, Items.GOLD_INGOT, "补全并购买缺少物品", (mouse, shift) -> result(RaidPurchases.fillPlan(owner, revision, selection.plan, true)), "余额或货源不足则整单不成交");
            button(46, Items.BARREL, "打开仓库", (mouse, shift) -> open(owner, View.WAREHOUSE, selection, 0));
            button(47, Items.EMERALD, "直接购买到备战栏", (mouse, shift) -> open(owner, View.PURCHASES, selection, 0));
            button(48, Items.IRON_CHESTPLATE, "从穿戴栏移入装备", (mouse, shift) -> open(owner, View.EQUIPMENT, selection, 0));
            if (!selection.missing.isEmpty()) button(50, Items.PAPER, "缺少的物品", (mouse, shift) -> {}, selection.missing.toArray(String[]::new));
            button(52, Items.BOOK, "操作说明", (mouse, shift) -> { }, "点背包：移入所选位置（默认备战）；右键仅移 1 个", "拖动物品指定格；双击或 Shift+左键整组转移", "右键备战物品或点击空格：选为放入位置");
        }
        private void physical(int visual, RaidPlayerAssets.Store store, int index, ItemStack stack, String emptyName) {
            if (stack.isEmpty()) {
                button(visual, Items.LIGHT_GRAY_STAINED_GLASS_PANE, emptyName + "（空）", (mouse, shift) -> select(store, index), "点击选择放入位置，再点随身背包物品");
            } else {
                getSlot(visual).set(RaidUiItems.describe(stack, emptyName + " · " + stack.getHoverName().getString(),
                        "左键取回背包；右键选定放入位置", active ? "对局储物即时保存" : "双击或 Shift+左键：自动移回随身栏"));
                actions.put(visual, (mouse, shift) -> {
                    if (mouse == 1 && !shift) { select(store, index); return; }
                    transfer(store, index, shift && !active ? RaidPlayerAssets.Store.WAREHOUSE : RaidPlayerAssets.Store.INVENTORY, -1, false);
                });
            }
        }
        private String targetLabel() {
            if (selection.target < 0) return "自动备战";
            return switch (selection.targetStore) {
                case SAFE -> "安全箱格 " + (selection.target + 1);
                case EXTRA -> "拓展格 " + (selection.target + 1);
                default -> slotName(selection.target);
            };
        }
        private void select(RaidPlayerAssets.Store store, int slot) {
            selection.targetStore = store; selection.target = slot;
            message(owner, "已选择 " + targetLabel() + "，点击随身背包或快捷栏物品放入。");
            refresh();
        }
        private void storage(int base) {
            var safe = new SafeBoxRules.State(SafeBoxRules.Tier.valueOf(profile.getString("SafeTier")), profile.getInt("SafeUses"));
            int extraSlots = profile.getInt("ExtraSlots");
            String owned = "已有拓展 " + extraSlots + " / 75 格　安全箱 " + safe.tier().slots() + " 格　剩余保护 " + safe.remainingUses() + " 次";
            List<ItemStack> safeItems = items(RaidPlayerAssets.Store.SAFE), extra = items(RaidPlayerAssets.Store.EXTRA);
            for (int index = 0; index < 9; index++) {
                if (index < safe.tier().slots()) physical(base + index, RaidPlayerAssets.Store.SAFE, index, safeItems.get(index), "安全箱格 " + (index + 1));
                else button(base + index, Items.BLACK_STAINED_GLASS_PANE, "未开通安全箱格", (mouse, shift) -> message(owner, owned), "可在本页扩容区升级安全箱");
            }
            for (int offset = 0; offset < 36; offset++) {
                int index = page * 36 + offset, visual = base + 9 + offset;
                if (index < extraSlots) physical(visual, RaidPlayerAssets.Store.EXTRA, index, extra.get(index), "拓展格 " + (index + 1));
                else if (index < RaidPlayerAssets.MAX_EXTRA_SLOTS) button(visual, Items.BLACK_STAINED_GLASS_PANE, "未购买拓展格", (mouse, shift) -> message(owner, owned), "可在本页扩容区增加拓展格");
            }
            if (page > 0) button(base + 45, Items.ARROW, "上一页拓展格", (mouse, shift) -> open(owner, view, selection, page - 1));
            if ((page + 1) * 36 < RaidPlayerAssets.MAX_EXTRA_SLOTS) button(base + 46, Items.ARROW, "下一页拓展格", (mouse, shift) -> open(owner, view, selection, page + 1));
            button(base + 47, Items.CHEST, "扩容：左键 1 格 / 右键 5 格", (mouse, shift) -> {
                message(owner, RaidPlayerAssets.buyExtraSlot(owner, revision, mouse == 1 ? 5 : 1) ? "拓展空间已增加。" : "购买失败：请检查余额与格数上限。"); refresh();
            }, owned, "每格 3250 哈夫币，五格 16250 哈夫币", "成功撤离保留，失败或迷失重置");
            int offer = base + 48;
            for (SafeBoxRules.Tier tier : new SafeBoxRules.Tier[]{SafeBoxRules.Tier.FOUR, SafeBoxRules.Tier.SIX, SafeBoxRules.Tier.NINE}) {
                boolean covered = safe.covers(tier);
                String reason = "已拥有当前或更高等级的安全箱，剩余保护 " + safe.remainingUses() + " 次。";
                button(offer++, Items.ENDER_CHEST, tier.slots() + " 格安全箱 · " + (covered ? "已拥有" : tier.cost() + " 哈夫币"), (mouse, shift) -> {
                    if (covered) { message(owner, reason); return; }
                    if (active) { message(owner, "请在局外购买安全箱使用权。"); return; }
                    message(owner, RaidPlayerAssets.buySafeBox(owner, revision, tier) ? "已购买安全箱，剩余保护次数为 10。" : "购买失败：请检查余额及高位格是否已清空。"); refresh();
                }, owned, covered ? reason : active ? "请在局外购买安全箱使用权" : "购买或升级后获得 10 次失败保护", "失败时仅占用 0～2 格不扣次数");
            }
            button(base + 51, Items.PAPER, owned, (mouse, shift) -> {});
            button(base + 52, Items.BOOK, "储物操作 · 拓展格第 " + (page + 1) + " / 3 页", (mouse, shift) -> {}, "先选安全箱或拓展格，再点背包放入；点击储物物品取回");
        }
        private void warehouse() {
            List<ItemStack> warehouse = items(RaidPlayerAssets.Store.WAREHOUSE);
            for (int i = 0; i < 45; i++) {
                int index = page * 45 + i;
                if (index >= warehouse.size()) break;
                ItemStack stack = warehouse.get(index);
                if (stack.isEmpty()) continue;
                getContainer().setItem(i, RaidUiItems.describe(stack, stack.getHoverName().getString(), "点击移入备战；右键移 1 个", "Shift 点击取回普通背包"));
                actions.put(i, (mouse, shift) -> transfer(RaidPlayerAssets.Store.WAREHOUSE, index,
                        shift ? RaidPlayerAssets.Store.INVENTORY : RaidPlayerAssets.Store.PREP,
                        !shift && selection.targetStore == RaidPlayerAssets.Store.PREP ? selection.target : -1, mouse == 1));
            }
            button(45, Items.ARROW, "上一页", (mouse, shift) -> open(owner, view, selection, Math.max(0, page - 1)));
            button(46, Items.ARROW, "下一页", (mouse, shift) -> open(owner, view, selection, Math.min(Math.max(0, (warehouse.size() - 1) / 45), page + 1)));
            button(49, Items.CHEST, "返回备战", (mouse, shift) -> open(owner, View.PREP, selection, 0));
        }
        private void equipment() {
            for (int i = 0; i < 5; i++) {
                int slot = i == 4 ? 40 : 39 - i; ItemStack stack = owner.getInventory().getItem(slot);
                getContainer().setItem(i, stack.isEmpty() ? RaidUiItems.button(Items.LIGHT_GRAY_STAINED_GLASS_PANE, slotName(slot) + "（空）")
                        : RaidUiItems.describe(stack, slotName(slot) + " · " + stack.getHoverName().getString(), "点击移入相应备战格"));
                if (!stack.isEmpty()) actions.put(i, (mouse, shift) -> transfer(RaidPlayerAssets.Store.INVENTORY, slot, RaidPlayerAssets.Store.PREP, slot, false));
            }
            button(49, Items.CHEST, "返回备战", (mouse, shift) -> open(owner, View.PREP, selection, 0));
        }
        private void purchases() {
            List<ItemStack> prep = items(RaidPlayerAssets.Store.PREP);
            for (int index = 0; index < prep.size(); index++) getSlot(PREP_STORAGE_BASE+index).set(prep.get(index).copy());
            List<ItemStack> offers = new ArrayList<>(); List<Long> prices = new ArrayList<>();
            if (selection.shop == RaidPurchases.Source.REDEMPTION) for (ItemStack stack : SpecialOperationsBaseManager.soldStacks(owner)) {
                offers.add(stack); prices.add(SpecialOperationsBaseManager.redemptionCost(owner, stack, 1));
            } else for (var entry : GtiTradingHallManager.entries()) { offers.add(entry.stack()); prices.add(entry.price()); }
            List<Integer> filtered = new ArrayList<>();
            for (int index = 0; index < offers.size(); index++) {
                ItemStack offer = offers.get(index);
                String label = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(offer.getItem()) + " " + offer.getHoverName().getString();
                if (selection.localMatches.get(net.minecraft.world.item.Item.getId(offer.getItem())) || label.toLowerCase(java.util.Locale.ROOT).contains(selection.query)) filtered.add(index);
            }
            for (int i = 0; i < 45; i++) {
                int visibleIndex = page * 45 + i;
                if (visibleIndex >= filtered.size()) break;
                int index = filtered.get(visibleIndex);
                ItemStack offer = offers.get(index); long price = prices.get(index);
                getContainer().setItem(i, RaidUiItems.describe(offer, offer.getHoverName().getString(), price < 0 ? "当前不可赎回" : "每个 " + price + " 哈夫币", "左键买 1 个；右键购买当前目标格可容纳数量", "购买后直接进入备战栏"));
                actions.put(i, (mouse, shift) -> {
                    if (!sameOffer(index, offer, price)) { message(owner, "货源已更新，请重新选择。"); refresh(); return; }
                    int target = findTarget(RaidPlayerAssets.Store.PREP, offer, selection.targetStore == RaidPlayerAssets.Store.PREP ? selection.target : -1);
                    if (target < 0) { message(owner, "备战栏没有合适的空位。"); return; }
                    int count = mouse == 1 ? room(RaidPlayerAssets.Store.PREP, target, offer) : 1;
                    result(RaidPurchases.buyIntoPrep(owner, revision, selection.shop, index, count, target));
                });
            }
            button(45, Items.ARROW, "上一页", (mouse, shift) -> open(owner, view, selection, Math.max(0, page - 1)));
            button(46, Items.ARROW, "下一页", (mouse, shift) -> open(owner, view, selection, Math.min(Math.max(0, (filtered.size() - 1) / 45), page + 1)));
            button(47, Items.EMERALD, "切换：" + (selection.shop == RaidPurchases.Source.REDEMPTION ? "交易行" : "特勤处赎回"), (mouse, shift) -> {
                selection.shop = selection.shop == RaidPurchases.Source.REDEMPTION ? RaidPurchases.Source.TRADING_HALL : RaidPurchases.Source.REDEMPTION; open(owner, view, selection, 0);
            });
            button(48, Items.GOLD_INGOT, "余额 " + SpecialOperationsBaseManager.havocCoins(owner) + " 哈夫币", (mouse, shift) -> refresh());
            button(49, Items.CHEST, "返回备战 / 选择放入位置", (mouse, shift) -> open(owner, View.PREP, selection, 0));
        }
        private boolean sameOffer(int index, ItemStack expected, long price) {
            if (selection.shop == RaidPurchases.Source.REDEMPTION) {
                var offers = SpecialOperationsBaseManager.soldStacks(owner);
                return index < offers.size() && RaidItemCodec.same(offers.get(index), expected) && SpecialOperationsBaseManager.redemptionCost(owner, offers.get(index), 1) == price;
            }
            var offers = GtiTradingHallManager.entries();
            return index < offers.size() && RaidItemCodec.same(offers.get(index).stack(), expected) && offers.get(index).price() == price;
        }
        private void transfer(RaidPlayerAssets.Store from, int sourceSlot, RaidPlayerAssets.Store to, int selected, boolean one) {
            List<ItemStack> source = items(from);
            if (sourceSlot < 0 || sourceSlot >= source.size() || source.get(sourceSlot).isEmpty()) { refresh(); return; }
            ItemStack stack = source.get(sourceSlot); int target = findTarget(to, stack, selected);
            if (target < 0) { message(owner, "目标空间没有合适的空位，或该物品不能存入。"); return; }
            int count = Math.min(one ? 1 : stack.getCount(), room(to, target, stack));
            if (!RaidPlayerAssets.move(owner, revision, from, sourceSlot, to, target, count)) message(owner, "物品未移动：请检查目标位置与可用空间。");
            refresh();
        }
        private int findTarget(RaidPlayerAssets.Store store, ItemStack stack, int selected) {
            List<ItemStack> target = items(store);
            int cap = switch (store) {
                case INVENTORY -> 36; case PREP -> 41; case WAREHOUSE -> target.size();
                case SAFE -> SafeBoxRules.Tier.valueOf(profile.getString("SafeTier")).slots(); case EXTRA -> profile.getInt("ExtraSlots");
            };
            if (selected >= 0) return selected < cap && room(store, selected, stack) > 0 ? selected : -1;
            if (store == RaidPlayerAssets.Store.PREP) {
                int armor = switch (owner.getEquipmentSlotForItem(stack)) { case FEET -> 36; case LEGS -> 37; case CHEST -> 38; case HEAD -> 39; default -> -1; };
                if (armor >= 0 && room(store, armor, stack) > 0) return armor;
                cap = 36;
            }
            for (int i = 0; i < cap; i++) if (!target.get(i).isEmpty() && room(store, i, stack) > 0) return i;
            for (int i = 0; i < cap; i++) if (target.get(i).isEmpty() && room(store, i, stack) > 0) return i;
            return -1;
        }
        private int room(RaidPlayerAssets.Store store, int slot, ItemStack stack) {
            if (store == RaidPlayerAssets.Store.SAFE && !SafeBoxRules.acceptsItem(stack.getMaxStackSize())) return 0;
            ItemStack existing = items(store).get(slot);
            if (!existing.isEmpty() && !RaidItemCodec.same(existing, stack)) return 0;
            int maximum = stack.getMaxStackSize();
            if (store == RaidPlayerAssets.Store.PREP && slot >= 36 && slot < 40) {
                EquipmentSlot expected = switch (slot) { case 36 -> EquipmentSlot.FEET; case 37 -> EquipmentSlot.LEGS; case 38 -> EquipmentSlot.CHEST; default -> EquipmentSlot.HEAD; };
                if (owner.getEquipmentSlotForItem(stack) != expected) return 0;
                maximum = 1;
            }
            return Math.max(0, maximum - existing.getCount());
        }
        private void result(RaidPurchases.Result result) {
            selection.missing = result.missing().stream().map(stack -> stack.getHoverName().getString() + " × " + stack.getCount()).toList();
            if (result.success()) message(owner, "已完成备战物资补充，花费 " + result.cost() + " 哈夫币。");
            else if (!result.missing().isEmpty()) {
                message(owner, "未成交，缺少以下物品或可购买货源：");
                for (ItemStack item : result.missing()) message(owner, "  " + item.getHoverName().getString() + " × " + item.getCount());
            } else message(owner, switch (result.reason()) {
                case "insufficient_funds" -> "余额不足，整单未成交。需要 " + result.cost() + " 哈夫币。";
                case "missing_plan" -> "所选方案尚未保存。"; case "occupied_prep_slot" -> "方案对应备战格已有其他物品，请先取回。";
                case "prep_capacity" -> "目标备战格空间不足。"; default -> "本次购买未成交，请检查货源、位置与需求。";
            });
            refresh();
        }
        private void refresh() { open(owner, view, selection, page); }
        @Override public boolean stillValid(Player player) {
            return player == owner && !RaidAssetService.pending(owner)
                    && (!RaidAssetService.isActive(owner.getServer(), owner.getUUID()) || view == View.STORAGE);
        }
        @Override public void clicked(int slot, int mouse, ClickType type, Player player) {
            if (player != owner || owner.containerMenu != this || type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) return;
            try {
                if (!stillValid(player)) { owner.closeContainer(); return; }
                if (RaidPlayerAssets.revision(owner) != revision) { message(owner, "物品状态已更新，已刷新页面。"); refresh(); return; }
                if (type == ClickType.QUICK_MOVE && (view == View.STORAGE || view == View.PREP)) {
                    Address from = address(slot);
                    if (from != null) {
                        RaidPlayerAssets.quickMove(owner, revision, from.store, from.index, active);
                        refresh(); return;
                    }
                }
                Action action = actions.get(slot);
                if (action != null) action.run(mouse, type == ClickType.QUICK_MOVE);
                else if (view != View.PURCHASES && (slot >= 54 && slot < PREP_STORAGE_BASE || view == View.PREP && slot >= 144 && slot < 149)) {
                    int inventorySlot = slots.get(slot).getContainerSlot();
                    RaidPlayerAssets.Store targetStore = view == View.STORAGE || view == View.PREP ? selection.targetStore : RaidPlayerAssets.Store.PREP;
                    if (view == View.STORAGE && targetStore != RaidPlayerAssets.Store.SAFE && targetStore != RaidPlayerAssets.Store.EXTRA) {
                        message(owner, "请先点击安全箱或拓展格，选择放入位置。"); return;
                    }
                    int target = selection.targetStore == targetStore ? selection.target : -1;
                    transfer(RaidPlayerAssets.Store.INVENTORY, inventorySlot, targetStore, target, mouse == 1);
                }
            } catch (IllegalArgumentException stale) { message(owner, "请求已过期，请重新选择。"); refresh(); }
            catch (IllegalStateException blocked) {
                com.mojang.logging.LogUtils.getLogger().error("Raid asset action blocked for {}", owner.getUUID(), blocked);
                message(owner, "物品操作尚未完成，已保留恢复记录。请联系管理员检查。"); owner.closeContainer();
            }
        }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return false; }
    }
    private static String slotName(int slot) {
        return switch (slot) { case 36 -> "靴子"; case 37 -> "护腿"; case 38 -> "胸甲"; case 39 -> "头盔"; case 40 -> "副手"; default -> "备战格 " + (slot + 1); };
    }
}
