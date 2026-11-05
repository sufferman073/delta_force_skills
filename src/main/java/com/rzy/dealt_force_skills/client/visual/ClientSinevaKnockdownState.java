package com.rzy.dealt_force_skills.client.visual;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.sineva.SinevaKnockdownState;
import net.minecraft.world.entity.LivingEntity;
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

    private static Object level;
    private static void checkLevel() {
        Object current = Minecraft.getInstance().level;
        if (level != current) { reset(); level = current; }
    }

    public static void sync(int entityId, int ticks, float yaw) {
        checkLevel();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        KnockdownVisual old = ACTIVE.remove(entityId);
        if (old != null && old.entity != null) SinevaKnockdownState.clear(old.entity);
        if (ticks <= 0 || !Float.isFinite(yaw)) return;
        KnockdownVisual visual = new KnockdownVisual(ticks, yaw);
        ACTIVE.put(entityId, visual);
        if (mc.level.getEntity(entityId) instanceof LivingEntity entity) {
            visual.entity=entity;
            SinevaKnockdownState.begin(entity, ticks, yaw);
        }
    }

    public static void tick(Minecraft minecraft) {
        checkLevel();
        if (minecraft.level == null) return;
        Iterator<Map.Entry<Integer, KnockdownVisual>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry=iterator.next(); KnockdownVisual visual=entry.getValue();
            Entity entity=minecraft.level.getEntity(entry.getKey());
            visual.ageTicks++; visual.remainingTicks--;
            if (visual.entity == null && entity instanceof LivingEntity living && visual.remainingTicks > 0) {
                visual.entity=living;
                SinevaKnockdownState.begin(living, visual.remainingTicks, visual.yaw);
            }
            if (visual.remainingTicks <= 0 || visual.ageTicks > 20 && visual.entity == null
                    || visual.entity != null && (visual.entity != entity || !visual.entity.isAlive()
                    || !SinevaKnockdownState.active(visual.entity))) {
                if (visual.entity != null) SinevaKnockdownState.clear(visual.entity);
                iterator.remove();
            }
        }
    }

    public static void reset() {
        SinevaKnockdownState.clearSide(true);
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
        private LivingEntity entity;
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
