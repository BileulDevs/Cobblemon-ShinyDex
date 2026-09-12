package dev.darcosse.shinydex.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import dev.darcosse.shinydex.utils.DexCache.DexEntry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PokedexRegionUtils {

    private PokedexRegionUtils() {
    }

    private static PokedexManager pokedexOf(ServerPlayer player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
    }

    /**
     * Counts of a player's progress through one region.
     */
    public record RegionProgress(
            RegionUtils region,
            int totalImplemented,
            int totalSeen,
            int totalCaught,
            double completionPercentage,
            boolean isCompleted
    ) {
        public RegionProgress(RegionUtils region, int totalImplemented, int totalSeen, int totalCaught) {
            this(
                    region,
                    totalImplemented,
                    totalSeen,
                    totalCaught,
                    totalImplemented > 0 ? (double) totalCaught / totalImplemented * 100.0 : 0.0,
                    totalImplemented > 0 && totalCaught == totalImplemented
            );
        }
    }

    /**
     * One pass over the region, one Pokedex lookup per species.
     *
     * The previous version asked the Pokedex twice for every species — once for
     * "seen", once for "caught" — on top of resolving the species itself.
     */
    public static RegionProgress getRegionProgress(ServerPlayer player, RegionUtils region) {
        if (region == null) return null;

        PokedexManager pokedex = pokedexOf(player);
        List<DexEntry> entries = DexCache.entriesOf(region);

        int caught = 0;
        int seen = 0;

        for (DexEntry entry : entries) {
            PokedexEntryProgress knowledge = pokedex.getKnowledgeForSpecies(entry.identifier());

            if (knowledge == PokedexEntryProgress.OWNED) {
                caught++;
            } else if (knowledge == PokedexEntryProgress.SEEN) {
                seen++;
            }
        }

        return new RegionProgress(region, entries.size(), seen, caught);
    }

    /**
     * Cheaper than getRegionProgress when only the answer matters: it stops at
     * the first species the player is missing.
     */
    public static boolean isRegionCompleted(ServerPlayer player, RegionUtils region) {
        if (region == null) return false;

        PokedexManager pokedex = pokedexOf(player);
        List<DexEntry> entries = DexCache.entriesOf(region);

        if (entries.isEmpty()) return false;

        for (DexEntry entry : entries) {
            if (pokedex.getKnowledgeForSpecies(entry.identifier()) != PokedexEntryProgress.OWNED) {
                return false;
            }
        }
        return true;
    }

    public static List<DexEntry> getMissingPokemon(ServerPlayer player, RegionUtils region) {
        if (region == null) return List.of();

        PokedexManager pokedex = pokedexOf(player);
        List<DexEntry> missing = new ArrayList<>();

        for (DexEntry entry : DexCache.entriesOf(region)) {
            if (pokedex.getKnowledgeForSpecies(entry.identifier()) != PokedexEntryProgress.OWNED) {
                missing.add(entry);
            }
        }

        return missing;
    }
}
