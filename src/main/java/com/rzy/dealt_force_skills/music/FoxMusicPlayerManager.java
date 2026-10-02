package com.rzy.dealt_force_skills.music;

import com.rzy.dealt_force_skills.compat.CuriosMusicCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_FoxMusicPlayback;
import com.rzy.dealt_force_skills.network.S2C_FoxMusicSnapshot;
import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FoxMusicPlayerManager {
    public enum Action { PAGE, INSERT, TAKE, SELECT, TOGGLE, PREVIOUS, NEXT, MODE }
    private record Key(UUID owner, UUID device) { }
    private static final Map<Key, Session> SESSIONS = new HashMap<>();
    private static final net.minecraft.resources.ResourceLocation SPEED = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fox_music_speed");
    private static final net.minecraft.resources.ResourceLocation ARMOR = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fox_music_armor");
    private static final net.minecraft.resources.ResourceLocation TOUGHNESS = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fox_music_toughness");
    private static final net.minecraft.resources.ResourceLocation HEALTH = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "fox_music_health");

    private FoxMusicPlayerManager() { }

    public static List<ItemStack> carried(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        Set<ItemStack> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) add(result, seen, player.getInventory().getItem(i));
        for (ItemStack stack : CuriosMusicCompat.equipped(player)) add(result, seen, stack);
        return result;
    }
    private static void add(List<ItemStack> result, Set<ItemStack> seen, ItemStack stack) {
        if (stack.is(ModItems.FOX_MUSIC_PLAYER.get()) && seen.add(stack)) result.add(stack);
    }
    public static int discCount(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : carried(player)) count += FoxMusicData.count(stack);
        return count;
    }
    public static void open(ServerPlayer player, ItemStack stack, int requestedPage, String feedback) {
        if (!stack.is(ModItems.FOX_MUSIC_PLAYER.get())) return;
        List<ItemStack> discs = FoxMusicData.discs(stack, player.level());
        CompoundTag tag = FoxMusicData.data(stack);
        int page = Math.max(0, Math.min(Math.max(0, (discs.size() - 1) / FoxMusicData.PAGE_SIZE), requestedPage));
        int start = page * FoxMusicData.PAGE_SIZE;
        NetworkHandler.sendToPlayer(new S2C_FoxMusicSnapshot(FoxMusicData.id(stack), page, discs.size(),
                tag.getInt("Track"), tag.getBoolean("Playing"), FoxMusicData.mode(tag).ordinal(),
                discs.subList(start, Math.min(discs.size(), start + FoxMusicData.PAGE_SIZE)), feedback), player);
    }
    public static void action(ServerPlayer player, UUID id, Action action, int index, int page) {
        if (!player.isAlive() || player.isSpectator()) return;
        ItemStack stack = carried(player).stream().filter(s -> FoxMusicData.matches(s, id)).findFirst().orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return;
        List<ItemStack> discs = FoxMusicData.discs(stack, player.level());
        CompoundTag tag = FoxMusicData.data(stack);
        String feedback = "";
        switch (action) {
            case PAGE -> { }
            case INSERT -> {
                Set<String> stored = new HashSet<>();
                for (ItemStack disc : discs) stored.add(FoxMusicData.kind(disc, player.level()));
                int inserted = 0;
                for (int i = 0; i < player.getInventory().getContainerSize() && discs.size() < FoxMusicData.CAPACITY; i++) {
                    ItemStack disc = player.getInventory().getItem(i);
                    String kind = FoxMusicData.kind(disc, player.level());
                    if (!kind.isEmpty() && stored.add(kind)) {
                        ItemStack copy = disc.copy(); copy.setCount(1); discs.add(copy); disc.shrink(1); inserted++;
                    }
                }
                FoxMusicData.store(stack, discs, player.level());
                feedback = inserted > 0 ? "inserted" : discs.size() >= FoxMusicData.CAPACITY ? "full" : "no_new_discs";
            }
            case TAKE -> {
                if (index < 0 || index >= discs.size()) { feedback = "invalid"; break; }
                ItemStack removed = discs.remove(index);
                int oldTrack = tag.getInt("Track");
                FoxMusicData.store(stack, discs, player.level());
                tag = FoxMusicData.data(stack);
                if (index == oldTrack) FoxMusicData.select(stack, Math.min(index, discs.size() - 1), tag.getBoolean("Playing"));
                else if (index < oldTrack) { tag.putInt("Track", oldTrack - 1); FoxMusicData.save(stack, tag); }
                if (!player.getInventory().add(removed) && !removed.isEmpty()) player.drop(removed, false);
                feedback = "removed";
            }
            case SELECT -> {
                if (index < 0 || index >= discs.size() || FoxMusicData.song(discs.get(index), player.level()) == null) feedback = "invalid";
                else FoxMusicData.select(stack, index, true);
            }
            case TOGGLE -> {
                if (discs.isEmpty()) { feedback = "empty"; break; }
                if (FoxMusicData.song(FoxMusicData.current(stack, player.level()), player.level()) == null) { feedback = "invalid"; break; }
                tag.putBoolean("Playing", !tag.getBoolean("Playing")); FoxMusicData.save(stack, tag);
            }
            case PREVIOUS, NEXT -> {
                if (discs.isEmpty()) feedback = "empty";
                else navigate(player, stack, discs.size(), action == Action.PREVIOUS ? -1 : 1, false);
            }
            case MODE -> { tag.putInt("Mode", (FoxMusicData.mode(tag).ordinal() + 1) % FoxMusicData.Mode.values().length); FoxMusicData.save(stack, tag); }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        tick(player, true);
        open(player, stack, page, feedback);
    }
    private static void navigate(ServerPlayer player, ItemStack stack, int size, int direction, boolean automatic) {
        CompoundTag data = FoxMusicData.data(stack);
        int current = Math.floorMod(data.getInt("Track"), size);
        FoxMusicData.Mode mode = FoxMusicData.mode(data);
        int next = Math.floorMod(current + direction, size);
        if (automatic && mode == FoxMusicData.Mode.REPEAT_ONE) next = current;
        else if (mode == FoxMusicData.Mode.SHUFFLE && size > 1) next = (current + 1 + player.getRandom().nextInt(size - 1)) % size;
        FoxMusicData.select(stack, next, true);
    }
    public static void tick(ServerPlayer player, boolean force) {
        if (!force && player.tickCount % 5 != 0) return;
        List<ItemStack> players = player.isAlive() ? carried(player) : List.of();
        int count = players.stream().mapToInt(FoxMusicData::count).sum();
        modifier(player.getAttribute(Attributes.ATTACK_SPEED), SPEED, count * 0.025, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player.getAttribute(Attributes.ARMOR), ARMOR, count, AttributeModifier.Operation.ADD_VALUE);
        modifier(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS, count * 0.5, AttributeModifier.Operation.ADD_VALUE);
        modifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH, count, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        Set<Key> active = new HashSet<>();
        for (ItemStack stack : players) {
            if (FoxMusicData.count(stack) == 0) continue;
            Key key = new Key(player.getUUID(), FoxMusicData.id(stack));
            if (!active.add(key)) continue;
            Session session = SESSIONS.computeIfAbsent(key, ignored -> new Session());
            session.update(player, stack, key, force);
        }
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().owner().equals(player.getUUID()) && !active.contains(entry.getKey())) {
                entry.getValue().stop(player, entry.getKey()); iterator.remove();
            }
        }
    }
    private static void modifier(AttributeInstance attribute, net.minecraft.resources.ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && old.amount() == amount) return;
        attribute.removeModifier(id);
        if (amount > 0) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
    public static void logout(ServerPlayer player) {
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().owner().equals(player.getUUID())) { entry.getValue().stop(player, entry.getKey()); iterator.remove(); }
        }
    }
    public static void clear() { SESSIONS.clear(); }

    private static final class Session {
        private int generation = Integer.MIN_VALUE;
        private int elapsed;
        private long lastTick;
        private long lastBroadcast;
        private boolean playing;
        private FoxMusicData.Song song;
        private net.minecraft.resources.ResourceLocation dimension;
        private final Set<UUID> listeners = new HashSet<>();

        private void update(ServerPlayer owner, ItemStack stack, Key key, boolean force) {
            long now = owner.level().getGameTime();
            CompoundTag data = FoxMusicData.data(stack);
            FoxMusicData.Song currentSong = FoxMusicData.song(FoxMusicData.current(stack, owner.level()), owner.level());
            if (currentSong == null) { stop(owner, key); return; }
            boolean changed = generation != data.getInt("Generation") || !currentSong.equals(song)
                    || !owner.level().dimension().location().equals(dimension);
            if (changed) elapsed = 0;
            else if (playing) elapsed = (int) Math.min(Integer.MAX_VALUE, elapsed + Math.max(0L, now - lastTick));
            playing = data.getBoolean("Playing");
            if (playing && elapsed >= currentSong.ticks()) {
                navigate(owner, stack, FoxMusicData.count(stack), 1, true);
                data = FoxMusicData.data(stack);
                currentSong = FoxMusicData.song(FoxMusicData.current(stack, owner.level()), owner.level());
                if (currentSong == null) { data.putBoolean("Playing", false); FoxMusicData.save(stack, data); stop(owner, key); return; }
                elapsed = 0; changed = true;
            }
            generation = data.getInt("Generation"); song = currentSong;
            dimension = owner.level().dimension().location(); lastTick = now;
            if (!changed && !force && now - lastBroadcast < 20L) return;
            lastBroadcast = now;
            Set<UUID> audible = new HashSet<>();
            for (ServerPlayer listener : owner.serverLevel().players()) {
                if (listener == owner || listener.distanceToSqr(owner) <= 64.0 * 64.0) {
                    audible.add(listener.getUUID());
                    NetworkHandler.sendToPlayer(new S2C_FoxMusicPlayback(key.device(), owner.getUUID(), owner.getId(),
                            song.sound(), generation, elapsed, playing, false), listener);
                }
            }
            for (UUID listener : listeners) if (!audible.contains(listener)) sendStop(owner, key, listener);
            listeners.clear(); listeners.addAll(audible);
        }
        private void sendStop(ServerPlayer owner, Key key, UUID id) {
            ServerPlayer listener = owner.getServer().getPlayerList().getPlayer(id);
            if (listener != null && song != null) NetworkHandler.sendToPlayer(new S2C_FoxMusicPlayback(key.device(),
                    owner.getUUID(), owner.getId(), song.sound(), generation, elapsed, false, true), listener);
        }
        private void stop(ServerPlayer owner, Key key) {
            for (UUID listener : listeners) sendStop(owner, key, listener);
            listeners.clear(); playing = false;
        }
    }
}
