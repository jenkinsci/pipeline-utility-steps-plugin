package org.jenkinsci.plugins.pipeline.utility.steps;

import org.apache.commons.io.FilenameUtils;

public class FilenameTestsUtils {

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
        String pathConverted=FilenameUtils.separatorsToSystem(path);
        return pathConverted.replace("\\", "\\\\");
    }
}
