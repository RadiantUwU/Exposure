package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.render.GammaModifier;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class ExposureLightTextureMixin {
    @Inject(method = "getBrightness", at = @At(value = "RETURN"), cancellable = true)
    private static void modifyBrightness(DimensionType pDimensionType, int pLightLevel, CallbackInfoReturnable<Float> cir) {
        if (GammaModifier.getAdditionalBrightness() != 0f)
            cir.setReturnValue(GammaModifier.modifyBrightness(cir.getReturnValue()));
    }
}
