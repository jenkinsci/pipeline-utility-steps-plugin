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

import hudson.Extension;
import hudson.FilePath;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Reads java properties formatted files and texts into a map.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStep extends AbstractPropertiesStep {
    private String file;
    private String text;
    private Map<String, Object> defaults;

    @DataBoundConstructor
    public ReadPropertiesStep() {
    }

    /**
     * The path to a file in the workspace to read the properties from.
     *
     * @return te path
     */
    @Override
    public String getFile() {
        return file;
    }

    /**
     * The path to a file in the workspace to read the properties from.
     *
     * @param file the path
     */
    @DataBoundSetter
    @Override
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * A String containing properties formatted data.
     *
     * @return text to parse
     */
    @Override
    public String getText() {
        return text;
    }

    /**
     * A String containing properties formatted data.
     *
     * @param text text to parse
     */
    @DataBoundSetter
    @Override
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @return the defaults
     */
    @Override
    public Map<String, Object> getDefaults() {
        return defaults;
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @param defaults the defaults
     */
    @DataBoundSetter
    @Override
    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }

    @Override
    public ReadPropertiesStepExecution start(StepContext context) throws Exception {
        return new ReadPropertiesStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "readProperties";
        }

        @Override
        public String getDisplayName() {
            return "Read properties from files in the workspace or text.";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            return ConfReaderUtils.propertiesReaderFromMap(new ReadPropertiesStep(), arguments);
        }
    }
}
