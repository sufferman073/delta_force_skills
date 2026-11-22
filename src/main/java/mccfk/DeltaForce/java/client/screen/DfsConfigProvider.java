package mccfk.DeltaForce.java.client.screen;

import com.rzy.dealt_force_skills.client.ClientHudLayout;
import com.rzy.dealt_force_skills.config.ConfigAccess;
import com.rzy.dealt_force_skills.config.ConfigEntryData;
import com.rzy.dealt_force_skills.config.ConfigFileId;
import com.rzy.dealt_force_skills.config.ConfigHotReload;
import com.rzy.dealt_force_skills.network.C2S_ApplyConfigChanges;
import com.rzy.dealt_force_skills.network.C2S_RequestConfigSnapshot;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Uses the core editor's server-authorized snapshot/apply protocol. */
public class DfsConfigProvider implements IConfigProvider {
    private final Map<String, ConfigEntryData> entries = new LinkedHashMap<>();
    private final Map<String, String> dirty = new LinkedHashMap<>();
    private final Map<String, String> submitted = new LinkedHashMap<>();
    private boolean canEditServer;
    private boolean awaitingSnapshot;
    private String status = "";

    void refreshHud() {
        ClientHudLayout.snapshotEntries().forEach(entry -> { if (!dirty.containsKey(key(entry))) putEntry(entry); });
    }

    private final boolean dropsOnly;
    public DfsConfigProvider() { this(false); }
    protected DfsConfigProvider(boolean dropsOnly) {
        this.dropsOnly = dropsOnly;
        if (!dropsOnly) refreshHud();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            canEditServer = true;
            ConfigAccess.snapshotAllServerFiles().forEach(this::putEntry);
        } else if (!dropsOnly && mc.player != null && mc.player.hasPermissions(2)) {
            // 始终向服务端请求配置快照，由服务端检查权限决定是否授权
            // 避免客户端缓存hasPermission导致刚获得OP时无法获取服务器配置
            awaitingSnapshot = true;
            status = Component.translatable("config.dealt_force_skills.status.loading_server").getString();
            NetworkHandler.sendToServer(new C2S_RequestConfigSnapshot());
        }
    }

    private static String key(ConfigEntryData entry) {
        return entry.file().name() + "|" + entry.path();
    }

    private void putEntry(ConfigEntryData entry) {
        if (dropsOnly == (entry.file() == ConfigFileId.BASE && entry.path().equals("customdrop.enabled"))) entries.put(key(entry), entry);
    }

    ConfigEntryData entry(String path) {
        return entries.get(path);
    }

    void acceptSnapshot(List<ConfigEntryData> snapshot, boolean allowed) {
        // 无论是否主动等待服务器快照，都应更新权限状态
        // 防止在主菜单初始化后、进入多人服务器时 canEditServer 仍保持 true
        awaitingSnapshot = false;
        canEditServer = allowed;
        entries.values().removeIf(entry -> !entry.file().clientLocal());
        if (allowed) snapshot.stream().filter(entry -> !entry.file().clientLocal()).forEach(this::putEntry);
        dirty.keySet().removeIf(key -> !entries.containsKey(key));
        status = Component.translatable(allowed
                ? "config.dealt_force_skills.status.server_op"
                : "config.dealt_force_skills.status.server_denied", snapshot.size()).getString();
    }

    void acceptApplyResult(boolean success, int count, String code) {
        if (submitted.isEmpty()) return;
        // A partial acknowledgement must leave the submitted values available to retry.
        if (success && count == submitted.size()) {
            submitted.forEach((key, value) -> {
                ConfigEntryData entry = entries.get(key);
                if (entry != null) {
                    entries.put(key, new ConfigEntryData(entry.file(), entry.path(), entry.kind(), value, entry.comment()));
                    dirty.remove(key, value);
                }
            });
            status = Component.translatable("config.dealt_force_skills.status.saved", count).getString();
        } else {
            status = Component.translatable("config.dealt_force_skills.status.save_failed",
                    code == null ? "partial" : code).getString();
        }
        submitted.clear();
    }

    @Override
    public List<ConfigEntry> getConfigEntries() {
        return entries.entrySet().stream().map(pair -> {
            ConfigEntryData entry = pair.getValue();
            return new ConfigEntry(pair.getKey(), entry.path(), getValue(pair.getKey()), entry.valueText(),
                    entry.comment(), ConfigValueKind.valueOf(entry.kind().name()),
                    ConfigAccess.categoryKey(entry), ConfigAccess.subcategoryKey(entry));
        }).toList();
    }

    @Override
    public String getValue(String path) {
        ConfigEntryData entry = entries.get(path);
        return dirty.getOrDefault(path, entry == null ? "" : entry.valueText());
    }

    @Override
    public boolean setValue(String path, String value) {
        ConfigEntryData entry = entries.get(path);
        if (entry == null || !submitted.isEmpty() || (!entry.file().clientLocal() && !canEditServer)) return false;
        if (entry.valueText().equals(value)) dirty.remove(path);
        else dirty.put(path, value);
        // HUD客户端本地配置立即写入内存，实现设置界面内实时预览（不落盘）
        if (entry.file().clientLocal()) {
            try {
                ClientHudLayout.setValueInMemory(entry.path(), ConfigEntryData.parse(entry.kind(), value));
            } catch (RuntimeException ignored) {
            }
        }
        return true;
    }

    @Override
    public boolean validate() {
        if (!submitted.isEmpty() || awaitingSnapshot && dirty.keySet().stream().anyMatch(key -> !isClientLocal(key))) return false;
        for (var change : dirty.entrySet()) {
            ConfigEntryData entry = entries.get(change.getKey());
            try {
                if (entry == null || change.getValue().length() > 1024) throw new IllegalArgumentException();
                ConfigEntryData.parse(entry.kind(), change.getValue());
            } catch (RuntimeException error) {
                status = Component.translatable("config.dealt_force_skills.status.invalid",
                        entry == null ? change.getKey() : entry.path()).getString();
                return false;
            }
        }
        if (dirty.keySet().stream().filter(key -> !entries.get(key).file().clientLocal()).count() > 512) {
            status = "Too many server changes (maximum 512).";
            return false;
        }
        return true;
    }

    @Override
    public void save() {
        if (!validate()) return;
        List<C2S_ApplyConfigChanges.Change> changes = new ArrayList<>();
        List<String> saved = new ArrayList<>();
        boolean inWorld = Minecraft.getInstance().getConnection() != null;
        try {
            for (var change : dirty.entrySet()) {
                ConfigEntryData entry = entries.get(change.getKey());
                String value = change.getValue();
                if (entry.file().clientLocal()) {
                    if (ClientHudLayout.setValue(entry.path(), ConfigEntryData.parse(entry.kind(), value))) {
                        saved.add(change.getKey());
                    }
                } else if (canEditServer && inWorld) {
                    changes.add(C2S_ApplyConfigChanges.Change.from(entry, value));
                    submitted.put(change.getKey(), value);
                } else if (canEditServer && ConfigAccess.applyChange(entry.file(), entry.path(), entry.kind(), value)) {
                    ConfigAccess.flush(entry.file());
                    saved.add(change.getKey());
                }
            }
            if (!inWorld && saved.stream().anyMatch(key -> !entries.get(key).file().clientLocal())) ConfigHotReload.reloadAll();
            for (String key : saved) {
                ConfigEntryData entry = entries.get(key);
                entries.put(key, new ConfigEntryData(entry.file(), entry.path(), entry.kind(), dirty.remove(key), entry.comment()));
            }
            if (!changes.isEmpty()) {
                status = Component.translatable("config.dealt_force_skills.status.uploading", changes.size()).getString();
                NetworkHandler.sendToServer(new C2S_ApplyConfigChanges(changes));
            } else {
                status = Component.translatable("config.dealt_force_skills.status.saved_local", saved.size()).getString();
            }
        } catch (RuntimeException error) {
            submitted.clear();
            status = Component.translatable("config.dealt_force_skills.status.save_failed", error.getMessage()).getString();
        }
    }

    @Override public void cancel() { if (submitted.isEmpty()) { dirty.clear(); ClientHudLayout.forceReload(); } }
    @Override public boolean isDirty() { return !dirty.isEmpty(); }
    @Override public boolean isBusy() { return !submitted.isEmpty(); }
    @Override public String status() { return status; }

    /**
     * 根据配置项所属文件返回 C/S 标记：
     * HUD 文件 → 客户端本地（C，绿色），其余文件 → 服务端同步（S，红色）
     */
    @Override
    public boolean isClientLocal(String path) {
        ConfigEntryData entry = entries.get(path);
        return entry == null || entry.file().clientLocal();
    }

    /**
     * 服务器配置仅在拥有 OP 权限（本地/单人/服务器授权）时可编辑
     */
    @Override
    public boolean canEdit(String path) {
        ConfigEntryData entry = entries.get(path);
        if (entry == null) return false;
        return entry.file().clientLocal() || canEditServer;
    }
}
