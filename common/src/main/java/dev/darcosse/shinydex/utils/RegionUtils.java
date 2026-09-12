package dev.darcosse.shinydex.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Pokedex regions, with the national number range each one covers.
 *
 * The ranges used to live in a switch inside PokedexRegionUtils that built a
 * fresh int[] on every call. Keeping them here means one source of truth and
 * no allocation.
 */
public enum RegionUtils {
    KANTO(1, 151),
    JOHTO(152, 251),
    HOENN(252, 386),
    SINNOH(387, 493),
    UNOVA(494, 649),
    KALOS(650, 721),
    ALOLA(722, 809),
    GALAR(810, 898),
    PALDEA(899, 1025),
    NATIONAL(1, 1025);

    private final int first;
    private final int last;
    private final String id;

    RegionUtils(int first, int last) {
        this.first = first;
        this.last = last;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public int first() {
        return first;
    }

    public int last() {
        return last;
    }

    /** Lowercase identifier, as used in commands and advancement keys. */
    public String id() {
        return id;
    }

    /** Capitalised name for display, e.g. "Kanto". */
    public String displayName() {
        return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }

    @Override
    public String toString() {
        return id;
    }

    private static final List<String> IDS = Arrays.stream(values()).map(RegionUtils::id).toList();

    /** Every region id, for command suggestions. Immutable, built once. */
    public static List<String> ids() {
        return IDS;
    }

    /**
     * @return the matching region, or null when the name is not recognised.
     *         "unys" is accepted as an alias for Unova.
     */
    public static RegionUtils fromId(String name) {
        if (name == null) return null;

        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals("unys")) return UNOVA;

        for (RegionUtils region : values()) {
            if (region.id.equals(key)) return region;
        }
        return null;
    }
}
