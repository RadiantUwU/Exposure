package io.github.mortuusars.exposure.gui.screen;

import io.github.mortuusars.exposure.block.entity.Lightroom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Supplier;

public class ChromaticProcessToggleButton extends ImageButton {
    private final Supplier<Lightroom.Process> processGetter;

    public ChromaticProcessToggleButton(int x, int y, OnPress onPress, Supplier<Lightroom.Process> processGetter) {
        super(x, y, 18, 18, 198, 17, 18,
                LightroomScreen.MAIN_TEXTURE, 256, 256, onPress);
        this.processGetter = processGetter;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Lightroom.Process currentProcess = processGetter.get();
        int xTex = currentProcess == Lightroom.Process.CHROMATIC ? 18 : 0;

        this.renderTexture(guiGraphics, this.resourceLocation, this.getX(), this.getY(), this.xTexStart + xTex, this.yTexStart,
                this.yDiffTex, this.width, this.height, this.textureWidth, this.textureHeight);

        MutableComponent tooltip = Component.translatable("gui.exposure.lightroom.process." + currentProcess.getSerializedName());
        if (currentProcess == Lightroom.Process.CHROMATIC) {
            tooltip.append(CommonComponents.NEW_LINE);
            tooltip.append(Component.translatable("gui.exposure.lightroom.process.chromatic.info")
                    .withStyle(ChatFormatting.GRAY));
        }

        setTooltip(Tooltip.create(tooltip));
    }
}
