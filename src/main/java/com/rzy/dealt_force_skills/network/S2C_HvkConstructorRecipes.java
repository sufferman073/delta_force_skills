package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeSearchResult;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeView;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_HvkConstructorRecipes implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_HvkConstructorRecipes> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_hvk_constructor_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_HvkConstructorRecipes> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_HvkConstructorRecipes::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos pos;
    private final String query;
    private final int page;
    private final int total;
    private final boolean disassembler;
    private final List<RecipeView> recipes;

    public S2C_HvkConstructorRecipes(BlockPos pos, String query, RecipeSearchResult result) {
        this(pos, query, result, false);
    }

    public S2C_HvkConstructorRecipes(BlockPos pos, String query, RecipeSearchResult result, boolean disassembler) {
        this(pos, query, result == null ? 0 : result.page(), result == null ? 0 : result.total(),
                disassembler, result == null ? List.of() : result.recipes());
    }

    private S2C_HvkConstructorRecipes(BlockPos pos, String query, int page, int total, boolean disassembler,
                                      List<RecipeView> recipes) {
        this.pos = pos;
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
        this.total = Math.max(0, total);
        this.disassembler = disassembler;
        this.recipes = recipes == null ? List.of() : List.copyOf(recipes);
    }

    public static void encode(S2C_HvkConstructorRecipes msg, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.query);
        buf.writeVarInt(msg.page);
        buf.writeVarInt(msg.total);
        buf.writeBoolean(msg.disassembler);
        S2C_OpenHvkConstructor.writeRecipes(msg.recipes, buf);
    }

    public static S2C_HvkConstructorRecipes decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String query = buf.readUtf(128);
        int page = buf.readVarInt();
        int total = buf.readVarInt();
        boolean disassembler = buf.readBoolean();
        return new S2C_HvkConstructorRecipes(pos, query, page, total, disassembler,
                S2C_OpenHvkConstructor.readRecipes(buf));
    }

    public static void handle(S2C_HvkConstructorRecipes msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkConstructorScreen.updateRecipes(msg.pos, msg.query, msg.page, msg.total, msg.recipes,
                msg.disassembler));
    }
}
