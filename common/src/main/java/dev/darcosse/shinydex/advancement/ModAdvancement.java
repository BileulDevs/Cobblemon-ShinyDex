package dev.darcosse.shinydex.advancement;

import dev.darcosse.shinydex.ShinyCharm;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Every advancement this mod grants from code, with the key used to look the
 * region ones up by region id.
 *
 * All of them use an impossible criterion in their JSON: the only way to get
 * them is through HandleAdvancement, which is what makes them reliable flags.
 */
public enum ModAdvancement {
    ROOT("root", "Welcome to Silowmons", "root"),

    COMPLETED_JOHTO_DEX("completed_johto_dex", "Completed Johto Dex", "johto"),
    COMPLETED_KANTO_DEX("completed_kanto_dex", "Completed Kanto Dex", "kanto"),
    COMPLETED_HOENN_DEX("completed_hoenn_dex", "Completed Hoenn Dex", "hoenn"),
    COMPLETED_SINNOH_DEX("completed_sinnoh_dex", "Completed Sinnoh Dex", "sinnoh"),
    COMPLETED_UNOVA_DEX("completed_unova_dex", "Completed Unova Dex", "unova"),
    COMPLETED_KALOS_DEX("completed_kalos_dex", "Completed Kalos Dex", "kalos"),
    COMPLETED_ALOLA_DEX("completed_alola_dex", "Completed Alola Dex", "alola"),
    COMPLETED_GALAR_DEX("completed_galar_dex", "Completed Galar Dex", "galar"),
    COMPLETED_PALDEA_DEX("completed_paldea_dex", "Completed Paldea Dex", "paldea"),

    COMPLETED_NATIONAL_DEX("completed_national_dex", "Complete National Dex", "national"),

    /**
     * Visible advancement marking that the player has earned the charm under
     * the configured requirement. This is the entitlement the spawn check
     * reads; it carries no reward itself.
     */
    SHINY_CHARM_UNLOCKED("shiny_charm_unlocked", "Shiny Charm Unlocked", "shiny_charm_unlocked"),

    /**
     * Hidden advancement whose loot reward hands over the charm item. Kept
     * separate from the entitlement so a player can be entitled without
     * receiving a second item (see LEGACY_CHARM_OWNER).
     */
    SHINY_CHARM_REWARD("shiny_charm_reward", "Shiny Charm Reward", "shiny_charm_reward"),

    /**
     * Hidden flag for players who completed the National Pokedex before the
     * requirement became configurable: they already received the item from
     * the old National advancement reward.
     */
    LEGACY_CHARM_OWNER("legacy_charm_owner", "Legacy Charm Owner", "legacy_charm_owner"),

    /**
     * Hidden flag set the first time a player is examined by this version, so
     * the legacy detection above runs exactly once per player.
     */
    CHARM_MIGRATION_CHECKED("charm_migration_checked", "Charm Migration Checked", "charm_migration_checked");

    private final ResourceLocation identifier;
    private final String key;

    /**
     * @param path        the advancement JSON's path under data/shinydex/advancement
     * @param displayName developer-facing label only; players see the JSON's translated title
     * @param key         lookup key; for regions it must equal RegionUtils#id
     */
    ModAdvancement(String path, String displayName, String key) {
        this.identifier = ResourceLocation.fromNamespaceAndPath(ShinyCharm.MOD_ID, path);
        this.key = key;
    }

    /** This advancement as loaded by the server, or null if its JSON is missing. */
    public AdvancementHolder getAdvancement(MinecraftServer server) {
        return getAdvancement(server, this.identifier);
    }

    /** Any advancement by id, tolerating a server that is not running yet. */
    public static AdvancementHolder getAdvancement(MinecraftServer server, ResourceLocation identifier) {
        if (server == null) return null;
        return server.getAdvancements().get(identifier);
    }

    /** The advancement registered under a key, typically a region id. */
    public static AdvancementHolder getAdvancement(MinecraftServer server, String dex) {
        for (ModAdvancement modAdv : values()) {
            if (modAdv.key.equalsIgnoreCase(dex)) {
                return modAdv.getAdvancement(server);
            }
        }
        return null;
    }
}
