package dev.darcosse.shinydex.utils;

/**
 * The Cobblemon species labels this mod knows by name.
 *
 * These are the strings Cobblemon puts in a species' "labels" array. The
 * config's include flags map onto them; any other label can still be excluded
 * through extraExcludedLabels, so this list only needs the categories that get
 * a dedicated switch.
 *
 * If one of these strings does not match what Cobblemon actually uses, the
 * matching include flag silently stops excluding anything — the species are
 * simply counted. Check with a known species (e.g. Mewtwo for legendary) after
 * changing one.
 */
public final class SpeciesLabels {

    /** Cobblemon's label for legendary species (Mewtwo, Rayquaza...). */
    public static final String LEGENDARY = "legendary";

    /** Cobblemon's label for mythical species (Mew, Celebi...). */
    public static final String MYTHICAL = "mythical";

    /** Cobblemon's label for paradox species (Great Tusk, Iron Treads...). */
    public static final String PARADOX = "paradox";

    /** Cobblemon's label for Ultra Beasts (Nihilego, Buzzwole...). */
    public static final String ULTRA_BEAST = "ultra_beast";

    private SpeciesLabels() {
    }
}
