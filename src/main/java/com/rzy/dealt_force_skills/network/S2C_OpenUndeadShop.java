package com.rzy.dealt_force_skills.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record S2C_OpenUndeadShop(long souls, int professionOrdinal, CompoundTag upgrades) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_OpenUndeadShop> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_undead_shop"));

    public static final StreamCodec<FriendlyByteBuf, S2C_OpenUndeadShop> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_OpenUndeadShop::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public S2C_OpenUndeadShop {
        souls = Math.max(0L, souls);
        upgrades = upgrades == null ? new CompoundTag() : upgrades.copy();
    }

    public static void encode(S2C_OpenUndeadShop msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.souls);
        buf.writeVarInt(msg.professionOrdinal);
        buf.writeNbt(msg.upgrades);
    }

    public static S2C_OpenUndeadShop decode(FriendlyByteBuf buf) {
        CompoundTag upgrades;
        long souls = buf.readLong();
        int profession = buf.readVarInt();
        upgrades = buf.readNbt();
        return new S2C_OpenUndeadShop(souls, profession, upgrades);
    }

    public static void handle(S2C_OpenUndeadShop msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> UndeadShopScreen.open(msg.souls, msg.professionOrdinal, msg.upgrades));
    }
}
