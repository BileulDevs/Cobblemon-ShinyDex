package dev.darcosse.shinydex.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import dev.darcosse.shinydex.config.ShinyCharmConfig.UnlockSettings;
import dev.darcosse.shinydex.config.UnlockMode;
import dev.darcosse.shinydex.utils.DexCache.DexEntry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Evaluates the configurable Shiny Charm requirement against a player's
 * Pokedex.
 *
 * Kept apart from PokedexRegionUtils on purpose: the region advancements and
 * the /shinydex check / missing commands always measure the full dex, caught.
 * Only the charm follows the configured regions, mode and exclusions, so a
 * server that drops legendaries from the charm still shows complete region
 * progress.
 */
public final class CharmRequirements {

    /**
     * A player's standing in one required region, under the charm's rules.
     *
     * @param counted species in the region that count towards the charm
     * @param met     how many of those satisfy the configured mode
     */
    public record Progress(RegionUtils region, int counted, int met) {

        /** Share of the counted species already satisfied, 0 to 100. */
        public double percentage() {
            return counted > 0 ? (double) met / counted * 100.0 : 0.0;
        }

        /**
         * Whether this region is done. A region with nothing to count is NOT
         * done, matching PokedexRegionUtils.isRegionCompleted: an empty list
         * usually means species are not loaded yet, and granting the charm on
         * that would be wrong and permanent.
         */
        public boolean isMet() {
            return counted > 0 && met == counted;
        }
    }

    private CharmRequirements() {
    }

    /** The player's Pokedex data, as tracked by Cobblemon. */
    private static PokedexManager pokedexOf(ServerPlayer player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
    }

    /**
     * Whether a Pokedex state satisfies the configured mode.
     *
     * SEEN accepts OWNED as well: catching a species implies having seen it,
     * and Cobblemon records the catch as the higher state, not both.
     */
    private static boolean satisfies(PokedexEntryProgress knowledge, UnlockMode mode) {
        if (knowledge == null) return false;

        return switch (mode) {
            case CAUGHT -> knowledge == PokedexEntryProgress.OWNED;
            case SEEN -> knowledge == PokedexEntryProgress.SEEN
                    || knowledge == PokedexEntryProgress.OWNED;
        };
    }

    /**
     * The regions that actually need checking for a pass/fail answer.
     *
     * When the National Pokedex is required, every other region is a subset
     * of it under the same rules, so checking them again would only repeat
     * work.
     */
    private static List<RegionUtils> effectiveRegions(List<RegionUtils> required) {
        if (required.contains(RegionUtils.NATIONAL)) {
            return List.of(RegionUtils.NATIONAL);
        }
        return required;
    }

    /**
     * Whether the player currently meets the whole requirement.
     *
     * Stops at the first missing species. Called from the throttled
     * advancement check only for players who have not earned the charm yet,
     * so its worst case — about 1025 Pokedex lookups — is paid every
     * HandleAdvancement.CHECK_INTERVAL ticks per such player, not per tick.
     *
     * An empty region list means no requirement and is met by everyone.
     */
    public static boolean isMet(ServerPlayer player, UnlockSettings settings) {
        List<RegionUtils> required = settings.requiredRegions();
        if (required.isEmpty()) return true;

        PokedexManager pokedex = pokedexOf(player);
        Set<String> excluded = settings.excludedLabels();
        UnlockMode mode = settings.mode;

        for (RegionUtils region : effectiveRegions(required)) {
            int counted = 0;

            for (DexEntry entry : DexCache.entriesOf(region)) {
                if (entry.hasAnyLabel(excluded)) continue;
                counted++;

                if (!satisfies(pokedex.getKnowledgeForSpecies(entry.identifier()), mode)) {
                    return false;
                }
            }

            if (counted == 0) return false;
        }
        return true;
    }

    /**
     * The player's progress in every required region, in the order the config
     * lists them. Full scan, no short-circuit: meant for the /shinydex charm
     * command, not the tick.
     */
    public static List<Progress> progressOf(ServerPlayer player, UnlockSettings settings) {
        PokedexManager pokedex = pokedexOf(player);
        Set<String> excluded = settings.excludedLabels();
        UnlockMode mode = settings.mode;

        List<Progress> result = new ArrayList<>();

        for (RegionUtils region : settings.requiredRegions()) {
            int counted = 0;
            int met = 0;

            for (DexEntry entry : DexCache.entriesOf(region)) {
                if (entry.hasAnyLabel(excluded)) continue;
                counted++;

                if (satisfies(pokedex.getKnowledgeForSpecies(entry.identifier()), mode)) {
                    met++;
                }
            }
            result.add(new Progress(region, counted, met));
        }
        return result;
    }
}
