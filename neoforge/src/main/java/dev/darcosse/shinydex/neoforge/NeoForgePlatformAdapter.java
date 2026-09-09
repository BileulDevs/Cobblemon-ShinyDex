package dev.darcosse.shinydex.neoforge;

import dev.darcosse.shiny_charm.item.ModItems;
import dev.darcosse.shiny_charm.platform.PlatformAdapter;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.loading.FMLPaths;
import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.Path;

public class NeoForgePlatformAdapter implements PlatformAdapter {

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isWearingShinyCharm(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findFirstCurio(ModItems.SHINY_CHARM.get()).isPresent())
                .orElse(false);
    }
}
