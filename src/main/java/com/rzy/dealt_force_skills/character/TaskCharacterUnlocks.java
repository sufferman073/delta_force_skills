package com.rzy.dealt_force_skills.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Set;

/** Task rewards grant access without editing an administrator's bans or explicit allows. */
public final class TaskCharacterUnlocks {
    private static final String KEY = "dealt_force_skills.task_character_unlocks";
    public static final Set<String> REWARDED = Set.of(
            ModCharacters.TEMPEST_ID, ModCharacters.VYRON_ID, ModCharacters.NOX_ID, ModCharacters.VLINDER_ID,
            ModCharacters.ROVER_ID, ModCharacters.TOXIK_ID, ModCharacters.GIZMO_ID, ModCharacters.ULURU_ID,
            ModCharacters.N_TWO_ID, ModCharacters.SINEVA_ID, ModCharacters.HACKCLAW_ID, ModCharacters.RAPTOR_ID,
            ModCharacters.MORSE_ID, ModCharacters.CORPS_ID, ModCharacters.MANBA_ID, ModCharacters.UNDEAD_ID,
            ModCharacters.LEX_NINJIA_ID, ModCharacters.GAMBLER_ID, ModCharacters.NIKAIDOU_HIRO_WITCHIFICATION_ID,
            ModCharacters.DEPARTMENT_OF_TRANSPORTATION_ID, ModCharacters.SAEED_ID, ModCharacters.GHROTH_ID,
            ModCharacters.COMMANDER_ID);
    private TaskCharacterUnlocks() { }

    public static boolean isUnlocked(Player player, String id) {
        return player.getPersistentData().getCompound(KEY).getBoolean(id);
    }

    public static boolean isLocked(ServerPlayer player, String id) {
        return !player.isCreative() && REWARDED.contains(id) && !isUnlocked(player,id)
                && !CharacterBanManager.isPlayerAllowed(player,id)
                && !CharacterBanManager.isServerAllowed(player.getServer(),id);
    }

    public static boolean unlock(ServerPlayer player, String id) {
        if (!REWARDED.contains(id) || isUnlocked(player,id)) return false;
        CompoundTag data=player.getPersistentData();
        if(!data.contains(KEY,Tag.TAG_COMPOUND))data.put(KEY,new CompoundTag());
        data.getCompound(KEY).putBoolean(id,true);
        return true;
    }

    public static void copy(Player from,Player to) {
        if(from.getPersistentData().contains(KEY,Tag.TAG_COMPOUND))
            to.getPersistentData().put(KEY,from.getPersistentData().getCompound(KEY).copy());
    }
}
