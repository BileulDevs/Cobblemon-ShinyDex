package dev.darcosse.shinydex.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.darcosse.shinydex.config.ConfigManager;
import dev.darcosse.shinydex.utils.DexCache.DexEntry;
import dev.darcosse.shinydex.utils.PokedexRegionUtils;
import dev.darcosse.shinydex.utils.PokedexRegionUtils.RegionProgress;
import dev.darcosse.shinydex.utils.RegionUtils;
import dev.darcosse.shinydex.utils.TypeColors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ShinyCharmCommands {

    private static final String ROOT = "shinydex";
    private static final int POKEMON_PER_PAGE = 30;

    private static final TextColor NUMBER_COLOR = TextColor.fromRgb(0xFFAA00);

    private ShinyCharmCommands() {
    }

    /** Called by each loader from its own command registration event. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ROOT)
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ShinyCharmCommands::reloadConfig))

                .then(Commands.literal("info")
                        .executes(ShinyCharmCommands::showInfo))

                .then(Commands.literal("check")
                        .executes(ShinyCharmCommands::checkAll)
                        .then(regionArgument()
                                .executes(ShinyCharmCommands::checkRegion)))

                .then(Commands.literal("missing")
                        .then(regionArgument()
                                .executes(context -> showMissing(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> showMissing(context,
                                                IntegerArgumentType.getInteger(context, "page"))))))

                .then(Commands.literal("completion")
                        .then(regionArgument()
                                .executes(ShinyCharmCommands::showCompletion)))
        );
    }

    /**
     * The region argument, with suggestions driven by the enum so the two can
     * never drift apart.
     */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> regionArgument() {
        return Commands.argument("region", StringArgumentType.word())
                .suggests((context, builder) ->
                        SharedSuggestionProvider.suggest(RegionUtils.ids(), builder));
    }

    /**
     * Resolves the region argument, reporting the invalid value to the player.
     *
     * @return null when the name is not a region, after sending the error
     */
    private static RegionUtils resolveRegion(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String raw = StringArgumentType.getString(context, "region");
        RegionUtils region = RegionUtils.fromId(raw);

        if (region == null) {
            player.sendSystemMessage(Component.translatable("command.shinydex.error.invalid_region", raw));
        }
        return region;
    }

    // ---- rendering -------------------------------------------------------

    /**
     * The Pokedex number, padded so the list lines up in chat.
     */
    private static Component dexNumber(int number) {
        return Component.literal("#" + String.format("%04d", number))
                .withStyle(style -> style.withColor(NUMBER_COLOR));
    }

    /**
     * The species name, translated by Cobblemon and coloured by its primary
     * type.
     *
     * translatableWithFallback rather than translatable: should Cobblemon not
     * have a key for a species — an addon that ships no lang file, say — the
     * player sees the English name instead of the raw key printed as-is.
     */
    private static Component speciesName(DexEntry entry) {
        TextColor primary = TypeColors.of(entry.primaryType());

        return Component.translatableWithFallback(entry.translationKey(), entry.name())
                .withStyle(style -> style.withColor(primary));
    }

    /**
     * Wraps a translation argument in its own colour.
     *
     * Legacy section codes do NOT survive across component boundaries: once an
     * argument is substituted it becomes a sibling with its own style, and the
     * text after it restarts from the base style rather than from the code
     * written before it. Styling arguments here is the only way to keep a line
     * visually consistent.
     */
    private static Component arg(Object value, ChatFormatting color) {
        return Component.literal(String.valueOf(value)).withStyle(color);
    }

    /**
     * A percentage, sign included, as a single coloured argument.
     *
     * "%%" cannot stay in the translation keys: Minecraft splits a translation
     * on its format specifiers, and an escaped percent is emitted as its own
     * fragment. The section code before it stays behind in the previous
     * fragment, and the text after it starts a fresh one with no colour — the
     * same break an argument causes. Carrying the sign inside the argument is
     * the only way to keep it coloured.
     */
    private static Component percent(double value, ChatFormatting color) {
        return Component.literal(String.format("%.1f%%", value)).withStyle(color);
    }

    /** A clickable page link, greyed out when the page does not exist. */
    private static Component pageLink(String label, RegionUtils region, int page, boolean enabled) {
        MutableComponent link = Component.literal(label);

        if (!enabled) {
            return link.withStyle(ChatFormatting.DARK_GRAY);
        }

        String command = "/" + ROOT + " missing " + region.id() + " " + page;

        return link.withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
    }

    // ---- subcommands -----------------------------------------------------

    private static int checkAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        player.sendSystemMessage(Component.translatable("command.shinydex.check.header"));

        for (RegionUtils region : RegionUtils.values()) {
            RegionProgress progress = PokedexRegionUtils.getRegionProgress(player, region);
            if (progress == null || progress.totalImplemented() == 0) continue;

            player.sendSystemMessage(Component.translatable(
                    progress.isCompleted()
                            ? "command.shinydex.check.line.complete"
                            : "command.shinydex.check.line.progress",
                    arg(region.displayName(), ChatFormatting.WHITE),
                    arg(progress.totalCaught(), ChatFormatting.GRAY),
                    arg(progress.totalImplemented(), ChatFormatting.GRAY),
                    percent(progress.completionPercentage(), ChatFormatting.DARK_GRAY)
            ));
        }

        return 1;
    }

    private static int checkRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        RegionUtils region = resolveRegion(context, player);
        if (region == null) return 0;

        RegionProgress progress = PokedexRegionUtils.getRegionProgress(player, region);
        if (progress == null) return 0;

        if (progress.isCompleted()) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinydex.check.completed", region.displayName()));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "command.shinydex.check.progress",
                    arg(region.displayName(), ChatFormatting.GOLD),
                    arg(progress.totalCaught(), ChatFormatting.WHITE),
                    arg(progress.totalImplemented(), ChatFormatting.WHITE),
                    percent(progress.completionPercentage(), ChatFormatting.DARK_GRAY)
            ));
        }

        return 1;
    }

    private static int showMissing(CommandContext<CommandSourceStack> context, int page) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        RegionUtils region = resolveRegion(context, player);
        if (region == null) return 0;

        List<DexEntry> missing = PokedexRegionUtils.getMissingPokemon(player, region);

        if (missing.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinydex.missing.complete", region.displayName()));
            return 1;
        }

        int totalPages = (int) Math.ceil((double) missing.size() / POKEMON_PER_PAGE);

        if (page > totalPages) {
            player.sendSystemMessage(Component.translatable(
                    "command.shinydex.missing.invalid_page", page, totalPages));
            return 0;
        }

        // The header echoes back both arguments, so the output always says which
        // region and which page it is showing.
        player.sendSystemMessage(Component.translatable(
                "command.shinydex.missing.header",
                arg(region.displayName(), ChatFormatting.YELLOW),
                arg(page, ChatFormatting.YELLOW),
                arg(totalPages, ChatFormatting.YELLOW),
                arg(missing.size(), ChatFormatting.DARK_GRAY)));

        int from = (page - 1) * POKEMON_PER_PAGE;
        int to = Math.min(from + POKEMON_PER_PAGE, missing.size());

        for (int i = from; i < to; i++) {
            DexEntry entry = missing.get(i);

            player.sendSystemMessage(Component.literal(" ")
                    .append(dexNumber(entry.number()))
                    .append(Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(speciesName(entry)));
        }

        if (totalPages > 1) {
            player.sendSystemMessage(Component.empty()
                    .append(pageLink("[<<] ", region, page - 1, page > 1))
                    .append(Component.translatable("command.shinydex.missing.pagination", page, totalPages))
                    .append(pageLink(" [>>]", region, page + 1, page < totalPages)));
        }

        return 1;
    }

    private static int showCompletion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        RegionUtils region = resolveRegion(context, player);
        if (region == null) return 0;

        RegionProgress progress = PokedexRegionUtils.getRegionProgress(player, region);
        if (progress == null) return 0;

        player.sendSystemMessage(Component.translatable(
                "command.shinydex.completion.header", region.displayName()));
        player.sendSystemMessage(Component.translatable(
                "command.shinydex.completion.implemented", progress.totalImplemented()));
        player.sendSystemMessage(Component.translatable(
                "command.shinydex.completion.seen", progress.totalSeen()));
        player.sendSystemMessage(Component.translatable(
                "command.shinydex.completion.caught", progress.totalCaught()));
        player.sendSystemMessage(Component.translatable(
                "command.shinydex.completion.percentage",
                percent(progress.completionPercentage(), ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.translatable(
                progress.isCompleted()
                        ? "command.shinydex.completion.status.complete"
                        : "command.shinydex.completion.status.incomplete"));

        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            ConfigManager.reloadConfig();

            context.getSource().sendSuccess(
                    () -> Component.translatable("command.shinydex.reload.success"), true);
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.shinydex.reload.rate",
                            ConfigManager.getCurrentShinyChance()), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.translatable("command.shinydex.reload.error", e.getMessage()));
            return 0;
        }
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        int chance = ConfigManager.getCurrentShinyChance();
        double percentage = (1.0 / chance) * 100.0;

        context.getSource().sendSuccess(
                () -> Component.translatable("command.shinydex.info.header"), false);
        context.getSource().sendSuccess(
                () -> Component.translatable("command.shinydex.info.rate",
                        arg(chance, ChatFormatting.YELLOW),
                        Component.literal(String.format("%.3f%%", percentage))
                                .withStyle(ChatFormatting.YELLOW)), false);

        return 1;
    }
}
