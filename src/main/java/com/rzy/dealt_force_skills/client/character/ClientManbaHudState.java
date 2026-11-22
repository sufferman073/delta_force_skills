package com.rzy.dealt_force_skills.client.character;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.manba.ManbaBattery;
import com.rzy.dealt_force_skills.character.manba.ManbaBulb;
import com.rzy.dealt_force_skills.character.manba.ManbaFlashlightStats;
import com.rzy.dealt_force_skills.character.manba.ManbaLens;
import com.rzy.dealt_force_skills.character.manba.ManbaOpportunityMarker;
import com.rzy.dealt_force_skills.character.manba.ManbaTalent;
import com.rzy.dealt_force_skills.client.screen.ManbaLoadoutScreen;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ClientManbaHudState {
    private static boolean synced;
    private static int elbowCharges;
    private static int elbowMaxCharges;
    private static int elbowRechargeTicks;
    private static int flashlightDurability;
    private static int flashlightMaxDurability;
    private static boolean flashlightActive;
    private static int coreCooldownTicks;
    private static boolean duelActive;
    private static int duelAffection;
    private static int duelRemainingTicks;
    private static boolean configured;
    private static int[] talents = new int[0];
    private static int[] talentCooldowns = new int[0];
    private static int selfTaughtStacks;
    private static int[] talentDurations = new int[0];
    private static int talentActiveMask;
    private static int[] bulbs = new int[0];
    private static int lens;
    private static int[] batteries = new int[0];
    private static boolean loadoutScreenOpened;
    private static final Map<Integer, ProgressEntry> FLASH_PROGRESS = new HashMap<>();
    private static List<ManbaOpportunityMarker> opportunityMarkers = List.of();
    private static DuelView duelView = DuelView.inactive();

    private ClientManbaHudState() {
    }

    public static void reset() {
        synced = false;
        elbowCharges = 0;
        elbowMaxCharges = 0;
        elbowRechargeTicks = 0;
        flashlightDurability = 0;
        flashlightMaxDurability = 0;
        flashlightActive = false;
        coreCooldownTicks = 0;
        duelActive = false;
        duelAffection = 0;
        duelRemainingTicks = 0;
        configured = false;
        talents = new int[0];
        talentCooldowns = new int[0];
        selfTaughtStacks = 0;
        talentDurations = new int[0];
        talentActiveMask = 0;
        bulbs = new int[0];
        lens = 0;
        batteries = new int[0];
        loadoutScreenOpened = false;
        FLASH_PROGRESS.clear();
        opportunityMarkers = List.of();
        duelView = DuelView.inactive();
    }

    public static void sync(int elbowCharges, int elbowMaxCharges, int elbowRechargeTicks,
                            int flashlightDurability, int flashlightMaxDurability, boolean flashlightActive,
                            int coreCooldownTicks, boolean duelActive, int duelAffection, int duelRemainingTicks,
                            boolean configured, int[] talents, int[] talentCooldowns, int selfTaughtStacks,
                            int[] bulbs, int lens, int[] batteries) {
        synced = true;
        boolean wasConfigured = ClientManbaHudState.configured;
        ClientManbaHudState.elbowCharges = elbowCharges;
        ClientManbaHudState.elbowMaxCharges = elbowMaxCharges;
        ClientManbaHudState.elbowRechargeTicks = elbowRechargeTicks;
        ClientManbaHudState.flashlightDurability = flashlightDurability;
        ClientManbaHudState.flashlightMaxDurability = flashlightMaxDurability;
        ClientManbaHudState.flashlightActive = flashlightActive;
        ClientManbaHudState.coreCooldownTicks = coreCooldownTicks;
        ClientManbaHudState.duelActive = duelActive;
        ClientManbaHudState.duelAffection = duelAffection;
        ClientManbaHudState.duelRemainingTicks = duelRemainingTicks;
        ClientManbaHudState.configured = configured;
        ClientManbaHudState.talents = talents == null ? new int[0] : talents;
        ClientManbaHudState.talentCooldowns = talentCooldowns == null ? new int[0] : talentCooldowns;
        ClientManbaHudState.selfTaughtStacks = selfTaughtStacks;
        ClientManbaHudState.bulbs = bulbs == null ? new int[0] : bulbs;
        ClientManbaHudState.lens = lens;
        ClientManbaHudState.batteries = batteries == null ? new int[0] : batteries;
        if (!containsTalent(ManbaTalent.OPPORTUNITY_WINDOW)) {
            opportunityMarkers = List.of();
        }
        if (wasConfigured && !configured) {
            loadoutScreenOpened = false;
        }
    }

    public static void tick() {
        if (elbowRechargeTicks > 0) {
            elbowRechargeTicks--;
        }
        if (coreCooldownTicks > 0) {
            coreCooldownTicks--;
        }
        if (duelRemainingTicks > 0) {
            duelRemainingTicks--;
        }
        if (duelView.remainingTicks > 0) {
            duelView = new DuelView(duelView.active, duelView.ownerEntityId, duelView.targetEntityId,
                    duelView.ownerName, duelView.targetName, duelView.affection, duelView.remainingTicks - 1);
        }
        for (int i = 0; i < talentCooldowns.length; i++) if (talentCooldowns[i] > 0) talentCooldowns[i]--;
        for (int i = 0; i < talentDurations.length; i++) if (talentDurations[i] > 0) talentDurations[i]--;
        tickFlashProgress();
        openLoadoutIfNeeded();
    }

    public static boolean shouldRender() {
        return synced && ClientCharacterSelectionState.isSelectedCharacter(ModCharacters.MANBA_ID);
    }

    public static boolean shouldDisplay() {
        return synced && ClientCharacterSelectionState.isDisplayedCharacter(ModCharacters.MANBA_ID);
    }

    public static boolean flashlightActive() {
        return shouldRender() && flashlightActive;
    }

    public static boolean configured() {
        return configured;
    }

    public static int elbowCharges() {
        return elbowCharges;
    }

    public static int elbowMaxCharges() {
        return elbowMaxCharges;
    }

    public static int elbowRechargeTicks() {
        return elbowRechargeTicks;
    }

    public static int flashlightDurability() {
        return flashlightDurability;
    }

    public static int flashlightMaxDurability() {
        return flashlightMaxDurability;
    }

    public static int coreCooldownTicks() {
        return coreCooldownTicks;
    }

    public static boolean duelActive() {
        return duelActive;
    }

    public static int duelAffection() {
        return duelAffection;
    }

    public static int duelRemainingTicks() {
        return duelRemainingTicks;
    }

    public static int selfTaughtStacks() {
        return selfTaughtStacks;
    }

    public static void syncTalentDetails(int[] durations, int activeMask) {
        talentDurations = Arrays.copyOf(durations, durations.length);
        talentActiveMask = activeMask;
    }

    public static int talentDuration(int ordinal) {
        return ordinal >= 0 && ordinal < talentDurations.length ? talentDurations[ordinal] : 0;
    }

    public static boolean talentActive(int ordinal) {
        return ordinal >= 0 && ordinal < 32 && (talentActiveMask & (1 << ordinal)) != 0;
    }

    public static int[] talents() {
        return Arrays.copyOf(talents, talents.length);
    }

    public static boolean hasTalent(ManbaTalent talent) {
        return shouldRender() && containsTalent(talent);
    }

    private static boolean containsTalent(ManbaTalent talent) {
        if (talent == null) {
            return false;
        }
        int ordinal = talent.ordinal();
        for (int selected : talents) {
            if (selected == ordinal) {
                return true;
            }
        }
        return false;
    }

    public static void syncOpportunityMarkers(List<ManbaOpportunityMarker> markers) {
        opportunityMarkers = markers == null ? List.of() : List.copyOf(markers);
    }

    public static List<ManbaOpportunityMarker> opportunityMarkers() {
        return opportunityMarkers;
    }

    public static int[] talentCooldowns() {
        return Arrays.copyOf(talentCooldowns, talentCooldowns.length);
    }

    public static int[] bulbs() {
        return Arrays.copyOf(bulbs, bulbs.length);
    }

    public static int lens() {
        return lens;
    }

    public static int[] batteries() {
        return Arrays.copyOf(batteries, batteries.length);
    }

    public static ManbaFlashlightStats flashlightStats() {
        return ManbaFlashlightStats.of(
                enumSet(ManbaBulb.class, bulbs),
                enumValue(ManbaLens.class, lens, ManbaLens.PURPLE_LENS),
                enumSet(ManbaBattery.class, batteries)
        );
    }

    public static Map<Integer, Integer> flashlightProgress() {
        Map<Integer, Integer> copy = new HashMap<>();
        for (Map.Entry<Integer, ProgressEntry> entry : FLASH_PROGRESS.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().progress);
        }
        return copy;
    }

    public static int highestFlashlightProgress() {
        int best = 0;
        for (ProgressEntry entry : FLASH_PROGRESS.values()) {
            best = Math.max(best, entry.progress);
        }
        return best;
    }

    public static DuelView duelView() {
        return duelView;
    }

    public static boolean duelViewActive() {
        return duelView.active && duelView.remainingTicks > 0;
    }

    public static boolean localPlayerIsDuelOwner() {
        Minecraft minecraft = Minecraft.getInstance();
        return duelViewActive() && minecraft.player != null && minecraft.player.getId() == duelView.ownerEntityId;
    }

    public static boolean localPlayerIsDuelTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        return duelViewActive() && minecraft.player != null && minecraft.player.getId() == duelView.targetEntityId;
    }

    public static void updateFlashlightProgress(int entityId, int progressPercent, int ticks) {
        if (progressPercent <= 0 || ticks <= 0) {
            FLASH_PROGRESS.remove(entityId);
            return;
        }
        FLASH_PROGRESS.put(entityId, new ProgressEntry(Math.min(100, Math.max(0, progressPercent)), ticks));
    }

    public static void syncDuelView(boolean active, int ownerEntityId, int targetEntityId, String ownerName,
                                    String targetName, int affection, int remainingTicks) {
        duelView = new DuelView(active, ownerEntityId, targetEntityId, ownerName, targetName, affection, remainingTicks);
    }

    public static String talentName(int ordinal) {
        ManbaTalent[] values = ManbaTalent.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].displayName() : "?";
    }

    public static String bulbName(int ordinal) {
        ManbaBulb[] values = ManbaBulb.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].displayName() : "?";
    }

    public static String lensName(int ordinal) {
        ManbaLens[] values = ManbaLens.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].displayName() : "?";
    }

    public static String batteryName(int ordinal) {
        ManbaBattery[] values = ManbaBattery.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].displayName() : "?";
    }

    private static void openLoadoutIfNeeded() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldRender() || configured || loadoutScreenOpened || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        loadoutScreenOpened = true;
        minecraft.setScreen(new ManbaLoadoutScreen());
    }

    private static void tickFlashProgress() {
        Iterator<Map.Entry<Integer, ProgressEntry>> iterator = FLASH_PROGRESS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ProgressEntry> entry = iterator.next();
            ProgressEntry value = entry.getValue();
            value.ticks--;
            if (value.ticks <= 0) {
                iterator.remove();
            }
        }
    }

    private static <E extends Enum<E>> EnumSet<E> enumSet(Class<E> type, int[] ordinals) {
        EnumSet<E> set = EnumSet.noneOf(type);
        E[] values = type.getEnumConstants();
        for (int ordinal : ordinals) {
            if (ordinal >= 0 && ordinal < values.length) {
                set.add(values[ordinal]);
            }
        }
        return set;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, int ordinal, E fallback) {
        E[] values = type.getEnumConstants();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    private static final class ProgressEntry {
        private final int progress;
        private int ticks;

        private ProgressEntry(int progress, int ticks) {
            this.progress = progress;
            this.ticks = ticks;
        }
    }

    public record DuelView(boolean active, int ownerEntityId, int targetEntityId, String ownerName,
                           String targetName, int affection, int remainingTicks) {
        private static DuelView inactive() {
            return new DuelView(false, -1, -1, "", "", 0, 0);
        }
    }
}
