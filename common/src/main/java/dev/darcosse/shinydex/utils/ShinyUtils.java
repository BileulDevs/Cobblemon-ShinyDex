package dev.darcosse.shinydex.utils;

import dev.darcosse.shinydex.config.ConfigManager;

import java.util.concurrent.ThreadLocalRandom;

public class ShinyUtils {
    public static boolean getIsShiny() {
        int odds = ConfigManager.getShinyChance();
        int random = ThreadLocalRandom.current().nextInt(1, odds);
        return random == 1;
    }
}
