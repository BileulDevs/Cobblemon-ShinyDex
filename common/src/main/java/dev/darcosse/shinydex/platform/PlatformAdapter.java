package dev.darcosse.shinydex.platform;

import net.minecraft.world.entity.LivingEntity;

import java.nio.file.Path;

/**
 * Tout ce que le code commun ne peut pas faire lui-meme.
 * Une implementation par loader : Trinkets cote Fabric, Curios cote NeoForge.
 */
public interface PlatformAdapter {

    /** Dossier de config du jeu (config/ a la racine de l'instance). */
    Path getConfigDir();

    /** L'entite porte-t-elle le Shiny Charm dans un slot d'accessoire ? */
    boolean isWearingShinyCharm(LivingEntity entity);
}
