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
package org.jenkinsci.plugins.pipeline.utility.steps.template;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import java.util.Map;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Binds a map to a groovy template using the SimpleTemplateEngine.
 *
 * @author Martin d'Anjou
 */
public class SimpleTemplateEngineStep extends AbstractFileOrTextStep {

    protected Map<String, Object> bindings;
    protected boolean runInSandbox;

    @DataBoundConstructor
    public SimpleTemplateEngineStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new SimpleTemplateEngineStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends AbstractFileOrTextStepDescriptorImpl {

        public DescriptorImpl() {

        }

        @Override
        public String getFunctionName() {
            return "simpleTemplateEngine";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.SimpleTemplateEngineStep_DescriptorImpl_displayName();
        }
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    @DataBoundSetter
    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    public boolean isRunInSandbox() {
        return runInSandbox;
    }

    @DataBoundSetter
    public void setRunInSandbox(boolean runInSandbox) {
        this.runInSandbox = runInSandbox;
    }
}
