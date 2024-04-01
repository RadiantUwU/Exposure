package io.github.mortuusars.exposure.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FiltersResourceLoader extends SimpleJsonResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "filters";

    public FiltersResourceLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> content, ResourceManager resourceManager, ProfilerFiller profiler) {
        ConcurrentMap<Ingredient, ResourceLocation> filters = new ConcurrentHashMap<>();

        LogUtils.getLogger().info("Loading exposure filters:");

        for (var entry : content.entrySet()) {
            // Lenses should be in data/exposure/filters folder.
            // Excluding other namespaces because it potentially can cause conflicts,
            // if some other mod adds their own type of 'filter'.
            if (!entry.getKey().getNamespace().equals(Exposure.ID))
                continue;

            try {
                JsonObject jsonObject = entry.getValue().getAsJsonObject();
                JsonElement item = jsonObject.get("item");

                Ingredient ingredient = Ingredient.fromJson(item);
                if (ingredient.isEmpty())
                    throw new IllegalArgumentException("'item' cannot be empty.");

                String shader = jsonObject.get("shader").getAsString();

                filters.put(ingredient, new ResourceLocation(shader));

                LogUtils.getLogger().info("Filter [" + entry.getKey() + ", " + shader + "] added.");
            }
            catch (Exception e) {
                LogUtils.getLogger().error(e.toString());
            }
        }

        if (filters.isEmpty())
            LogUtils.getLogger().info("No filters have been loaded.");

        Filters.reload(filters);
    }
}