package mccfk.DeltaForce.java.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rzy.dealt_force_skills.item.DfsItemQuality;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.client.overlay.GroundItemOverlay;
import mccfk.DeltaForce.java.client.overlay.ItemDescriptionLoader;
import mccfk.DeltaForce.java.client.renderer.GroundItemRenderer;
import mccfk.DeltaForce.java.client.renderer.MarkerRenderer;
import mccfk.DeltaForce.java.entity.DeltaForceEntities;
import mccfk.DeltaForce.java.compat.SubLevelProjectionHelper;
import mccfk.DeltaForce.java.network.C2S_CreateMarker;
import mccfk.DeltaForce.java.network.Marker;
import mccfk.DeltaForce.java.network.MarkerType;
import mccfk.DeltaForce.java.network.MarkerManager;
import mccfk.DeltaForce.java.network.ClientMarkerStorage;
import mccfk.DeltaForce.java.client.screen.WarehousePanel;
import mccfk.DeltaForce.java.client.screen.Operator;
import com.rzy.dealt_force_skills.client.screen.GtiTradingHallScreen;
import com.rzy.dealt_force_skills.client.screen.SpecialOperationsBaseScreen;

/**
 * DeltaForce 客户端入口
 */
@Mod(value = DeltaForce.MODID, dist = Dist.CLIENT)
public class DeltaForceClient {

    // 鼠标中键标点系统
    private static final KeyMapping MARKER_KEY = new KeyMapping(
            "key.deltaforce.marker",
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, // 默认鼠标中键
            "key.categories.deltaforce"
    );

    public DeltaForceClient(IEventBus modEventBus) {
        // 注册实体渲染器
        modEventBus.addListener(this::registerEntityRenderers);
        // 注册GUI覆盖层
        modEventBus.addListener(this::registerGuiLayers);
        // 注册按键绑定
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::registerReloadListeners);
        // 注册客户端Tick事件
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        // 注册渲染事件
        NeoForge.EVENT_BUS.addListener(this::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册地面物品实体渲染器
        event.registerEntityRenderer(
                DeltaForceEntities.GROUND_ITEM.get(),
                GroundItemRenderer::new
        );
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        // 注册地面物品信息覆盖层
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(DeltaForce.MODID, "ground_item_overlay"),
                GroundItemOverlay.OVERLAY
        );
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MARKER_KEY);
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            ItemDescriptionLoader.clearCache();
            DfsItemQuality.clearTagQualityOverrides();
        });
    }

    private void onClientTick(ClientTickEvent.Post event) {
        // 清理客户端过期标点
        mccfk.DeltaForce.java.network.ClientMarkerStorage.cleanExpired();

        // 处理鼠标中键创建标点
        while (MARKER_KEY.consumeClick()) {
            createMarkerFromHitResult();
        }

    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientMarkerStorage.clearAll();
        WarehousePanel.resetInteractionState();
        Operator.clearPending();
        GtiTradingHallScreen.resetSessionState();
        SpecialOperationsBaseScreen.clearPendingGtiAccess();
    }

    /**
     * 根据当前视角指向创建标点
     */
    private void createMarkerFromHitResult() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double maxDistance = MarkerManager.MAX_MARKER_DISTANCE;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        // 实体射线检测
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.player,
                eyePos,
                endPos,
                mc.player.getBoundingBox().expandTowards(lookVec.scale(maxDistance)).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable(),
                maxDistance * maxDistance
        );

        // 方块射线检测
        HitResult blockHit = mc.player.pick(maxDistance, 0.0F, false);

        // 取两者中更近的结果，但优先选择掉落物（ItemEntity/GroundItemEntity）
        HitResult hitResult;
        if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double entityDist = entityHit.getLocation().distanceToSqr(eyePos);
            double blockDist = blockHit.getLocation().distanceToSqr(eyePos);

            // 如果实体是掉落物，优先选择
            var entity = entityHit.getEntity();
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity ||
                    entity instanceof mccfk.DeltaForce.java.entity.GroundItemEntity) {
                hitResult = entityHit;
            } else {
                hitResult = entityDist <= blockDist ? entityHit : blockHit;
            }
        } else if (entityHit != null) {
            hitResult = entityHit;
        } else {
            hitResult = blockHit;
        }

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return;

        UUID playerUUID = mc.player.getUUID();
        long currentTime = System.currentTimeMillis();
        Marker marker = null;

        switch (hitResult.getType()) {
            case ENTITY -> {
                EntityHitResult entityHit2 = (EntityHitResult) hitResult;
                var entity = entityHit2.getEntity();

                Vec3 precisePos = entity.position(); // 始终使用实体精确位置

                // 若实体在Sable子世界中，将坐标投影到主世界空间
                precisePos = SubLevelProjectionHelper.projectToGlobal(mc.level, precisePos);

                marker = new Marker(
                        playerUUID,
                        BlockPos.containing(precisePos),
                        MarkerType.ENTITY,
                        currentTime,
                        java.util.Optional.of(entity.getType()),
                        java.util.Optional.empty(),
                        java.util.Optional.of(precisePos)
                );
            }
            case BLOCK -> {
                BlockHitResult blockHit2 = (BlockHitResult) hitResult;
                BlockPos pos = blockHit2.getBlockPos();
                Vec3 preciseLocation = blockHit2.getLocation();

                // 先使用原始坐标检测方块实体（子世界局部坐标可正确找到子世界中的BlockEntity）
                var blockEntity = mc.level.getBlockEntity(pos);

                // 后备：若主世界查找失败，尝试通过SubLevel直接获取BlockEntity
                if (blockEntity == null) {
                    blockEntity = SubLevelProjectionHelper.getBlockEntityFromSubLevel(mc.level, pos);
                }

                // 将坐标投影到主世界全局空间（若不在子世界则为无操作）
                BlockPos worldPos = SubLevelProjectionHelper.projectToGlobal(mc.level, pos);
                Vec3 worldLocation = SubLevelProjectionHelper.projectToGlobal(mc.level, preciseLocation);

                if (blockEntity != null && isContainer(blockEntity)) {
                    // 指向容器（坐标已投影到主世界）
                    // 注意：将plotgrid原始坐标存入containerPos，精确世界命中点存入precisePosition
                    // 渲染时通过 containerPos 重建精确AABB，避免取整导致的网格偏移
                    marker = new Marker(
                            playerUUID,
                            worldPos,
                            MarkerType.CONTAINER,
                            currentTime,
                            java.util.Optional.empty(),
                            java.util.Optional.of(pos),               // plotgrid原始坐标
                            java.util.Optional.of(worldLocation)       // 精确世界命中点
                    );
                } else {
                    // 指向普通方块位置，使用已投影到主世界的精确命中点坐标
                    marker = new Marker(
                            playerUUID,
                            worldPos,
                            MarkerType.POSITION,
                            currentTime,
                            java.util.Optional.empty(),
                            java.util.Optional.empty(),
                            java.util.Optional.of(worldLocation)
                    );
                }
            }
            default -> {}
        }

        if (marker != null) {
            PacketDistributor.sendToServer(new C2S_CreateMarker(marker));
        }
    }

    /**
     * 判断BlockEntity是否为容器（支持其他模组）
     */
    private boolean isContainer(net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        // 原版容器接口
        if (blockEntity instanceof net.minecraft.world.Container ||
                blockEntity instanceof net.minecraft.world.WorldlyContainer) {
            return true;
        }

        // 检查是否有物品处理能力（兼容其他模组的容器）
        if (blockEntity instanceof net.neoforged.neoforge.items.IItemHandlerModifiable) {
            return true;
        }

        // 检查是否有InventoryCapability能力（NeoForge通用能力系统）
        var capability = blockEntity.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                blockEntity.getBlockPos(),
                null
        );
        if (capability != null) {
            return true;
        }

        return false;
    }

    /**
     * 渲染事件 - 绘制所有标点
     */
    private void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            MarkerRenderer.renderAllMarkers(
                    event.getPoseStack(),
                    event.getCamera(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false)
            );
        }
    }

    public static java.util.Optional<ViewMarker> captureMarkerFromView() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return java.util.Optional.empty();
        }
        double distance = MarkerManager.MAX_MARKER_DISTANCE;
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 look = minecraft.player.getLookAngle();
        Vec3 end = eye.add(look.scale(distance));
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(minecraft.player, eye, end,
                minecraft.player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isPickable(), distance * distance);
        HitResult blockHit = minecraft.player.pick(distance, 0.0F, false);
        HitResult hit = nearer(eye, entityHit, blockHit);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            return java.util.Optional.empty();
        }
        Marker marker;
        int entityId = -1;
        long now = System.currentTimeMillis();
        if (hit instanceof EntityHitResult entityResult) {
            Vec3 position = entityResult.getEntity().position();
            entityId = entityResult.getEntity().getId();
            marker = new Marker(minecraft.player.getUUID(), BlockPos.containing(position), MarkerType.ENTITY, now,
                    java.util.Optional.of(entityResult.getEntity().getType()), java.util.Optional.empty(),
                    java.util.Optional.of(position));
        } else if (hit instanceof BlockHitResult blockResult) {
            BlockPos position = blockResult.getBlockPos();
            var blockEntity = minecraft.level.getBlockEntity(position);
            boolean container = blockEntity instanceof net.minecraft.world.Container
                    || blockEntity != null && minecraft.level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    position, null) != null;
            marker = new Marker(minecraft.player.getUUID(), position,
                    container ? MarkerType.CONTAINER : MarkerType.POSITION,
                    now, java.util.Optional.empty(), container ? java.util.Optional.of(position) : java.util.Optional.empty(),
                    java.util.Optional.of(blockResult.getLocation()));
        } else {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ViewMarker(marker, entityId));
    }

    private static HitResult nearer(Vec3 eye, EntityHitResult entityHit, HitResult blockHit) {
        if (entityHit == null) return blockHit;
        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) return entityHit;
        return entityHit.getLocation().distanceToSqr(eye) <= blockHit.getLocation().distanceToSqr(eye)
                ? entityHit : blockHit;
    }

    public record ViewMarker(Marker marker, int entityId) {
    }
}