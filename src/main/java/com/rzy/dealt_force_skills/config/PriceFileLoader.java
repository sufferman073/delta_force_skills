package com.rzy.dealt_force_skills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.language.IModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 价格文件加载器
 * 从所有非原版模组命名空间的 /price/ 目录读取物品价值，优先级最高
 * 文件: assets/<namespace>/price/<item_name>.txt
 * 格式: .price=[{coin:"havoc",price:"价值"},{coin:"delta",price:"0"}]
 */
public final class PriceFileLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("dealt_force_skills/price_loader");

    private static final Map<ResourceLocation, Long> HAVOC_PRICE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Long> DELTA_PRICE_CACHE = new HashMap<>();

    private static final Set<String> VANILLA_NAMESPACES = Set.of(
            "minecraft", "neoforge", "forge", "c", "fabric"
    );

    private PriceFileLoader() {
    }

    public static synchronized void loadAll() {
        HAVOC_PRICE_CACHE.clear();
        DELTA_PRICE_CACHE.clear();

        int loaded = 0;

        for (IModInfo modInfo : ModList.get().getMods()) {
            String modId = modInfo.getModId();
            if (VANILLA_NAMESPACES.contains(modId)) continue;

            IModFile modFile = modInfo.getOwningFile().getFile();
            Path priceDir = modFile.findResource("assets", modId, "price");
            if (priceDir == null || !Files.isDirectory(priceDir)) continue;

            loaded += scanPriceDir(modId, priceDir);
        }

        LOGGER.info("PriceFileLoader: 加载了 {} 个物品的哈夫币价格", loaded);
    }

    private static int scanPriceDir(String namespace, Path priceDir) {
        int loaded = 0;
        try (Stream<Path> files = Files.list(priceDir)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".txt")) continue;

                String itemName = fileName.substring(0, fileName.length() - 4);
                ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(namespace, itemName);

                String content = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (content.isEmpty()) continue;

                PriceData priceData = parsePriceFile(content);
                if (priceData != null) {
                    HAVOC_PRICE_CACHE.put(itemId, priceData.havocPrice);
                    if (priceData.deltaPrice > 0) {
                        DELTA_PRICE_CACHE.put(itemId, priceData.deltaPrice);
                    }
                    loaded++;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("PriceFileLoader: 无法扫描目录 {}: {}", priceDir, e.getMessage());
        }
        return loaded;
    }

    public static synchronized Long getHavocPrice(ResourceLocation itemId) {
        if (itemId == null) return null;
        return HAVOC_PRICE_CACHE.get(itemId);
    }

    public static synchronized Long getHavocPrice(Item item) {
        if (item == null) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? null : HAVOC_PRICE_CACHE.get(id);
    }

    public static synchronized Long getHavocPrice(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return getHavocPrice(stack.getItem());
    }

    public static synchronized Map<ResourceLocation, Long> getHavocPriceMap() {
        return new HashMap<>(HAVOC_PRICE_CACHE);
    }

    public static synchronized void clearCache() {
        HAVOC_PRICE_CACHE.clear();
        DELTA_PRICE_CACHE.clear();
    }

    @SuppressWarnings("deprecation")
    public static PriceData parsePriceFile(String content) {
        if (content == null || content.isEmpty()) return null;
        try {
            content = content.trim();
            if (content.startsWith(".price=")) {
                content = content.substring(7);
            }

            JsonArray array = new JsonParser().parse(content).getAsJsonArray();
            long havocPrice = 0;
            long deltaPrice = 0;
            boolean foundHavoc = false;
            boolean foundDelta = false;

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String coin = obj.get("coin").getAsString();
                long price = Long.parseLong(obj.get("price").getAsString());

                if ("havoc".equals(coin)) {
                    havocPrice = price;
                    foundHavoc = true;
                } else if ("delta".equals(coin)) {
                    deltaPrice = price;
                    foundDelta = true;
                }
            }

            if (foundHavoc || foundDelta) {
                return new PriceData(havocPrice, deltaPrice);
            }
        } catch (Exception e) {
            LOGGER.debug("PriceFileLoader: 解析失败: {}", content, e);
        }
        return null;
    }

    public record PriceData(long havocPrice, long deltaPrice) {
    }
}
