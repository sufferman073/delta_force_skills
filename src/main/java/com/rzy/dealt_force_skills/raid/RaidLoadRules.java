package com.rzy.dealt_force_skills.raid;

import com.rzy.dealt_force_skills.raid.assets.RaidAssetService;
import com.rzy.dealt_force_skills.raid.assets.RaidItemCodec;
import com.rzy.dealt_force_skills.raid.assets.RaidPlayerAssets;
import com.rzy.dealt_force_skills.raid.rules.SafeBoxRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;

/** Occupied slots, never weight. Armor and offhand are excluded from both numerator and capacity. */
public final class RaidLoadRules {
    public record Load(int occupied, int capacity) { public double ratio() { return (double) occupied / capacity; } }
    private RaidLoadRules() { }
    public static double ratio(Entity actor) { return count(actor).ratio(); }
    public static Load count(Entity actor) {
        if (!(actor.level() instanceof ServerLevel level)) throw new IllegalArgumentException("Server actor required");
        CompoundTag actual;
        UUID player;
        if (actor instanceof ServerPlayer online) { player = online.getUUID(); actual = RaidAssetService.captureActual(online); }
        else if (actor instanceof RaidOfflineBodyEntity body) { player = body.playerId(); actual = body.actual(); }
        else throw new IllegalArgumentException("Only participants have inventory capacity");
        return count(level.getServer(), actual, RaidPlayerAssets.snapshot(level.getServer(), player));
    }
    public static Load count(MinecraftServer server, CompoundTag actual, CompoundTag profile) {
        int extra = profile.getInt("ExtraSlots");
        if (extra < 0 || extra > RaidPlayerAssets.MAX_EXTRA_SLOTS) throw new IllegalStateException("Invalid expansion capacity");
        SafeBoxRules.State safe = new SafeBoxRules.State(SafeBoxRules.Tier.valueOf(profile.getString("SafeTier")), profile.getInt("SafeUses"));
        int occupied = occupied(RaidItemCodec.readList(server, actual.getList("Inventory", Tag.TAG_COMPOUND), 41), 36)
                + occupied(RaidItemCodec.readList(server, actual.getList("EXTRA", Tag.TAG_COMPOUND), 75), extra)
                + occupied(RaidItemCodec.readList(server, actual.getList("SAFE", Tag.TAG_COMPOUND), 9), safe.tier().slots());
        return new Load(occupied, 36 + extra + safe.tier().slots());
    }
    private static int occupied(List<ItemStack> items, int unlocked) {
        int count = 0;
        for (int i = 0; i < unlocked; i++) if (!items.get(i).isEmpty()) count++;
        return count;
    }
}
