package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
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
import org.kohsuke.stapler.DataBoundConstructor;
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
import java.util.Set;

/**
 * Compute the SHA256 of a file.
 */
public class FileSha256Step extends Step {
    private final String file;

    @DataBoundConstructor
    public FileSha256Step(String file) throws Descriptor.FormException {
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
            return "sha256";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Compute the SHA256 of a given file";
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

        private transient FileSha256Step step;

        protected ExecutionImpl(@Nonnull FileSha256Step step, @Nonnull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            FilePath filePath = ws.child(step.getFile());
            return filePath.act(new ComputeSha256());
        }

        private static class ComputeSha256 extends MasterToSlaveFileCallable<String> {
            @Override
            public String invoke(File file, VirtualChannel virtualChannel) throws IOException, InterruptedException {
                if (file.exists() && file.isFile()) {
                    try {
                        return sha256(file);
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                } else {
                    return null;
                }
            }

            public String sha256(final File file) throws NoSuchAlgorithmException, IOException {
                final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

                try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                    final byte[] buffer = new byte[1024];
                    for (int read = 0; (read = is.read(buffer)) != -1; ) {
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
