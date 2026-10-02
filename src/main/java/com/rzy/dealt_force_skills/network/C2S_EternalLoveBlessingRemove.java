package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.item.EternalLoveBlessingItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_EternalLoveBlessingRemove(ResourceLocation effectId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_EternalLoveBlessingRemove> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "c2s_eternal_love_blessing_remove"));

    public static final StreamCodec<FriendlyByteBuf, C2S_EternalLoveBlessingRemove> STREAM_CODEC =
            StreamCodec.of(C2S_EternalLoveBlessingRemove::encode, C2S_EternalLoveBlessingRemove::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buf, C2S_EternalLoveBlessingRemove msg) {
        buf.writeUtf(msg.effectId == null ? "minecraft:speed" : msg.effectId.toString());
    }

    private static C2S_EternalLoveBlessingRemove decode(FriendlyByteBuf buf) {
        try {
            return new C2S_EternalLoveBlessingRemove(ResourceLocation.parse(buf.readUtf(512)));
        } catch (RuntimeException ignored) {
            return new C2S_EternalLoveBlessingRemove(ResourceLocation.fromNamespaceAndPath("minecraft", "speed"));
        }
    }

    public static void handle(C2S_EternalLoveBlessingRemove msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                EternalLoveBlessingItem.removeRecordFromAnyBlessing(player, msg.effectId);
            }
        });
    }
}
