package com.rzy.dealt_force_skills.skill;

import com.rzy.dealt_force_skills.character.department.DepartmentOfTransportationStateManager;
import com.rzy.dealt_force_skills.character.department.DepartmentTool;
import com.rzy.dealt_force_skills.character.chamber.ChamberStateManager;
import com.rzy.dealt_force_skills.character.chamber.ChamberTool;
import com.rzy.dealt_force_skills.character.gizmo.GizmoStateManager;
import com.rzy.dealt_force_skills.character.gizmo.GizmoTool;
import com.rzy.dealt_force_skills.character.ntwo.NTwoStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawStateManager;
import com.rzy.dealt_force_skills.character.hackclaw.HackclawTool;
import com.rzy.dealt_force_skills.character.luna.LunaStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaTool;
import com.rzy.dealt_force_skills.character.manba.ManbaStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseStateManager;
import com.rzy.dealt_force_skills.character.morse.MorseTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroStateManager;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroWitchificationStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxStateManager;
import com.rzy.dealt_force_skills.character.nox.NoxTool;
import com.rzy.dealt_force_skills.character.raptor.RaptorStateManager;
import com.rzy.dealt_force_skills.character.raptor.RaptorTool;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager;
import com.rzy.dealt_force_skills.character.shepherd.ShepherdTool;
import com.rzy.dealt_force_skills.character.stinger.StingerStateManager;
import com.rzy.dealt_force_skills.character.stinger.StingerTool;
import com.rzy.dealt_force_skills.character.tempest.TempestStateManager;
import com.rzy.dealt_force_skills.character.tempest.TempestTool;
import com.rzy.dealt_force_skills.character.toxik.ToxikStateManager;
import com.rzy.dealt_force_skills.character.toxik.ToxikTool;
import com.rzy.dealt_force_skills.character.uluru.UluruStateManager;
import com.rzy.dealt_force_skills.character.uluru.UluruTool;
import com.rzy.dealt_force_skills.character.vlinder.VlinderStateManager;
import com.rzy.dealt_force_skills.character.vlinder.VlinderTool;
import com.rzy.dealt_force_skills.character.vyron.VyronStateManager;
import com.rzy.dealt_force_skills.character.vyron.VyronTool;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.network.S2C_SyncHeldToolVisual;
import net.minecraft.server.level.ServerPlayer;

public final class HeldToolVisualSync {
    private HeldToolVisualSync() {
    }

    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingAndSelf(packet(player), player);
    }

    public static void syncOneTo(ServerPlayer target, ServerPlayer watcher) {
        NetworkHandler.sendToPlayer(packet(target), watcher);
    }

    private static S2C_SyncHeldToolVisual packet(ServerPlayer player) {
        return new S2C_SyncHeldToolVisual(player.getId(), current(player).ordinal());
    }

    private static HeldToolVisual current(ServerPlayer player) {
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(player)
                && com.rzy.dealt_force_skills.character.rover.RoverStateManager.data(player).getInt("Tool") > 0) {
            return switch (com.rzy.dealt_force_skills.character.rover.RoverStateManager.data(player).getInt("Tool")) {
                case 1 -> HeldToolVisual.STINGER_SMOKE_GRENADE;
                case 2 -> HeldToolVisual.STINGER_STIM_GUN;
                default -> HeldToolVisual.NONE;
            };
        }

        if (ManbaStateManager.isManba(player) && ManbaStateManager.isFlashlightActive(player)) {
            return HeldToolVisual.MANBA_FLASHLIGHT;
        }
        if (NikaidouHiroStateManager.isNikaidouHiro(player)) {
            return switch (NikaidouHiroStateManager.equippedTool(player)) {
                case HOT_IRON -> HeldToolVisual.NIKAIDOU_HOT_IRON;
                case RITUAL_SWORD -> HeldToolVisual.NIKAIDOU_RITUAL_SWORD;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (NikaidouHiroWitchificationStateManager.isNikaidouHiroWitchification(player)) {
            return NikaidouHiroWitchificationStateManager.equippedTool(player) == NikaidouHiroTool.RITUAL_SWORD
                    ? HeldToolVisual.NIKAIDOU_RITUAL_SWORD
                    : HeldToolVisual.NONE;
        }
        if (GizmoStateManager.isGizmo(player)) {
            return switch (GizmoStateManager.equippedTool(player)) {
                case SMOKE_TRAP -> HeldToolVisual.GIZMO_SMOKE_TRAP;
                case SPIDER_NEST -> HeldToolVisual.GIZMO_SPIDER_NEST;
                case T_BOY -> HeldToolVisual.GIZMO_T_BOY;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (ChamberStateManager.isChamber(player)) {
            return switch (ChamberStateManager.equippedTool(player)) {
                case TELEPORT_CARD -> HeldToolVisual.CHAMBER_TELEPORT_CARD;
                case TRAP_CARD -> HeldToolVisual.CHAMBER_TRAP_CARD;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (NTwoStateManager.isNTwo(player)) {
            return switch (NTwoStateManager.equippedTool(player)) {
                case DEWAR_CANISTER -> HeldToolVisual.N_TWO_DEWAR_CANISTER;
                case CONDENSER_LAUNCHER -> HeldToolVisual.N_TWO_CONDENSER_LAUNCHER;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (MorseStateManager.isMorse(player)) {
            return switch (MorseStateManager.equippedTool(player)) {
                case SHOCK_ORB -> HeldToolVisual.MORSE_SHOCK_ORB;
                case FLASH_GRENADE -> HeldToolVisual.MORSE_FLASH_GRENADE;
                case SONAR_DETECTOR -> HeldToolVisual.MORSE_SONAR_DETECTOR;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (ShepherdStateManager.isShepherd(player)) {
            return switch (ShepherdStateManager.equippedTool(player)) {
                case SONIC_TRAP -> HeldToolVisual.SHEPHERD_SONIC_TRAP;
                case FRAG_GRENADE -> HeldToolVisual.SHEPHERD_GRENADE;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (RaptorStateManager.isRaptor(player)) {
            return switch (RaptorStateManager.equippedTool(player)) {
                case FALCON_DRONE -> HeldToolVisual.RAPTOR_FALCON_DRONE;
                case PULSE_GRENADE -> HeldToolVisual.RAPTOR_PULSE_GRENADE;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (NoxStateManager.isNox(player)) {
            return switch (NoxStateManager.equippedTool(player)) {
                case ROTOR -> HeldToolVisual.NOX_ROTOR;
                case FLASH_GRENADE -> HeldToolVisual.NOX_FLASH_GRENADE;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (ToxikStateManager.isToxik(player)) {
            return switch (ToxikStateManager.equippedTool(player)) {
                case TEAR_GAS -> HeldToolVisual.TOXIK_TEAR_GAS;
                case FIREFLY_SWARM -> HeldToolVisual.TOXIK_FIREFLY_SWARM;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (StingerStateManager.isStinger(player)) {
            return switch (StingerStateManager.equippedTool(player)) {
                case SMOKE_GRENADE -> HeldToolVisual.STINGER_SMOKE_GRENADE;
                case SMOKE_DRONE -> HeldToolVisual.STINGER_SMOKE_DRONE;
                case STIM_GUN -> HeldToolVisual.STINGER_STIM_GUN;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (HackclawStateManager.isHackclaw(player)) {
            return switch (HackclawStateManager.equippedTool(player)) {
                case HACKING_KNIFE -> HeldToolVisual.HACKCLAW_HACKING_KNIFE;
                case FLASH_DRONE -> HeldToolVisual.HACKCLAW_FLASH_DRONE;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (TempestStateManager.isTempest(player)) {
            return TempestStateManager.equippedTool(player) == TempestTool.WALL_DRILL_STINGER
                    ? HeldToolVisual.TEMPEST_WALL_DRILL_STINGER
                    : HeldToolVisual.NONE;
        }
        if (LunaStateManager.isLuna(player)) {
            return LunaStateManager.equippedTool(player) == LunaTool.COMPOSITE_GRENADE
                    ? HeldToolVisual.LUNA_COMPOSITE_GRENADE
                    : HeldToolVisual.NONE;
        }
        if (UluruStateManager.isUluru(player)) {
            return switch (UluruStateManager.equippedTool(player)) {
                case INCENDIARY -> HeldToolVisual.ULURU_INCENDIARY;
                case COVER -> HeldToolVisual.ULURU_COVER;
                case MISSILE -> HeldToolVisual.ULURU_MISSILE;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (VlinderStateManager.isVlinder(player)) {
            return VlinderStateManager.equippedTool(player) == VlinderTool.MEDICAL_DRONE
                    ? HeldToolVisual.VLINDER_MEDICAL_DRONE
                    : HeldToolVisual.NONE;
        }
        if (VyronStateManager.isVyron(player)) {
            return switch (VyronStateManager.equippedTool(player)) {
                case MAGNETIC_BOMB -> HeldToolVisual.VYRON_MAGNETIC_BOMB;
                case TIGER_CANNON -> HeldToolVisual.VYRON_TIGER_CANNON;
                case NONE -> HeldToolVisual.NONE;
            };
        }
        if (DepartmentOfTransportationStateManager.isDepartment(player)) {
            return DepartmentOfTransportationStateManager.equippedTool(player) == DepartmentTool.EXPLOSIVE_TRAP
                    ? HeldToolVisual.DEPARTMENT_EXPLOSIVE_TRAP
                    : HeldToolVisual.NONE;
        }
        return HeldToolVisual.NONE;
    }
}
