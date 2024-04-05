package io.github.mortuusars.exposure.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Filters {
    private static ConcurrentMap<Ingredient, ResourceLocation> filters = new ConcurrentHashMap<>();

    public static void reload(ConcurrentMap<Ingredient, ResourceLocation> newFilters) {
        filters.clear();
        filters = newFilters;
    }

    public static Optional<ResourceLocation> getShaderOf(ItemStack stack) {
        for (var filter : filters.entrySet()) {
            if (filter.getKey().test(stack))
                return Optional.of(filter.getValue());
        }

        return Optional.empty();
    }
}
