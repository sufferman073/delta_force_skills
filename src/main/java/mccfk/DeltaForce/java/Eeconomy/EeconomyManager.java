package mccfk.DeltaForce.java.Eeconomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 经济系统管理器
 * 管理DF模组的经济相关数据，包括代币的初始化、查询与更新
 * 数据持久化路径：世界文件夹/.mccfk_alldata/DeltaForce/economy/
 *
 * 支持的货币类型：
 * - HAVOC_COIN (havoc_coin)：哈夫币
 * - DELTA_COIN (delta_coin)：三角币
 *
 * 限制数据从 manager.json 读取
 */
public class EeconomyManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static EeconomyManager instance;

    // 记录读取失败的文件信息
    private final java.util.List<FailedFile> failedFiles = new java.util.ArrayList<>();

    public static class FailedFile {
        public final String fileName;
        public final String filePath;
        public FailedFile(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }

    // 初始数量配置
    private final Map<CurrencyType, Long> initialQuantity = new HashMap<>();
    // 最大值配置
    private final Map<CurrencyType, Long> max = new HashMap<>();
    // 最小值配置
    private final Map<CurrencyType, Long> min = new HashMap<>();

    // 玩家货币余额缓存
    private final Map<String, Map<CurrencyType, Long>> playerBalances = new HashMap<>();

    // 各货币的数据文件目录 economy/<currency_id>/
    private final Map<CurrencyType, Path> currencyDirs = new HashMap<>();

    private EeconomyManager() {
    }

    /**
     * 获取单例实例
     */
    public static EeconomyManager getInstance() {
        if (instance == null) {
            instance = new EeconomyManager();
        }
        return instance;
    }

    /**
     * 从 manager.json 文件加载货币限制配置
     * @param configPath manager.json 的完整路径
     */
    public void loadConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject root = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();

            parseArray(root, "initial_quantity", initialQuantity);
            parseArray(root, "max", max);
            parseArray(root, "min", min);
            // Upgrade only the historical shipped limit; retain administrator custom limits.
            if (max.getOrDefault(CurrencyType.HAVOC_COIN, Long.MAX_VALUE) == Integer.MAX_VALUE) {
                max.put(CurrencyType.HAVOC_COIN, Long.MAX_VALUE);
                for (JsonElement element : root.getAsJsonArray("max")) {
                    JsonObject entry = element.getAsJsonObject();
                    if (entry.has("havoc_coin")) entry.addProperty("havoc_coin", Long.toString(Long.MAX_VALUE));
                }
                try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
                }
            }

        } catch (IOException | JsonSyntaxException e) {
            Path noPath = configPath.resolveSibling(configPath.getFileName() + ".no");
            try {
                Files.move(configPath, noPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}

            String absPath = configPath.toAbsolutePath().toString();
            LOGGER.error("无法读取经济配置文件，已重命名为: {}.no", absPath);
            failedFiles.add(new FailedFile(configPath.getFileName().toString(), absPath));
        }
    }

    /**
     * 获取读取失败的文件路径列表
     */
    public java.util.List<FailedFile> getFailedFiles() {
        return java.util.List.copyOf(failedFiles);
    }

    /**
     * 设置指定货币的数据目录（如 economy/havoc_coin）
     */
    public void setCurrencyDir(CurrencyType type, Path dir) {
        this.currencyDirs.put(type, dir);
    }

    /**
     * 获取指定货币的数据目录
     */
    public Path getCurrencyDir(CurrencyType type) {
        return currencyDirs.get(type);
    }

    /**
     * 检查玩家是否已有指定货币数据文件（新版保留，供 SpecialOperationsBaseManager 调用）
     */
    public boolean hasCurrencyData(String playerName, CurrencyType type) {
        Path directory = currencyDirs.get(type);
        return directory != null && Files.exists(directory.resolve(playerName + ".json"));
    }

    /**
     * 从所有货币目录中收集已有玩家名（去重）
     */
    public java.util.List<String> getPlayerNamesFromFiles() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (CurrencyType type : CurrencyType.values()) {
            Path dir = currencyDirs.get(type);
            if (dir == null || !Files.exists(dir)) continue;
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .forEach(names::add);
            } catch (IOException ignored) {}
        }
        return java.util.List.copyOf(names);
    }

    /**
     * 初始化玩家经济数据
     * 检查各货币文件是否存在，不存在则用默认数量创建
     */
    public void initPlayerData(String playerName) {
        for (CurrencyType type : CurrencyType.values()) {
            Path dir = currencyDirs.get(type);
            if (dir == null) continue;
            Path file = dir.resolve(playerName + ".json");
            if (Files.notExists(file)) {
                saveCurrencyData(playerName, type, getInitialQuantity(type));
            }
        }
    }

    /**
     * 保存单种货币数据到文件 economy/<currency_id>/<playerName>.json
     * 格式: {"":"<amount>"}
     */
    private void saveCurrencyData(String playerName, CurrencyType type, long amount) {
        Path dir = currencyDirs.get(type);
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(playerName + ".json");
            JsonObject obj = readCurrencyDocument(file);
            obj.addProperty("", String.valueOf(amount));
            writeCurrencyDocument(file,obj);
        } catch (IOException e) {
            LOGGER.error("无法保存玩家经济数据: {}/{}.json", dir, playerName);
        }
    }

    private static JsonObject readCurrencyDocument(Path file) throws IOException {
        if (!Files.exists(file)) return new JsonObject();
        try (Reader reader=Files.newBufferedReader(file)) {
            JsonObject document=new com.google.gson.Gson().fromJson(reader, JsonObject.class);
            if(document==null)throw new IOException("Empty currency document: "+file);
            return document;
        } catch (RuntimeException error) {
            throw new IOException("Invalid currency document: "+file,error);
        }
    }

    private static void writeCurrencyDocument(Path file,JsonObject document) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary=Files.createTempFile(file.getParent(),"mail-credit-",".tmp");
        try {
            try (BufferedWriter writer=Files.newBufferedWriter(temporary)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(document,writer);
            }
            try {Files.move(temporary,file,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {Files.move(temporary,file,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporary);}
    }

    /** Balance and mail receipt share one atomic file, so restoring older player NBT cannot credit twice. */
    public synchronized boolean creditMail(String playerName,java.util.UUID mailId,long amount) {
        if(amount<=0)return true;
        CurrencyType type=CurrencyType.HAVOC_COIN;Path dir=currencyDirs.get(type);
        if(dir==null)return false;
        Path file=dir.resolve(playerName+".json");
        try {
            JsonObject document=readCurrencyDocument(file);
            JsonObject receipts=document.has("mailCredits")?document.getAsJsonObject("mailCredits"):new JsonObject();
            String key=mailId.toString();
            if(receipts.has(key))return receipts.get(key).getAsLong()==amount;
            long current=getBalance(playerName,type);
            if(current<0 || amount>getMax(type)-current)return false;
            long next=current+amount;
            document.addProperty("",Long.toString(next));receipts.addProperty(key,Long.toString(amount));
            document.add("mailCredits",receipts);
            writeCurrencyDocument(file,document);
            playerBalances.computeIfAbsent(playerName,ignored->new HashMap<>()).put(type,next);
            return true;
        } catch(IOException|RuntimeException error) {
            LOGGER.error("Unable to credit mailbox reward for {}",playerName,error);
            return false;
        }
    }

    /**
     * 从文件加载单种货币数据
     * 首次创建（文件不存在）返回默认数量
     */
    private long loadCurrencyData(String playerName, CurrencyType type) {
        Path dir = currencyDirs.get(type);
        if (dir == null) return getInitialQuantity(type);
        Path file = dir.resolve(playerName + ".json");
        if (!Files.exists(file)) return getInitialQuantity(type);

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject obj = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
            String val = obj.get("").getAsString();
            if (val != null && !val.isEmpty()) {
                return Long.parseLong(val);
            }
        } catch (IOException | NumberFormatException | JsonSyntaxException e) {
            Path noPath = file.resolveSibling(file.getFileName() + ".no");
            try {
                Files.move(file, noPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            String absPath = file.toAbsolutePath().toString();
            LOGGER.error("无法读取玩家经济数据，已重命名为: {}.no", absPath);
            failedFiles.add(new FailedFile(file.getFileName().toString(), absPath));
            saveCurrencyData(playerName, type, getInitialQuantity(type));
        }
        return getInitialQuantity(type);
    }

    private void parseArray(JsonObject root, String key, Map<CurrencyType, Long> target) {
        JsonArray array = root.getAsJsonArray(key);
        if (array == null) return;

        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> pair : entry.entrySet()) {
                CurrencyType type = CurrencyType.fromId(pair.getKey());
                if (type != null) {
                    target.put(type, Long.parseLong(pair.getValue().getAsString()));
                }
            }
        }
    }

    /**
     * 获取指定货币的初始数量
     */
    public long getInitialQuantity(CurrencyType type) {
        return initialQuantity.getOrDefault(type, 0L);
    }

    /**
     * 获取指定货币的最大值
     */
    public long getMax(CurrencyType type) {
        return max.getOrDefault(type, Long.MAX_VALUE);
    }

    /**
     * 获取指定货币的最小值
     */
    public long getMin(CurrencyType type) {
        return min.getOrDefault(type, 0L);
    }

    /**
     * 获取玩家指定货币余额（从缓存或文件读取）
     */
    public long getBalance(String playerName, CurrencyType type) {
        Map<CurrencyType, Long> balances = playerBalances.get(playerName);
        if (balances == null) {
            balances = new HashMap<>();
            playerBalances.put(playerName, balances);
        }
        return balances.computeIfAbsent(type, t -> loadCurrencyData(playerName, t));
    }

    /**
     * 设置玩家指定货币余额（自动限制在[min, max]范围内，写入文件）
     */
    public void setBalance(String playerName, CurrencyType type, long amount) {
        long clamped = Math.max(getMin(type), Math.min(getMax(type), amount));
        Map<CurrencyType, Long> balances = playerBalances
            .computeIfAbsent(playerName, k -> new HashMap<>());
        balances.put(type, clamped);
        saveCurrencyData(playerName, type, clamped);
    }

    /**
     * 增加玩家指定货币余额
     */
    public long addBalance(String playerName, CurrencyType type, long amount) {
        long current = getBalance(playerName, type);
        long newBalance = amount > 0L && current > Long.MAX_VALUE - amount ? Long.MAX_VALUE
                : amount < 0L && current < Long.MIN_VALUE - amount ? Long.MIN_VALUE : current + amount;
        setBalance(playerName, type, newBalance);
        return getBalance(playerName, type);
    }

    /**
     * 减少玩家指定货币余额
     */
    public long subtractBalance(String playerName, CurrencyType type, long amount) {
        if (amount == Long.MIN_VALUE) {
            long current = getBalance(playerName, type);
            setBalance(playerName, type, current >= 0L ? Long.MAX_VALUE : current - amount);
            return getBalance(playerName, type);
        }
        return addBalance(playerName, type, -amount);
    }

    /**
     * 是否为有效货币类型
     */
    public boolean isValidCurrency(String id) {
        for (CurrencyType type : CurrencyType.values()) {
            if (type.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    // ========== 静态快捷方法（%havoc_coin% / %delta_coin%，供其他位置或模组调用）==========

    private static boolean hasPlayerData(String playerName) {
        for (CurrencyType type : CurrencyType.values()) {
            Path dir = getInstance().getCurrencyDir(type);
            if (dir != null && Files.exists(dir.resolve(playerName + ".json"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取哈夫币数量（默认当前玩家）
     */
    public static Component getHavocCoin() {
        var player = Minecraft.getInstance().player;
        if (player == null) return Component.translatable("deltaforce.economy.invalid_player");
        return Component.literal(String.valueOf(getInstance().getBalance(player.getName().getString(), CurrencyType.HAVOC_COIN)));
    }

    /**
     * 获取指定玩家哈夫币数量
     */
    public static Component getHavocCoin(String playerName) {
        if (!hasPlayerData(playerName)) return Component.translatable("deltaforce.economy.invalid_player");
        return Component.literal(String.valueOf(getInstance().getBalance(playerName, CurrencyType.HAVOC_COIN)));
    }

    /**
     * 获取三角币（三角洲）数量（默认当前玩家）
     */
    public static Component getDeltaCoin() {
        var player = Minecraft.getInstance().player;
        if (player == null) return Component.translatable("deltaforce.economy.invalid_player");
        return Component.literal(String.valueOf(getInstance().getBalance(player.getName().getString(), CurrencyType.DELTA_COIN)));
    }

    /**
     * 获取指定玩家三角币（三角洲）数量
     */
    public static Component getDeltaCoin(String playerName) {
        if (!hasPlayerData(playerName)) return Component.translatable("deltaforce.economy.invalid_player");
        return Component.literal(String.valueOf(getInstance().getBalance(playerName, CurrencyType.DELTA_COIN)));
    }
}
