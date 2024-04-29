package io.github.mortuusars.exposure.camera;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public record AttachmentType(String id, int slot, Predicate<ItemStack> itemPredicate, AttachmentSound sound) {
    public boolean matches(ItemStack stack) {
        return itemPredicate.test(stack);
    }

    @Override
    public String toString() {
        return "AttachmentType{" +
                "id='" + id + '\'' +
                ", slot=" + slot +
                '}';
    }
}
