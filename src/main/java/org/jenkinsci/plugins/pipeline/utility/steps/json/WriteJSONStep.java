/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Nikolas Falco
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
package org.jenkinsci.plugins.pipeline.utility.steps.json;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.Util;
import net.sf.json.JSON;

/**
 * Writes a {@link JSON} object to file in the current working directory.
 *
 * @author Nikolas Falco
 */
public class WriteJSONStep extends AbstractStepImpl {

    private final String file;
    private final JSON json;

    @DataBoundConstructor
    public WriteJSONStep(String file, JSON json) {
        this.file = Util.fixNull(file);
        this.json = json;
    }

    /**
     * Returns the name of the file to write.
     *
     * @return the file name
     */
    public String getFile() {
        return file;
    }

    /**
     * Return the JSON object to save.
     *
     * @return a {@link JSON} object
     */
    public JSON getJson() {
        return json;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(WriteJSONStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "writeJSON";
        }

        @Override
        public String getDisplayName() {
            return Messages.WriteJSONStep_DescriptorImpl_displayName();
        }

    }

}