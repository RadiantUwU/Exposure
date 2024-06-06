package io.github.mortuusars.exposure.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.entity.PhotographFrameEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
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
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        boolean invisible = entity.isInvisible();

        poseStack.pushPose();

        Direction direction = entity.getDirection();
        double d = 0.46875;
        poseStack.translate(direction.getStepX() * d, direction.getStepY() * d, direction.getStepZ() * d);

        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees((entity.getRotation() * 360.0F / 4.0F)));
//        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        if (!invisible) {
            renderFrame(entity, poseStack, bufferSource, packedLight);
        }

        ItemStack item = entity.getItem();
        if (!item.isEmpty()) {
            poseStack.pushPose();
            int size = entity.getSize();
            float scale = (size + 1) / (float)ExposureClient.getExposureRenderer().getSize();
//
//            scale *= size + 1;
//
            scale = scale * (1f - (4 / 16f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(-0.5 * (size + 1), -0.5 * (size + 1), 0.48);
            poseStack.scale(scale, scale, 0);
//            poseStack.translate(-0.5 * scale, -0.5 * scale, -0.4875);

            if (entity.isGlowing())
                packedLight = LightTexture.FULL_BRIGHT;

            PhotographRenderer.render(item, false, false, poseStack, bufferSource, packedLight,
                    240, 240, 240, 255);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderFrame(@NotNull T entity, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        ModelResourceLocation modelLocation;
        int size = entity.getSize();
        if (size == 0)
            modelLocation = ExposureClient.Models.PHOTOGRAPH_FRAME_SMALL;
        else if (size == 1)
            modelLocation = ExposureClient.Models.PHOTOGRAPH_FRAME_MEDIUM;
        else
            modelLocation = ExposureClient.Models.PHOTOGRAPH_FRAME_LARGE;

        BakedModel model = blockRenderer.getBlockModelShaper().getModelManager().getModel(modelLocation);
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(Sheets.solidBlockSheet()),
                null, model, 1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
