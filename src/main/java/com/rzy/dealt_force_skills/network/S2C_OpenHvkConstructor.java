package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeView;
import com.rzy.dealt_force_skills.block.HvkAdvancedStandardTemplateConstructorBlockEntity.RecipeSearchResult;
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

public class S2C_OpenHvkConstructor implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenHvkConstructor> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_hvk_constructor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenHvkConstructor> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenHvkConstructor::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final int MAX_RECIPES = 256;
    private static final int MAX_INGREDIENTS = 256;

    private final BlockPos pos;
    private final String query;
    private final int page;
    private final int total;
    private final boolean disassembler;
    private final List<RecipeView> recipes;

    public S2C_OpenHvkConstructor(BlockPos pos, String query, RecipeSearchResult result) {
        this(pos, query, result, false);
    }

    public S2C_OpenHvkConstructor(BlockPos pos, String query, RecipeSearchResult result, boolean disassembler) {
        this(pos, query, result == null ? 0 : result.page(), result == null ? 0 : result.total(),
                disassembler, result == null ? List.of() : result.recipes());
    }

    private S2C_OpenHvkConstructor(BlockPos pos, String query, int page, int total, boolean disassembler,
                                   List<RecipeView> recipes) {
        this.pos = pos;
        this.query = query == null ? "" : query;
        this.page = Math.max(0, page);
        this.total = Math.max(0, total);
        this.disassembler = disassembler;
        this.recipes = recipes == null ? List.of() : List.copyOf(recipes);
    }

    public static void encode(S2C_OpenHvkConstructor msg, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.query);
        buf.writeVarInt(msg.page);
        buf.writeVarInt(msg.total);
        buf.writeBoolean(msg.disassembler);
        writeRecipes(msg.recipes, buf);
    }

    public static S2C_OpenHvkConstructor decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String query = buf.readUtf(128);
        int page = buf.readVarInt();
        int total = buf.readVarInt();
        boolean disassembler = buf.readBoolean();
        return new S2C_OpenHvkConstructor(pos, query, page, total, disassembler, readRecipes(buf));
    }

    public static void handle(S2C_OpenHvkConstructor msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HvkConstructorScreen.open(msg.pos, msg.query, msg.page, msg.total, msg.recipes,
                msg.disassembler));
    }

    static void writeRecipes(List<RecipeView> recipes, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(Math.min(MAX_RECIPES, recipes.size()));
        for (int i = 0; i < Math.min(MAX_RECIPES, recipes.size()); i++) {
            RecipeView recipe = recipes.get(i);
            buf.writeUtf(recipe.id().toString());
            writeCountedItem(buf, recipe.result());
            List<ItemStack> ingredients = recipe.ingredients();
            buf.writeVarInt(Math.min(MAX_INGREDIENTS, ingredients.size()));
            for (int j = 0; j < Math.min(MAX_INGREDIENTS, ingredients.size()); j++) {
                writeCountedItem(buf, ingredients.get(j));
            }
        }
    }

    static List<RecipeView> readRecipes(RegistryFriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(MAX_RECIPES, buf.readVarInt()));
        List<RecipeView> recipes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = readResourceLocation(buf);
            ItemStack result = readCountedItem(buf);
            int ingredientCount = Math.max(0, Math.min(MAX_INGREDIENTS, buf.readVarInt()));
            List<ItemStack> ingredients = new ArrayList<>(ingredientCount);
            for (int j = 0; j < ingredientCount; j++) {
                ingredients.add(readCountedItem(buf));
            }
            recipes.add(new RecipeView(id, result, ingredients));
        }
        return recipes;
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
