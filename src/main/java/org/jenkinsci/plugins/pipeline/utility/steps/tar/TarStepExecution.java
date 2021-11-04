/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Alexander Falkenstern
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

package org.jenkinsci.plugins.pipeline.utility.steps.tar;

import edu.umd.cs.findbugs.annotations.NonNull;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Execution of {@link TarStep}.
 *
 * @author Alexander Falkenstern &lt;Alexander.Falkenstern@gmail.com&gt;.
 */
public class TarStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient TarStep step;

    protected TarStepExecution(@NonNull TarStep step, @NonNull StepContext context) {
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
        FilePath destination = ws.child(step.getFile());
        if (destination.exists() && !step.isOverwrite()) {
            throw new IOException(destination.getRemote() + " exists.");
        }
        if (StringUtils.isBlank(step.getGlob()) && StringUtils.isBlank(step.getExclude())) {
            listener.getLogger().println("Writing tar file of " + source.getRemote() + " to " + destination.getRemote());
        } else {
            listener.getLogger().println("Writing tar file of " + source.getRemote()
                    + " filtered by [" + step.getGlob() + "] - [" + step.getExclude() + "] to " + destination.getRemote());
        }
        int count = source.act(new TarItFileCallable(destination, step.getGlob(), step.getExclude(), step.isCompress(), step.isOverwrite()));
        listener.getLogger().println("Tared " + count + " entries.");
        if (step.isArchive()) {
            Run<?, ?> build = getContext().get(Run.class);
            if (build == null) {
                throw new MissingContextVariableException(Run.class);
            }
            Launcher launcher = getContext().get(Launcher.class);
            if (launcher == null) {
                throw new MissingContextVariableException(Launcher.class);
            }
            listener.getLogger().println("Archiving " + destination.getRemote());
            Map<String, String> files = new HashMap<>();
            String s = step.getFile().replace('\\', '/');
            files.put(s, s);
            build.pickArtifactManager().archive(ws, launcher, new BuildListenerAdapter(listener), files);
        }

        return null;
    }

    /**
     * Performs the actual tar operation on the slave where the source dir is located.
     */
    static class TarItFileCallable extends MasterToSlaveFileCallable<Integer> {
        final FilePath tarFile;
        final String glob;
        final String exclude;
        final boolean compress;
        final boolean overwrite;

        public TarItFileCallable(FilePath tarFile, String glob, String exclude, boolean compress, boolean overwrite) {
            this.tarFile = tarFile;
            this.glob = StringUtils.isBlank(glob) ? "**/*" : glob;
            this.exclude = exclude;
            this.compress = compress;
            this.overwrite = overwrite;
        }

        @Override
        public Integer invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
            Path p = Paths.get(tarFile.getRemote());
            if (overwrite && Files.exists(p)) {
                Files.delete(p); //Will throw exception if it fails to delete it
            }

            Archiver archiver = (compress ? ArchiverFactory.TARGZ : ArchiverFactory.TAR).create(tarFile.write());
            FileSet fileSet = Util.createFileSet(dir, glob, exclude);
            DirectoryScanner scanner = fileSet.getDirectoryScanner(new org.apache.tools.ant.Project());
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
