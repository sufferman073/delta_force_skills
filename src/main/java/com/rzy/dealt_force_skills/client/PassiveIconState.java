package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.client.character.*;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;

/** Continuous passive states come from existing synchronization; instantaneous ones use a server pulse. */
final class PassiveIconState {
    private PassiveIconState() {}
    static String detail() {
        String id = ClientCharacterSelectionState.displayedCharacterId();
        String role = id.substring(id.lastIndexOf('/') + 1);
        return switch (role) {
            case "corps" -> Integer.toString(ClientCorpsHudState.loyaltyOrangeStacks());
            case "nikaidou_hiro" -> ClientNikaidouHiroHudState.riftStacks() + (ClientNikaidouHiroHudState.riftTicks() > 0 ? " / " + seconds(ClientNikaidouHiroHudState.riftTicks()) : "");
            case "department_of_transportation" -> Integer.toString(ClientDepartmentHudState.concealed() ? ClientDepartmentHudState.reductionSteps() : ClientDepartmentHudState.vulnerabilityStacks());
            case "stinger" -> seconds(ClientStingerHudState.rescueCooldownTicks());
            case "vlinder" -> seconds(ClientVlinderHudState.rescueProtectionTicks());
            case "vyron" -> seconds(ClientVyronHudState.poweredTicks());
            case "gambler" -> seconds(ClientGamblerHudState.invulnerableTicks());
            case "tempest" -> {
                var player = Minecraft.getInstance().player;
                var effect = player == null ? null : player.getEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
                yield effect == null ? "" : seconds(effect.getDuration());
            }
            default -> "";
        };
    }

    private static String seconds(int ticks) {
        return ticks > 0 ? (ticks + 19) / 20 + "s" : "";
    }

    static SkillIcons.State resolve() {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.isAlive()) return SkillIcons.State.DISABLED;
        String id = ClientCharacterSelectionState.displayedCharacterId();
        String role = id.substring(id.lastIndexOf('/') + 1);
        boolean available = switch (role) {
            case "sineva" -> !ClientSinevaHudState.shieldDeployed();
            case "stinger" -> ClientStingerHudState.rescueCooldownTicks() <= 0;
            case "lex_ninjia" -> ClientLexNinjiaHudState.leicra() > 0;
            case "gambler" -> true;
            default -> true;
        };
        boolean active = switch (role) {
            case "hackclaw" -> p.getDeltaMovement().lengthSqr() > .0001;
            case "morse" -> ClientMorseHudState.worldMarkers().stream().anyMatch(marker -> marker.type() == com.rzy.dealt_force_skills.character.morse.MorseMarkerType.SOUND_SOURCE);
            case "raptor" -> !ClientRaptorHudState.footprints().isEmpty();
            case "n_two" -> !ClientNTwoHudState.coldMarkers().isEmpty();
            case "toxik" -> true;
            case "ghroth" -> ClientGhrothState.tacticalImmune();
            case "gizmo" -> ClientGizmoHudState.passiveBoost();
            case "d_wolf" -> ClientDWolfHudState.slideTicks() > 0;
            case "vyron" -> ClientVyronHudState.poweredTicks() > 0;
            case "tempest" -> p.hasEffect(ModEffects.TEMPEST_EXPLOSIVE_SPINE);
            case "stinger" -> ClientStingerHudState.rescueTicks() > 0;
            case "vlinder" -> ClientVlinderHudState.rescueTicks() > 0 || ClientVlinderHudState.rescueProtectionTicks() > 0;
            case "rover" -> RoverClient.hudValue("Rescue") > 0;
            case "corps" -> ClientCorpsHudState.loyaltyOrangeStacks() > 0;
            case "nikaidou_hiro" -> ClientNikaidouHiroHudState.riftStacks() > 0;
            case "nikaidou_hiro_witchification" -> p.getHealth() < p.getMaxHealth();
            case "gambler" -> ClientGamblerHudState.invulnerableTicks() > 0;
            case "department_of_transportation" -> ClientDepartmentHudState.concealed();
            case "wildfire" -> WildfireClient.local().getBoolean("Passive");
            default -> false;
        };
        return active ? SkillIcons.State.ACTIVE : available ? SkillIcons.State.NORMAL : SkillIcons.State.DISABLED;
    }
}
