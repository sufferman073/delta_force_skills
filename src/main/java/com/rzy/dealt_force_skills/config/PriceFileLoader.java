package com.rzy.dealt_force_skills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
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
 * Loads the highest-priority item values supplied by mod resources.
 * Files live at assets/<namespace>/price/<item_name>.txt.
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
            if (VANILLA_NAMESPACES.contains(modId)) {
                continue;
            }

            IModFile modFile = modInfo.getOwningFile().getFile();
            Path priceDir = modFile.findResource("assets", modId, "price");
            if (priceDir == null || !Files.isDirectory(priceDir)) {
                continue;
            }

            loaded += scanPriceDir(modId, priceDir);
        }

        LOGGER.info("PriceFileLoader: loaded Havoc prices for {} items", loaded);
    }

    private static int scanPriceDir(String namespace, Path priceDir) {
        int loaded = 0;
        try (Stream<Path> files = Files.list(priceDir)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".txt")) {
                    continue;
                }

                String itemName = fileName.substring(0, fileName.length() - 4);
                ResourceLocation itemId = new ResourceLocation(namespace, itemName);

                String content = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (content.isEmpty()) {
                    continue;
                }

                PriceData priceData = parsePriceFile(content);
                if (priceData != null) {
                    if (priceData.havocPrice > 0 || priceData.havocPrice == -1) HAVOC_PRICE_CACHE.put(itemId, priceData.havocPrice);
                    if (priceData.deltaPrice > 0) {
                        DELTA_PRICE_CACHE.put(itemId, priceData.deltaPrice);
                    }
                    loaded++;
                }
            }
        } catch (IOException error) {
            LOGGER.warn("PriceFileLoader: unable to scan directory {}: {}", priceDir, error.getMessage());
        }
        return loaded;
    }

    public static synchronized Long getHavocPrice(ResourceLocation itemId) {
        return itemId == null ? null : HAVOC_PRICE_CACHE.get(itemId);
    }

    public static synchronized Long getHavocPrice(Item item) {
        if (item == null) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? null : HAVOC_PRICE_CACHE.get(id);
    }

    public static synchronized Long getHavocPrice(ItemStack stack) {
        return stack.isEmpty() ? null : getHavocPrice(stack.getItem());
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
        if (content == null || content.isEmpty()) {
            return null;
        }
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
                JsonObject object = element.getAsJsonObject();
                String coin = object.get("coin").getAsString();
                long price = Long.parseLong(object.get("price").getAsString());

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
        } catch (Exception error) {
            LOGGER.debug("PriceFileLoader: unable to parse {}", content, error);
        }
        return null;
    }

    public record PriceData(long havocPrice, long deltaPrice) {
    }
}
