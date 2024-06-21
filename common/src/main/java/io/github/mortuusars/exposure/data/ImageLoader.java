package io.github.mortuusars.exposure.data;

import com.google.common.io.Files;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ImageLoader {
//    public static void load(File file, @NotNull String exposureId, int size, boolean dither, FilmType type, Runnable onLoaded) {
//        new Thread(() -> {
//            try {
//                Thread.sleep(300); // Delay to overwrite normal exposure. It may cause errors. Needs testing.
//            } catch (InterruptedException ignored) { }
//
//            try {
//                FileInputStream inputStream = new FileInputStream(file);
//
//                NativeImage image = NativeImage.read(NativeImage.Format.RGB, inputStream);
//
////                BufferedImage image = ImageIO.read(file);
//
//                if (image.getWidth() > 9999 || image.getHeight() > 9999) {
//                    Exposure.LOGGER.error("Cannot load exposure '{}': Image size is too large {}x{}",
//                            exposureId, image.getWidth(), image.getHeight());
//                    return;
//                }
//
////                NativeImage image = new NativeImage(read.getWidth(), read.getHeight(), false);
//
////                for (int x = 0; x < read.getWidth(); x++) {
////                    for (int y = 0; y < read.getHeight(); y++) {
////                        image.setPixelRGBA(x, y, ColorUtils.BGRtoRGB(read.getRGB(x, y)));
////                    }
////                }
//
////                Capture capture = new Capture()
////                        .setSize(size)
////                        .cropFactor(1f)
////                        .addComponents(new ExposureStorageSaveComponent(exposureId, true))
////                        .setConverter(dither ? new DitheringColorConverter() : new SimpleColorConverter());
////
////                if (type == FilmType.BLACK_AND_WHITE)
////                    capture.addComponents(new BlackAndWhiteComponent());
////
////                capture.processImage(image);
//
////                CompoundTag frameData = new CompoundTag();
////                frameData.putString(FrameData.ID, exposureId);
////
////                CapturedFramesHistory.add(frameData);
//
//                onLoaded.run();
//
//                Exposure.LOGGER.info("Loaded exposure from file '{}' with Id: '{}'.", file.getPath(), exposureId);
//            } catch (IOException e) {
//                Exposure.LOGGER.error("Cannot load exposure: {}", e.toString());
//            }
//        }).start();
//    }
//
//    public static Optional<Error> validateFilepath(@NotNull String filepath) {
//        if (StringUtil.isNullOrEmpty(filepath)) {
//            return Optional.of(Error.PATH_EMPTY);
//        }
//
//        File file = new File(filepath);
//        if (file.isDirectory()) {
//            return Optional.of(Error.PATH_IS_DIRECTORY);
//        }
//
//        String extension = Files.getFileExtension(filepath);
//        if (StringUtil.isNullOrEmpty(extension)) {
//            return Optional.of(Error.NO_EXTENSION);
//        }
//
//        List<String> formats = getSupportedFormats();
//        String ext = extension.replace(".", "");
//        if (!formats.contains(ext)) {
//            return Optional.of(Error.NOT_SUPPORTED);
//        }
//
//        if (!file.exists()) {
//            return Optional.of(Error.FILE_DOES_NOT_EXIST);
//        }
//
//        return Optional.empty();
//    }
//
//    /**
//     * If provided filepath is missing an extension - searches for first file
//     * in parent directory that matches the name of given file.
//     * @return File with extension or error.
//     */
//    public static Either<File, Error> getFileWithExtension(String filepath) {
//        File file = new File(filepath);
//
//        String extension = Files.getFileExtension(filepath);
//        if (!StringUtil.isNullOrEmpty(extension)) {
//            return Either.left(file);
//        }
//
//        @Nullable File parentFile = file.getParentFile();
//        if (parentFile == null) {
//            return Either.right(Error.PATH_INVALID);
//        }
//
//        File[] files = parentFile.listFiles();
//        if (files == null) {
//            return Either.right(Error.CANNOT_READ);
//        }
//
//        String name = file.getName();
//        for (File fileInDirectory : files) {
//            if (fileInDirectory.isDirectory()) {
//                continue;
//            }
//
//            String fileName = Files.getNameWithoutExtension(fileInDirectory.getName());
//            if (fileName.equals(name)) {
//                return Either.left(fileInDirectory);
//            }
//        }
//
//        return Either.right(Error.FILE_DOES_NOT_EXIST);
//    }
//
//    /**
//     * @return List of formats that can be loaded. (png, jpg, etc).
//     * Without a dot.
//     */
//    public static List<String> getSupportedFormats() {
//        List<String> formats = new ArrayList<>();
//        IIORegistry registry = IIORegistry.getDefaultInstance();
//        Iterator<ImageWriterSpi> serviceProviders = registry.getServiceProviders(ImageWriterSpi.class, false);
//        while(serviceProviders.hasNext()) {
//            Collections.addAll(formats, serviceProviders.next().getFormatNames());
//        }
//        return formats;
//    }
//
//    public enum Error {
//        PATH_EMPTY("path_empty"),
//        PATH_INVALID("path_invalid"),
//        NO_EXTENSION("no_extension"),
//        PATH_IS_DIRECTORY("path_is_directory"),
//        FILE_DOES_NOT_EXIST("file_does_not_exist"),
//        CANNOT_READ("cannot_read"),
//        NOT_SUPPORTED("not_supported");
//
//        private final String key;
//
//        Error(String key) {
//            this.key = key;
//        }
//
//        public String getKey() {
//            return key;
//        }
//
//        public MutableComponent getTranslation() {
//            return Component.translatable("exposure.image_loader.fail." + getKey());
//        }
//    }
}
