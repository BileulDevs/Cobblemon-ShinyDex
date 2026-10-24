package dev.darcosse.shinydex.config;

import com.google.gson.annotations.SerializedName;

/**
 * What a player must have done with a species for it to count towards the
 * Shiny Charm requirement.
 *
 * The serialized names are lowercase so the config file reads naturally
 * ("mode": "seen"). An unknown value in the file deserializes to null, which
 * ConfigManager replaces with the default.
 */
public enum UnlockMode {

    /** The species must be registered as caught in the Pokedex. */
    @SerializedName("caught")
    CAUGHT,

    /** Seeing the species is enough; catching it also counts, as it implies having seen it. */
    @SerializedName("seen")
    SEEN
}
