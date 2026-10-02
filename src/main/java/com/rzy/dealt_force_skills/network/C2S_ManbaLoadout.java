package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class C2S_ManbaLoadout implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2S_ManbaLoadout> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "c2s_manba_loadout"));

    public static final StreamCodec<FriendlyByteBuf, C2S_ManbaLoadout> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    C2S_ManbaLoadout::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int[] talents;
    private final int[] bulbs;
    private final int lens;
    private final int[] batteries;

    public C2S_ManbaLoadout(int[] talents, int[] bulbs, int lens, int[] batteries) {
        this.talents = talents == null ? new int[0] : talents;
        this.bulbs = bulbs == null ? new int[0] : bulbs;
        this.lens = lens;
        this.batteries = batteries == null ? new int[0] : batteries;
    }

    public static void encode(C2S_ManbaLoadout msg, FriendlyByteBuf buf) {
        writeArray(buf, msg.talents);
        writeArray(buf, msg.bulbs);
        buf.writeVarInt(msg.lens);
        writeArray(buf, msg.batteries);
    }

    public static C2S_ManbaLoadout decode(FriendlyByteBuf buf) {
        int[] talents = readArray(buf, 12);
        int[] bulbs = readArray(buf, 5);
        int lens = buf.readVarInt();
        int[] batteries = readArray(buf, 3);
        return new C2S_ManbaLoadout(talents, bulbs, lens, batteries);
    }

    public static void handle(C2S_ManbaLoadout msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                ManbaStateManager.configure(player, msg.talents, msg.bulbs, msg.lens, msg.batteries);
            }
        });
    }

    private static void writeArray(FriendlyByteBuf buf, int[] values) {
        buf.writeVarInt(values.length);
        for (int value : values) {
            buf.writeVarInt(value);
        }
    }

    private static int[] readArray(FriendlyByteBuf buf, int maxLength) {
        int encodedLength = Math.max(0, buf.readVarInt());
        int length = Math.min(maxLength, encodedLength);
        int[] values = new int[length];
        for (int i = 0; i < encodedLength; i++) {
            int value = buf.readVarInt();
            if (i < length) {
                values[i] = value;
            }
        }
        return values;
    }
}
