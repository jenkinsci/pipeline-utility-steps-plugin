package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Verify the SHA256 of a file.
 */
public class FileVerifySha256Step extends FileVerifyHashStep {
    @DataBoundConstructor
    public FileVerifySha256Step(String file, String hash) throws Descriptor.FormException {
        super(file, hash, "SHA-256");
    }

    @Extension
    public static class DescriptorImpl extends FileVerifyHashStep.DescriptorImpl {
        public DescriptorImpl() {
            super("Sha256");
        }
    }

}
