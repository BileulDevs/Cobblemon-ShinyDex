package dev.darcosse.shinydex.config;

import dev.darcosse.shinydex.utils.RegionUtils;
import dev.darcosse.shinydex.utils.SpeciesLabels;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The content of config/cobblemon_shinydex/main.json, as Gson maps it.
 *
 * Every field carries its default as an initializer. Gson builds these objects
 * through their no-argument constructor, so a field missing from the file
 * keeps its default instead of falling back to zero / false / null. That is
 * what lets a server owner write only the settings they care about.
 *
 * Values are not trusted as read: ConfigManager validates and corrects them
 * before anything else looks at them.
 */
public class ShinyCharmConfig {

    /**
     * Version of this file's layout. ConfigManager uses it to recognise an
     * older file and migrate it rather than silently resetting it.
     */
    public int configVersion = ConfigManager.CURRENT_CONFIG_VERSION;

    /** How the charm behaves once a player is wearing it. */
    public CharmSettings shinyCharm = new CharmSettings();

    /** What a player has to achieve to earn the charm. */
    public UnlockSettings unlock = new UnlockSettings();

    /**
     * Settings for the charm's effect.
     */
    public static class CharmSettings {

        /**
         * Master switch. When false the charm does nothing for anyone and no
         * player can earn it; the Pokedex tracking keeps working.
         */
        public boolean enabled = true;

        /** Denominator of the shiny chance for a player wearing the charm: 3750 means 1 in 3750. */
        public int spawnChance = ConfigManager.DEFAULT_CHANCE;

        /**
         * When true, the charm only works for — and can only be equipped by —
         * a player who has earned it. When false, anyone wearing one benefits,
         * however they got it.
         */
        public boolean requireUnlock = true;
    }

    /**
     * Settings for earning the charm.
     */
    public static class UnlockSettings {

        /**
         * Whether completing the requirement awards the charm. Players who
         * already earned it keep it when this is turned off.
         */
        public boolean enabled = true;

        /**
         * Region ids that must all be completed. An explicitly empty list
         * means no requirement at all: every player earns the charm on their
         * next check.
         */
        public List<String> regions = new ArrayList<>(List.of(RegionUtils.NATIONAL.id()));

        /** Whether the counted species must be caught, or only seen. */
        public UnlockMode mode = UnlockMode.CAUGHT;

        /** Count legendary species towards the requirement. */
        public boolean includeLegendaries = true;

        /** Count mythical species towards the requirement. */
        public boolean includeMythicals = true;

        /** Count paradox species towards the requirement. */
        public boolean includeParadox = true;

        /** Count Ultra Beasts towards the requirement. */
        public boolean includeUltraBeasts = true;

        /**
         * Any further Cobblemon species labels to leave out of the
         * requirement, for categories without a dedicated switch.
         */
        public List<String> extraExcludedLabels = new ArrayList<>();

        /**
         * The labels a species must NOT carry to be counted, merged from the
         * include switches and the free-form list.
         *
         * Rebuilt on each call rather than cached: it is read once per
         * player per check, and caching it would need invalidating on reload.
         */
        public Set<String> excludedLabels() {
            Set<String> excluded = new LinkedHashSet<>();

            if (!includeLegendaries) excluded.add(SpeciesLabels.LEGENDARY);
            if (!includeMythicals) excluded.add(SpeciesLabels.MYTHICAL);
            if (!includeParadox) excluded.add(SpeciesLabels.PARADOX);
            if (!includeUltraBeasts) excluded.add(SpeciesLabels.ULTRA_BEAST);

            if (extraExcludedLabels != null) {
                for (String label : extraExcludedLabels) {
                    if (label != null && !label.isBlank()) {
                        excluded.add(label.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            return excluded;
        }

        /**
         * The required regions, resolved to the enum. Ids that do not name a
         * region are skipped; ConfigManager has already removed them from a
         * validated config, so this only matters for an unvalidated one.
         */
        public List<RegionUtils> requiredRegions() {
            List<RegionUtils> resolved = new ArrayList<>();
            if (regions == null) return resolved;

            for (String id : regions) {
                RegionUtils region = RegionUtils.fromId(id);
                if (region != null && !resolved.contains(region)) {
                    resolved.add(region);
                }
            }
            return resolved;
        }
    }
}
