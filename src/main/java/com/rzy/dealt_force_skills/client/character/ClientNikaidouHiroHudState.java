package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.nikaidou.NikaidouHiroTool;

public final class ClientNikaidouHiroHudState {
    private static boolean synced;
    private static int riftStacks;
    private static int riftTicks;
    private static int active1CooldownTicks;
    private static int active1Ticks;
    private static NikaidouHiroTool equippedTool = NikaidouHiroTool.NONE;
    private static int coreCooldownTicks;
    private static boolean coreActive;
    private static int coreActiveTicks;
    private static int doomedTicks;
    private static int attackCooldownTicks;
    private static int witchActive2CooldownTicks;
    private static int witchCorePhase;
    private static int witchCoreLockTicks;
    private static boolean anchorMarkerActive;
    private static String anchorMarkerDimension = "";
    private static double anchorMarkerX;
    private static double anchorMarkerY;
    private static double anchorMarkerZ;

    private ClientNikaidouHiroHudState() {
    }

    public static void reset() {
        synced = false;
        riftStacks = 0;
        riftTicks = 0;
        active1CooldownTicks = 0;
        active1Ticks = 0;
        equippedTool = NikaidouHiroTool.NONE;
        coreCooldownTicks = 0;
        coreActive = false;
        coreActiveTicks = 0;
        doomedTicks = 0;
        attackCooldownTicks = 0;
        witchActive2CooldownTicks = witchCorePhase = witchCoreLockTicks = 0;
        anchorMarkerActive = false;
        anchorMarkerDimension = "";
        anchorMarkerX = 0.0D;
        anchorMarkerY = 0.0D;
        anchorMarkerZ = 0.0D;
    }

    public static void sync(int riftStacks, int riftTicks, int active1CooldownTicks, int active1Ticks,
                            int equippedToolOrdinal, int coreCooldownTicks, boolean coreActive,
                            int coreActiveTicks, int doomedTicks, int attackCooldownTicks) {
        sync(riftStacks, riftTicks, active1CooldownTicks, active1Ticks, equippedToolOrdinal, coreCooldownTicks,
                coreActive, coreActiveTicks, doomedTicks, attackCooldownTicks, false, "", 0.0D, 0.0D, 0.0D);
    }

    public static void sync(int riftStacks, int riftTicks, int active1CooldownTicks, int active1Ticks,
                            int equippedToolOrdinal, int coreCooldownTicks, boolean coreActive,
                            int coreActiveTicks, int doomedTicks, int attackCooldownTicks,
                            boolean anchorMarkerActive, String anchorMarkerDimension,
                            double anchorMarkerX, double anchorMarkerY, double anchorMarkerZ) {
        synced = true;
        ClientNikaidouHiroHudState.riftStacks = riftStacks;
        ClientNikaidouHiroHudState.riftTicks = riftTicks;
        ClientNikaidouHiroHudState.active1CooldownTicks = active1CooldownTicks;
        ClientNikaidouHiroHudState.active1Ticks = active1Ticks;
        NikaidouHiroTool[] tools = NikaidouHiroTool.values();
        ClientNikaidouHiroHudState.equippedTool = equippedToolOrdinal >= 0 && equippedToolOrdinal < tools.length
                ? tools[equippedToolOrdinal]
                : NikaidouHiroTool.NONE;
        ClientNikaidouHiroHudState.coreCooldownTicks = coreCooldownTicks;
        ClientNikaidouHiroHudState.coreActive = coreActive;
        ClientNikaidouHiroHudState.coreActiveTicks = coreActiveTicks;
        ClientNikaidouHiroHudState.doomedTicks = doomedTicks;
        ClientNikaidouHiroHudState.attackCooldownTicks = attackCooldownTicks;
        ClientNikaidouHiroHudState.anchorMarkerActive = anchorMarkerActive;
        ClientNikaidouHiroHudState.anchorMarkerDimension = anchorMarkerDimension == null ? "" : anchorMarkerDimension;
        ClientNikaidouHiroHudState.anchorMarkerX = anchorMarkerX;
        ClientNikaidouHiroHudState.anchorMarkerY = anchorMarkerY;
        ClientNikaidouHiroHudState.anchorMarkerZ = anchorMarkerZ;
    }

    public static void syncWitchDetails(int cooldown, int phase, int lockTicks) {
        witchActive2CooldownTicks = Math.max(0, cooldown);
        witchCorePhase = phase;
        witchCoreLockTicks = Math.max(0, lockTicks);
    }

    public static int witchActive2CooldownTicks() { return witchActive2CooldownTicks; }
    public static int witchCorePhase() { return witchCorePhase; }
    public static int witchCoreDisplayTicks() {
        if (witchCorePhase == 4) return doomedTicks;
        if (witchCorePhase == 2) return witchCoreLockTicks;
        return coreActiveTicks;
    }

    public static void tick() {
        if (witchActive2CooldownTicks > 0) witchActive2CooldownTicks--;
        if (witchCoreLockTicks > 0) witchCoreLockTicks--;
        if (!isWitchificationDisplayed() && riftTicks > 0) riftTicks--;
        if (active1CooldownTicks > 0) active1CooldownTicks--;
        if (active1Ticks > 0) active1Ticks--;
        if (coreCooldownTicks > 0) coreCooldownTicks--;
        if (coreActive) coreActiveTicks++;
        if (doomedTicks > 0) doomedTicks--;
        if (attackCooldownTicks > 0) attackCooldownTicks--;
    }

    public static boolean shouldRender() {
        return synced && (ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.NIKAIDOU_HIRO_ID)
                || ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID));
    }

    public static boolean shouldDisplay() {
        return synced && (ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.NIKAIDOU_HIRO_ID)
                || ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID));
    }

    public static boolean isWitchificationDisplayed() {
        return ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID);
    }

    public static boolean hasEquippedTool() {
        return shouldRender() && equippedTool != NikaidouHiroTool.NONE;
    }

    public static boolean hasAnchorMarker() {
        return shouldRender() && anchorMarkerActive && !anchorMarkerDimension.isEmpty();
    }

    public static int riftStacks() { return riftStacks; }
    public static int riftTicks() { return riftTicks; }
    public static int active1CooldownTicks() { return active1CooldownTicks; }
    public static int active1Ticks() { return active1Ticks; }
    public static NikaidouHiroTool equippedTool() { return equippedTool; }
    public static int coreCooldownTicks() { return coreCooldownTicks; }
    public static boolean coreActive() { return coreActive; }
    public static int coreActiveTicks() { return coreActiveTicks; }
    public static int doomedTicks() { return doomedTicks; }
    public static int attackCooldownTicks() { return attackCooldownTicks; }
    public static String anchorMarkerDimension() { return anchorMarkerDimension; }
    public static double anchorMarkerX() { return anchorMarkerX; }
    public static double anchorMarkerY() { return anchorMarkerY; }
    public static double anchorMarkerZ() { return anchorMarkerZ; }
}
