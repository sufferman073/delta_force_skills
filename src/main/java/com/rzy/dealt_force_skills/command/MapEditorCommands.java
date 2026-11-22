package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.rzy.dealt_force_skills.mapeditor.*;
import java.util.*;

public final class MapEditorCommands {
    private MapEditorCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var markers = Commands.literal("marker").executes(c -> run(c, "help", ""));
        var add = Commands.argument("kind", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(MarkerRules.KINDS, b))
                .executes(c -> run(c, "add", "target"));
        var addId = Commands.argument("id", StringArgumentType.word()).executes(c -> run(c, "add", "target"));
        locations(addId, "add"); add.then(addId); markers.then(Commands.literal("add").then(add));
        for (String action : List.of("inspect", "remove", "select", "edit", "move")) {
            var id = idArgument().executes(c -> run(c, action, "target"));
            if (action.equals("move")) locations(id, action);
            var command = Commands.literal(action).then(id);
            if (action.equals("inspect") || action.equals("select") || action.equals("edit")) command.executes(c -> run(c, action, ""));
            markers.then(command);
        }
        markers.then(Commands.literal("set").then(idArgument().then(fieldArgument()
                .then(Commands.argument("value", StringArgumentType.greedyString()).executes(c -> run(c, "set", ""))))));
        markers.then(Commands.literal("unset").then(idArgument().then(fieldArgument().executes(c -> run(c, "unset", "")))));
        markers.then(Commands.literal("list").executes(c -> run(c, "list", ""))
                .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(c -> run(c, "list", ""))));
        markers.then(Commands.literal("validate").executes(c -> run(c, "validate", "")));
        markers.then(Commands.literal("export").executes(c -> run(c, "export", ""))
                .then(Commands.argument("mapId", StringArgumentType.word()).executes(c -> run(c, "export", ""))));
        dispatcher.register(Commands.literal("dfs").requires(s -> s.hasPermission(2))
                .then(Commands.literal("map").requires(s -> s.hasPermission(2)).executes(c -> run(c, "help", ""))
                        .then(Commands.literal("editor")
                                .then(Commands.literal("tools").executes(c -> run(c, "tools", "")))
                                .then(Commands.literal("on").executes(c -> run(c, "on", "")))
                                .then(Commands.literal("off").executes(c -> run(c, "off", ""))))
                        .then(markers)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> idArgument() {
        return Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            if (!c.getSource().hasPermission(2)) return b.buildFuture();
            return SharedSuggestionProvider.suggest(MapMarkerData.get(c.getSource().getServer()).all().stream()
                    .filter(m -> m.dimension().equals(c.getSource().getLevel().dimension().location().toString())).map(MapMarker::id), b);
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> fieldArgument() {
        return Commands.argument("field", StringArgumentType.word()).suggests((c, b) -> {
            if (!c.getSource().hasPermission(2)) return b.buildFuture();
            try { return SharedSuggestionProvider.suggest(MarkerRules.fields(MapMarkerData.get(c.getSource().getServer()).byId(string(c, "id")).kind()), b); }
            catch (IllegalArgumentException e) { return b.buildFuture(); }
        });
    }

    private static void locations(RequiredArgumentBuilder<CommandSourceStack, String> node, String action) {
        node.then(Commands.literal("here").executes(c -> run(c, action, "here")));
        node.then(Commands.literal("at").then(Commands.argument("pos", Vec3Argument.vec3(false)).executes(c -> run(c, action, "at"))));
    }

    private static String string(CommandContext<CommandSourceStack> c, String name) { return StringArgumentType.getString(c, name); }
    private static String optional(CommandContext<CommandSourceStack> c, String name, String fallback) {
        try { return string(c, name); } catch (IllegalArgumentException e) { return fallback; }
    }
    private static void say(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.map_editor." + key, args), false);
    }

    private static int run(CommandContext<CommandSourceStack> c, String action, String location) throws CommandSyntaxException {
        var source = c.getSource();
        if (!source.hasPermission(2)) { source.sendFailure(Component.literal("Requires permission level 2")); return 0; }
        try {
            var level = source.getLevel(); var data = MapMarkerData.get(source.getServer());
            if (action.equals("help")) { say(source, "help"); return 1; }
            if (action.equals("on") || action.equals("off")) {
                var player = source.getPlayerOrException(); MapEditorView.enable(player, action.equals("on"));
                if (action.equals("on")) MapMarkerService.scan(level);
                say(source, action); return 1;
            }
            if (Set.of("add", "move", "remove", "set", "unset", "select", "edit", "tools").contains(action))
                MarkerRules.require(MapEditorView.enabled(source.getPlayerOrException()), "Use /dfs map editor on");
            if (action.equals("tools")) { MapEditorToolItem.giveTools(source.getPlayerOrException()); return 1; }
            if (action.equals("list")) {
                MapMarkerService.scan(level); int page;
                try { page = IntegerArgumentType.getInteger(c, "page"); } catch (IllegalArgumentException e) { page = 1; }
                var markers = data.all().stream().filter(m -> m.dimension().equals(level.dimension().location().toString())).sorted(Comparator.comparing(MapMarker::id)).toList();
                say(source, "list", markers.size(), page, Math.max(1, (markers.size() + 9) / 10));
                markers.stream().skip((long) (page - 1) * 10).limit(10).forEach(m -> source.sendSuccess(() -> Component.literal(m.id() + " | " + m.kind() + " | " + m.pos()), false));
                return 1;
            }
            if (action.equals("validate")) {
                var report = MapMarkerService.validate(level); say(source, "validation", report.errors().size(), report.warnings().size());
                report.errors().stream().limit(30).forEach(e -> source.sendFailure(Component.literal(e)));
                report.warnings().stream().limit(30).forEach(e -> source.sendSuccess(() -> Component.literal("WARN " + e), false));
                return report.errors().isEmpty() ? 1 : 0;
            }
            if (action.equals("export")) { say(source, "export", MapMarkerService.export(level, optional(c, "mapId", "map")).toAbsolutePath().toString()); return 1; }
            if (action.equals("add")) {
                var player = source.getPlayerOrException(); String kind = string(c, "kind");
                Vec3 pos = position(c, kind, location);
                String id = optional(c, "id", kind.replace('.', '_') + "_" + UUID.randomUUID().toString().substring(0, 8));
                MapMarker marker = MapMarkerService.create(level, kind, id, pos, player.getYRot(), player.getXRot());
                MapEditorView.select(player, marker); say(source, "created", id, MapEditorPanel.kindName(marker), pos.toString()); return 1;
            }
            String id = optional(c, "id", "");
            MapMarker marker;
            if (id.isEmpty()) { marker = MapEditorView.pointed(source.getPlayerOrException()); MarkerRules.require(marker != null, "No marker under crosshair/near your feet"); }
            else marker = data.byId(id);
            MarkerRules.require(marker.dimension().equals(level.dimension().location().toString()), "Marker is in " + marker.dimension());
            switch (action) {
                case "inspect" -> {
                    source.sendSuccess(() -> Component.literal(MarkerRules.JSON.toJson(marker.export())), false);
                    if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && MapEditorView.enabled(player)) MapEditorView.select(player, marker);
                }
                case "select", "edit" -> MapEditorPanel.show(source.getPlayerOrException(), marker);
                case "move" -> {
                    var player = source.getPlayerOrException(); MapMarkerService.move(level, marker, position(c, marker.kind(), location), player.getYRot(), player.getXRot()); say(source, "moved", marker.id());
                }
                case "remove" -> { MapMarkerService.remove(level, marker); say(source, "removed", marker.id()); }
                case "set" -> { MapMarkerService.edit(level, marker, string(c, "field"), string(c, "value")); say(source, "updated", marker.id()); }
                case "unset" -> { MapMarkerService.unset(level, marker, string(c, "field")); say(source, "updated", marker.id()); }
                default -> throw new IllegalArgumentException("Unknown action");
            }
            return 1;
        } catch (IllegalArgumentException | java.io.IOException e) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.map_editor.error", e.getMessage())); return 0;
        }
    }

    private static Vec3 position(CommandContext<CommandSourceStack> c, String kind, String location) throws CommandSyntaxException {
        if (location.equals("at")) return Vec3Argument.getVec3(c, "pos");
        var player = c.getSource().getPlayerOrException();
        if (location.equals("here") || kind.equals("spawn.team")) return player.position();
        return MapEditorView.targetPosition(player);
    }
}
