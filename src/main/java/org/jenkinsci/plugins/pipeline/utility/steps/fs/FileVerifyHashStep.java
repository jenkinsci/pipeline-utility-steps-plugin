package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

public abstract class FileVerifyHashStep extends Step {
    private final String file;
    private final String hash;
    private final String hashAlgorithm;

    public FileVerifyHashStep(String file, String hash, @NonNull String hashAlgorithm) throws Descriptor.FormException {
        if (StringUtils.isBlank(file)) {
            throw new Descriptor.FormException("can't be blank", "file");
        }
        if (StringUtils.isBlank(hash)) {
            throw new Descriptor.FormException("can't be blank", "hash");
        }

        this.file = file;
        this.hash = hash;
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getFile() {
        return file;
    }

    public String getHash() {
        return hash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new ExecutionImpl(this, context);
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
            return "verify" + algorithm;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Verify the " + algorithm.toUpperCase(Locale.ENGLISH) + " of a given file";
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckFile(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Needs a value");
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckHash(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Needs a value");
            }
            return FormValidation.ok();
        }
    }


    public static class ExecutionImpl extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 1L;
        private transient final FileVerifyHashStep step;

        public ExecutionImpl(FileVerifyHashStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            FilePath filePath = ws.child(step.getFile());
            final String calculatedHash = filePath.act(new ComputeHashCallable(step.getHashAlgorithm()));

            if (calculatedHash == null) {
                throw new FileNotFoundException("File not found: " + this.step.getFile());
            }

            if (!calculatedHash.equalsIgnoreCase(this.step.getHash())) {
                throw new AbortException(step.getHashAlgorithm() + " hash mismatch: expected: '" + this.step.getHash()
                        + "', actual: '" + calculatedHash + "'");
            }
            return null;
        }
    }

}
