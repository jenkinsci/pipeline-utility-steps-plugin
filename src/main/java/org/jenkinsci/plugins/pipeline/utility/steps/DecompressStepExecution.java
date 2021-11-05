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

package org.jenkinsci.plugins.pipeline.utility.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;

/**
 * Execution of {@link AbstractFileCompressStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public abstract class DecompressStepExecution extends SynchronousNonBlockingStepExecution<Object> {
    private transient AbstractFileCallable<? extends Object> callable;
    private transient final AbstractFileDecompressStep step;

    protected DecompressStepExecution(@NonNull AbstractFileDecompressStep step, @NonNull StepContext context) {
        super(context);
        this.step = step;
    }

    protected void setCallable(final AbstractFileCallable<? extends Object> callable) {
        this.callable = callable;
    }

    @Override
    protected Object run() throws IOException, InterruptedException {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;

        FilePath workspace = getContext().get(FilePath.class);
        assert workspace != null;

        if (step.isTest()) {
            return test(listener, workspace);
        }

        FilePath source = workspace.child(step.getFile());
        if (!source.exists()) {
            throw new IOException(source.getRemote() + " does not exist.");
        } else if (source.isDirectory()) {
            throw new IOException(source.getRemote() + " is a directory.");
        }
        FilePath destination = workspace;
        if (!StringUtils.isBlank(step.getDir())) {
            destination = workspace.child(step.getDir());
        }

        callable.setDestination(destination);
        return source.act(callable);
    }

    private Object test(TaskListener listener, FilePath workspace) throws IOException, InterruptedException {
        FilePath source = workspace.child(step.getFile());
        if (!source.exists()) {
            listener.error(source.getRemote() + " does not exist.");
            return Boolean.FALSE;
        } else if (source.isDirectory()) {
            listener.error(source.getRemote() + " is a directory.");
            return Boolean.FALSE;
        }
        return source.act(callable);
    }
}
