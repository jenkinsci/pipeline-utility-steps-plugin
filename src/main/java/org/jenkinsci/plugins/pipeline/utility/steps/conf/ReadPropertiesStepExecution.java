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

import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationConverter;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Execution of {@link ReadPropertiesStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStepExecution extends AbstractFileOrTextStepExecution<Map<String, Object>> {

    private static final long serialVersionUID = 1L;

    @StepContextParameter
    private transient TaskListener listener;

    @Inject
    private transient ReadPropertiesStep step;

    @Override
    protected Map<String, Object> doRun() throws Exception {
        PrintStream logger = listener.getLogger();
        Properties properties = new Properties();

        if (!StringUtils.isBlank(step.getFile())) {
            FilePath f = ws.child(step.getFile());
            if (f.exists() && !f.isDirectory()) {
                try(InputStream is = f.read()){
                   properties.load(is);
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

        // Interpolated values in the properties
        properties = interpolateProperties(properties);

        Map<String, Object> result = new HashMap<>();
        addAll(step.getDefaults(), result);
        addAll(properties, result);
        return result;
    }

    /**
     * addAll implementation that will coerce keys into Strings.
     *
     * @param src
     * @param dst
     */
    private void addAll(Map src, Map<String, Object> dst) {
        if (src == null) {
            return;
        }

        for (Map.Entry e : (Set<Map.Entry>) src.entrySet()) {
            dst.put(e.getKey() != null ? e.getKey().toString(): null, e.getValue());
        }
    }

    /**
     * Using commons collection to interpolated the values inside the properties
     * @param properties the list of properties to be interpolated
     * @return a new Properties object with the interpolated values
     */
    private Properties interpolateProperties(Properties properties) {
        if ( properties == null)
            return null;
        // Convert the Properties to a Configuration object in order to apply the interpolation
        Configuration conf = ConfigurationConverter.getConfiguration(properties);

        // Apply interpolation
        Configuration interpolatedProp = ((AbstractConfiguration)conf).interpolatedConfiguration();

        // Convert back to properties
        return ConfigurationConverter.getProperties(interpolatedProp);
    }
}
