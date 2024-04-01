package io.github.mortuusars.exposure.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LensesDataLoader extends SimpleJsonResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "lenses";

    public LensesDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> content, ResourceManager resourceManager, ProfilerFiller profiler) {
        ConcurrentMap<Ingredient, FocalRange> lenses = new ConcurrentHashMap<>();

        LogUtils.getLogger().info("Loading exposure lenses:");

        for (var entry : content.entrySet()) {
            // Lenses should be in data/exposure/lenses folder.
            // Excluding other namespaces because it potentially can cause conflicts,
            // if some other mod adds their own type of 'lens'.
            if (!entry.getKey().getNamespace().equals(Exposure.ID))
                continue;

            try {
                JsonObject jsonObject = entry.getValue().getAsJsonObject();
                JsonElement item = jsonObject.get("item");

                Ingredient ingredient = Ingredient.fromJson(item);
                if (ingredient.isEmpty())
                    throw new IllegalArgumentException("'item' cannot be empty.");

                JsonElement value = jsonObject.get("focal_range");
                FocalRange focalRange = FocalRange.fromJson(value);

                lenses.put(ingredient, focalRange);

                LogUtils.getLogger().info("Lens [" + entry.getKey() + ", " + focalRange + "] added.");
            }
            catch (Exception e) {
                LogUtils.getLogger().error(e.toString());
            }
        }

        if (lenses.isEmpty())
            LogUtils.getLogger().info("No lenses have been loaded.");

        Lenses.reload(lenses);
    }
}