package mccfk.DeltaForce.java;

import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.DeltaForceNetwork;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.client.screen.IConfigProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.level.ServerPlayer;

import mccfk.DeltaForce.java.item.DeltaForceItems;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.command.DFCommand;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 主类
 * 单例模式，统一管理所有DF（三角洲行动）相关物品、实体和事件
 * 数据持久化路径：世界文件夹/.mccfk_alldata/DeltaForce/
 *
 * 公共API：
 * - DeltaForce.getInstance()         获取单例
 */
@Mod(DeltaForce.MODID)
public class DeltaForce {

    public static final String MODID = "deltaforce";

    private static DeltaForce instance;

    // 设置界面白名单：df 和 dfs 为永久白名单，其他模组在主类中标注即可自动识别
    // 标注方式：DeltaForce.registerConfigurableMod(MODID) 或 InterModComms.sendTo("deltaforce", "register_configurable", () -> MODID)
    private static final Set<String> configWhitelist = new HashSet<>(Arrays.asList(
        "deltaforce",
        "dealt_force_skills"
    ));

    // 配置提供者注册表：modId -> IConfigProvider
    private static final Map<String, IConfigProvider> configProviders = new HashMap<>();

    public DeltaForce(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;

        // 注册 IMC 侦听：接收其他模组的"注册可配置"消息
        modEventBus.addListener(this::onInterModEnqueue);

        // 注册DF物品
        DeltaForceItems.register(modEventBus);

        // 注册DF实体
        DeltaForceEntities.ENTITY_TYPES.register(modEventBus);

        // 注册服务端事件
        NeoForge.EVENT_BUS.register(this);

        // 客户端专用初始化（通过反射调用，避免服务端类加载触发 RuntimeDistCleaner）
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            try {
                Class<?> clientSetup = Class.forName("mccfk.DeltaForce.java.client.DeltaForceClientSetup");
                clientSetup.getMethod("init", IEventBus.class, ModContainer.class)
                    .invoke(null, modEventBus, modContainer);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(DeltaForce.class)
                    .error("客户端初始化失败", e);
            }
        }
    }

    /**
     * 获取单例实例
     */
    public static DeltaForce getInstance() {
        return instance;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DataInitializer.init(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    /**
     * 获取设置界面白名单（允许显示配置的模组ID集合）
     */
    public static Set<String> getConfigWhitelist() {
        return configWhitelist;
    }

    /**
     * 其他模组在主类中调用此方法注册自身，使其出现在 DF 设置界面中
     */
    public static void registerConfigurableMod(String modId) {
        configWhitelist.add(modId);
    }

    /**
     * 检查指定模组ID是否在设置界面白名单中
     */
    public static boolean isConfigWhitelisted(String modId) {
        return configWhitelist.contains(modId);
    }

    /**
     * 注册配置提供者（其他模组在主类中调用）
     * @param modId 模组ID
     * @param provider 配置提供者实例
     */
    public static void registerConfigProvider(String modId, IConfigProvider provider) {
        configProviders.put(modId, provider);
    }

    /**
     * 获取指定模组的配置提供者
     */
    public static IConfigProvider getConfigProvider(String modId) {
        return configProviders.get(modId);
    }

    /**
     * 获取所有已注册的配置提供者
     */
    public static Map<String, IConfigProvider> getConfigProviders() {
        return configProviders;
    }

    /**
     * 接收其他模组通过 IMC 发送的注册请求
     */
    private void onInterModEnqueue(InterModEnqueueEvent event) {
        InterModComms.getMessages(MODID, method -> method.equals("register_configurable"))
            .forEach(msg -> configWhitelist.add((String) msg.messageSupplier().get()));
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DFCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        var player = event.getEntity();
        String playerName = player.getGameProfile().getName();

        // 初始化玩家经济数据（文件不存在则创建默认）
        EeconomyManager.getInstance().initPlayerData(playerName);

        // 初始化玩家角色数据目录（干员定义已统一合并到 friend 模组，此处仅确保目录存在）
        PlayerCharacterStore.init();

        if (player instanceof ServerPlayer serverPlayer) {
            DeltaForceNetwork.syncMarkers(serverPlayer);
        }

        // 提示读取失败的配置文件
        var failedFiles = EeconomyManager.getInstance().getFailedFiles();
        if (!failedFiles.isEmpty()) {
            for (var f : failedFiles) {
                player.sendSystemMessage(
                    Component.literal("§c[DF] 文件 ")
                        .append(Component.literal(f.fileName).withStyle(style -> style.withUnderlined(true)))
                        .append(Component.literal(" 损坏，文件路径: "))
                        .append(Component.literal(f.filePath + ".no")
                            .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, f.filePath + ".no"))
                                .withUnderlined(true)
                            )
                        )
                );
                org.slf4j.LoggerFactory.getLogger(DeltaForce.class)
                    .warn("[DF] 文件 {} 损坏，路径: {}.no", f.fileName, f.filePath);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarkerManager.removeMarker(player.getUUID());
            DeltaForceNetwork.broadcastMarkers(player.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        MarkerManager.clearAll();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (MarkerManager.tick()) {
            var server = event.getServer();
            if (server != null) {
                DeltaForceNetwork.broadcastMarkers(server);
            }
        }
    }
}