package com.rzy.dealt_force_skills.config;

import java.util.List;
import java.util.Locale;

/** UI ownership only: persisted keys and network snapshot paths remain unchanged. */
public final class CharacterConfigGrouping {
    private static final List<String> CHARACTERS = List.of("catdad", "chamber", "commander", "corps",
            "department", "dwolf", "fox", "gambler", "ghroth", "gizmo", "hackclaw", "lexninjia",
            "luna", "manba", "morse", "nikaidou", "nox", "ntwo", "raptor", "rover", "saeed",
            "shepherd", "sineva", "stinger", "tempest", "toxik", "uluru", "undead", "vlinder", "vyron");

    private CharacterConfigGrouping() { }

    public static String canonical(String id) {
        String compact = id.toLowerCase(Locale.ROOT).replace("_", "");
        if (compact.startsWith("nikaidouhiro")) return "nikaidou";
        if (compact.equals("departmentoftransportation")) return "department";
        return CHARACTERS.contains(compact) ? compact : id;
    }

    public static String owner(String path) {
        String[] parts = path.toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length < 2) return "";
        if (parts[0].equals("characters")) return canonical(parts[1]);
        if (!List.of("summons", "entities", "deployables", "client").contains(parts[0])) return "";
        String module = parts[1];
        if (parts[0].equals("client") && (module.equals("character") || module.equals("visual"))
                && parts.length > 2) module = parts[2];
        String compact = module.replace("_", "");
        if (compact.startsWith("client")) compact = compact.substring("client".length());
        if (compact.startsWith("grapplehook") || compact.startsWith("bladewire")) return "sineva";
        if (compact.startsWith("quickcover")) return "uluru";
        for (String character : CHARACTERS) {
            if (compact.startsWith(character)) return character;
        }
        return "";
    }
}
