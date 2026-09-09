package dev.darcosse.shiny_charm;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import dev.darcosse.shiny_charm.config.ConfigManager;
import dev.darcosse.shiny_charm.handlers.SpawnHandler;
import dev.darcosse.shiny_charm.platform.Platform;
import dev.darcosse.shiny_charm.platform.PlatformAdapter;
import kotlin.Unit;

/**
 * Point d'entree commun aux deux loaders.
 * Chaque module de plateforme appelle init() apres avoir fourni son adapter
 * et enregistre ses items / tabs / events.
 */
public final class ShinyCharm {

    public static final String MOD_ID = "shiny_charm";

    private ShinyCharm() {
    }

    /**
     * A appeler par le module de plateforme, une seule fois, au demarrage.
     */
    public static void init(PlatformAdapter adapter) {
        Platform.set(adapter);

        ConfigManager.loadConfig();

        System.out.println("[ShinyCharm] Shiny Charm Rate : 1/" + ConfigManager.getCurrentShinyChance());

        CobblemonEvents.ENTITY_SPAWN.subscribe(
                Priority.HIGHEST,
                event -> (Unit) SpawnHandler.handlePokemonSpawn(event)
        );
    }
}
