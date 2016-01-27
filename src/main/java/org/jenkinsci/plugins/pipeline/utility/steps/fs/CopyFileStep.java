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
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import jenkins.MasterToSlaveFileCallable;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Copy a file in the workspace.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class CopyFileStep extends AbstractStepImpl {
    private final String from;
    private final String to;
    private boolean overwrite;

    @DataBoundConstructor
    public CopyFileStep(String from, String to) throws Descriptor.FormException {
        if (isBlank(from)) {
            throw new Descriptor.FormException("Can't be blank", "from");
        }
        if (isBlank(to)) {
            throw new Descriptor.FormException("Can't be blank", "to");
        }
        this.from = from;
        this.to = to;
        overwrite = false;
    }

    /**
     * The file or directory to copy from.
     * @return the path
     */
    public String getFrom() {
        return from;
    }

    /**
     * the file or directory to copy to.
     *
     * @return the path.
     */
    public String getTo() {
        return to;
    }

    /**
     * Wether the destination should be overwritten/merged if it exists.
     *
     * @return true if so.
     */
    public boolean isOverwrite() {
        return overwrite;
    }

    @DataBoundSetter
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(ExecutionImpl.class);
        }

        @Override
        public String getFunctionName() {
            return "cp";
        }

        @Override
        public String getDisplayName() {
            return "Copy a file.";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckFrom(@QueryParameter String value) {
            if (isBlank(value)) {
                return FormValidation.error("Needs a value");
            } else {
                return FormValidation.ok();
            }
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckTo(@QueryParameter String value) {
            if (isBlank(value)) {
                return FormValidation.error("Needs a value");
            } else {
                return FormValidation.ok();
            }
        }
    }

    /**
     * The execution of {@link CopyFileStep}.
     */
    public static class ExecutionImpl extends AbstractSynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject
        private transient CopyFileStep step;

        @Override
        protected Void run() throws Exception {
            FilePath from = ws.child(step.getFrom());
            FilePath to = ws.child(step.getTo());

            if(!from.exists()) {
                throw new FileNotFoundException(from.getRemote() + " does not exist.");
            } else {
                return from.act(new CopyFileStepFileCallable(to, step.isOverwrite(), listener));
            }
        }

        /**
         * {@link hudson.FilePath.FileCallable} to make sure that most file operations are performed on the slave,
         * instead of ping pong back and forth for every simple file op.
         */
        static class CopyFileStepFileCallable extends MasterToSlaveFileCallable<Void> {
            private static final long serialVersionUID = 1L;

            private final FilePath to;
            private final boolean overwrite;
            private final TaskListener listener;

            public CopyFileStepFileCallable(FilePath to, boolean overwrite, TaskListener listener) {
                this.to = to;
                this.overwrite = overwrite;
                this.listener = listener;
            }

            @Override
            public Void invoke(File fromFile, VirtualChannel channel) throws IOException, InterruptedException {
                FilePath from = new FilePath(fromFile);

                if (!overwrite && to.exists()) {
                    throw new IOException(to.getRemote() + " exists.");
                }

                if (from.isDirectory()) {
                    if (to.exists() && !to.isDirectory()) {
                        throw new IOException(to.getRemote() + " is not a directory.");
                    }
                    int count = from.copyRecursiveTo(to);
                    listener.getLogger().println(count + " files copied to " + to.getRemote());
                } else {
                    if (to.exists() && to.isDirectory()) {
                        throw new IOException(to.getRemote() + " is a directory.");
                    }
                    from.copyTo(to);
                    to.chmod(from.mode());
                    listener.getLogger().println("Copied " + from.getRemote() + " to " + to.getRemote());
                }

                return null;
            }
        }
    }
}
