package dev.darcosse.shinydex.utils;

import dev.darcosse.shinydex.advancement.ModAdvancement;
import dev.darcosse.shinydex.item.ShinyCharmItem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who is allowed to benefit from the Shiny Charm.
 *
 * The charm is the reward for completing the National Pokedex, so owning one
 * is not enough — the advancement has to be there. Two checks guard it, and
 * they are deliberately not equivalent:
 *
 *  - equipping is blocked for convenience, so the player understands why
 *    nothing happens. Anything that goes through an inventory can be worked
 *    around, so this one guarantees nothing.
 *  - the spawn check is the one that matters. Even a charm equipped through a
 *    command, a bug or another mod does nothing without the advancement.
 */
public final class ShinyCharmAccess {

    private ShinyCharmAccess() {
    }

    /**
     * Has the player completed the National Pokedex?
     *
     * Reading the advancement is cheap — it is a map lookup on data the server
     * already holds — so this is safe to call on the spawn path.
     */
    public static boolean hasNationalDex(ServerPlayer player) {
        if (player == null) return false;

        MinecraftServer server = player.getServer();
        if (server == null) return false;

        AdvancementHolder advancement = ModAdvancement.COMPLETED_NATIONAL_DEX.getAdvancement(server);
        if (advancement == null) return false;

        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /**
     * Is the charm actually doing anything for this player?
     *
     * Both conditions, in the cheap-first order: wearing it, and entitled to it.
     */
    public static boolean isCharmActive(ServerPlayer player) {
        return ShinyCharmItem.isWearingShinyCharm(player) && hasNationalDex(player);
    }
}
