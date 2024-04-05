package io.github.mortuusars.exposure.forge.data.generation.provider;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.forge.data.generation.recipe.FilmDevelopingFinishedRecipe;
import io.github.mortuusars.exposure.forge.data.generation.recipe.PhotographCopyingFinishedRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RecipesProvider /*extends net.minecraft.data.recipes.RecipeProvider*/ {
    /*public RecipesProvider(DataGenerator generator) {
        super(generator.getPackOutput());
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> recipeConsumer) {
        ResourceLocation bwRecipeId = Exposure.resource("developing_black_and_white_film");
        Advancement.Builder bwAdvancementBuilder = Advancement.Builder.advancement()
                .parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(bwRecipeId))
                .addCriterion("has_black_and_white_film", has(Exposure.Items.BLACK_AND_WHITE_FILM.get()))
                .rewards(AdvancementRewards.Builder.recipe(bwRecipeId))
                .requirements(RequirementsStrategy.OR);

        recipeConsumer.accept(new FilmDevelopingFinishedRecipe(bwRecipeId,
                Exposure.Items.DEVELOPED_BLACK_AND_WHITE_FILM.get(), 1, "",
                List.of(Ingredient.of(Exposure.Items.BLACK_AND_WHITE_FILM.get()), potionIngredient(Potions.WATER)), bwAdvancementBuilder,
                new ResourceLocation(bwRecipeId.getNamespace(), "recipes/" + bwRecipeId.getPath())
        ));

        ResourceLocation colorRecipeId = Exposure.resource("developing_color_film");
        Advancement.Builder colorAdvancementBuilder = Advancement.Builder.advancement()
                .parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(colorRecipeId))
                .addCriterion("has_color_film", has(Exposure.Items.COLOR_FILM.get()))
                .rewards(AdvancementRewards.Builder.recipe(colorRecipeId))
                .requirements(RequirementsStrategy.OR);

        recipeConsumer.accept(new FilmDevelopingFinishedRecipe(colorRecipeId,
                Exposure.Items.DEVELOPED_COLOR_FILM.get(), 1, "",
                List.of(Ingredient.of(Exposure.Items.COLOR_FILM.get()), potionIngredient(Potions.AWKWARD), potionIngredient(Potions.MUNDANE), potionIngredient(Potions.THICK)), colorAdvancementBuilder,
                new ResourceLocation(colorRecipeId.getNamespace(), "recipes/" + colorRecipeId.getPath())
        ));


        ResourceLocation bwPhotoCloningRecipeId = Exposure.resource("cloning_black_and_white_photograph");
        Advancement.Builder bwPhotoCloningAdvancementBuilder = Advancement.Builder.advancement()
                .parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(bwPhotoCloningRecipeId))
                .addCriterion("has_photograph", has(Exposure.Items.PHOTOGRAPH.get()))
                .rewards(AdvancementRewards.Builder.recipe(bwPhotoCloningRecipeId))
                .requirements(RequirementsStrategy.OR);

        CompoundTag bwTag = new CompoundTag();
        bwTag.putString(FrameData.TYPE, FilmType.BLACK_AND_WHITE.getSerializedName());

        recipeConsumer.accept(new PhotographCopyingFinishedRecipe(bwPhotoCloningRecipeId,
                Exposure.Items.PHOTOGRAPH.get(), 1, "",
                List.of(PartialNBTIngredient.of(Exposure.Items.PHOTOGRAPH.get(), bwTag),
                        Ingredient.of(Exposure.Tags.Items.PHOTO_PAPERS),
                        Ingredient.of(Exposure.Tags.Items.BLACK_PRINTING_DYES)), bwPhotoCloningAdvancementBuilder,
                new ResourceLocation(bwPhotoCloningRecipeId.getNamespace(), "recipes/" + bwPhotoCloningRecipeId.getPath())
        ));

        ResourceLocation colorPhotoCloningRecipeId = Exposure.resource("cloning_color_photograph");
        Advancement.Builder colorPhotoCloningAdvancementBuilder = Advancement.Builder.advancement()
                .parent(new ResourceLocation("recipes/root"))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(bwPhotoCloningRecipeId))
                .addCriterion("has_photograph", has(Exposure.Items.PHOTOGRAPH.get()))
                .rewards(AdvancementRewards.Builder.recipe(colorPhotoCloningRecipeId))
                .requirements(RequirementsStrategy.OR);

        recipeConsumer.accept(new PhotographCopyingFinishedRecipe(colorPhotoCloningRecipeId,
                Exposure.Items.PHOTOGRAPH.get(), 1, "",
                List.of(Ingredient.of(Exposure.Items.PHOTOGRAPH.get()),
                        Ingredient.of(Exposure.Tags.Items.PHOTO_PAPERS),
                        Ingredient.of(Exposure.Tags.Items.BLACK_PRINTING_DYES),
                        Ingredient.of(Exposure.Tags.Items.CYAN_PRINTING_DYES),
                        Ingredient.of(Exposure.Tags.Items.MAGENTA_PRINTING_DYES),
                        Ingredient.of(Exposure.Tags.Items.YELLOW_PRINTING_DYES)), colorPhotoCloningAdvancementBuilder,
                new ResourceLocation(colorPhotoCloningRecipeId.getNamespace(), "recipes/" + colorPhotoCloningRecipeId.getPath())
        ));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Exposure.Items.CAMERA.get())
                .pattern("LPB")
                .pattern("IGI")
                .pattern("NIN")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('G', Tags.Items.GLASS_PANES_COLORLESS)
                .define('L', Items.LEVER)
                .define('P', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .define('B', ItemTags.BUTTONS)
                .group("camera")
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(recipeConsumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Exposure.Items.BLACK_AND_WHITE_FILM.get())
                .pattern("NBB")
                .pattern("IGG")
                .pattern("IKK")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', Items.BONE_MEAL)
                .define('G', Items.GUNPOWDER)
                .define('K', Items.DRIED_KELP)
                .unlockedBy("has_camera", has(Exposure.Items.CAMERA.get()))
                .save(recipeConsumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Exposure.Items.COLOR_FILM.get())
                .pattern("NLL")
                .pattern("IGG")
                .pattern("IKK")
                .define('N', Tags.Items.NUGGETS_IRON)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('L', Items.LAPIS_LAZULI)
                .define('G', Tags.Items.NUGGETS_GOLD)
                .define('K', Items.DRIED_KELP)
                .unlockedBy("has_camera", has(Exposure.Items.CAMERA.get()))
                .save(recipeConsumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, Exposure.Items.LIGHTROOM.get())
                .pattern("LT ")
                .pattern("PP ")
                .pattern("PP ")
                .define('L', Items.LANTERN)
                .define('T', Items.REDSTONE_TORCH)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_developed_film", has(Exposure.Tags.Items.DEVELOPED_FILM_ROLLS))
                .save(recipeConsumer);
    }

    private Ingredient potionIngredient(Potion potion) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Potion", Objects.requireNonNull(ForgeRegistries.POTIONS.getKey(potion)).toString());
        return PartialNBTIngredient.of(Items.POTION, tag);
    }*/
}
