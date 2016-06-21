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
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;
import java.io.File;
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
public class UnZipStepExecution extends AbstractSynchronousNonBlockingStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient FilePath ws;

    @Inject
    private transient UnZipStep step;

    @Override
    protected Object run() throws Exception {
        if (step.isTest()) {
            return test();
        }
        FilePath source = ws.child(step.getZipFile());
        if (!source.exists()) {
            throw new IOException(source.getRemote() + " does not exist.");
        } else if (source.isDirectory()) {
            throw new IOException(source.getRemote() + " is a directory.");
        }
        FilePath destination = ws;
        if (!StringUtils.isBlank(step.getDir())) {
            destination = ws.child(step.getDir());
        }
        return source.act(new UnZipFileCallable(listener, destination, step.getGlob(), step.isRead()));
    }

    private Boolean test() throws IOException, InterruptedException {
        FilePath source = ws.child(step.getZipFile());
        if (!source.exists()) {
            listener.error(source.getRemote() + " does not exist.");
            return false;
        } else if (source.isDirectory()) {
            listener.error(source.getRemote() + " is a directory.");
            return false;
        }
        return source.act(new TestZipFileCallable(listener));
    }

    /**
     * Performs the unzip on the slave where the zip file is located.
     */
    public static class UnZipFileCallable extends MasterToSlaveFileCallable<Map<String,String>> {
        private final TaskListener listener;
        private final FilePath destination;
        private final String glob;
        private final boolean read;

        public UnZipFileCallable(TaskListener listener, FilePath destination, String glob, boolean read) {
            this.listener = listener;
            this.destination = destination;
            this.glob = glob;
            this.read = read;
        }

        @Override
        public Map<String, String> invoke(File zipFile, VirtualChannel channel) throws IOException, InterruptedException {
            if (!read) {
                destination.mkdirs();
            }
            PrintStream logger = listener.getLogger();
            ZipFile zip = null;
            boolean doGlob = !StringUtils.isBlank(glob);
            Map<String, String> strMap = new TreeMap<String, String>();
            try {
                logger.println("Extracting from " + zipFile.getAbsolutePath());
                zip = new ZipFile(zipFile);
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (doGlob && !matches(entry.getName(), glob)) {
                        continue;
                    }
                    FilePath f = destination.child(entry.getName());
                    if (entry.isDirectory()) {
                        if (!read) {
                            f.mkdirs();
                        }
                    } else {
                        if (!read) {
                            logger.print("Extracting: ");
                            logger.print(entry.getName());
                            logger.print(" -> ");
                            logger.println(f.getRemote());

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
                            logger.print("Reading: ");
                            logger.println(entry.getName());

                            try (InputStream is = zip.getInputStream(entry)) {
                                strMap.put(entry.getName(), IOUtils.toString(is, Charset.defaultCharset()));
                            }
                        }
                    }
                }
                if (read) {
                    return strMap;
                } else {
                    return null;
                }
            } finally {
                IOUtils.closeQuietly(zip);
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
    static class TestZipFileCallable extends MasterToSlaveFileCallable<Boolean> {
        private TaskListener listener;

        public TestZipFileCallable(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            PrintStream logger = listener.getLogger();
            ZipFile zip = null;
            try {
                zip = new ZipFile(f);
                logger.print("Checking ");
                logger.print(zip.size());
                logger.print(" zipped entries in ");
                logger.println(f.getAbsolutePath());

                Checksum checksum = new CRC32();
                byte[] buffer = new byte[4096];

                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    checksum.reset();

                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
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
                //according to docs this should also close all open streams.
                IOUtils.closeQuietly(zip);
            }
        }
    }
}
