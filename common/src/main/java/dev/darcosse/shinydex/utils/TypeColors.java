package dev.darcosse.shinydex.utils;

import net.minecraft.network.chat.TextColor;

import java.util.Locale;
import java.util.Map;

/**
 * Chat colours for the eighteen elemental types.
 *
 * Cobblemon's ElementalType carries a hue used for its own UI, but it is not
 * tuned for dark chat backgrounds. These are the standard type colours, which
 * read well on chat.
 */
public final class TypeColors {

    private static final TextColor FALLBACK = TextColor.fromRgb(0xFFFFFF);

    private static final Map<String, TextColor> COLORS = Map.ofEntries(
            Map.entry("normal", TextColor.fromRgb(0xA8A77A)),
            Map.entry("fire", TextColor.fromRgb(0xEE8130)),
            Map.entry("water", TextColor.fromRgb(0x6390F0)),
            Map.entry("electric", TextColor.fromRgb(0xF7D02C)),
            Map.entry("grass", TextColor.fromRgb(0x7AC74C)),
            Map.entry("ice", TextColor.fromRgb(0x96D9D6)),
            Map.entry("fighting", TextColor.fromRgb(0xC22E28)),
            Map.entry("poison", TextColor.fromRgb(0xA33EA1)),
            Map.entry("ground", TextColor.fromRgb(0xE2BF65)),
            Map.entry("flying", TextColor.fromRgb(0xA98FF3)),
            Map.entry("psychic", TextColor.fromRgb(0xF95587)),
            Map.entry("bug", TextColor.fromRgb(0xA6B91A)),
            Map.entry("rock", TextColor.fromRgb(0xB6A136)),
            Map.entry("ghost", TextColor.fromRgb(0x735797)),
            Map.entry("dragon", TextColor.fromRgb(0x6F35FC)),
            Map.entry("dark", TextColor.fromRgb(0x705746)),
            Map.entry("steel", TextColor.fromRgb(0xB7B7CE)),
            Map.entry("fairy", TextColor.fromRgb(0xD685AD))
    );

    private TypeColors() {
    }

    /** @return the type's colour, or white for an unknown or null type. */
    public static TextColor of(String typeName) {
        if (typeName == null) return FALLBACK;
        return COLORS.getOrDefault(typeName.toLowerCase(Locale.ROOT), FALLBACK);
    }
}
