package dev.darcosse.shiny_charm.item;

import dev.darcosse.shiny_charm.platform.Platform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

/**
 * L'item lui-meme est du vanilla pur : l'integration accessoire
 * (Trinkets / Curios) est faite par le module de plateforme.
 */
public class ShinyCharmItem extends Item {

    public ShinyCharmItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isWearingShinyCharm(LivingEntity entity) {
        return Platform.get().isWearingShinyCharm(entity);
    }
}
