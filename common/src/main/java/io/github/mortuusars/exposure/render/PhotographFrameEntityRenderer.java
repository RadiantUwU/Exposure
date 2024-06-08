package io.github.mortuusars.exposure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.entity.PhotographFrameEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class PhotographFrameEntityRenderer<T extends PhotographFrameEntity> extends EntityRenderer<T> {
    private final BlockRenderDispatcher blockRenderer;

    public PhotographFrameEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        blockRenderer = context.getBlockRenderDispatcher();
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
        if (Minecraft.getInstance().hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) {
            Minecraft.getInstance().crosshairPickEntity = entity;
        }

        Direction direction = entity.getDirection();
        int size = entity.getSize();
        boolean isStripped = entity.isStripped();

        poseStack.pushPose();
        // Offsets name tag rendering to be like item frame:
        poseStack.translate(direction.getStepX() * 0.3f, direction.getStepY() * 0.3f, direction.getStepZ() * 0.3f);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        poseStack.pushPose();

        // thickness of the frame is 1px (0.5 - (1/16 * 0.5)) (0.5 is because we are offsetting from the center)
        // stripped frame is thin, so 1/16 becomes 0.15/16 (thickness of the backplate)
        double hangOffset = 0.46875;
        poseStack.translate(direction.getStepX() * hangOffset, direction.getStepY() * hangOffset, direction.getStepZ() * hangOffset);

        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));

        if (!entity.isInvisible()) {
            renderFrame(entity, poseStack, bufferSource, packedLight, size, isStripped);
        }

        ItemStack item = entity.getItem();
        if (!item.isEmpty()) {
            renderPhotograph(entity, poseStack, bufferSource, packedLight, item, size, isStripped);
        }

        poseStack.popPose();
    }

    private void renderFrame(@NotNull T entity, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, 
                             int packedLight, int size, boolean isStripped) {
        poseStack.pushPose();
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        ModelResourceLocation modelLocation = getModelLocation(entity, size, isStripped);
        BakedModel model = blockRenderer.getBlockModelShaper().getModelManager().getModel(modelLocation);
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(Sheets.solidBlockSheet()),
                null, model, 1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    public ModelResourceLocation getModelLocation(T entity, int size, boolean isStripped) {
        if (size == 0)
            return isStripped ? ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL_STRIPPED : ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL;
        else if (size == 1)
            return isStripped ? ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM_STRIPPED : ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM;
        else
            return isStripped ? ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE_STRIPPED : ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE;
    }

    private void renderPhotograph(@NotNull T entity, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                                  int packedLight, ItemStack item, int size, boolean isStripped) {
        poseStack.pushPose();

        boolean frameInvisible = entity.isInvisible();

        float frameBorderOffset = frameInvisible || isStripped ? 0f : 0.125f; // (2px / 16px = 0.125)
        float offsetFromCenter = frameInvisible ? 0.497f : 0.48f;
        float desiredSize = size + 1 - frameBorderOffset * 2;
        float scale = desiredSize / (float)ExposureClient.getExposureRenderer().getSize();

        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.getItemRotation() * 360.0F / 4.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(-0.5 * (size + 1) + frameBorderOffset, -0.5 * (size + 1) + frameBorderOffset, offsetFromCenter);
        poseStack.scale(scale, scale, 1);

        boolean isGlowing = entity.isGlowing();
        if (isGlowing)
            packedLight = LightTexture.FULL_BRIGHT;

        int brightness = isGlowing ? 255 : getPhotographBrightness(entity);

        PhotographRenderer.render(item, false, false, poseStack, bufferSource, packedLight,
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
        shadeFactor += (1f - shadeFactor) * 0.2f;

        int shadedBrightness = (int)(255 * shadeFactor);
        int missingLight = 255 - shadedBrightness;
        int lightUp = (int)(missingLight * (lightLevel / 15f * 0.5f));
        return Math.min(255, shadedBrightness + lightUp);
    }

    @Override
    protected boolean shouldShowName(T entity) {
        if (Minecraft.renderNames() && (!entity.getItem().isEmpty() && entity.getItem().hasCustomHoverName()
                && Minecraft.getInstance().crosshairPickEntity == entity)) {
            double distSqr = Minecraft.getInstance().crosshairPickEntity.distanceToSqr(entity);
            float showRangeSqr = entity.isDiscrete() ? 32.0f : 64.0f;
            return distSqr < (double) (showRangeSqr * showRangeSqr);
        }
        return false;
    }

    @Override
    protected void renderNameTag(T entity, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.renderNameTag(entity, entity.getItem().getHoverName(), poseStack, buffer, packedLight);
    }
}
