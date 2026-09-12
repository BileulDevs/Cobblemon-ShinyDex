package dev.darcosse.shinydex.neoforge;

import dev.darcosse.shinydex.ShinyCharm;
import dev.darcosse.shinydex.commands.ShinyCharmCommands;
import dev.darcosse.shinydex.item.ModItems;
import dev.darcosse.shinydex.tick.HandleAdvancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.theillusivec4.curios.api.CuriosCapability;

@Mod(ShinyDexNeoForge.MOD_ID)
public class ShinyDexNeoForge {

    /** Doit correspondre au modId de neoforge.mods.toml. */
    public static final String MOD_ID = "shinydex";

    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ShinyCharm.MOD_ID);

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShinyCharm.MOD_ID);

    private static final DeferredHolder<Item, Item> SHINY_CHARM =
            ITEMS.register(ModItems.SHINY_CHARM_ID, ModItems::createShinyCharm);

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register(ModItems.TAB_ID, ModItems::createTab);

    public ShinyDexNeoForge(IEventBus modEventBus) {
        // DeferredHolder implemente Supplier<Item> : resolution paresseuse, pas de souci d'ordre
        ModItems.SHINY_CHARM = SHINY_CHARM;

        ITEMS.register(modEventBus);
        TABS.register(modEventBus);

        // Curios refuses the slot through the ICurio capability, where Trinkets
        // uses a plain interface. Registering it here gives NeoForge the same
        // equip-time feedback the Fabric side already has.
        modEventBus.addListener((RegisterCapabilitiesEvent event) ->
                event.registerItem(
                        CuriosCapability.ITEM,
                        (stack, context) -> new ShinyCharmCurio(stack),
                        SHINY_CHARM.get()));

        ShinyCharm.init(new NeoForgePlatformAdapter());

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        ShinyCharmCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        HandleAdvancement.checkAdvancements(event.getServer());
    }
}
