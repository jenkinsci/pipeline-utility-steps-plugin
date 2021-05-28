package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Compute the SHA256 of a file.
 */
public class FileSha256Step extends FileHashStep {
    @DataBoundConstructor
    public FileSha256Step(String file) throws Descriptor.FormException {
        super(file, "SHA-256");
    }

    @Extension
    public static class DescriptorImpl extends FileHashStep.DescriptorImpl {
        public DescriptorImpl() {
            super("SHA256");
        }
    }
}
