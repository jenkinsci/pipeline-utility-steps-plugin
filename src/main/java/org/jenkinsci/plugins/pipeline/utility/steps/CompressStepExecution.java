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
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution of {@link AbstractFileCompressStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public abstract class CompressStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private transient AbstractFileCallable<Integer> callable;
    private transient final AbstractFileCompressStep step;

    protected CompressStepExecution(@NonNull AbstractFileCompressStep step, @NonNull StepContext context) {
        super(context);
        this.step = step;
    }

    protected void setCallable(final AbstractFileCallable callable) {
        this.callable = callable;
    }
    
    @Override
    protected Void run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;

        PrintStream logger = listener.getLogger();
        assert logger != null;

        FilePath ws = getContext().get(FilePath.class);
        assert ws != null;
        FilePath source = ws;
        if (!StringUtils.isBlank(step.getDir())) {
            source = ws.child(step.getDir());
            if (!source.exists()) {
                throw new IOException(source.getRemote() + " does not exist.");
            } else if (!source.isDirectory()) {
                throw new IOException(source.getRemote() + " is not a directory.");
            }
        }
        FilePath destination = ws.child(step.getFile());
        if (destination.exists() && !step.isOverwrite()) {
            throw new IOException(destination.getRemote() + " exists.");
        }

        logger.print("Compress " + source.getRemote());
        if (!StringUtils.isBlank(step.getGlob()) || !StringUtils.isBlank(step.getExclude())) {
            logger.print(" filtered by [" + step.getGlob() + "] - [" + step.getExclude() + "]");
        }
        logger.println(" to " + destination.getRemote());

        callable.setDestination(destination);
        Integer count = source.act(callable);
        logger.println("Compressed " + count + " entries.");

        if (step.isArchive()) {
            Run<?, ?> build = getContext().get(Run.class);
            if (build == null) {
                throw new MissingContextVariableException(Run.class);
            }
            Launcher launcher = getContext().get(Launcher.class);
            if (launcher == null) {
                throw new MissingContextVariableException(Launcher.class);
            }
            logger.println("Archiving " + destination.getRemote());

            Map<String, String> files = new HashMap<>();
            String s = step.getFile().replace('\\', '/');
            files.put(s, s);
            build.pickArtifactManager().archive(ws, launcher, new BuildListenerAdapter(listener), files);
        }

        return null;
    }

}
