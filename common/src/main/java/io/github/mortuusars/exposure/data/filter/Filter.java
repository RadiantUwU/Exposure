package io.github.mortuusars.exposure.data.filter;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

@SuppressWarnings("ClassCanBeRecord")
public class Filter {
    public static final ResourceLocation DEFAULT_GLASS_TEXTURE = Exposure.resource("textures/gui/filter/stained_glass.png");
    public static final int DEFAULT_TINT_COLOR = 0xFFFFFF;

    private final Ingredient ingredient;
    private final ResourceLocation shader;
    private final ResourceLocation attachmentTexture;
    private final int tintColor;

    public Filter(Ingredient ingredient, ResourceLocation shader, ResourceLocation attachmentTexture, int tintColor) {
        this.ingredient = ingredient;
        this.shader = shader;
        this.attachmentTexture = attachmentTexture;
        this.tintColor = tintColor;
    }

    public boolean matches(ItemStack stack) {
        return ingredient.test(stack);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public ResourceLocation getShader() {
        return shader;
    }

    public ResourceLocation getAttachmentTexture() {
        return attachmentTexture;
    }

    public int getTintColor() {
        return tintColor;
    }
}
