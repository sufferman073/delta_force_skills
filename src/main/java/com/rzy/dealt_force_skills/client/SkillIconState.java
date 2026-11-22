package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.client.character.*;

/** Presentation only: uses the existing synchronized gameplay state. */
record SkillIconState(int charges, int rechargeTicks, int durationTicks, boolean active, boolean available) {
    static SkillIconState single(int cooldown, boolean active, int duration) {
        return new SkillIconState(cooldown > 0 && !active ? 0 : 1, cooldown, active ? duration : 0, active, active || cooldown <= 0);
    }
    static SkillIconState charged(int count, int recharge, boolean active, int duration) {
        return new SkillIconState(count, recharge, duration, active, active || count > 0);
    }
    private static SkillIconState deployed(int cooldown, com.rzy.dealt_force_skills.character.chamber.ChamberMarkerType type, boolean equipped) {
        boolean placed = ClientChamberHudState.markers().stream().anyMatch(marker -> marker.type() == type);
        return placed ? new SkillIconState(0, cooldown, 0, false, false) : single(cooldown, equipped, 0);
    }

    String timerText(String key, int feedbackDuration) {
        if (key.equals("manba.skill.flashlight"))
            return ClientManbaHudState.flashlightDurability() / 100 + "/" + Math.max(1, ClientManbaHudState.flashlightMaxDurability() / 100);
        // A lit grenade fuse is urgent even while another charge is recharging.
        if ((key.equals("luna.skill.composite_grenade") || key.equals("shepherd.skill.frag_grenade")) && durationTicks > 0)
            return String.format(java.util.Locale.ROOT, "%.1fs", durationTicks / 20.0F);
        if (key.equals("nikaidou_hiro_witchification.skill.save_everyone") && active)
            return Math.max(0, (durationTicks + 19) / 20) + "s";
        // Cooldowns are always actual remaining ticks. A resource count/length is never a duration.
        int ticks = rechargeTicks > 0 ? rechargeTicks : Math.max(durationTicks, feedbackDuration);
        if (ticks > 0) return Math.max(1, (ticks + 19) / 20) + "s";
        if (key.equals("tempest.skill.emergency_recall") && ClientTempestHudState.ropeActive())
            return Math.round(ClientTempestHudState.ropeRemaining()) + "m";
        return "";
    }

    String statusText(String key) {
        if (!key.equals("nikaidou_hiro_witchification.skill.save_everyone") || !active) return "";
        String phase = switch (ClientNikaidouHiroHudState.witchCorePhase()) {
            case 2 -> "remnant_lock";
            case 3 -> "remnant";
            case 4 -> "final_phase";
            default -> "core_active";
        };
        return net.minecraft.network.chat.Component.translatable("hud.dealt_force_skills.nikaidou_hiro_witchification." + phase).getString();
    }

    private static int fuseRemaining(boolean equipped, int cooked, int total) {
        return equipped && cooked > 0 ? Math.max(1, total - cooked) : 0;
    }

    static SkillIconState resolve(String key, int cooldown) {
        return switch (key) {
            case "catdad.skill.spine_block" -> charged(ClientCatDadHudState.blockCharges(), ClientCatDadHudState.blockRechargeTicks(), ClientCatDadHudState.blockWindowTicks()>0, ClientCatDadHudState.blockWindowTicks());
            case "catdad.skill.triple_hiss" -> single(cooldown, ClientCatDadHudState.hissStage()>0, ClientCatDadHudState.hissTimerTicks());
            case "chamber.skill.headhunter" -> single(cooldown, ClientChamberHudState.equippedGun().name().equals("HEADHUNTER"), 0);
            case "chamber.skill.rendezvous" -> deployed(cooldown, com.rzy.dealt_force_skills.character.chamber.ChamberMarkerType.TELEPORT_ANCHOR, ClientChamberHudState.equippedTool().name().equals("TELEPORT_CARD"));
            case "chamber.skill.tour_de_force" -> single(cooldown, ClientChamberHudState.equippedGun().name().equals("TOUR_DE_FORCE"), 0);
            case "chamber.skill.trademark" -> deployed(cooldown, com.rzy.dealt_force_skills.character.chamber.ChamberMarkerType.TRAP, ClientChamberHudState.equippedTool().name().equals("TRAP_CARD"));
            case "corps.skill.unrestricted_fighting_tournament" -> single(cooldown, ClientCorpsHudState.duelRemainingTicks()>0, ClientCorpsHudState.duelRemainingTicks());
            case "d_wolf.skill.hand_cannon" -> charged(ClientDWolfHudState.handCannonCharges(), ClientDWolfHudState.handCannonRechargeTicks(), ClientDWolfHudState.equippedTool().name().equals("HAND_CANNON"), 0);
            case "d_wolf.skill.overload" -> single(cooldown, ClientDWolfHudState.overloadStartupTicks()>0 || ClientDWolfHudState.overloadActiveTicks()>0, Math.max(ClientDWolfHudState.overloadStartupTicks(), ClientDWolfHudState.overloadActiveTicks()));
            case "d_wolf.skill.smoke" -> charged(ClientDWolfHudState.smokeCharges(), ClientDWolfHudState.smokeRechargeTicks(), false, 0);
            case "department.skill.armor" -> charged(ClientDepartmentHudState.trapCharges(), cooldown, ClientDepartmentHudState.equippedTool().name().equals("EXPLOSIVE_TRAP"), 0);
            case "department.skill.contingency" -> single(cooldown, ClientDepartmentHudState.coreCountdownTicks()>0 || ClientDepartmentHudState.coreAscendTicks()>0, Math.max(ClientDepartmentHudState.coreCountdownTicks(),ClientDepartmentHudState.coreAscendTicks()));
            case "department.skill.overheat_laser" -> single(cooldown, false, 0);
            case "gambler.skill.hakko_ichiu" -> charged(ClientGamblerHudState.active2UsesLeft() > 0 ? ClientGamblerHudState.active2UsesLeft() : cooldown > 0 ? 0 : 1, cooldown, ClientGamblerHudState.active2UsesLeft()>0, ClientGamblerHudState.active2UsesLeft()>0 ? ClientGamblerHudState.active2ExpiresTicks() : 0);
            case "gambler.skill.killing_gambler" -> single(cooldown, ClientGamblerHudState.coreActive(), ClientGamblerHudState.jackpotTicks());
            case "ghroth.skill.justice" -> single(cooldown, ClientGhrothState.justiceActiveTicks()>0, ClientGhrothState.justiceActiveTicks());
            case "ghroth.skill.noon" -> single(cooldown, ClientGhrothState.noonActiveTicks()>0, ClientGhrothState.noonActiveTicks());
            case "ghroth.skill.stars_orbit" -> single(cooldown, ClientGhrothState.starsActiveTicks()>0, ClientGhrothState.starsActiveTicks());
            case "gizmo.skill.smoke_trap" -> charged(ClientGizmoHudState.smokeCharges(), ClientGizmoHudState.smokeRechargeTicks(), ClientGizmoHudState.equippedTool().name().equals("SMOKE_TRAP"), 0);
            case "gizmo.skill.spider_nest" -> charged(ClientGizmoHudState.spiderCharges(), ClientGizmoHudState.spiderRechargeTicks(), ClientGizmoHudState.equippedTool().name().equals("SPIDER_NEST"), 0);
            case "gizmo.skill.t_boy" -> single(cooldown, ClientGizmoHudState.equippedTool().name().equals("T_BOY"), 0);
            case "hackclaw.skill.advanced_hack" -> single(cooldown, ClientHackclawHudState.coreChannelTicks()>0 || ClientHackclawHudState.coreActiveTicks()>0, Math.max(ClientHackclawHudState.coreChannelTicks(),ClientHackclawHudState.coreActiveTicks()));
            case "hackclaw.skill.flash_drone" -> charged(ClientHackclawHudState.flashDroneCharges(), ClientHackclawHudState.flashDroneRechargeTicks(), ClientHackclawHudState.equippedTool().name().equals("FLASH_DRONE"), 0);
            case "hackclaw.skill.hacking_knife" -> charged(ClientHackclawHudState.knifeCharges(), ClientHackclawHudState.knifeRechargeTicks(), ClientHackclawHudState.equippedTool().name().equals("HACKING_KNIFE"), 0);
            case "lex_ninjia.skill.blade" -> new SkillIconState(ClientLexNinjiaHudState.bladeStacks(), 0, 0, ClientLexNinjiaHudState.bladeStacks()>0, ClientLexNinjiaHudState.leicra()>0);
            case "lex_ninjia.skill.hand" -> new SkillIconState(ClientLexNinjiaHudState.handStacks(), 0, 0, ClientLexNinjiaHudState.handStacks()>0, ClientLexNinjiaHudState.leicra()>0);
            case "lex_ninjia.skill.harmony" -> new SkillIconState(ClientLexNinjiaHudState.harmonyStacks(), 0, 0, ClientLexNinjiaHudState.harmonyStacks()>0, ClientLexNinjiaHudState.leicra()>0);
            case "luna.skill.composite_grenade" -> charged(ClientLunaHudState.grenadeCharges(), ClientLunaHudState.grenadeRechargeTicks(), ClientLunaHudState.equippedTool().name().equals("COMPOSITE_GRENADE"),
                    fuseRemaining(ClientLunaHudState.equippedTool().name().equals("COMPOSITE_GRENADE"), ClientLunaHudState.grenadeCookTicks(), com.rzy.dealt_force_skills.character.luna.LunaStateManager.COMPOSITE_GRENADE_FUSE_TICKS));
            case "luna.skill.recon_arrow" -> single(cooldown, ClientLunaHudState.equippedTool().name().equals("RECON_BOW"), 0);
            case "luna.skill.shock_arrow" -> charged(ClientLunaHudState.shockCharges(), ClientLunaHudState.shockRechargeTicks(), ClientLunaHudState.equippedTool().name().equals("SHOCK_BOW"), 0);
            case "manba.skill.elbow" -> charged(ClientManbaHudState.elbowCharges(), ClientManbaHudState.elbowRechargeTicks(), false, 0);
            case "manba.skill.flashlight" -> new SkillIconState(ClientManbaHudState.flashlightDurability()>0 ? 1 : 0, 0, 0, ClientManbaHudState.flashlightActive(), ClientManbaHudState.flashlightDurability()>0);
            case "manba.skill.la_legend" -> single(cooldown, ClientManbaHudState.duelActive(), ClientManbaHudState.duelRemainingTicks());
            case "morse.skill.composite_flash" -> charged(ClientMorseHudState.flashCharges(), ClientMorseHudState.flashRechargeTicks(), ClientMorseHudState.equippedTool().name().equals("FLASH_GRENADE"), 0);
            case "morse.skill.shock_orb" -> charged(ClientMorseHudState.shockCharges(), ClientMorseHudState.shockRechargeTicks(), ClientMorseHudState.equippedTool().name().equals("SHOCK_ORB"), 0);
            case "morse.skill.sonar_detector" -> single(cooldown, ClientMorseHudState.deployTicks()>0, 0);
            case "nikaidou_hiro.skill.correct_error" -> single(ClientNikaidouHiroHudState.active1CooldownTicks(), false, 0);
            case "nikaidou_hiro.skill.erase_sin" -> single(cooldown, ClientNikaidouHiroHudState.hasEquippedTool(), 0);
            case "nikaidou_hiro.skill.only_i" -> single(cooldown, ClientNikaidouHiroHudState.coreActive(), ClientNikaidouHiroHudState.coreActiveTicks());
            case "nikaidou_hiro_witchification.skill.erase_error" -> single(ClientNikaidouHiroHudState.witchActive2CooldownTicks(), false, 0);
            case "nikaidou_hiro_witchification.skill.save_everyone" -> single(ClientNikaidouHiroHudState.coreCooldownTicks(), ClientNikaidouHiroHudState.coreActive(), ClientNikaidouHiroHudState.witchCoreDisplayTicks());
            case "nikaidou_hiro_witchification.skill.time_rewind" -> single(ClientNikaidouHiroHudState.active1CooldownTicks(), false, 0);
            case "nox.skill.flash_grenade" -> charged(ClientNoxHudState.flashCharges(), ClientNoxHudState.flashRechargeTicks(), ClientNoxHudState.equippedTool().name().equals("FLASH_GRENADE"), 0);
            case "nox.skill.rotor" -> single(cooldown, ClientNoxHudState.equippedTool().name().equals("ROTOR"), 0);
            case "nox.skill.silent_step" -> single(cooldown, ClientNoxHudState.corePrepTicks()>0 || ClientNoxHudState.stealthTicks()>0, Math.max(ClientNoxHudState.corePrepTicks(),ClientNoxHudState.stealthTicks()));
            case "ntwo.skill.condensed_launcher" -> new SkillIconState(ClientNTwoHudState.coreActiveTicks()>0 ? ClientNTwoHudState.coreAmmo() : cooldown>0 ? 0 : 1, cooldown, ClientNTwoHudState.coreActiveTicks(), ClientNTwoHudState.coreActiveTicks()>0, cooldown<=0 || ClientNTwoHudState.coreActiveTicks()>0);
            case "ntwo.skill.dewar_canister" -> charged(ClientNTwoHudState.dewarCharges(), ClientNTwoHudState.dewarRechargeTicks(), ClientNTwoHudState.equippedTool().name().equals("DEWAR_CANISTER"), 0);
            case "ntwo.skill.tracking_stun" -> single(cooldown, false, 0);
            case "raptor.skill.falcon_drone" -> single(cooldown, RaptorFalconController.isControlling() || ClientRaptorHudState.equippedTool().name().equals("FALCON_DRONE"), 0);
            case "raptor.skill.hummingbird_camera" -> single(cooldown,
                    ClientRaptorHudState.hummingbirdPendingTicks()>0 || ClientRaptorHudState.hummingbirdRemainingTicks()>0,
                    ClientRaptorHudState.hummingbirdRemainingTicks());
            case "raptor.skill.pulse_grenade" -> charged(ClientRaptorHudState.pulseCharges(), ClientRaptorHudState.pulseRechargeTicks(), ClientRaptorHudState.equippedTool().name().equals("PULSE_GRENADE"), 0);
            case "rover.skill.dog" -> single(cooldown, RoverClient.hudValue("DogTicks") > 0, RoverClient.hudValue("DogTicks"));
            case "rover.skill.injector" -> charged(RoverClient.hudValue("Injector"), RoverClient.hudValue("InjectorCooldown"), RoverClient.tool()==2, 0);
            case "rover.skill.smoke" -> charged(RoverClient.hudValue("Smoke"), RoverClient.hudValue("SmokeCooldown"), RoverClient.tool()==1, 0);
            case "saeed.skill.command_order" -> single(cooldown, ClientSaeedHudState.coreActiveTicks()>0, ClientSaeedHudState.coreActiveTicks());
            case "saeed.skill.fire_arrow" -> charged(ClientSaeedHudState.fireAmmo(), ClientSaeedHudState.fireRechargeTicks(), ClientSaeedHudState.hasCrossbowEquipped(), 0);
            case "shepherd.skill.frag_grenade" -> charged(ClientShepherdHudState.fragCharges(), ClientShepherdHudState.fragRechargeTicks(), ClientShepherdHudState.equippedTool().name().equals("FRAG_GRENADE"),
                    fuseRemaining(ClientShepherdHudState.equippedTool().name().equals("FRAG_GRENADE"), ClientShepherdHudState.grenadeCookTicks(), com.rzy.dealt_force_skills.character.shepherd.ShepherdStateManager.FRAG_GRENADE_FUSE_TICKS));
            case "shepherd.skill.sonic_trap" -> charged(ClientShepherdHudState.trapCharges(), ClientShepherdHudState.trapRechargeTicks(), ClientShepherdHudState.equippedTool().name().equals("SONIC_TRAP"), 0);
            case "sineva.skill.grapple" -> single(cooldown, SinevaInputHandler.isGrappleCharging(), 0);
            case "sineva.skill.blade_wire" -> charged(ClientSinevaHudState.bladeWireCharges(), ClientSinevaHudState.bladeWireRechargeTicks(), false, 0);
            case "sineva.skill.bomb_suit" -> single(cooldown, ClientSinevaHudState.bombSuitActive() || ClientSinevaHudState.bombSuitEquipTicks()>0, ClientSinevaHudState.bombSuitEquipTicks());
            case "stinger.skill.smoke_drone" -> single(cooldown, ClientStingerHudState.equippedTool().name().equals("SMOKE_DRONE"), 0);
            case "stinger.skill.smoke_grenade" -> single(cooldown, ClientStingerHudState.equippedTool().name().equals("SMOKE_GRENADE"), 0);
            case "stinger.skill.stim_gun" -> charged(ClientStingerHudState.stimCharges(), ClientStingerHudState.stimRechargeTicks(), ClientStingerHudState.equippedTool().name().equals("STIM_GUN"), 0);
            case "tempest.skill.emergency_recall" -> single(ClientTempestHudState.coreCooldownTicks(), ClientTempestHudState.ropeActive() || ClientTempestHudState.recalling(), 0);
            case "tempest.skill.tactical_roll" -> single(cooldown, ClientTempestHudState.pendingLandingRoll(), 0);
            case "tempest.skill.wall_drill_stinger" -> charged(ClientTempestHudState.wallCharges(), ClientTempestHudState.wallRechargeTicks(), ClientTempestHudState.equippedTool().name().equals("WALL_DRILL_STINGER"), 0);
            case "toxik.skill.adrenaline" -> single(cooldown, false, 0);
            case "toxik.skill.firefly_swarm" -> single(cooldown, ClientToxikHudState.equippedTool().name().equals("FIREFLY_SWARM"), 0);
            case "toxik.skill.tear_gas" -> charged(ClientToxikHudState.tearGasCharges(), ClientToxikHudState.tearGasRechargeTicks(), ClientToxikHudState.equippedTool().name().equals("TEAR_GAS"), 0);
            case "uluru.skill.incendiary" -> charged(ClientUluruHudState.incendiaryCharges(), ClientUluruHudState.incendiaryRechargeTicks(), ClientUluruHudState.equippedTool().name().equals("INCENDIARY"), 0);
            case "uluru.skill.loitering_missile" -> single(cooldown, ClientUluruHudState.equippedTool().name().equals("MISSILE") || UluruMissileController.isControlling(), 0);
            case "uluru.skill.quick_cover" -> charged(ClientUluruHudState.coverCharges(), ClientUluruHudState.coverRechargeTicks(), ClientUluruHudState.equippedTool().name().equals("COVER"), 0);
            case "undead.explorer.skill.active2" -> single(cooldown, ClientUndeadHudState.explorerMeditationTicks()>0, ClientUndeadHudState.explorerMeditationTicks());
            case "undead.explorer.skill.core" -> single(cooldown, ClientUndeadHudState.explorerSpaceTicks()>0, ClientUndeadHudState.explorerSpaceTicks());
            case "undead.hunter.skill.core" -> single(cooldown, ClientUndeadHudState.hunterScatter(), 0);
            case "undead.knight.skill.core" -> single(cooldown, ClientUndeadHudState.knightShield()>0, ClientUndeadHudState.knightShieldTicks());
            case "undead.rogue.skill.active1" -> single(cooldown, ClientUndeadHudState.rogueInvisibleTicks()>0, ClientUndeadHudState.rogueInvisibleTicks());
            case "undead.scholar.skill.active2" -> single(cooldown, ClientUndeadHudState.scholarRitualTicks()>0, ClientUndeadHudState.scholarRitualTicks());
            case "undead.warrior.skill.active2" -> single(cooldown, ClientUndeadHudState.warriorMight(), 0);
            case "undead.warrior.skill.core" -> single(cooldown, ClientUndeadHudState.warriorBloodlustTicks()>0, ClientUndeadHudState.warriorBloodlustTicks());
            case "vlinder.skill.active_defense_drone" -> single(cooldown, false, 0);
            case "vlinder.skill.medical_drone" -> charged(ClientVlinderHudState.medicalCharges(), ClientVlinderHudState.medicalRechargeTicks(), ClientVlinderHudState.equippedTool().name().equals("MEDICAL_DRONE"), 0);
            case "vlinder.skill.remote_smoke" -> charged(ClientVlinderHudState.smokeCharges(), ClientVlinderHudState.smokeRechargeTicks(), false, 0);
            case "vyron.skill.jet_dash" -> single(cooldown, ClientVyronHudState.dashTicks()>0, ClientVyronHudState.dashTicks());
            case "vyron.skill.magnetic_bomb" -> charged(ClientVyronHudState.bombCharges(), ClientVyronHudState.bombRechargeTicks(), ClientVyronHudState.equippedTool().name().equals("MAGNETIC_BOMB"), 0);
            case "vyron.skill.tiger_cannon" -> single(cooldown, ClientVyronHudState.equippedTool().name().equals("TIGER_CANNON"), 0);
            case "wildfire.skill.he_grenade" -> charged(WildfireClient.local().getInt("Grenade"), WildfireClient.local().getInt("GrenadeRecharge"), WildfireClient.tool()==2, 0);
            case "wildfire.skill.ring_smoke" -> charged(WildfireClient.local().getInt("Smoke"), WildfireClient.local().getInt("SmokeRecharge"), WildfireClient.tool()==1, 0);
            case "wildfire.skill.shield_breaker" -> single(WildfireClient.local().getInt("Cooldown"), WildfireClient.local().getInt("Phase")>0, WildfireClient.local().getInt("Prepare"));
            default -> single(cooldown, false, 0);
        };
    }
}
