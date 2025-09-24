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
import hudson.Extension;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Map;

/**
 * Reads java properties formatted files and texts into a map.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStep extends AbstractFileOrTextStep {
    private Map<Object, Object> defaults;
    private boolean interpolate;
    private String charset;

    @DataBoundConstructor
    public ReadPropertiesStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ReadPropertiesStepExecution(this, context);
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @return the defaults
     */
    public Map<Object, Object> getDefaults() {
        return defaults;
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @param defaults the defaults
     */
    @DataBoundSetter
    public void setDefaults(Map<Object, Object> defaults) {
        this.defaults = defaults;
    }

    /**
     * Flag to indicate if the properties should be interpolated or not.
     * I.E. :
     *   baseUrl = http://localhost
     *   url = ${baseUrl}/resources
     * The value of <i>url</i> should be evaluated to http://localhost/resources with the interpolation on.
     * @return the value of interpolated
     */
    public Boolean isInterpolate() {
        return interpolate;
    }

    /**
     * Set the interpolated parameter.
     * @param interpolate parameter.
     */
    @DataBoundSetter
    public void setInterpolate(Boolean interpolate) {
        this.interpolate = interpolate;
    }

    public String getCharset() {
        return charset;
    }

    /**
     * The charset encoding to use when read the properties file. Defaults to ISO 8859-1 .
     * @param charset the charset
     */
    @DataBoundSetter
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {
        public DescriptorImpl() {

        }

        @Override
        public String getFunctionName() {
            return "readProperties";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Read properties from a file in the workspace or text.";
        }
    }
}
