package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_SyncSelectedCharacter(String characterId, boolean normalPlayer) implements CustomPacketPayload {

    public S2C_SyncSelectedCharacter {
        characterId = characterId == null ? "" : characterId;
        normalPlayer = normalPlayer && characterId.isBlank();
    }

    public S2C_SyncSelectedCharacter(String characterId) {
        this(characterId, false);
    }

    public static final CustomPacketPayload.Type<S2C_SyncSelectedCharacter> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_selected_character"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncSelectedCharacter> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeUtf(msg.characterId);
                        buf.writeBoolean(msg.normalPlayer);
                    },
                    buf -> new S2C_SyncSelectedCharacter(buf.readUtf(256), buf.readBoolean())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2C_SyncSelectedCharacter msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCharacterSelectionState.syncSelectedCharacter(msg.characterId, msg.normalPlayer));
    }
}
