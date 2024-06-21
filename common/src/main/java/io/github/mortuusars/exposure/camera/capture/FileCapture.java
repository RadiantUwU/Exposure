package io.github.mortuusars.exposure.camera.capture;

import com.google.common.io.Files;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.ImageLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class FileCapture extends Capture {
    protected final String filepath;
    protected final Consumer<Error> onError;

    public FileCapture(String filepath) {
        this(filepath, err -> {});
    }

    public FileCapture(String filepath, Consumer<Error> onError) {
        this.filepath = filepath;
        this.onError = onError;
    }

    public String getFilepath() {
        return filepath;
    }

    @Override
    public @Nullable NativeImage captureImage() {
        return captureImage(getFilepath());
    }

    public @Nullable NativeImage captureImage(String filepath) {
        Either<File, Error> fileOrError = findFileWithExtension(filepath);

        File file;

        if (fileOrError.right().isPresent()) {
            onError.accept(fileOrError.right().get());
            return null;
        }
        else {
            file = fileOrError.left().orElseThrow();
        }

        Optional<Error> error = validateFilepath(file.getPath());

        if (error.isPresent()) {
            onError.accept(error.get());
            return null;
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            return NativeImage.read(NativeImage.Format.RGBA, inputStream);
        } catch (IOException e) {
            Exposure.LOGGER.error("Loading image failed: {}", e.toString());
            onError.accept(Error.CANNOT_READ);
        }

        return null;
    }

    public static Optional<Error> validateFilepath(@NotNull String filepath) {
        if (StringUtil.isNullOrEmpty(filepath)) {
            return Optional.of(Error.PATH_EMPTY);
        }

        File file = new File(filepath);
        if (file.isDirectory()) {
            return Optional.of(Error.PATH_IS_DIRECTORY);
        }

        String extension = Files.getFileExtension(filepath);
        if (StringUtil.isNullOrEmpty(extension)) {
            return Optional.of(Error.NO_EXTENSION);
        }

//        List<String> formats = getSupportedFormats();
//        String ext = extension.replace(".", "");
//        if (!formats.contains(ext)) {
//            return Optional.of(ImageLoader.Error.NOT_SUPPORTED);
//        }

        if (!file.exists()) {
            return Optional.of(Error.FILE_DOES_NOT_EXIST);
        }

        return Optional.empty();
    }

    /**
     * If provided filepath is missing an extension - searches for first file
     * in parent directory that matches the name of given file.
     * @return File with extension or error.
     */
    public static Either<File, Error> findFileWithExtension(String filepath) {
        File file = new File(filepath);

        String extension = Files.getFileExtension(filepath);
        if (!StringUtil.isNullOrEmpty(extension)) {
            return Either.left(file);
        }

        @Nullable File parentFile = file.getParentFile();
        if (parentFile == null) {
            return Either.right(Error.PATH_INVALID);
        }

        File[] files = parentFile.listFiles();
        if (files == null) {
            return Either.right(Error.CANNOT_READ);
        }

        String name = file.getName();
        for (File fileInDirectory : files) {
            if (fileInDirectory.isDirectory()) {
                continue;
            }

            String fileName = Files.getNameWithoutExtension(fileInDirectory.getName());
            if (fileName.equals(name)) {
                return Either.left(fileInDirectory);
            }
        }

        return Either.right(Error.FILE_DOES_NOT_EXIST);
    }

    public enum Error {
        PATH_EMPTY("path_empty"),
        PATH_INVALID("path_invalid"),
        NO_EXTENSION("no_extension"),
        PATH_IS_DIRECTORY("path_is_directory"),
        FILE_DOES_NOT_EXIST("file_does_not_exist"),
        CANNOT_READ("cannot_read"),
        NOT_SUPPORTED("not_supported");

        private final String key;

        Error(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public MutableComponent getCasualTranslation() {
            return Component.translatable("exposure.file_capture.error.casual." + getKey());
        }

        public MutableComponent getTechnicalTranslation() {
            return Component.translatable("exposure.file_capture.error.technical." + getKey());
        }
    }
}
