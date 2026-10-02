package com.rzy.dealt_force_skills.client.visual;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.sineva.SinevaShieldGeometry;
import com.rzy.dealt_force_skills.client.SinevaInputHandler;
import com.rzy.dealt_force_skills.client.character.ClientSinevaRenderState;
import com.rzy.dealt_force_skills.client.renderer.BlockbenchAnimatedModelRenderer;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import com.rzy.dealt_force_skills.entity.GrappleHookEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.Set;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class SinevaPlaceholderVisuals {
    private static final ItemStack BLADE_WIRE_PLACEHOLDER = new ItemStack(Items.COBWEB);
    private static final ResourceLocation BLADE_WIRE_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_blade_wire_core");
    private static final ResourceLocation GRAPPLE_GUN_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_grapple_gun");
    private static final ResourceLocation RIOT_SHIELD_MODEL =
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "sineva_riot_shield");
    private static final String GRAPPLE_GUN_KEY = "sineva_grapple_gun";
    private static final int OUTER_R = 10;
    private static final int OUTER_G = 12;
    private static final int OUTER_B = 31;
    private static final int FACE_R = 6;
    private static final int FACE_G = 6;
    private static final int FACE_B = 12;
    private static final int TRIM_R = 8;
    private static final int TRIM_G = 34;
    private static final int TRIM_B = 86;
    private static final int GLASS_R = 132;
    private static final int GLASS_G = 178;
    private static final int GLASS_B = 190;
    // Vanilla player layers use humanoid model space, where positive Y points down.
    // The YSM fallback uses world space, where positive Y points up.
    private static final float FIRST_PERSON_VIEWPORT_Y = 0.04f;
    private static final float THIRD_PERSON_WORLD_VIEWPORT_Y = 0.14f;
    private static final float THIRD_PERSON_MODEL_VIEWPORT_Y = -0.16f;
    private static final Map<UUID, ItemStack> SAVED_MAIN_HAND = new HashMap<>();
    private static final Map<UUID, YsmSavedMainHand> YSM_WORLD_SAVED_MAIN_HAND = new HashMap<>();
    private static final Set<UUID> VANILLA_RENDERED_THIS_FRAME = new HashSet<>();
    private static final Set<UUID> VANILLA_RENDERED_LAST_FRAME = new HashSet<>();

    private SinevaPlaceholderVisuals() {
    }

    public static void resetTransientState() {
        SAVED_MAIN_HAND.clear();
        YSM_WORLD_SAVED_MAIN_HAND.clear();
        VANILLA_RENDERED_THIS_FRAME.clear();
        VANILLA_RENDERED_LAST_FRAME.clear();
    }

    private static boolean isYesSteveModelLoaded() {
        return ModList.get().getMods().stream().anyMatch(modInfo -> {
            String id = modInfo.getModId().toLowerCase(Locale.ROOT);
            return id.equals("yes_steve_model")
                    || id.equals("yesstevemodel")
                    || id.equals("ysm")
                    || id.contains("yes_steve")
                    || id.contains("yessteve");
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shieldMode = shouldHideHeldHands(minecraft.player);
        boolean bladeWireMode = SinevaInputHandler.isBladeWireHeld();
        boolean grappleMode = ClientToolModelAnimationState.isActive(GRAPPLE_GUN_KEY)
                || SinevaInputHandler.isGrappleCharging()
                || hasActiveLocalGrapple(minecraft);

        if (!shieldMode && !bladeWireMode && !grappleMode) {
            return;
        }

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();

        // Render blade wire in main hand when held (works with or without shield)
        if (bladeWireMode) {
            poseStack.pushPose();
            poseStack.translate(0.56d, -0.42d, -0.72d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-80.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-15.0f));
            poseStack.scale(0.65f, 0.65f, 0.65f);
            boolean rendered = BlockbenchAnimatedModelRenderer.render(
                    BLADE_WIRE_MODEL, null, 0.0F, poseStack,
                    event.getMultiBufferSource(), event.getPackedLight());
            if (!rendered) {
                minecraft.getItemRenderer().renderStatic(
                        minecraft.player,
                        BLADE_WIRE_PLACEHOLDER,
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        false,
                        poseStack,
                        event.getMultiBufferSource(),
                        minecraft.level,
                        event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY,
                        0
                );
            }
            poseStack.popPose();
        }

        // Render the shield first so the offhand grapple gun remains visible over its edge.
        if (shieldMode) {
            float bash = ClientSinevaVisualState.bashProgress(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
            float charge = ClientSinevaVisualState.chargeProgress(com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
            float thrust = Mth.sin(bash * Mth.PI) * 0.08f + charge * 0.05f;

            poseStack.pushPose();
            poseStack.translate(0.0d, 1.68d, -0.96d - thrust * 0.12f);
            renderSinevaShield(poseStack, event.getMultiBufferSource(), event.getPackedLight(), true,
                    ClientSinevaRenderState.isViewportBroken(minecraft.player));
            poseStack.popPose();
        }

        if (grappleMode) {
            poseStack.pushPose();
            poseStack.translate(-0.48D, -0.36D, -0.68D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-9.0F));
            poseStack.scale(0.72F, 0.72F, 0.72F);
            ClientToolModelAnimationState.AnimationFrame frame = ClientToolModelAnimationState.frame(
                    GRAPPLE_GUN_KEY, GRAPPLE_GUN_MODEL, null, 0.0F);
            boolean rendered = BlockbenchAnimatedModelRenderer.render(
                    GRAPPLE_GUN_MODEL, frame.animation(), frame.seconds(), poseStack,
                    event.getMultiBufferSource(), event.getPackedLight());
            if (!rendered) {
                minecraft.getItemRenderer().renderStatic(
                        minecraft.player, new ItemStack(Items.CROSSBOW),
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND, true, poseStack,
                        event.getMultiBufferSource(), minecraft.level, event.getPackedLight(),
                        OverlayTexture.NO_OVERLAY, 0);
            }
            poseStack.popPose();
        }
    }

    private static boolean hasActiveLocalGrapple(Minecraft minecraft) {
        return hasActiveGrapple(minecraft, minecraft.player);
    }

    private static boolean hasActiveGrapple(Minecraft minecraft, Player player) {
        if (player == null || minecraft.level == null) {
            return false;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof GrappleHookEntity hook && hook.getRopeOwner() == player) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderArm(RenderArmEvent event) {
        if (shouldHideHeldHands(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!shouldHideHeldHands(player) || SAVED_MAIN_HAND.containsKey(player.getUUID())) {
            return;
        }

        int selectedSlot = player.getInventory().selected;
        ItemStack stack = player.getInventory().items.get(selectedSlot);
        if (stack.isEmpty()) {
            return;
        }

        SAVED_MAIN_HAND.put(player.getUUID(), stack);
        player.getInventory().items.set(selectedSlot, ItemStack.EMPTY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (isYesSteveModelLoaded()) {
            VANILLA_RENDERED_THIS_FRAME.add(player.getUUID());
        }
        ItemStack saved = SAVED_MAIN_HAND.remove(player.getUUID());
        if (saved != null) {
            player.getInventory().items.set(player.getInventory().selected, saved);
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!hasActiveGrapple(minecraft, player)
                || player.isInvisible()
                || player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        event.getRenderer().getModel().leftArm.translateAndRotate(poseStack);
        poseStack.translate(0.04D, 0.43D, 0.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        BlockbenchAnimatedModelRenderer.render(
                GRAPPLE_GUN_MODEL, null, 0.0F, poseStack,
                event.getMultiBufferSource(), event.getPackedLight());
        poseStack.popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!isYesSteveModelLoaded()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            restoreYsmWorldMainHands(minecraft);
            clearVanillaRenderFrameMarkers();
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            // YSM owns its own player renderer / held-item path, so RenderPlayerEvent.Pre is
            // not always enough to hide the main-hand gun. Hide only during world entity
            // rendering and only when YSM is loaded; the vanilla/no-YSM path remains unchanged.
            hideYsmWorldMainHands(minecraft);
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        for (Player player : minecraft.level.players()) {
            if (vanillaPlayerRendererActive(player)) {
                continue;
            }
            boolean renderShield = shouldRenderFor(player);
            boolean renderGrapple = hasActiveGrapple(minecraft, player);
            if (!renderShield && !renderGrapple) {
                continue;
            }
            if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                continue;
            }
            if (renderShield) {
                renderYsmWorldFallbackShield(event, player, buffer);
            }
            if (renderGrapple) {
                renderYsmWorldFallbackGrapple(event, player, buffer);
            }
        }
        buffer.endBatch(DfsRenderTypes.untexturedQuads());
        restoreYsmWorldMainHands(minecraft);
        finishVanillaRenderFrame();
    }

    private static void hideYsmWorldMainHands(Minecraft minecraft) {
        if (minecraft.level == null) {
            return;
        }

        for (Player player : minecraft.level.players()) {
            if (!shouldHideHeldHands(player) || vanillaPlayerRendererActive(player)
                    || YSM_WORLD_SAVED_MAIN_HAND.containsKey(player.getUUID())) {
                continue;
            }

            int selectedSlot = player.getInventory().selected;
            ItemStack stack = player.getInventory().items.get(selectedSlot);
            if (stack.isEmpty()) {
                continue;
            }

            YSM_WORLD_SAVED_MAIN_HAND.put(player.getUUID(), new YsmSavedMainHand(selectedSlot, stack));
            player.getInventory().items.set(selectedSlot, ItemStack.EMPTY);
        }
    }

    private static void restoreYsmWorldMainHands(Minecraft minecraft) {
        if (YSM_WORLD_SAVED_MAIN_HAND.isEmpty()) {
            return;
        }

        if (minecraft.level == null) {
            YSM_WORLD_SAVED_MAIN_HAND.clear();
            return;
        }

        for (Map.Entry<UUID, YsmSavedMainHand> entry : new HashMap<>(YSM_WORLD_SAVED_MAIN_HAND).entrySet()) {
            Player player = minecraft.level.getPlayerByUUID(entry.getKey());
            if (player == null) {
                YSM_WORLD_SAVED_MAIN_HAND.remove(entry.getKey());
                continue;
            }

            YsmSavedMainHand saved = entry.getValue();
            if (saved.slot() >= 0 && saved.slot() < player.getInventory().items.size()) {
                player.getInventory().items.set(saved.slot(), saved.stack());
            }
            YSM_WORLD_SAVED_MAIN_HAND.remove(entry.getKey());
        }
    }

    private static boolean vanillaPlayerRendererActive(Player player) {
        if (player == null) {
            return false;
        }
        UUID id = player.getUUID();
        return VANILLA_RENDERED_THIS_FRAME.contains(id) || VANILLA_RENDERED_LAST_FRAME.contains(id);
    }

    private static void finishVanillaRenderFrame() {
        VANILLA_RENDERED_LAST_FRAME.clear();
        VANILLA_RENDERED_LAST_FRAME.addAll(VANILLA_RENDERED_THIS_FRAME);
        VANILLA_RENDERED_THIS_FRAME.clear();
    }

    private static void clearVanillaRenderFrameMarkers() {
        VANILLA_RENDERED_THIS_FRAME.clear();
        VANILLA_RENDERED_LAST_FRAME.clear();
    }

    private static void renderYsmWorldFallbackShield(RenderLevelStageEvent event, Player player, MultiBufferSource buffer) {
        float partialTick = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        double x = Mth.lerp(partialTick, player.xOld, player.getX()) - event.getCamera().getPosition().x;
        double y = Mth.lerp(partialTick, player.yOld, player.getY()) - event.getCamera().getPosition().y;
        double z = Mth.lerp(partialTick, player.zOld, player.getZ()) - event.getCamera().getPosition().z;
        boolean frontShield = ClientSinevaRenderState.isShieldDeployed(player);
        float bash = ClientSinevaVisualState.bashProgress(partialTick);
        float charge = ClientSinevaVisualState.chargeProgress(partialTick);
        float thrust = Mth.sin(bash * Mth.PI) * 0.18f + charge * 0.14f;
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        if (frontShield) {
            poseStack.translate(0.0d, 0.38d, -0.58d - thrust * 0.45f);
        } else {
            poseStack.translate(0.0d, 0.6625d, 0.40d);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0f));
        }
        renderSinevaShield(poseStack, buffer, LevelRenderer.getLightColor(player.level(), player.blockPosition()), false,
                ClientSinevaRenderState.isViewportBroken(player), THIRD_PERSON_WORLD_VIEWPORT_Y);
        poseStack.popPose();
    }

    private static void renderYsmWorldFallbackGrapple(RenderLevelStageEvent event, Player player, MultiBufferSource buffer) {
        float partialTick = com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick());
        Vec3 muzzle = GrappleHookEntity.ropeOrigin(player, partialTick)
                .subtract(event.getCamera().getPosition());
        Vec3 look = player.getViewVector(partialTick);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(muzzle.x, muzzle.y, muzzle.z);
        alignModelX(poseStack, look);
        poseStack.translate(-0.40D, 0.0D, 0.0D);
        poseStack.scale(0.55F, 0.55F, 0.55F);
        BlockbenchAnimatedModelRenderer.render(
                GRAPPLE_GUN_MODEL, null, 0.0F, poseStack, buffer,
                LevelRenderer.getLightColor(player.level(), player.blockPosition()));
        poseStack.popPose();
    }

    private static void alignModelX(PoseStack poseStack, Vec3 direction) {
        if (direction.lengthSqr() < 0.000001D) {
            return;
        }
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x));
        float pitch = (float) Math.toDegrees(Math.atan2(direction.y, horizontal));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
    }

    private record YsmSavedMainHand(int slot, ItemStack stack) {
    }

    private static boolean shouldRenderFor(Player player) {
        return player != null
                && !player.isInvisible()
                && ClientSinevaRenderState.isBombSuitActive(player);
    }

    private static boolean shouldHideHeldHands(Player player) {
        return shouldRenderFor(player) && ClientSinevaRenderState.isShieldDeployed(player);
    }

    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new SinevaShieldLayer(renderer));
            }
        }
    }

    public static final class SinevaShieldLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public SinevaShieldLayer(PlayerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                AbstractClientPlayer player,
                float limbSwing,
                float limbSwingAmount,
                float partialTick,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
        ) {
            if (!shouldRenderFor(player)) {
                return;
            }

            boolean frontShield = ClientSinevaRenderState.isShieldDeployed(player);
            float bash = ClientSinevaVisualState.bashProgress(partialTick);
            float charge = ClientSinevaVisualState.chargeProgress(partialTick);
            float thrust = Mth.sin(bash * Mth.PI) * 0.18f + charge * 0.14f;

            poseStack.pushPose();
            if (frontShield) {
                getParentModel().body.translateAndRotate(poseStack);
                poseStack.mulPose(Axis.YP.rotationDegrees(netHeadYaw));
                poseStack.translate(0.0d, 0.94d, -0.54d - thrust * 0.45f);
            } else {
                getParentModel().body.translateAndRotate(poseStack);
                poseStack.translate(0.0d, 0.9875d, 0.32d);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(10.0f));
            }
            renderSinevaShield(poseStack, buffer, packedLight, false,
                    ClientSinevaRenderState.isViewportBroken(player), THIRD_PERSON_MODEL_VIEWPORT_Y, true);
            poseStack.popPose();
        }
    }

    private static void renderSinevaShield(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean firstPerson, boolean viewportBroken) {
        float viewportY = firstPerson ? FIRST_PERSON_VIEWPORT_Y : THIRD_PERSON_WORLD_VIEWPORT_Y;
        renderSinevaShield(poseStack, buffer, packedLight, firstPerson, viewportBroken, viewportY, false);
    }

    private static void renderSinevaShield(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean firstPerson, boolean viewportBroken, float viewportY) {
        renderSinevaShield(poseStack, buffer, packedLight, firstPerson, viewportBroken, viewportY, false);
    }

    private static void renderSinevaShield(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            boolean firstPerson,
            boolean viewportBroken,
            float viewportY,
            boolean modelSpaceYDown
    ) {
        if (!firstPerson && renderSinevaShieldModel(
                poseStack, buffer, packedLight, false, viewportBroken, modelSpaceYDown)) {
            return;
        }
        float outerWidth = firstPerson ? 3.20f : (float) SinevaShieldGeometry.SHIELD_WORLD_WIDTH;
        float outerHeight = firstPerson ? 2.10f : (float) SinevaShieldGeometry.SHIELD_WORLD_HEIGHT;
        float faceWidth = outerWidth - 0.12f;
        float faceHeight = outerHeight - (firstPerson ? 0.12f : 0.16f);
        float viewportX = 0.0f;
        float viewportWidth = firstPerson ? 2.70f : (float) SinevaShieldGeometry.VIEWPORT_WORLD_WIDTH;
        float viewportHeight = firstPerson ? 1.54f : (float) SinevaShieldGeometry.VIEWPORT_WORLD_HEIGHT;
        float frame = firstPerson ? 0.045f : 0.055f;

        VertexConsumer bodyConsumer = buffer.getBuffer(DfsRenderTypes.untexturedQuads());
        renderRectAroundHole(poseStack, bodyConsumer, outerWidth, outerHeight,
                faceWidth, faceHeight, 0.000f, OUTER_R, OUTER_G, OUTER_B, 255);
        renderRectAroundHole(poseStack, bodyConsumer, faceWidth, faceHeight,
                viewportX, viewportY, viewportWidth + frame * 2.0f, viewportHeight + frame * 2.0f,
                0.002f, FACE_R, FACE_G, FACE_B, 255);

        if (!firstPerson) {
            float trimX = outerWidth * 0.36f;
            renderRect(poseStack, bodyConsumer, -trimX, -0.10f, 0.055f,
                    faceHeight * 0.76f, 0.004f, TRIM_R, TRIM_G, TRIM_B, 255);
            renderRect(poseStack, bodyConsumer, trimX, -0.10f, 0.055f,
                    faceHeight * 0.76f, 0.004f, TRIM_R, TRIM_G, TRIM_B, 255);
        }
        if (!viewportBroken) {
            renderFrame(poseStack, bodyConsumer, viewportX, viewportY, viewportWidth, viewportHeight,
                    frame, 0.006f, GLASS_R, GLASS_G, GLASS_B, 255);
        }

        if (!viewportBroken) {
            VertexConsumer glassConsumer = buffer.getBuffer(DfsRenderTypes.untexturedQuads());
            renderRect(poseStack, glassConsumer, viewportX, viewportY, viewportWidth, viewportHeight,
                    0.008f, GLASS_R, GLASS_G, GLASS_B, firstPerson ? 112 : 90);
            renderRect(poseStack, glassConsumer, viewportX - viewportWidth * 0.30f,
                    viewportY + viewportHeight * 0.24f, viewportWidth * 0.30f, 0.018f,
                    0.010f, 214, 235, 238, firstPerson ? 170 : 130);
            if (firstPerson) {
                renderRect(poseStack, glassConsumer, viewportX + viewportWidth * 0.25f,
                        viewportY - viewportHeight * 0.22f, viewportWidth * 0.24f, 0.014f,
                        0.011f, 190, 222, 232, 128);
                renderRect(poseStack, glassConsumer, viewportX - viewportWidth * 0.10f,
                        viewportY - viewportHeight * 0.05f, viewportWidth * 0.52f, 0.012f,
                        0.012f, 226, 246, 250, 94);
            }
        }
    }

    private static boolean renderSinevaShieldModel(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            boolean firstPerson,
            boolean viewportBroken,
            boolean modelSpaceYDown
    ) {
        poseStack.pushPose();
        if (firstPerson) {
            poseStack.translate(0.0D, -0.46D, 0.0D);
            poseStack.scale(1.90F, 1.04F, 1.0F);
        } else if (modelSpaceYDown) {
            poseStack.translate(0.0D, 0.48D, 0.0D);
            poseStack.scale(1.0F, -1.0F, 1.0F);
        } else {
            poseStack.translate(0.0D, -0.78D, 0.0D);
        }
        boolean rendered = BlockbenchAnimatedModelRenderer.render(
                RIOT_SHIELD_MODEL,
                ClientSinevaVisualState.shieldAnimation(),
                ClientSinevaVisualState.shieldAnimationSeconds(com.rzy.dealt_force_skills.client.ClientRenderCompat.frameTime(Minecraft.getInstance())),
                poseStack,
                buffer,
                packedLight,
                viewportBroken ? Set.of("viewport_glass") : Set.of()
        );
        poseStack.popPose();
        return rendered;
    }

    public static void startGrappleFire() {
        ClientToolModelAnimationState.start(GRAPPLE_GUN_KEY, GRAPPLE_GUN_MODEL, "fire", 0.5F);
    }

    private static void renderRectAroundHole(
            PoseStack poseStack,
            VertexConsumer consumer,
            float outerWidth,
            float outerHeight,
            float holeWidth,
            float holeHeight,
            float centerZ,
            int r,
            int g,
            int b,
            int a
    ) {
        renderRectAroundHole(poseStack, consumer, outerWidth, outerHeight, 0.0f, 0.0f, holeWidth, holeHeight, centerZ, r, g, b, a);
    }

    private static void renderRectAroundHole(
            PoseStack poseStack,
            VertexConsumer consumer,
            float outerWidth,
            float outerHeight,
            float holeCenterX,
            float holeCenterY,
            float holeWidth,
            float holeHeight,
            float centerZ,
            int r,
            int g,
            int b,
            int a
    ) {
        float left = -outerWidth * 0.5f;
        float right = outerWidth * 0.5f;
        float bottom = -outerHeight * 0.5f;
        float top = outerHeight * 0.5f;
        float holeLeft = holeCenterX - holeWidth * 0.5f;
        float holeRight = holeCenterX + holeWidth * 0.5f;
        float holeBottom = holeCenterY - holeHeight * 0.5f;
        float holeTop = holeCenterY + holeHeight * 0.5f;

        renderRectFromBounds(poseStack, consumer, left, right, holeTop, top, centerZ, r, g, b, a);
        renderRectFromBounds(poseStack, consumer, left, right, bottom, holeBottom, centerZ, r, g, b, a);
        renderRectFromBounds(poseStack, consumer, left, holeLeft, holeBottom, holeTop, centerZ, r, g, b, a);
        renderRectFromBounds(poseStack, consumer, holeRight, right, holeBottom, holeTop, centerZ, r, g, b, a);
    }

    private static void renderFrame(
            PoseStack poseStack,
            VertexConsumer consumer,
            float centerX,
            float centerY,
            float width,
            float height,
            float frame,
            float centerZ,
            int r,
            int g,
            int b,
            int a
    ) {
        renderRectAroundHole(poseStack, consumer, width + frame * 2.0f, height + frame * 2.0f,
                centerX, centerY, width, height, centerZ, r, g, b, a);
    }

    private static void renderRect(
            PoseStack poseStack,
            VertexConsumer consumer,
            float centerX,
            float centerY,
            float width,
            float height,
            float centerZ,
            int r,
            int g,
            int b,
            int a
    ) {
        renderRectFromBounds(poseStack, consumer,
                centerX - width * 0.5f, centerX + width * 0.5f,
                centerY - height * 0.5f, centerY + height * 0.5f,
                centerZ, r, g, b, a);
    }

    private static void renderRectFromBounds(
            PoseStack poseStack,
            VertexConsumer consumer,
            float left,
            float right,
            float bottom,
            float top,
            float z,
            int r,
            int g,
            int b,
            int a
    ) {
        if (right <= left || top <= bottom) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, left, bottom, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, bottom, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, top, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, left, top, z).setColor(r, g, b, a);

        consumer.addVertex(matrix, left, top, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, top, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, right, bottom, z).setColor(r, g, b, a);
        consumer.addVertex(matrix, left, bottom, z).setColor(r, g, b, a);
    }

    @EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            SinevaPlaceholderVisuals.registerLayers(event);
        }
    }
}
