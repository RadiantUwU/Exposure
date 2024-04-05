package io.github.mortuusars.exposure.fabric.resources;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.LensesDataLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public class FabricLensesDataLoader extends LensesDataLoader implements IdentifiableResourceReloadListener {
    public static final ResourceLocation ID = Exposure.resource("lenses_data");
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
