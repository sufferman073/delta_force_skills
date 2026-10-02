package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class ClientSinevaKnockdownState {
    private static final int FADE_TICKS = 6;
    private static final Map<Integer, KnockdownVisual> ACTIVE = new HashMap<>();

    private ClientSinevaKnockdownState() {
    }

    public static void sync(int entityId, int ticks, float yaw) {
        if (ticks <= 0) {
            ACTIVE.remove(entityId);
            return;
        }
        ACTIVE.put(entityId, new KnockdownVisual(ticks, yaw));
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            ACTIVE.clear();
            return;
        }

        Iterator<Map.Entry<Integer, KnockdownVisual>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, KnockdownVisual> entry = iterator.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }
            if (entity == minecraft.player && !minecraft.player.hasEffect(ModEffects.STUN)) {
                iterator.remove();
                continue;
            }

            KnockdownVisual visual = entry.getValue();
            visual.ageTicks++;
            visual.remainingTicks--;
            if (visual.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    public static void reset() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        KnockdownVisual visual = ACTIVE.get(minecraft.player.getId());
        if (visual == null) {
            return;
        }

        float fade = visual.fade((float) com.rzy.dealt_force_skills.client.ClientRenderCompat.partialTick(event.getPartialTick()));
        event.setYaw(visual.yaw);
        event.setPitch(Mth.clamp(event.getPitch(), -18.0F, 18.0F) * (1.0F - 0.65F * fade));
        event.setRoll(event.getRoll() + 90.0F * fade);
    }

    private static final class KnockdownVisual {
        private final float yaw;
        private int remainingTicks;
        private int ageTicks;

        private KnockdownVisual(int ticks, float yaw) {
            this.remainingTicks = ticks;
            this.yaw = yaw;
        }

        private float fade(float partialTick) {
            float fadeIn = Math.min(1.0F, (ageTicks + partialTick) / FADE_TICKS);
            float fadeOut = Math.min(1.0F, (remainingTicks - partialTick) / FADE_TICKS);
            float value = Mth.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
            return value * value * (3.0F - 2.0F * value);
        }
    }
}
