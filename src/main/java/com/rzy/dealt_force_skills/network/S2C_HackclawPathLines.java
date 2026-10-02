package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_HackclawPathLines implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_HackclawPathLines> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_hackclaw_path_lines"));

    public static final StreamCodec<FriendlyByteBuf, S2C_HackclawPathLines> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_HackclawPathLines::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final int MAX_LINES = 64;
    public static final int MAX_LINE_TICKS = 120;
    private static final int MAX_SERIALIZED_LINES = 256;

    private final List<Line> lines;

    public S2C_HackclawPathLines(List<Line> lines) {
        List<Line> limited = new ArrayList<>(Math.min(lines.size(), MAX_LINES));
        for (Line line : lines) {
            if (limited.size() >= MAX_LINES) {
                break;
            }
            if (line == null || !isFinite(line.from()) || !isFinite(line.to()) || line.ticks() <= 0) {
                continue;
            }
            limited.add(new Line(line.sourceEntityId(), line.targetEntityId(), line.from(), line.to(), line.primary(),
                    Math.min(MAX_LINE_TICKS, line.ticks())));
        }
        this.lines = List.copyOf(limited);
    }

    public static void encode(S2C_HackclawPathLines msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.lines.size());
        for (Line line : msg.lines) {
            buf.writeVarInt(line.sourceEntityId());
            buf.writeVarInt(line.targetEntityId());
            buf.writeDouble(line.from().x);
            buf.writeDouble(line.from().y);
            buf.writeDouble(line.from().z);
            buf.writeDouble(line.to().x);
            buf.writeDouble(line.to().y);
            buf.writeDouble(line.to().z);
            buf.writeBoolean(line.primary());
            buf.writeVarInt(line.ticks());
        }
    }

    public static S2C_HackclawPathLines decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_SERIALIZED_LINES) {
            throw new IllegalArgumentException("Invalid Hackclaw path line count: " + count);
        }
        List<Line> lines = new ArrayList<>(Math.min(count, MAX_LINES));
        for (int i = 0; i < count; i++) {
            int sourceEntityId = buf.readVarInt();
            int targetEntityId = buf.readVarInt();
            Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 to = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            boolean primary = buf.readBoolean();
            int ticks = buf.readVarInt();
            if (lines.size() < MAX_LINES) {
                lines.add(new Line(sourceEntityId, targetEntityId, from, to, primary, ticks));
            }
        }
        return new S2C_HackclawPathLines(lines);
    }

    public static void handle(S2C_HackclawPathLines msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HackclawPathLineRenderer.setLines(msg.lines));
    }

    public record Line(int sourceEntityId, int targetEntityId, Vec3 from, Vec3 to, boolean primary, int ticks) {
    }

    private static boolean isFinite(Vec3 vec) {
        return vec != null
                && Double.isFinite(vec.x)
                && Double.isFinite(vec.y)
                && Double.isFinite(vec.z);
    }
}
