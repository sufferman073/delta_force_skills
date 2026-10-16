package mccfk.DeltaForce.java.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class DFClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get(".MCCFK_ALL_MODCONFIG", "deltaforce", "deltaforce-client.json");

    private static DFClientConfig instance;

    @SerializedName("tab_x_offset")
    private int tabXOffset = 0;

    @SerializedName("tab_y_offset")
    private int tabYOffset = 0;

    private DFClientConfig() {
    }

    public static DFClientConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public int getTabXOffset() {
        return tabXOffset;
    }

    public void setTabXOffset(int tabXOffset) {
        this.tabXOffset = tabXOffset;
    }

    public int getTabYOffset() {
        return tabYOffset;
    }

    public void setTabYOffset(int tabYOffset) {
        this.tabYOffset = tabYOffset;
    }

    public static DFClientConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DFClientConfig cfg = GSON.fromJson(reader, DFClientConfig.class);
                if (cfg != null) {
                    instance = cfg;
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[DFClientConfig] 加载配置失败: " + e.getMessage());
            }
        }
        DFClientConfig cfg = new DFClientConfig();
        cfg.save();
        instance = cfg;
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path temp = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(this, writer);
            }
            Files.move(temp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[DFClientConfig] 保存配置失败: " + e.getMessage());
        }
    }
}