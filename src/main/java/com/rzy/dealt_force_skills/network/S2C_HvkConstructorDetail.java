package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.CraftabilityStatus;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeDetail;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeTreeLine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_HvkConstructorDetail implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_HvkConstructorDetail> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_hvk_constructor_detail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_HvkConstructorDetail> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_HvkConstructorDetail::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_MISSING = 4096;
    private static final int MAX_ALTERNATIVES =
            HvkAdvancedStandardTemplateConstructorBlockEntity.MAX_RECIPE_TREE_ALTERNATIVES;

    private final BlockPos pos;
    private final int requestId;
    private final RecipeDetail detail;

    public S2C_HvkConstructorDetail(BlockPos pos, RecipeDetail detail) {
        this(pos, 0, detail);
    }

    public S2C_HvkConstructorDetail(BlockPos pos, int requestId, RecipeDetail detail) {
        this.pos = pos;
        this.requestId = Math.max(0, requestId);
        this.detail = detail == null ? RecipeDetail.empty(ResourceLocation.fromNamespaceAndPath("minecraft", "air")) : detail;
    }

    public static void encode(S2C_HvkConstructorDetail msg, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.requestId);
        buf.writeUtf(msg.detail.id().toString());
        List<RecipeTreeLine> tree = msg.detail.tree();
        buf.writeVarInt(Math.min(HvkAdvancedStandardTemplateConstructorBlockEntity.MAX_RECIPE_TREE_LINES, tree.size()));
        for (int i = 0; i < Math.min(HvkAdvancedStandardTemplateConstructorBlockEntity.MAX_RECIPE_TREE_LINES, tree.size()); i++) {
            RecipeTreeLine line = tree.get(i);
            writeCountedItem(buf, line.stack());
            buf.writeVarInt(line.depth());
            buf.writeBoolean(line.raw());
            buf.writeBoolean(line.missing());
            List<ItemStack> alternatives = line.alternatives();
            buf.writeVarInt(Math.min(MAX_ALTERNATIVES, alternatives.size()));
            for (int j = 0; j < Math.min(MAX_ALTERNATIVES, alternatives.size()); j++) {
                writeCountedItem(buf, alternatives.get(j));
            }
        }
        List<ItemStack> missing = msg.detail.missing();
        buf.writeVarInt(Math.min(MAX_MISSING, missing.size()));
        for (int i = 0; i < Math.min(MAX_MISSING, missing.size()); i++) {
            writeCountedItem(buf, missing.get(i));
        }
        buf.writeVarInt(msg.detail.queuedCrafts());
        buf.writeVarInt(msg.detail.queuedCompleted());
        buf.writeVarInt(msg.detail.craftTicks());
        buf.writeVarInt(msg.detail.craftIntervalTicks());
        buf.writeBoolean(msg.detail.truncated());
        buf.writeUtf(msg.detail.truncationReason(), 256);
        buf.writeEnum(msg.detail.status());
    }

    public static S2C_HvkConstructorDetail decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int requestId = Math.max(0, buf.readVarInt());
        ResourceLocation id = readResourceLocation(buf);
        int treeCount = Math.max(0, Math.min(
                HvkAdvancedStandardTemplateConstructorBlockEntity.MAX_RECIPE_TREE_LINES, buf.readVarInt()));
        List<RecipeTreeLine> tree = new ArrayList<>(treeCount);
        for (int i = 0; i < treeCount; i++) {
            ItemStack stack = readCountedItem(buf);
            int depth = buf.readVarInt();
            boolean raw = buf.readBoolean();
            boolean missing = buf.readBoolean();
            int alternativeCount = Math.max(0, Math.min(MAX_ALTERNATIVES, buf.readVarInt()));
            List<ItemStack> alternatives = new ArrayList<>(alternativeCount);
            for (int j = 0; j < alternativeCount; j++) {
                alternatives.add(readCountedItem(buf));
            }
            tree.add(new RecipeTreeLine(stack, depth, raw, missing, alternatives));
        }
        int missingCount = Math.max(0, Math.min(MAX_MISSING, buf.readVarInt()));
        List<ItemStack> missing = new ArrayList<>(missingCount);
        for (int i = 0; i < missingCount; i++) {
            missing.add(readCountedItem(buf));
        }
        int queuedCrafts = Math.max(0, buf.readVarInt());
        int queuedCompleted = Math.max(0, Math.min(queuedCrafts, buf.readVarInt()));
        int craftTicks = Math.max(0, buf.readVarInt());
        int craftIntervalTicks = Math.max(1, buf.readVarInt());
        boolean truncated = buf.readBoolean();
        String truncationReason = buf.readUtf(256);
        CraftabilityStatus status = buf.readEnum(CraftabilityStatus.class);
        return new S2C_HvkConstructorDetail(pos, requestId,
                new RecipeDetail(id, tree, missing, queuedCrafts, queuedCompleted, craftTicks, craftIntervalTicks,
                        truncated, truncationReason, status));
    }

    public static void handle(S2C_HvkConstructorDetail msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkConstructorScreen.updateDetail(msg.pos, msg.requestId, msg.detail));
    }

    private static ResourceLocation readResourceLocation(RegistryFriendlyByteBuf buf) {
        String value = buf.readUtf(512);
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException ex) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        }
    }

    private static void writeItem(RegistryFriendlyByteBuf buf, ItemStack stack) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    private static ItemStack readItem(RegistryFriendlyByteBuf buf) {
        return ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    private static void writeCountedItem(RegistryFriendlyByteBuf buf, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
            writeItem(buf, ItemStack.EMPTY);
            buf.writeVarInt(0);
            return;
        }
        ItemStack wireStack = stack.copy();
        wireStack.setCount(1);
        writeItem(buf, wireStack);
        buf.writeVarInt(stack.getCount());
    }

    private static ItemStack readCountedItem(RegistryFriendlyByteBuf buf) {
        ItemStack stack = readItem(buf);
        int count = Math.max(0, buf.readVarInt());
        if (stack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        stack.setCount(count);
        return stack;
    }
}
