package io.github.mortuusars.exposure.forge.data.generation.provider;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ItemTagsProvider /*extends net.minecraft.data.tags.ItemTagsProvider*/ {
    /*public ItemTagsProvider(DataGenerator generator, CompletableFuture<HolderLookup.Provider> lookupProvider, BlockTagsProvider blockTagsProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator.getPackOutput(), lookupProvider, blockTagsProvider.contentsGetter(), Exposure.ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Exposure.Tags.Items.FILM_ROLLS).add(Exposure.Items.BLACK_AND_WHITE_FILM.get(), Exposure.Items.COLOR_FILM.get());
        tag(Exposure.Tags.Items.DEVELOPED_FILM_ROLLS).add(Exposure.Items.DEVELOPED_BLACK_AND_WHITE_FILM.get(), Exposure.Items.DEVELOPED_COLOR_FILM.get());

        tag(Exposure.Tags.Items.CYAN_PRINTING_DYES).add(Items.CYAN_DYE);
        tag(Exposure.Tags.Items.MAGENTA_PRINTING_DYES).add(Items.MAGENTA_DYE);
        tag(Exposure.Tags.Items.YELLOW_PRINTING_DYES).add(Items.YELLOW_DYE);
        tag(Exposure.Tags.Items.BLACK_PRINTING_DYES).add(Items.BLACK_DYE);
        tag(Exposure.Tags.Items.PHOTO_PAPERS).add(Items.PAPER);
    }

    private void optionalTags(TagAppender<Item> tag, String namespace, String... items) {
        for (String item : items) {
            tag.addOptionalTag(new ResourceLocation(namespace, item));
        }
    }

    private void optional(TagAppender<Item> tag, String namespace, String... items) {
        for (String item : items) {
            tag.addOptional(new ResourceLocation(namespace, item));
        }
    }*/
}