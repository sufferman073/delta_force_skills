package com.rzy.dealt_force_skills.raid.assets;

import com.rzy.dealt_force_skills.mail.MailboxManager;
import com.rzy.dealt_force_skills.raid.rules.SafeBoxRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Physical loadout escrow and exactly-once mail settlement. Match eligibility belongs to the match ledger. */
public final class RaidAssetService {
    public enum SessionPhase { ENTERING, ACTIVE, SETTLING, TERMINAL }
    public record SettlementResult(UUID settlementId, SafeBoxRules.Outcome outcome,
                                   List<ItemStack> dropItems, boolean fresh) {
        public SettlementResult { dropItems = dropItems.stream().map(ItemStack::copy).toList(); }
    }
    private RaidAssetService() { }

    public static boolean begin(ServerPlayer player, UUID instanceId) {
        RaidAssetJournal.requireThread(player.getServer());
        if (pending(player)) return false;
        CompoundTag account = RaidAssetJournal.read(player.getServer(), player.getUUID());
        String phase = account.getCompound("Session").getString("Phase");
        if (!phase.isEmpty() && !phase.equals(SessionPhase.TERMINAL.name())) return false;
        player.closeContainer();
        if (!player.containerMenu.getCarried().isEmpty()) return false;
        com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager.clearRaidTransient(player);
        CompoundTag profile = RaidPlayerAssets.profile(account);
        List<ItemStack> prep = RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.PREP);
        CompoundTag original = new CompoundTag();
        original.put("Inventory", RaidItemCodec.writeList(player.getServer(), RaidItemCodec.inventory(player)));
        original.put("Curios", RaidEquipmentEscrow.capture(player));
        original.putInt("Selected", player.getInventory().selected);
        CompoundTag entry = new CompoundTag();
        entry.put("Inventory", RaidItemCodec.writeList(player.getServer(), prep));
        entry.put("Curios", new ListTag());
        entry.put("SAFE", RaidItemCodec.writeList(player.getServer(), RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.SAFE)));
        entry.put("EXTRA", RaidItemCodec.writeList(player.getServer(), RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.EXTRA)));
        CompoundTag session = new CompoundTag();
        session.putUUID("Instance", instanceId);
        session.putString("Phase", SessionPhase.ENTERING.name());
        session.put("Original", original); session.put("Entry", entry); session.put("Actual", entry.copy());
        account.put("Session", session);
        // Persist both ownership domains before removing survival items or equipping prep.
        RaidAssetJournal.write(player.getServer(), player.getUUID(), account);
        RaidEquipmentEscrow.clear(player);
        RaidItemCodec.setInventory(player, prep);
        player.getInventory().selected = 0;
        RaidAssetJournal.savePlayer(player);
        RaidPlayerAssets.put(player.getServer(), profile, RaidPlayerAssets.Store.PREP, RaidItemCodec.empty(41));
        RaidPlayerAssets.bump(profile);
        session.putString("Phase", SessionPhase.ACTIVE.name());
        RaidAssetJournal.write(player.getServer(), player.getUUID(), account);
        return true;
    }

    public static boolean isActive(MinecraftServer server, UUID playerId) {
        return session(server, playerId).getString("Phase").equals(SessionPhase.ACTIVE.name());
    }

    /** Transfers the disconnected body's actual ownership back to its still-eligible player. */
    public static boolean resumeActual(ServerPlayer player, UUID instanceId, CompoundTag actual) {
        MinecraftServer server = player.getServer();
        CompoundTag account = RaidAssetJournal.read(server, player.getUUID()), session = account.getCompound("Session");
        if (!session.getString("Phase").equals(SessionPhase.ACTIVE.name()) || !session.hasUUID("Instance")
                || !session.getUUID("Instance").equals(instanceId) || account.contains("Transaction")) return false;
        CompoundTag resumed = session.contains("Resuming", Tag.TAG_COMPOUND) ? session.getCompound("Resuming").copy() : actual.copy();
        validateActual(server, resumed);
        if (!session.contains("Resuming", Tag.TAG_COMPOUND)) {
            CompoundTag profile = RaidPlayerAssets.profile(account);
            for (var store : new RaidPlayerAssets.Store[]{RaidPlayerAssets.Store.SAFE, RaidPlayerAssets.Store.EXTRA}) {
                List<ItemStack> items = RaidItemCodec.readList(server, resumed.getList(store.name(), Tag.TAG_COMPOUND), store == RaidPlayerAssets.Store.SAFE ? 9 : 75);
                int unlocked = store == RaidPlayerAssets.Store.SAFE ? RaidPlayerAssets.safeState(profile).tier().slots() : profile.getInt("ExtraSlots");
                for (int i = unlocked; i < items.size(); i++) if (!items.get(i).isEmpty()) throw new IllegalStateException("Body has items beyond storage entitlement");
                RaidPlayerAssets.put(server, profile, store, items);
            }
            RaidPlayerAssets.bump(profile);
            session.put("Resuming", resumed.copy()); session.put("Actual", resumed.copy());
            RaidAssetJournal.write(server, player.getUUID(), account);
        }
        player.closeContainer();
        RaidEquipmentEscrow.clear(player);
        RaidItemCodec.setInventory(player, RaidItemCodec.readList(server, resumed.getList("Inventory", Tag.TAG_COMPOUND), 41));
        RaidEquipmentEscrow.restore(player, resumed.getList("Curios", Tag.TAG_COMPOUND));
        RaidAssetJournal.savePlayer(player);
        session.remove("Resuming");
        RaidAssetJournal.write(server, player.getUUID(), account);
        return true;
    }

    /** Server-only snapshot. The returned NBT is detached from every live inventory. */
    public static CompoundTag captureActual(ServerPlayer player) {
        CompoundTag profile = RaidPlayerAssets.profile(RaidAssetJournal.read(player.getServer(), player.getUUID()));
        CompoundTag actual = new CompoundTag();
        actual.put("Inventory", RaidItemCodec.writeList(player.getServer(), RaidItemCodec.inventory(player)));
        actual.put("Curios", RaidEquipmentEscrow.capture(player));
        actual.put("SAFE", RaidItemCodec.writeList(player.getServer(), RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.SAFE)));
        actual.put("EXTRA", RaidItemCodec.writeList(player.getServer(), RaidPlayerAssets.items(player.getServer(), profile, RaidPlayerAssets.Store.EXTRA)));
        return actual;
    }

    /** Also used for disconnected participants; callers update the actual dummy state before invoking this. */
    public static void checkpoint(MinecraftServer server, UUID playerId, CompoundTag actual) {
        CompoundTag account = RaidAssetJournal.read(server, playerId), session = account.getCompound("Session");
        if (!session.getString("Phase").equals(SessionPhase.ACTIVE.name())) return;
        validateActual(server, actual);
        if (session.getCompound("Actual").equals(actual)) return;
        session.put("Actual", actual.copy());
        RaidAssetJournal.write(server, playerId, account);
    }

    /** Durable before returning. Replays return the same ID and complete drop list for idempotent crate creation. */
    public static SettlementResult prepareSettlement(MinecraftServer server, UUID playerId, UUID instanceId,
                                                     SafeBoxRules.Outcome outcome, CompoundTag actual) {
        CompoundTag account = RaidAssetJournal.read(server, playerId), session = account.getCompound("Session");
        if (!session.hasUUID("Instance") || !session.getUUID("Instance").equals(instanceId))
            throw new IllegalStateException("Settlement does not own this raid asset session");
        if (session.contains("Settlement", Tag.TAG_COMPOUND)) return result(server, session.getCompound("Settlement"), false);
        if (account.contains("Transaction")) throw new IllegalStateException("Finish pending storage transaction before settling");
        String phase = session.getString("Phase");
        if (!phase.equals(SessionPhase.ACTIVE.name()) && !phase.equals(SessionPhase.ENTERING.name()))
            throw new IllegalStateException("Participant is already terminal");
        if (phase.equals(SessionPhase.ENTERING.name()) && outcome != SafeBoxRules.Outcome.SERVER_RESTART)
            throw new IllegalStateException("Incomplete entry can only be recovered as an interrupted session");
        CompoundTag profile = RaidPlayerAssets.profile(account);
        CompoundTag current = actual == null ? session.getCompound("Actual").copy() : actual.copy();
        // ENTERING may still contain the untouched survival inventory. Never count it as raid loot.
        if (phase.equals(SessionPhase.ENTERING.name())) current = emptyActual(server);
        validateActual(server, current);
        List<ItemStack> ordinary = ordinary(server, current);
        List<ItemStack> safe = RaidItemCodec.readList(server, current.getList("SAFE", Tag.TAG_COMPOUND), 9);
        SafeBoxRules.State safeState = RaidPlayerAssets.safeState(profile);
        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < safe.size(); i++) if (!safe.get(i).isEmpty()) occupied.add(i);
        SafeBoxRules.Settlement safeSettlement = SafeBoxRules.settle(safeState, outcome, occupied);
        List<ItemStack> mail = new ArrayList<>(), drops = new ArrayList<>();
        if (outcome == SafeBoxRules.Outcome.SERVER_RESTART) {
            RaidItemCodec.appendNonempty(mail, all(server, session.getCompound("Entry")));
            RaidItemCodec.appendNonempty(mail, ordinary);
            RaidItemCodec.appendNonempty(mail, safe);
        } else if (outcome == SafeBoxRules.Outcome.EXTRACTED) {
            RaidItemCodec.appendNonempty(mail, ordinary); RaidItemCodec.appendNonempty(mail, safe);
        } else {
            RaidItemCodec.appendNonempty(drops, ordinary);
            for (int index : safeSettlement.protectedSlots()) mail.add(safe.get(index).copy());
            profile.putInt("ExtraSlots", 0);
        }
        // Generated bound shields are skill state, including offline snapshots and restart entry compensation.
        mail.removeIf(com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager::isHandGuardShield);
        drops.removeIf(com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager::isHandGuardShield);
        // The last paid use protects every occupied slot before reducing the next-session entitlement.
        RaidPlayerAssets.safeState(profile, safeSettlement.nextState());
        for (var store : new RaidPlayerAssets.Store[]{RaidPlayerAssets.Store.PREP, RaidPlayerAssets.Store.SAFE, RaidPlayerAssets.Store.EXTRA})
            RaidPlayerAssets.put(server, profile, store, RaidItemCodec.empty(store == RaidPlayerAssets.Store.PREP ? 41 : store == RaidPlayerAssets.Store.SAFE ? 9 : 75));
        RaidPlayerAssets.bump(profile);
        CompoundTag settlement = new CompoundTag();
        settlement.putUUID("Id", UUID.randomUUID()); settlement.putString("Outcome", outcome.name());
        settlement.put("Mail", RaidItemCodec.writeList(server, mail)); settlement.put("Drops", RaidItemCodec.writeList(server, drops));
        settlement.putString("RestoreStage", "CLEARING");
        session.put("Settlement", settlement); session.put("Actual", current);
        session.remove("Resuming");
        session.putString("Phase", SessionPhase.SETTLING.name());
        RaidAssetJournal.write(server, playerId, account);
        return result(server, settlement, true);
    }

    /** Must run while match events block pickups, transfers, shops and new entry for this player. */
    public static boolean applySettlement(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        CompoundTag account = RaidAssetJournal.read(server, player.getUUID()), session = account.getCompound("Session");
        if (session.getString("Phase").equals(SessionPhase.TERMINAL.name())) return true;
        if (!session.getString("Phase").equals(SessionPhase.SETTLING.name())) return false;
        CompoundTag settlement = session.getCompound("Settlement");
        if (settlement.getString("RestoreStage").equals("CLEARING")) {
            player.closeContainer();
            com.rzy.dealt_force_skills.character.lexninjia.LexNinjiaStateManager.clearRaidTransient(player);
            RaidEquipmentEscrow.clear(player);
            RaidItemCodec.setInventory(player, RaidItemCodec.empty(41));
            RaidAssetJournal.savePlayer(player);
            settlement.putString("RestoreStage", "RESTORE");
            RaidAssetJournal.write(server, player.getUUID(), account);
        }
        CompoundTag original = session.getCompound("Original");
        List<ItemStack> survival = RaidItemCodec.readList(server, original.getList("Inventory", Tag.TAG_COMPOUND), 41);
        for (int i = 0; i < survival.size(); i++) {
            ItemStack current = player.getInventory().getItem(i), expected = survival.get(i);
            if (!current.isEmpty() && !RaidItemCodec.equal(current, expected))
                throw new IllegalStateException("Conflicting survival inventory recovery slot " + i);
        }
        RaidItemCodec.setInventory(player, survival);
        RaidEquipmentEscrow.restore(player, original.getList("Curios", Tag.TAG_COMPOUND));
        player.getInventory().selected = Math.max(0, Math.min(8, original.getInt("Selected")));
        UUID id = settlement.getUUID("Id");
        ListTag attachments = settlement.getList("Mail", Tag.TAG_COMPOUND);
        MailboxManager.sendRewardOnce(player, "raid/" + id, "Raid settlement: " + settlement.getString("Outcome"),
                RaidItemCodec.readList(server, attachments, attachments.size()), 0, 0, List.of());
        RaidAssetJournal.savePlayer(player);
        session.putString("Phase", SessionPhase.TERMINAL.name()); settlement.putString("RestoreStage", "DONE");
        RaidAssetJournal.write(server, player.getUUID(), account);
        return true;
    }

    /** Finishes durable work only. An ACTIVE session is left for the match manager to resume or abort. */
    public static boolean recover(ServerPlayer player) {
        if (!RaidAssetTransactions.recover(player)) return false;
        CompoundTag session = session(player.getServer(), player.getUUID());
        if (session.getString("Phase").equals(SessionPhase.ACTIVE.name()) && session.contains("Resuming", Tag.TAG_COMPOUND))
            return resumeActual(player, session.getUUID("Instance"), session.getCompound("Resuming"));
        if (session.getString("Phase").equals(SessionPhase.ENTERING.name())) {
            prepareSettlement(player.getServer(), player.getUUID(), session.getUUID("Instance"), SafeBoxRules.Outcome.SERVER_RESTART, null);
            return applySettlement(player);
        }
        if (session.getString("Phase").equals(SessionPhase.SETTLING.name())) return applySettlement(player);
        return true;
    }

    public static boolean pending(ServerPlayer player) {
        CompoundTag account = RaidAssetJournal.read(player.getServer(), player.getUUID());
        String phase = account.getCompound("Session").getString("Phase");
        return account.contains("Transaction") || account.getCompound("Session").contains("Resuming")
                || phase.equals(SessionPhase.ENTERING.name()) || phase.equals(SessionPhase.SETTLING.name());
    }
    public static CompoundTag session(MinecraftServer server, UUID playerId) {
        return RaidAssetJournal.read(server, playerId).getCompound("Session").copy();
    }
    private static SettlementResult result(MinecraftServer server, CompoundTag settlement, boolean fresh) {
        ListTag drops = settlement.getList("Drops", Tag.TAG_COMPOUND);
        return new SettlementResult(settlement.getUUID("Id"), SafeBoxRules.Outcome.valueOf(settlement.getString("Outcome")),
                RaidItemCodec.readList(server, drops, drops.size()), fresh);
    }
    private static void validateActual(MinecraftServer server, CompoundTag actual) {
        for (String key : new String[]{"Inventory", "Curios", "SAFE", "EXTRA"})
            if (!actual.contains(key, Tag.TAG_LIST)) throw new IllegalArgumentException("Incomplete actual loadout: " + key);
        all(server, actual);
    }
    private static CompoundTag emptyActual(MinecraftServer server) {
        CompoundTag result = new CompoundTag();
        for (String key : new String[]{"Inventory", "Curios", "SAFE", "EXTRA"}) result.put(key, new ListTag());
        return result;
    }
    private static List<ItemStack> ordinary(MinecraftServer server, CompoundTag actual) {
        List<ItemStack> items = new ArrayList<>();
        RaidItemCodec.appendNonempty(items, RaidItemCodec.readList(server, actual.getList("Inventory", Tag.TAG_COMPOUND), 41));
        RaidItemCodec.appendNonempty(items, RaidEquipmentEscrow.flatten(server, actual.getList("Curios", Tag.TAG_COMPOUND)));
        RaidItemCodec.appendNonempty(items, RaidItemCodec.readList(server, actual.getList("EXTRA", Tag.TAG_COMPOUND), 75));
        return items;
    }
    private static List<ItemStack> all(MinecraftServer server, CompoundTag actual) {
        List<ItemStack> result = ordinary(server, actual);
        RaidItemCodec.appendNonempty(result, RaidItemCodec.readList(server, actual.getList("SAFE", Tag.TAG_COMPOUND), 9));
        return result;
    }
}
