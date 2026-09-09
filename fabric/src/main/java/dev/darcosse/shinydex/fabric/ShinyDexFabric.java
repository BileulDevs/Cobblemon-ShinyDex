package dev.darcosse.shinydex.fabric;

import dev.darcosse.shiny_charm.ShinyCharm;
import dev.darcosse.shiny_charm.commands.ShinyCharmCommands;
import dev.darcosse.shiny_charm.item.ModItems;
import dev.darcosse.shiny_charm.tick.HandleAdvancement;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ShinyDexFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 1. enregistrement de l'item, avant tout le reste
        Item shinyCharm = Registry.register(
                BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(ShinyCharm.MOD_ID, ModItems.SHINY_CHARM_ID),
                ModItems.createShinyCharm()
        );
        ModItems.SHINY_CHARM = () -> shinyCharm;

        // 2. onglet creatif
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(ShinyCharm.MOD_ID, ModItems.TAB_ID),
                ModItems.createTab()
        );

        // 3. integration Trinkets : l'item devient equipable dans un slot d'accessoire
        TrinketsApi.registerTrinket(shinyCharm, new ShinyDexTrinket());

        // 4. logique commune
        ShinyCharm.init(new FabricPlatformAdapter());

        // 5. events specifiques Fabric
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> ShinyCharmCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(HandleAdvancement::checkAdvancements);
    }
}
