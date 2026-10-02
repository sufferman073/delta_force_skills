package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.team.DealtTeamManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class PositionRevealHelper {
    private PositionRevealHelper() {
    }

    public static boolean isValidReconTarget(ServerPlayer caster, LivingEntity target) {
        if (target == null || !target.isAlive() || caster == null) {
            return false;
        }
        if (target == caster || target.getUUID().equals(caster.getUUID())) {
            return false;
        }
        return !(target instanceof ServerPlayer teammateCandidate
                && DealtTeamManager.areTeammates(caster, teammateCandidate));
    }

    public static List<LivingEntity> filterReconTargets(ServerPlayer caster, Collection<? extends LivingEntity> targets) {
        List<LivingEntity> filtered = new ArrayList<>();
        for (LivingEntity target : targets) {
            if (isValidReconTarget(caster, target)) {
                filtered.add(target);
            }
        }
        return filtered;
    }

    public static void sendToCasterAndTeammates(ServerPlayer caster, CustomPacketPayload packet) {
        if (caster == null || packet == null) {
            return;
        }
        NetworkHandler.sendToPlayer(packet, caster);
        for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(caster)) {
            NetworkHandler.sendToPlayer(packet, teammate);
        }
    }

    public static void sendToCasterAndTeammates(ServerPlayer caster, Function<ServerPlayer, CustomPacketPayload> packetFactory) {
        if (caster == null || packetFactory == null) {
            return;
        }
        CustomPacketPayload packet = packetFactory.apply(caster);
        if (packet != null) {
            NetworkHandler.sendToPlayer(packet, caster);
        }
        for (ServerPlayer teammate : DealtTeamManager.onlineTeammates(caster)) {
            CustomPacketPayload teammatePacket = packetFactory.apply(teammate);
            if (teammatePacket != null) {
                NetworkHandler.sendToPlayer(teammatePacket, teammate);
            }
        }
    }
}