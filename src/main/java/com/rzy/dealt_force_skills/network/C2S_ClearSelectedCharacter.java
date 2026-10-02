package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2S_ClearSelectedCharacter() implements CustomPacketPayload {
    public static final Type<C2S_ClearSelectedCharacter> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_clear_selected_character"));
    public static final StreamCodec<FriendlyByteBuf, C2S_ClearSelectedCharacter> STREAM_CODEC =
            StreamCodec.unit(new C2S_ClearSelectedCharacter());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2S_ClearSelectedCharacter message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator()) {
                CharacterSelectionManager.clearSelectedCharacter(player, false);
                player.displayClientMessage(Component.translatable("message.dealt_force_skills.character_cleared"), true);
            }
        });
    }
}
