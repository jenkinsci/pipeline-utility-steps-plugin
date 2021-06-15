package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Verify the SHA256 of a file.
 */
public class FileSha256VerifyStep extends FileHashVerifyStep {
    @DataBoundConstructor
    public FileSha256VerifyStep(String file, String hash) throws Descriptor.FormException {
        super(file, hash, "SHA-256");
    }

    @Extension
    public static class DescriptorImpl extends FileHashVerifyStep.DescriptorImpl {
        public DescriptorImpl() {
            super("SHA256");
        }
    }

}
