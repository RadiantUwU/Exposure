package io.github.mortuusars.exposure.fabric.mixin.create;

import com.simibubi.create.compat.emi.CreateEmiPlugin;
import com.simibubi.create.compat.emi.recipes.SequencedAssemblyEmiRecipe;
import dev.emi.emi.api.EmiRegistry;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.fabric.integration.create.CreateFilmDeveloping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yes, it's ugly.
 */
@Mixin(value = CreateEmiPlugin.class, remap = false)
public abstract class CreateEmiPluginMixin {
    @Inject(method = "register(Ldev/emi/emi/api/EmiRegistry;)V", at = @At("RETURN"))
    public void onRegister(EmiRegistry registry, CallbackInfo ci) {
        registry.addRecipe(new SequencedAssemblyEmiRecipe(CreateFilmDeveloping.createSequencedDevelopingRecipe(FilmType.BLACK_AND_WHITE)));
        registry.addRecipe(new SequencedAssemblyEmiRecipe(CreateFilmDeveloping.createSequencedDevelopingRecipe(FilmType.COLOR)));
    }
}
