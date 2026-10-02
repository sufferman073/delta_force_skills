package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderSupportManager;
import mccfk.DeltaForce.java.network.MarkerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_CommanderSupport implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_CommanderSupport> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_commander_support"));

    public static final StreamCodec<FriendlyByteBuf, C2S_CommanderSupport> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_CommanderSupport::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CommanderSupport support;
    private final Vec3 position;
    private final int targetId;
    private final String vehicleId;

    public C2S_CommanderSupport(CommanderSupport support, Vec3 position, int targetId, String vehicleId) {
        this.support = support;
        this.position = position;
        this.targetId = targetId;
        this.vehicleId = vehicleId;
    }

    public static void encode(C2S_CommanderSupport msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.support.ordinal());
        buf.writeDouble(msg.position.x);
        buf.writeDouble(msg.position.y);
        buf.writeDouble(msg.position.z);
        buf.writeVarInt(msg.targetId);
        buf.writeUtf(msg.vehicleId == null ? "" : msg.vehicleId, 256);
    }

    public static C2S_CommanderSupport decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderSupport[] values = CommanderSupport.values();
        CommanderSupport support = ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
        Vec3 position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new C2S_CommanderSupport(support, position, buf.readVarInt(), buf.readUtf(256));
    }

    public static void handle(C2S_CommanderSupport msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                Vec3 validated = msg.support.targeted()
                        ? MarkerManager.validatePosition(player, msg.position).orElse(null)
                        : player.position();
                CommanderSupportManager.use(player, msg.support, validated, msg.targetId, msg.vehicleId);
            }
        });
    }
}