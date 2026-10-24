package dev.darcosse.shinydex.tick;

import dev.darcosse.shinydex.advancement.ModAdvancement;
import dev.darcosse.shinydex.config.ConfigManager;
import dev.darcosse.shinydex.config.ShinyCharmConfig;
import dev.darcosse.shinydex.utils.AdvancementUtils;
import dev.darcosse.shinydex.utils.CharmRequirements;
import dev.darcosse.shinydex.utils.PokedexRegionUtils;
import dev.darcosse.shinydex.utils.RegionUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Grants the Pokedex completion advancements and the Shiny Charm.
 *
 * Called by each platform module on every server tick, but the work is throttled
 * and skipped wherever possible. The previous version scanned all ten regions
 * for every player twenty times a second: roughly 2000 species resolutions and
 * 4000 Pokedex lookups per player per tick, to answer a question that changes
 * once every few hours of play.
 */
public final class HandleAdvancement {

    /**
     * Ticks between two checks. Completing a dex is not time-sensitive, and the
     * player has to catch something for the answer to change at all. Lowering
     * it makes unlocks feel snappier at a proportional CPU cost; raising it
     * delays toasts and the charm by that much.
     */
    private static final int CHECK_INTERVAL = 100; // 5 seconds

    private static int tickCounter = 0;

    /** Inventory#getFreeSlot()'s answer when every main slot is taken. */
    private static final int NO_FREE_SLOT = -1;

    /** Action bar hint shown while the charm waits for a free slot. */
    private static final String INVENTORY_FULL_KEY = "message.shinydex.charm_inventory_full";

    private HandleAdvancement() {
    }

    /** Tick entry point: runs a full check for every online player once per interval. */
    public static void checkAdvancements(MinecraftServer server) {
        if (++tickCounter < CHECK_INTERVAL) return;
        tickCounter = 0;

        // Read once per pass so every player in it is judged by the same rules,
        // even if /shinydex reload lands mid-pass.
        ShinyCharmConfig config = ConfigManager.get();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            checkPlayer(server, player, config);
        }
    }

    /** All per-player work for one pass, in dependency order. */
    private static void checkPlayer(MinecraftServer server, ServerPlayer player, ShinyCharmConfig config) {
        AdvancementUtils.grantAdvancement(player, ModAdvancement.ROOT.getAdvancement(server));

        // Must run before the region loop: it reads the National advancement as
        // it stood BEFORE this version could grant it, and the loop below may
        // grant it in this very pass.
        migrateLegacyOwner(server, player);

        for (RegionUtils region : RegionUtils.values()) {
            AdvancementHolder advancement = ModAdvancement.getAdvancement(server, region.id());
            if (advancement == null) continue;

            // The advancement itself is the source of truth, and asking it is
            // free. Once a region is done it is never recomputed again — which
            // is the common case for any established player.
            if (AdvancementUtils.isDone(player, advancement)) continue;

            if (PokedexRegionUtils.isRegionCompleted(player, region)) {
                AdvancementUtils.grantAdvancement(player, advancement);
            }
        }

        updateCharmUnlock(server, player, config);
    }

    /**
     * One-time detection of players who earned the charm under the old rule.
     *
     * Before the requirement became configurable, the National advancement
     * carried the charm as its loot reward. Those players already hold the
     * item and must neither receive a second one nor lose their entitlement
     * if the current requirement has since grown (new species implemented,
     * stricter config). They get the entitlement directly and are flagged as
     * legacy owners so no reward is handed out.
     *
     * The CHARM_MIGRATION_CHECKED flag makes this run once per player: a player
     * who completes the National Pokedex later, under this version, is not a
     * legacy owner and must get the item normally.
     */
    private static void migrateLegacyOwner(MinecraftServer server, ServerPlayer player) {
        AdvancementHolder checked = ModAdvancement.CHARM_MIGRATION_CHECKED.getAdvancement(server);
        // Without the flag advancement the migration cannot be made one-shot,
        // so it is skipped rather than risk flagging new players as legacy.
        if (checked == null || AdvancementUtils.isDone(player, checked)) return;

        AdvancementHolder national = ModAdvancement.COMPLETED_NATIONAL_DEX.getAdvancement(server);

        if (AdvancementUtils.isDone(player, national)) {
            AdvancementUtils.grantAdvancement(player, ModAdvancement.LEGACY_CHARM_OWNER.getAdvancement(server));
            AdvancementUtils.grantAdvancement(player, ModAdvancement.SHINY_CHARM_UNLOCKED.getAdvancement(server));
        }

        AdvancementUtils.grantAdvancement(player, checked);
    }

    /**
     * Grants the charm entitlement once the configured requirement is met, and
     * hands over the item the first time.
     *
     * The requirement scan only runs while the player is still locked, so an
     * established player costs a single advancement lookup here.
     */
    private static void updateCharmUnlock(MinecraftServer server, ServerPlayer player, ShinyCharmConfig config) {
        AdvancementHolder unlocked = ModAdvancement.SHINY_CHARM_UNLOCKED.getAdvancement(server);
        if (unlocked == null) return;

        if (!AdvancementUtils.isDone(player, unlocked)) {
            if (!config.shinyCharm.enabled || !config.unlock.enabled) return;
            if (!CharmRequirements.isMet(player, config.unlock)) return;

            AdvancementUtils.grantAdvancement(player, unlocked);
        }

        deliverCharmItem(server, player);
    }

    /**
     * Gives the item through the reward advancement, unless the player already
     * received one — either from that reward or, for legacy owners, from the
     * old National advancement.
     *
     * Checked on every pass for unlocked players rather than only at the moment
     * of unlocking, so a server stopping between the two grants cannot leave a
     * player entitled but empty-handed. The same retry is what makes waiting
     * for a free slot possible: the delivery simply happens on the first pass
     * where the player has room.
     */
    private static void deliverCharmItem(MinecraftServer server, ServerPlayer player) {
        AdvancementHolder reward = ModAdvancement.SHINY_CHARM_REWARD.getAdvancement(server);
        if (reward == null || AdvancementUtils.isDone(player, reward)) return;

        if (AdvancementUtils.isDone(player, ModAdvancement.LEGACY_CHARM_OWNER.getAdvancement(server))) return;

        // With a full inventory the vanilla reward drops the item at the
        // player's feet and the reward is marked done anyway: the charm could
        // then despawn after five minutes or fall into the void, and nothing
        // would ever hand it out again. Waiting for room avoids that.
        if (!hasFreeSlot(player)) {
            // Action bar rather than chat: this repeats every pass until the
            // player makes room, and would flood the chat.
            player.displayClientMessage(
                    Component.translatable(INVENTORY_FULL_KEY).withStyle(ChatFormatting.YELLOW),
                    true);
            return;
        }

        AdvancementUtils.grantAdvancement(player, reward);
    }

    /**
     * Whether the main inventory (hotbar included) has an empty slot.
     *
     * An empty slot is required, not merely room in a stack: the charm stacks
     * to 1, so it can never merge into an existing stack. Armour and offhand
     * are not considered, as the vanilla reward does not place items there.
     */
    private static boolean hasFreeSlot(ServerPlayer player) {
        // UNVERIFIED API: Inventory#getFreeSlot() — returns -1 when the main
        // inventory is full. If it does not compile, this is the line to adapt.
        return player.getInventory().getFreeSlot() != NO_FREE_SLOT;
    }
}
