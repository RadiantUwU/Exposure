package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.PlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CameraItemClientExtensions {
    public static void applyDefaultHoldingPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (PlatformHelper.isModLoaded("realcamera")) {
            return;
        }

        model.head.xRot += 0.4f; // If we turn head down completely - arms will be too low.
        if (arm == HumanoidArm.RIGHT) {
            AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, true);
        } else if (arm == HumanoidArm.LEFT) {
            AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, false);
        }
        model.head.xRot += 0.3f;
    }

    public static void applySelfieHoldingPose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, boolean undoArmBobbing) {
        ModelPart cameraArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;

        // Arm follows camera:
        cameraArm.xRot = (model.head.xRot + Math.abs(model.head.xRot * 0.13f)) + (-(float) Math.PI / 2F);
        cameraArm.yRot = model.head.yRot + (arm == HumanoidArm.RIGHT ? -0.25f : 0.25f);
        if (model.head.xRot <= 0) {
            cameraArm.zRot = -(model.head.xRot * 0.15f);
        } else {
            cameraArm.zRot = -(model.head.xRot * 0.22f);
        }

        if (undoArmBobbing) {
            AnimationUtils.bobModelPart(cameraArm, entity.tickCount + Minecraft.getInstance().getFrameTime(),
                    arm == HumanoidArm.LEFT ? 1.0F : -1.0F);
        }
    }

    public static float itemPropertyFunction(ItemStack stack, ClientLevel clientLevel, LivingEntity livingEntity, int seed) {
        if (stack.getItem() instanceof CameraItem cameraItem) {
            if (cameraItem.isActive(stack)) {
                if (cameraItem.isInSelfieMode(stack))
                    return livingEntity == Minecraft.getInstance().player ? 0.2f : 0.3f;

                return 0.1f;
            }
        }

        return 0f;
    }

    public static void releaseUseButton() {
        Minecraft.getInstance().options.keyUse.setDown(false);
    }
}
