package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import static com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM;
import static org.lwjgl.glfw.GLFW.*;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindRegister {
    public static final String CAT = "key.categories.dealt_force_skills";
    public static KeyMapping CHARACTER_SELECT;
    public static KeyMapping ACTIVE_SKILL_1;
    public static KeyMapping ACTIVE_SKILL_2;
    public static KeyMapping CORE_SKILL;

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
