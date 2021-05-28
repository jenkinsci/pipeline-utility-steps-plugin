package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import jenkins.MasterToSlaveFileCallable;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Formatter;
import java.util.Locale;
import java.util.Set;

/**
 * Base class for file hash steps.
 */
public abstract class FileHashStep extends Step {
    private final String file;
    private final String hashAlgorithm;

    public FileHashStep(String file, @Nonnull String hashAlgorithm) throws Descriptor.FormException {
        if (StringUtils.isBlank(file)) {
            throw new Descriptor.FormException("can't be blank", "file");
        }
        this.file = file;
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getFile() {
        return file;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context, hashAlgorithm);
    }


    public static abstract class DescriptorImpl extends StepDescriptor {
        private final String algorithm;

        public DescriptorImpl(String algorithm) {
            this.algorithm = algorithm;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(FilePath.class);
        }

        @Override
        public String getFunctionName() {
            return algorithm.toLowerCase(Locale.ENGLISH);
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Compute the " + algorithm.toUpperCase(Locale.ENGLISH) + " of a given file";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckFile(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Needs a value");
            }
            return FormValidation.ok();
        }
    }


    public static class ExecutionImpl extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;
        private transient final FileHashStep step;
        private transient final String algorithm;

        protected ExecutionImpl(@Nonnull FileHashStep step, @Nonnull StepContext context, @Nonnull String algorithm) {
            super(context);
            this.step = step;
            this.algorithm = algorithm;
        }

        @Override
        protected String run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            FilePath filePath = ws.child(step.getFile());
            return filePath.act(new ExecutionImpl.ComputeHash(algorithm));
        }


        private static class ComputeHash extends MasterToSlaveFileCallable<String> {
            private final String hashAlgorithm;

            public ComputeHash(String hashAlgorithm) {
                this.hashAlgorithm = hashAlgorithm;
            }

            @Override
            public String invoke(File file, VirtualChannel virtualChannel) throws IOException {
                if (file.exists() && file.isFile()) {
                    try {
                        return hashOfFile(file);
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                }
                return null;
            }

            public String hashOfFile(final File file) throws NoSuchAlgorithmException, IOException {
                final MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);

                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    final byte[] buffer = new byte[1024];
                    for (int read; (read = is.read(buffer)) != -1; ) {
                        messageDigest.update(buffer, 0, read);
                    }
                }

                return byteToHex(messageDigest.digest());
            }

            private String byteToHex(byte[] bytes) {
                try (Formatter formatter = new Formatter()) {
                    for (final byte b : bytes) {
                        formatter.format("%02x", b);
                    }
                    return formatter.toString();
                }
            }
        }
    }
}
