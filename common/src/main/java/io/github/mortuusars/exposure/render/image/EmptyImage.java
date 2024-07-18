package io.github.mortuusars.exposure.render.image;

public class EmptyImage implements IImage {
    @Override
    public String getName() {
        return "<empty>";
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }

    @Override
    public int getPixelABGR(int x, int y) {
        return 0x00000000;
    }
}
