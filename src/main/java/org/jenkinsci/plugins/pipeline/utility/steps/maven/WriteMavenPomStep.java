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

package org.jenkinsci.plugins.pipeline.utility.steps.maven;

import hudson.Extension;
import hudson.FilePath;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Writes a maven pom file to the current working directory.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class WriteMavenPomStep extends Step {

    private String file;
    private final Model model;

    @DataBoundConstructor
    public WriteMavenPomStep(Model model) {
        this.model = model;
    }

    /**
     * Optional name of the maven file to write.
     * If empty 'pom.xml' in the current working directory will be used.
     *
     * @return file name
     */
    public String getFile() {
        return file;
    }

    /**
     * Optional name of the maven file to write.
     * If empty 'pom.xml' in the current working directory will be used.
     *
     * @param file file name
     */
    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    public Model getModel() {
        return model;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {

        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "writeMavenPom";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Write a maven project file.";
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {

        private transient WriteMavenPomStep step;

        protected Execution(@Nonnull WriteMavenPomStep step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            assert ws != null;
            FilePath path;
            if (!StringUtils.isBlank(step.getFile())) {
                path = ws.child(step.getFile());
            } else {
                path = ws.child("pom.xml");
            }
            if (path.isDirectory()) {
                throw new FileNotFoundException(path.getRemote() + " is a directory.");
            }
            try (OutputStream os = path.write()) {
                new MavenXpp3Writer().write(os, step.getModel());
            }
            return null;
        }
    }
}
