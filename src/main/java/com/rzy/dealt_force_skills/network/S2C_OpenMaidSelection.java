package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record S2C_OpenMaidSelection(UUID token, String current, List<String> available) {
    public static void encode(S2C_OpenMaidSelection msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.token); buf.writeUtf(msg.current, 256);
        buf.writeCollection(msg.available, (b, id) -> b.writeUtf(id, 256));
    }
    public static S2C_OpenMaidSelection decode(FriendlyByteBuf buf) {
        return new S2C_OpenMaidSelection(buf.readUUID(), buf.readUtf(256), buf.readList(b -> b.readUtf(256)));
    }
    public static void handle(S2C_OpenMaidSelection msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketBridge.invoke("com.rzy.dealt_force_skills.client.screen.MaidCharacterSelectionScreen", "open",
                        msg.token, msg.current, msg.available)));
        ctx.get().setPacketHandled(true);
    }
}
