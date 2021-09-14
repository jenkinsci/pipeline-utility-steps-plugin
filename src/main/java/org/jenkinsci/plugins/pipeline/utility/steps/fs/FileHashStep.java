package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

/**
 * Base class for file hash steps.
 */
public abstract class FileHashStep extends Step {
    private final String file;
    private final String hashAlgorithm;

    public FileHashStep(String file, @NonNull String hashAlgorithm) throws Descriptor.FormException {
        if (StringUtils.isBlank(file)) {
            throw new Descriptor.FormException("can't be blank", "file");
        }
        this.file = file;
        this.hashAlgorithm = hashAlgorithm;
    }

    public String getFile() {
        return file;
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
            return algorithm.toLowerCase(Locale.ENGLISH);
        }

        @Override
        @NonNull
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

        protected ExecutionImpl(@NonNull FileHashStep step, @NonNull StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            FilePath ws = getContext().get(FilePath.class);
            FilePath filePath = ws.child(step.getFile());
            return filePath.act(new ComputeHashCallable(step.getHashAlgorithm()));
        }
    }

}
