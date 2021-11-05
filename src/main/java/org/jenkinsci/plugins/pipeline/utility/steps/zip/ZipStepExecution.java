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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;
import hudson.util.io.Archiver;
import hudson.util.io.ArchiverFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileCallable;
import org.jenkinsci.plugins.pipeline.utility.steps.CompressStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Execution of {@link ZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class ZipStepExecution extends CompressStepExecution {
    private static final long serialVersionUID = 3738228158324163075L;

    private transient ZipStep step;

    protected ZipStepExecution(@NonNull ZipStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        setCallable(new ZipItFileCallable(step.getGlob(), step.getExclude(), step.isOverwrite()));
        return super.run();
    }

    /**
     * Performs the actual zip operation on the slave where the source dir is located.
     *
     * This is a more direct implementation because {@link FilePath#zip(FilePath)}
     * will include the source dir as a base path in the zip file while this implementation doesn't.
     */
    static class ZipItFileCallable extends AbstractFileCallable<Integer> {
        final String glob;
        final String exclude;
        final boolean overwrite;

        public ZipItFileCallable(String glob, String exclude, boolean overwrite) {
            this.glob = StringUtils.isBlank(glob) ? "**/*" : glob;
            this.exclude = exclude;
            this.overwrite = overwrite;
        }

        @Override
        public Integer invoke(File dir, VirtualChannel channel) throws IOException, InterruptedException {
            Path p = Paths.get(getDestination().getRemote());
            if (overwrite && Files.exists(p)) {
                Files.delete(p); //Will throw exception if it fails to delete it
            }

            Archiver archiver = ArchiverFactory.ZIP.create(getDestination().write());
            FileSet fs = Util.createFileSet(dir, glob, exclude);
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
