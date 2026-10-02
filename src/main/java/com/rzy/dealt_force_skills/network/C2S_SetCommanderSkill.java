package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_SetCommanderSkill implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_SetCommanderSkill> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_set_commander_skill"));

    public static final StreamCodec<FriendlyByteBuf, C2S_SetCommanderSkill> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_SetCommanderSkill::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CommanderSkillChoice choice;
    private final String characterId;

    public C2S_SetCommanderSkill(CommanderSkillChoice choice, String characterId) {
        this.choice = choice;
        this.characterId = characterId;
    }

    public static void encode(C2S_SetCommanderSkill msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.choice.ordinal());
        buf.writeUtf(msg.characterId, 256);
    }

    public static C2S_SetCommanderSkill decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        CommanderSkillChoice[] values = CommanderSkillChoice.values();
        CommanderSkillChoice choice = ordinal >= 0 && ordinal < values.length ? values[ordinal] : values[0];
        return new C2S_SetCommanderSkill(choice, buf.readUtf(256));
    }

    public static void handle(C2S_SetCommanderSkill msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null && CommanderStateManager.isCommander(player)) {
                CommanderStateManager.setSelectedSource(player, msg.choice, msg.characterId);
            }
        });
    }
}