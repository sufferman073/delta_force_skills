package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class HelmetVisionClient {
    private static final RenderLevelStageEvent.Stage HELMET_VISION_RENDER_STAGE = RenderLevelStageEvent.Stage.AFTER_LEVEL;
    private static final int HEARING_REVEAL_COLOR = 0xFF7EE8FF;
    private static final double MAX_HEARING_REVEAL_RANGE = Math.max(0.0D,
            DealtForceConfig.doubleValue("client.helmet_vision_client.hearing_reveal_max_range", 64.0D));
    private static final int MAX_HEARING_REVEAL_TARGETS = Math.max(0,
            DealtForceConfig.intValue("client.helmet_vision_client.hearing_reveal_max_targets", 64));
    private static final int HEARING_REVEAL_TTL_TICKS = Math.max(1,
            DealtForceConfig.intValue("client.helmet_vision_client.hearing_reveal_ttl_ticks", 20));
    private static final double MAX_HEARING_BOX_EDGE = Math.max(0.0D,
            DealtForceConfig.doubleValue("client.helmet_vision_client.hearing_reveal_max_box_edge", 16.0D));
    private static final double HEARING_REVEAL_RANGE_PER_BOOST = Math.max(0.0D,
            DealtForceConfig.doubleValue("client.helmet_vision_client.hearing_reveal_range_per_boost", 100.0D));
    private static final Map<Integer, HearingRevealState> HEARING_REVEALS = new HashMap<>();
    private static ClientLevel hearingLevel;

    private HelmetVisionClient() {
    }

    public static boolean isThermalVisionActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && DfsEquipmentItem.activeVisionMode(minecraft.player) == DfsEquipmentItem.VISION_THERMAL;
    }

    public static void syncLocalHelmetMode(int mode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ItemStack head = minecraft.player.getItemBySlot(EquipmentSlot.HEAD);
        if (DfsEquipmentItem.profile(head) != null) {
            DfsEquipmentItem.setVisionMode(head, mode);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (hearingLevel != minecraft.level) {
            clearHearingRevealCache();
            hearingLevel = minecraft.level;
        }
        tickHearingReveals(minecraft);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HelmetVisionRenderer.close();
        clearHearingRevealCache();
        hearingLevel = null;
    }

    @SubscribeEvent
    public static void onClientLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel) {
            HelmetVisionRenderer.close();
            clearHearingRevealCache();
            hearingLevel = null;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != HELMET_VISION_RENDER_STAGE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        DfsEquipmentItem.Profile profile = DfsEquipmentItem.profile(head);
        double hearingRange = hearingRevealRange(player, profile);
        boolean hearing = hearingRange > 0.0D && !HEARING_REVEALS.isEmpty();
        if (!hearing) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;
        RenderSystem.disableDepthTest();
        try {
            var lineBuffer = buffers.getBuffer(DfsRenderTypes.xrayLines());
            if (hearing) {
                renderHearingRevealBoxes(minecraft, player, poseStack, lineBuffer, cameraPos);
            }
            buffers.endBatch(DfsRenderTypes.xrayLines());

            if (hearing) {
                renderHearingRevealLabels(minecraft, player, poseStack, buffers, font, camera, cameraPos);
                buffers.endBatch();
            }
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderHearingRevealBoxes(Minecraft minecraft, LocalPlayer player,
                                                 PoseStack poseStack, VertexConsumer lineBuffer, Vec3 cameraPos) {
        int rendered = 0;
        for (Integer entityId : HEARING_REVEALS.keySet()) {
            if (rendered >= MAX_HEARING_REVEAL_TARGETS) {
                break;
            }
            LivingEntity entity = hearingRevealEntity(minecraft, player, entityId);
            if (entity == null) {
                continue;
            }
            AABB box = safeHearingRenderBox(entity.getBoundingBox(), cameraPos, 0.04D);
            if (box == null) {
                continue;
            }
            LevelRenderer.renderLineBox(poseStack, lineBuffer, box, 0.49F, 0.91F, 1.0F, 0.82F);
            rendered++;
        }
    }

    private static void renderHearingRevealLabels(Minecraft minecraft, LocalPlayer player,
                                                  PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                                  Font font, Camera camera, Vec3 cameraPos) {
        String labelPrefix = Component.translatable("hud.dealt_force_skills.equipment.hearing_revealed").getString();
        int rendered = 0;
        for (Integer entityId : HEARING_REVEALS.keySet()) {
            if (rendered >= MAX_HEARING_REVEAL_TARGETS) {
                break;
            }
            LivingEntity entity = hearingRevealEntity(minecraft, player, entityId);
            if (entity == null || !isFinite(entity.getBbHeight()) || entity.getBbHeight() > MAX_HEARING_BOX_EDGE) {
                continue;
            }
            Vec3 labelPos = entity.position().add(0.0D, entity.getBbHeight() + 0.35D, 0.0D);
            if (!isFinite(labelPos)) {
                continue;
            }
            double distance = labelPos.distanceTo(player.getEyePosition());
            if (!isFinite(distance)) {
                continue;
            }
            String text = labelPrefix + " " + Math.round(distance) + "m";
            renderWorldLabel(poseStack, buffers, font, camera, cameraPos, labelPos, text, HEARING_REVEAL_COLOR);
            rendered++;
        }
    }

    private static boolean isHearingRevealTarget(LocalPlayer player, LivingEntity entity) {
        return entity.isAlive()
                && entity != player
                && !entity.isSpectator()
                && !isSameTeamPlayer(player, entity)
                && !(entity instanceof Player);
    }

    private static boolean isSameTeamPlayer(LocalPlayer player, LivingEntity entity) {
        if (!(entity instanceof Player other) || player == null) {
            return false;
        }
        if (other == player) {
            return true;
        }
        return player.isAlliedTo(other) || other.isAlliedTo(player);
    }

    private static double hearingRevealRange(LocalPlayer player, DfsEquipmentItem.Profile profile) {
        var amplification = player.getEffect(ModEffects.HEARING_AMPLIFICATION);
        int level = amplification == null ? 0 : amplification.getAmplifier() + 1;
        double baseRange = profile == null || profile.hearingBoost() <= 0.0D ? 0.0D
                : Math.min(MAX_HEARING_REVEAL_RANGE, profile.hearingBoost() * HEARING_REVEAL_RANGE_PER_BOOST);
        // The effect grants its own reveal radius, including without a hearing helmet.
        return baseRange * (1.0D + 0.1D * level) + 10.0D * level;
    }

    private static void tickHearingReveals(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clearHearingRevealCache();
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        DfsEquipmentItem.Profile profile = DfsEquipmentItem.profile(head);
        double hearingRange = hearingRevealRange(player, profile);
        if (hearingRange <= 0.0D) {
            clearHearingRevealCache();
            return;
        }

        int now = player.tickCount;
        double rangeSqr = hearingRange * hearingRange;
        pruneHearingReveals(minecraft, player, rangeSqr, now);

        for (Entity rawEntity : minecraft.level.entitiesForRendering()) {
            if (!(rawEntity instanceof LivingEntity entity)) {
                continue;
            }
            if (!isHearingRevealCandidate(player, entity, rangeSqr)) {
                continue;
            }
            HearingRevealState state = HEARING_REVEALS.get(entity.getId());
            if (state == null) {
                if (HEARING_REVEALS.size() >= MAX_HEARING_REVEAL_TARGETS) {
                    continue;
                }
                state = new HearingRevealState(now);
                HEARING_REVEALS.put(entity.getId(), state);
            } else {
                state.lastSeenTick = now;
            }
        }
    }

    private static void pruneHearingReveals(Minecraft minecraft, LocalPlayer player, double rangeSqr, int now) {
        Iterator<Map.Entry<Integer, HearingRevealState>> iterator = HEARING_REVEALS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, HearingRevealState> entry = iterator.next();
            if (now - entry.getValue().lastSeenTick > HEARING_REVEAL_TTL_TICKS) {
                iterator.remove();
                continue;
            }
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)
                    || !isHearingRevealCandidate(player, living, rangeSqr)) {
                iterator.remove();
            }
        }
    }

    private static LivingEntity hearingRevealEntity(Minecraft minecraft, LocalPlayer player, int entityId) {
        if (minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        double range = hearingRevealRange(player, DfsEquipmentItem.profile(player.getItemBySlot(EquipmentSlot.HEAD)));
        double rangeSqr = range * range;
        return isHearingRevealCandidate(player, living, rangeSqr) ? living : null;
    }

    private static boolean isHearingRevealCandidate(LocalPlayer player, LivingEntity entity, double rangeSqr) {
        return isHearingRevealTarget(player, entity)
                && isWithinHearingRenderRange(player, entity, rangeSqr)
                && isReasonableBox(entity.getBoundingBox(), MAX_HEARING_BOX_EDGE);
    }

    private static boolean isWithinHearingRenderRange(LocalPlayer player, Entity entity, double rangeSqr) {
        if (!hasFinitePosition(player) || !hasFinitePosition(entity)) {
            return false;
        }
        double distanceSqr = entity.distanceToSqr(player);
        return isFinite(distanceSqr)
                && distanceSqr <= rangeSqr
                && rangeSqr > 0.0D;
    }

    private static AABB safeHearingRenderBox(AABB box, Vec3 cameraPos, double inflate) {
        return safeRenderBox(box, cameraPos, inflate, MAX_HEARING_BOX_EDGE);
    }

    private static AABB safeRenderBox(AABB box, Vec3 cameraPos, double inflate, double maxEdge) {
        if (!isReasonableBox(box, maxEdge) || !isFinite(cameraPos)) {
            return null;
        }
        AABB renderBox = box.inflate(inflate).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        return isReasonableBox(renderBox, maxEdge + inflate * 2.0D) ? renderBox : null;
    }

    private static boolean isReasonableBox(AABB box, double maxEdge) {
        if (box == null
                || !isFinite(box.minX) || !isFinite(box.minY) || !isFinite(box.minZ)
                || !isFinite(box.maxX) || !isFinite(box.maxY) || !isFinite(box.maxZ)) {
            return false;
        }
        double xSize = box.maxX - box.minX;
        double ySize = box.maxY - box.minY;
        double zSize = box.maxZ - box.minZ;
        return isFinite(xSize) && isFinite(ySize) && isFinite(zSize)
                && xSize >= 0.0D && ySize >= 0.0D && zSize >= 0.0D
                && xSize <= maxEdge && ySize <= maxEdge && zSize <= maxEdge;
    }

    private static boolean hasFinitePosition(Entity entity) {
        return entity != null
                && isFinite(entity.getX())
                && isFinite(entity.getY())
                && isFinite(entity.getZ());
    }

    private static boolean isFinite(Vec3 vec) {
        return vec != null && isFinite(vec.x) && isFinite(vec.y) && isFinite(vec.z);
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    private static void renderWorldLabel(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                         Font font, Camera camera, Vec3 cameraPos, Vec3 worldPos,
                                         String text, int color) {
        poseStack.pushPose();
        poseStack.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH,
                0x66000000, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void clearHearingRevealCache() {
        HEARING_REVEALS.clear();
    }

    private static final class HearingRevealState {
        private int lastSeenTick;

        private HearingRevealState(int lastSeenTick) {
            this.lastSeenTick = lastSeenTick;
        }
    }
}
