package com.rzy.dealt_force_skills.mapeditor;

import com.google.gson.JsonElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

/** Clickable chat suggestions reuse the permission-checked editor command path. */
public final class MapEditorPanel {
    private static final String PREFIX = "commands.dealt_force_skills.map_editor.";
    private MapEditorPanel() { }
    public static Component kindName(String kind) { return Component.translatable(PREFIX + "kind." + kind); }
    public static Component kindName(MapMarker marker) {
        return marker.kind().equals("spawn.team")
                ? Component.translatable(PREFIX + "side." + marker.config().get("side").getAsString()) : kindName(marker.kind());
    }
    public static void show(ServerPlayer player, MapMarker marker) {
        MapEditorView.select(player, marker);
        player.displayClientMessage(Component.translatable(PREFIX + "panel", kindName(marker), marker.id()), false);
        String base = "/dfs map marker ";
        player.displayClientMessage(button("inspect", base + "inspect " + marker.id())
                .append(" ").append(button("move_here", base + "move " + marker.id() + " here"))
                .append(" ").append(button("remove", base + "remove " + marker.id())), false);
        if (marker.kind().equals("extract.timed"))
            player.displayClientMessage(Component.translatable(PREFIX + "switch_hint"), false);
        for (String field : MarkerRules.fields(marker.kind())) {
            if (field.equals("switchId")) continue;
            JsonElement value = marker.config();
            for (String part : field.split("\\.")) value = value != null && value.isJsonObject() ? value.getAsJsonObject().get(part) : null;
            if (field.equals("switchIds")) value = MarkerRules.JSON.toJsonTree(MarkerRules.switchIds(marker.config()));
            if (field.equals("requiredSwitchCount")) value = MarkerRules.JSON.toJsonTree(MarkerRules.requiredSwitchCount(marker.config()));
            if (value != null && value.isJsonObject()) continue;
            String text = value == null ? "" : value.toString();
            String command = base + "set " + marker.id() + " " + field + " " + text;
            player.displayClientMessage(Component.literal("  ").append(Component.translatable(PREFIX + "field." + field))
                    .append(": " + (text.isEmpty() ? "-" : text)).withStyle(s -> s.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PREFIX + "edit_hint")))), false);
        }
    }
    private static MutableComponent button(String label, String command) {
        return Component.translatable(PREFIX + "button." + label).withStyle(s -> s.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
    }
}
