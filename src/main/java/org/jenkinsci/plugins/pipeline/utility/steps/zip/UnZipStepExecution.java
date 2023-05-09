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
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileCallable;
import org.jenkinsci.plugins.pipeline.utility.steps.DecompressStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The execution of a {@link UnZipStep}.
 *
 * @author Robert Sandell &lt;rsandell@cloudbees.com&gt;.
 */
public class UnZipStepExecution extends DecompressStepExecution {
    private static final long serialVersionUID = 6445244612862545236L;

    private transient UnZipStep step;

    protected UnZipStepExecution(@NonNull UnZipStep step, @NonNull StepContext context) {
        super(step, context);
        this.step = step;
    }

    @Override
    protected Object run() throws IOException, InterruptedException {
        TaskListener listener = getContext().get(TaskListener.class);
        assert listener != null;

        if (step.isTest()) {
            setCallable(new TestZipFileCallable(listener));
        } else {
            setCallable(new UnZipFileCallable(listener, step.getGlob(), step.isRead(),step.getCharset(),step.isQuiet()));
        }
        return super.run();
    }

    /**
     * Performs the unzip on the slave where the zip file is located.
     */
    public static class UnZipFileCallable extends AbstractFileCallable<Map<String,String>> {
        private final TaskListener listener;
        private final String glob;
        private final boolean read;
        private final boolean quiet;
        private final String charset;

        public UnZipFileCallable(TaskListener listener, String glob, boolean read, String charset, boolean quiet) {
            this.listener = listener;
            this.glob = glob;
            this.read = read;
            this.charset = charset;
            this.quiet = quiet;
        }

        @Override
        public Map<String, String> invoke(File zipFile, VirtualChannel channel) throws IOException, InterruptedException {
            if (!read) {
                getDestination().mkdirs();
            }
            PrintStream logger = listener.getLogger();
            boolean doGlob = !StringUtils.isBlank(glob);
            Map<String, String> strMap = new TreeMap<>();
            try (ZipFile zip = new ZipFile(zipFile, Charset.forName(charset))) {
                logger.println("Extracting from " + zipFile.getAbsolutePath());
                Enumeration<? extends ZipEntry> entries = zip.entries();
                Integer fileCount = 0;
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (doGlob && !matches(entry.getName(), glob)) {
                        continue;
                    }
                    FilePath f = getDestination().child(entry.getName());
                    if (!isDescendantOfDestination(f)) {
                        throw new FileNotFoundException(f.getRemote() + " is out of bounds!");
                    }
                    if (entry.isDirectory()) {
                        if (!read) {
                            f.mkdirs();
                        }
                    } else {
                        fileCount++;

                        if (!read) {
                            if (!quiet) {
                                logger.printf("Extracting: %s -> %s%n", entry.getName(), f.getRemote());
                            }

                            /*
                            It is not by all means required to close the input streams of the zip file because they are
                            closed once the zip file is closed. How ever doing so allows the zip class to reuse the
                            Inflater instance that is used.
                             */
                            try (InputStream inputStream = zip.getInputStream(entry);
                                 OutputStream outputStream = f.write()) {
                                IOUtils.copy(inputStream, outputStream);
                                outputStream.flush();
                            }
                        } else {
                            if (!quiet) {
                                logger.printf("Reading: %s%n", entry.getName());
                            }

                            try (InputStream is = zip.getInputStream(entry)) {
                                strMap.put(entry.getName(), IOUtils.toString(is, Charset.defaultCharset()));
                            }
                        }
                    }
                }
                if (read) {
                    logger.printf("Read: %d files%n", fileCount);
                    return strMap;
                } else {
                    logger.printf("Extracted: %d files%n", fileCount);
                    return null;
                }
            } finally {
                logger.flush();
            }
        }

        boolean matches(String path, String glob) {
            String safeGlob = glob.replace('/', File.separatorChar);
            String safePath = path.replace('/', File.separatorChar);
            return SelectorUtils.matchPath(safeGlob, safePath);
        }
    }

    /**
     * Performs a test of a zip file on the slave where the file is.
     */
    static class TestZipFileCallable extends AbstractFileCallable<Boolean> {
        private TaskListener listener;

        public TestZipFileCallable(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            PrintStream logger = listener.getLogger();
            try (ZipFile zip = new ZipFile(f)) {
                logger.printf("Checking %d zipped entries in %s%n", zip.size(), f.getAbsolutePath());

                Checksum checksum = new CRC32();
                byte[] buffer = new byte[4096];

                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    checksum.reset();

                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        FilePath destination = getDestination();
                        if (destination != null) {
                            FilePath ef = destination.child(entry.getName());
                            if (!isDescendantOfDestination(ef)) {
                                listener.error(ef.getRemote() + " is out of bounds!");
                                return false;
                            }
                        }
                        try (InputStream inputStream = zip.getInputStream(entry)) {
                            int length;
                            while ((length = IOUtils.read(inputStream, buffer)) > 0) {
                                checksum.update(buffer, 0, length);
                            }
                            if (checksum.getValue() != entry.getCrc()) {
                                listener.error("Checksum error in : " + f.getAbsolutePath() + ":" + entry.getName());
                                return false;
                            }
                        }
                    }
                }
                return true;
            } catch (ZipException e) {
                listener.error("Error validating zip file: " + e.getMessage());
                return false;
            } finally {
                logger.flush();
            }
        }
    }
}
