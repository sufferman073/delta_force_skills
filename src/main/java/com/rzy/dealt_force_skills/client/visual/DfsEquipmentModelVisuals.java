package com.rzy.dealt_force_skills.client.visual;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes;
import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DfsEquipmentModelVisuals {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EquipmentModel EMPTY_MODEL = new EquipmentModel(List.of(), List.of(), List.of(), List.of());
    private static final Map<String, EquipmentModel> MODEL_CACHE = new HashMap<>();

    private DfsEquipmentModelVisuals() {
    }

    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new EquipmentGeometryLayer(renderer));
            }
        }
    }

    public static final class EquipmentGeometryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public EquipmentGeometryLayer(PlayerRenderer renderer) {
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
            if (player == null || player.isInvisible()
                    || player.hasEffect(ModEffects.UNDEAD_TRUE_INVISIBILITY)) {
                return;
            }

            VertexConsumer consumer = buffer.getBuffer(DfsRenderTypes.equipmentSolidQuads());
            renderHead(poseStack, consumer, player.getItemBySlot(EquipmentSlot.HEAD));
            renderChest(poseStack, consumer, player.getItemBySlot(EquipmentSlot.CHEST));
        }

        private void renderHead(PoseStack poseStack, VertexConsumer consumer, ItemStack stack) {
            EquipmentModel model = modelFor(stack);
            renderPart(poseStack, consumer, getParentModel().head, model.head());
        }

        private void renderChest(PoseStack poseStack, VertexConsumer consumer, ItemStack stack) {
            EquipmentModel model = modelFor(stack);
            renderPart(poseStack, consumer, getParentModel().body, model.body());
        }
    }

    private static EquipmentModel modelFor(ItemStack stack) {
        DfsEquipmentItem.Profile profile = DfsEquipmentItem.profile(stack);
        if (profile == null) {
            return EMPTY_MODEL;
        }
        return MODEL_CACHE.computeIfAbsent(profile.id(), DfsEquipmentModelVisuals::loadModel);
    }

    private static EquipmentModel loadModel(String itemId) {
        ResourceLocation location = ResourceLocation.tryBuild(
                DealtForceSkillsMod.MODID,
                "equipment_models/" + itemId + ".json"
        );
        if (location == null) {
            return EMPTY_MODEL;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            return EMPTY_MODEL;
        }

        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject parts = root.getAsJsonObject("parts");
            return new EquipmentModel(
                    readCubes(parts, "head"),
                    readCubes(parts, "body"),
                    readCubes(parts, "left_arm"),
                    readCubes(parts, "right_arm")
            );
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load equipment model {}", location, exception);
            return EMPTY_MODEL;
        }
    }

    private static List<Cube> readCubes(JsonObject parts, String name) {
        if (parts == null || !parts.has(name)) {
            return List.of();
        }
        JsonArray array = parts.getAsJsonArray(name);
        List<Cube> cubes = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            JsonObject cube = element.getAsJsonObject();
            cubes.add(new Cube(
                    vector(cube, "from"),
                    vector(cube, "to"),
                    vector(cube, "origin"),
                    vector(cube, "rotation"),
                    color(cube.getAsJsonArray("color"))
            ));
        }
        return List.copyOf(cubes);
    }

    private static Vector vector(JsonObject object, String name) {
        JsonArray values = object.getAsJsonArray(name);
        return new Vector(
                values.get(0).getAsFloat(),
                values.get(1).getAsFloat(),
                values.get(2).getAsFloat()
        );
    }

    private static Color color(JsonArray values) {
        return new Color(
                values.get(0).getAsInt(),
                values.get(1).getAsInt(),
                values.get(2).getAsInt(),
                values.get(3).getAsInt()
        );
    }

    private static void renderPart(
            PoseStack poseStack,
            VertexConsumer consumer,
            ModelPart parent,
            List<Cube> cubes
    ) {
        if (cubes.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        parent.translateAndRotate(poseStack);
        for (Cube cube : cubes) {
            poseStack.pushPose();
            rotateAroundOrigin(poseStack, cube.origin(), cube.rotation());
            box(poseStack, consumer, cube.from(), cube.to(), cube.color());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void rotateAroundOrigin(PoseStack poseStack, Vector origin, Vector rotation) {
        if (rotation.isZero()) {
            return;
        }
        poseStack.translate(origin.x(), origin.y(), origin.z());
        if (rotation.z() != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        }
        if (rotation.y() != 0.0f) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        }
        if (rotation.x() != 0.0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
        }
        poseStack.translate(-origin.x(), -origin.y(), -origin.z());
    }

    private static void box(PoseStack poseStack, VertexConsumer consumer, Vector from, Vector to, Color color) {
        Matrix4f matrix = poseStack.last().pose();
        float minX = from.x();
        float minY = from.y();
        float minZ = from.z();
        float maxX = to.x();
        float maxY = to.y();
        float maxZ = to.z();
        face(matrix, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color, 1.08f);
        face(matrix, consumer, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, color, 0.78f);
        face(matrix, consumer, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, color, 0.88f);
        face(matrix, consumer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color, 0.93f);
        face(matrix, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, color, 1.15f);
        face(matrix, consumer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color, 0.68f);
    }

    private static void face(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            Color color,
            float shade
    ) {
        int r = shade(color.r(), shade);
        int g = shade(color.g(), shade);
        int b = shade(color.b(), shade);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, color.a());
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, color.a());
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, color.a());
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, color.a());
    }

    private static int shade(int value, float factor) {
        return Math.max(0, Math.min(255, Math.round(value * factor)));
    }

    private record EquipmentModel(List<Cube> head, List<Cube> body, List<Cube> leftArm, List<Cube> rightArm) {
    }

    private record Cube(Vector from, Vector to, Vector origin, Vector rotation, Color color) {
    }

    private record Vector(float x, float y, float z) {
        private boolean isZero() {
            return x == 0.0f && y == 0.0f && z == 0.0f;
        }
    }

    private record Color(int r, int g, int b, int a) {
    }
}
