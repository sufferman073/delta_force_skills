package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.block.AdvancedWorkBlockData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_UpdateAdvancedWorkBlock implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_UpdateAdvancedWorkBlock> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    DealtForceSkillsMod.MODID, "s2c_update_advanced_work_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_UpdateAdvancedWorkBlock> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_UpdateAdvancedWorkBlock::decode
            );

    private final AdvancedWorkBlockData data;

    public S2C_UpdateAdvancedWorkBlock(AdvancedWorkBlockData data) {
        this.data = data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(S2C_UpdateAdvancedWorkBlock msg, RegistryFriendlyByteBuf buf) {
        S2C_OpenAdvancedWorkBlock.writeData(msg.data, buf);
    }

    public static S2C_UpdateAdvancedWorkBlock decode(RegistryFriendlyByteBuf buf) {
        return new S2C_UpdateAdvancedWorkBlock(S2C_OpenAdvancedWorkBlock.readData(buf));
    }

    public static void handle(S2C_UpdateAdvancedWorkBlock msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> AdvancedWorkBlockScreen.update(msg.data));
    }
}
