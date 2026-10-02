package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_ToggleCommanderPassive implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ToggleCommanderPassive> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_toggle_commander_passive"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ToggleCommanderPassive> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ToggleCommanderPassive::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String characterId;
    private final boolean enabled;

    public C2S_ToggleCommanderPassive(String characterId, boolean enabled) {
        this.characterId = characterId;
        this.enabled = enabled;
    }

    public static void encode(C2S_ToggleCommanderPassive msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.characterId, 256);
        buf.writeBoolean(msg.enabled);
    }

    public static C2S_ToggleCommanderPassive decode(FriendlyByteBuf buf) {
        return new C2S_ToggleCommanderPassive(buf.readUtf(256), buf.readBoolean());
    }

    public static void handle(C2S_ToggleCommanderPassive msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.setPassiveEnabled(player, msg.characterId, msg.enabled);
            }
        });
    }
}