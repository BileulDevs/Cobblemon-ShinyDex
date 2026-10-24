package dev.darcosse.shinydex.utils;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * Small helpers around a player's advancement progress, tolerant of an
 * advancement whose JSON failed to load (null holder).
 */
public final class AdvancementUtils {

    private AdvancementUtils() {
    }

    /**
     * Completes every remaining criterion, which triggers the advancement's
     * toast, chat announcement and rewards exactly as a vanilla grant would.
     */
    public static void grantAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);

        if (!progress.isDone()) {
            for (String criterion : progress.getRemainingCriteria()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }

    /**
     * Whether the player has this advancement. A missing advancement counts as
     * not done, so a broken datapack fails closed.
     */
    public static boolean isDone(ServerPlayer player, AdvancementHolder advancement) {
        if (advancement == null) return false;
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
