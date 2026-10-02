package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class S2C_CommanderVehicleState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_CommanderVehicleState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_commander_vehicle_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_CommanderVehicleState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_CommanderVehicleState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final UUID entityUuid;
    private final boolean active;

    public S2C_CommanderVehicleState(int entityId, UUID entityUuid, boolean active) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.active = active;
    }

    public static void encode(S2C_CommanderVehicleState message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.entityUuid);
        buffer.writeBoolean(message.active);
    }

    public static S2C_CommanderVehicleState decode(FriendlyByteBuf buffer) {
        return new S2C_CommanderVehicleState(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(S2C_CommanderVehicleState message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCommanderVehicleState.sync(
                message.entityId, message.entityUuid, message.active));
    }
}
