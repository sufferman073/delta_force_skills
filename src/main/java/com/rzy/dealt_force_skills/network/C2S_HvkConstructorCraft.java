package com.rzy.dealt_force_skills.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkAdvancedDisassemblyBeamEmitterBlockEntity;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class C2S_HvkConstructorCraft implements CustomPacketPayload {
    private static final int MAX_SELECTED_OUTPUTS = 64;

    public static final CustomPacketPayload.Type<C2S_HvkConstructorCraft> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_hvk_constructor_craft"));

    public static final StreamCodec<FriendlyByteBuf, C2S_HvkConstructorCraft> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_HvkConstructorCraft::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final ResourceLocation recipeId;
    private final int count;
    private final List<ResourceLocation> selectedOutputs;

    public C2S_HvkConstructorCraft(BlockPos pos, ResourceLocation recipeId, int count) {
        this(pos, recipeId, count, List.of());
    }

    public C2S_HvkConstructorCraft(BlockPos pos, ResourceLocation recipeId, int count,
                                   List<ResourceLocation> selectedOutputs) {
        this.pos = pos;
        this.recipeId = recipeId;
        this.count = Math.max(1, count);
        this.selectedOutputs = selectedOutputs == null ? List.of() : List.copyOf(selectedOutputs);
    }

    public static void encode(C2S_HvkConstructorCraft msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.recipeId == null ? "minecraft:air" : msg.recipeId.toString());
        buf.writeVarInt(msg.count);
        buf.writeVarInt(Math.min(MAX_SELECTED_OUTPUTS, msg.selectedOutputs.size()));
        for (int i = 0; i < msg.selectedOutputs.size() && i < MAX_SELECTED_OUTPUTS; i++) {
            ResourceLocation selected = msg.selectedOutputs.get(i);
            buf.writeUtf(selected == null ? "minecraft:air" : selected.toString());
        }
    }

    public static C2S_HvkConstructorCraft decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation recipeId = readResourceLocation(buf);
        int count = buf.readVarInt();
        int selectedCount = Math.max(0, Math.min(MAX_SELECTED_OUTPUTS, buf.readVarInt()));
        List<ResourceLocation> selectedOutputs = new ArrayList<>(selectedCount);
        for (int i = 0; i < selectedCount; i++) {
            selectedOutputs.add(readResourceLocation(buf));
        }
        return new C2S_HvkConstructorCraft(pos, recipeId, count, selectedOutputs);
    }

    public static void handle(C2S_HvkConstructorCraft msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(msg.pos);
            if (blockEntity instanceof HvkAdvancedStandardTemplateConstructorBlockEntity constructor) {
                constructor.craftFromUi(player, msg.recipeId, msg.count);
            } else if (blockEntity instanceof HvkAdvancedDisassemblyBeamEmitterBlockEntity emitter) {
                emitter.disassembleFromUi(player, msg.recipeId, msg.count, msg.selectedOutputs);
            }
        });
    }

    private static ResourceLocation readResourceLocation(FriendlyByteBuf buf) {
        String value = buf.readUtf(512);
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException ex) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "air");
        }
    }
}
