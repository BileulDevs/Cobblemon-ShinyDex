package dev.darcosse.shinydex.neoforge;

import dev.darcosse.shinydex.utils.ShinyCharmAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * The Curios counterpart of ShinyDexTrinket: refuses the slot to a player who
 * has not completed the National Pokedex.
 *
 * Convenience only — SpawnHandler is what actually enforces the rule. This
 * exists so the NeoForge side behaves like the Fabric one instead of silently
 * accepting a charm that does nothing.
 */
public class ShinyCharmCurio implements ICurio {

    private final ItemStack stack;

    public ShinyCharmCurio(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public boolean canEquip(SlotContext slotContext) {
        // Called often, including while rendering the slot, so it stays a plain
        // check with no side effects.
        if (slotContext.entity() instanceof ServerPlayer player) {
            return ShinyCharmAccess.hasNationalDex(player);
        }

        // Client side: show it as equippable rather than flicker. The server
        // refuses on its own copy anyway.
        return true;
    }
}
