package com.rzy.dealt_force_skills.block;

import com.rzy.dealt_force_skills.arena.HvkAsaraArenaManager;
import com.rzy.dealt_force_skills.arena.HvkAsaraArenaModifier;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_OpenHvkAsaraArena;
import com.rzy.dealt_force_skills.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class HvkAsaraArenaBlockEntity extends BlockEntity {
    public static final int MAX_STRUCTURE_ID_LENGTH = 256;
    public static final int MAX_STRUCTURES = 8192;
    private static final String PLAYER_CONFIGS_KEY = "HvkAsaraArenaConfigs";
    private static final String STRUCTURE_ID_KEY = "StructureId";
    private static final String MODIFIER_MASK_KEY = "ModifierMask";

    private ResourceLocation legacyStructureId;
    private long legacyModifierMask;

    public HvkAsaraArenaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HVK_ASARA_ARENA.get(), pos, state);
    }

    public void openConfiguration(ServerPlayer player) {
        Selection selection = selectionFor(player);
        NetworkHandler.sendToPlayer(new S2C_OpenHvkAsaraArena(worldPosition, selection.structureId,
                selection.modifierMask, availableStructures(player)), player);
    }

    public boolean configure(ServerPlayer player, String requestedStructure, long requestedMask, boolean start) {
        if (!isAccessibleBy(player)) {
            return false;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(requestedStructure);
        if (parsed == null || !player.server.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(parsed)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.dealt_force_skills.hvk_asara_arena.invalid_structure"), true);
            return false;
        }
        Selection selection = new Selection(parsed.toString(), HvkAsaraArenaModifier.sanitize(requestedMask));
        saveSelection(player, selection);
        if (start) {
            return HvkAsaraArenaManager.start(player, parsed, selection.modifierMask);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.dealt_force_skills.hvk_asara_arena.saved", HvkAsaraArenaModifier.score(selection.modifierMask)), true);
        return true;
    }

    private List<String> availableStructures(ServerPlayer player) {
        return player.server.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .map(ResourceLocation::toString).filter(id -> id.length() <= MAX_STRUCTURE_ID_LENGTH)
                .sorted().limit(MAX_STRUCTURES).toList();
    }

    private Selection selectionFor(ServerPlayer player) {
        CompoundTag configs = player.getPersistentData().getCompound(PLAYER_CONFIGS_KEY);
        String key = selectionKey(player);
        if (!configs.contains(key, Tag.TAG_COMPOUND)) {
            return new Selection(legacyStructureId == null ? "" : legacyStructureId.toString(),
                    HvkAsaraArenaModifier.sanitize(legacyModifierMask));
        }
        CompoundTag config = configs.getCompound(key);
        return new Selection(config.getString(STRUCTURE_ID_KEY),
                HvkAsaraArenaModifier.sanitize(config.getLong(MODIFIER_MASK_KEY)));
    }

    private void saveSelection(ServerPlayer player, Selection selection) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag configs = playerData.getCompound(PLAYER_CONFIGS_KEY);
        CompoundTag config = new CompoundTag();
        config.putString(STRUCTURE_ID_KEY, selection.structureId);
        config.putLong(MODIFIER_MASK_KEY, selection.modifierMask);
        configs.put(selectionKey(player), config);
        playerData.put(PLAYER_CONFIGS_KEY, configs);
    }

    public static void copyPlayerSelections(Player original, Player clone) {
        CompoundTag configs = original.getPersistentData().getCompound(PLAYER_CONFIGS_KEY);
        if (!configs.isEmpty()) {
            clone.getPersistentData().put(PLAYER_CONFIGS_KEY, configs.copy());
        }
    }

    private String selectionKey(ServerPlayer player) {
        return player.level().dimension().location() + ":" + worldPosition.asLong();
    }

    private boolean isAccessibleBy(ServerPlayer player) {
        return player.level() == level && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (legacyStructureId != null) {
            tag.putString(STRUCTURE_ID_KEY, legacyStructureId.toString());
        }
        tag.putLong(MODIFIER_MASK_KEY, legacyModifierMask);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        legacyStructureId = ResourceLocation.tryParse(tag.getString(STRUCTURE_ID_KEY));
        legacyModifierMask = HvkAsaraArenaModifier.sanitize(tag.getLong(MODIFIER_MASK_KEY));
    }

    private record Selection(String structureId, long modifierMask) {
    }
}
