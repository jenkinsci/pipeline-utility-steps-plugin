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

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import net.sf.json.JSONObject;

/**
 * Reads a JSON file from the workspace.
 *
 * @author Nikolas Falco
 */
public class ReadJSONStep extends AbstractStepImpl {

    private String file;
    private String text;

    @DataBoundConstructor
    public ReadJSONStep() {
    }

    /**
     * The path to a file in the workspace to read JSON content from.
     *
     * @return the file path
     */
    public String getFile() {
        return file;
    }

    /**
     * The path to a file in the workspace to read JSON content from.
     *
     * @param file the path to file in the workspace
     */
    @DataBoundSetter
    public void setFile(String file) {
        this.file = Util.fixEmpty(file);
    }

    /**
     * A String containing JSON formatted data.
     *
     * @return text to parse
     */
    public String getText() {
        return text;
    }

    /**
     * A String containing JSON formatted data.
     *
     * @param text to parse
     */
    @DataBoundSetter
    public void setText(String text) {
        this.text = Util.fixEmpty(text);
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ReadJSONStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "readJSON";
        }

        @Override
        public String getDisplayName() {
            return Messages.ReadJSONStep_DescriptorImpl_displayName();
        }

        @Override
        public Step newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String file = formData.getString("file");
            String text = formData.getString("text");
            if (StringUtils.isNotBlank(file) && StringUtils.isNotBlank(text)) {
                // seems that FormException is not handled correctly, it is not shown at all client side
                throw new FormException(Messages.ReadJSONStepExecution_tooManyArguments(getFunctionName()), "text");
            }

            return super.newInstance(req, formData);
        }
    }

}