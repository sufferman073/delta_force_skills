package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class S2C_CommanderSnapshot implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2C_CommanderSnapshot> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_commander_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, S2C_CommanderSnapshot> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> encode(msg, buf),
                    S2C_CommanderSnapshot::decode
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final long tacticalPoints;
    private final String[] sources;
    private final String[] enabledPassives;
    private final int[] upgrades;
    private final int[] cooldowns;
    private final boolean[] branchUpgrades;

    public S2C_CommanderSnapshot(long tacticalPoints, String[] sources, String[] enabledPassives,
                                  int[] upgrades, int[] cooldowns, boolean[] branchUpgrades) {
        this.tacticalPoints = tacticalPoints;
        this.sources = sources;
        this.enabledPassives = enabledPassives;
        this.upgrades = upgrades;
        this.cooldowns = cooldowns;
        this.branchUpgrades = branchUpgrades;
    }

    public static S2C_CommanderSnapshot from(ServerPlayer player) {
        CommanderSkillChoice[] choices = CommanderSkillChoice.values();
        String[] sources = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            sources[i] = CommanderStateManager.selectedSource(player, choices[i]).orElse("");
        }
        CommanderUpgrade[] upgradeValues = CommanderUpgrade.values();
        int[] upgrades = new int[upgradeValues.length];
        for (int i = 0; i < upgradeValues.length; i++) {
            upgrades[i] = CommanderStateManager.upgradeLevel(player, upgradeValues[i]);
        }
        CommanderSupport[] supportValues = CommanderSupport.values();
        int[] cooldowns = new int[supportValues.length];
        for (int i = 0; i < supportValues.length; i++) {
            cooldowns[i] = CommanderStateManager.supportCooldownTicks(player, supportValues[i]);
        }
        CommanderBranchUpgrade[] branchValues = CommanderBranchUpgrade.values();
        boolean[] branchUpgrades = new boolean[branchValues.length];
        for (int i = 0; i < branchValues.length; i++) {
            branchUpgrades[i] = CommanderStateManager.ownsBranchUpgrade(player, branchValues[i]);
        }
        String[] enabledPassives = CommanderStateManager.enabledPassives(player).toArray(String[]::new);
        return new S2C_CommanderSnapshot(CommanderStateManager.tacticalPoints(player), sources, enabledPassives,
                upgrades, cooldowns, branchUpgrades);
    }

    public static void encode(S2C_CommanderSnapshot msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.tacticalPoints);
        buf.writeVarInt(msg.sources.length);
        for (String source : msg.sources) {
            buf.writeUtf(source, 256);
        }
        buf.writeVarInt(msg.enabledPassives.length);
        for (String passive : msg.enabledPassives) {
            buf.writeUtf(passive, 256);
        }
        buf.writeVarInt(msg.upgrades.length);
        for (int level : msg.upgrades) {
            buf.writeVarInt(level);
        }
        buf.writeVarInt(msg.cooldowns.length);
        for (int cooldown : msg.cooldowns) {
            buf.writeVarInt(cooldown);
        }
        buf.writeVarInt(msg.branchUpgrades.length);
        for (boolean owned : msg.branchUpgrades) {
            buf.writeBoolean(owned);
        }
    }

    public static S2C_CommanderSnapshot decode(FriendlyByteBuf buf) {
        long points = buf.readVarLong();
        String[] sources = new String[Math.min(32, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = buf.readUtf(256);
        }
        String[] enabledPassives = new String[Math.min(64, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < enabledPassives.length; i++) {
            enabledPassives[i] = buf.readUtf(256);
        }
        int[] upgrades = new int[Math.min(16, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < upgrades.length; i++) {
            upgrades[i] = buf.readVarInt();
        }
        int[] cooldowns = new int[Math.min(32, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = buf.readVarInt();
        }
        boolean[] branchUpgrades = new boolean[Math.min(128, Math.max(0, buf.readVarInt()))];
        for (int i = 0; i < branchUpgrades.length; i++) {
            branchUpgrades[i] = buf.readBoolean();
        }
        return new S2C_CommanderSnapshot(points, sources, enabledPassives, upgrades, cooldowns, branchUpgrades);
    }

    public static void handle(S2C_CommanderSnapshot msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientCommanderState.sync(msg.tacticalPoints, msg.sources, msg.enabledPassives,
                msg.upgrades, msg.cooldowns, msg.branchUpgrades));
    }
}
