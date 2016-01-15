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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.inject.Inject;

/**
 * Touch a file.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class TouchStep extends AbstractStepImpl {
    private final String file;
    private Long timestamp;

    @DataBoundConstructor
    public TouchStep(String file) throws Descriptor.FormException {
        if (StringUtils.isBlank(file)) {
            throw new Descriptor.FormException("can's be blank", "file");
        }
        this.file = file;
    }

    /**
     * The file to touch.
     *
     * @return the file
     */
    public String getFile() {
        return file;
    }

    /**
     * Optional timestamp to set
     *
     * @return timestamp
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Optional timestamp to set
     *
     * @param timestamp timestamp
     */
    @DataBoundSetter
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ExecutionImpl.class);
        }

        @Override
        public String getFunctionName() {
            return "touch";
        }

        @Override
        public String getDisplayName() {
            return "Create a file (if not already exist) in the workspace, and set the timestamp";
        }
    }

    /**
     * The execution of {@link TouchStep}.
     */
    public static class ExecutionImpl extends AbstractSynchronousNonBlockingStepExecution<FileWrapper> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient FilePath ws;

        @Inject
        private transient TouchStep step;

        @Override
        protected FileWrapper run() throws Exception {
            FilePath file = ws.child(step.getFile());
            long timestamp = step.getTimestamp() != null ? step.getTimestamp() : System.currentTimeMillis();
            file.getParent().mkdirs();
            file.touch(timestamp);
            return new FileWrapper(file);
        }
    }
}
