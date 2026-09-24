package com.Gabou.sereneseasonsplus.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Handles the one-time config reset required by the descriptive-name update. */
public final class ConfigResetManager {
    private static final String RESET_MARKER = ".sereneseasonsplus-descriptive-config-reset.done";
    private static final String WARNING_PENDING = ".sereneseasonsplus-descriptive-config-warning.pending";
    private static final String BACKUP_SUFFIX = ".pre-descriptive-config-update.bak";

    private ConfigResetManager() {
    }

    public static synchronized void prepare(Path configDirectory, String configFileName) {
        Path resetMarker = configDirectory.resolve(RESET_MARKER);
        if (Files.exists(resetMarker)) return;

        try {
            Files.createDirectories(configDirectory);
            Path oldConfig = configDirectory.resolve(configFileName);
            if (Files.exists(oldConfig)) {
                Path backup = findAvailableBackup(oldConfig);
                Files.move(oldConfig, backup);
                Files.writeString(configDirectory.resolve(WARNING_PENDING), backup.getFileName().toString(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            Files.writeString(resetMarker,
                    "The descriptive config-name update has been prepared. The active config must not be migrated from the legacy file.",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not preserve and reset the Serene Seasons Plus config", exception);
        }
    }

    public static boolean isWarningPending(Path configDirectory) {
        return Files.exists(configDirectory.resolve(WARNING_PENDING));
    }

    public static String getBackupFileName(Path configDirectory) {
        try {
            String name = Files.readString(configDirectory.resolve(WARNING_PENDING), StandardCharsets.UTF_8).trim();
            return name.isEmpty() ? "the backup file" : name;
        } catch (IOException exception) {
            return "the backup file";
        }
    }

    public static void markWarningShown(Path configDirectory) {
        try {
            Files.deleteIfExists(configDirectory.resolve(WARNING_PENDING));
        } catch (IOException ignored) {
            // Leaving the marker in place safely shows the warning again next launch.
        }
    }

    private static Path findAvailableBackup(Path oldConfig) {
        Path candidate = oldConfig.resolveSibling(oldConfig.getFileName() + BACKUP_SUFFIX);
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = oldConfig.resolveSibling(oldConfig.getFileName() + BACKUP_SUFFIX + "." + index++);
        }
        return candidate;
    }
}
