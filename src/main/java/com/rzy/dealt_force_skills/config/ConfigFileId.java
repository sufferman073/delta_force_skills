package com.rzy.dealt_force_skills.config;

/**
 * Logical configuration files exposed by the graphical editor and hot-reload path.
 */
public enum ConfigFileId {
    GAMEPLAY("dealtforceskills.toml", false),
    BASE("dealtbase.toml", false),
    BOSSES("dealtbosses.toml", false),
    SHOP("dealtforceshop.toml", false),
    SHOP_SET("dealtshopset.toml", false),
    PLAYER("dealtforceplayer.toml", false),
    GAMBLER("dealtgambler.toml", false),
    HUD("dealtforceskills-hud.toml", true);

    private final String fileName;
    private final boolean clientLocal;

    ConfigFileId(String fileName, boolean clientLocal) {
        this.fileName = fileName;
        this.clientLocal = clientLocal;
    }

    public String fileName() {
        return fileName;
    }

    /** True when the file lives on each client and never uploads to the server. */
    public boolean clientLocal() {
        return clientLocal;
    }

    public static ConfigFileId byId(String id) {
        for (ConfigFileId value : values()) {
            if (value.name().equalsIgnoreCase(id) || value.fileName.equalsIgnoreCase(id)) {
                return value;
            }
        }
        return null;
    }
}
