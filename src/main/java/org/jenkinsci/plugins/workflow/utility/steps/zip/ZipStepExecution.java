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

package org.jenkinsci.plugins.workflow.utility.steps.zip;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution of {@link ZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient Launcher launcher;

    @Inject
    private transient ZipStep step;

    @Override
    protected Void run() throws Exception {
        FilePath source = ws;
        if (!StringUtils.isBlank(step.getDir())) {
            source = ws.child(step.getDir());
            if (!source.exists()) {
                throw new IOException(source.getRemote() + " does not exist.");
            } else if (!source.isDirectory()) {
                throw new IOException(source.getRemote() + " is not a directory.");
            }
        }
        FilePath destination = ws.child(step.getZipFile());
        if (destination.exists()) {
            throw new IOException(destination.getRemote() + " exists.");
        }
        if (StringUtils.isBlank(step.getGlob())) {
            listener.getLogger().println("Writing zip file of " + source.getRemote() + " to " + destination.getRemote());
            source.zip(destination);
        } else {
            listener.getLogger().println("Writing zip file of " + source.getRemote()
                    + " filtered by [" + step.getGlob() + "] to " + destination.getRemote());
            source.zip(destination.write(), step.getGlob());
        }
        if (step.isArchive()) {
            listener.getLogger().println("Archiving " + destination.getRemote());
            Map<String, String> files = new HashMap<String, String>();
            String s = step.getZipFile().replace('\\', '/');
            files.put(s, s);
            build.pickArtifactManager().archive(ws, launcher, new BuildListenerAdapter(listener), files);
        }

        return null;
    }
}
