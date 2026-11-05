package com.rzy.dealt_force_skills.home;

import com.rzy.dealt_force_skills.mixin.HomeServerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import java.util.List;
import java.util.UUID;

/** UUID levels are restored from HomeData before player login; registries stay frozen. */
public final class HomeDimensions {
    public static final String NAMESPACE = "dealt_force_skills", PREFIX = "home/";
    public static final ResourceKey<net.minecraft.world.level.dimension.DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(NAMESPACE, "home"));
    private HomeDimensions() { }
    public static ResourceKey<Level> key(UUID owner) { return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NAMESPACE, PREFIX + owner)); }
    public static UUID owner(ResourceKey<Level> key) {
        ResourceLocation id = key.location();
        if (!id.getNamespace().equals(NAMESPACE) || !id.getPath().startsWith(PREFIX)) return null;
        try { return UUID.fromString(id.getPath().substring(PREFIX.length())); } catch (IllegalArgumentException ignored) { return null; }
    }
    public static boolean isHome(Level level) { return owner(level.dimension()) != null; }
    public static ServerLevel getOrCreate(MinecraftServer server, HomeData.Home home) {
        ServerLevel existing = server.getLevel(key(home.owner));
        if (existing != null) return existing;
        HomeTerrain.backupLegacy(server, home);
        var registries = server.registryAccess();
        var settings = new net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings(
                java.util.Optional.of(net.minecraft.core.HolderSet.direct()),
                registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS), List.of());
        settings.getLayersInfo().add(new net.minecraft.world.level.levelgen.flat.FlatLayerInfo(1, Blocks.AIR));
        settings.updateLayers();
        LevelStem stem = new LevelStem(registries.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(TYPE),
                new net.minecraft.world.level.levelgen.FlatLevelSource(settings));
        HomeServerAccess access = (HomeServerAccess) server;
        ServerLevel level = new ServerLevel(server, access.homeExecutor(), access.homeStorage(),
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()), key(home.owner), stem,
                access.homeProgress().create(0), false, server.getWorldData().worldGenOptions().seed(), List.of(), true, null);
        level.getWorldBorder().setCenter(0, 0);
        level.getWorldBorder().setSize(59_999_968);
        level.getWorldBorder().setWarningBlocks(0);
        level.getWorldBorder().setDamagePerBlock(0);
        server.forgeGetWorldMap().put(key(home.owner), level);
        server.markWorldsDirty();
        com.rzy.dealt_force_skills.network.NetworkHandler.sendToAll(new com.rzy.dealt_force_skills.network.S2C_HomeDimension(home.owner));
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
        return level;
    }
    public static void restore(MinecraftServer server) {
        for (HomeData.Home home : HomeData.get(server).homes.values()) getOrCreate(server, home);
    }
    public static void initialize(ServerLevel level, HomeData.Home home) {
        if (home.initialized) { fixLegacyChest(level, home); fixLegacyFurnace(level, home); return; }
        HomeTerrain.generate(level, home);
        if (!HomeSettings.flag("starter_cabin")) {
            home.initialized = true; home.chestFixed = true; home.furnaceFixed = true; HomeData.get(level.getServer()).setDirty(); return;
        }
        // Small original starter cabin, placed only on the home's first initialization.
        for (int x = -4; x <= 4; x++) for (int z = -10; z <= -3; z++) {
            level.setBlock(new BlockPos(x, 63, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, 68, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            if (x == -4 || x == 4 || z == -10 || z == -3) for (int y = 64; y <= 67; y++) {
                if (z == -3 && Math.abs(x) <= 1 && y <= 66) continue;
                boolean window = y == 65 && ((Math.abs(x) == 4 && z == -6) || (z == -10 && Math.abs(x) <= 1));
                level.setBlock(new BlockPos(x, y, z), (window ? Blocks.GLASS : Blocks.OAK_LOG).defaultBlockState(), 3);
            }
        }
        level.setBlock(new BlockPos(-3, 64, -9), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(new BlockPos(-2, 64, -9), Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        level.setBlock(new BlockPos(3, 64, -9), Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        level.setBlock(new BlockPos(0, 67, -7), Blocks.GLOWSTONE.defaultBlockState(), 3);
        if (HomeSettings.flag("starter_supplies") && level.getBlockEntity(new BlockPos(3, 64, -9)) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.OAK_PLANKS, 64)); chest.setItem(1, new ItemStack(Items.COBBLESTONE, 64));
            chest.setItem(2, new ItemStack(Items.TORCH, 32)); chest.setItem(3, new ItemStack(Items.OAK_SAPLING, 8));
            chest.setItem(4, new ItemStack(Items.WHEAT_SEEDS, 16)); chest.setItem(5, new ItemStack(Items.WATER_BUCKET));
            chest.setItem(6, new ItemStack(Items.WHITE_BED)); chest.setItem(7, new ItemStack(Items.LAVA_BUCKET));
            chest.setItem(8, new ItemStack(com.rzy.dealt_force_skills.registry.ModItems.OUTDOOR_BASEBALL_CAP.get()));
            chest.setItem(9, new ItemStack(Items.LEATHER_CHESTPLATE)); chest.setItem(10, new ItemStack(Items.LEATHER_LEGGINGS));
            chest.setItem(11, new ItemStack(Items.LEATHER_BOOTS)); chest.setItem(12, new ItemStack(Items.STONE_SWORD));
            chest.setItem(13, new ItemStack(Items.STONE_PICKAXE)); chest.setItem(14, new ItemStack(Items.STONE_AXE));
            chest.setItem(15, new ItemStack(Items.STONE_SHOVEL)); chest.setItem(16, new ItemStack(Items.STONE_HOE)); chest.setChanged();
        }
        home.initialized = true; home.chestFixed = true; home.furnaceFixed = true;
        HomeData.get(level.getServer()).setDirty();
    }
    private static void fixLegacyFurnace(ServerLevel level, HomeData.Home home) {
        if (home.furnaceFixed) return;
        BlockPos pos = new BlockPos(-2, 64, -9);
        level.getChunkAt(pos);
        var state = level.getBlockState(pos);
        if (state.is(Blocks.FURNACE) && state.getValue(net.minecraft.world.level.block.FurnaceBlock.FACING) == net.minecraft.core.Direction.NORTH
                && level.getBlockState(pos.north()).is(Blocks.OAK_LOG) && level.getBlockState(pos.below()).is(Blocks.OAK_PLANKS)) {
            level.setBlock(pos, state.setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        }
        home.furnaceFixed = true; HomeData.get(level.getServer()).setDirty();
    }
    private static void fixLegacyChest(ServerLevel level, HomeData.Home home) {
        if (home.chestFixed) return;
        BlockPos pos = new BlockPos(3, 64, -9);
        level.getChunkAt(pos);
        var state = level.getBlockState(pos);
        if (state.is(Blocks.CHEST) && state.getValue(net.minecraft.world.level.block.ChestBlock.FACING) == net.minecraft.core.Direction.NORTH
                && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) == net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                && level.getBlockState(pos.north()).is(Blocks.OAK_LOG) && level.getBlockState(pos.below()).is(Blocks.OAK_PLANKS)) {
            level.setBlock(pos, state.setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        }
        home.chestFixed = true; HomeData.get(level.getServer()).setDirty();
    }

}
