package dev.darcosse.shiny_charm.handlers;

import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import dev.darcosse.shiny_charm.config.ConfigManager;
import dev.darcosse.shiny_charm.item.ShinyCharmItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnHandler {

    public static boolean getIsShiny() {
        int odds = ConfigManager.getShinyChance();
        int random = ThreadLocalRandom.current().nextInt(1, odds);
        return random == 1;
    }

    public static Object handlePokemonSpawn(SpawnEvent<?> event) {

        if (!(event.getEntity() instanceof PokemonEntity pokemonEntity)) {
            return null;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();

        if (!(event.getSpawnablePosition().getCause().getEntity() instanceof ServerPlayer player)) {
            return null;
        }

        if (ShinyCharmItem.isWearingShinyCharm(player) && getIsShiny()) {
            pokemon.setShiny(true);
            player.displayClientMessage(
                    Component.translatable("message.shiny_charm.success")
                            .withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE)),
                    false);
        }

        return null;
    }
}
