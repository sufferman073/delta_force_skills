package com.rzy.dealt_force_skills.character.commander;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class CommanderSkillContext {
    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();

    private CommanderSkillContext() {
    }

    public static <T> T run(Player player, String characterId, Supplier<T> action) {
        Context previous = ACTIVE.get();
        ACTIVE.set(new Context(player.getUUID(), characterId));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }

    public static void run(Player player, String characterId, Runnable action) {
        run(player, characterId, () -> {
            action.run();
            return null;
        });
    }

    public static Optional<String> effectiveCharacterId(Player player) {
        Context context = ACTIVE.get();
        return context != null && player != null && context.playerId().equals(player.getUUID())
                ? Optional.of(context.characterId()) : Optional.empty();
    }

    private record Context(UUID playerId, String characterId) {
    }
}