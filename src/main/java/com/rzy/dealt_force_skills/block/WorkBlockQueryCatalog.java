package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData.EntryView;
import com.rzy.dealt_force_skills.compat.JustEnoughCharactersCompat;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_UpdateAdvancedWorkBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;

/** Server-thread-only metadata indexes and bounded, cancellable page preview work. */
public final class WorkBlockQueryCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger("dealt_force_skills/work_catalog");
    private static final long TICK_BUDGET_NANOS = 2_000_000L;
    private static MinecraftServer owner;
    private static final Map<String, Catalog> CATALOGS = new HashMap<>();
    private static final LinkedHashMap<UUID, Preview> PENDING = new LinkedHashMap<>();

    private WorkBlockQueryCatalog() { }

    public static void clear() {
        CATALOGS.clear();
        PENDING.clear();
        owner = null;
    }

    public static void register(MinecraftServer server, String key, List<EntryView> entries) {
        if (owner != server) {
            clear();
            owner = server;
        }
        CATALOGS.put(key, new Catalog(entries));
        LOGGER.info("Prepared work catalog {} entries={}", key, entries.size());
    }

    public static void warnEntry(String catalog, Object id, Throwable error) {
        LOGGER.warn("Cannot inspect {} entry {}", catalog, id, error);
    }

    public static AdvancedWorkBlockData page(ServerPlayer player, BlockEntity block, String catalogKey,
                                             int blockType, int mode, String query, int requestedPage,
                                             double energy, BooleanSupplier valid, UnaryOperator<EntryView> resolve) {
        PENDING.remove(player.getUUID());
        Catalog catalog = owner == player.getServer() ? CATALOGS.get(catalogKey) : null;
        boolean metadata = AdvancedWorkBlockData.CATALOG_QUERY.equals(query);
        List<EntryView> matches;
        if (catalog == null) {
            matches = List.of();
        } else if (AdvancedWorkBlockData.PREVIEW_QUERY.equals(query)) {
            matches = requestedPage >= 0 && requestedPage < catalog.entries.size()
                    ? List.of(catalog.entries.get(requestedPage)) : List.of();
            requestedPage = 0;
        } else {
            matches = metadata ? catalog.entries : catalog.search(query);
        }
        int page = Math.max(0, Math.min(requestedPage,
                matches.isEmpty() ? 0 : (matches.size() - 1) / AdvancedWorkBlockData.PAGE_SIZE));
        int from = page * AdvancedWorkBlockData.PAGE_SIZE;
        List<EntryView> entries = new ArrayList<>(matches.subList(from,
                Math.min(matches.size(), from + AdvancedWorkBlockData.PAGE_SIZE)));
        String loading = Component.translatable("screen.dealt_force_skills.advanced_work.loading").getString();
        if (!metadata) entries.replaceAll(e -> new EntryView(e.id(), e.icon(), e.name(), loading, List.of(), 0, e.cost()));
        AdvancedWorkBlockData initial = new AdvancedWorkBlockData(block.getBlockPos(), blockType, mode,
                query, page, matches.size(), energy, entries);
        if (!metadata && !entries.isEmpty()) {
            PENDING.put(player.getUUID(), new Preview(player, block, initial, valid, resolve));
        }
        return initial;
    }

    public static void tick(MinecraftServer server) {
        if (owner != server) {
            return;
        }
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        // Rotate after each row so many players share the same global budget fairly.
        while (!PENDING.isEmpty() && System.nanoTime() < deadline) {
            var iterator = PENDING.entrySet().iterator();
            var next = iterator.next();
            Preview preview = next.getValue();
            iterator.remove();
            if (preview.valid()) {
                if (!preview.step()) {
                    PENDING.put(next.getKey(), preview);
                }
            }
        }
    }

    private static final class Catalog {
        private final List<EntryView> entries;
        private final List<String> text;
        private final Map<String, List<EntryView>> searches = new LinkedHashMap<>(16, 0.75F, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, List<EntryView>> eldest) {
                return size() > 48;
            }
        };

        private Catalog(List<EntryView> entries) {
            this.entries = entries.stream().sorted(Comparator.comparing(e -> e.id().toString())).toList();
            this.text = this.entries.stream().map(e -> e.id().toString().toLowerCase(Locale.ROOT)).toList();
        }

        private List<EntryView> search(String query) {
            String key = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                return entries;
            }
            return searches.computeIfAbsent(key, ignored -> {
                List<EntryView> result = new ArrayList<>();
                for (int i = 0; i < entries.size(); i++) {
                    String value = text.get(i);
                    if (value.contains(key) || JustEnoughCharactersCompat.matches(value, key)) {
                        result.add(entries.get(i));
                    }
                }
                return List.copyOf(result);
            });
        }
    }

    private static final class Preview {
        private final ServerPlayer player;
        private final BlockEntity block;
        private final AdvancedWorkBlockData initial;
        private final BooleanSupplier valid;
        private final UnaryOperator<EntryView> resolve;
        private final List<EntryView> entries;
        private int index;
        private int lastSentTick;

        private Preview(ServerPlayer player, BlockEntity block, AdvancedWorkBlockData initial,
                        BooleanSupplier valid, UnaryOperator<EntryView> resolve) {
            this.player = player;
            this.block = block;
            this.initial = initial;
            this.valid = valid;
            this.resolve = resolve;
            this.entries = new ArrayList<>(initial.entries());
            this.lastSentTick = player.getServer().getTickCount();
        }

        private boolean valid() {
            return player.getServer().getPlayerList().getPlayer(player.getUUID()) == player
                    && !block.isRemoved() && valid.getAsBoolean();
        }

        private boolean step() {
            EntryView entry = entries.get(index);
            try {
                EntryView resolved = resolve.apply(entry);
                // A loot sampler yields after each roll; resume this row on the next slice.
                if (resolved == null) return false;
                entries.set(index, resolved);
            } catch (RuntimeException | LinkageError error) {
                warnEntry("preview", entry.id(), error);
                entries.set(index, new EntryView(entry.id(), entry.icon(), entry.name(),
                        Component.translatable("screen.dealt_force_skills.advanced_work.preview_failed").getString(),
                        List.of(), 0, entry.cost()));
            }
            index++;
            boolean done = index == entries.size();
            int tick = player.getServer().getTickCount();
            if (done || tick - lastSentTick >= 5) {
                NetworkHandler.sendToPlayer(new S2C_UpdateAdvancedWorkBlock(new AdvancedWorkBlockData(
                        initial.pos(), initial.blockType(), initial.mode(), initial.query(), initial.page(),
                        initial.total(), initial.storedEnergy(), entries)), player);
                lastSentTick = tick;
            }
            return done;
        }
    }
}
