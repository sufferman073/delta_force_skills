package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.vyron.VyronSkills;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.entity.UluruLoiteringMissileEntity;
import com.rzy.dealt_force_skills.registry.ModEffects;
import com.rzy.dealt_force_skills.team.RoundStartFreezeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_VyronDash implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_VyronDash> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_vyron_dash"));

    public static final StreamCodec<FriendlyByteBuf, C2S_VyronDash> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_VyronDash::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final double dirX;
    private final double dirZ;

    public C2S_VyronDash(double dirX, double dirZ) {
        this.dirX = dirX;
        this.dirZ = dirZ;
    }

    public static void encode(C2S_VyronDash msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.dirX);
        buf.writeDouble(msg.dirZ);
    }

    public static C2S_VyronDash decode(FriendlyByteBuf buf) {
        return new C2S_VyronDash(buf.readDouble(), buf.readDouble());
    }

    public static void handle(C2S_VyronDash msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (com.rzy.dealt_force_skills.climb.HvkZiplineManager.blocksSkills(player)) return;
            if (player == null || player.isSpectator() || UluruLoiteringMissileEntity.isPlayerControlling(player)) {
                return;
            }
            if (player.hasEffect(ModEffects.STUN) || player.hasEffect(ModEffects.WEBBED) || StingerStateManager.isDowned(player) || RoundStartFreezeManager.isFrozen(player)) {
                return;
            }
            VyronSkills.tryDash(player, new Vec3(msg.dirX, 0.0D, msg.dirZ));
            VyronStateManager.syncToClient(player);
        });
    }
}
