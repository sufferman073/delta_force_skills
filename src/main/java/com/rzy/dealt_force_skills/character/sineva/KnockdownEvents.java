package com.rzy.dealt_force_skills.character.sineva;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class KnockdownEvents {
    private KnockdownEvents() { }
    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            var size = SinevaKnockdownState.dimensions(entity);
            if (size != null) { event.setNewSize(size); event.setNewEyeHeight(size.height * .8F); }
        }
    }
    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity target)
            SinevaKnockdownState.tracking(viewer, target);
    }
    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity entity) SinevaKnockdownState.clear(entity);
    }
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) { SinevaKnockdownState.clear(event.getOriginal()); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { SinevaKnockdownState.clear(event.getEntity()); }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { SinevaKnockdownState.clear(event.getEntity()); }
    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { SinevaKnockdownState.clearSide(false); }
}
