package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncPlayerCharacterSkin implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncPlayerCharacterSkin> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_player_character_skin"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncPlayerCharacterSkin> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncPlayerCharacterSkin::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int entityId;
    private final String characterId;
    private final boolean skinsEnabled;

    public S2C_SyncPlayerCharacterSkin(int entityId, String characterId) {
        this(entityId, characterId, true);
    }

    public S2C_SyncPlayerCharacterSkin(int entityId, String characterId, boolean skinsEnabled) {
        this.entityId = entityId;
        this.characterId = characterId == null ? "" : characterId;
        this.skinsEnabled = skinsEnabled;
    }

    public static void encode(S2C_SyncPlayerCharacterSkin msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.characterId);
        buf.writeBoolean(msg.skinsEnabled);
    }

    public static S2C_SyncPlayerCharacterSkin decode(FriendlyByteBuf buf) {
        return new S2C_SyncPlayerCharacterSkin(buf.readVarInt(), buf.readUtf(256), buf.readBoolean());
    }

    public static void handle(S2C_SyncPlayerCharacterSkin msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCharacterSkinState.sync(msg.entityId, msg.characterId, msg.skinsEnabled));
    }
}
