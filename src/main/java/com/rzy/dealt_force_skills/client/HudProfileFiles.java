package com.rzy.dealt_force_skills.client;

import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class HudProfileFiles {
    private HudProfileFiles() {}

    public static Path local(Path legacy) {
        return forAccount(legacy, Minecraft.getInstance().getUser().getProfileId());
    }

    static Path forAccount(Path legacy, UUID account) {
        Path file = legacy.getParent().resolve("hud-profiles").resolve(account.toString()).resolve(legacy.getFileName());
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file) && Files.isRegularFile(legacy)) Files.copy(legacy, file);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException failure) {
            throw new UncheckedIOException("Cannot open local HUD profile " + file, failure);
        }
        return file;
    }
}
