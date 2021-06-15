package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Verify the SHA1 of a file.
 */
public class FileSha1VerifyStep extends FileHashVerifyStep {
    @DataBoundConstructor
    public FileSha1VerifyStep(String file, String hash) throws Descriptor.FormException {
        super(file, hash, "SHA1");
    }

    @Extension
    public static class DescriptorImpl extends FileHashVerifyStep.DescriptorImpl {
        public DescriptorImpl() {
            super("SHA1");
        }
    }

}
