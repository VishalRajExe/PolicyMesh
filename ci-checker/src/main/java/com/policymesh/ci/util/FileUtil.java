package com.policymesh.ci.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for file operations.
 */
public final class FileUtil {

    private FileUtil() {
        // utility class
    }

    /**
     * Reads a file to string using UTF-8 encoding.
     */
    public static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * Checks if a file exists and is readable.
     */
    public static boolean isReadable(Path path) {
        return path != null && Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
    }

    /**
     * Checks if a directory exists.
     */
    public static boolean isDirectory(Path path) {
        return path != null && Files.isDirectory(path);
    }
}
