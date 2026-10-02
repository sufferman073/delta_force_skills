package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.raptor.RaptorFalconControlAction;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.RaptorFalconDroneEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_RaptorFalconControl implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_RaptorFalconControl> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_raptor_falcon_control"));

    public static final StreamCodec<FriendlyByteBuf, C2S_RaptorFalconControl> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_RaptorFalconControl::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final float yaw;
    private final float pitch;
    private final boolean boosting;
    private final float forward;
    private final float strafe;
    private final float vertical;
    private final RaptorFalconControlAction action;

    public C2S_RaptorFalconControl(int entityId, float yaw, float pitch, boolean boosting) {
        this(entityId, yaw, pitch, boosting, 0.0f, 0.0f, 0.0f, RaptorFalconControlAction.NONE);
    }

    public C2S_RaptorFalconControl(
            int entityId,
            float yaw,
            float pitch,
            boolean boosting,
            RaptorFalconControlAction action
    ) {
        this(entityId, yaw, pitch, boosting, 0.0f, 0.0f, 0.0f, action);
    }

    public C2S_RaptorFalconControl(
            int entityId,
            float yaw,
            float pitch,
            boolean boosting,
            float forward,
            float strafe,
            float vertical,
            RaptorFalconControlAction action
    ) {
        this.entityId = entityId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.boosting = boosting;
        this.forward = forward;
        this.strafe = strafe;
        this.vertical = vertical;
        this.action = action;
    }

    public static void encode(C2S_RaptorFalconControl msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
        buf.writeBoolean(msg.boosting);
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
        buf.writeFloat(msg.vertical);
        buf.writeVarInt(msg.action.ordinal());
    }

    public static C2S_RaptorFalconControl decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        boolean boosting = buf.readBoolean();
        float forward = buf.readFloat();
        float strafe = buf.readFloat();
        float vertical = buf.readFloat();
        int ordinal = buf.readVarInt();
        RaptorFalconControlAction[] values = RaptorFalconControlAction.values();
        RaptorFalconControlAction action = ordinal >= 0 && ordinal < values.length
                ? values[ordinal]
                : RaptorFalconControlAction.NONE;
        return new C2S_RaptorFalconControl(entityId, yaw, pitch, boosting, forward, strafe, vertical, action);
    }

    public static void handle(C2S_RaptorFalconControl msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            if (msg.action == RaptorFalconControlAction.SIGNAL_LOST) {
                RaptorFalconDroneEntity.abortPlayerControl(player, msg.entityId);
                return;
            }
            if (msg.action == RaptorFalconControlAction.EXIT) {
                RaptorFalconDroneEntity.stopPlayerControl(player);
                return;
            }
            if (player.isSpectator()) {
                RaptorFalconDroneEntity.stopPlayerControl(player);
                return;
            }
            if (player.hasEffect(ModEffects.STUN)
                    || player.hasEffect(ModEffects.WEBBED)
                    || StingerStateManager.isDowned(player)
                    || RoundStartFreezeManager.isFrozen(player)) {
                RaptorFalconDroneEntity.stopPlayerControl(player);
                return;
            }
            if (!(player.level().getEntity(msg.entityId) instanceof RaptorFalconDroneEntity drone)
                    || !drone.isOwnedBy(player)
                    || !RaptorFalconDroneEntity.isPlayerControlling(player)) {
                RaptorFalconDroneEntity.stopPlayerControl(player);
                return;
            }

            drone.applyGuidance(msg.yaw, msg.pitch, msg.boosting, msg.forward, msg.strafe, msg.vertical);
            switch (msg.action) {
                case REVEAL -> drone.revealFor(player);
                case PULSE -> {
                    if (drone.consumeFalconPulse()) {
                        drone.throwPulseFor(player);
                        RaptorStateManager.syncToClient(player);
                    } else {
                        player.displayClientMessage(Component.translatable(
                                "message.dealt_force_skills.raptor.pulse_empty"), true);
                    }
                }
                case SELF_DESTRUCT -> {
                    drone.selfDestructFor(player);
                    RaptorStateManager.syncToClient(player);
                }
                case NONE, EXIT -> {
                }
            }
        });
    }
}
