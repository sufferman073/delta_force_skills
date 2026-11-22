package com.rzy.dealt_force_skills.feedback;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class SkillFeedbackEvents {
    private record Attack(net.minecraft.world.entity.Entity attacker, long tick) {}
    private static final Map<LivingEntity, Attack> ATTACKS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Set<String>> EFFECTS = new WeakHashMap<>();
    private SkillFeedbackEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getEntity() != null)
            ATTACKS.put(player, new Attack(event.getSource().getEntity(), player.level().getGameTime()));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator()) { EFFECTS.remove(player); return; }
        Set<String> previous = EFFECTS.getOrDefault(player, Set.of()), current = new HashSet<>();
        effect(player, previous, current, ModEffects.TEMPEST_DISARMED.get(), "disarmed");
        effect(player, previous, current, ModEffects.STINGER_STIM_SUPPRESSION.get(), "stim");
        effect(player, previous, current, ModEffects.TOXIK_FIREFLY_INTERFERENCE.get(), "firefly");
        effect(player, previous, current, ModEffects.VLINDER_MEDICAL_WASTE_INTERFERENCE.get(), "dust");
        effect(player, previous, current, ModEffects.N_TWO_FROZEN.get(), "frozen");
        effect(player, previous, current, ModEffects.RAPTOR_HUMMINGBIRD_MARKED.get(), "hummingbird");
        effect(player, previous, current, ModEffects.DEPARTMENT_CALIBRATION.get(), "calibration");
        downed(player, previous, current, ModEffects.STINGER_DOWNED.get(), "stinger", "professional_rescue");
        downed(player, previous, current, ModEffects.VLINDER_VITAL_DOWNED.get(), "vlinder", "vital_monitoring");
        downed(player, previous, current, ModEffects.ROVER_DOWNED.get(), "rover", "survivor");
        downed(player, previous, current, ModEffects.TEMPEST_EMERGENCY_DOWNED.get(), "tempest", "emergency_recall");
        downed(player, previous, current, ModEffects.CATDAD_DOWNED.get(), "catdad", "bully");
        if (com.rzy.dealt_force_skills.compat.PlayerReviveCompat.isTempestSelfRescueAllowed(player)) {
            current.add("downed.tempest");
            if (!previous.contains("downed.tempest")) notifyDowned(player, "tempest", "emergency_recall");
        }
        EFFECTS.put(player, current);
    }

    private static void effect(ServerPlayer player, Set<String> old, Set<String> current, MobEffect effect, String id) {
        if (!player.hasEffect(effect)) return;
        current.add(id);
        if (!old.contains(id) || player.tickCount % 10 == 0) {
            int duration = player.getEffect(effect).getDuration();
            SkillFeedback.phaseNotice(player, null, id, duration, duration);
        }
    }

    private static void downed(ServerPlayer player, Set<String> old, Set<String> current, MobEffect effect, String role, String skill) {
        String id = "downed." + role;
        if (!player.hasEffect(effect)) return;
        current.add(id);
        if (old.contains(id)) return;
        notifyDowned(player, role, skill);
    }

    private static void notifyDowned(ServerPlayer player, String role, String skill) {
        Attack attack = ATTACKS.get(player);
        var attacker = attack != null && player.level().getGameTime() - attack.tick <= 5 ? attack.attacker : null;
        if (attacker != player) SkillFeedback.notice(attacker, "self_rescue", role, role + ".skill." + skill);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        ATTACKS.remove(event.getEntity()); EFFECTS.remove(event.getEntity());
    }
}
