package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.manba.ManbaOpportunityMarker;
import com.rzy.dealt_force_skills.character.manba.ManbaOpportunityMarkerType;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.manba.ManbaTalent;
import com.rzy.dealt_force_skills.client.character.ClientManbaHudState;
import com.rzy.dealt_force_skills.client.character.ClientUndeadHudState;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ManbaOpportunityWindowRenderer {
    private static final double UNDEAD_EXPLORER_RANGE = ManbaStateManager.OPPORTUNITY_WINDOW_RANGE * 2.0D;
    private static final int PLAYER_COLOR = 0xFF6CE8FF;
    private static final int UNOPENED_LOOT_COLOR = 0xFFFF5AA8;
    private static final int OPENED_CONTAINER_COLOR = 0xFFFFD45A;

    private ManbaOpportunityWindowRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        boolean manbaOpportunity = ClientManbaHudState.hasTalent(ManbaTalent.OPPORTUNITY_WINDOW);
        boolean undeadExplorerOpportunity = ClientUndeadHudState.shouldRender()
                && ClientUndeadHudState.explorerSpaceTicks() > 0
                && ClientUndeadHudState.explorerCanSeeEntities();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || (!manbaOpportunity && !undeadExplorerOpportunity)
                || ClientManbaHudState.opportunityMarkers().isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewerPos = minecraft.player.position();
        double range = undeadExplorerOpportunity
                ? UNDEAD_EXPLORER_RANGE
                : ManbaStateManager.OPPORTUNITY_WINDOW_RANGE;
        double rangeSqr = range * range;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Font font = minecraft.font;

        var markers = ClientManbaHudState.opportunityMarkers();
        RenderSystem.disableDepthTest();
        try {
            var lineBuffer = buffers.getBuffer(DfsRenderTypes.xrayLines());
            for (ManbaOpportunityMarker marker : markers) {
                double distanceSqr = marker.position().distanceToSqr(viewerPos);
                if (distanceSqr > rangeSqr) {
                    continue;
                }
                AABB box = markerBox(marker).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                LevelRenderer.renderLineBox(poseStack, lineBuffer, box,
                        red(marker.type()), green(marker.type()), blue(marker.type()), 0.95F);
            }
            buffers.endBatch(DfsRenderTypes.xrayLines());

            for (ManbaOpportunityMarker marker : markers) {
                double distanceSqr = marker.position().distanceToSqr(viewerPos);
                if (distanceSqr <= rangeSqr) {
                    renderLabel(poseStack, buffers, font, camera, cameraPos,
                            labelPosition(marker),
                            markerText(marker, Math.round(Math.sqrt(distanceSqr))),
                            labelColor(marker.type()));
                }
            }
            buffers.endBatch();
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    private static AABB markerBox(ManbaOpportunityMarker marker) {
        Vec3 position = marker.position();
        if (marker.type() == ManbaOpportunityMarkerType.PLAYER) {
            return new AABB(position.x - 0.35D, position.y, position.z - 0.35D,
                    position.x + 0.35D, position.y + 1.9D, position.z + 0.35D);
        }
        return new AABB(position.x - 0.5D, position.y - 0.5D, position.z - 0.5D,
                position.x + 0.5D, position.y + 0.5D, position.z + 0.5D).inflate(0.035D);
    }

    private static Vec3 labelPosition(ManbaOpportunityMarker marker) {
        return marker.type() == ManbaOpportunityMarkerType.PLAYER
                ? marker.position().add(0.0D, 2.2D, 0.0D)
                : marker.position().add(0.0D, 0.75D, 0.0D);
    }

    private static String markerText(ManbaOpportunityMarker marker, long distance) {
        if (marker.type() == ManbaOpportunityMarkerType.PLAYER) {
            String name = marker.label().isBlank()
                    ? Component.translatable("hud.dealt_force_skills.manba.opportunity_player").getString()
                    : marker.label();
            return name + " " + distance + "m";
        }
        return Component.translatable(translationKey(marker.type())).getString() + " " + distance + "m";
    }

    private static String translationKey(ManbaOpportunityMarkerType type) {
        return switch (type) {
            case PLAYER -> "hud.dealt_force_skills.manba.opportunity_player";
            case UNOPENED_LOOT -> "hud.dealt_force_skills.manba.opportunity_unopened_loot";
            case OPENED_OR_NORMAL -> "hud.dealt_force_skills.manba.opportunity_container";
        };
    }

    private static int labelColor(ManbaOpportunityMarkerType type) {
        return switch (type) {
            case PLAYER -> PLAYER_COLOR;
            case UNOPENED_LOOT -> UNOPENED_LOOT_COLOR;
            case OPENED_OR_NORMAL -> OPENED_CONTAINER_COLOR;
        };
    }

    private static float red(ManbaOpportunityMarkerType type) {
        return switch (type) {
            case PLAYER -> 0.42F;
            case UNOPENED_LOOT -> 1.0F;
            case OPENED_OR_NORMAL -> 1.0F;
        };
    }

    private static float green(ManbaOpportunityMarkerType type) {
        return switch (type) {
            case PLAYER -> 0.91F;
            case UNOPENED_LOOT -> 0.35F;
            case OPENED_OR_NORMAL -> 0.83F;
        };
    }

    private static float blue(ManbaOpportunityMarkerType type) {
        return switch (type) {
            case PLAYER -> 1.0F;
            case UNOPENED_LOOT -> 0.66F;
            case OPENED_OR_NORMAL -> 0.35F;
        };
    }

    private static void renderLabel(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
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
}
