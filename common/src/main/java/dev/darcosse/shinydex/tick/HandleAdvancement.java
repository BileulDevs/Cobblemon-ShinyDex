package dev.darcosse.shinydex.tick;

import dev.darcosse.shinydex.advancement.ModAdvancement;
import dev.darcosse.shinydex.utils.AdvancementUtils;
import dev.darcosse.shinydex.utils.PokedexRegionUtils;
import dev.darcosse.shinydex.utils.RegionUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants the Pokedex completion advancements.
 *
 * Called by each platform module on every server tick, but the work is throttled
 * and skipped wherever possible. The previous version scanned all ten regions
 * for every player twenty times a second: roughly 2000 species resolutions and
 * 4000 Pokedex lookups per player per tick, to answer a question that changes
 * once every few hours of play.
 */
public final class HandleAdvancement {

    /**
     * Ticks between two checks. Completing a dex is not time-sensitive, and the
     * player has to catch something for the answer to change at all.
     */
    private static final int CHECK_INTERVAL = 100; // 5 seconds

    private static int tickCounter = 0;

    private HandleAdvancement() {
    }

    public static void checkAdvancements(MinecraftServer server) {
        if (++tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayer(server, player);
        }
    }

    private static void checkPlayer(MinecraftServer server, ServerPlayer player) {
        AdvancementUtils.grantAdvancement(player, ModAdvancement.ROOT.getAdvancement(server));

        for (RegionUtils region : RegionUtils.values()) {
            AdvancementHolder advancement = ModAdvancement.getAdvancement(server, region.id());
            if (advancement == null) continue;

            // The advancement itself is the source of truth, and asking it is
            // free. Once a region is done it is never recomputed again — which
            // is the common case for any established player.
            if (player.getAdvancements().getOrStartProgress(advancement).isDone()) continue;

            if (PokedexRegionUtils.isRegionCompleted(player, region)) {
                AdvancementUtils.grantAdvancement(player, advancement);
            }
        }
    }
}
