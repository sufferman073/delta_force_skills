package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.motion.TacticalMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Keep the slide's low collision pose separate from swimming animation inputs. */
public final class SlideRenderState {
    private static final ThreadLocal<Entity> SLIDING = new ThreadLocal<>();
    private SlideRenderState() { }

    public static boolean active(Entity entity) {
        return entity != null && SLIDING.get() == entity;
    }

    public static boolean suppressSwimming(Entity entity) {
        if (entity == null || !entity.level().isClientSide || entity.isInWater()) return false;
        if (active(entity)) return true;
        if (entity instanceof Player player) {
            // Gun animations can sample swimming before the renderer is entered.
            if (player == net.minecraft.client.Minecraft.getInstance().player
                    && com.rzy.dealt_force_skills.client.DWolfInputHandler.isSlideActiveForVisual()) return true;
            var frame = ClientTacticalMotion.frame(player, 0);
            return frame != null && frame.kind() == TacticalMotion.SLIDE;
        }
        return false;
    }

    public static void render(Entity entity, float partial, Runnable render) {
        Entity previous = SLIDING.get();
        boolean slide = false;
        if (entity instanceof Player player) {
            var frame = ClientTacticalMotion.frame(player, partial);
            // A zero-weight entry frame already has the low collision pose.
            slide = frame != null && frame.kind() == TacticalMotion.SLIDE;
        }
        if (entity instanceof com.rzy.dealt_force_skills.entity.OperativeEntity bot) {
            var frame = ClientOperativeMotion.frame(bot, partial);
            slide = frame != null && frame.pose().weight > 0 && (frame.kind() == TacticalMotion.SLIDE
                    || frame.kind() == TacticalMotion.ROLL || frame.kind() == TacticalMotion.DIVE);
        }
        if (slide) SLIDING.set(entity);
        else SLIDING.remove();
        try {
            render.run();
        } finally {
            // Covers cancelled renders, exceptions, and nested entity/preview rendering.
            if (previous == null) SLIDING.remove();
            else SLIDING.set(previous);
        }
    }
}
