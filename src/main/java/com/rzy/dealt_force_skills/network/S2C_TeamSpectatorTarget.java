package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_TeamSpectatorTarget(boolean managed, int targetEntityId, String targetName,
                                      String characterId, ItemStack mainHand, ItemStack offHand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_TeamSpectatorTarget> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_team_spectator_target"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_TeamSpectatorTarget> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_TeamSpectatorTarget::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static S2C_TeamSpectatorTarget disabled() {
        return new S2C_TeamSpectatorTarget(false, -1, "", "", ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public static S2C_TeamSpectatorTarget managed(ServerPlayer target) {
        if (target == null) {
            return new S2C_TeamSpectatorTarget(true, -1, "", "", ItemStack.EMPTY, ItemStack.EMPTY);
        }
        return new S2C_TeamSpectatorTarget(true, target.getId(), target.getGameProfile().getName(),
                CharacterSelectionManager.getSelectedCharacterId(target).orElse(""),
                target.getMainHandItem().copy(), target.getOffhandItem().copy());
    }

    public static void encode(S2C_TeamSpectatorTarget msg, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(msg.managed);
        buf.writeVarInt(msg.targetEntityId);
        buf.writeUtf(msg.targetName);
        buf.writeUtf(msg.characterId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.mainHand);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.offHand);
    }

    public static S2C_TeamSpectatorTarget decode(RegistryFriendlyByteBuf buf) {
        return new S2C_TeamSpectatorTarget(buf.readBoolean(), buf.readVarInt(), buf.readUtf(), buf.readUtf(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public static void handle(S2C_TeamSpectatorTarget msg, IPayloadContext ctx) {
        ctx.enqueueWork(() ->                 ClientTeamSpectatorState.sync(msg.managed, msg.targetEntityId, msg.targetName,
                        msg.characterId, msg.mainHand, msg.offHand));
    }
}
