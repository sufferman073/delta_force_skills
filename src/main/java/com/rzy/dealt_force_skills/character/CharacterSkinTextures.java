package com.rzy.dealt_force_skills.character;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public final class CharacterSkinTextures {
    private static final String ROOT = "textures/entity/character_skins/";

    private static final Map<String, ResourceLocation> SKINS = Map.ofEntries(
            Map.entry(ModCharacters.SINEVA_ID, skin("sineva")),
            Map.entry(ModCharacters.ULURU_ID, skin("uluru")),
            Map.entry(ModCharacters.GIZMO_ID, skin("gizmo")),
            Map.entry(ModCharacters.SHEPHERD_ID, skin("shepherd")),
            Map.entry(ModCharacters.N_TWO_ID, skin("n_two")),
            Map.entry(ModCharacters.D_WOLF_ID, skin("d_wolf")),
            Map.entry(ModCharacters.VYRON_ID, skin("vyron")),
            Map.entry(ModCharacters.NOX_ID, skin("nox")),
            Map.entry(ModCharacters.TEMPEST_ID, skin("tempest")),
            Map.entry(ModCharacters.LUNA_ID, skin("luna")),
            Map.entry(ModCharacters.HACKCLAW_ID, skin("hackclaw")),
            Map.entry(ModCharacters.MORSE_ID, skin("morse")),
            Map.entry(ModCharacters.RAPTOR_ID, skin("raptor")),
            Map.entry(ModCharacters.STINGER_ID, skin("stinger")),
            Map.entry(ModCharacters.ROVER_ID, skin("rover")),
            Map.entry(ModCharacters.COMMANDER_ID, skin("commander")),
            Map.entry(ModCharacters.TOXIK_ID, skin("toxik")),
            Map.entry(ModCharacters.VLINDER_ID, skin("vlinder")),
            Map.entry(ModCharacters.MANBA_ID, skin("manba")),
            Map.entry(ModCharacters.NIKAIDOU_HIRO_ID, skin("nikaidou_hiro")),
            Map.entry(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID, skin("nikaidou_hiro")),
            Map.entry(ModCharacters.CATDAD_ID, skin("catdad")),
            Map.entry(ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID, skin("department_of_transportation")),
            Map.entry(ModCharacters.UNDEAD_ID, skin("undead")),
            Map.entry(ModCharacters.LEX_NINJIA_ID, skin("lex_ninjia")),
            Map.entry(ModCharacters.SAEED_ID, skin("saeed")),
            Map.entry(ModCharacters.GHROTH_ID, skin("ghroth"))
    );

    // Only supplied skins override geometry; other roles retain their existing model behavior.
    private static final Map<String, String> MODELS = Map.of(
            ModCharacters.VLINDER_ID, "default",
            ModCharacters.SHEPHERD_ID, "default",
            ModCharacters.N_TWO_ID, "default",
            ModCharacters.VYRON_ID, "default",
            ModCharacters.RAPTOR_ID, "slim",
            ModCharacters.TOXIK_ID, "slim",
            ModCharacters.ROVER_ID, "slim",
            ModCharacters.COMMANDER_ID, "slim"
    );

    private CharacterSkinTextures() {
    }

    public static Optional<ResourceLocation> skinFor(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(SKINS.get(characterId));
    }

    public static Optional<String> modelFor(String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(MODELS.get(characterId));
    }

    private static ResourceLocation skin(String name) {
        return ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, ROOT + name + ".png");
    }
}
