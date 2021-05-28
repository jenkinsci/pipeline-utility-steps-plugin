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

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Writes a {@link JSON} object to file in the current working directory.
 *
 * @author Nikolas Falco
 */
public class WriteJSONStep extends Step {

    private String file;
    private final Object json;
    private int pretty = 0;
    private boolean returnText;

    @DataBoundConstructor
    public WriteJSONStep(Object json) {
        this.json = json;
    }

    @Deprecated
    public WriteJSONStep(String file, Object json) {
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

    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Return the JSON object to save.
     *
     * <p>
     * If it is not a {@link JSON} object, {@link net.sf.json.JSONObject#fromObject(Object)} will be used in a first
     * step.
     * </p>
     *
     * @return an object
     */
    public Object getJson() {
        return json;
    }

    /**
     * Return the number of spaces used to prettify the JSON dump.
     *
     * @return a int
     */
    public int getPretty() {
        return pretty;
    }

    /**
     * Indents to use if the JSON should be pretty printed.
     * A greater than zero integer will do so.
     *
     * @param pretty the indent size
     */
    @DataBoundSetter
    void setPretty(int pretty) {
        this.pretty = pretty;
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
        if (this.json == null) {
            throw new IllegalArgumentException(Messages.WriteJSONStepExecution_missingJSON(this.getDescriptor().getFunctionName()));
        }

        if (this.returnText) {
            if (this.file != null) {
                throw new IllegalArgumentException(Messages.WriteJSONStepExecution_bothReturnTextAndFile(this.getDescriptor().getFunctionName()));
            }
            return new ReturnTextExecution(this, context);
        }

        if (isBlank(this.file)) {
            throw new IllegalArgumentException(Messages.WriteJSONStepExecution_missingReturnTextAndFile(this.getDescriptor().getFunctionName()));
        }
        return new WriteJSONStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {

        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "writeJSON";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.WriteJSONStep_DescriptorImpl_displayName();
        }

    }

    void execute(Writer writer) throws java.io.IOException {
        JSON jsonObject;
        if (this.json instanceof JSON) {
            jsonObject = (JSON) this.json;
        } else {
            jsonObject = JSONSerializer.toJSON(this.json);
        }

        if (this.pretty > 0) {
            writer.write(jsonObject.toString(pretty));
        } else {
            jsonObject.write(writer);
        }
    }

    private static class ReturnTextExecution extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        private transient WriteJSONStep step;

        protected ReturnTextExecution(WriteJSONStep step, @Nonnull StepContext context) {
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
