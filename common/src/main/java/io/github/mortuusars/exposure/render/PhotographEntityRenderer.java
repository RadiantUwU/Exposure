package io.github.mortuusars.exposure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.entity.PhotographEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import org.jetbrains.annotations.NotNull;

public class PhotographEntityRenderer<T extends PhotographEntity> extends EntityRenderer<T> {

    public PhotographEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T pEntity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();

        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.getRotation() * 360.0F / 4.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        poseStack.translate(-0.5, -0.5, 1f / 32f - 0.005);
        float scale = 1f / ExposureClient.getExposureRenderer().getSize();
        poseStack.scale(scale, scale, -scale);

        boolean isGlowing = entity.isGlowing();
        if (isGlowing)
            packedLight = LightTexture.FULL_BRIGHT;

        int brightness = isGlowing ? 255 : getPhotographBrightness(entity);

        ItemStack item = entity.getItem();

        PhotographRenderer.render(item, !entity.isInvisible(), true, poseStack, bufferSource, packedLight,
                brightness, brightness, brightness, 255);

        poseStack.popPose();
    }

    public int getPhotographBrightness(T entity) {
        if (entity.getDirection() == Direction.UP)
            return 255;

        // Darken the photo same way as the block sides darken,
        // but not quite as much and allow light sources to brighten it:
        int lightLevel = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
        float shadeFactor = entity.level().getShade(entity.getDirection(), true);
        shadeFactor += (1f - shadeFactor) * 0.05f;

        int shadedBrightness = (int)(255 * shadeFactor);
        int missingLight = 255 - shadedBrightness;
        int lightUp = (int)(missingLight * (lightLevel / 15f * 0.7f));
        return Math.min(255, shadedBrightness + lightUp);
    }
}
