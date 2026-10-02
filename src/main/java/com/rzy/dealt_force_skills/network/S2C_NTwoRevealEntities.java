package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_NTwoRevealEntities implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_NTwoRevealEntities> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_n_two_reveal_entities"));

    public static final StreamCodec<FriendlyByteBuf, S2C_NTwoRevealEntities> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_NTwoRevealEntities::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<Integer> entityIds;

    public S2C_NTwoRevealEntities(List<Integer> entityIds) {
        this.entityIds = List.copyOf(entityIds);
    }

    public static void encode(S2C_NTwoRevealEntities msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityIds.size());
        for (Integer entityId : msg.entityIds) {
            buf.writeVarInt(entityId);
        }
    }

    public static S2C_NTwoRevealEntities decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(buf.readVarInt());
        }
        return new S2C_NTwoRevealEntities(ids);
    }

    public static void handle(S2C_NTwoRevealEntities msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNTwoHudState.revealEntities(msg.entityIds));
    }
}
