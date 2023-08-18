package io.github.mortuusars.exposure.client.gui.screen.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.component.ShutterSpeed;
import io.github.mortuusars.exposure.camera.infrastructure.SynchronizedCameraInHandActions;
import io.github.mortuusars.exposure.config.Config;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ShutterSpeedButton extends ImageButton {
    private final Screen screen;
    private final ResourceLocation texture;
    private final List<ShutterSpeed> shutterSpeeds;

    private int currentShutterSpeedIndex = 0;
    private long lastChangeTime;

    public ShutterSpeedButton(Screen screen, int x, int y, int width, int height, ResourceLocation texture) {
        super(x, y, width, height, 112, 0, height, texture, 256, 256, button -> {}, Button.NO_TOOLTIP, Component.empty());
        this.screen = screen;
        this.texture = texture;

        CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);

        shutterSpeeds = camera.getItem().getAllShutterSpeeds(camera.getStack());

        ShutterSpeed shutterSpeed = camera.getItem().getShutterSpeed(camera.getStack());
        if (!shutterSpeeds.contains(shutterSpeed))
            shutterSpeed = camera.getItem().getDefaultShutterSpeed(camera.getStack());

        for (int i = 0; i < shutterSpeeds.size(); i++) {
            if (shutterSpeed.equals(shutterSpeeds.get(i)))
                currentShutterSpeedIndex = i;
        }
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(SimpleSoundInstance.forUI(Exposure.SoundEvents.CAMERA_BUTTON_CLICK.get(),
                Objects.requireNonNull(Minecraft.getInstance().level).random.nextFloat() * 0.05f + 0.9f + currentShutterSpeedIndex * 0.01f, 0.75f));
    }

    @Override
    public void renderButton(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        int offset = this.getYImage(this.isHoveredOrFocused());

        // Button
        blit(poseStack, x, y, 112, height  * (offset - 1), width, height);

        CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);
        ShutterSpeed shutterSpeed = camera.getItem().getShutterSpeed(camera.getStack());
        String text = shutterSpeed.toString();
        if (shutterSpeed.equals(camera.getItem().getDefaultShutterSpeed(camera.getStack())))
            text = text + "•";

        Font font = minecraft.font;
        int textWidth = font.width(text);
        int xPos = 35 - (textWidth / 2);

        font.draw(poseStack, text, x + xPos, y + 4, Config.Client.getSecondaryFontColor());
        font.draw(poseStack, text, x + xPos, y + 3, Config.Client.getMainFontColor());
    }

    @Override
    public void renderToolTip(@NotNull PoseStack poseStack, int mouseX, int mouseY) {
        screen.renderTooltip(poseStack, Component.translatable("gui.exposure.viewfinder.shutter_speed.tooltip"), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered) {
            cycleShutterSpeed(button == 1);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (System.currentTimeMillis() - lastChangeTime > 40)
            cycleShutterSpeed(delta > 0d);

        return true;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        boolean pressed = super.keyPressed(pKeyCode, pScanCode, pModifiers);

        if (pressed)
            cycleShutterSpeed(Screen.hasShiftDown());

        return pressed;
    }

    public void cycleShutterSpeed(boolean reverse) {
        int prevIndex = currentShutterSpeedIndex;
        currentShutterSpeedIndex = Mth.clamp(currentShutterSpeedIndex + (reverse ? -1 : 1), 0, shutterSpeeds.size() - 1);

        if (prevIndex != currentShutterSpeedIndex) {
            CameraInHand camera = Exposure.getCamera().getCameraInHand(Minecraft.getInstance().player);
            if (!camera.isEmpty()) {
                if (camera.getItem().getShutterSpeed(camera.getStack()) != shutterSpeeds.get(currentShutterSpeedIndex)) {
                    SynchronizedCameraInHandActions.setShutterSpeed(shutterSpeeds.get(currentShutterSpeedIndex));
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    lastChangeTime = System.currentTimeMillis();
                }
            }
        }
    }
}
