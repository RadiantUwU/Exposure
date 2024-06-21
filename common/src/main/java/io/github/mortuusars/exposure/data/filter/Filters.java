package io.github.mortuusars.exposure.data.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.util.Color;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Filters {
    private static final Map<ResourceLocation, Filter> filters = new HashMap<>();

    public static Map<ResourceLocation, Filter> getFilters() {
        return filters;
    }

    public static Optional<Filter> of(ItemStack stack) {
        for (var filter : filters.values()) {
            if (filter.matches(stack))
                return Optional.of(filter);
        }

        return Optional.empty();
    }

    public static Optional<ResourceLocation> getShaderOf(ItemStack stack) {
        return of(stack).map(Filter::getShader);
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        public static final String DIRECTORY = "filters";

        public Loader() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> content, ResourceManager resourceManager, ProfilerFiller profiler) {
            filters.clear();

            Exposure.LOGGER.info("Loading exposure filters:");

            for (var entry : content.entrySet()) {
                ResourceLocation key = entry.getKey();

                // Lenses should be in data/exposure/filters folder.
                // Excluding other namespaces because it potentially can cause conflicts,
                // if some other mod adds their own type of 'filter'.
                if (!key.getNamespace().equals(Exposure.ID)) {
                    continue;
                }

                JsonObject jsonObject = entry.getValue().getAsJsonObject();

                deserializeFilter(key, jsonObject).ifPresent(filter -> {
                    filters.put(key, filter);
                    Exposure.LOGGER.info("Filter [" + key + ", " + filter.getShader() + "] added.");
                });
            }

            if (filters.isEmpty()) {
                Exposure.LOGGER.info("No filters have been loaded.");
            }
        }

        private Optional<Filter> deserializeFilter(ResourceLocation key, JsonObject jsonObject) {
            try {
                Ingredient ingredient = Ingredient.fromJson(jsonObject.get("item"));
                if (ingredient.isEmpty()) {
                    Exposure.LOGGER.error("Filter '{}' was not loaded: ingredient cannot be empty.", key);
                    return Optional.empty();
                }

                ResourceLocation shader = new ResourceLocation(jsonObject.get("shader").getAsString());

                ResourceLocation attachmentTexture = Filter.DEFAULT_GLASS_TEXTURE;
                if (jsonObject.has("attachment_texture")) {
                    attachmentTexture = new ResourceLocation(jsonObject.get("attachment_texture").getAsString());
                }

                int tintColor = Filter.DEFAULT_TINT_COLOR;
                if (jsonObject.has("tint_color")) {
                    String hexString = jsonObject.get("tint_color").getAsString();
                    tintColor = Color.getRGBFromHex(hexString);
                }

                return Optional.of(new Filter(ingredient, shader, attachmentTexture, tintColor));
            } catch (Exception e) {
                Exposure.LOGGER.error("Filter '{}' was not loaded: {}", key, e);
                return Optional.empty();
            }
        }
    }
}