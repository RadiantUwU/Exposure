package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.util.ColorChannel;

import java.awt.*;

@SuppressWarnings("ClassCanBeRecord")
public class SelectiveChannelBlackAndWhiteComponent implements ICaptureComponent {
    private final ColorChannel channel;

    public SelectiveChannelBlackAndWhiteComponent(ColorChannel channel) {
        this.channel = channel;
    }

    public ColorChannel getChannel() {
        return channel;
    }

    @Override
    public Color modifyPixel(Capture capture, int red, int green, int blue) {
        if (channel == ColorChannel.RED)
            return new Color(red, red, red);
        if (channel == ColorChannel.GREEN)
            return new Color(green, green, green);
        else
            return new Color(blue, blue, blue);
    }
}
