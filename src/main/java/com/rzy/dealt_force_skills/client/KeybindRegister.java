package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.client.character.ClientCharacterSelectionState;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM;
import static org.lwjgl.glfw.GLFW.*;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindRegister {
    public static final String CAT = "key.categories.dealt_force_skills";
    public static KeyMapping CHARACTER_SELECT;
    public static KeyMapping ACTIVE_SKILL_1;
    public static KeyMapping ACTIVE_SKILL_2;
    public static KeyMapping CORE_SKILL;
    private static final java.util.Set<String> REVERSED = java.util.Set.of(
            ModCharacters.VYRON_ID, ModCharacters.D_WOLF_ID, ModCharacters.NOX_ID,
            ModCharacters.WILDFIRE_ID, ModCharacters.TEMPEST_ID, ModCharacters.STINGER_ID,
            ModCharacters.TOXIK_ID, ModCharacters.VLINDER_ID, ModCharacters.SHEPHERD_ID,
            ModCharacters.LUNA_ID, ModCharacters.MORSE_ID, ModCharacters.RAPTOR_ID);

    public static boolean reversedActives() {
        String selected = ClientCharacterSelectionState.selectedCharacterId();
        return selected != null && REVERSED.contains(selected);
    }

    public static KeyMapping activeKey(int originalSlot) {
        return (originalSlot == 1) != reversedActives() ? ACTIVE_SKILL_1 : ACTIVE_SKILL_2;
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        CHARACTER_SELECT = new KeyMapping("key.dealt_force_skills.character_select", KEYSYM, GLFW_KEY_K, CAT);
        ACTIVE_SKILL_1 = new KeyMapping("key.dealt_force_skills.active_skill_1", KEYSYM, GLFW_KEY_Z, CAT);
        ACTIVE_SKILL_2 = new KeyMapping("key.dealt_force_skills.active_skill_2", KEYSYM, GLFW_KEY_X, CAT);
        CORE_SKILL = new KeyMapping("key.dealt_force_skills.core_skill", KEYSYM, GLFW_KEY_C, CAT);
        e.register(CHARACTER_SELECT);
        e.register(ACTIVE_SKILL_1);
        e.register(ACTIVE_SKILL_2);
        e.register(CORE_SKILL);
    }
}
