package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_LunaRevealEntities implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_LunaRevealEntities> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_luna_reveal_entities"));

    public static final StreamCodec<FriendlyByteBuf, S2C_LunaRevealEntities> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_LunaRevealEntities::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final List<Integer> entityIds;
    private final int ticks;

    public S2C_LunaRevealEntities(List<Integer> entityIds, int ticks) {
        this.entityIds = List.copyOf(entityIds);
        this.ticks = ticks;
    }

    public static void encode(S2C_LunaRevealEntities msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ticks);
        buf.writeVarInt(msg.entityIds.size());
        for (Integer entityId : msg.entityIds) {
            buf.writeVarInt(entityId);
        }
    }

    public static S2C_LunaRevealEntities decode(FriendlyByteBuf buf) {
        int ticks = buf.readVarInt();
        int count = buf.readVarInt();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(buf.readVarInt());
        }
        return new S2C_LunaRevealEntities(ids, ticks);
    }

    public static void handle(S2C_LunaRevealEntities msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLunaHudState.revealEntities(msg.entityIds, msg.ticks));
    }
}
