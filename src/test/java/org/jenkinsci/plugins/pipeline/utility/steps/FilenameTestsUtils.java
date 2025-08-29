package org.jenkinsci.plugins.pipeline.utility.steps;

import java.io.File;
import org.apache.commons.io.FilenameUtils;

public final class FilenameTestsUtils {

    private FilenameTestsUtils() {
        // private constructor
    }

    /**
     * Converts all separators to the system separator
     * and escape them for Windows.
     *
     * @param path  the path to be changed, null ignored
     * @return the updated path
     */
    public static String separatorsToSystemEscaped(String path) {
        if (path == null) {
            return null;
        }
        String pathConverted = FilenameUtils.separatorsToSystem(path);
        return pathConverted.replace("\\", "\\\\");
    }

    /**
     * Convert a file to a platform-agnostic representation.
     *
     * @param file
     * @return a file path operative system aware
     */
    public static String toPath(File file) {
        if (file == null) {
            return null;
        }

        return file.getAbsolutePath().replace("\\", "/");
    }
}
