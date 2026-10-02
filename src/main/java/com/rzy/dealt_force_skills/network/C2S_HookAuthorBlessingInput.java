package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.item.HookAuthorBlessingItem;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_HookAuthorBlessingInput(int button, int phase, boolean sneaking, int heldTicks)
        implements CustomPacketPayload {
    public static final Type<C2S_HookAuthorBlessingInput> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            DealtForceSkillsMod.MODID, "c2s_hook_author_blessing_input"));
    public static final StreamCodec<FriendlyByteBuf, C2S_HookAuthorBlessingInput> STREAM_CODEC =
            StreamCodec.of(C2S_HookAuthorBlessingInput::encode, C2S_HookAuthorBlessingInput::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, C2S_HookAuthorBlessingInput message) {
        buffer.writeByte(message.button);
        buffer.writeByte(message.phase);
        buffer.writeBoolean(message.sneaking);
        buffer.writeVarInt(message.heldTicks);
    }

    public static C2S_HookAuthorBlessingInput decode(FriendlyByteBuf buffer) {
        return new C2S_HookAuthorBlessingInput(buffer.readByte(), buffer.readByte(), buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(C2S_HookAuthorBlessingInput message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                HookAuthorBlessingItem.input(player, message.button, message.phase, message.sneaking, message.heldTicks);
            }
        });
    }
}
