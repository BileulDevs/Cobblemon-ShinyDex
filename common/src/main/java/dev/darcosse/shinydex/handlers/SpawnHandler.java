package dev.darcosse.shinydex.handlers;

import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.shinydex.config.ConfigManager;
import dev.darcosse.shinydex.utils.ShinyCharmAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

public final class SpawnHandler {

    private SpawnHandler() {
    }

    public static boolean getIsShiny() {
        int odds = ConfigManager.getShinyChance();
        return ThreadLocalRandom.current().nextInt(1, odds) == 1;
    }

    public static Object handlePokemonSpawn(SpawnEvent<?> event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemonEntity)) {
            return null;
        }

        if (!(event.getSpawnablePosition().getCause().getEntity() instanceof ServerPlayer player)) {
            return null;
        }

        // isCharmActive() checks the National Dex advancement as well as the
        // charm itself. This is the authoritative gate: a charm obtained or
        // equipped by any other means still does nothing here.
        if (!ShinyCharmAccess.isCharmActive(player)) {
            return null;
        }

        if (!getIsShiny()) {
            return null;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();
        pokemon.setShiny(true);

        player.displayClientMessage(
                Component.translatable("message.shinydex.success")
                        .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE)),
                false);

        return null;
    }
}
