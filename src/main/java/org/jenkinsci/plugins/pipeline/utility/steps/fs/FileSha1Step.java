/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Emanuele Zattin
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

package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.Set;


/**
 * Compute the SHA1 of a file.
 *
 * @author Emanuele Zattin &lt;emanuelez@gmail.com&gt;.
 */
public class FileSha1Step extends Step {
    private final String file;

    @DataBoundConstructor
    public FileSha1Step(String file) throws Descriptor.FormException {
        if (StringUtils.isBlank(file)) {
            throw new Descriptor.FormException("can't be blank", "file");
        }
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public DescriptorImpl() {

        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return "sha1";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Compute the SHA1 of a given file";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckFile(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Needs a value");
            } else {
                return FormValidation.ok();
            }
        }
    }

    public static class ExecutionImpl extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        private transient FileSha1Step step;

        protected ExecutionImpl(@Nonnull FileSha1Step step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            FilePath filePath = ws.child(step.getFile());
            return filePath.act(new ComputeSha1());
        }

        private class ComputeSha1 extends MasterToSlaveFileCallable<String> {
            @Override
            public String invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
                if (file.exists() && file.isFile()) {
                    try {
                        return sha1(file);
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                } else {
                    return null;
                }
            }

            public String sha1(final File file) throws NoSuchAlgorithmException, IOException {
                final MessageDigest messageDigest = MessageDigest.getInstance("SHA1");

                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    final byte[] buffer = new byte[1024];
                    for (int read = 0; (read = is.read(buffer)) != -1;) {
                        messageDigest.update(buffer, 0, read);
                    }
                }

                // Convert the byte to hex format
                try (Formatter formatter = new Formatter()) {
                    for (final byte b : messageDigest.digest()) {
                        formatter.format("%02x", b);
                    }
                    return formatter.toString();
                }
            }
        }
    }

}
