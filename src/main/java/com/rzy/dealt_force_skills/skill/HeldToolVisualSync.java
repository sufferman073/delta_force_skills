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
import com.rzy.dealt_force_skills.character.CharacterSelectionManager;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.dwolf.DWolfStateManager;
import com.rzy.dealt_force_skills.character.luna.LunaSkills;
import com.rzy.dealt_force_skills.character.saeed.SaeedStateManager;
import com.rzy.dealt_force_skills.character.sineva.SinevaSkills;
import com.rzy.dealt_force_skills.character.sineva.SinevaStateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class HeldToolVisualSync {
    private static final java.util.Map<ServerPlayer, Integer> WHISTLE_UNTIL = new java.util.WeakHashMap<>();

    public static void showRoverWhistle(ServerPlayer player) {
        WHISTLE_UNTIL.put(player, player.tickCount + 10);
        sync(player);
    }

    public static final int PREP_NONE = 0, PREP_SMOKE = 1, PREP_WIRE = 2, CANCEL_DRAW = 3;
    private record Prep(int kind, int expires, ServerLevel level) {}
    private static final java.util.Map<ServerPlayer, Prep> PREP = new java.util.WeakHashMap<>();

    private static final java.util.Map<ServerPlayer, Integer> PENDING = new java.util.WeakHashMap<>();
    private static final java.util.Set<ServerPlayer> DIRTY = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void queueInput(ServerPlayer player, int kind) {
        if (kind == CANCEL_DRAW) LunaSkills.endDraw(player);
        else if (kind >= PREP_NONE && kind <= PREP_WIRE) PENDING.put(player, kind);
    }

    public static void clearTransient(ServerPlayer player) {
        PENDING.remove(player);
        PREP.remove(player);
        LunaSkills.endDraw(player);
        SinevaSkills.hidePreparingGrapple(player);
    }

    public static void changed(ServerPlayer player) {
        DIRTY.add(player);
    }

    public static void tick(ServerPlayer player) {
        Integer input = PENDING.remove(player);
        if (input != null) acceptPrep(player, input);
        if (DIRTY.remove(player) || player.tickCount % 5 == 0) sync(player);
    }

    private static void acceptPrep(ServerPlayer player, int kind) {
        Prep before = PREP.get(player);
        if (kind == PREP_NONE) {
            PREP.remove(player);
        } else if (player.isAlive() && !player.isSpectator() && ownsPrep(player, kind)) {
            PREP.put(player, new Prep(kind, player.tickCount + 80, player.serverLevel()));
        }
        Prep after = PREP.get(player);
        if ((before == null ? PREP_NONE : before.kind()) != (after == null ? PREP_NONE : after.kind())) changed(player);
    }

    private static boolean ownsPrep(ServerPlayer player, int kind) {
        if (kind != PREP_SMOKE && kind != PREP_WIRE) return false;
        String role = kind == PREP_SMOKE ? ModCharacters.D_WOLF_ID : ModCharacters.SINEVA_ID;
        return CharacterSelectionManager.getSelectedCharacterId(player).filter(role::equals).isPresent()
                || CommanderStateManager.isCommander(player) && CommanderStateManager.sourceForSkill(player,
                kind == PREP_SMOKE ? SkillSlot.ACTIVE_2 : SkillSlot.ACTIVE_1, false).filter(role::equals).isPresent();
    }

    private static int prep(ServerPlayer player) {
        Prep state = PREP.get(player);
        if (state == null) return PREP_NONE;
        if (state.level() == player.serverLevel() && player.tickCount < state.expires() && ownsPrep(player, state.kind())) return state.kind();
        PREP.remove(player);
        return PREP_NONE;
    }

    private HeldToolVisualSync() {
    }

    public static void sync(ServerPlayer player) {
        NetworkHandler.sendToTrackingAndSelf(packet(player), player);
    }

    public static void syncOneTo(ServerPlayer target, ServerPlayer watcher) {
        NetworkHandler.sendToPlayer(packet(target), watcher);
    }

    private static S2C_SyncHeldToolVisual packet(ServerPlayer player) {
        HeldToolVisual visual = current(player);
        int elapsed = -1, duration = 0;
        if (visual == HeldToolVisual.MORSE_SONAR_DETECTOR) {
            elapsed = MorseStateManager.visualDeployTicks(player);
            duration = Math.max(1, MorseStateManager.SONAR_DEPLOY_TICKS);
        } else if (visual == HeldToolVisual.LUNA_SHOCK_BOW || visual == HeldToolVisual.LUNA_RECON_BOW) {
            elapsed = LunaSkills.drawAge(player, visual == HeldToolVisual.LUNA_RECON_BOW ? LunaTool.RECON_BOW : LunaTool.SHOCK_BOW);
        }
        return new S2C_SyncHeldToolVisual(player.getId(), visual.ordinal(), elapsed, duration);
    }

    private static HeldToolVisual current(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            clearTransient(player);
            return HeldToolVisual.NONE;
        }
        int preparing = prep(player);
        if (preparing == PREP_SMOKE) return HeldToolVisual.D_WOLF_SMOKE;
        if (DWolfStateManager.isDWolf(player) && DWolfStateManager.handCannonVisible(player)) return HeldToolVisual.D_WOLF_HAND_CANNON;
        if (SinevaStateManager.isSineva(player) && SinevaSkills.grapplePreparing(player)) return HeldToolVisual.SINEVA_HOOK;
        if (preparing == PREP_WIRE) return HeldToolVisual.SINEVA_WIRE;
        if (SaeedStateManager.isSaeed(player)) return SaeedStateManager.crossbowVisual(player);
        Integer until = WHISTLE_UNTIL.get(player);
        if (until != null) {
            if (player.isAlive() && player.tickCount < until && com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(player)) {
                return HeldToolVisual.ROVER_DOG_WHISTLE;
            }
            WHISTLE_UNTIL.remove(player);
        }
        if (com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.isWildfire(player)) return com.rzy.dealt_force_skills.character.wildfire.WildfireStateManager.heldVisual(player);
        if (com.rzy.dealt_force_skills.character.rover.RoverStateManager.isRover(player)
                && com.rzy.dealt_force_skills.character.rover.RoverStateManager.data(player).getInt("Tool") > 0) {
            return switch (com.rzy.dealt_force_skills.character.rover.RoverStateManager.data(player).getInt("Tool")) {
                case 1 -> HeldToolVisual.ROVER_IRRITANT_SMOKE;
                case 2 -> HeldToolVisual.ROVER_AEROSOL_INJECTOR;
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
            return switch (LunaStateManager.equippedTool(player)) {
                case COMPOSITE_GRENADE -> HeldToolVisual.LUNA_COMPOSITE_GRENADE;
                case SHOCK_BOW -> HeldToolVisual.LUNA_SHOCK_BOW;
                case RECON_BOW -> HeldToolVisual.LUNA_RECON_BOW;
                case NONE -> HeldToolVisual.NONE;
            };
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
