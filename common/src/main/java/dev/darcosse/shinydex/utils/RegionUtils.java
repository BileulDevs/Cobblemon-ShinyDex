package dev.darcosse.shinydex.utils;

public enum RegionUtils {
    JOHTO,
    KANTO,
    HOENN,
    SINNOH,
    UNOVA,
    KALOS,
    ALOLA,
    GALAR,
    PALDEA,
    NATIONAL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
