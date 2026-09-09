package dev.darcosse.shiny_charm.item;

import dev.darcosse.shiny_charm.ShinyCharm;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.function.Supplier;

/**
 * Definitions communes. L'enregistrement reel est fait par chaque loader,
 * qui vient ensuite remplir SHINY_CHARM avec son propre supplier.
 */
public final class ModItems {

    public static final String SHINY_CHARM_ID = "shiny_charm";
    public static final String TAB_ID = "main";

    /** Rempli par le module de plateforme au moment de l'enregistrement. */
    public static Supplier<Item> SHINY_CHARM;

    private ModItems() {
    }

    /** Factory utilisee par les deux loaders pour construire l'item. */
    public static Item createShinyCharm() {
        return new ShinyCharmItem(new Item.Properties()
                .fireResistant()
                .rarity(Rarity.EPIC)
                .stacksTo(1));
    }

    /** Factory utilisee par les deux loaders pour construire l'onglet creatif. */
    public static CreativeModeTab createTab() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, -1)
                .icon(() -> new ItemStack(SHINY_CHARM.get()))
                .title(Component.translatable("itemgroup." + ShinyCharm.MOD_ID))
                .displayItems((params, output) -> output.accept(SHINY_CHARM.get()))
                .build();
    }
}
