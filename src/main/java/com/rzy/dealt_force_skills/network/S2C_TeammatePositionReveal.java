package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: expose online teammates to the local viewer only.
 */
public class S2C_TeammatePositionReveal implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_TeammatePositionReveal> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "s2c_teammate_position_reveal"));

    public static final StreamCodec<FriendlyByteBuf, S2C_TeammatePositionReveal> STREAM_CODEC =
            StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_TeammatePositionReveal::decode);

    private final List<Entry> entries;
    private final int teamColorRgb;

    public S2C_TeammatePositionReveal(List<Entry> entries, int teamColorRgb) {
        this.entries = List.copyOf(entries);
        this.teamColorRgb = teamColorRgb;
    }

    public static S2C_TeammatePositionReveal clear() {
        return new S2C_TeammatePositionReveal(List.of(), 0x55FFFF);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_TeammatePositionReveal msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.teamColorRgb);
        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeVarInt(entry.entityId);
        }
    }

    public static S2C_TeammatePositionReveal decode(FriendlyByteBuf buf) {
        int color = buf.readVarInt();
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readVarInt()));
        }
        return new S2C_TeammatePositionReveal(entries, color);
    }

    public static void handle(S2C_TeammatePositionReveal msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            List<Integer> ids = new ArrayList<>(msg.entries.size());
            for (Entry entry : msg.entries) {
                ids.add(entry.entityId);
            }
            ClientTeammateRevealState.apply(ids, msg.teamColorRgb);
        });
    }

    public record Entry(int entityId) {
    }
}
