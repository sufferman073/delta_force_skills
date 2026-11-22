package com.rzy.dealt_force_skills.mapeditor;

import com.rzy.dealt_force_skills.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import java.util.*;

/** Authoring tools only. Block interactions are intercepted before containers/doors open. */
public final class MapEditorToolItem extends Item {
    private static final String MODE = "DfsMapEditorMode";
    public enum Tool {
        PLAYER("player_spawn", "spawn.team"), OPERATIVE("operative_spawn", "spawn.team"),
        DOOR("door", "interact.paid_door"), LOOT("loot", "loot.common", "loot.rare", "loot.high"),
        MOB("mob_spawn", "spawn.continuous", "spawn.special_once", "spawn.boss_random"),
        EXTRACT("extraction", "extract.timed", "extract.conditional"), SWITCH("evac_switch", "switch.evac");
        public final String id;
        public final List<String> kinds;
        Tool(String id, String... kinds) { this.id = id; this.kinds = List.of(kinds); }
    }
    private final Tool tool;
    public MapEditorToolItem(Tool tool) { super(new Properties().stacksTo(1)); this.tool = tool; }

    private int mode(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt(MODE), tool.kinds.size());
    }
    private void mode(ItemStack stack, int value) { net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE, value)); }
    public String kind(ItemStack stack) { return tool.kinds.get(mode(stack)); }
    private boolean team() { return tool == Tool.PLAYER || tool == Tool.OPERATIVE; }
    private Component modeName(ItemStack stack) {
        return team() ? Component.translatable(getDescriptionId()) : MapEditorPanel.kindName(kind(stack));
    }

    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("commands.dealt_force_skills.map_editor.tool_mode", modeName(stack)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("commands.dealt_force_skills.map_editor.tool_hint").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("commands.dealt_force_skills.map_editor.tool_points").withStyle(ChatFormatting.GRAY));
    }

    private boolean allowed(ServerPlayer player) {
        if (MapEditorView.enabled(player)) return true;
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.tool_denied"), false);
        return false;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player) || !allowed(player)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) { cycle(player, stack); return InteractionResult.CONSUME; }
        try {
            BlockPos clicked = context.getClickedPos();
            var level = player.serverLevel();
            var state = level.getBlockState(clicked);
            if (tool == Tool.DOOR && state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                clicked = clicked.below(); state = level.getBlockState(clicked);
            }
            Vec3 pos = switch (tool) {
                case DOOR, LOOT, SWITCH -> Vec3.atCenterOf(clicked);
                default -> context.getClickedFace() == Direction.UP
                        ? new Vec3(clicked.getX() + .5, context.getClickLocation().y, clicked.getZ() + .5)
                        : Vec3.atBottomCenterOf(clicked.relative(context.getClickedFace()));
            };
            MapMarker existing = existing(player, pos);
            if (existing != null) { MapEditorPanel.show(player, existing); return InteractionResult.CONSUME; }
            if (tool == Tool.DOOR && !state.is(BlockTags.DOORS)) return invalid(player, "target_door");
            if (tool == Tool.LOOT && !(level.getBlockEntity(clicked) instanceof Container)) return invalid(player, "target_container");
            if (tool == Tool.SWITCH && !(state.getBlock() instanceof LeverBlock)) return invalid(player, "target_switch");
            create(player, stack, pos, clicked);
            return InteractionResult.CONSUME;
        } catch (IllegalArgumentException e) {
            player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.error", e.getMessage()), false);
            return InteractionResult.FAIL;
        }
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer editor) || !allowed(editor)) return InteractionResultHolder.fail(stack);
        if (player.isShiftKeyDown()) cycle(editor, stack);
        else if (team() || tool == Tool.MOB || tool == Tool.EXTRACT) {
            try {
                MapMarker existing = existing(editor, player.position());
                if (existing != null) MapEditorPanel.show(editor, existing);
                else create(editor, stack, player.position(), player.blockPosition());
            } catch (IllegalArgumentException e) {
                editor.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.error", e.getMessage()), false);
                return InteractionResultHolder.fail(stack);
            }
        } else editor.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.tool_hint"), false);
        return InteractionResultHolder.consume(stack);
    }

    private void cycle(ServerPlayer player, ItemStack stack) {
        mode(stack, (mode(stack) + 1) % tool.kinds.size());
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.tool_mode", modeName(stack)), true);
    }
    private InteractionResult invalid(ServerPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor." + reason), false);
        return InteractionResult.FAIL;
    }
    private MapMarker existing(ServerPlayer player, Vec3 pos) {
        return MapMarkerData.get(player.server).all().stream()
                .filter(m -> m.dimension().equals(player.serverLevel().dimension().location().toString()))
                .filter(m -> tool.kinds.contains(m.kind()))
                .filter(m -> !team() || m.config().get("side").getAsString().equals(tool == Tool.PLAYER ? "player" : "ai"))
                .filter(m -> BlockPos.containing(m.pos()).equals(BlockPos.containing(pos)))
                .min(Comparator.comparingDouble(m -> m.pos().distanceToSqr(pos))).orElse(null);
    }
    private void create(ServerPlayer player, ItemStack stack, Vec3 pos, BlockPos target) {
        var level = player.serverLevel(); String kind = kind(stack);
        MapMarkerService.position(level, pos);
        if ((team() || tool == Tool.MOB) && !level.noCollision(new AABB(pos.x - .3, pos.y, pos.z - .3, pos.x + .3, pos.y + 1.8, pos.z + .3))) {
            invalid(player, "target_space"); return;
        }
        var config = MarkerRules.defaults(kind);
        String id = tool.id + "_" + UUID.randomUUID().toString().substring(0, 8);
        if (team()) {
            config.addProperty("side", tool == Tool.PLAYER ? "player" : "ai");
            config.addProperty("teamKey", id);
        }
        if (tool == Tool.LOOT) config.addProperty("containerBlock", BuiltInRegistries.BLOCK.getKey(level.getBlockState(target).getBlock()).toString());
        if (tool == Tool.DOOR && level.getBlockState(target).getBlock() instanceof DoorBlock)
            config.add("targetBlockOffsets", new com.google.gson.JsonParser().parse("[[0,1,0]]"));
        MapMarker marker = MapMarkerService.create(level, kind, id, pos, player.getYRot(), player.getXRot(), config);
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.created", id, MapEditorPanel.kindName(marker), pos.toString()), false);
        MapEditorPanel.show(player, marker);
    }

    public static void giveTools(ServerPlayer player) {
        if (!MapEditorView.enabled(player)) {
            player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.tool_denied"), false); return;
        }
        var missing = ModItems.MAP_EDITOR_TOOLS.values().stream().map(java.util.function.Supplier::get)
                .filter(item -> player.getInventory().items.stream().noneMatch(stack -> stack.is(item)))
                .filter(item -> !player.getOffhandItem().is(item)).toList();
        long empty = player.getInventory().items.stream().filter(ItemStack::isEmpty).count();
        if (empty < missing.size()) {
            player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.inventory_full", missing.size()), false); return;
        }
        missing.forEach(item -> player.getInventory().add(new ItemStack(item)));
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("commands.dealt_force_skills.map_editor.tools_given"), false);
    }
}
