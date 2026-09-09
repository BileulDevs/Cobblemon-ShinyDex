package dev.darcosse.shiny_charm.platform;

/**
 * Porte l'adapter fourni par le loader courant.
 * Rempli par ShinyCharm.init() avant toute autre utilisation.
 */
public final class Platform {

    private static PlatformAdapter adapter;

    private Platform() {
    }

    public static void set(PlatformAdapter platformAdapter) {
        adapter = platformAdapter;
    }

    public static PlatformAdapter get() {
        if (adapter == null) {
            throw new IllegalStateException(
                    "PlatformAdapter non initialise : ShinyCharm.init(adapter) n'a pas ete appele.");
        }
        return adapter;
    }
}
