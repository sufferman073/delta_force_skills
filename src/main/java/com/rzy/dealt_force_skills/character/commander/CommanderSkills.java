package com.rzy.dealt_force_skills.character.commander;

import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.skill.SkillDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CommanderSkills {
    private CommanderSkills() {
    }

    public static boolean useSkill(ServerPlayer player, SkillSlot slot, boolean alternate) {
        CommanderStateManager.initializeIfNeeded(player);
        var source = CommanderStateManager.sourceForSkill(player, slot, alternate);
        if (source.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.skill_not_selected"), true);
            return false;
        }
        boolean inheritedAlternate = slot == SkillSlot.CORE ? false : alternate;
        return SkillDispatcher.useInheritedSkill(player, source.get(), slot, inheritedAlternate);
    }

    public static boolean useSelectedSkill(ServerPlayer player, CommanderSkillChoice choice,
                                           boolean inheritedAlternate) {
        CommanderStateManager.initializeIfNeeded(player);
        if (!CommanderStateManager.isCommander(player) || choice == null
                || choice.skillSlot() == SkillSlot.PASSIVE) {
            return false;
        }
        var source = CommanderStateManager.selectedSource(player, choice);
        if (source.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.dealt_force_skills.commander.skill_not_selected"), true);
            return false;
        }
        boolean handled = SkillDispatcher.useInheritedSkill(
                player, source.get(), choice.skillSlot(), inheritedAlternate);
        CommanderStateManager.syncToClient(player);
        return handled;
    }
}