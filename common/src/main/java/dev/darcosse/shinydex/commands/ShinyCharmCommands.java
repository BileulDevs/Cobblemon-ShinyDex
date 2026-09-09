package dev.darcosse.shiny_charm.commands;

import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.darcosse.shiny_charm.config.ConfigManager;
import dev.darcosse.shiny_charm.utils.PokedexRegionUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

public class ShinyCharmCommands {

    private static final List<String> AVAILABLE_REGIONS = Arrays.asList(
            "kanto", "johto", "hoenn", "sinnoh", "unova",
            "kalos", "alola", "galar", "paldea", "national"
    );

    private static final int POKEMON_PER_PAGE = 30;

    /** Appele par chaque loader depuis son propre event d'enregistrement de commandes. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shinydex")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ShinyCharmCommands::reloadConfig))
                .then(Commands.literal("info")
                        .executes(ShinyCharmCommands::showInfo))
                .then(Commands.literal("check")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PokedexRegionUtils.checkAllRegionsProgress(player);
                            return 1;
                        })
                        .then(Commands.argument("region", StringArgumentType.string())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(AVAILABLE_REGIONS, builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String region = StringArgumentType.getString(context, "region");

                                    if (!isValidRegion(region)) {
                                        player.sendSystemMessage(Component.translatable(
                                                "command.shinycharm.error.invalid_region", region));
                                        return 0;
                                    }

                                    PokedexRegionUtils.checkPlayerRegionProgress(player, region.toLowerCase());
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("missing")
                        .then(Commands.argument("region", StringArgumentType.string())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(AVAILABLE_REGIONS, builder))
                                .executes(context -> showMissing(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> showMissing(context,
                                                IntegerArgumentType.getInteger(context, "page")))
                                )
                        )
                )
                .then(Commands.literal("completion")
                        .then(Commands.argument("region", StringArgumentType.string())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(AVAILABLE_REGIONS, builder))
                                .executes(ShinyCharmCommands::showCompletion)
                        )
                )
        );
    }

    private static int showMissing(CommandContext<CommandSourceStack> context, int page) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String region = StringArgumentType.getString(context, "region");

        if (!isValidRegion(region)) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.error.invalid_region", region));
            return 0;
        }

        List<Species> missing = PokedexRegionUtils.getMissingPokemon(player, region);
        if (missing.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.missing.complete", region));
        } else {
            showPaginatedMissingPokemon(player, missing, region, page);
        }
        return 1;
    }

    private static int showCompletion(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String region = StringArgumentType.getString(context, "region");

        if (!isValidRegion(region)) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.error.invalid_region", region));
            return 0;
        }

        PokedexRegionUtils.RegionProgress progress =
                PokedexRegionUtils.getRegionProgress(player, region);

        if (progress == null) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.error.invalid_region", region));
            return 0;
        }

        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.header", region.toUpperCase()));
        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.implemented", progress.totalImplemented()));
        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.seen", progress.totalSeen()));
        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.caught", progress.totalCaught()));
        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.percentage",
                String.format("%.1f%%", progress.completionPercentage())));
        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.completion.status." + (progress.isCompleted() ? "complete" : "incomplete")));

        return 1;
    }

    private static boolean isValidRegion(String region) {
        return AVAILABLE_REGIONS.contains(region.toLowerCase());
    }

    private static void showPaginatedMissingPokemon(ServerPlayer player, List<Species> missing,
                                                    String region, int page) {
        int totalPages = (int) Math.ceil((double) missing.size() / POKEMON_PER_PAGE);

        if (page < 1 || page > totalPages) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.missing.invalid_page", totalPages));
            return;
        }

        int startIndex = (page - 1) * POKEMON_PER_PAGE;
        int endIndex = Math.min(startIndex + POKEMON_PER_PAGE, missing.size());

        player.sendSystemMessage(Component.translatable(
                "command.shinycharm.missing.header", region.toUpperCase(), missing.size()));

        for (int i = startIndex; i < endIndex; i++) {
            Species species = missing.get(i);
            String speciesTranslationKey = "pokemon.species." + species.showdownId().toLowerCase();
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.missing.entry",
                    species.getNationalPokedexNumber(),
                    Component.translatable(speciesTranslationKey)
            ));
        }

        if (totalPages > 1) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinycharm.missing.pagination", page, totalPages));
        }
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.reloadConfig();

            context.getSource().sendSuccess(
                    () -> Component.translatable("command.shiny.reload.success"), true);

            context.getSource().sendSuccess(
                    () -> Component.translatable("command.shiny.reload.rate",
                            ConfigManager.getCurrentShinyChance()), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.translatable("command.shiny.reload.error", e.getMessage()));
            return 0;
        }
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        int chance = ConfigManager.getCurrentShinyChance();
        double percentage = (1.0 / chance) * 100.0;

        context.getSource().sendSuccess(
                () -> Component.translatable("command.shiny.info.header"), false);
        context.getSource().sendSuccess(
                () -> Component.translatable("command.shiny.info.rate", chance,
                        String.format("%.3f", percentage)), false);

        return 1;
    }
}
