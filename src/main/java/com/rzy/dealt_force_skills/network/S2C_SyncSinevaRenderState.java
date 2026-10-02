package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncSinevaRenderState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncSinevaRenderState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_sineva_render_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncSinevaRenderState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncSinevaRenderState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final boolean sineva;
    private final boolean bombSuitActive;
    private final boolean shieldDeployed;
    private final boolean viewportBroken;

    public S2C_SyncSinevaRenderState(int entityId, boolean sineva, boolean bombSuitActive, boolean shieldDeployed, boolean viewportBroken) {
        this.entityId = entityId;
        this.sineva = sineva;
        this.bombSuitActive = bombSuitActive;
        this.shieldDeployed = shieldDeployed;
        this.viewportBroken = viewportBroken;
    }

    public static void encode(S2C_SyncSinevaRenderState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.sineva);
        buf.writeBoolean(msg.bombSuitActive);
        buf.writeBoolean(msg.shieldDeployed);
        buf.writeBoolean(msg.viewportBroken);
    }

    public static S2C_SyncSinevaRenderState decode(FriendlyByteBuf buf) {
        return new S2C_SyncSinevaRenderState(
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(S2C_SyncSinevaRenderState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSinevaRenderState.sync(
                        msg.entityId,
                        msg.sineva,
                        msg.bombSuitActive,
                        msg.shieldDeployed,
                        msg.viewportBroken
                ));
    }
}
