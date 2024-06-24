/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.utility.steps.conf;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.util.SystemProperties;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.DefaultLookups;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Execution of {@link ReadPropertiesStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStepExecution extends AbstractFileOrTextStepExecution<Map<String, Object>> {
    private static final long serialVersionUID = 1L;

    private static final Map<String, Lookup> SAFE_PREFIX_INTERPOLATOR_LOOKUPS = new HashMap<String, Lookup>() {{
        put(DefaultLookups.BASE64_DECODER.getPrefix(), DefaultLookups.BASE64_DECODER.getLookup());
        put(DefaultLookups.BASE64_ENCODER.getPrefix(), DefaultLookups.BASE64_ENCODER.getLookup());
        put(DefaultLookups.DATE.getPrefix(), DefaultLookups.DATE.getLookup());
        put(DefaultLookups.URL_DECODER.getPrefix(), DefaultLookups.URL_DECODER.getLookup());
        put(DefaultLookups.URL_ENCODER.getPrefix(), DefaultLookups.URL_ENCODER.getLookup());
    }};

    static /* not final */ String CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS = SystemProperties.getString(ReadPropertiesStepExecution.class.getName() + ".CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS");

    private transient ReadPropertiesStep step;

    protected ReadPropertiesStepExecution(@NonNull ReadPropertiesStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected Map<String, Object> doRun() throws Exception {
        PrintStream logger = getLogger();
        Properties properties = new Properties();

        if (step.getDefaults() != null) {
        	properties.putAll(step.getDefaults());
        }

        if (!StringUtils.isBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());
            if (f.exists() && !f.isDirectory()) {
                try(InputStream is = f.read()){
                    if (StringUtils.isEmpty(step.getCharset())) {
                        properties.load(is);
                    } else {
                        try (InputStreamReader isr = new InputStreamReader(is, step.getCharset()); BufferedReader br = new BufferedReader(isr)) {
                            properties.load(br);
                        }
                    }
                }
            } else if (f.isDirectory()) {
                logger.print("warning: ");
                logger.print(f.getRemote());
                logger.println(" is a directory, omitting from properties gathering");
            } else if(!f.exists()) {
                logger.print("warning: ");
                logger.print(f.getRemote());
                logger.println(" does not exist, omitting from properties gathering");
            }
        }

        if (!StringUtils.isBlank(step.getText())) {
            StringReader sr = new StringReader(step.getText());
            properties.load(sr);
        }

        // Check if we should interpolated values in the properties
        if ( step.isInterpolate() ) {
            logger.println("Interpolation set to true, starting to parse the variable!");
            properties = interpolateProperties(properties);
        }

        Map<String, Object> result = new HashMap<>();
        addAll(properties, result);
        return result;
    }

    /**
     * addAll implementation that will coerce keys into Strings.
     *
     * @param src the source
     * @param dst the destination
     */
    private void addAll(Map<Object, Object> src, Map<String, Object> dst) {
        if (src == null) {
            return;
        }

        for (Map.Entry<Object, Object> e : src.entrySet()) {
            dst.put(e.getKey() != null ? e.getKey().toString(): null, e.getValue());
        }
    }

    /**
     * Using commons collection to interpolated the values inside the properties
     * @param properties the list of properties to be interpolated
     * @return a new Properties object with the interpolated values
     */
    private Properties interpolateProperties(Properties properties) throws Exception {
        if ( properties == null)
            return null;
        PrintStream logger = getLogger();
        try {
            ConfigurationInterpolator configurationInterpolator = ConfigurationInterpolator.fromSpecification(
                    new InterpolatorSpecification.Builder()
                            .withPrefixLookups(
                                    CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS == null ?
                                            SAFE_PREFIX_INTERPOLATOR_LOOKUPS :
                                            parseLookups(CUSTOM_PREFIX_INTERPOLATOR_LOOKUPS)
                            )
                            .create()
            );
            // Convert the Properties to a Configuration object in order to apply the interpolation
            Configuration conf = ConfigurationConverter.getConfiguration(properties);
            conf.setInterpolator(configurationInterpolator);

            // Apply interpolation
            Configuration interpolatedProp = ((AbstractConfiguration)conf).interpolatedConfiguration();

            // Convert back to properties
            return ConfigurationConverter.getProperties(interpolatedProp);
        } catch (Exception e) {
            logger.println("Got exception while interpolating the variables: " + e.getMessage());
            logger.println("Returning the original properties list!");
            return properties;
        }
    }

    /**
     * Helper method to get the logger from the context.
     * @return the logger from the context.
     * @throws Exception
     */
    private PrintStream getLogger() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;
        return listener.getLogger();
    }

    /*
     * Method was copied from https://github.com/apache/commons-configuration/blob/aff776e3d4d81f1f856304306353be3279aec11a/src/main/java/org/apache/commons/configuration2/interpol/ConfigurationInterpolator.java#L673-L687
     * licensed under https://github.com/apache/commons-configuration/blob/aff776e3d4d81f1f856304306353be3279aec11a/LICENSE.txt
     * and slightly modified.
     */
    private static Map<String, Lookup> parseLookups(final String str) {
        final Map<String, Lookup> lookupMap = new HashMap<>();
        if (StringUtils.isBlank(str))
            return lookupMap;

        try {
            for (final String lookupName : str.split("[\\s,]+")) {
                if (!lookupName.isEmpty()) {
                    DefaultLookups lookup = DefaultLookups.valueOf(lookupName.toUpperCase());
                    lookupMap.put(lookup.getPrefix(), lookup.getLookup());
                }
            }
        } catch (IllegalArgumentException exc) {
            throw new IllegalArgumentException("Invalid default lookups definition: " + str, exc);
        }

        return lookupMap;
    }
}
