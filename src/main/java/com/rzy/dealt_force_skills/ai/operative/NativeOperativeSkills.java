package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Only publish a role after its native executor exists. */
public final class NativeOperativeSkills {
    private static final Map<String, Supplier<OperativeSkills>> FACTORIES = Map.ofEntries(
            Map.entry(ModCharacters.VYRON_ID, NativeVyronSkills::new),
            Map.entry(ModCharacters.LUNA_ID, NativeLunaSkills::new),
            Map.entry(ModCharacters.D_WOLF_ID, NativeDWolfSkills::new),
            Map.entry(ModCharacters.STINGER_ID, NativeStingerSkills::new),
            Map.entry(ModCharacters.TOXIK_ID, NativeToxikSkills::new),
            Map.entry(ModCharacters.HACKCLAW_ID, NativeHackclawSkills::new),
            Map.entry(ModCharacters.ULURU_ID, NativeUluruSkills::new),
            Map.entry(ModCharacters.SHEPHERD_ID, NativeShepherdSkills::new),
            Map.entry(ModCharacters.SINEVA_ID, NativeSinevaSkills::new),
            Map.entry(ModCharacters.GIZMO_ID, NativeGizmoSkills::new),
            Map.entry(ModCharacters.NOX_ID, NativeNoxSkills::new),
            Map.entry(ModCharacters.TEMPEST_ID, NativeTempestSkills::new),
            Map.entry(ModCharacters.RAPTOR_ID, NativeRaptorSkills::new),
            Map.entry(ModCharacters.MORSE_ID, NativeMorseSkills::new),
            Map.entry(ModCharacters.VLINDER_ID, NativeVlinderSkills::new),
            Map.entry(ModCharacters.ROVER_ID, NativeRoverSkills::new),
            Map.entry(ModCharacters.N_TWO_ID, NativeNTwoSkills::new),
            Map.entry(ModCharacters.CATDAD_ID, NativeCatDadSkills::new),
            Map.entry(ModCharacters.GHROTH_ID, NativeGhrothSkills::new),
            Map.entry(ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID, NativeDepartmentSkills::new),
            Map.entry(ModCharacters.NIKAIDOU_HIRO_ID, NativeNikaidouSkills::new),
            Map.entry(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID, NativeWitchSkills::new),
            Map.entry(ModCharacters.CORPS_ID, NativeCorpsSkills::new),
            Map.entry(ModCharacters.SAEED_ID, NativeSaeedSkills::new),
            Map.entry(ModCharacters.CHAMBER_ID, NativeChamberSkills::new),
            Map.entry(ModCharacters.MANBA_ID, NativeManbaSkills::new),
            Map.entry(ModCharacters.COMMANDER_ID, NativeCommanderSkills::new),
            Map.entry(ModCharacters.UNDEAD_ID, NativeUndeadSkills::new),
            Map.entry(ModCharacters.LEX_NINJIA_ID, NativeLexSkills::new));

    private NativeOperativeSkills() { }
    public static boolean supports(String id) { return FACTORIES.containsKey(id); }
    public static Set<String> characterIds() { return FACTORIES.keySet(); }
    public static OperativeSkills create(String id) { return FACTORIES.get(id).get(); }
}
