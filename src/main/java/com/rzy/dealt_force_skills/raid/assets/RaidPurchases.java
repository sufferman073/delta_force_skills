package com.rzy.dealt_force_skills.raid.assets;

import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import com.rzy.dealt_force_skills.shop.SpecialOperationsBaseManager;
import mccfk.DeltaForce.java.data.DfWarehouseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Orders are quoted entirely before any source, balance or physical prep slot changes. */
public final class RaidPurchases {
    public enum Source { REDEMPTION, TRADING_HALL }
    public record Result(boolean success, String reason, long cost, List<ItemStack> missing) {
        public Result { missing = missing.stream().map(ItemStack::copy).toList(); }
    }
    private record Offer(ItemStack actualTemplate, long cost) { }
    private RaidPurchases() { }
    public static Result fillPlan(ServerPlayer player, long revision, int index, boolean buyMissing) {
        if (index < 0 || index >= 5) return failed("invalid_plan", List.of());
        CompoundTag account = RaidPlayerAssets.editable(player, revision, false), after = account.copy();
        CompoundTag profile = RaidPlayerAssets.profile(after);
        CompoundTag plans = profile.getCompound("Plans");
        if (!plans.contains(Integer.toString(index), Tag.TAG_LIST)) return failed("missing_plan", List.of());
        List<ItemStack> wanted = RaidItemCodec.readList(player.getServer(), plans.getList(Integer.toString(index), Tag.TAG_COMPOUND), 41);
        List<ItemStack> prep = RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.PREP);
        List<ItemStack> invBefore = RaidItemCodec.inventory(player), whBefore = DfWarehouseManager.loadWarehouse(player);
        List<ItemStack> invAfter = RaidPlayerAssets.copy(invBefore), whAfter = RaidPlayerAssets.copy(whBefore);
        List<ItemStack> missing = new ArrayList<>();
        long cost = 0;
        for (int slot = 0; slot < 41; slot++) {
            ItemStack requirement = wanted.get(slot), existing = prep.get(slot);
            if (requirement.isEmpty()) continue;
            if (!RaidPlayerAssets.accepts(player, RaidPlayerAssets.Store.PREP, slot, requirement)
                    || requirement.getCount() > requirement.getMaxStackSize()) return failed("invalid_plan_stack", List.of(requirement));
            if (!existing.isEmpty() && !RaidItemCodec.same(existing, requirement)) return failed("occupied_prep_slot", List.of(requirement));
            int need = Math.max(0, requirement.getCount() - existing.getCount());
            if (need == 0) continue;
            need = take(invAfter, requirement, need, prep, slot);
            need = take(whAfter, requirement, need, prep, slot);
            if (need == 0) continue;
            ItemStack shortfall = requirement.copy(); shortfall.setCount(need);
            Offer offer = buyMissing ? offer(player, requirement, need) : null;
            if (offer == null) { missing.add(shortfall); continue; }
            try { cost = Math.addExact(cost, offer.cost()); }
            catch (ArithmeticException overflow) { return failed("order_too_large", List.of()); }
            add(prep, slot, offer.actualTemplate(), need);
        }
        if (!missing.isEmpty()) return failed("missing_items", missing);
        if (cost > SpecialOperationsBaseManager.havocCoins(player)) return new Result(false, "insufficient_funds", cost, List.of());
        RaidPlayerAssets.put(player.getServer(), profile, RaidPlayerAssets.Store.PREP, prep);
        RaidPlayerAssets.bump(profile);
        boolean committed = RaidAssetTransactions.commit(player, account, after, invBefore, invAfter, whBefore, whAfter, cost);
        return new Result(committed, committed ? "ok" : "insufficient_funds", cost, List.of());
    }
    public static Result buyIntoPrep(ServerPlayer player, long revision, Source source, int offerIndex, int count, int prepSlot) {
        if (count <= 0 || prepSlot < 0 || prepSlot >= 41 || offerIndex < 0) return failed("invalid_purchase", List.of());
        CompoundTag account = RaidPlayerAssets.editable(player, revision, false), after = account.copy();
        CompoundTag profile = RaidPlayerAssets.profile(after);
        ItemStack template; long cost;
        if (source == Source.REDEMPTION) {
            List<ItemStack> sold = SpecialOperationsBaseManager.soldStacks(player);
            if (offerIndex >= sold.size()) return failed("unavailable", List.of());
            template = sold.get(offerIndex);
            cost = SpecialOperationsBaseManager.redemptionCost(player, template, count);
        } else {
            List<GtiTradingHallManager.Entry> entries = GtiTradingHallManager.entries();
            if (offerIndex >= entries.size()) return failed("unavailable", List.of());
            var entry = entries.get(offerIndex); template = entry.stack();
            try { cost = Math.multiplyExact(entry.price(), count); }
            catch (ArithmeticException overflow) { return failed("order_too_large", List.of()); }
        }
        if (cost < 0 || template.isEmpty()) return failed("unavailable", List.of());
        List<ItemStack> prep = RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.PREP);
        ItemStack existing = prep.get(prepSlot);
        int cap = prepSlot >= 36 && prepSlot < 40 ? 1 : template.getMaxStackSize();
        if (!RaidPlayerAssets.accepts(player, RaidPlayerAssets.Store.PREP, prepSlot, template)
                || !existing.isEmpty() && !RaidItemCodec.same(existing, template)
                || count > cap - existing.getCount()) return failed("prep_capacity", List.of());
        add(prep, prepSlot, template, count);
        RaidPlayerAssets.put(player.getServer(), profile, RaidPlayerAssets.Store.PREP, prep); RaidPlayerAssets.bump(profile);
        if (cost > SpecialOperationsBaseManager.havocCoins(player)) return new Result(false, "insufficient_funds", cost, List.of());
        boolean committed = RaidPlayerAssets.profileCommit(player, account, after, cost);
        return new Result(committed, committed ? "ok" : "insufficient_funds", cost, List.of());
    }
    private static Offer offer(ServerPlayer player, ItemStack requirement, int count) {
        Offer best = null;
        long redemption = SpecialOperationsBaseManager.redemptionCost(player, requirement, count);
        if (redemption >= 0) for (ItemStack actual : SpecialOperationsBaseManager.soldStacks(player)) {
            if (RaidItemCodec.same(actual, requirement)) { best = new Offer(actual.copy(), redemption); break; }
        }
        for (var entry : GtiTradingHallManager.entries()) if (RaidItemCodec.same(entry.stack(), requirement)) {
            long cost;
            try { cost = Math.multiplyExact(entry.price(), count); } catch (ArithmeticException overflow) { continue; }
            if (cost >= 0 && (best == null || cost < best.cost())) best = new Offer(entry.stack().copy(), cost);
        }
        return best;
    }
    private static int take(List<ItemStack> source, ItemStack wanted, int need, List<ItemStack> prep, int slot) {
        for (int i = 0; i < source.size() && need > 0; i++) {
            ItemStack actual = source.get(i);
            if (!RaidItemCodec.same(actual, wanted)) continue;
            int amount = Math.min(need, actual.getCount());
            add(prep, slot, actual, amount); actual.shrink(amount); need -= amount;
            if (actual.isEmpty()) source.set(i, ItemStack.EMPTY);
        }
        return need;
    }
    private static void add(List<ItemStack> target, int slot, ItemStack actual, int count) {
        ItemStack output = actual.copy(); output.setCount(target.get(slot).getCount() + count); target.set(slot, output);
    }
    private static Result failed(String reason, List<ItemStack> missing) { return new Result(false, reason, 0, missing); }
}
