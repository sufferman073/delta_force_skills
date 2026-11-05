package com.rzy.dealt_force_skills.mixin;

import com.rzy.dealt_force_skills.department.DepartmentActions;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DepartmentConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private Vec3 awaitingPositionFromClient;
    @Unique private Vec3 dfs$departmentMoveStart;
    @Unique private boolean dfs$departmentSprint, dfs$departmentSneak;

    @Inject(method="handleMovePlayer",at=@At("HEAD"))
    private void dfs$beforeMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        dfs$departmentMoveStart = awaitingPositionFromClient == null ? player.position() : null;
    }
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void dfs$afterMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        Vec3 before = dfs$departmentMoveStart;
        dfs$departmentMoveStart = null;
        if (before != null && awaitingPositionFromClient == null) DepartmentActions.moved(player,before);
    }
    @Inject(method="handlePlayerCommand",at=@At("HEAD"))
    private void dfs$beforeCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        dfs$departmentSprint = player.isSprinting();
        dfs$departmentSneak = player.isShiftKeyDown();
    }
    @Inject(method="handlePlayerCommand",at=@At("RETURN"))
    private void dfs$afterCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (dfs$departmentSprint != player.isSprinting()) DepartmentActions.action(player,"sprint",0);
        if (dfs$departmentSneak != player.isShiftKeyDown()) DepartmentActions.action(player,"sneak",0);
    }
}
