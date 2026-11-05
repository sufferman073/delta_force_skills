package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class OperativeGunInteractionHandler {
    private OperativeGunInteractionHandler() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.getAction() != GLFW.GLFW_PRESS) return;
        var mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.player.isSpectator() || mc.getConnection() == null
                || !(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof OperativeEntity bot)
                || !bot.isAlive() || !mc.player.getUUID().equals(bot.getOwnerUUID())
                || IGun.getIGunOrNull(mc.player.getMainHandItem()) == null
                || !mc.player.canInteractWithEntity(bot, 0)) return;
        // Use the ordinary server interaction, including ownership, downed and equipment checks.
        mc.getConnection().send(ServerboundInteractPacket.createInteractionPacket(bot, mc.player.isShiftKeyDown(), InteractionHand.MAIN_HAND));
        event.setCanceled(true);
        mc.options.keyUse.setDown(false);
        while (mc.options.keyUse.consumeClick()) { }
    }
}
