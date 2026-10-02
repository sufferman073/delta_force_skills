package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class C2S_UseCommanderInheritedSkill implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_UseCommanderInheritedSkill> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_use_commander_inherited_skill"));

    public static final StreamCodec<FriendlyByteBuf, C2S_UseCommanderInheritedSkill> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_UseCommanderInheritedSkill::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final CommanderSkillChoice choice;
    private final boolean inheritedAlternate;

    public C2S_UseCommanderInheritedSkill(CommanderSkillChoice choice, boolean inheritedAlternate) {
        this.choice = choice;
        this.inheritedAlternate = inheritedAlternate;
    }

    public static void encode(C2S_UseCommanderInheritedSkill msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.choice);
        buf.writeBoolean(msg.inheritedAlternate);
    }

    public static C2S_UseCommanderInheritedSkill decode(FriendlyByteBuf buf) {
        return new C2S_UseCommanderInheritedSkill(buf.readEnum(CommanderSkillChoice.class), buf.readBoolean());
    }

    public static void handle(C2S_UseCommanderInheritedSkill msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null || player.isSpectator()) {
                return;
            }
            SkillDispatcher.useCommanderSelectedSkill(player, msg.choice, msg.inheritedAlternate);
        });
    }
}