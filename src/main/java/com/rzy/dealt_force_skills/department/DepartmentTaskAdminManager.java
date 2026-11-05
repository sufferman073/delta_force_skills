package com.rzy.dealt_force_skills.department;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DepartmentTaskAdminManager {
    private static final String DATA_NAME = "dealt_force_skills_department_task_rules";

    public enum Action {
        FINISH("finish"), RESET("reset"), DELETE("delete");

        private final String id;

        Action(String id) { this.id = id; }
        public String id() { return id; }

        public static Action byId(String id) {
            for (Action action : values()) if (action.id.equals(id)) return action;
            return null;
        }
    }

    public record ServerResult(int onlinePlayers, int appliedPlayers) { }
    private record Rule(Action action, long revision) { }

    private DepartmentTaskAdminManager() { }

    public static boolean hasTask(String id) {
        return DepartmentTasks.tracker().definitions().containsKey(id);
    }

    public static boolean applyPlayer(ServerPlayer player, String id, Action action) {
        if (!hasTask(id) || !DepartmentTasks.tracker().administer(player, id, action)) return false;
        Rule rule = taskRules(player.getServer()).rules.get(id);
        if (rule != null) player.getPersistentData().getCompound(DepartmentTracker.KEY).getCompound(id)
                .putLong("ServerRevision", rule.revision());
        return true;
    }

    public static ServerResult setServer(MinecraftServer server, String id, Action action) {
        if (!hasTask(id)) return null;
        TaskRules rules = taskRules(server);
        Rule rule = rules.set(id, action);
        if (rule == null) return null;

        int onlinePlayers = 0;
        int appliedPlayers = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers++;
            if (applyRule(player, id, rule)) appliedPlayers++;
        }
        return new ServerResult(onlinePlayers, appliedPlayers);
    }

    public static void applyPending(ServerPlayer player) {
        if (player.getServer() == null) return;
        for (var entry : taskRules(player.getServer()).rules().entrySet()) {
            if (hasTask(entry.getKey())) applyRule(player, entry.getKey(), entry.getValue());
        }
    }

    private static boolean applyRule(ServerPlayer player, String id, Rule rule) {
        var persistent = player.getPersistentData();
        if (!persistent.contains(DepartmentTracker.KEY, Tag.TAG_COMPOUND))
            persistent.put(DepartmentTracker.KEY, new CompoundTag());
        CompoundTag tasks = persistent.getCompound(DepartmentTracker.KEY);
        CompoundTag current = tasks.getCompound(id);
        if (current.getLong("ServerRevision") >= rule.revision()) return true;
        if (!DepartmentTasks.tracker().administer(player, id, rule.action())) return false;

        tasks = persistent.getCompound(DepartmentTracker.KEY);
        CompoundTag updated = tasks.getCompound(id);
        updated.putLong("ServerRevision", rule.revision());
        tasks.put(id, updated);
        return true;
    }

    private static TaskRules taskRules(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TaskRules::new, TaskRules::load), DATA_NAME);
    }

    private static final class TaskRules extends SavedData {
        private final Map<String, Rule> rules = new LinkedHashMap<>();

        private static TaskRules load(CompoundTag tag, HolderLookup.Provider registries) {
            TaskRules data = new TaskRules();
            CompoundTag saved = tag.getCompound("Rules");
            for (String id : saved.getAllKeys()) {
                CompoundTag value = saved.getCompound(id);
                Action action = Action.byId(value.getString("Action"));
                long revision = value.getLong("Revision");
                if (action != null && revision > 0) data.rules.put(id, new Rule(action, revision));
            }
            return data;
        }

        private Rule set(String id, Action action) {
            Rule previous = rules.get(id);
            if (previous != null && previous.revision() == Long.MAX_VALUE) return null;
            Rule next = new Rule(action, previous == null ? 1 : previous.revision() + 1);
            rules.put(id, next);
            setDirty();
            return next;
        }

        private Map<String, Rule> rules() { return Map.copyOf(rules); }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag saved = new CompoundTag();
            rules.forEach((id, rule) -> {
                CompoundTag value = new CompoundTag();
                value.putString("Action", rule.action().id());
                value.putLong("Revision", rule.revision());
                saved.put(id, value);
            });
            tag.put("Rules", saved);
            return tag;
        }
    }
}
