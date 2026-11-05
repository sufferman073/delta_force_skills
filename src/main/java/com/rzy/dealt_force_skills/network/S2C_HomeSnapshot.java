package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.home.HomeData;
import com.rzy.dealt_force_skills.home.HomeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record S2C_HomeSnapshot(HomeManager.Snapshot snapshot) implements CustomPacketPayload {
    public static final Type<S2C_HomeSnapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "s2c_homesnapshot"));
    public static final StreamCodec<FriendlyByteBuf, S2C_HomeSnapshot> STREAM_CODEC = StreamCodec.of((buf, msg) -> encode(msg, buf), S2C_HomeSnapshot::decode);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void encode(S2C_HomeSnapshot msg, FriendlyByteBuf buf) {
        var s = msg.snapshot; buf.writeBoolean(s.inHome()); buf.writeUtf(s.status(), 32); buf.writeBoolean(s.close());
        buf.writeBoolean(s.enabled()); buf.writeBoolean(s.transportAllowed()); buf.writeBoolean(s.targeted()); buf.writeBoolean(s.nearby());
        buf.writeVarInt(s.regions().size());
        for (var r : s.regions()) {
            buf.writeUUID(r.owner()); buf.writeUtf(r.name(), 64); buf.writeVarInt(r.size()); buf.writeBoolean(r.own());
            buf.writeBoolean(r.created()); buf.writeBoolean(r.allowed()); buf.writeBoolean(r.pending()); buf.writeBoolean(r.online());
        }
        buf.writeVarInt(s.people().size());
        for (var p : s.people()) { buf.writeUUID(p.id()); buf.writeUtf(p.name(), 64); buf.writeBoolean(p.approved()); }
    }
    private static int count(FriendlyByteBuf buf, int max) {
        int n = buf.readVarInt(); if (n < 0 || n > max) throw new io.netty.handler.codec.DecoderException("Invalid home list length"); return n;
    }
    public static S2C_HomeSnapshot decode(FriendlyByteBuf buf) {
        boolean inHome = buf.readBoolean(); String status = buf.readUtf(32); boolean close = buf.readBoolean();
        boolean enabled = buf.readBoolean(), transportAllowed = buf.readBoolean(), targeted = buf.readBoolean(), nearby = buf.readBoolean();
        List<HomeManager.Region> regions = new ArrayList<>(); int n = count(buf, 64);
        for (int i = 0; i < n; i++) regions.add(new HomeManager.Region(buf.readUUID(), buf.readUtf(64), count(buf, HomeData.MAX_SIZE),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
        List<HomeManager.Person> people = new ArrayList<>(); n = count(buf, 32);
        for (int i = 0; i < n; i++) people.add(new HomeManager.Person(buf.readUUID(), buf.readUtf(64), buf.readBoolean()));
        return new S2C_HomeSnapshot(new HomeManager.Snapshot(List.copyOf(regions), List.copyOf(people), inHome, status, close, enabled, transportAllowed, targeted, nearby));
    }
    public static void handle(S2C_HomeSnapshot msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.rzy.dealt_force_skills.client.screen.HomeScreen.accept(msg.snapshot));
    }
}
