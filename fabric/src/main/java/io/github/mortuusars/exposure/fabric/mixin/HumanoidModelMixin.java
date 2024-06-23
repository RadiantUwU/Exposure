package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.CameraItemClientExtensions;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> extends AgeableListModel<T> {
    @Shadow public abstract ModelPart getHead();
    @Final
    @Shadow public ModelPart hat;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player))
            return;

        @Nullable Camera<CameraItem> camera = CameraInHand.ofPlayer(player, CameraItem.class);
        if (!(camera instanceof CameraInHand<CameraItem> cameraInHand))
            return;

        HumanoidArm arm = Minecraft.getInstance().options.mainHand().get();
        if (cameraInHand.getHand() == InteractionHand.OFF_HAND)
            arm = arm.getOpposite();

        if (camera.get().getItem().isInSelfieMode(camera.get().getStack()))
            CameraItemClientExtensions.applySelfieHoldingPose((HumanoidModel<?>) (Object) this, entity, arm, false);
        else
            CameraItemClientExtensions.applyDefaultHoldingPose((HumanoidModel<?>) (Object) this, entity, arm);

        hat.copyFrom(getHead());
    }
}
