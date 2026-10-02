package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class NTwoFrozenVisuals {
    private static final float MIN_SHELL_WIDTH = 1.02F;
    private static final float SHELL_PADDING = 0.28F;
    private static final float SHELL_OVERLAP = 0.14F;
    private static final Map<Integer, Long> SYNCED_FROZEN_UNTIL = new HashMap<>();

    private NTwoFrozenVisuals() {
    }

    public static void syncFrozenVisual(int entityId, int remainingTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (remainingTicks <= 0 || minecraft.level == null) {
            SYNCED_FROZEN_UNTIL.remove(entityId);
            return;
        }
        SYNCED_FROZEN_UNTIL.put(entityId, minecraft.level.getGameTime() + remainingTicks);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity.isInvisible() || !shouldRenderFrozen(entity)) {
            return;
        }
        float width = Math.max(0.42F, entity.getBbWidth());
        float height = Math.max(0.8F, entity.getBbHeight());
        float shellWidth = Math.max(MIN_SHELL_WIDTH, width + SHELL_PADDING);
        float segmentHeight = Math.max(0.52F, (height + SHELL_OVERLAP) * 0.5F);
        renderIceShell(event, segmentHeight * 0.5F - SHELL_OVERLAP * 0.25F,
                shellWidth, segmentHeight, shellWidth);
        renderIceShell(event, height - segmentHeight * 0.5F + SHELL_OVERLAP * 0.25F,
                shellWidth, segmentHeight, shellWidth);
    }

    private static void renderIceShell(RenderLivingEvent.Post<?, ?> event, float y,
                                       float xScale, float yScale, float zScale) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.scale(xScale, yScale, zScale);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.ICE.defaultBlockState(),
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static boolean shouldRenderFrozen(LivingEntity entity) {
        if (entity.hasEffect(ModEffects.N_TWO_FROZEN)) {
            return true;
        }
        Long syncedUntil = SYNCED_FROZEN_UNTIL.get(entity.getId());
        if (syncedUntil == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            SYNCED_FROZEN_UNTIL.clear();
            return false;
        }
        if (minecraft.level.getGameTime() >= syncedUntil) {
            SYNCED_FROZEN_UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }
}
