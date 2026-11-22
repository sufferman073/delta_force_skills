package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.feedback.SkillNotice;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Short-lived, dimension-scoped visual feedback. Does not discover hidden traps client-side. */
public final class ClientSkillFeedback {
    private record Notice(SkillNotice spec, String character, String icon, long start, long end, long timerEnd, int total) {}
    private record Threat(int entityId, String icon, Vec3 position, double radius, double warning,
                          boolean triggered, long end) {}
    private static final Map<String, Notice> NOTICES = new LinkedHashMap<>();
    private static final Map<UUID, Threat> THREATS = new LinkedHashMap<>();
    private static final Map<String, long[]> ACTIVE = new LinkedHashMap<>();
    private static Object levelIdentity;
    private static float projectionX, projectionY;
    private static int passive;
    private static long passiveUntil;
    private static String passiveCharacter = "";
    private static String availabilityPrefix = "";
    private static int availabilityMask = 7;
    private ClientSkillFeedback() {}

    private record Exit(Vec3 center, double radius, double height, boolean round, long until) {}
    private static final Map<String, Exit> EXITS = new LinkedHashMap<>();
    private static long raidEnd, raidClockUntil;
    private static void checkLevel() {
        var current = Minecraft.getInstance().level;
        if (current != levelIdentity) {
            raidClockUntil = 0; EXITS.clear(); NOTICES.clear(); THREATS.clear(); ACTIVE.clear(); passive = 0; passiveCharacter = ""; availabilityPrefix = ""; levelIdentity = current;
        }
    }

    public static void projection(org.joml.Matrix4f matrix) {
        projectionX = matrix.m00(); projectionY = matrix.m11();
    }

    public static void accept(CompoundTag data) {
        checkLevel();
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !data.getString("Dimension").equals(minecraft.level.dimension().location().toString())) return;
        long tick = minecraft.level.getGameTime();
        switch (data.getString("Kind")) {
            case "raid_exit" -> EXITS.put(data.getString("Id"), new Exit(new Vec3(data.getDouble("X"), data.getDouble("Y"), data.getDouble("Z")),
                    data.getDouble("Radius"), data.getDouble("Height"), data.getBoolean("Round"), tick + data.getLong("Ticks")));
            case "raid_clock" -> { raidEnd = tick + data.getLong("Remaining"); raidClockUntil = tick + 40; }
            case "notice" -> {
                String id = data.getString("Notice");
                SkillNotice spec = SkillNotice.find(id);
                if (spec == null) return;
                String token = id + data.getString("Character") + data.getString("NoticeSource");
                int remaining = Math.max(0, data.getInt("Remaining"));
                int total = Math.max(remaining, data.getInt("Total"));
                Notice previous = NOTICES.get(token);
                if (previous != null && previous.end > tick && remaining > 0) total = Math.max(total, previous.total);
                long start = previous != null && previous.end > tick && remaining > 0 ? previous.start : tick;
                int ttl = remaining > 0 ? Math.min(remaining, Math.max(1, data.getInt("NoticeTtl"))) : 60;
                NOTICES.put(token, new Notice(spec, data.getString("Character"), data.getString("Icon"), start,
                        tick + ttl, remaining > 0 ? tick + remaining : 0, total));
                while (NOTICES.size() > 8) NOTICES.remove(NOTICES.keySet().iterator().next());
            }
            case "threat" -> {
                if (!data.hasUUID("Source")) return;
                THREATS.put(data.getUUID("Source"), new Threat(data.getInt("Entity"), data.getString("Icon"),
                        new Vec3(data.getDouble("X"), data.getDouble("Y"), data.getDouble("Z")),
                        data.getDouble("Radius"), data.getDouble("Warning"), data.getBoolean("Triggered"),
                        tick + Math.min(60, Math.max(1, data.getInt("Ticks")))));
            }
            case "raptor_hummingbird" -> com.rzy.dealt_force_skills.client.character.ClientRaptorHudState.syncHummingbirdRemaining(data.getInt("Remaining"));
            case "nikaidou_witch" -> com.rzy.dealt_force_skills.client.character.ClientNikaidouHiroHudState.syncWitchDetails(data.getInt("Active2Cooldown"), data.getInt("CorePhase"), data.getInt("CoreLockTicks"));
            case "talents" -> com.rzy.dealt_force_skills.client.character.ClientManbaHudState.syncTalentDetails(data.getIntArray("Durations"), data.getInt("ActiveMask"));
            case "active" -> ACTIVE.put(data.getString("Icon"), new long[]{tick + 8, tick + data.getInt("Remaining")});
            case "passive" -> { passive = data.getInt("State"); passiveCharacter = data.getString("Character"); passiveUntil = tick + 20; }
            case "availability" -> { availabilityPrefix = data.getString("Prefix"); availabilityMask = data.getInt("Mask"); }
            default -> { }
        }
    }

    static void renderExits(com.mojang.blaze3d.vertex.PoseStack poses, Vec3 camera) {
        checkLevel();
        var minecraft = Minecraft.getInstance();
        if (EXITS.isEmpty() || minecraft.level == null) return;
        EXITS.values().removeIf(exit -> exit.until <= minecraft.level.getGameTime());
        var type = com.rzy.dealt_force_skills.client.renderer.DfsRenderTypes.xrayLines();
        var buffers = minecraft.renderBuffers().bufferSource();
        var vertices = buffers.getBuffer(type);
        for (Exit exit : EXITS.values()) {
            Vec3 offset = exit.center.subtract(camera);
            double scale = Math.min(1, 48 / Math.max(1, offset.length()));
            Vec3 center = offset.scale(scale);
            double radius = exit.radius * scale, height = exit.height * scale;
            if (!exit.round) {
                net.minecraft.client.renderer.LevelRenderer.renderLineBox(poses, vertices, new net.minecraft.world.phys.AABB(center, center).inflate(radius, height, radius), .18F, 1F, .42F, 1F);
                continue;
            }
            for (int index = 0; index < 32; index++) {
                double angle = index * Math.PI / 16, next = (index + 1) * Math.PI / 16;
                Vec3 bottom = center.add(Math.cos(angle)*radius, -height, Math.sin(angle)*radius);
                Vec3 neighbor = center.add(Math.cos(next)*radius, -height, Math.sin(next)*radius);
                Vec3 top = bottom.add(0, 2*height, 0);
                exitEdge(poses.last(), vertices, bottom, neighbor);
                exitEdge(poses.last(), vertices, top, neighbor.add(0, 2*height, 0));
                if (index % 4 == 0) exitEdge(poses.last(), vertices, bottom, top);
            }
        }
        buffers.endBatch(type);
    }

    private static void exitEdge(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertices, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start).normalize();
        for (Vec3 point : new Vec3[]{start, end}) vertices.vertex(pose.pose(), (float)point.x, (float)point.y, (float)point.z)
                .color(46,255,107,255).normal(pose.normal(), (float)direction.x, (float)direction.y, (float)direction.z).endVertex();
    }

    static boolean active(String key) {
        checkLevel();
        var state = ACTIVE.get(key);
        var level = Minecraft.getInstance().level;
        return state != null && level != null && state[0] > level.getGameTime();
    }

    static int remaining(String key) {
        var state = ACTIVE.get(key);
        return active(key) ? (int)Math.max(0, state[1] - Minecraft.getInstance().level.getGameTime()) : 0;
    }

    static boolean available(String key) {
        if (availabilityPrefix.isEmpty() || !key.startsWith(availabilityPrefix)) return true;
        int bit = key.endsWith("active1") ? 1 : key.endsWith("active2") ? 2 : key.endsWith("core") ? 4 : 0;
        return bit == 0 || (availabilityMask & bit) != 0;
    }

    static SkillIcons.State passiveState() {
        checkLevel();
        var level = Minecraft.getInstance().level;
        if (level != null && level.getGameTime() < passiveUntil
                && ClientCharacterSelectionState.displayedCharacterId().equals(passiveCharacter))
            return passive == 2 ? SkillIcons.State.DISABLED : passive == 1 ? SkillIcons.State.ACTIVE : PassiveIconState.resolve();
        return PassiveIconState.resolve();
    }

    public static void render(GuiGraphics graphics) {
        checkLevel();
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) return;
        long tick = minecraft.level.getGameTime();
        if (tick < raidClockUntil && minecraft.level.dimension().equals(com.rzy.dealt_force_skills.raid.RaidInstances.DIMENSION)) {
            long ticks = Math.max(0, raidEnd-tick), seconds = (ticks+19)/20;
            graphics.drawString(minecraft.font, String.format(java.util.Locale.ROOT, "行动剩余 %02d:%02d", seconds/60, seconds%60), 8, 8,
                    ticks < 12000 ? 0xFFFF4444 : 0xFFFFFFFF, true);
        }
        NOTICES.values().removeIf(n -> n.end <= tick);
        THREATS.values().removeIf(t -> t.end <= tick);
        renderThreats(graphics);
        int y = 35;
        for (Notice notice : NOTICES.values()) {
            if (y > graphics.guiHeight() / 2) break;
            y += renderNotice(graphics, notice, y, tick) + 3;
        }
    }

    private static int renderNotice(GuiGraphics graphics, Notice notice, int top, long tick) {
        if (notice.end <= tick || (notice.total > 0 && notice.timerEnd <= tick)) return 0;
        var font = Minecraft.getInstance().font;
        int maxText = Math.max(60, Math.min(310, graphics.guiWidth() - 112));
        List<FormattedCharSequence> lines = font.split(Component.translatable(notice.spec.translationKey()), maxText);
        int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        String countdown = notice.total > 0 ? String.format(java.util.Locale.ROOT, "%.1fs", Math.max(0, notice.timerEnd - tick) / 20.0F) : "";
        int timerWidth = countdown.isEmpty() ? 0 : font.width(countdown) + 8;
        int iconSize = 18, gap = 6, contentWidth = iconSize + gap + textWidth + timerWidth;
        int height = Math.max(26, lines.size() * font.lineHeight + 10), width = contentWidth + 20;
        int left = (graphics.guiWidth() - width) / 2;
        float alpha = Math.min(1F, Math.min((tick - notice.start + 1) / 5F, ((notice.total > 0 ? notice.timerEnd : notice.end) - tick) / 8F));
        int theme = notice.spec.color() & 0xFFFFFF;
        graphics.fill(left, top, left + width, top + height, ((int)(195 * alpha) << 24) | 0x13151A);
        int stripe = ((int)(65 * alpha) << 24) | theme;
        int offset = (int)(tick % 5);
        for (int x = left - 5 + offset; x < left + width - 1; x += 5) {
            int start = Math.max(left + 1, x), end = Math.min(x + 2, left + width - 1);
            if (start < end) graphics.fill(start, top + 2, end, top + height - 2, stripe);
        }
        if (notice.total > 0) {
            float progress = 1F - Math.max(0F, Math.min(1F, (notice.timerEnd - tick) / (float)notice.total));
            int head = left + 2 + Math.round((width - 4) * progress);
            graphics.fill(left + 2, top + height - 3, head, top + height - 1, ((int)(230 * alpha) << 24) | theme);
            if (head < left + width - 2) graphics.fill(head, top + 2, head + 1, top + height - 2, ((int)(180 * alpha) << 24) | theme);
        }
        int border = ((int)(235 * alpha) << 24) | theme;
        graphics.fill(left, top, left + 2, top + height, border);
        graphics.fill(left + width - 2, top, left + width, top + height, border);
        int contentX = (graphics.guiWidth() - contentWidth) / 2;
        SkillIconHud.icon(graphics, SkillIcons.notification(notice.character, notice.icon), contentX,
                top + (height - iconSize) / 2F, iconSize, ((int)(255 * alpha) << 24) | 0xFFFFFF);
        if (!countdown.isEmpty()) graphics.drawString(font, countdown, contentX + contentWidth - font.width(countdown),
                top + (height - font.lineHeight) / 2, ((int)(255 * alpha) << 24) | theme, true);
        int textY = top + (height - lines.size() * font.lineHeight) / 2;
        for (var line : lines) {
            graphics.drawString(font, line, contentX + iconSize + gap, textY, ((int)(255 * alpha) << 24) | 0xFFFFFF, true);
            textY += font.lineHeight;
        }
        return height;
    }

    private static void renderThreats(GuiGraphics graphics) {
        var minecraft = Minecraft.getInstance();
        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 look = new Vec3(camera.getLookVector()), up = new Vec3(camera.getUpVector());
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        double halfHeight = graphics.guiHeight() / 2D, halfWidth = graphics.guiWidth() / 2D;
        double tangent = Math.tan(Math.toRadians(minecraft.options.fov().get()) / 2D);
        double aspect = graphics.guiWidth() / (double)Math.max(1, graphics.guiHeight());
        for (var entry : THREATS.entrySet()) {
            Threat threat = entry.getValue();
            var entity = minecraft.level.getEntity(threat.entityId);
            if (entity != null && !entry.getKey().equals(entity.getUUID())) entity = null;
            Vec3 position = entity != null ? entity.position() : threat.position;
            double distance = minecraft.player.position().distanceTo(position);
            if (!threat.triggered && distance > threat.warning) continue;
            boolean danger = threat.triggered || distance <= threat.radius;
            int color = danger ? 0xFFFF4141 : 0xFFFFFFFF;
            Vec3 relative = position.add(0, 0.3, 0).subtract(camera.getPosition());
            double depth = relative.dot(look), horizontal = relative.dot(right), vertical = relative.dot(up);
            double px = depth > 0.01 ? horizontal / depth * (projectionX > 0 ? projectionX : 1 / tangent / aspect) : 2;
            double py = depth > 0.01 ? -vertical / depth * (projectionY > 0 ? projectionY : 1 / tangent) : 2;
            boolean onScreen = depth > 0.01 && Math.abs(px) < .9 && Math.abs(py) < .85;
            double x, y;
            if (onScreen) {
                x = halfWidth * (1 + px); y = halfHeight * (1 + py) - 14;
            } else {
                double angle = Math.atan2(horizontal, depth);
                // Above/below the camera must retain their vertical direction.
                if (Math.abs(horizontal) < .001 && Math.abs(vertical) > .001) angle = vertical > 0 ? 0 : Math.PI;
                double radius = Math.max(12, Math.min(halfWidth, halfHeight) - 34);
                x = halfWidth + Math.sin(angle) * radius; y = halfHeight - Math.cos(angle) * radius;
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotation((float)angle));
                graphics.fill(-3, -14, 4, -12, color); graphics.fill(-2, -16, 3, -14, color); graphics.fill(-1, -18, 2, -16, color);
                graphics.pose().popPose();
            }
            x = Math.max(14, Math.min(graphics.guiWidth() - 14, x));
            y = Math.max(14, Math.min(graphics.guiHeight() - 22, y));
            SkillIconHud.icon(graphics, SkillIcons.skill(threat.icon, danger ? SkillIcons.State.ACTIVE : SkillIcons.State.NORMAL),
                    (float)x - 5, (float)y - 5, 10, color);
            String text = Math.round(distance) + "m";
            graphics.pose().pushPose(); graphics.pose().translate(x, y + 6, 0); graphics.pose().scale(.65F, .65F, 1);
            graphics.drawString(minecraft.font, text, -minecraft.font.width(text) / 2, 0, color, true); graphics.pose().popPose();
        }
    }
}
