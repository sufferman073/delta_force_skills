package com.rzy.dealt_force_skills.client.visual;

enum ConsumableMotion {
    BLISTER, BOTTLE, DRINK, INJECT, OE2, WRAP, TOURNIQUET, SURGERY, FIRST_AID, SUPPLY, REPAIR;

    static ConsumableMotion forPath(String path) {
        return switch (path) {
            case "sustained_release_painkiller" -> BLISTER;
            case "bottled_antibiotics", "dve_painkiller" -> BOTTLE;
            case "guanshan_nanyue", "highball", "heartbeat", "starlight_glitter", "long_goodbye",
                    "mountain_rain", "sky_island", "cranberry_special", "mist_morning", "tulip",
                    "wild_rose", "asara_style", "reis_dance" -> DRINK;
            case "prototype_madness_compound", "raven_shadow_extract", "dark_zone_rainbow_injection",
                    "experimental_iron_curtain_catalyst", "m1_muscle_booster", "m1_muscle_injection",
                    "stamina_activation_injection", "stamina_enhancer", "norepinephrine",
                    "perception_activation_injection", "perception_activator", "simple_injector", "strong_injector" -> INJECT;
            case "oe2_combat_stimulant" -> OE2;
            case "elastic_bandage" -> WRAP;
            case "cat_tourniquet" -> TOURNIQUET;
            case "simple_surgical_pack", "tactical_quick_surgical_pack", "dek_field_surgical_pack" -> SURGERY;
            case "car_first_aid_kit", "field_first_aid_kit", "outdoor_medical_kit", "battlefield_medical_kit" -> FIRST_AID;
            case "special_equipment_supply" -> SUPPLY;
            case "self_made_armor_repair_kit", "standard_armor_repair_kit", "precision_armor_repair_kit",
                    "advanced_armor_repair_kit", "self_made_helmet_repair_kit", "standard_helmet_repair_kit",
                    "precision_helmet_repair_kit", "advanced_helmet_repair_kit" -> REPAIR;
            default -> null;
        };
    }

    static float ramp(float value, float start, float end) {
        float amount = Math.max(0, Math.min(1, (value - start) / (end - start)));
        return amount * amount * (3 - 2 * amount);
    }

    static float progress(int duration, int remaining, float partial) {
        return Math.max(0, Math.min(1, (duration - remaining + partial) / Math.max(1, duration)));
    }

    static float carry(float seconds) {
        return ramp(seconds, 1.45F, 2.17F) * (1 - ramp(seconds, 2.29F, 3));
    }

    record Pose(float inward, float lift, float nearer, float pitch, float roll, float armPitch, float armInward) {}

    String rig(String itemPath) {
        return switch (this) {
            case DRINK -> "drink";
            case SUPPLY -> "supply";
            case REPAIR -> "repair";
            default -> itemPath;
        };
    }

    float clipTime(float fraction, float ticks) {
        return this == REPAIR ? (ticks % 20) / 20 : fraction;
    }

    Pose sample(float progress, float ticks) {
        float reach = this == REPAIR ? ramp(ticks, 0, 6)
                : ramp(progress, 0, .22F) * (1 - ramp(progress, .79F, 1));
        float work = ramp(progress, .25F, .48F) * (1 - ramp(progress, .71F, .88F));
        float stroke = reach * (float)Math.sin(2 * Math.PI * (this == REPAIR ? ticks / 20 : progress * 3));
        return switch (this) {
            case BOTTLE -> new Pose(.04F*reach, .055F*reach, 0, -5*reach, -19*work, 41*reach, 23*reach);
            case DRINK -> new Pose(.21F*reach, .15F*reach+.064F*work, .18F*work, 12*reach+(48+4*stroke)*work, -8*reach, 59*reach+34*work, 27*reach);
            case INJECT -> new Pose(.12F*reach+.07F*work, .07F*reach-.04F*work, -.04F*work, -9*reach, -35*reach-26*work, 43*reach+12*work, 33*reach);
            case OE2 -> new Pose(.11F*reach+.08F*work, .08F*reach-.045F*work, -.05F*work, -14*reach, -18*reach-52*work, 47*reach+18*work, 37*reach);
            case WRAP -> new Pose(.10F*reach+.039F*stroke, .047F*reach+.029F*stroke, 0, -12*reach, 18*stroke, 46*reach+8*stroke, 33*reach);
            case TOURNIQUET -> new Pose(.11F*reach, .08F*reach, -.025F*work, -14*reach, 24*work, 55*reach, 39*reach);
            case SURGERY -> new Pose(.075F*reach, .047F*reach, -.03F*stroke, 16*reach, -11*work, 49*reach+5*stroke, 30*reach);
            case FIRST_AID -> new Pose(.085F*reach, .065F*reach-.019F*stroke, 0, 12*reach, 8*work, 54*reach-6*stroke, 25*reach);
            case SUPPLY -> new Pose(.07F*reach, .06F*reach, -.025F*work, 9*reach, -16*work, 52*reach, 22*reach);
            case REPAIR -> new Pose(.08F*reach, .045F*reach+.027F*stroke, -.024F*stroke, 12*reach, 10*stroke, 49*reach+9*stroke, 25*reach);
            case BLISTER -> throw new IllegalStateException("The blister uses its authored clip");
        };
    }
}
