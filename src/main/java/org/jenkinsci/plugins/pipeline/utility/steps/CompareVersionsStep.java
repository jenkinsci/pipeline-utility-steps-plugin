/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 CloudBees Inc.
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
package org.jenkinsci.plugins.pipeline.utility.steps;

import hudson.AbortException;
import hudson.Extension;
import hudson.util.VersionNumber;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Compares two version numbers with each other.
 *
 * @see hudson.util.VersionNumber
 */
public class CompareVersionsStep extends Step {

    private final String v1;
    private final String v2;
    private boolean failIfEmpty = false;

    @DataBoundConstructor
    public CompareVersionsStep(final String v1, final String v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    public String getV1() {
        return v1;
    }

    public String getV2() {
        return v2;
    }

    public boolean isFailIfEmpty() {
        return failIfEmpty;
    }

    @DataBoundSetter
    public void setFailIfEmpty(final boolean failIfEmpty) {
        this.failIfEmpty = failIfEmpty;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new ExecutionImpl(context, v1, v2, failIfEmpty);
    }

    public static class ExecutionImpl extends SynchronousStepExecution<Integer> {

        private final String v1;
        private final String v2;
        private final boolean failIfEmpty;

        protected ExecutionImpl(@Nonnull final StepContext context, final String v1, final String v2, final boolean failIfEmpty) {
            super(context);
            this.v1 = v1;
            this.v2 = v2;
            this.failIfEmpty = failIfEmpty;
        }

        @Override
        protected Integer run() throws Exception {
            if (isEmpty(v1) && isEmpty(v2)) {
                if (failIfEmpty) {
                    throw new AbortException("Both parameters are empty.");
                }
                return 0;
            }
            if (isEmpty(v1)) {
                if (failIfEmpty) {
                    throw new AbortException("v1 is empty.");
                }
                return -1;
            }
            if (isEmpty(v2)) {
                if (failIfEmpty) {
                    throw new AbortException("v2 is empty.");
                }
                return 1;
            }
            VersionNumber vn1 = new VersionNumber(v1);
            VersionNumber vn2 = new VersionNumber(v2);
            return vn1.compareTo(vn2);
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public String getFunctionName() {
            return "compareVersions";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.CompareVersionsStep_DisplayName();
        }
    }
}
