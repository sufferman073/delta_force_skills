package com.rzy.dealt_force_skills.mapeditor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import java.util.*;

/** Vanilla particle packets go to a single authorized editor; no client mod/network protocol is added. */
public final class MapEditorView {
    private static final Map<ServerPlayer, UUID> EDITORS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> BUDGETS = new WeakHashMap<>();
    private static final List<String> COLOR_KINDS = List.copyOf(MarkerRules.KINDS);
    private static final int[] COLORS = {0xeeeeee, 0x40bfff, 0xffce33, 0x44ff77, 0xff8833, 0x22ffff, 0x77ddaa, 0xff4444, 0xbb77ff, 0xff44aa, 0x9933ff};
    private MapEditorView() { }

    public static boolean enabled(ServerPlayer player) { return player.hasPermissions(2) && EDITORS.containsKey(player); }
    public static void enable(ServerPlayer player, boolean enabled) {
        MarkerRules.require(player.hasPermissions(2), "Requires permission level 2");
        if (enabled) EDITORS.put(player, null); else clear(player);
    }
    public static void clear(ServerPlayer player) { EDITORS.remove(player); BUDGETS.remove(player); }
    public static void clearAll() { EDITORS.clear(); BUDGETS.clear(); }
    public static void select(ServerPlayer player, MapMarker marker) {
        MarkerRules.require(enabled(player), "Use /dfs map editor on"); EDITORS.put(player, marker.uuid());
    }

    public static BlockHitResult ray(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        return player.serverLevel().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(64)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    public static Vec3 targetPosition(ServerPlayer player) {
        BlockHitResult hit = ray(player);
        MarkerRules.require(hit.getType() == HitResult.Type.BLOCK, "No target block within 64 blocks; use here or at <x y z>");
        return Vec3.atCenterOf(hit.getBlockPos());
    }

    public static MapMarker pointed(ServerPlayer player) {
        BlockHitResult hit = ray(player); Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        double limit = hit.getType() == HitResult.Type.BLOCK ? eye.distanceTo(hit.getLocation()) + 1 : 64;
        MapMarker best = null; double score = Double.MAX_VALUE;
        for (MapMarker marker : MapMarkerData.get(player.server).all()) {
            if (!marker.dimension().equals(player.serverLevel().dimension().location().toString()) || marker.pos().distanceToSqr(eye) > 65 * 65) continue;
            Vec3 delta = marker.pos().subtract(eye); double along = delta.dot(look);
            boolean block = hit.getType() == HitResult.Type.BLOCK && (BlockPos.containing(marker.pos()).equals(hit.getBlockPos()) || MapMarkerService.targets(marker).contains(hit.getBlockPos()));
            double perpendicular = delta.subtract(look.scale(along)).lengthSqr();
            double candidate = block ? -1 + marker.pos().distanceToSqr(hit.getLocation()) * .0001
                    : along >= 0 && along <= limit && perpendicular <= 1 ? along
                    : marker.pos().distanceToSqr(player.position()) <= 4 ? 100 + marker.pos().distanceToSqr(player.position()) : Double.MAX_VALUE;
            if (candidate < score) { score = candidate; best = marker; }
        }
        return best;
    }

    public static MapMarker selected(ServerPlayer player) {
        UUID selected = EDITORS.get(player);
        MapMarker marker = selected == null ? null : MapMarkerData.get(player.server).byUuid(selected);
        return marker != null && marker.dimension().equals(player.serverLevel().dimension().location().toString()) ? marker : pointed(player);
    }

    public static void tick(ServerPlayer player) {
        if (!EDITORS.containsKey(player)) return;
        if (!player.hasPermissions(2)) { clear(player); return; }
        if (player.tickCount % 10 != 0) return;
        BUDGETS.put(player, 384);
        List<MapMarker> nearby = MapMarkerData.get(player.server).all().stream()
                .filter(m -> m.dimension().equals(player.serverLevel().dimension().location().toString()) && m.pos().distanceToSqr(player.position()) <= 64 * 64)
                .sorted(Comparator.comparingDouble(m -> m.pos().distanceToSqr(player.position()))).limit(64).toList();
        for (MapMarker marker : nearby) point(player, marker, marker.pos().add(0, .75, 0));
        MapMarker selected = selected(player);
        if (selected == null) {
            player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.looking", nearby.size()), true); return;
        }
        var config = selected.config(); String range = "-";
        if (config.has("zone")) {
            var zone = config.getAsJsonObject("zone"); double radius = zone.get("radius").getAsDouble(), height = zone.get("halfHeight").getAsDouble();
            range = zone.get("shape").getAsString() + " r=" + radius + " h=±" + height;
            outline(player, selected, radius, height, zone.get("shape").getAsString().equals("box"));
        } else if (config.has("activationRadius")) {
            double radius = config.get("activationRadius").getAsDouble(); range = "r=" + radius; outline(player, selected, radius, 0, false);
        } else if (config.has("separationRadius")) {
            double radius = config.get("separationRadius").getAsDouble(); range = "r=" + radius; outline(player, selected, radius, 0, false);
        }
        for (BlockPos target : MapMarkerService.targets(selected)) {
            // Eight corners per target keep all multi-block targets visible within the packet budget.
            for (int corner = 0; corner < 8; corner++) point(player, selected,
                    new Vec3(target.getX() + (corner & 1), target.getY() + ((corner >> 1) & 1), target.getZ() + ((corner >> 2) & 1)));
        }
        double angle = Math.toRadians(selected.yaw());
        for (int i = 0; i <= 6; i++) point(player, selected, selected.pos().add(-Math.sin(angle) * i / 3, .2, Math.cos(angle) * i / 3));
        var refs = MapMarkerService.references(selected);
        for (String id : refs.stream().limit(4).toList()) {
            try {
                MapMarker target = MapMarkerData.get(player.server).byId(id);
                if (target.dimension().equals(selected.dimension())) for (int i = 1; i <= 12; i++)
                    point(player, selected, selected.pos().lerp(target.pos(), i / 12.0));
            } catch (IllegalArgumentException ignored) { /* Invalid references are shown by validate. */ }
        }
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.overlay", MapEditorPanel.kindName(selected), selected.id(), range,
                selected.yaw(), refs.isEmpty() ? "-" : String.join(",", refs.stream().limit(4).toList())), true);
    }

    private static void outline(ServerPlayer player, MapMarker marker, double radius, double height, boolean box) {
        if (box) { box(player, marker, new AABB(marker.pos().subtract(radius, height, radius), marker.pos().add(radius, height, radius))); return; }
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16, x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            point(player, marker, marker.pos().add(x, height, z));
            if (height > 0) point(player, marker, marker.pos().add(x, -height, z));
        }
    }

    private static void box(ServerPlayer player, MapMarker marker, AABB b) {
        for (int edge = 0; edge < 12; edge++) for (int step = 0; step <= 4; step++) {
            int axis = edge / 4, corner = edge % 4; double t = step / 4.0;
            double x = axis == 0 ? b.minX + (b.maxX - b.minX) * t : (corner & 1) == 0 ? b.minX : b.maxX;
            double y = axis == 1 ? b.minY + (b.maxY - b.minY) * t : (axis == 0 ? (corner & 1) : (corner & 2)) == 0 ? b.minY : b.maxY;
            double z = axis == 2 ? b.minZ + (b.maxZ - b.minZ) * t : (corner & 2) == 0 ? b.minZ : b.maxZ;
            point(player, marker, new Vec3(x, y, z));
        }
    }

    private static void point(ServerPlayer player, MapMarker marker, Vec3 at) {
        int budget = BUDGETS.getOrDefault(player, 0);
        if (budget <= 0 || at.distanceToSqr(player.position()) > 64 * 64) return;
        BUDGETS.put(player, budget - 1);
        int index = COLOR_KINDS.indexOf(marker.kind()), rgb = COLORS[Math.max(0, index)];
        var dust = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.0f);
        player.serverLevel().sendParticles(player, dust, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }
}
