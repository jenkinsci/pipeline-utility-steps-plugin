package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import hudson.FilePath;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static org.apache.commons.lang.StringUtils.isBlank;

public class ConfReaderUtils {
    static Map<String,String> toStringMap(@Nonnull Map<String,Object> inMap) {
        Map<String,String> result = new TreeMap<>();
        for (Map.Entry<String,Object> e : inMap.entrySet()) {
            result.put(e.getKey(), e.getValue().toString());
        }

        return result;
    }

    static Map<String,Object> readProperties(@Nonnull String filename, Map<String,Object> defaults,
                                             @Nonnull FilePath ws, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();
        Properties properties = new Properties();

        FilePath f = ws.child(filename);
        if (f.exists() && !f.isDirectory()) {
            properties.load(f.read());
        } else if (f.isDirectory()) {
            logger.print("warning: ");
            logger.print(f.getRemote());
            logger.println(" is a directory, omitting from properties gathering");
        } else if (!f.exists()) {
            logger.print("warning: ");
            logger.print(f.getRemote());
            logger.println(" does not exist, omitting from properties gathering");
        }

        return propertiesToMap(properties, defaults);
    }

    static Map<String,Object> readProperties(@Nonnull String text, Map<String,Object> defaults) throws IOException {
        Properties properties = new Properties();
        StringReader sr = new StringReader(text);
        properties.load(sr);

        return propertiesToMap(properties, defaults);
    }

    private static Map<String,Object> propertiesToMap(@Nonnull Properties properties, Map<String,Object> defaults) {
        Map<String, Object> result = new HashMap<>();
        if (defaults != null) {
            result.putAll(defaults);
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            result.put(entry.getKey() != null ? entry.getKey().toString(): null, entry.getValue());
        }

        return result;
    }

    static AbstractPropertiesStep propertiesReaderFromMap(@Nonnull AbstractPropertiesStep step, Map<String, Object> arguments) throws Exception {
        if(arguments.containsKey("file")) {
            Object file = arguments.get("file");
            if (file != null) {
                step.setFile(file.toString());
            }
        }
        if (arguments.containsKey("text")) {
            Object text = arguments.get("text");
            if (text != null) {
                step.setText(text.toString());
            }
        }
        if (arguments.containsKey("defaults")) {
            Object defaults = arguments.get("defaults");
            if (defaults != null && defaults instanceof Map) {
                step.setDefaults((Map)defaults);
            }
        }
        if (isBlank(step.getFile()) && isBlank(step.getText())) {
            throw new IllegalArgumentException("At least one of file or text needs to be provided to " +
                    step.getDescriptor().getFunctionName() + ".");
        }
        return step;
    }
}
