package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncHaffCoins implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncHaffCoins> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_haff_coins"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncHaffCoins> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncHaffCoins::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final long coins;

    public S2C_SyncHaffCoins(long coins) {
        this.coins = Math.max(0L, coins);
    }

    public static void encode(S2C_SyncHaffCoins msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.coins);
    }

    public static S2C_SyncHaffCoins decode(FriendlyByteBuf buf) {
        return new S2C_SyncHaffCoins(buf.readLong());
    }

    public static void handle(S2C_SyncHaffCoins msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
                    HaffShopScreen.updateCoins(msg.coins);
                    GhrothArmoryScreen.updateCoins(msg.coins);
                });
    }
}
