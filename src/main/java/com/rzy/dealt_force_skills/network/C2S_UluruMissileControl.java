package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_UluruMissileControl implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_UluruMissileControl> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_uluru_missile_control"));

    public static final StreamCodec<FriendlyByteBuf, C2S_UluruMissileControl> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_UluruMissileControl::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final float yaw;
    private final float pitch;
    private final boolean boosting;
    private final boolean release;

    public C2S_UluruMissileControl(int entityId, float yaw, float pitch, boolean boosting) {
        this(entityId, yaw, pitch, boosting, false);
    }

    public C2S_UluruMissileControl(int entityId, float yaw, float pitch, boolean boosting, boolean release) {
        this.entityId = entityId;
        this.yaw = yaw;
        this.pitch = pitch;
        this.boosting = boosting;
        this.release = release;
    }

    public static void encode(C2S_UluruMissileControl msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
        buf.writeBoolean(msg.boosting);
        buf.writeBoolean(msg.release);
    }

    public static C2S_UluruMissileControl decode(FriendlyByteBuf buf) {
        return new C2S_UluruMissileControl(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(C2S_UluruMissileControl msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }
            // Client requested release of missile control
            if (msg.release) {
                UluruLoiteringMissileEntity.abortPlayerControl(player, msg.entityId);
                return;
            }
            if (player.isSpectator()) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            if (player.level().getEntity(msg.entityId) instanceof UluruLoiteringMissileEntity missile
                    && missile.isGuided()
                    && missile.isOwnedBy(player)) {
                missile.applyGuidance(msg.yaw, msg.pitch, msg.boosting);
            }
        });
    }
}
