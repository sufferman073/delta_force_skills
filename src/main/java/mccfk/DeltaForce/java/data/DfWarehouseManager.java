package mccfk.DeltaForce.java.data;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * DF 仓库管理器（独立于特勤处仓库）
 * 数据持久化：世界文件夹/.MCCFK_ALL_MODDATA/deltaforce/仓库/玩家名.json
 * 文件格式：
 * {
 *   "": [
 *     {
 *       "仓库页": 页码(第一页为 0),
 *       "页容器": 容器名(暂时只有"主仓库"),
 *       "页总格数": 本页格子数,
 *       "物品": [
 *         { "位置": 格子编号, "占用格数": 数量, "物品": 物品ID, "data": 完整物品数据(可选,保留附魔等) }
 *       ]
 *     }
 *   ]
 * }
 */
public final class DfWarehouseManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT_TAG = "dealt_force_skills.special_operations_base";
    private static final String WAREHOUSE_TAG = "Warehouse";

    private DfWarehouseManager() {
    }

    /**
     * 初始化仓库目录（服务端启动时调用）
     */
    public static void init(Path worldSavePath) {
        // 保留现有启动约定，仓库数据使用玩家持久化NBT存储
    }

    /**
     * 当前仓库页（页码，第一页为 0）
     */
    public static int warehousePage(ServerPlayer player) {
        return 0;
    }

    /**
     * 当前页容器名（暂时只有"主仓库"一个）
     */
    public static String warehouseContainer(ServerPlayer player) {
        return "主仓库";
    }

    /**
     * 页总格数：根据特勤处仓库设施等级动态计算主仓库容量
     * 使用 SpecialOperationsBaseManager.WAREHOUSE_SLOTS_PER_PAGE 映射表
     */
    public static int totalSlots(ServerPlayer player) {
        return com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager.warehouseCapacity(player);
    }

    /**
     * 读取仓库内容，返回大小=总格数的列表（空槽为 ItemStack.EMPTY）
     */
    public static List<ItemStack> loadWarehouse(ServerPlayer player) {
        int total = totalSlots(player);
        List<ItemStack> stacks = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            stacks.add(ItemStack.EMPTY);
        }
        ListTag stored = root(player).getList(WAREHOUSE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size(); i++) {
            CompoundTag entry = stored.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < total) {
                stacks.set(slot, ItemStack.parseOptional(player.registryAccess(), entry.getCompound("Stack")));
            }
        }
        return stacks;
    }

    /**
     * 保存仓库内容到 JSON
     */
    public static void saveWarehouse(ServerPlayer player, List<ItemStack> stacks) {
        ListTag result = new ListTag();
        ListTag previous = root(player).getList(WAREHOUSE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < previous.size(); i++) {
            CompoundTag entry = previous.getCompound(i);
            if (entry.getInt("Slot") >= stacks.size()) {
                result.add(entry.copy());
            }
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Stack", stack.saveOptional(player.registryAccess()));
                result.add(entry);
            }
        }
        root(player).put(WAREHOUSE_TAG, result);
    }

    /** Extracts from one warehouse slot only when its current stack still matches. */
    public static ItemStack extractMatching(ServerPlayer player, int slot, int count,
                                            Predicate<ItemStack> predicate) {
        if (player == null || slot < 0 || count <= 0 || predicate == null) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> warehouse = loadWarehouse(player);
        if (slot >= warehouse.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack stored = warehouse.get(slot);
        if (stored.isEmpty() || !predicate.test(stored)) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = stored.split(Math.min(count, stored.getCount()));
        if (stored.isEmpty()) {
            warehouse.set(slot, ItemStack.EMPTY);
        }
        saveWarehouse(player, warehouse);
        return extracted;
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_TAG, new CompoundTag());
        }
        return persistent.getCompound(ROOT_TAG);
    }

    /**
     * 物品栏 → 仓库：取玩家背包 invSlot 的 count 个放入仓库。
     *
     * @return 是否成功（失败会提示仓库已满）
     */
    public static boolean deposit(ServerPlayer player, int invSlot, int count) {
        if (invSlot < 0 || invSlot >= player.getInventory().items.size() || count <= 0) {
            return false;
        }
        ItemStack source = player.getInventory().getItem(invSlot);
        if (source.isEmpty()) {
            return false;
        }
        int move = Math.min(count, source.getCount());
        List<ItemStack> wh = loadWarehouse(player);
        ItemStack offered = source.copy();
        offered.setCount(move);
        ItemStack remainder = insert(wh, offered);
        int moved = offered.getCount() - remainder.getCount();
        if (moved <= 0) {
            player.displayClientMessage(Component.translatable("warehouse.deltaforce.full"), false);
            return false;
        }
        source.shrink(moved);
        player.getInventory().setChanged();
        saveWarehouse(player, wh);
        return true;
    }

    /**
     * 仓库 → 物品栏：取仓库 slot 的 count 个放回玩家背包。
     *
     * @return 是否成功（失败会提示背包已满）
     */
    public static boolean withdraw(ServerPlayer player, int slot, int count) {
        if (slot < 0 || count <= 0) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (slot >= wh.size() || wh.get(slot).isEmpty()) {
            return false;
        }
        ItemStack extracted = wh.get(slot).split(Math.min(count, wh.get(slot).getCount()));
        int requested = extracted.getCount();
        ItemStack remainder = addToInventory(player, extracted);
        if (!remainder.isEmpty()) {
            // 未放入的部分退回原槽；已经放入的部分仍算成功并同步快照。
            wh.get(slot).grow(remainder.getCount());
            saveWarehouse(player, wh);
            if (requested == remainder.getCount()) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.backpack_full"), false);
                return false;
            }
            player.getInventory().setChanged();
            return true;
        }
        saveWarehouse(player, wh);
        player.getInventory().setChanged();
        return true;
    }

    /**
     * 插入仓库：先合并相同物品再填空位，返回无法放入的剩余
     */
    private static ItemStack insert(List<ItemStack> wh, ItemStack offered) {
        ItemStack remainder = offered.copy();
        for (ItemStack stored : wh) {
            if (remainder.isEmpty()) {
                break;
            }
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remainder)) {
                int move = Math.min(remainder.getCount(), stored.getMaxStackSize() - stored.getCount());
                stored.grow(move);
                remainder.shrink(move);
            }
        }
        for (int i = 0; i < wh.size() && !remainder.isEmpty(); i++) {
            if (wh.get(i).isEmpty()) {
                int move = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                ItemStack placed = remainder.copy();
                placed.setCount(move);
                wh.set(i, placed);
                remainder.shrink(move);
            }
        }
        return remainder;
    }

    /**
     * 放入玩家背包：先合并再填空位，返回无法放入的剩余
     */
    private static ItemStack addToInventory(ServerPlayer player, ItemStack offered) {
        ItemStack remainder = offered.copy();
        for (ItemStack stored : player.getInventory().items) {
            if (remainder.isEmpty()) {
                break;
            }
            if (!stored.isEmpty() && ItemStack.isSameItemSameComponents(stored, remainder)) {
                int move = Math.min(remainder.getCount(), stored.getMaxStackSize() - stored.getCount());
                stored.grow(move);
                remainder.shrink(move);
            }
        }
        for (int i = 0; i < player.getInventory().items.size() && !remainder.isEmpty(); i++) {
            if (player.getInventory().items.get(i).isEmpty()) {
                int move = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                ItemStack placed = remainder.copy();
                placed.setCount(move);
                player.getInventory().items.set(i, placed);
                remainder.shrink(move);
            }
        }
        return remainder;
    }

    /**
     * 物品栏内部移动：一格移到另一格。
     * 目标格空→放入；同物品→合并堆叠；不同物品→整堆交换。
     *
     * @return 是否成功
     */
    public static boolean moveInInventory(ServerPlayer player, int from, int to, int count) {
        if (from < 0 || to < 0 || from == to
                || from >= player.getInventory().items.size() || to >= player.getInventory().items.size()) {
            return false;
        }
        ItemStack source = player.getInventory().getItem(from);
        if (source.isEmpty()) {
            return false;
        }
        ItemStack target = player.getInventory().getItem(to);
        int move = Math.min(count, source.getCount());
        if (target.isEmpty()) {
            ItemStack placed = source.copy();
            placed.setCount(move);
            if (move >= source.getCount()) {
                player.getInventory().items.set(from, ItemStack.EMPTY);
            } else {
                source.shrink(move);
            }
            player.getInventory().items.set(to, placed);
        } else if (ItemStack.isSameItemSameComponents(target, source)) {
            int add = Math.min(move, target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                return false;
            }
            target.grow(add);
            source.shrink(add);
        } else {
            // 不同物品：整堆交换
            player.getInventory().items.set(to, source.copy());
            player.getInventory().items.set(from, target.copy());
        }
        player.getInventory().setChanged();
        return true;
    }

    /**
     * 仓库内部移动：一格移到另一格。
     * 目标格空→放入；同物品→合并堆叠；不同物品→整堆交换。
     *
     * @return 是否成功
     */
    public static boolean moveInWarehouse(ServerPlayer player, int from, int to, int count) {
        if (from < 0 || to < 0 || from == to) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (from >= wh.size() || to >= wh.size() || wh.get(from).isEmpty()) {
            return false;
        }
        ItemStack source = wh.get(from);
        ItemStack target = wh.get(to);
        int move = Math.min(count, source.getCount());
        if (target.isEmpty()) {
            ItemStack placed = source.copy();
            placed.setCount(move);
            if (move >= source.getCount()) {
                wh.set(from, ItemStack.EMPTY);
            } else {
                source.shrink(move);
            }
            wh.set(to, placed);
        } else if (ItemStack.isSameItemSameComponents(target, source)) {
            int add = Math.min(move, target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                return false;
            }
            target.grow(add);
            source.shrink(add);
        } else {
            // 不同物品：整堆交换
            wh.set(to, source.copy());
            wh.set(from, target.copy());
        }
        saveWarehouse(player, wh);
        return true;
    }

    // ========== 精确放置（拖拽到指定格子） ==========

    /**
     * 物品栏 → 仓库指定格：目标格为空或同物品可堆叠则放入，否则提示占用。
     *
     * @return 是否成功
     */
    public static boolean depositTo(ServerPlayer player, int invSlot, int whTarget, int count) {
        if (invSlot < 0 || invSlot >= player.getInventory().items.size() || whTarget < 0 || count <= 0) {
            return false;
        }
        ItemStack source = player.getInventory().getItem(invSlot);
        if (source.isEmpty()) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (whTarget >= wh.size()) {
            return false;
        }
        int move = Math.min(count, source.getCount());
        ItemStack target = wh.get(whTarget);
        if (!target.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(target, source)) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.slot_occupied"), false);
                return false;
            }
            int add = Math.min(move, target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.slot_occupied"), false);
                return false;
            }
            target.grow(add);
            source.shrink(add);
        } else {
            ItemStack placed = source.copy();
            placed.setCount(move);
            wh.set(whTarget, placed);
            source.shrink(move);
        }
        player.getInventory().setChanged();
        saveWarehouse(player, wh);
        return true;
    }

    /**
     * 仓库指定格 → 物品栏指定格：目标格为空或同物品可堆叠则放入，否则提示占用。
     *
     * @return 是否成功
     */
    public static boolean withdrawTo(ServerPlayer player, int whSlot, int invTarget, int count) {
        if (whSlot < 0 || invTarget < 0 || invTarget >= player.getInventory().items.size() || count <= 0) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (whSlot >= wh.size() || wh.get(whSlot).isEmpty()) {
            return false;
        }
        int move = Math.min(count, wh.get(whSlot).getCount());
        ItemStack extracted = wh.get(whSlot).copy();
        extracted.setCount(move);
        ItemStack target = player.getInventory().getItem(invTarget);
        if (!target.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(target, extracted)) {
                if (move == wh.get(whSlot).getCount()) {
                    wh.set(whSlot, target.copy());
                    player.getInventory().items.set(invTarget, extracted);
                    player.getInventory().setChanged();
                    saveWarehouse(player, wh);
                    return true;
                }
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.backpack_slot_occupied"), false);
                return false;
            }
            int add = Math.min(move, target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.backpack_slot_occupied"), false);
                return false;
            }
            target.grow(add);
            wh.get(whSlot).shrink(add);
        } else {
            player.getInventory().items.set(invTarget, extracted);
            wh.get(whSlot).shrink(move);
        }
        player.getInventory().setChanged();
        saveWarehouse(player, wh);
        return true;
    }

    // ========== 装备交互 ==========

    /** 装备槽索引 → EquipmentSlot：0头 1甲 2裤 3鞋 4副手 5主手 */
    private static EquipmentSlot equipSlot(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            case 4 -> EquipmentSlot.OFFHAND;
            case 5 -> EquipmentSlot.MAINHAND;
            default -> null;
        };
    }

    /** 读取装备槽物品 */
    private static ItemStack getEquipped(ServerPlayer player, EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> player.getInventory().getItem(player.getInventory().selected);
            case OFFHAND -> player.getInventory().offhand.get(0);
            default -> player.getInventory().armor.get(slot.getIndex());
        };
    }

    /** 写入装备槽 */
    private static void setEquipped(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        switch (slot) {
            case MAINHAND -> player.getInventory().setItem(player.getInventory().selected, stack);
            case OFFHAND -> player.getInventory().offhand.set(0, stack);
            default -> player.getInventory().armor.set(slot.getIndex(), stack);
        }
        player.getInventory().setChanged();
    }

    /** 物品能否装备到指定槽位（主手/副手任意物品均可，盔甲槽需匹配部位） */
    private static boolean canEquip(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || slot == null) {
            return false;
        }
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            return true;
        }
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            return armor.getEquipmentSlot() == slot;
        }
        // 鞘翅等非 ArmorItem 但可穿胸甲的物品
        if (item instanceof ElytraItem) {
            return slot == EquipmentSlot.CHEST;
        }
        return false;
    }

    /**
     * 物品栏 → 装备槽：穿上。目标装备槽已有物品则与背包格整堆交换。
     *
     * @return 是否成功
     */
    public static boolean equipFromInventory(ServerPlayer player, int invSlot, int equipSlotIndex, int count) {
        EquipmentSlot slot = equipSlot(equipSlotIndex);
        if (slot == null || invSlot < 0 || invSlot >= player.getInventory().items.size()) {
            return false;
        }
        ItemStack source = player.getInventory().getItem(invSlot);
        if (source.isEmpty()) {
            return false;
        }
        if (!canEquip(source, slot)) {
            player.displayClientMessage(Component.translatable("warehouse.deltaforce.cannot_equip"), false);
            return false;
        }
        ItemStack worn = getEquipped(player, slot);
        if (worn.isEmpty()) {
            int move = Math.min(count, source.getCount());
            ItemStack equipped = source.copy();
            equipped.setCount(move);
            setEquipped(player, slot, equipped);
            source.shrink(move);
        } else {
            // 整堆交换：原装备回背包格，背包物品穿上
            player.getInventory().items.set(invSlot, worn.copy());
            setEquipped(player, slot, source.copy());
        }
        return true;
    }

    /**
     * 装备槽 → 物品栏指定格：卸下装备。
     *
     * @return 是否成功
     */
    public static boolean unequipToInventory(ServerPlayer player, int equipSlotIndex, int invTarget) {
        EquipmentSlot slot = equipSlot(equipSlotIndex);
        if (slot == null || invTarget < 0 || invTarget >= player.getInventory().items.size()) {
            return false;
        }
        ItemStack worn = getEquipped(player, slot);
        if (worn.isEmpty()) {
            return false;
        }
        ItemStack target = player.getInventory().getItem(invTarget);
        if (!target.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(target, worn)) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.backpack_slot_occupied"), false);
                return false;
            }
            int add = Math.min(worn.getCount(), target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.backpack_slot_occupied"), false);
                return false;
            }
            target.grow(add);
            worn.shrink(add);
        } else {
            player.getInventory().items.set(invTarget, worn.copy());
            setEquipped(player, slot, ItemStack.EMPTY);
        }
        return true;
    }

    /**
     * 仓库 → 装备槽：穿上。目标装备槽已有物品则与仓库格整堆交换。
     *
     * @return 是否成功
     */
    public static boolean equipFromWarehouse(ServerPlayer player, int whSlot, int equipSlotIndex, int count) {
        EquipmentSlot slot = equipSlot(equipSlotIndex);
        if (slot == null || whSlot < 0 || count <= 0) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (whSlot >= wh.size() || wh.get(whSlot).isEmpty()) {
            return false;
        }
        ItemStack source = wh.get(whSlot);
        if (!canEquip(source, slot)) {
            player.displayClientMessage(Component.translatable("warehouse.deltaforce.cannot_equip"), false);
            return false;
        }
        int move = Math.min(count, source.getCount());
        ItemStack worn = getEquipped(player, slot);
        if (worn.isEmpty()) {
            ItemStack equipped = source.copy();
            equipped.setCount(move);
            setEquipped(player, slot, equipped);
            source.shrink(move);
        } else {
            // 整堆交换：原装备回仓库格，仓库物品穿上
            wh.set(whSlot, worn.copy());
            setEquipped(player, slot, source.copy());
        }
        saveWarehouse(player, wh);
        return true;
    }

    /**
     * 装备槽 → 仓库指定格：卸下装备放入仓库。
     *
     * @return 是否成功
     */
    public static boolean unequipToWarehouse(ServerPlayer player, int equipSlotIndex, int whTarget) {
        EquipmentSlot slot = equipSlot(equipSlotIndex);
        if (slot == null || whTarget < 0) {
            return false;
        }
        List<ItemStack> wh = loadWarehouse(player);
        if (whTarget >= wh.size()) {
            return false;
        }
        ItemStack worn = getEquipped(player, slot);
        if (worn.isEmpty()) {
            return false;
        }
        ItemStack target = wh.get(whTarget);
        if (!target.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(target, worn)) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.slot_occupied"), false);
                return false;
            }
            int add = Math.min(worn.getCount(), target.getMaxStackSize() - target.getCount());
            if (add <= 0) {
                player.displayClientMessage(Component.translatable("warehouse.deltaforce.slot_occupied"), false);
                return false;
            }
            target.grow(add);
            worn.shrink(add);
        } else {
            wh.set(whTarget, worn.copy());
            setEquipped(player, slot, ItemStack.EMPTY);
        }
        saveWarehouse(player, wh);
        return true;
    }
}