package io.github.mortuusars.exposure.render.image;

import com.mojang.blaze3d.platform.NativeImage;

public class Image implements IImage {
    private final String name;
    private final NativeImage image;

    public Image(String name, NativeImage image) {
        this.name = name;
        this.image = image;
    }

    @Override
    public String getImageId() {
        return name;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getPixelABGR(int x, int y) {
        return image.getPixelRGBA(x, y); // this returns ABGR
    }
}
