package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_UseCharacterSkill implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_UseCharacterSkill> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_use_character_skill"));

    public static final StreamCodec<FriendlyByteBuf, C2S_UseCharacterSkill> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_UseCharacterSkill::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final SkillSlot slot;
    private final boolean alternate;

    public C2S_UseCharacterSkill(SkillSlot slot) {
        this(slot, false);
    }

    public C2S_UseCharacterSkill(SkillSlot slot, boolean alternate) {
        this.slot = slot;
        this.alternate = alternate;
    }

    public static void encode(C2S_UseCharacterSkill msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot.ordinal());
        buf.writeBoolean(msg.alternate);
    }

    public static C2S_UseCharacterSkill decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        SkillSlot[] values = SkillSlot.values();
        SkillSlot slot = ordinal >= 0 && ordinal < values.length ? values[ordinal] : SkillSlot.PASSIVE;
        boolean alternate = buf.readBoolean();
        return new C2S_UseCharacterSkill(slot, alternate);
    }

    public static void handle(C2S_UseCharacterSkill msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player.isSpectator()) return;

            SkillDispatcher.useSkill(player, msg.slot, msg.alternate);
        });
    }
}
