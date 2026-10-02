package com.rzy.dealt_force_skills.network;

import net.minecraft.network.FriendlyByteBuf;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_SyncSinevaState implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_SyncSinevaState> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_sync_sineva_state"));

    public static final StreamCodec<FriendlyByteBuf, S2C_SyncSinevaState> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_SyncSinevaState::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int bladeWireCharges;
    private final int bladeWireMaxCharges;
    private final int bladeWireRechargeTicks;
    private final int grappleCooldownTicks;
    private final int bombSuitCooldownTicks;
    private final int bombSuitEquipTicks;
    private final int bashCooldownTicks;
    private final int chargeCooldownTicks;
    private final boolean bombSuitActive;
    private final boolean shieldDeployed;
    private final int viewportHealth;
    private final int viewportMaxHealth;
    private final int shieldDurability;
    private final int shieldMaxDurability;

    public S2C_SyncSinevaState(
            int bladeWireCharges,
            int bladeWireMaxCharges,
            int bladeWireRechargeTicks,
            int grappleCooldownTicks,
            int bombSuitCooldownTicks,
            int bombSuitEquipTicks,
            int bashCooldownTicks,
            int chargeCooldownTicks,
            boolean bombSuitActive,
            boolean shieldDeployed,
            int viewportHealth,
            int viewportMaxHealth,
            int shieldDurability,
            int shieldMaxDurability
    ) {
        this.bladeWireCharges = bladeWireCharges;
        this.bladeWireMaxCharges = bladeWireMaxCharges;
        this.bladeWireRechargeTicks = bladeWireRechargeTicks;
        this.grappleCooldownTicks = grappleCooldownTicks;
        this.bombSuitCooldownTicks = bombSuitCooldownTicks;
        this.bombSuitEquipTicks = bombSuitEquipTicks;
        this.bashCooldownTicks = bashCooldownTicks;
        this.chargeCooldownTicks = chargeCooldownTicks;
        this.bombSuitActive = bombSuitActive;
        this.shieldDeployed = shieldDeployed;
        this.viewportHealth = viewportHealth;
        this.viewportMaxHealth = viewportMaxHealth;
        this.shieldDurability = shieldDurability;
        this.shieldMaxDurability = shieldMaxDurability;
    }

    public static void encode(S2C_SyncSinevaState msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.bladeWireCharges);
        buf.writeVarInt(msg.bladeWireMaxCharges);
        buf.writeVarInt(msg.bladeWireRechargeTicks);
        buf.writeVarInt(msg.grappleCooldownTicks);
        buf.writeVarInt(msg.bombSuitCooldownTicks);
        buf.writeVarInt(msg.bombSuitEquipTicks);
        buf.writeVarInt(msg.bashCooldownTicks);
        buf.writeVarInt(msg.chargeCooldownTicks);
        buf.writeBoolean(msg.bombSuitActive);
        buf.writeBoolean(msg.shieldDeployed);
        buf.writeVarInt(msg.viewportHealth);
        buf.writeVarInt(msg.viewportMaxHealth);
        buf.writeVarInt(msg.shieldDurability);
        buf.writeVarInt(msg.shieldMaxDurability);
    }

    public static S2C_SyncSinevaState decode(FriendlyByteBuf buf) {
        return new S2C_SyncSinevaState(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(S2C_SyncSinevaState msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSinevaHudState.sync(
                        msg.bladeWireCharges,
                        msg.bladeWireMaxCharges,
                        msg.bladeWireRechargeTicks,
                        msg.grappleCooldownTicks,
                        msg.bombSuitCooldownTicks,
                        msg.bombSuitEquipTicks,
                        msg.bashCooldownTicks,
                        msg.chargeCooldownTicks,
                        msg.bombSuitActive,
                        msg.shieldDeployed,
                        msg.viewportHealth,
                        msg.viewportMaxHealth,
                        msg.shieldDurability,
                        msg.shieldMaxDurability
                ));
    }
}
