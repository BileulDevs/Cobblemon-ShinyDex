package dev.darcosse.shinydex.fabric;

import dev.darcosse.shinydex.item.ModItems;
import dev.darcosse.shinydex.platform.PlatformAdapter;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;

import java.nio.file.Path;

public class FabricPlatformAdapter implements PlatformAdapter {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isWearingShinyCharm(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .map(component -> component.isEquipped(ModItems.SHINY_CHARM.get()))
                .orElse(false);
    }
}
