package dev.darcosse.shinydex.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The implemented species of each region, resolved once.
 *
 * Every lookup used to go through PokemonSpecies.getByPokedexNumber on demand.
 * Walking the national dex alone is 1025 of those calls, and the advancement
 * tick did all ten regions for every player, every tick.
 *
 * Only immutable data is kept — number, identifier, name, types — rather than
 * the Species objects themselves, so a Cobblemon reload cannot leave stale
 * references behind. The identifier is what the Pokedex is queried with, and
 * it is stable.
 */
public final class DexCache {

    /**
     * One Pokedex slot, flattened to exactly what this mod displays.
     *
     * @param secondaryType null for single-type species
     */
    public record DexEntry(
            int number,
            ResourceLocation identifier,
            String showdownId,
            String name,
            String translationKey,
            String primaryType,
            String secondaryType
    ) {}

    /**
     * Cobblemon's own translation key for a species name.
     *
     * Using Cobblemon's namespace rather than this mod's own
     * pokemon.species.* entries means the names follow whatever language
     * Cobblemon ships, for every species it knows — including any added by an
     * addon after this mod was built.
     */
    private static String translationKeyOf(String showdownId) {
        return "cobblemon.species." + showdownId + ".name";
    }

    private static final Map<RegionUtils, List<DexEntry>> CACHE = new EnumMap<>(RegionUtils.class);

    private DexCache() {
    }

    /**
     * @return the implemented species of this region, in Pokedex order.
     *         Built on first use, then reused.
     */
    public static synchronized List<DexEntry> entriesOf(RegionUtils region) {
        List<DexEntry> cached = CACHE.get(region);
        if (cached != null) return cached;

        List<DexEntry> entries = build(region);
        CACHE.put(region, entries);
        return entries;
    }

    private static List<DexEntry> build(RegionUtils region) {
        // The national dex covers everything, so the regional lists are slices
        // of it rather than ten independent scans.
        if (region != RegionUtils.NATIONAL) {
            List<DexEntry> national = entriesOf(RegionUtils.NATIONAL);
            List<DexEntry> slice = new ArrayList<>();

            for (DexEntry entry : national) {
                if (entry.number() >= region.first() && entry.number() <= region.last()) {
                    slice.add(entry);
                }
            }
            return List.copyOf(slice);
        }

        List<DexEntry> entries = new ArrayList<>();

        for (int number = region.first(); number <= region.last(); number++) {
            Species species = PokemonSpecies.INSTANCE.getByPokedexNumber(number, "cobblemon");
            if (species == null || !species.getImplemented()) continue;

            entries.add(new DexEntry(
                    number,
                    species.resourceIdentifier,
                    species.showdownId().toLowerCase(),
                    species.getName(),
                    translationKeyOf(species.showdownId().toLowerCase()),
                    typeName(species.getPrimaryType()),
                    typeName(species.getSecondaryType())
            ));
        }

        return List.copyOf(entries);
    }

    private static String typeName(com.cobblemon.mod.common.api.types.ElementalType type) {
        return type == null ? null : type.getName();
    }

    /**
     * Drops the cache. Only needed if Cobblemon's species registry is ever
     * rebuilt within the same process; it is loaded once per server instance,
     * so this is a safety valve rather than routine maintenance.
     */
    public static synchronized void invalidate() {
        CACHE.clear();
    }
}
