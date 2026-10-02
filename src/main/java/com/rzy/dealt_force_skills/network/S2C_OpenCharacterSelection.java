package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_OpenCharacterSelection implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenCharacterSelection> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_character_selection"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenCharacterSelection> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenCharacterSelection::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final boolean allowReselection;
    private final boolean freeReselection;

    public S2C_OpenCharacterSelection() {
        this(false);
    }

    public S2C_OpenCharacterSelection(boolean allowReselection) {
        this(allowReselection, false);
    }

    public S2C_OpenCharacterSelection(boolean allowReselection, boolean freeReselection) {
        this.allowReselection = allowReselection;
        this.freeReselection = freeReselection;
    }

    public static void encode(S2C_OpenCharacterSelection msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.allowReselection);
        buf.writeBoolean(msg.freeReselection);
    }

    public static S2C_OpenCharacterSelection decode(FriendlyByteBuf buf) {
        return new S2C_OpenCharacterSelection(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(S2C_OpenCharacterSelection msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    if (msg.allowReselection) {
                        ClientCharacterSelectionState.openReselectionScreen(msg.freeReselection);
                    } else {
                        ClientCharacterSelectionState.openSelectionScreen();
                    }
                });
    }
}
