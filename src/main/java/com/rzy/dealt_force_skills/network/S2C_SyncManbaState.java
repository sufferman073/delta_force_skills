package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncManbaState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncManbaState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_manba_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncManbaState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncManbaState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int elbowCharges;
    private final int elbowMaxCharges;
    private final int elbowRechargeTicks;
    private final int flashlightDurability;
    private final int flashlightMaxDurability;
    private final boolean flashlightActive;
    private final int coreCooldownTicks;
    private final boolean duelActive;
    private final int duelAffection;
    private final int duelRemainingTicks;
    private final boolean configured;
    private final int[] talents;
    private final int[] talentCooldowns;
    private final int selfTaughtStacks;
    private final int[] bulbs;
    private final int lens;
    private final int[] batteries;

    public S2C_SyncManbaState(int elbowCharges, int elbowMaxCharges, int elbowRechargeTicks,
                              int flashlightDurability, int flashlightMaxDurability, boolean flashlightActive,
                              int coreCooldownTicks, boolean duelActive, int duelAffection, int duelRemainingTicks,
                              boolean configured, int[] talents, int[] talentCooldowns, int selfTaughtStacks,
                              int[] bulbs, int lens, int[] batteries) {
        this.elbowCharges = elbowCharges;
        this.elbowMaxCharges = elbowMaxCharges;
        this.elbowRechargeTicks = elbowRechargeTicks;
        this.flashlightDurability = flashlightDurability;
        this.flashlightMaxDurability = flashlightMaxDurability;
        this.flashlightActive = flashlightActive;
        this.coreCooldownTicks = coreCooldownTicks;
        this.duelActive = duelActive;
        this.duelAffection = duelAffection;
        this.duelRemainingTicks = duelRemainingTicks;
        this.configured = configured;
        this.talents = talents == null ? new int[0] : talents;
        this.talentCooldowns = talentCooldowns == null ? new int[0] : talentCooldowns;
        this.selfTaughtStacks = selfTaughtStacks;
        this.bulbs = bulbs == null ? new int[0] : bulbs;
        this.lens = lens;
        this.batteries = batteries == null ? new int[0] : batteries;
    }

    public static void encode(S2C_SyncManbaState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.elbowCharges);
        buf.writeVarInt(msg.elbowMaxCharges);
        buf.writeVarInt(msg.elbowRechargeTicks);
        buf.writeVarInt(msg.flashlightDurability);
        buf.writeVarInt(msg.flashlightMaxDurability);
        buf.writeBoolean(msg.flashlightActive);
        buf.writeVarInt(msg.coreCooldownTicks);
        buf.writeBoolean(msg.duelActive);
        buf.writeVarInt(msg.duelAffection);
        buf.writeVarInt(msg.duelRemainingTicks);
        buf.writeBoolean(msg.configured);
        writeArray(buf, msg.talents);
        writeArray(buf, msg.talentCooldowns);
        buf.writeVarInt(msg.selfTaughtStacks);
        writeArray(buf, msg.bulbs);
        buf.writeVarInt(msg.lens);
        writeArray(buf, msg.batteries);
    }

    public static S2C_SyncManbaState decode(FriendlyByteBuf buf) {
        return new S2C_SyncManbaState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                readArray(buf, 12),
                readArray(buf, 12),
                buf.readVarInt(),
                readArray(buf, 5),
                buf.readVarInt(),
                readArray(buf, 3)
        );
    }

    public static void handle(S2C_SyncManbaState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientManbaHudState.sync(
                        msg.elbowCharges,
                        msg.elbowMaxCharges,
                        msg.elbowRechargeTicks,
                        msg.flashlightDurability,
                        msg.flashlightMaxDurability,
                        msg.flashlightActive,
                        msg.coreCooldownTicks,
                        msg.duelActive,
                        msg.duelAffection,
                        msg.duelRemainingTicks,
                        msg.configured,
                        msg.talents,
                        msg.talentCooldowns,
                        msg.selfTaughtStacks,
                        msg.bulbs,
                        msg.lens,
                        msg.batteries
                ));
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
