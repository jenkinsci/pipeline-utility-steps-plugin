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
package org.jenkinsci.plugins.pipeline.utility.steps.json;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;

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
        this.file = file;
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
        this.text = text;
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
            return "Read JSON from files in the workspace.";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            ReadJSONStep step = new ReadJSONStep();
            if (arguments.containsKey("file")) {
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
            if (StringUtils.isNotBlank(step.getFile()) && StringUtils.isNotBlank(step.getText())) {
                throw new IllegalArgumentException("At most one of file or text must be provided to " + getFunctionName() + '.');
            }
            if (StringUtils.isBlank(step.getFile()) && StringUtils.isBlank(step.getText())) {
                throw new IllegalArgumentException("At least one of file or text needs to be provided to " + getFunctionName() + '.');
            }
            return step;
        }
    }

}
