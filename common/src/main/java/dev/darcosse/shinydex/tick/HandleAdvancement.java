package dev.darcosse.shiny_charm.tick;

import dev.darcosse.shiny_charm.advancement.ModAdvancement;
import dev.darcosse.shiny_charm.utils.AdvancementUtils;
import dev.darcosse.shiny_charm.utils.PokedexRegionUtils;
import dev.darcosse.shiny_charm.utils.RegionUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handler generique pour les advancements.
 * Appele a chaque fin de tick serveur par le module de plateforme.
 */
public class HandleAdvancement {

    public static void checkAdvancements(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            grantRootAdvancement(player);
            for (RegionUtils region : RegionUtils.values()) {
                checkDex(player, region.toString());
            }
        });
    }

    private static void grantRootAdvancement(ServerPlayer player) {
        AdvancementUtils.grantAdvancement(player, ModAdvancement.ROOT.getAdvancement(player.server));
    }

    private static void checkDex(ServerPlayer player, String dex) {
        PokedexRegionUtils.RegionProgress progress =
                PokedexRegionUtils.getRegionProgress(player, dex.toLowerCase());

        if (progress != null && progress.isCompleted()) {
            AdvancementHolder advancement = ModAdvancement.getAdvancement(player.server, dex);
            if (advancement != null) {
                AdvancementUtils.grantAdvancement(player, advancement);
            }
        }
    }
}
