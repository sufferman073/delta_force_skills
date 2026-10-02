package com.rzy.dealt_force_skills.character.commander;

public enum CommanderUpgrade {
    EFFICIENCY(4),
    STRENGTH(10),
    GENE(20);

    private final int maxLevel;

    CommanderUpgrade(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public long cost(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= maxLevel) {
            return -1L;
        }
        return switch (this) {
            case EFFICIENCY -> 5_000L * (currentLevel + 1L);
            case STRENGTH -> 5_000L << currentLevel;
            case GENE -> 10_000L << currentLevel;
        };
    }

    public String translationKey() {
        return "screen.dealt_force_skills.commander.upgrade." + name().toLowerCase(java.util.Locale.ROOT);
    }
}