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
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Map;

/**
 * Reads java properties formatted files and texts into a map.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ReadPropertiesStep extends AbstractFileOrTextStep {
    private Map defaults;

    @DataBoundConstructor
    public ReadPropertiesStep() {
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @return the defaults
     */
    public Map getDefaults() {
        return defaults;
    }

    /**
     * Default key/values to populate the map with before parsing.
     *
     * @param defaults the defaults
     */
    @DataBoundSetter
    public void setDefaults(Map defaults) {
        this.defaults = defaults;
    }

    @Extension
    public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {
        public DescriptorImpl() {
            super(ReadPropertiesStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "readProperties";
        }

        @Override
        public String getDisplayName() {
            return "Read properties from files in the workspace or text.";
        }
    }
}
