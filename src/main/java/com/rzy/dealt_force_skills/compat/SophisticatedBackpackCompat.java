package com.rzy.dealt_force_skills.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/** Optional API boundary. Unsupported installations keep the registry item but never link the upgrade. */
public final class SophisticatedBackpackCompat {
    private static final String CORE = "net.p3pp3rf1y.sophisticatedcore.";
    private static volatile boolean disabled;
    private SophisticatedBackpackCompat() {}

    private static final class Availability {
        private static final boolean SUPPORTED = detect();
    }

    public static boolean isLoaded() {
        return !disabled && Availability.SUPPORTED;
    }

    private static boolean detect() {
        if (!ModList.get().isLoaded("sophisticatedbackpacks") || !ModList.get().isLoaded("sophisticatedcore")) return false;
        if (!atLeast("sophisticatedcore", "1.0.2") || !atLeast("sophisticatedbackpacks", "3.20.27")) {
            disable("versions precede the supported upgrade API", null);
            return false;
        }
        try {
            Class<?> storage = api("api.IStorageWrapper");
            Class<?> wrapper = api("upgrades.IUpgradeWrapper");
            Class<?> limit = api("upgrades.IUpgradeCountLimitConfig");
            api("upgrades.UpgradeItemBase").getDeclaredConstructor(limit);
            limit.getMethod("getMaxUpgradesPerStorage", String.class, ResourceLocation.class);
            limit.getMethod("getMaxUpgradesInGroupPerStorage", String.class, api("upgrades.UpgradeGroup"));
            api("upgrades.UpgradeWrapperBase").getDeclaredConstructor(storage, ItemStack.class, java.util.function.Consumer.class);
            api("upgrades.UpgradeType").getConstructor(api("upgrades.UpgradeType$IFactory"));
            storage.getMethod("getStorageType");
            if (storage.getMethod("getInventoryForUpgradeProcessing").getReturnType() != api("inventory.ITrackedContentsItemHandler")) {
                throw new NoSuchMethodException("upgrade-processing inventory return type");
            }
            api("upgrades.UpgradeHandler").getMethod("refreshWrappersThatImplementAndTypeWrappers");
            Class<?> ticker = api("upgrades.ITickableUpgrade");
            try { ticker.getMethod("tick", Entity.class, Level.class, BlockPos.class); }
            catch (NoSuchMethodException oldApi) { ticker.getMethod("tick", LivingEntity.class, Level.class, BlockPos.class); }
            Class<?> filter = api("upgrades.FilterLogic");
            filter.getConstructor(ItemStack.class, java.util.function.Consumer.class, int.class,
                    net.neoforged.neoforge.registries.DeferredHolder.class);
            if (api("init.ModCoreDataComponents").getField("FILTER_ATTRIBUTES").getType()
                    != net.neoforged.neoforge.registries.DeferredHolder.class) {
                throw new NoSuchFieldException("FILTER_ATTRIBUTES type");
            }
            filter.getMethod("setAllowByDefault", boolean.class);
            filter.getMethod("matchesFilter", ItemStack.class);
            filter.getMethod("isAllowList");
            filter.getMethod("setAllowList", boolean.class);
            api("upgrades.FilterLogicContainer").getConstructor(java.util.function.Supplier.class,
                    api("common.gui.IServerUpdater"), java.util.function.Consumer.class);
            Class<?> type = api("common.gui.UpgradeContainerType");
            type.getConstructor(api("common.gui.UpgradeContainerType$IFactory"));
            api("common.gui.UpgradeContainerBase").getDeclaredConstructor(Player.class, int.class, wrapper, type);
            api("common.gui.UpgradeContainerRegistry").getMethod("register", ResourceLocation.class, type);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            disable("required upgrade API is unavailable", failure);
            return false;
        }
    }

    private static boolean atLeast(String modId, String minimum) {
        return ModList.get().getModContainerById(modId).map(container ->
                container.getModInfo().getVersion().compareTo(new DefaultArtifactVersion(minimum)) >= 0).orElse(false);
    }

    private static Class<?> api(String name) throws ClassNotFoundException {
        return Class.forName(CORE + name, false, SophisticatedBackpackCompat.class.getClassLoader());
    }

    private static void disable(String reason, Throwable failure) {
        if (!disabled) LogUtils.getLogger().warn("DFS backpack auto-sell disabled: {}. Both mods may still load.", reason, failure);
        disabled = true;
    }

    public static Item createUpgrade() {
        if (isLoaded()) {
            try { return Loaded.create(); }
            catch (LinkageError | RuntimeException failure) { disable("upgrade initialization failed", failure); }
        }
        return new Item(new Item.Properties().stacksTo(1));
    }

    public static void register() {
        if (isLoaded()) {
            try { Loaded.register(); }
            catch (LinkageError | RuntimeException failure) { disable("container registration failed", failure); }
        }
    }

    /** Called only by client setup; no client/API-linked types in this boundary's signatures. */
    public static void registerClient() {
        if (!isLoaded()) return;
        try {
            Class<?> screen = api("client.gui.StorageScreenBase");
            Class<?> position = api("client.gui.utils.Position");
            Class<?> buttons = java.lang.reflect.Array.newInstance(api("upgrades.FilterLogicControlBase$MatchButton"), 0).getClass();
            api("upgrades.FilterLogicControl").getConstructor(screen, position,
                    api("upgrades.FilterLogicContainer"), int.class, buttons);
            Class.forName("com.rzy.dealt_force_skills.compat.sophisticated.AutoSellUpgradeTab")
                    .getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            disable("client settings API is unavailable", failure);
        }
    }

    private static final class Loaded {
        private static Item create() { return new com.rzy.dealt_force_skills.compat.sophisticated.AutoSellUpgradeItem(); }
        private static void register() { com.rzy.dealt_force_skills.compat.sophisticated.AutoSellUpgradeContainer.register(); }
    }
}
