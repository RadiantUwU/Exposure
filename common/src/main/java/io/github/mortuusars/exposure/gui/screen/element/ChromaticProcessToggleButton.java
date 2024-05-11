package io.github.mortuusars.exposure.gui.screen.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.gui.screen.LightroomScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ChromaticProcessToggleButton extends ImageButton {
    private final Supplier<Lightroom.Process> processGetter;
    private final ResourceLocation texture;
    private final int xTexStart;
    private final int yTexStart;
    private final int yDiffTex;

    public ChromaticProcessToggleButton(int x, int y, OnTooltip onTooltip, OnPress onPress, Supplier<Lightroom.Process> processGetter) {
        super(x, y, 18, 18, 198, 17, 18,
                LightroomScreen.MAIN_TEXTURE, 256, 256, onPress, onTooltip, Component.empty());
        this.processGetter = processGetter;
        this.texture = LightroomScreen.MAIN_TEXTURE;
        this.xTexStart = 198;
        this.yTexStart = 17;
        this.yDiffTex = 18;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        RenderSystem.setShaderTexture(0, texture);
        int i = this.yTexStart;
        if (!this.isActive()) {
            i += this.yDiffTex * 2;
        } else if (this.isHoveredOrFocused()) {
            i += this.yDiffTex;
        }

        Lightroom.Process currentProcess = processGetter.get();
        int xTex = currentProcess == Lightroom.Process.CHROMATIC ? 18 : 0;

        RenderSystem.enableDepthTest();
        blit(poseStack, this.x, this.y, this.xTexStart + xTex, (float)i, this.width, this.height, 256, 256);
        if (this.isHovered) {
            this.renderToolTip(poseStack, mouseX, mouseY);
        }
    }
}
