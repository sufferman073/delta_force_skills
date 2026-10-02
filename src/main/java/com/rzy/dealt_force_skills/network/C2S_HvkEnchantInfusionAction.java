package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.HvkEnchantInfusionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_HvkEnchantInfusionAction(BlockPos pos, Action action, int selectedSlot,
                                            String enchantmentId, int level) implements CustomPacketPayload {
    public static final Type<C2S_HvkEnchantInfusionAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "c2s_hvk_enchant_infusion_action"));
    public static final StreamCodec<FriendlyByteBuf, C2S_HvkEnchantInfusionAction> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), C2S_HvkEnchantInfusionAction::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Action { INSERT_SLOT, RETRIEVE, APPLY, APPLY_AFFIX }

    public static void encode(C2S_HvkEnchantInfusionAction msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.selectedSlot);
        buf.writeUtf(msg.enchantmentId, 256);
        buf.writeVarInt(msg.level);
    }

    public static C2S_HvkEnchantInfusionAction decode(FriendlyByteBuf buf) {
        return new C2S_HvkEnchantInfusionAction(buf.readBlockPos(), buf.readEnum(Action.class),
                buf.readVarInt(), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(C2S_HvkEnchantInfusionAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)
                    || !(player.level().getBlockEntity(msg.pos) instanceof HvkEnchantInfusionBlockEntity infusion)) return;
            switch (msg.action) {
                case INSERT_SLOT -> infusion.insertFromSlot(player, msg.selectedSlot);
                case RETRIEVE -> infusion.retrieve(player);
                case APPLY -> infusion.apply(player, msg.enchantmentId, msg.level);
                case APPLY_AFFIX -> infusion.applyAffix(player, msg.enchantmentId, msg.level);
            }
        });
    }
}
