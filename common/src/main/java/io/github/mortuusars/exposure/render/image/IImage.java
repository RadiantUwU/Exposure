package io.github.mortuusars.exposure.render.image;

public interface IImage {
    String getImageId();
    int getWidth();
    int getHeight();
    int getPixelABGR(int x, int y);
    default void close() {}
}
