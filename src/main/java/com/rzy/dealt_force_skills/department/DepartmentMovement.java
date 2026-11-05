package com.rzy.dealt_force_skills.department;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Measures accepted displacement, not requested velocity or client-reported progress. */
public final class DepartmentMovement {
    private DepartmentMovement() { }
    public static void moved(ServerPlayer player,Vec3 before) {
        if(!player.isAlive()||player.isSpectator()||player.isPassenger())return;
        double distance=player.position().subtract(before).horizontalDistance();
        if(!Double.isFinite(distance)||distance<=0||distance>64)return;
        DepartmentConsumables.moved(player,distance);
        if(com.rzy.dealt_force_skills.character.ghroth.GhrothStateManager.isTacticalMoving(player))
            DepartmentTasks.signal(player,"ghroth.mobility.distance",distance);
        if(com.rzy.dealt_force_skills.character.undead.UndeadStateManager.knightCharging(player))
            DepartmentTasks.signal(player,"undead.knight.charge.distance",distance);
        if(com.rzy.dealt_force_skills.character.tempest.TempestStateManager.isRolling(player))
            DepartmentTasks.signal(player,"tempest.roll.distance",distance);
        if(com.rzy.dealt_force_skills.character.vyron.VyronStateManager.isVyron(player)
                &&com.rzy.dealt_force_skills.character.vyron.VyronStateManager.dashTicks(player)>0)
            DepartmentTasks.signal(player,"vyron.thrust.distance",distance);
    }
}
