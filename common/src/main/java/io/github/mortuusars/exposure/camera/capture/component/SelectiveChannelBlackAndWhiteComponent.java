package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.util.ColorChannel;
import net.minecraft.util.FastColor;

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
    public int modifyPixel(Capture capture, int color) {
        if (channel == ColorChannel.RED) {
            int red = FastColor.ABGR32.red(color);
            return FastColor.ABGR32.color(255, red, red, red);
        }
        if (channel == ColorChannel.GREEN) {
            int green = FastColor.ABGR32.green(color);
            return FastColor.ABGR32.color(255, green, green, green);
        } else {
            int blue = FastColor.ABGR32.blue(color);
            return FastColor.ABGR32.color(255, blue, blue, blue);
        }
    }
}
