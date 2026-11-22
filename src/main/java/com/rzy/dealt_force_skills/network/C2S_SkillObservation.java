package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client-only camera/lock events; the server validates every observation. */
public record C2S_SkillObservation(int observer, int target) {
    public static void encode(C2S_SkillObservation msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.observer); buf.writeVarInt(msg.target);
    }
    public static C2S_SkillObservation decode(FriendlyByteBuf buf) {
        return new C2S_SkillObservation(buf.readVarInt(), buf.readVarInt());
    }
    public static void handle(C2S_SkillObservation msg, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.feedback.SkillFeedback.observe(ctx.getSender(), msg.observer, msg.target));
        ctx.setPacketHandled(true);
    }
}
