package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.entity.GrappleHookEntity;
import com.rzy.dealt_force_skills.entity.TempestRecallAnchorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TempestRecallVisuals {
    private static final ResourceLocation DEVICE =
            new ResourceLocation(DealtForceSkillsMod.MODID, "tempest_emergency_recall_device");

    private TempestRecallVisuals() {
    }

    private static final Map<Integer, Integer> ANCHORS = new HashMap<>();
    private static final Set<Integer> GRAPPLES = new HashSet<>();
    private static java.lang.ref.WeakReference<ClientLevel> lastLevel = new java.lang.ref.WeakReference<>(null);
    private static long lastTick = Long.MIN_VALUE;

    private static void refresh() {
        ClientLevel level = Minecraft.getInstance().level;
        long tick = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (level == lastLevel.get() && tick == lastTick) return;
        lastLevel = new java.lang.ref.WeakReference<>(level);
        lastTick = tick;
        ANCHORS.clear();
        GRAPPLES.clear();
        if (level == null) return;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof TempestRecallAnchorEntity anchor && !anchor.isRemoved())
                ANCHORS.put(anchor.ownerEntityId(), anchor.getId());
            else if (entity instanceof GrappleHookEntity hook && hook.getRopeOwner() instanceof Player owner)
                GRAPPLES.add(owner.getId());
        }
    }

    static boolean hasGrapple(Player player) {
        refresh();
        return GRAPPLES.contains(player.getId());
    }

    static void render(PlayerModel<?> model, Player player, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        refresh();
        Integer anchorId = ANCHORS.get(player.getId());
        if (anchorId == null || !(player.level().getEntity(anchorId) instanceof TempestRecallAnchorEntity anchor) || anchor.isRemoved()) return;
        for (int side = -1; side <= 1; side += 2) {
            poses.pushPose();
            try {
                attach(model, side, poses);
                BlockbenchAnimatedModelRenderer.render(DEVICE, anchor.isRecalling() ? "recall_winch" : "idle",
                        (anchor.tickCount + partial) / 20F, poses, buffers, light);
            } finally {
                poses.popPose();
            }
        }
    }
    static void attach(PlayerModel<?> model, int side, PoseStack poses) {
        model.body.translateAndRotate(poses);
        poses.translate(side * 4.3 / 16, 10.5 / 16, 3.8 / 16);
        poses.mulPose(Axis.ZP.rotationDegrees(180));
        poses.mulPose(Axis.YP.rotationDegrees(180 - side * 24));
        poses.scale(.48F, .48F, .48F);
    }
}
