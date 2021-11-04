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
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;

/**
 * The execution of a {@link UnTarStep}.
 *
 * @author Alexander Falkenstern &lt;Alexander.Falkenstern@gmail.com&gt;.
 */
public class UnTarStepExecution extends SynchronousNonBlockingStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    private transient UnTarStep step;

    protected UnTarStepExecution(@NonNull UnTarStep step, @NonNull StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Object run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;
        FilePath ws = getContext().get(FilePath.class);
        assert ws != null;
        if (step.isTest()) {
            return test();
        }
        FilePath source = ws.child(step.getFile());
        if (!source.exists()) {
            throw new IOException(source.getRemote() + " does not exist.");
        } else if (source.isDirectory()) {
            throw new IOException(source.getRemote() + " is a directory.");
        }
        FilePath destination = ws;
        if (!StringUtils.isBlank(step.getDir())) {
            destination = ws.child(step.getDir());
        }

        return source.act(new UnTarFileCallable(listener, destination, step.getGlob(), step.isQuiet()));
    }

    private Boolean test() throws IOException, InterruptedException {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;
        FilePath ws = getContext().get(FilePath.class);
        assert ws != null;
        FilePath source = ws.child(step.getFile());
        if (!source.exists()) {
            listener.error(source.getRemote() + " does not exist.");
            return false;
        } else if (source.isDirectory()) {
            listener.error(source.getRemote() + " is a directory.");
            return false;
        }
        return source.act(new TestTarFileCallable(listener));
    }

    /**
     * Performs the untar on the slave where the tar file is located.
     */
    public static class UnTarFileCallable extends MasterToSlaveFileCallable<Void> {
        private final TaskListener listener;
        private final FilePath destination;
        private final String glob;
        private final boolean quiet;

        public UnTarFileCallable(TaskListener listener, FilePath destination, String glob, boolean quiet) {
            this.listener = listener;
            this.destination = destination;
            this.glob = glob;
            this.quiet = quiet;
        }

        @Override
        public Void invoke(File tarFile, VirtualChannel channel) throws IOException, InterruptedException {
            PrintStream logger = listener.getLogger();
            boolean doGlob = !StringUtils.isBlank(glob);

            InputStream fileStream = new FileInputStream(tarFile);

            try {
                //check if matches standard gzip magic number
                fileStream = new GzipCompressorInputStream(fileStream);
            } catch (IOException exception) {
                // Eat exception, may be not compressed file
            }

            destination.mkdirs();
            try (TarArchiveInputStream tarStream = new TarArchiveInputStream(fileStream)) {
                logger.println("Extracting from " + tarFile.getAbsolutePath());
                TarArchiveEntry entry;
                Integer fileCount = 0;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    if (doGlob && !matches(entry.getName(), glob)) {
                        continue;
                    }

                    FilePath f = destination.child(entry.getName());
                    if (entry.isDirectory()) {
                        f.mkdirs();
                    } else {
                        fileCount++;
                        if (!quiet) {
                            logger.printf("Extracting: %s -> %s%n", entry.getName(), f.getRemote());
                        }

                        if (entry.isCheckSumOK()) {
                            OutputStream outputStream = f.write();
                            IOUtils.copy(tarStream, outputStream);
                            outputStream.close();
                        } else {
                            throw new IOException("Not a tar archive");
                        }
                    }
                }
                logger.printf("Extracted: %d files%n", fileCount);
            }
            finally {
                logger.flush();
            }
            return null;
        }

        boolean matches(String path, String glob) {
            String safeGlob = glob.replace('/', File.separatorChar);
            String safePath = path.replace('/', File.separatorChar);
            return SelectorUtils.matchPath(safeGlob, safePath);
        }
    }

    /**
     * Performs a test of a tar file on the slave where the file is.
     */
    static class TestTarFileCallable extends MasterToSlaveFileCallable<Boolean> {
        private TaskListener listener;

        public TestTarFileCallable(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            PrintStream logger = listener.getLogger();

            FileInputStream fileStream = new FileInputStream(f);
            FileChannel fileChannel = fileStream.getChannel();
            logger.printf("Checking %d bytes in %s%n", f.length(), f.getAbsolutePath());

            byte[] signature = new byte[2];
            try {
                fileStream.read(signature);
                fileChannel.position(0);
            } catch (IOException exception) {
                fileStream.close();
                listener.error("Error validating tar/tgz file: " + exception.getMessage());
                return false;
            } finally {
                logger.flush();
            }

            InputStream inputStream = fileStream;
            if(GzipCompressorInputStream.matches(signature, signature.length)) {
                try {
                    inputStream = new GzipCompressorInputStream(inputStream);
                    int nRead = -1;
                    do {
                        byte[] buffer = new byte[4096];
                        nRead = inputStream.read(buffer);
                    } while (nRead >= 0);
                    fileChannel.position(0);
                } catch (IOException exception) {
                    inputStream.close();
                    listener.error("Error validating tgz file: " + exception.getMessage());
                    return false;
                } finally {
                    logger.flush();
                }
            }

            TarArchiveInputStream tarStream = new TarArchiveInputStream(inputStream);
            try {
                TarArchiveEntry entry;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    if (!entry.isCheckSumOK()) {
                        throw new IOException("Not a tar archive");
                    }
                }
            } catch (IOException exception) {
                listener.error("Error validating tar file: " + exception.getMessage());
                return false;
            } finally {
                tarStream.close();
                logger.flush();
            }
            return true;
        }
    }
}
