package com.rzy.dealt_force_skills.team;

import com.rzy.dealt_force_skills.config.DealtForceConfig;
import com.rzy.dealt_force_skills.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks the short "round just started" window during which a dealtmatch player cannot move,
 * attack, fire, or use active character skills, while still being free to buy items, use
 * consumables, wear armor, and switch fire modes. Backed by a client-synced
 * {@link ModEffects#ROUND_START_FREEZE} MobEffect rather than a server-only NBT timestamp,
 * because a real {@code ServerPlayer}'s position is client-authoritative: only a client-visible
 * signal lets {@code RoundStartFreezeInputHandler} actually stop the player from walking (see
 * that class for the input-cancelling side of this feature). Deliberately independent from the
 * "downed" persistent-state family (see {@code StingerStateManager#isDowned}) because that
 * family also blocks GUIs/item use, which this feature must not touch.
 */
public final class RoundStartFreezeManager {
    public static volatile int FREEZE_DURATION_TICKS = com.rzy.dealt_force_skills.config.DealtForceConfig.bind("FREEZE_DURATION_TICKS", () -> com.rzy.dealt_force_skills.config.DealtForceConfig.intValue("match.round_start_freeze.duration_ticks", 500));
    private RoundStartFreezeManager() {
    }

    public static void beginFreeze(ServerPlayer player) {
        if (FREEZE_DURATION_TICKS <= 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.ROUND_START_FREEZE,
                FREEZE_DURATION_TICKS, 0, false, false, true));
        player.displayClientMessage(Component.translatable("message.dealt_force_skills.match.round_start_freeze",
                FREEZE_DURATION_TICKS / 20), true);
    }

    public static boolean isFrozen(Player player) {
        return player.hasEffect(ModEffects.ROUND_START_FREEZE);
    }
}
