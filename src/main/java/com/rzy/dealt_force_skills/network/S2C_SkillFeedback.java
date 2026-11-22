package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server-authored visual feedback. Never accepted from a client. */
public record S2C_SkillFeedback(CompoundTag data) {
    public static void encode(S2C_SkillFeedback packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.data); }
    public static S2C_SkillFeedback decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new S2C_SkillFeedback(tag == null ? new CompoundTag() : tag);
    }
    public static void handle(S2C_SkillFeedback packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientPacketBridge.invoke(
                "com.rzy.dealt_force_skills.client.ClientSkillFeedback", "accept", packet.data));
        context.get().setPacketHandled(true);
    }
}
