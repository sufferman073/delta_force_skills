package com.rzy.dealt_force_skills.character;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModCharacters {
    public static final String SINEVA_ID = DealtForceSkillsMod.MODID + ":engineer/sineva";
    public static final String ULURU_ID = DealtForceSkillsMod.MODID + ":engineer/uluru";
    public static final String GIZMO_ID = DealtForceSkillsMod.MODID + ":engineer/gizmo";
    public static final String SHEPHERD_ID = DealtForceSkillsMod.MODID + ":engineer/shepherd";
    public static final String N_TWO_ID = DealtForceSkillsMod.MODID + ":engineer/n_two";
    public static final String D_WOLF_ID = DealtForceSkillsMod.MODID + ":assault/d_wolf";
    public static final String CORPS_ID = DealtForceSkillsMod.MODID + ":special/corps";
    public static final String LEGACY_CORPS_ID = DealtForceSkillsMod.MODID + ":assault/corps";
    public static final String VYRON_ID = DealtForceSkillsMod.MODID + ":assault/vyron";
    public static final String NOX_ID = DealtForceSkillsMod.MODID + ":assault/nox";
    public static final String TEMPEST_ID = DealtForceSkillsMod.MODID + ":assault/tempest";
    public static final String LUNA_ID = DealtForceSkillsMod.MODID + ":recon/luna";
    public static final String HACKCLAW_ID = DealtForceSkillsMod.MODID + ":recon/hackclaw";
    public static final String MORSE_ID = DealtForceSkillsMod.MODID + ":recon/morse";
    public static final String RAPTOR_ID = DealtForceSkillsMod.MODID + ":recon/raptor";
    public static final String SAEED_ID = DealtForceSkillsMod.MODID + ":boss/saeed";
    public static final String GHROTH_ID = DealtForceSkillsMod.MODID + ":boss/ghroth";
    public static final String COMMANDER_ID = DealtForceSkillsMod.MODID + ":boss/commander";
    public static final String LEGACY_COMMANDER_ID = DealtForceSkillsMod.MODID + ":special/commander";
    public static final String ROVER_ID = DealtForceSkillsMod.MODID + ":support/rover";
    public static final String STINGER_ID = DealtForceSkillsMod.MODID + ":support/stinger";
    public static final String TOXIK_ID = DealtForceSkillsMod.MODID + ":support/toxik";
    public static final String VLINDER_ID = DealtForceSkillsMod.MODID + ":support/vlinder";
    public static final String MANBA_ID = DealtForceSkillsMod.MODID + ":special/manba";
    public static final String NIKAIDOU_HIRO_ID = DealtForceSkillsMod.MODID + ":special/nikaidou_hiro";
    public static final String NIKAIDOU_HIRO_WITCHIFICATION_ID = DealtForceSkillsMod.MODID + ":special/nikaidou_hiro_witchification";
    public static final String CATDAD_ID = DealtForceSkillsMod.MODID + ":special/catdad";
    public static final String DEPARTMENT_OF_TRANSPORTATION_ID = DealtForceSkillsMod.MODID + ":special/department_of_transportation";
    public static final String UNDEAD_ID = DealtForceSkillsMod.MODID + ":special/undead";
    public static final String LEX_NINJIA_ID = DealtForceSkillsMod.MODID + ":special/lex_ninjia";
    public static final String GAMBLER_ID = DealtForceSkillsMod.MODID + ":special/gambler";
    public static final String CHAMBER_ID = DealtForceSkillsMod.MODID + ":valorant/chamber";

    private static final Map<String, String> COMMAND_NAME_ALIASES = Map.ofEntries(
            Map.entry(normalizeCommandName("深蓝"), SINEVA_ID),
            Map.entry(normalizeCommandName("乌鲁鲁"), ULURU_ID),
            Map.entry(normalizeCommandName("比特"), GIZMO_ID),
            Map.entry(normalizeCommandName("牧羊人"), SHEPHERD_ID),
            Map.entry(normalizeCommandName("液氮"), N_TWO_ID),
            Map.entry(normalizeCommandName("N-two"), N_TWO_ID),
            Map.entry(normalizeCommandName("红狼"), D_WOLF_ID),
            Map.entry(normalizeCommandName("军团"), CORPS_ID),
            Map.entry(normalizeCommandName("Corps"), CORPS_ID),
            Map.entry(normalizeCommandName("拳抖幻"), CORPS_ID),
            Map.entry(normalizeCommandName("QuanDouhuan"), CORPS_ID),
            Map.entry(normalizeCommandName("威龙"), VYRON_ID),
            Map.entry(normalizeCommandName("疾风"), TEMPEST_ID),
            Map.entry(normalizeCommandName("露娜"), LUNA_ID),
            Map.entry(normalizeCommandName("骇爪"), HACKCLAW_ID),
            Map.entry(normalizeCommandName("回响"), MORSE_ID),
            Map.entry(normalizeCommandName("银翼"), RAPTOR_ID),
            Map.entry(normalizeCommandName("赛伊德"), SAEED_ID),
            Map.entry(normalizeCommandName("典狱长"), GHROTH_ID),
            Map.entry(normalizeCommandName("指挥官"), COMMANDER_ID),
            Map.entry(normalizeCommandName("Commander"), COMMANDER_ID),
            Map.entry(normalizeCommandName("毒刺"), STINGER_ID),
            Map.entry(normalizeCommandName("旅人"), ROVER_ID),
            Map.entry(normalizeCommandName("Rover"), ROVER_ID),
            Map.entry(normalizeCommandName("毒克"), TOXIK_ID),
            Map.entry(normalizeCommandName("蝶"), VLINDER_ID),
            Map.entry(normalizeCommandName("曼巴"), MANBA_ID),
            Map.entry(normalizeCommandName("二阶堂浩"), NIKAIDOU_HIRO_ID),
            Map.entry(normalizeCommandName("二阶堂希罗魔女化"), NIKAIDOU_HIRO_WITCHIFICATION_ID),
            Map.entry(normalizeCommandName("NikaidouHiro-Witchification"), NIKAIDOU_HIRO_WITCHIFICATION_ID),
            Map.entry(normalizeCommandName("猫爸"), CATDAD_ID),
            Map.entry(normalizeCommandName("运输部"), DEPARTMENT_OF_TRANSPORTATION_ID),
            Map.entry(normalizeCommandName("亡灵"), UNDEAD_ID),
            Map.entry(normalizeCommandName("蕾克拉忍者"), LEX_NINJIA_ID),
            Map.entry(normalizeCommandName("赌徒"), GAMBLER_ID),
            Map.entry(normalizeCommandName("Gambler"), GAMBLER_ID),
            Map.entry(normalizeCommandName("尚博勒"), CHAMBER_ID),
            Map.entry(normalizeCommandName("Chamber"), CHAMBER_ID)
    );
    private static final List<String> COMMAND_NAME_ALIAS_SUGGESTIONS = List.of(
            "深蓝", "乌鲁鲁", "比特", "牧羊人", "液氮", "N-two",
            "红狼", "军团", "Corps", "拳抖幻", "QuanDouhuan", "威龙", "疾风",
            "露娜", "骇爪", "回响", "银翼",
            "赛伊德", "典狱长", "指挥官", "Commander",
            "毒刺", "毒克", "蝶",
            "曼巴", "二阶堂浩", "猫爸", "运输部", "亡灵", "蕾克拉忍者",
            "赌徒", "Gambler",
            "二阶堂希罗魔女化", "二阶堂希罗·魔女化", "NikaidouHiro-Witchification",
            "尚博勒", "Chamber"
    );
    private static final Map<String, CharacterDefinition> CHARACTERS = new LinkedHashMap<>();

    public static final CharacterDefinition SINEVA = register(new CharacterDefinition(
            SINEVA_ID,
            "character.dealt_force_skills.sineva.name",
            CharacterRole.ENGINEER,
            "characters_records/Engineer/Sineva.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.sineva.skill.rear_guard",
                            "character.dealt_force_skills.sineva.skill.rear_guard.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.sineva.skill.blade_wire",
                            "character.dealt_force_skills.sineva.skill.blade_wire.desc",
                            35 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.sineva.skill.grapple",
                            "character.dealt_force_skills.sineva.skill.grapple.desc",
                            8 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.sineva.skill.bomb_suit",
                            "character.dealt_force_skills.sineva.skill.bomb_suit.desc",
                            60 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition ULURU = register(new CharacterDefinition(
            ULURU_ID,
            "character.dealt_force_skills.uluru.name",
            CharacterRole.ENGINEER,
            "characters_records/Engineer/Uluru.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.uluru.skill.veteran",
                            "character.dealt_force_skills.uluru.skill.veteran.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.uluru.skill.incendiary",
                            "character.dealt_force_skills.uluru.skill.incendiary.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.uluru.skill.quick_cover",
                            "character.dealt_force_skills.uluru.skill.quick_cover.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.uluru.skill.loitering_missile",
                            "character.dealt_force_skills.uluru.skill.loitering_missile.desc",
                            90 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition GIZMO = register(new CharacterDefinition(
            GIZMO_ID,
            "character.dealt_force_skills.gizmo.name",
            CharacterRole.ENGINEER,
            "characters_records/Engineer/Gizmo.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.gizmo.skill.defensive_vanguard",
                            "character.dealt_force_skills.gizmo.skill.defensive_vanguard.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.gizmo.skill.smoke_trap",
                            "character.dealt_force_skills.gizmo.skill.smoke_trap.desc",
                            50 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.gizmo.skill.spider_nest",
                            "character.dealt_force_skills.gizmo.skill.spider_nest.desc",
                            50 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.gizmo.skill.t_boy",
                            "character.dealt_force_skills.gizmo.skill.t_boy.desc",
                            75 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition D_WOLF = register(new CharacterDefinition(
            D_WOLF_ID,
            "character.dealt_force_skills.d_wolf.name",
            CharacterRole.ASSAULT,
            "characters_records/Assault/D-Wolf.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.d_wolf.skill.tactical_slide",
                            "character.dealt_force_skills.d_wolf.skill.tactical_slide.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.d_wolf.skill.hand_cannon",
                            "character.dealt_force_skills.d_wolf.skill.hand_cannon.desc",
                            25 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.d_wolf.skill.smoke",
                            "character.dealt_force_skills.d_wolf.skill.smoke.desc",
                            25 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.d_wolf.skill.overload",
                            "character.dealt_force_skills.d_wolf.skill.overload.desc",
                            75 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition CORPS = register(new CharacterDefinition(
            CORPS_ID,
            "character.dealt_force_skills.corps.name",
            CharacterRole.SPECIAL,
            "characters_records/Assault/Corps.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.corps.skill.loyalty",
                            "character.dealt_force_skills.corps.skill.loyalty.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.corps.skill.warm_enforcement",
                            "character.dealt_force_skills.corps.skill.warm_enforcement.desc",
                            10 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.corps.skill.forceful_baton",
                            "character.dealt_force_skills.corps.skill.forceful_baton.desc",
                            10 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.corps.skill.unrestricted_fighting_tournament",
                            "character.dealt_force_skills.corps.skill.unrestricted_fighting_tournament.desc",
                            90 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition VYRON = register(new CharacterDefinition(
            VYRON_ID,
            "character.dealt_force_skills.vyron.name",
            CharacterRole.ASSAULT,
            "characters_records/Assault/Vyron.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.vyron.skill.powered_landing",
                            "character.dealt_force_skills.vyron.skill.powered_landing.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.vyron.skill.jet_dash",
                            "character.dealt_force_skills.vyron.skill.jet_dash.desc",
                            8 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.vyron.skill.magnetic_bomb",
                            "character.dealt_force_skills.vyron.skill.magnetic_bomb.desc",
                            28 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.vyron.skill.tiger_cannon",
                            "character.dealt_force_skills.vyron.skill.tiger_cannon.desc",
                            45 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition NOX = register(new CharacterDefinition(
            NOX_ID,
            "character.dealt_force_skills.nox.name",
            CharacterRole.ASSAULT,
            "characters_records/Assault/Nox.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.nox.skill.delayed_wound",
                            "character.dealt_force_skills.nox.skill.delayed_wound.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.nox.skill.rotor",
                            "character.dealt_force_skills.nox.skill.rotor.desc",
                            55 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.nox.skill.flash_grenade",
                            "character.dealt_force_skills.nox.skill.flash_grenade.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.nox.skill.silent_step",
                            "character.dealt_force_skills.nox.skill.silent_step.desc",
                            75 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition TEMPEST = register(new CharacterDefinition(
            TEMPEST_ID,
            "character.dealt_force_skills.tempest.name",
            CharacterRole.ASSAULT,
            "characters_records/需要完成的角色/Tempest.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.tempest.skill.explosive_spine",
                            "character.dealt_force_skills.tempest.skill.explosive_spine.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.tempest.skill.tactical_roll",
                            "character.dealt_force_skills.tempest.skill.tactical_roll.desc",
                            20 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.tempest.skill.wall_drill_stinger",
                            "character.dealt_force_skills.tempest.skill.wall_drill_stinger.desc",
                            35 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.tempest.skill.emergency_recall",
                            "character.dealt_force_skills.tempest.skill.emergency_recall.desc",
                            110 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition STINGER = register(new CharacterDefinition(
            STINGER_ID,
            "character.dealt_force_skills.stinger.name",
            CharacterRole.SUPPORT,
            "characters_records/Support/Stinger.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.stinger.skill.professional_rescue",
                            "character.dealt_force_skills.stinger.skill.professional_rescue.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.stinger.skill.smoke_grenade",
                            "character.dealt_force_skills.stinger.skill.smoke_grenade.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.stinger.skill.smoke_drone",
                            "character.dealt_force_skills.stinger.skill.smoke_drone.desc",
                            55 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.stinger.skill.stim_gun",
                            "character.dealt_force_skills.stinger.skill.stim_gun.desc",
                            25 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition TOXIK = register(new CharacterDefinition(
            TOXIK_ID,
            "character.dealt_force_skills.toxik.name",
            CharacterRole.SUPPORT,
            "characters_records/需要完成的角色/Toxik.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.toxik.skill.efficient_treatment",
                            "character.dealt_force_skills.toxik.skill.efficient_treatment.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.toxik.skill.adrenaline",
                            "character.dealt_force_skills.toxik.skill.adrenaline.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.toxik.skill.tear_gas",
                            "character.dealt_force_skills.toxik.skill.tear_gas.desc",
                            35 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.toxik.skill.firefly_swarm",
                            "character.dealt_force_skills.toxik.skill.firefly_swarm.desc",
                            90 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition VLINDER = register(new CharacterDefinition(
            VLINDER_ID,
            "character.dealt_force_skills.vlinder.name",
            CharacterRole.SUPPORT,
            "characters_records/需要完成的角色/Vlinder.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.vlinder.skill.vital_monitoring",
                            "character.dealt_force_skills.vlinder.skill.vital_monitoring.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.vlinder.skill.medical_drone",
                            "character.dealt_force_skills.vlinder.skill.medical_drone.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.vlinder.skill.remote_smoke",
                            "character.dealt_force_skills.vlinder.skill.remote_smoke.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.vlinder.skill.active_defense_drone",
                            "character.dealt_force_skills.vlinder.skill.active_defense_drone.desc",
                            100 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition ROVER = register(new CharacterDefinition(
            ROVER_ID, "character.dealt_force_skills.rover.name", CharacterRole.SUPPORT,
            "characters_records/Support/Rover.txt", List.of(
                    new SkillDefinition(SkillSlot.PASSIVE, "character.dealt_force_skills.rover.skill.survivor",
                            "character.dealt_force_skills.rover.skill.survivor.desc", 0, true),
                    new SkillDefinition(SkillSlot.ACTIVE_1, "character.dealt_force_skills.rover.skill.smoke",
                            "character.dealt_force_skills.rover.skill.smoke.desc", 600, true),
                    new SkillDefinition(SkillSlot.ACTIVE_2, "character.dealt_force_skills.rover.skill.injector",
                            "character.dealt_force_skills.rover.skill.injector.desc", 700, true),
                    new SkillDefinition(SkillSlot.CORE, "character.dealt_force_skills.rover.skill.dog",
                            "character.dealt_force_skills.rover.skill.dog.desc", 1500, true))));

    public static final CharacterDefinition SHEPHERD = register(new CharacterDefinition(
            SHEPHERD_ID,
            "character.dealt_force_skills.shepherd.name",
            CharacterRole.ENGINEER,
            "characters_records/Engineer/Shepherd.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.shepherd.skill.reflex_field",
                            "character.dealt_force_skills.shepherd.skill.reflex_field.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.shepherd.skill.sonic_trap",
                            "character.dealt_force_skills.shepherd.skill.sonic_trap.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.shepherd.skill.frag_grenade",
                            "character.dealt_force_skills.shepherd.skill.frag_grenade.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.shepherd.skill.drone_stun",
                            "character.dealt_force_skills.shepherd.skill.drone_stun.desc",
                            90 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition N_TWO = register(new CharacterDefinition(
            N_TWO_ID,
            "character.dealt_force_skills.ntwo.name",
            CharacterRole.ENGINEER,
            "characters_records/Engineer/N-two.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.ntwo.skill.frost_reaction",
                            "character.dealt_force_skills.ntwo.skill.frost_reaction.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.ntwo.skill.tracking_stun",
                            "character.dealt_force_skills.ntwo.skill.tracking_stun.desc",
                            35 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.ntwo.skill.dewar_canister",
                            "character.dealt_force_skills.ntwo.skill.dewar_canister.desc",
                            60 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.ntwo.skill.condensed_launcher",
                            "character.dealt_force_skills.ntwo.skill.condensed_launcher.desc",
                            100 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition LUNA = register(new CharacterDefinition(
            LUNA_ID,
            "character.dealt_force_skills.luna.name",
            CharacterRole.RECON,
            "characters_records/Recon/Luna.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.luna.skill.target_trace",
                            "character.dealt_force_skills.luna.skill.target_trace.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.luna.skill.shock_arrow",
                            "character.dealt_force_skills.luna.skill.shock_arrow.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.luna.skill.composite_grenade",
                            "character.dealt_force_skills.luna.skill.composite_grenade.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.luna.skill.recon_arrow",
                            "character.dealt_force_skills.luna.skill.recon_arrow.desc",
                            45 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition HACKCLAW = register(new CharacterDefinition(
            HACKCLAW_ID,
            "character.dealt_force_skills.hackclaw.name",
            CharacterRole.RECON,
            "characters_records/需要完成的角色/Hackclaw.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.hackclaw.skill.silent_movement",
                            "character.dealt_force_skills.hackclaw.skill.silent_movement.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.hackclaw.skill.hacking_knife",
                            "character.dealt_force_skills.hackclaw.skill.hacking_knife.desc",
                            35 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.hackclaw.skill.flash_drone",
                            "character.dealt_force_skills.hackclaw.skill.flash_drone.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.hackclaw.skill.advanced_hack",
                            "character.dealt_force_skills.hackclaw.skill.advanced_hack.desc",
                            60 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition MORSE = register(new CharacterDefinition(
            MORSE_ID,
            "character.dealt_force_skills.morse.name",
            CharacterRole.RECON,
            "characters_records/需要完成的角色/Morse.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.morse.skill.alert_hearing",
                            "character.dealt_force_skills.morse.skill.alert_hearing.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.morse.skill.shock_orb",
                            "character.dealt_force_skills.morse.skill.shock_orb.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.morse.skill.composite_flash",
                            "character.dealt_force_skills.morse.skill.composite_flash.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.morse.skill.sonar_detector",
                            "character.dealt_force_skills.morse.skill.sonar_detector.desc",
                            75 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition RAPTOR = register(new CharacterDefinition(
            RAPTOR_ID,
            "character.dealt_force_skills.raptor.name",
            CharacterRole.RECON,
            "characters_records/需要完成的角色/Raptor.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.raptor.skill.field_experience",
                            "character.dealt_force_skills.raptor.skill.field_experience.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.raptor.skill.falcon_drone",
                            "character.dealt_force_skills.raptor.skill.falcon_drone.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.raptor.skill.pulse_grenade",
                            "character.dealt_force_skills.raptor.skill.pulse_grenade.desc",
                            40 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.raptor.skill.hummingbird_camera",
                            "character.dealt_force_skills.raptor.skill.hummingbird_camera.desc",
                            45 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition SAEED = register(new CharacterDefinition(
            SAEED_ID,
            "character.dealt_force_skills.saeed.name",
            CharacterRole.BOSS,
            "characters_records/Bosses/Saeed_team/Saeed.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.saeed.skill.red_sky_hunt",
                            "character.dealt_force_skills.saeed.skill.red_sky_hunt.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.saeed.skill.tactical_roll",
                            "character.dealt_force_skills.saeed.skill.tactical_roll.desc",
                            12 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.saeed.skill.fire_arrow",
                            "character.dealt_force_skills.saeed.skill.fire_arrow.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.saeed.skill.command_order",
                            "character.dealt_force_skills.saeed.skill.command_order.desc",
                            120 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition GHROTH = register(new CharacterDefinition(
            GHROTH_ID,
            "character.dealt_force_skills.ghroth.name",
            CharacterRole.BOSS,
            "characters_records/Bosses/Ghroth.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.ghroth.skill.demigod_brain",
                            "character.dealt_force_skills.ghroth.skill.demigod_brain.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.ghroth.skill.stars_orbit",
                            "character.dealt_force_skills.ghroth.skill.stars_orbit.desc",
                            25 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.ghroth.skill.justice",
                            "character.dealt_force_skills.ghroth.skill.justice.desc",
                            25 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.ghroth.skill.noon",
                            "character.dealt_force_skills.ghroth.skill.noon.desc",
                            50 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition COMMANDER = register(new CharacterDefinition(
            COMMANDER_ID,
            "character.dealt_force_skills.commander.name",
            CharacterRole.BOSS,
            "characters_records/新文件/指挥官相关文件/Commander.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.commander.skill.tactical_command",
                            "character.dealt_force_skills.commander.skill.tactical_command.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.commander.skill.inherited_active_1",
                            "character.dealt_force_skills.commander.skill.inherited_active_1.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.commander.skill.inherited_active_2",
                            "character.dealt_force_skills.commander.skill.inherited_active_2.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.commander.skill.inherited_core",
                            "character.dealt_force_skills.commander.skill.inherited_core.desc",
                            0,
                            true)
            )
    ));

    public static final CharacterDefinition MANBA = register(new CharacterDefinition(
            MANBA_ID,
            "character.dealt_force_skills.manba.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/Manba.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.manba.skill.evil_talent",
                            "character.dealt_force_skills.manba.skill.evil_talent.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.manba.skill.elbow",
                            "character.dealt_force_skills.manba.skill.elbow.desc",
                            6 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.manba.skill.flashlight",
                            "character.dealt_force_skills.manba.skill.flashlight.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.manba.skill.la_legend",
                            "character.dealt_force_skills.manba.skill.la_legend.desc",
                            5 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition NIKAIDOU_HIRO = register(new CharacterDefinition(
            NIKAIDOU_HIRO_ID,
            "character.dealt_force_skills.nikaidou_hiro.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/NikaidouHiro.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.nikaidou_hiro.skill.remove_gap",
                            "character.dealt_force_skills.nikaidou_hiro.skill.remove_gap.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.nikaidou_hiro.skill.correct_error",
                            "character.dealt_force_skills.nikaidou_hiro.skill.correct_error.desc",
                            15 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.nikaidou_hiro.skill.erase_sin",
                            "character.dealt_force_skills.nikaidou_hiro.skill.erase_sin.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.nikaidou_hiro.skill.only_i",
                            "character.dealt_force_skills.nikaidou_hiro.skill.only_i.desc",
                            90 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition NIKAIDOU_HIRO_WITCHIFICATION = register(new CharacterDefinition(
            NIKAIDOU_HIRO_WITCHIFICATION_ID,
            "character.dealt_force_skills.nikaidou_hiro_witchification.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/NikaidouHiro-Witchification.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.witchification",
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.witchification.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.time_rewind",
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.time_rewind.desc",
                            5 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.erase_error",
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.erase_error.desc",
                            22 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.save_everyone",
                            "character.dealt_force_skills.nikaidou_hiro_witchification.skill.save_everyone.desc",
                            10 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition CATDAD = register(new CharacterDefinition(
            CATDAD_ID,
            "character.dealt_force_skills.catdad.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/CatDad.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.catdad.skill.bully",
                            "character.dealt_force_skills.catdad.skill.bully.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.catdad.skill.triple_hiss",
                            "character.dealt_force_skills.catdad.skill.triple_hiss.desc",
                            10 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.catdad.skill.spine_block",
                            "character.dealt_force_skills.catdad.skill.spine_block.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.catdad.skill.highway",
                            "character.dealt_force_skills.catdad.skill.highway.desc",
                            45 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition DEPARTMENT_OF_TRANSPORTATION = register(new CharacterDefinition(
            DEPARTMENT_OF_TRANSPORTATION_ID,
            "character.dealt_force_skills.department.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/Department_of_Transportation-Mass_Production_Model.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.department.skill.traffic_booth",
                            "character.dealt_force_skills.department.skill.traffic_booth.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.department.skill.overheat_laser",
                            "character.dealt_force_skills.department.skill.overheat_laser.desc",
                            7 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.department.skill.armor",
                            "character.dealt_force_skills.department.skill.armor.desc",
                            45 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.department.skill.contingency",
                            "character.dealt_force_skills.department.skill.contingency.desc",
                            5 * 20,
                            true)
            )
    ));

    public static final CharacterDefinition UNDEAD = register(new CharacterDefinition(
            UNDEAD_ID,
            "character.dealt_force_skills.undead.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/Undead.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.undead.skill.many_faces",
                            "character.dealt_force_skills.undead.skill.many_faces.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.undead.skill.profession_active1",
                            "character.dealt_force_skills.undead.skill.profession_active1.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.undead.skill.profession_active2",
                            "character.dealt_force_skills.undead.skill.profession_active2.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.undead.skill.profession_core",
                            "character.dealt_force_skills.undead.skill.profession_core.desc",
                            0,
                            true)
            )
    ));

    public static final CharacterDefinition LEX_NINJIA = register(new CharacterDefinition(
            LEX_NINJIA_ID,
            "character.dealt_force_skills.lex_ninjia.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/Lex_Ninjia.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.lex_ninjia.skill.leicra",
                            "character.dealt_force_skills.lex_ninjia.skill.leicra.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.lex_ninjia.skill.hand",
                            "character.dealt_force_skills.lex_ninjia.skill.hand.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.lex_ninjia.skill.blade",
                            "character.dealt_force_skills.lex_ninjia.skill.blade.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.lex_ninjia.skill.harmony",
                            "character.dealt_force_skills.lex_ninjia.skill.harmony.desc",
                            0,
                            true)
            )
    ));

    public static final CharacterDefinition GAMBLER = register(new CharacterDefinition(
            GAMBLER_ID,
            "character.dealt_force_skills.gambler.name",
            CharacterRole.SPECIAL,
            "characters_records/Special/Gambler.txt",
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.gambler.skill.super_shield",
                            "character.dealt_force_skills.gambler.skill.super_shield.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.gambler.skill.final_bet",
                            "character.dealt_force_skills.gambler.skill.final_bet.desc",
                            80 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.gambler.skill.hakko_ichiu",
                            "character.dealt_force_skills.gambler.skill.hakko_ichiu.desc",
                            120 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.gambler.skill.killing_gambler",
                            "character.dealt_force_skills.gambler.skill.killing_gambler.desc",
                            0,
                            true)
            )
    ));

    public static final CharacterDefinition CHAMBER = register(new CharacterDefinition(
            CHAMBER_ID,
            "character.dealt_force_skills.chamber.name",
            true,
            CharacterRole.RECON,
            "characters_records/Valorant/Chamber.txt",
            false,
            List.of(
                    new SkillDefinition(SkillSlot.PASSIVE,
                            "character.dealt_force_skills.chamber.skill.spare_cash",
                            "character.dealt_force_skills.chamber.skill.spare_cash.desc",
                            0,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_1,
                            "character.dealt_force_skills.chamber.skill.rendezvous",
                            "character.dealt_force_skills.chamber.skill.rendezvous.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.ACTIVE_2,
                            "character.dealt_force_skills.chamber.skill.trademark",
                            "character.dealt_force_skills.chamber.skill.trademark.desc",
                            30 * 20,
                            true),
                    new SkillDefinition(SkillSlot.CORE,
                            "character.dealt_force_skills.chamber.skill.headhunter",
                            "character.dealt_force_skills.chamber.skill.headhunter.desc",
                            60 * 20,
                            true)
            )
    ));

    private ModCharacters() {
    }

    public static Collection<CharacterDefinition> all() {
        return Collections.unmodifiableCollection(CHARACTERS.values());
    }

    public static Optional<CharacterDefinition> get(String id) {
        if (LEGACY_CORPS_ID.equals(id)) {
            id = CORPS_ID;
        } else if (LEGACY_COMMANDER_ID.equals(id)) {
            id = COMMANDER_ID;
        }
        return Optional.ofNullable(CHARACTERS.get(id));
    }

    public static Optional<CharacterDefinition> findByCommandName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        CharacterDefinition exact = CHARACTERS.get(trimmed);
        if (exact != null) {
            return Optional.of(exact);
        }
        CharacterDefinition namespaced = CHARACTERS.get(DealtForceSkillsMod.MODID + ":" + trimmed);
        if (namespaced != null) {
            return Optional.of(namespaced);
        }

        String normalized = normalizeCommandName(trimmed);
        String aliasedId = COMMAND_NAME_ALIASES.get(normalized);
        if (aliasedId != null) {
            return get(aliasedId);
        }
        return CHARACTERS.values().stream()
                .filter(character -> matchesCommandName(normalized, character))
                .findFirst();
    }

    public static List<String> commandNameSuggestions() {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (CharacterDefinition character : CHARACTERS.values()) {
            String id = character.id();
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            String shortName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            suggestions.add(shortName);
            suggestions.add(path);
            suggestions.add(id);
        }
        suggestions.addAll(COMMAND_NAME_ALIAS_SUGGESTIONS);
        return List.copyOf(suggestions);
    }

    public static List<CharacterDefinition> byRole(CharacterRole role) {
        return CHARACTERS.values().stream()
                .filter(character -> character.role() == role)
                .filter(CharacterDefinition::selectable)
                .toList();
    }

    private static CharacterDefinition register(CharacterDefinition character) {
        if (CHARACTERS.containsKey(character.id())) {
            throw new IllegalStateException("Duplicate character id: " + character.id());
        }
        CHARACTERS.put(character.id(), character);
        return character;
    }

    private static boolean matchesCommandName(String normalized, CharacterDefinition character) {
        String id = character.id();
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String shortName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        String translationName = character.nameTranslationKey()
                .replace("character." + DealtForceSkillsMod.MODID + ".", "")
                .replace(".name", "");
        return normalized.equals(normalizeCommandName(id))
                || normalized.equals(normalizeCommandName(path))
                || normalized.equals(normalizeCommandName(shortName))
                || normalized.equals(normalizeCommandName(translationName));
    }

    private static String normalizeCommandName(String value) {
        StringBuilder result = new StringBuilder();
        value.toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(result::appendCodePoint);
        return result.toString();
    }
}
