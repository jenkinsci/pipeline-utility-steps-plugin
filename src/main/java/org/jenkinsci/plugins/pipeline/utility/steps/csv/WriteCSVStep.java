/*
 * The MIT License
 *
 * Copyright (C) 2018 Electronic Arts Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.pipeline.utility.steps.csv;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.CSVFormat;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import java.util.Set;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Writes a List of String Arrays to file in the current working directory.
 *
 * @author Stuart Rowe
 */
public class WriteCSVStep extends Step {

    private String file;
    private CSVFormat format;
    private Iterable<?> records;

    @DataBoundConstructor
    public WriteCSVStep(String file, Iterable<?> records) {
        this.file = Util.fixNull(file);
        this.format = CSVFormat.DEFAULT;
        this.records = records;
    }

     public String getFile() {
        return this.file;
    }

    public Iterable<?> getRecords() {
        return this.records;
    }

    public CSVFormat getFormat() {
        return this.format;
    }

    @DataBoundSetter
    public void setFormat(CSVFormat format) {
        this.format = format;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new WriteCSVStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "writeCSV";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.WriteCSVStep_DescriptorImpl_displayName();
        }
    }
}
