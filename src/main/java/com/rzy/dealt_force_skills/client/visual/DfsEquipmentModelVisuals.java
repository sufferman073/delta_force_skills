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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
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
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@net.neoforged.fml.common.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class DfsEquipmentModelVisuals {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final WearFit NO_FIT = new WearFit(new Vector(1, 1, 1), 0, false, List.of());
    private static final EquipmentModel EMPTY_MODEL = new EquipmentModel(List.of(), List.of(), List.of(), List.of(), NO_FIT, NO_FIT, NO_FIT);
    private static final Set<PlayerRenderer> WEAR_RENDERERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Model EMPTY_ARMOR = new Model(RenderType::entityCutoutNoCull) {
        @Override
        public void renderToBuffer(PoseStack poses, VertexConsumer consumer, int light, int overlay, int color) {
        }
    };
    public static final IClientItemExtensions ARMOR_EXTENSION = new IClientItemExtensions() {
        @Override
        public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            if (entity instanceof AbstractClientPlayer player && stack.getItem() instanceof DfsEquipmentItem item
                    && item.profile().slot() == slot && WEAR_RENDERERS.contains(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player))) {
                EquipmentModel model = modelFor(stack);
                if (slot == EquipmentSlot.HEAD ? !model.head().isEmpty() : !model.body().isEmpty()) return EMPTY_ARMOR;
            }
            return original;
        }
    };
    private static final Map<String, EquipmentModel> MODEL_CACHE = new HashMap<>();

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerArmorModels(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (var entry : com.rzy.dealt_force_skills.registry.ModItems.ITEMS.getEntries()) {
            if (entry.get() instanceof DfsEquipmentItem item) event.registerItem(ARMOR_EXTENSION, item);
        }
    }

    private DfsEquipmentModelVisuals() {
    }

    public static void registerLayers(EntityRenderersEvent.AddLayers event) {
        MODEL_CACHE.clear();
        WEAR_RENDERERS.clear();
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new EquipmentGeometryLayer(renderer, skin == PlayerSkin.Model.SLIM));
                WEAR_RENDERERS.add(renderer);
            }
        }
    }

    public static final class EquipmentGeometryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final boolean slim;

        public EquipmentGeometryLayer(PlayerRenderer renderer, boolean slim) {
            super(renderer);
            this.slim = slim;
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
            renderPart(poseStack, consumer, getParentModel().head, model.head(), model.fit(), 0);
        }

        private void renderChest(PoseStack poseStack, VertexConsumer consumer, ItemStack stack) {
            EquipmentModel model = modelFor(stack);
            renderPart(poseStack, consumer, getParentModel().body, model.body(), model.fit(), 0);
            renderPart(poseStack, consumer, getParentModel().leftArm, model.leftArm(), model.leftFit(), slim ? -0.88f / 16 : 0);
            renderPart(poseStack, consumer, getParentModel().rightArm, model.rightArm(), model.rightFit(), slim ? 0.88f / 16 : 0);
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
            JsonObject fit = root.getAsJsonObject("wear_fit");
            return new EquipmentModel(
                    readCubes(parts, "head"),
                    readCubes(parts, "body"),
                    readCubes(parts, "left_arm"),
                    readCubes(parts, "right_arm"), readFit(fit),
                    fit == null ? NO_FIT : readFit(fit.getAsJsonObject("left_arm")),
                    fit == null ? NO_FIT : readFit(fit.getAsJsonObject("right_arm"))
            );
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load equipment model {}", location, exception);
            return EMPTY_MODEL;
        }
    }

    private static WearFit readFit(JsonObject data) {
        if (data == null) return NO_FIT;
        List<ShellQuad> shell = new ArrayList<>();
        for (JsonElement element : data.getAsJsonArray("quads")) {
            JsonArray quad = element.getAsJsonArray();
            float[] xyz = new float[12];
            for (int index = 0; index < xyz.length; index++) xyz[index] = quad.get(3 + index).getAsFloat() / 16.0f;
            shell.add(new ShellQuad(xyz, new Color(quad.get(0).getAsInt(), quad.get(1).getAsInt(), quad.get(2).getAsInt(), 255)));
        }
        return new WearFit(vector(data, "scale"), data.get("offset_y").getAsFloat() / 16.0f,
                data.get("replace").getAsBoolean(), List.copyOf(shell));
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
            List<Cube> cubes, WearFit fit, float lateralOffset
    ) {
        if (cubes.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        parent.translateAndRotate(poseStack);
        if (fit != NO_FIT && lateralOffset != 0) poseStack.translate(lateralOffset, 0, 0);
        for (ShellQuad quad : fit.shell()) {
            Matrix4f matrix = poseStack.last().pose();
            float[] xyz = quad.xyz();
            Color color = quad.color();
            for (int i = 0; i < xyz.length; i += 3) {
                consumer.addVertex(matrix, xyz[i], xyz[i + 1], xyz[i + 2]).setColor(color.r(), color.g(), color.b(), 255);
            }
        }
        if (fit.replace()) {
            poseStack.popPose();
            return;
        }
        if (fit != NO_FIT) {
            poseStack.translate(0, fit.offsetY(), 0);
            poseStack.scale(fit.scale().x(), fit.scale().y(), fit.scale().z());
        }
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

    private record ShellQuad(float[] xyz, Color color) { }

    private record WearFit(Vector scale, float offsetY, boolean replace, List<ShellQuad> shell) { }

    private record EquipmentModel(List<Cube> head, List<Cube> body, List<Cube> leftArm, List<Cube> rightArm,
                                  WearFit fit, WearFit leftFit, WearFit rightFit) {
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
