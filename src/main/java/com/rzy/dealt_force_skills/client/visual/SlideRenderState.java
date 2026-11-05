package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.motion.TacticalMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Separate the slide's low collision pose from animation inputs, only while drawing it. */
public final class SlideRenderState {
    private static final ThreadLocal<Entity> SLIDING = new ThreadLocal<>();
    private SlideRenderState() { }

    public static boolean active(Entity entity) {
        return entity != null && SLIDING.get() == entity;
    }

    public static void render(Entity entity, float partial, Runnable render) {
        Entity previous = SLIDING.get();
        boolean slide = false;
        if (entity instanceof Player player) {
            var frame = ClientTacticalMotion.frame(player, partial);
            slide = frame != null && frame.kind() == TacticalMotion.SLIDE && frame.pose().weight > 0;
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
