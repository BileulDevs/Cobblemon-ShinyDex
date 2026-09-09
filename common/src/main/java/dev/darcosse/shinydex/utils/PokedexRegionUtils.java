package dev.darcosse.shiny_charm.utils;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokedexRegionUtils {

    public static boolean isRegionCompleted(ServerPlayer player, String region) {
        RegionProgress progress = getRegionProgress(player, region);
        return progress != null && progress.isCompleted();
    }

    /** Pokemon implementes d'une region, en texte brut. */
    public static String getImplementedRegion(String region) {
        int[] range = getRegionRange(region);
        if (range == null) return "Region non reconnue";

        StringBuilder result = new StringBuilder();
        result.append("Pokemon implementes pour ").append(region).append(":\n");

        int implementedCount = 0;
        for (int i = range[0]; i <= range[1]; i++) {
            Species species = PokemonSpecies.INSTANCE.getByPokedexNumber(i, "cobblemon");

            if (species != null && species.getImplemented()) {
                result.append("- #").append(i).append(" ").append(species.getName()).append("\n");
                implementedCount++;
            }
        }

        result.append("\nTotal implementes: ").append(implementedCount)
                .append("/").append(range[1] - range[0] + 1);

        return result.toString();
    }

    private static int[] getRegionRange(String region) {
        return switch (region.toLowerCase()) {
            case "kanto" -> new int[]{1, 151};
            case "johto" -> new int[]{152, 251};
            case "hoenn" -> new int[]{252, 386};
            case "sinnoh" -> new int[]{387, 493};
            case "unova", "unys" -> new int[]{494, 649};
            case "kalos" -> new int[]{650, 721};
            case "alola" -> new int[]{722, 809};
            case "galar" -> new int[]{810, 898};
            case "paldea" -> new int[]{899, 1025};
            case "national" -> new int[]{1, 1025};
            default -> null;
        };
    }

    private static PokedexManager pokedexOf(ServerPlayer player) {
        return Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player.getUUID());
    }

    public static RegionProgress getRegionProgress(ServerPlayer player, String region) {
        PokedexManager pokedexData = pokedexOf(player);

        int[] range = getRegionRange(region);
        if (range == null) return null;

        int implementedCount = 0;
        int caughtCount = 0;
        int seenCount = 0;

        for (int i = range[0]; i <= range[1]; i++) {
            Species species = PokemonSpecies.INSTANCE.getByPokedexNumber(i, "cobblemon");

            if (species != null && species.getImplemented()) {
                implementedCount++;

                if (hasSeenSpecies(pokedexData, species)) {
                    seenCount++;
                }

                if (hasCaughtSpecies(pokedexData, species)) {
                    caughtCount++;
                }
            }
        }

        return new RegionProgress(region, implementedCount, seenCount, caughtCount);
    }

    public record RegionProgress(
            String region,
            int totalImplemented,
            int totalSeen,
            int totalCaught,
            double completionPercentage,
            boolean isCompleted
    ) {
        public RegionProgress(String region, int totalImplemented, int totalSeen, int totalCaught) {
            this(
                    region,
                    totalImplemented,
                    totalSeen,
                    totalCaught,
                    totalImplemented > 0 ? (double) totalCaught / totalImplemented * 100.0 : 0.0,
                    totalImplemented > 0 && totalCaught == totalImplemented
            );
        }

        @Override
        public String toString() {
            return String.format("%s: %d/%d captures (%.1f%%) - %s",
                    region, totalCaught, totalImplemented, completionPercentage,
                    isCompleted ? "COMPLETE" : "EN COURS");
        }
    }

    public static void checkPlayerRegionProgress(ServerPlayer player, String region) {
        RegionProgress progress = getRegionProgress(player, region);

        if (progress == null) {
            player.sendSystemMessage(
                    Component.translatable("command.shinycharm.error.invalid_region", region));
            return;
        }

        if (progress.isCompleted()) {
            player.sendSystemMessage(
                    Component.translatable("command.shinycharm.check.completed", region));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.check.progress",
                    region,
                    progress.totalCaught(),
                    progress.totalImplemented(),
                    String.format("%.1f%%", progress.completionPercentage())
            ));
        }
    }

    public static void checkAllRegionsProgress(ServerPlayer player) {
        String[] regions = {"kanto", "johto", "hoenn", "sinnoh", "unova",
                "kalos", "alola", "galar", "paldea", "national"};

        player.sendSystemMessage(Component.literal("§6=== Progression Pokedex ==="));

        for (String region : regions) {
            RegionProgress progress = getRegionProgress(player, region);
            if (progress != null && progress.totalImplemented() > 0) {
                String status = progress.isCompleted() ? "§a✓" : "§e⚬";
                player.sendSystemMessage(Component.literal(
                        String.format("%s §f%s: §7%d/%d (%.1f%%)",
                                status,
                                region.substring(0, 1).toUpperCase() + region.substring(1),
                                progress.totalCaught(),
                                progress.totalImplemented(),
                                progress.completionPercentage())
                ));
            }
        }
    }

    public static List<Species> getMissingPokemon(ServerPlayer player, String region) {
        PokedexManager pokedexData = pokedexOf(player);

        int[] range = getRegionRange(region);
        if (range == null) return Collections.emptyList();

        List<Species> missing = new ArrayList<>();

        for (int i = range[0]; i <= range[1]; i++) {
            Species species = PokemonSpecies.INSTANCE.getByPokedexNumber(i, "cobblemon");

            if (species != null && species.getImplemented() && !hasCaughtSpecies(pokedexData, species)) {
                missing.add(species);
            }
        }

        return missing;
    }

    private static boolean hasCaughtSpecies(PokedexManager pokedexData, Species species) {
        return pokedexData.getKnowledgeForSpecies(species.resourceIdentifier) == PokedexEntryProgress.OWNED;
    }

    private static boolean hasSeenSpecies(PokedexManager pokedexData, Species species) {
        return pokedexData.getKnowledgeForSpecies(species.resourceIdentifier) == PokedexEntryProgress.SEEN;
    }
}
