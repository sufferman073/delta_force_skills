package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record S2C_HomeDimension(UUID owner) {
    public static void encode(S2C_HomeDimension msg, FriendlyByteBuf buf) { buf.writeUUID(msg.owner); }
    public static S2C_HomeDimension decode(FriendlyByteBuf buf) { return new S2C_HomeDimension(buf.readUUID()); }
    public static void handle(S2C_HomeDimension msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.rzy.dealt_force_skills.client.screen.HomeScreen.addDimension(msg.owner)));
        ctx.get().setPacketHandled(true);
    }
}
