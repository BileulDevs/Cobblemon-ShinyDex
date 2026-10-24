package dev.darcosse.shinydex.utils;

import dev.darcosse.shinydex.advancement.ModAdvancement;
import dev.darcosse.shinydex.config.ConfigManager;
import dev.darcosse.shinydex.config.ShinyCharmConfig.CharmSettings;
import dev.darcosse.shinydex.item.ShinyCharmItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who is allowed to benefit from the Shiny Charm.
 *
 * The charm is a reward, so by default owning one is not enough — the
 * SHINY_CHARM_UNLOCKED advancement has to be there. Two checks guard it, and
 * they are deliberately not equivalent:
 *
 *  - equipping is blocked for convenience, so the player understands why
 *    nothing happens. Anything that goes through an inventory can be worked
 *    around, so this one guarantees nothing.
 *  - the spawn check is the one that matters. Even a charm equipped through a
 *    command, a bug or another mod does nothing without the advancement.
 *
 * Entitlement is read from the advancement, never recomputed from the Pokedex
 * here: the spawn path must stay a couple of map lookups, and a charm once
 * earned must not be lost because the owner tightened the config later.
 */
public final class ShinyCharmAccess {

    private ShinyCharmAccess() {
    }

    /**
     * Has the player earned the charm?
     *
     * Reading the advancement is cheap — it is a map lookup on data the server
     * already holds — so this is safe to call on the spawn path.
     */
    public static boolean hasUnlocked(ServerPlayer player) {
        if (player == null) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        return AdvancementUtils.isDone(player, ModAdvancement.SHINY_CHARM_UNLOCKED.getAdvancement(server));
    }

    /**
     * May this player wear and use the charm? True for everyone when the
     * config does not require the unlock, otherwise only for players who
     * earned it.
     */
    public static boolean isEntitled(ServerPlayer player) {
        CharmSettings charm = ConfigManager.charm();

        if (!charm.enabled) return false;
        if (!charm.requireUnlock) return true;

        return hasUnlocked(player);
    }

    /**
     * Is the charm actually doing anything for this player?
     *
     * Both conditions, in the cheap-first order: wearing it, and entitled to it.
     */
    public static boolean isCharmActive(ServerPlayer player) {
        return ShinyCharmItem.isWearingShinyCharm(player) && isEntitled(player);
    }
}
