package dev.darcosse.shinydex.fabric;

import dev.darcosse.shinydex.utils.ShinyCharmAccess;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The charm has no effect of its own — SpawnHandler reads it through
 * isCharmActive(). What this adds is the equip-time refusal, so a player
 * without the National Pokedex sees the slot reject the item instead of
 * wearing something that silently does nothing.
 *
 * Convenience only: the spawn check is what actually enforces the rule.
 */
public class ShinyDexTrinket implements Trinket {

    @Override
    public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        // Called often, including client-side while rendering the slot, so it
        // stays a plain check with no side effects and no messages.
        if (entity instanceof ServerPlayer player) {
            return ShinyCharmAccess.hasNationalDex(player);
        }

        // Client side: let it show as equippable rather than flicker. The
        // server refuses on its own copy anyway.
        return true;
    }
}
