package dev.darcosse.shinydex.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.darcosse.shinydex.config.ShinyCharmConfig.CharmSettings;
import dev.darcosse.shinydex.config.ShinyCharmConfig.UnlockSettings;
import dev.darcosse.shinydex.platform.Platform;
import dev.darcosse.shinydex.utils.RegionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads, validates, migrates and exposes config/cobblemon_shinydex/main.json.
 *
 * The config is server-side only: nothing in it is sent to clients, which is
 * why there is no network format to keep in sync. The file layout itself is
 * versioned through configVersion so an older file is upgraded in place
 * instead of losing the owner's settings.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ShinyDex");

    /**
     * Folder of this mod inside the loader's config directory. A folder rather
     * than a single file so further config files can sit next to main.json
     * without cluttering config/. Renaming it orphans every server's existing
     * settings unless a migration step is added to loadConfig().
     */
    private static final String CONFIG_DIR = "cobblemon_shinydex";

    /** Main config file inside CONFIG_DIR. */
    private static final String CONFIG_FILE = "main.json";

    /** Relative path used in log lines, so the owner knows which file to open. */
    private static final String CONFIG_DISPLAY = CONFIG_DIR + "/" + CONFIG_FILE;

    /**
     * Where versions up to 3.0 kept the config, directly in config/. Read once
     * when the new file does not exist yet, then renamed out of the way.
     */
    private static final String LEGACY_CONFIG_FILE = "shinydex.json";

    /**
     * Appended to the legacy file once its content has been moved. Renamed
     * rather than deleted so nothing the owner wrote is ever destroyed, and
     * renamed at all so they do not keep editing a file that is no longer read.
     */
    private static final String LEGACY_BACKUP_SUFFIX = ".old";

    /**
     * Current layout version. Bump it whenever a field is added, renamed or
     * moved, and add the matching step to migrateLegacy(). Files written by a newer
     * version are still read, but a warning is logged.
     */
    public static final int CURRENT_CONFIG_VERSION = 1;

    /**
     * The only field of the pre-versioned file (layout 0). Kept so its value
     * survives the migration to the nested layout.
     */
    private static final String LEGACY_CHANCE_FIELD = "shinyCharmSpawnChance";

    private static final String VERSION_FIELD = "configVersion";

    /** Used when the file is missing, empty or unreadable. */
    public static final int DEFAULT_CHANCE = 3750;

    /** Below this, ThreadLocalRandom.nextInt(1, odds) would throw. */
    public static final int MIN_CHANCE = 1;

    /**
     * Volatile because /shinydex reload swaps the whole object: readers either
     * see the old config or the new one, never a half-validated instance.
     */
    private static volatile ShinyCharmConfig config;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private ConfigManager() {
    }

    /** config/cobblemon_shinydex/main.json, inside the loader's config directory. */
    private static Path configFile() {
        return Platform.get().getConfigDir().resolve(CONFIG_DIR).resolve(CONFIG_FILE);
    }

    /** config/shinydex.json, the pre-3.1 location. */
    private static Path legacyConfigFile() {
        return Platform.get().getConfigDir().resolve(LEGACY_CONFIG_FILE);
    }

    /**
     * Reads the file, migrating and correcting it as needed.
     *
     * A missing file is created with the defaults, unless the pre-3.1
     * config/shinydex.json exists: its settings are then carried over to the
     * new location. An unreadable file — new or legacy — is left untouched on
     * disk so the owner can fix their typo, and the defaults apply meanwhile.
     */
    public static void loadConfig() {
        Path file = configFile();
        boolean fromLegacyLocation = false;

        if (!Files.exists(file)) {
            Path legacy = legacyConfigFile();

            if (!Files.exists(legacy)) {
                config = new ShinyCharmConfig();
                saveConfig();
                return;
            }

            file = legacy;
            fromLegacyLocation = true;
        }

        ShinyCharmConfig loaded = null;
        // Content read from the old location must always be written to the new one.
        boolean needsSave = fromLegacyLocation;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);

            if (root != null && root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();

                if (!object.has(VERSION_FIELD)) {
                    loaded = migrateLegacy(object);
                    needsSave = true;
                } else {
                    loaded = GSON.fromJson(object, ShinyCharmConfig.class);
                }
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            // IllegalStateException covers a field of the wrong JSON kind,
            // e.g. "regions": "national" instead of a list.
            // A broken legacy file is not moved: the new file is not created
            // either, so the owner's fix is picked up on the next start.
            LOGGER.error("Could not read {}, falling back to defaults: {}", file, e.getMessage());
            config = new ShinyCharmConfig();
            validate(config);
            return;
        }

        // Empty file, a file containing literally "null", or a non-object root.
        if (loaded == null) {
            loaded = new ShinyCharmConfig();
            needsSave = true;
        }

        if (validate(loaded)) {
            needsSave = true;
        }

        config = loaded;

        if (needsSave) {
            boolean saved = saveConfig();

            // Only once the content is safely in the new file: renaming first
            // and failing to write would leave the owner with neither.
            if (saved && fromLegacyLocation) {
                retireLegacyFile();
            }
        }
    }

    /**
     * Renames config/shinydex.json to shinydex.json.old after its content has
     * been moved to the new location.
     */
    private static void retireLegacyFile() {
        Path legacy = legacyConfigFile();
        Path backup = legacy.resolveSibling(LEGACY_CONFIG_FILE + LEGACY_BACKUP_SUFFIX);

        try {
            Files.move(legacy, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Moved config from {} to {}; the old file was kept as {}.",
                    LEGACY_CONFIG_FILE, CONFIG_DISPLAY, backup.getFileName());
        } catch (IOException e) {
            // Harmless: the new file exists and wins from now on. The old one
            // simply stays where it was.
            LOGGER.warn("Config moved to {}, but {} could not be renamed: {}. It is no longer read.",
                    CONFIG_DISPLAY, LEGACY_CONFIG_FILE, e.getMessage());
        }
    }

    /**
     * Upgrades a layout-0 file, which held nothing but the spawn chance at the
     * top level, to the current layout with every other setting at its
     * default. Those defaults reproduce the old behaviour exactly: National
     * Pokedex, caught, every species counted.
     */
    private static ShinyCharmConfig migrateLegacy(JsonObject legacy) {
        ShinyCharmConfig migrated = new ShinyCharmConfig();

        if (legacy.has(LEGACY_CHANCE_FIELD) && legacy.get(LEGACY_CHANCE_FIELD).isJsonPrimitive()) {
            try {
                migrated.shinyCharm.spawnChance = legacy.get(LEGACY_CHANCE_FIELD).getAsInt();
            } catch (NumberFormatException e) {
                LOGGER.warn("Ignoring non-numeric {} during migration.", LEGACY_CHANCE_FIELD);
            }
        }

        LOGGER.info("Migrated {} to config layout version {}.", CONFIG_DISPLAY, CURRENT_CONFIG_VERSION);
        return migrated;
    }

    /**
     * Replaces missing sections and clamps out-of-range values back into the
     * accepted domain, logging each correction.
     *
     * @return true if at least one value was corrected, so the caller writes
     *         the file back and the owner sees what actually applies
     */
    private static boolean validate(ShinyCharmConfig cfg) {
        boolean changed = false;

        if (cfg.configVersion > CURRENT_CONFIG_VERSION) {
            // Not downgraded: an older jar must not rewrite a newer file's version.
            LOGGER.warn("{} has configVersion {} but this build knows {}; unknown settings are ignored.",
                    CONFIG_DISPLAY, cfg.configVersion, CURRENT_CONFIG_VERSION);
        } else if (cfg.configVersion < CURRENT_CONFIG_VERSION) {
            cfg.configVersion = CURRENT_CONFIG_VERSION;
            changed = true;
        }

        if (cfg.shinyCharm == null) {
            cfg.shinyCharm = new CharmSettings();
            changed = true;
        }
        if (cfg.unlock == null) {
            cfg.unlock = new UnlockSettings();
            changed = true;
        }

        if (cfg.shinyCharm.spawnChance < MIN_CHANCE) {
            LOGGER.warn("Invalid shinyCharm.spawnChance={}, clamped to {}.",
                    cfg.shinyCharm.spawnChance, MIN_CHANCE);
            cfg.shinyCharm.spawnChance = MIN_CHANCE;
            changed = true;
        }

        if (cfg.unlock.mode == null) {
            LOGGER.warn("Invalid unlock.mode, reset to {}.", UnlockMode.CAUGHT);
            cfg.unlock.mode = UnlockMode.CAUGHT;
            changed = true;
        }

        changed |= validateRegions(cfg.unlock);

        if (cfg.unlock.extraExcludedLabels == null) {
            cfg.unlock.extraExcludedLabels = new ArrayList<>();
            changed = true;
        }

        return changed;
    }

    /**
     * Normalises the required regions to their canonical ids, drops unknown
     * and duplicate entries.
     *
     * A list that had entries but none valid is reset to the default rather
     * than left empty: an empty list means "no requirement", and turning a
     * typo into "everyone gets the charm" would be the worst way to fail.
     *
     * @return true if the list was changed
     */
    private static boolean validateRegions(UnlockSettings unlock) {
        if (unlock.regions == null) {
            unlock.regions = new UnlockSettings().regions;
            return true;
        }

        if (unlock.regions.isEmpty()) {
            return false;
        }

        List<String> normalised = new ArrayList<>();

        for (String raw : unlock.regions) {
            RegionUtils region = RegionUtils.fromId(raw == null ? null : raw.trim());

            if (region == null) {
                LOGGER.warn("Unknown region '{}' in unlock.regions, ignored. Valid: {}",
                        raw, RegionUtils.ids());
                continue;
            }
            if (!normalised.contains(region.id())) {
                normalised.add(region.id());
            }
        }

        if (normalised.isEmpty()) {
            LOGGER.warn("unlock.regions contained no valid region, reset to the default.");
            normalised = new UnlockSettings().regions;
        }

        if (normalised.equals(unlock.regions)) {
            return false;
        }

        unlock.regions = normalised;
        return true;
    }

    /**
     * Writes the current config to disk, pretty-printed, creating the
     * cobblemon_shinydex folder first if needed.
     *
     * @return true if the file was written
     */
    public static boolean saveConfig() {
        ShinyCharmConfig current = config;
        if (current == null) return false;

        Path file = configFile();

        try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not write {}: {}", CONFIG_DISPLAY, e.getMessage());
            return false;
        }
    }

    /** Re-reads the file from disk, e.g. after /shinydex reload. */
    public static void reloadConfig() {
        config = null;
        loadConfig();
    }

    /**
     * The validated config, loaded on first access. Callers must not modify
     * it: changes would neither be validated nor saved.
     */
    public static ShinyCharmConfig get() {
        ShinyCharmConfig current = config;
        if (current == null) {
            loadConfig();
            current = Objects.requireNonNull(config, "config failed to load");
        }
        return current;
    }

    /** Shortcut to the charm section. */
    public static CharmSettings charm() {
        return get().shinyCharm;
    }

    /** Shortcut to the unlock section. */
    public static UnlockSettings unlock() {
        return get().unlock;
    }

    /** Exclusive upper bound for ThreadLocalRandom.nextInt(1, odds). */
    public static int getShinyChance() {
        return getCurrentShinyChance() + 1;
    }

    /** The configured denominator, as shown to players. */
    public static int getCurrentShinyChance() {
        return charm().spawnChance;
    }
}
