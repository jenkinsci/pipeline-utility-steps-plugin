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
package org.jenkinsci.plugins.pipeline.utility.steps.toml;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Writes a {@link java.util.LinkedHashMap} object to file in the current working directory.
 *
 */
public class WriteTOMLStep extends Step {

    private String file;
    private final Object toml;
    private boolean returnText;

    @DataBoundConstructor
    public WriteTOMLStep(Object toml) {
        this.toml = toml;
    }

    @Deprecated
    public WriteTOMLStep(String file, Object toml) {
        this.file = Util.fixNull(file);
        this.toml = toml;
    }

    /**
     * Returns the name of the file to write.
     *
     * @return the file name
     */
    public String getFile() {
        return file;
    }

    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Return the toml object to save.
     *
     * @return an object
     */
    public Object getTOML() {
        return toml;
    }

    public boolean isReturnText() {
        return returnText;
    }

    @DataBoundSetter
    public void setReturnText(boolean returnText) {
        this.returnText = returnText;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        if (this.toml == null) {
            throw new IllegalArgumentException(Messages.WriteTOMLStepExecution_missingTOML(
                    this.getDescriptor().getFunctionName()));
        }

        if (this.returnText) {
            if (isNotBlank(this.file)) {
                throw new IllegalArgumentException(Messages.WriteTOMLStepExecution_bothReturnTextAndFile(
                        this.getDescriptor().getFunctionName()));
            }
            return new ReturnTextExecution(this, context);
        }

        if (isBlank(this.file)) {
            throw new IllegalArgumentException(Messages.WriteTOMLStepExecution_missingReturnTextAndFile(
                    this.getDescriptor().getFunctionName()));
        }

        return new WriteTOMLStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {}

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "writeTOML";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.WriteTOMLStep_DescriptorImpl_displayName();
        }
    }

    void execute(Writer writer) throws java.io.IOException {
        TomlMapper mapper = new TomlMapper();
        mapper.writeValue(writer, toml);
    }

    private static class ReturnTextExecution extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        private transient WriteTOMLStep step;

        protected ReturnTextExecution(WriteTOMLStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            StringWriter w = new StringWriter();
            this.step.execute(w);
            return w.toString();
        }
    }
}
