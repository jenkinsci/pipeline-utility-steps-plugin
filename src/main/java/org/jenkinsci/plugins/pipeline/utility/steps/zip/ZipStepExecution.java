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

package org.jenkinsci.plugins.pipeline.utility.steps.zip;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.io.Archiver;
import hudson.util.io.ArchiverFactory;
import jenkins.MasterToSlaveFileCallable;
import jenkins.util.BuildListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.workflow.steps.MissingContextVariableException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution of {@link ZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient ZipStep step;

    protected ZipStepExecution(@Nonnull ZipStep step, @Nonnull StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;
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
        FilePath destination = ws.child(step.getZipFile());
        if (destination.exists() && !step.isOverwrite()) {
            throw new IOException(destination.getRemote() + " exists.");
        }
        if (StringUtils.isBlank(step.getGlob())) {
            listener.getLogger().println("Writing zip file of " + source.getRemote() + " to " + destination.getRemote());
        } else {
            listener.getLogger().println("Writing zip file of " + source.getRemote()
                    + " filtered by [" + step.getGlob() + "] to " + destination.getRemote());
        }
        int count = source.act(new ZipItFileCallable(destination, step.getGlob(), step.isOverwrite()));
        listener.getLogger().println("Zipped " + count + " entries.");
        if (step.isArchive()) {
            Run build = getContext().get(Run.class);
            if (build == null) {
                throw new MissingContextVariableException(Run.class);
            }
            Launcher launcher = getContext().get(Launcher.class);
            if (launcher == null) {
                throw new MissingContextVariableException(Launcher.class);
            }
            listener.getLogger().println("Archiving " + destination.getRemote());
            Map<String, String> files = new HashMap<String, String>();
            String s = step.getZipFile().replace('\\', '/');
            files.put(s, s);
            build.pickArtifactManager().archive(ws, launcher, new BuildListenerAdapter(listener), files);
        }

        return null;
    }

    /**
     * Performs the actual zip operation on the slave where the source dir is located.
     *
     * This is a more direct implementation because {@link FilePath#zip(FilePath)}
     * will include the source dir as a base path in the zip file while this implementation doesn't.
     */
    static class ZipItFileCallable extends MasterToSlaveFileCallable<Integer> {
        final FilePath zipFile;
        final String glob;
        final boolean overwrite;

        public ZipItFileCallable(FilePath zipFile, String glob, boolean overwrite) {
            this.zipFile = zipFile;
            this.glob = StringUtils.isBlank(glob) ? "**/*" : glob;
            this.overwrite = overwrite;
        }

        @Override
        public Integer invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
            String canonicalZip = zipFile.getRemote();

            Path p = Paths.get(canonicalZip);
            if (overwrite && Files.exists(p)) {
                Files.delete(p); //Will throw exception if it fails to delete it
            }

            Archiver archiver = ArchiverFactory.ZIP.create(zipFile.write());
            FileSet fs = Util.createFileSet(dir, glob);
            DirectoryScanner scanner = fs.getDirectoryScanner(new org.apache.tools.ant.Project());
            try {
                for (String path : scanner.getIncludedFiles()) {
                    File toArchive = new File(dir, path).getCanonicalFile();
                    if (!Files.isSameFile(toArchive.toPath(), p)) {
                        archiver.visit(toArchive, path);
                    }
                }
            } finally {
                archiver.close();
            }
            return archiver.countEntries();
        }
    }
}
