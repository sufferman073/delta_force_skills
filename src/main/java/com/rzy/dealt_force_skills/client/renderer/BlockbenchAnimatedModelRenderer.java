package com.rzy.dealt_force_skills.client.renderer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class BlockbenchAnimatedModelRenderer {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ModelData> MODEL_CACHE = new HashMap<>();
    private static ResourceManager cachedResourceManager;

    private BlockbenchAnimatedModelRenderer() {
    }

    public static boolean render(
            ResourceLocation modelId,
            String animationName,
            float animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        return render(modelId, animationName, animationSeconds, poseStack, buffer, packedLight, Set.of(), 0xFFFFFFFF);
    }

    public static boolean render(
            ResourceLocation modelId,
            String animationName,
            float animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int argbColor
    ) {
        return render(modelId, animationName, animationSeconds, poseStack, buffer, packedLight, Set.of(), argbColor);
    }

    public static boolean render(
            ResourceLocation modelId,
            String animationName,
            float animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Set<String> hiddenGroups
    ) {
        return render(modelId, animationName, animationSeconds, poseStack, buffer, packedLight, hiddenGroups, 0xFFFFFFFF);
    }

    public static boolean render(
            ResourceLocation modelId,
            String animationName,
            float animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Set<String> hiddenGroups,
            int argbColor
    ) {
        ModelData model = modelFor(modelId);
        if (model.empty()) {
            return false;
        }

        Animation animation = model.findAnimation(animationName);
        float sampleTime = animation == null ? 0.0F : animation.sampleTime(animationSeconds);
        RenderContext context = new RenderContext(
                poseStack,
                buffer,
                packedLight,
                model.textureWidth(),
                model.textureHeight(),
                model.textures(),
                animation,
                sampleTime,
                hiddenGroups == null ? Set.of() : hiddenGroups,
                argbColor,
                Map.of()
        );
        for (Node root : model.roots()) {
            renderNode(root, Vec.ZERO, context);
        }
        return true;
    }

    public static boolean renderWithGroupReplacements(
            ResourceLocation modelId,
            String animationName,
            float animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Set<String> hiddenGroups,
            Map<String, Runnable> groupReplacements
    ) {
        ModelData model = modelFor(modelId);
        if (model.empty()) {
            return false;
        }

        Animation animation = model.findAnimation(animationName);
        float sampleTime = animation == null ? 0.0F : animation.sampleTime(animationSeconds);
        RenderContext context = new RenderContext(
                poseStack,
                buffer,
                packedLight,
                model.textureWidth(),
                model.textureHeight(),
                model.textures(),
                animation,
                sampleTime,
                hiddenGroups == null ? Set.of() : hiddenGroups,
                0xFFFFFFFF,
                groupReplacements == null ? Map.of() : groupReplacements
        );
        for (Node root : model.roots()) {
            renderNode(root, Vec.ZERO, context);
        }
        return true;
    }

    public static float animationLength(ResourceLocation modelId, String animationName) {
        Animation animation = modelFor(modelId).findAnimation(animationName);
        return animation == null ? 0.0F : animation.length();
    }

    public static ResourceLocation textureLocation(ResourceLocation modelId) {
        ModelData model = modelFor(modelId);
        return model.empty() ? MissingTextureAtlasSprite.getLocation() : model.textures().get(0).location();
    }

    private static ModelData modelFor(ResourceLocation modelId) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != cachedResourceManager) {
            MODEL_CACHE.clear();
            cachedResourceManager = manager;
        }
        return MODEL_CACHE.computeIfAbsent(modelId, BlockbenchAnimatedModelRenderer::loadModel);
    }

    private static ModelData loadModel(ResourceLocation modelId) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(),
                "animated_models/" + modelId.getPath() + ".bbmodel"
        );
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            LOGGER.warn("Missing Blockbench model {}", file);
            return ModelData.EMPTY;
        }

        try (Reader reader = resource.get().openAsReader()) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject resolution = root.getAsJsonObject("resolution");
            int textureWidth = intValue(resolution, "width", 16);
            int textureHeight = intValue(resolution, "height", 16);
            List<TextureData> textures = loadTextures(modelId, root.getAsJsonArray("textures"));
            Map<String, GroupDef> groups = readGroups(root.getAsJsonArray("groups"));
            Map<String, ElementDef> elements = readElements(root.getAsJsonArray("elements"));
            List<Node> roots = readNodes(root.getAsJsonArray("outliner"), groups, elements);
            Map<String, Animation> animations = readAnimations(root.getAsJsonArray("animations"));
            return new ModelData(
                    textures,
                    textureWidth,
                    textureHeight,
                    roots,
                    animations,
                    roots.isEmpty()
            );
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to load Blockbench model {}", file, exception);
            return ModelData.EMPTY;
        }
    }

    private static List<TextureData> loadTextures(ResourceLocation modelId, JsonArray textures) throws IOException {
        if (textures == null || textures.size() == 0) {
            return List.of(new TextureData(MissingTextureAtlasSprite.getLocation(), false));
        }
        List<TextureData> loaded = new ArrayList<>(textures.size());
        for (int index = 0; index < textures.size(); index++) {
            JsonObject texture = textures.get(index).getAsJsonObject();
            String source = stringValue(texture, "source", "");
            String renderMode = stringValue(texture, "render_mode", "default");
            boolean translucent = "additive".equalsIgnoreCase(renderMode)
                    || "emissive".equalsIgnoreCase(renderMode)
                    || "layered".equalsIgnoreCase(renderMode);
            int separator = source.indexOf(',');
            if (separator < 0) {
                loaded.add(new TextureData(MissingTextureAtlasSprite.getLocation(), translucent));
                continue;
            }

            byte[] imageBytes = Base64.getDecoder().decode(source.substring(separator + 1));
            NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    modelId.getNamespace(),
                    "dynamic/blockbench/" + modelId.getPath().replace('/', '_') + "_" + index
            );
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            loaded.add(new TextureData(location, translucent));
        }
        return List.copyOf(loaded);
    }

    private static Map<String, GroupDef> readGroups(JsonArray array) {
        if (array == null) {
            return Map.of();
        }
        Map<String, GroupDef> groups = new HashMap<>();
        for (JsonElement element : array) {
            JsonObject group = element.getAsJsonObject();
            String uuid = stringValue(group, "uuid", "");
            if (!uuid.isEmpty()) {
                groups.put(uuid, new GroupDef(
                        uuid,
                        stringValue(group, "name", uuid),
                        vector(group, "origin", Vec.ZERO),
                        vector(group, "rotation", Vec.ZERO),
                        booleanValue(group, "visibility", true)
                ));
            }
        }
        return groups;
    }

    private static Map<String, ElementDef> readElements(JsonArray array) {
        if (array == null) {
            return Map.of();
        }
        Map<String, ElementDef> elements = new HashMap<>();
        for (JsonElement entry : array) {
            JsonObject element = entry.getAsJsonObject();
            String uuid = stringValue(element, "uuid", "");
            String type = stringValue(element, "type", "");
            if (uuid.isEmpty() || !booleanValue(element, "visibility", true)
                    || !booleanValue(element, "export", true)) {
                continue;
            }
            ElementDef parsed = switch (type) {
                case "cube" -> readCube(element);
                case "mesh" -> readMesh(element);
                default -> null;
            };
            if (parsed != null) {
                elements.put(uuid, parsed);
            }
        }
        return elements;
    }

    private static CubeDef readCube(JsonObject element) {
        Map<String, CubeFace> faces = new LinkedHashMap<>();
        JsonObject faceObject = element.getAsJsonObject("faces");
        if (faceObject != null) {
            for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                if (!face.has("uv") || !face.has("texture") || face.get("texture").isJsonNull()) {
                    continue;
                }
                JsonArray uv = face.getAsJsonArray("uv");
                if (uv.size() >= 4) {
                    faces.put(entry.getKey(), new CubeFace(
                            uv.get(0).getAsFloat(),
                            uv.get(1).getAsFloat(),
                            uv.get(2).getAsFloat(),
                            uv.get(3).getAsFloat(),
                            intValue(face, "rotation", 0),
                            textureIndex(face)
                    ));
                }
            }
        }
        return new CubeDef(
                stringValue(element, "uuid", ""),
                vector(element, "origin", Vec.ZERO),
                vector(element, "rotation", Vec.ZERO),
                vector(element, "from", Vec.ZERO),
                vector(element, "to", Vec.ZERO),
                faces
        );
    }

    private static MeshDef readMesh(JsonObject element) {
        Map<String, Vec> vertices = new LinkedHashMap<>();
        JsonObject vertexObject = element.getAsJsonObject("vertices");
        if (vertexObject != null) {
            for (Map.Entry<String, JsonElement> entry : vertexObject.entrySet()) {
                vertices.put(entry.getKey(), vector(entry.getValue().getAsJsonArray(), Vec.ZERO));
            }
        }

        List<MeshFace> faces = new ArrayList<>();
        JsonObject faceObject = element.getAsJsonObject("faces");
        if (faceObject != null) {
            for (Map.Entry<String, JsonElement> faceEntry : faceObject.entrySet()) {
                JsonObject face = faceEntry.getValue().getAsJsonObject();
                JsonArray vertexIds = face.getAsJsonArray("vertices");
                JsonObject uvObject = face.getAsJsonObject("uv");
                if (vertexIds == null || uvObject == null || vertexIds.size() < 3) {
                    continue;
                }
                List<String> ids = new ArrayList<>(vertexIds.size());
                Map<String, Uv> uvs = new HashMap<>();
                for (JsonElement vertexId : vertexIds) {
                    String id = vertexId.getAsString();
                    ids.add(id);
                    JsonArray uv = uvObject.getAsJsonArray(id);
                    if (uv != null && uv.size() >= 2) {
                        uvs.put(id, new Uv(uv.get(0).getAsFloat(), uv.get(1).getAsFloat()));
                    }
                }
                faces.add(new MeshFace(List.copyOf(ids), Map.copyOf(uvs), textureIndex(face)));
            }
        }
        return new MeshDef(
                stringValue(element, "uuid", ""),
                vector(element, "origin", Vec.ZERO),
                vector(element, "rotation", Vec.ZERO),
                Map.copyOf(vertices),
                List.copyOf(faces)
        );
    }

    private static List<Node> readNodes(
            JsonArray array,
            Map<String, GroupDef> groups,
            Map<String, ElementDef> elements
    ) {
        if (array == null) {
            return List.of();
        }
        List<Node> nodes = new ArrayList<>();
        for (JsonElement element : array) {
            Node node = readNode(element, groups, elements);
            if (node != null) {
                nodes.add(node);
            }
        }
        return List.copyOf(nodes);
    }

    private static Node readNode(
            JsonElement element,
            Map<String, GroupDef> groups,
            Map<String, ElementDef> elements
    ) {
        if (element.isJsonPrimitive()) {
            ElementDef modelElement = elements.get(element.getAsString());
            return modelElement == null ? null : new ElementNode(modelElement);
        }
        JsonObject object = element.getAsJsonObject();
        GroupDef group = groups.get(stringValue(object, "uuid", ""));
        if (group == null || !group.visible()) {
            return null;
        }
        return new GroupNode(group, readNodes(object.getAsJsonArray("children"), groups, elements));
    }

    private static Map<String, Animation> readAnimations(JsonArray array) {
        if (array == null) {
            return Map.of();
        }
        Map<String, Animation> animations = new LinkedHashMap<>();
        for (JsonElement entry : array) {
            JsonObject animationObject = entry.getAsJsonObject();
            String name = stringValue(animationObject, "name", "");
            float length = floatValue(animationObject, "length", 0.0F);
            boolean loop = "loop".equalsIgnoreCase(stringValue(animationObject, "loop", "once"));
            Map<String, BoneAnimation> bones = new HashMap<>();
            JsonObject animators = animationObject.getAsJsonObject("animators");
            if (animators != null) {
                for (Map.Entry<String, JsonElement> animatorEntry : animators.entrySet()) {
                    JsonObject animator = animatorEntry.getValue().getAsJsonObject();
                    if (!"bone".equals(stringValue(animator, "type", ""))) {
                        continue;
                    }
                    JsonArray keyframeArray = animator.getAsJsonArray("keyframes");
                    Map<String, List<Keyframe>> channels = new HashMap<>();
                    if (keyframeArray != null) {
                        for (JsonElement keyframeEntry : keyframeArray) {
                            JsonObject keyframe = keyframeEntry.getAsJsonObject();
                            String channel = stringValue(keyframe, "channel", "");
                            if (!"rotation".equals(channel) && !"position".equals(channel) && !"scale".equals(channel)) {
                                continue;
                            }
                            JsonArray points = keyframe.getAsJsonArray("data_points");
                            if (points == null || points.size() == 0) {
                                continue;
                            }
                            JsonObject point = points.get(0).getAsJsonObject();
                            channels.computeIfAbsent(channel, ignored -> new ArrayList<>()).add(new Keyframe(
                                    floatValue(keyframe, "time", 0.0F),
                                    vector(point, Vec.ZERO)
                            ));
                        }
                    }
                    channels.values().forEach(keyframes -> keyframes.sort(Comparator.comparingDouble(Keyframe::time)));
                    bones.put(animatorEntry.getKey(), new BoneAnimation(Map.copyOf(channels)));
                }
            }
            Animation animation = new Animation(name, length, loop, Map.copyOf(bones));
            animations.put(name.toLowerCase(Locale.ROOT), animation);
        }
        return Map.copyOf(animations);
    }

    private static void renderNode(Node node, Vec parentOrigin, RenderContext context) {
        if (node instanceof GroupNode groupNode) {
            GroupDef group = groupNode.group();
            if (context.hiddenGroups().contains(group.name())) {
                return;
            }
            BoneTransform animation = context.animation() == null
                    ? BoneTransform.IDENTITY
                    : context.animation().transform(group.uuid(), context.sampleTime());
            Vec relativeOrigin = group.origin().subtract(parentOrigin).add(animation.position());

            context.poseStack().pushPose();
            context.poseStack().translate(
                    relativeOrigin.x() / 16.0F,
                    relativeOrigin.y() / 16.0F,
                    relativeOrigin.z() / 16.0F
            );
            context.poseStack().scale(animation.scale().x(), animation.scale().y(), animation.scale().z());
            rotate(context.poseStack(), group.rotation().add(animation.rotation()));
            Runnable replacement = context.groupReplacements().get(group.name());
            if (replacement != null) {
                replacement.run();
            } else {
                for (Node child : groupNode.children()) {
                    renderNode(child, group.origin(), context);
                }
            }
            context.poseStack().popPose();
            return;
        }

        ElementDef element = ((ElementNode) node).element();
        context.poseStack().pushPose();
        Vec relativeOrigin = element.origin().subtract(parentOrigin);
        context.poseStack().translate(
                relativeOrigin.x() / 16.0F,
                relativeOrigin.y() / 16.0F,
                relativeOrigin.z() / 16.0F
        );
        rotate(context.poseStack(), element.rotation());
        if (element instanceof CubeDef cube) {
            renderCube(cube, context);
        } else if (element instanceof MeshDef mesh) {
            renderMesh(mesh, context);
        }
        context.poseStack().popPose();
    }

    private static void renderCube(CubeDef cube, RenderContext context) {
        Vec from = cube.from().subtract(cube.origin()).scale(1.0F / 16.0F);
        Vec to = cube.to().subtract(cube.origin()).scale(1.0F / 16.0F);
        float minX = Math.min(from.x(), to.x());
        float minY = Math.min(from.y(), to.y());
        float minZ = Math.min(from.z(), to.z());
        float maxX = Math.max(from.x(), to.x());
        float maxY = Math.max(from.y(), to.y());
        float maxZ = Math.max(from.z(), to.z());

        for (Map.Entry<String, CubeFace> entry : cube.faces().entrySet()) {
            CubeFace face = entry.getValue();
            List<Vec> points;
            Vec normal;
            switch (entry.getKey()) {
                case "north" -> {
                    points = List.of(
                            new Vec(minX, minY, minZ), new Vec(maxX, minY, minZ),
                            new Vec(maxX, maxY, minZ), new Vec(minX, maxY, minZ)
                    );
                    normal = new Vec(0, 0, -1);
                }
                case "south" -> {
                    points = List.of(
                            new Vec(maxX, minY, maxZ), new Vec(minX, minY, maxZ),
                            new Vec(minX, maxY, maxZ), new Vec(maxX, maxY, maxZ)
                    );
                    normal = new Vec(0, 0, 1);
                }
                case "west" -> {
                    points = List.of(
                            new Vec(minX, minY, maxZ), new Vec(minX, minY, minZ),
                            new Vec(minX, maxY, minZ), new Vec(minX, maxY, maxZ)
                    );
                    normal = new Vec(-1, 0, 0);
                }
                case "east" -> {
                    points = List.of(
                            new Vec(maxX, minY, minZ), new Vec(maxX, minY, maxZ),
                            new Vec(maxX, maxY, maxZ), new Vec(maxX, maxY, minZ)
                    );
                    normal = new Vec(1, 0, 0);
                }
                case "down" -> {
                    points = List.of(
                            new Vec(minX, minY, maxZ), new Vec(maxX, minY, maxZ),
                            new Vec(maxX, minY, minZ), new Vec(minX, minY, minZ)
                    );
                    normal = new Vec(0, -1, 0);
                }
                case "up" -> {
                    points = List.of(
                            new Vec(minX, maxY, minZ), new Vec(maxX, maxY, minZ),
                            new Vec(maxX, maxY, maxZ), new Vec(minX, maxY, maxZ)
                    );
                    normal = new Vec(0, 1, 0);
                }
                default -> {
                    continue;
                }
            }
            List<Uv> uvs = rotateUvs(List.of(
                    new Uv(face.u1(), face.v2()),
                    new Uv(face.u2(), face.v2()),
                    new Uv(face.u2(), face.v1()),
                    new Uv(face.u1(), face.v1())
            ), face.rotation());
            renderQuad(points, uvs, normal, face.texture(), context);
        }
    }

    private static List<Uv> rotateUvs(List<Uv> uvs, int rotation) {
        int steps = Math.floorMod(rotation / 90, 4);
        if (steps == 0) {
            return uvs;
        }
        List<Uv> rotated = new ArrayList<>(uvs);
        for (int i = 0; i < steps; i++) {
            Uv last = rotated.remove(rotated.size() - 1);
            rotated.add(0, last);
        }
        return rotated;
    }

    private static void renderMesh(MeshDef mesh, RenderContext context) {
        for (MeshFace face : mesh.faces()) {
            List<Vec> points = new ArrayList<>(face.vertexIds().size());
            List<Uv> uvs = new ArrayList<>(face.vertexIds().size());
            for (String vertexId : face.vertexIds()) {
                Vec point = mesh.vertices().get(vertexId);
                Uv uv = face.uvs().get(vertexId);
                if (point == null || uv == null) {
                    points.clear();
                    break;
                }
                points.add(point.scale(1.0F / 16.0F));
                uvs.add(uv);
            }
            if (points.size() == 3) {
                points.add(points.get(2));
                uvs.add(uvs.get(2));
            }
            if (points.size() == 4) {
                renderQuad(points, uvs, normal(points), face.texture(), context);
            }
        }
    }

    private static Vec normal(List<Vec> points) {
        Vec first = points.get(1).subtract(points.get(0));
        Vec second = points.get(2).subtract(points.get(0));
        return first.cross(second).normalize();
    }

    private static void renderQuad(
            List<Vec> points,
            List<Uv> uvs,
            Vec normal,
            int textureIndex,
            RenderContext context
    ) {
        PoseStack.Pose pose = context.poseStack().last();
        Matrix4f matrix = pose.pose();
        VertexConsumer consumer = context.consumer(textureIndex);
        int color = context.argbColor();
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        for (int i = 0; i < 4; i++) {
            Vec point = points.get(i);
            Uv uv = uvs.get(i);
            consumer.addVertex(matrix, point.x(), point.y(), point.z())
                    .setColor(red, green, blue, alpha)
                    .setUv(uv.u() / context.textureWidth(), uv.v() / context.textureHeight())
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(context.packedLight())
                    .setNormal(pose, normal.x(), normal.y(), normal.z())
                    ;
        }
    }

    private static void rotate(PoseStack poseStack, Vec rotation) {
        if (rotation.z() != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        }
        if (rotation.y() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        }
        if (rotation.x() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
        }
    }

    private static Vec vector(JsonObject object, String name, Vec fallback) {
        if (object == null || !object.has(name) || !object.get(name).isJsonArray()) {
            return fallback;
        }
        return vector(object.getAsJsonArray(name), fallback);
    }

    private static Vec vector(JsonObject object, Vec fallback) {
        if (object == null) {
            return fallback;
        }
        return new Vec(
                floatValue(object, "x", fallback.x()),
                floatValue(object, "y", fallback.y()),
                floatValue(object, "z", fallback.z())
        );
    }

    private static Vec vector(JsonArray values, Vec fallback) {
        if (values == null || values.size() < 3) {
            return fallback;
        }
        return new Vec(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static String stringValue(JsonObject object, String name, String fallback) {
        return object != null && object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString()
                : fallback;
    }

    private static boolean booleanValue(JsonObject object, String name, boolean fallback) {
        return object != null && object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsBoolean()
                : fallback;
    }

    private static int intValue(JsonObject object, String name, int fallback) {
        return object != null && object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsInt()
                : fallback;
    }

    private static int textureIndex(JsonObject face) {
        if (face == null || !face.has("texture") || face.get("texture").isJsonNull()) {
            return 0;
        }
        JsonElement value = face.get("texture");
        try {
            return Math.max(0, value.getAsInt());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static float floatValue(JsonObject object, String name, float fallback) {
        return object != null && object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsFloat()
                : fallback;
    }

    private interface Node {
    }

    private interface ElementDef {
        String uuid();

        Vec origin();

        Vec rotation();
    }

    private record GroupNode(GroupDef group, List<Node> children) implements Node {
    }

    private record ElementNode(ElementDef element) implements Node {
    }

    private record GroupDef(String uuid, String name, Vec origin, Vec rotation, boolean visible) {
    }

    private record CubeDef(
            String uuid,
            Vec origin,
            Vec rotation,
            Vec from,
            Vec to,
            Map<String, CubeFace> faces
    ) implements ElementDef {
    }

    private record MeshDef(
            String uuid,
            Vec origin,
            Vec rotation,
            Map<String, Vec> vertices,
            List<MeshFace> faces
    ) implements ElementDef {
    }

    private record CubeFace(float u1, float v1, float u2, float v2, int rotation, int texture) {
    }

    private record MeshFace(List<String> vertexIds, Map<String, Uv> uvs, int texture) {
    }

    private record TextureData(ResourceLocation location, boolean translucent) {
    }

    private record ModelData(
            List<TextureData> textures,
            int textureWidth,
            int textureHeight,
            List<Node> roots,
            Map<String, Animation> animations,
            boolean empty
    ) {
        private static final ModelData EMPTY = new ModelData(
                List.of(new TextureData(MissingTextureAtlasSprite.getLocation(), false)),
                16, 16, List.of(), Map.of(), true
        );

        private Animation findAnimation(String requestedName) {
            if (requestedName == null || requestedName.isBlank()) {
                return null;
            }
            String normalized = requestedName.toLowerCase(Locale.ROOT);
            Animation direct = animations.get(normalized);
            if (direct != null) {
                return direct;
            }
            for (Map.Entry<String, Animation> entry : animations.entrySet()) {
                if (entry.getKey().endsWith("." + normalized)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    private record Animation(String name, float length, boolean loop, Map<String, BoneAnimation> bones) {
        private float sampleTime(float seconds) {
            if (length <= 0.0F) {
                return 0.0F;
            }
            if (loop) {
                return Math.floorMod((long) (seconds * 1000000.0F), (long) (length * 1000000.0F)) / 1000000.0F;
            }
            return Math.max(0.0F, Math.min(length, seconds));
        }

        private BoneTransform transform(String boneUuid, float time) {
            BoneAnimation animation = bones.get(boneUuid);
            if (animation == null) {
                return BoneTransform.IDENTITY;
            }
            return new BoneTransform(
                    animation.sample("position", time, Vec.ZERO),
                    animation.sample("rotation", time, Vec.ZERO),
                    animation.sample("scale", time, Vec.ONE)
            );
        }
    }

    private record BoneAnimation(Map<String, List<Keyframe>> channels) {
        private Vec sample(String channel, float time, Vec fallback) {
            List<Keyframe> keyframes = channels.get(channel);
            if (keyframes == null || keyframes.isEmpty()) {
                return fallback;
            }
            if (time <= keyframes.get(0).time()) {
                return keyframes.get(0).value();
            }
            Keyframe last = keyframes.get(keyframes.size() - 1);
            if (time >= last.time()) {
                return last.value();
            }
            for (int i = 1; i < keyframes.size(); i++) {
                Keyframe next = keyframes.get(i);
                if (time <= next.time()) {
                    Keyframe previous = keyframes.get(i - 1);
                    float span = next.time() - previous.time();
                    float progress = span <= 0.0F ? 0.0F : (time - previous.time()) / span;
                    return previous.value().lerp(next.value(), progress);
                }
            }
            return last.value();
        }
    }

    private record Keyframe(float time, Vec value) {
    }

    private record BoneTransform(Vec position, Vec rotation, Vec scale) {
        private static final BoneTransform IDENTITY = new BoneTransform(Vec.ZERO, Vec.ZERO, Vec.ONE);
    }

    private record RenderContext(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            float textureWidth,
            float textureHeight,
            List<TextureData> textures,
            Animation animation,
            float sampleTime,
            Set<String> hiddenGroups,
            int argbColor,
            Map<String, Runnable> groupReplacements
    ) {
        private VertexConsumer consumer(int textureIndex) {
            TextureData texture = textures.get(Math.max(0, Math.min(textureIndex, textures.size() - 1)));
            boolean translucent = texture.translucent() || (argbColor >>> 24 & 0xFF) < 255;
            RenderType renderType = translucent
                    ? RenderType.entityTranslucent(texture.location())
                    : RenderType.entityCutoutNoCull(texture.location());
            return buffer.getBuffer(renderType);
        }
    }

    private record Uv(float u, float v) {
    }

    private record Vec(float x, float y, float z) {
        private static final Vec ZERO = new Vec(0.0F, 0.0F, 0.0F);
        private static final Vec ONE = new Vec(1.0F, 1.0F, 1.0F);

        private Vec add(Vec other) {
            return new Vec(x + other.x, y + other.y, z + other.z);
        }

        private Vec subtract(Vec other) {
            return new Vec(x - other.x, y - other.y, z - other.z);
        }

        private Vec scale(float factor) {
            return new Vec(x * factor, y * factor, z * factor);
        }

        private Vec lerp(Vec other, float progress) {
            return new Vec(
                    x + (other.x - x) * progress,
                    y + (other.y - y) * progress,
                    z + (other.z - z) * progress
            );
        }

        private Vec cross(Vec other) {
            return new Vec(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }

        private Vec normalize() {
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            return length < 0.000001F ? new Vec(0.0F, 1.0F, 0.0F) : scale(1.0F / length);
        }
    }
}