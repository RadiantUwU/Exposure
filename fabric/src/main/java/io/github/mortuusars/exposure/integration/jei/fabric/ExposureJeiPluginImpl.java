package io.github.mortuusars.exposure.integration.jei.fabric;

import com.simibubi.create.Create;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.fabric.integration.create.CreateFilmDeveloping;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExposureJeiPluginImpl {
    public static void addSequencedDevelopingRecipes(@NotNull IRecipeRegistration registration) {
        registration.addRecipes(new RecipeType<>(Create.asResource("sequenced_assembly"), SequencedAssemblyRecipe.class),
                List.of(CreateFilmDeveloping.createSequencedDevelopingRecipe(FilmType.BLACK_AND_WHITE),
                        CreateFilmDeveloping.createSequencedDevelopingRecipe(FilmType.COLOR)));
    }
}
