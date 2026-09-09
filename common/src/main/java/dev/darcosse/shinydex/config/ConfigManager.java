package dev.darcosse.shinydex.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.darcosse.shinydex.platform.Platform;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    private static final String CONFIG_FILE = "shinydex.json";

    /** Used when the file is missing, empty or unreadable. */
    public static final int DEFAULT_CHANCE = 3750;

    /** Below this, ThreadLocalRandom.nextInt(1, odds) would throw. */
    public static final int MIN_CHANCE = 1;

    private static ShinyCharmConfig config;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static File configFile() {
        return new File(Platform.get().getConfigDir().toFile(), CONFIG_FILE);
    }

    public static void loadConfig() {
        File configFile = configFile();

        if (!configFile.exists()) {
            config = new ShinyCharmConfig();
            saveConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            config = GSON.fromJson(reader, ShinyCharmConfig.class);
        } catch (IOException | JsonParseException e) {
            System.err.println("[ShinyDex] Could not read " + CONFIG_FILE
                    + ", falling back to defaults: " + e.getMessage());
            config = null;
        }

        // Empty file, or a file containing literally "null"
        if (config == null) {
            config = new ShinyCharmConfig();
        }

        // If validation changed anything, write the corrected file back
        if (validate()) {
            saveConfig();
        }
    }

    /**
     * Clamps out-of-range values back into the accepted domain.
     *
     * @return true if at least one value was corrected
     */
    private static boolean validate() {
        int chance = config.shinyCharmSpawnChance;

        if (chance <= 0) {
            System.err.println("[ShinyDex] Invalid shinyCharmSpawnChance=" + chance
                    + ", clamped to " + MIN_CHANCE + ".");
            config.shinyCharmSpawnChance = MIN_CHANCE;
            return true;
        }

        return false;
    }

    public static void saveConfig() {
        if (config == null) return;

        try (FileWriter writer = new FileWriter(configFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[ShinyDex] Could not write " + CONFIG_FILE
                    + ": " + e.getMessage());
        }
    }

    /** Exclusive upper bound for ThreadLocalRandom.nextInt(1, odds). */
    public static int getShinyChance() {
        return getCurrentShinyChance() + 1;
    }

    public static void reloadConfig() {
        config = null;
        loadConfig();
    }

    public static int getCurrentShinyChance() {
        if (config == null) loadConfig();
        return config.shinyCharmSpawnChance;
    }
}