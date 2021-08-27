package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Verify the SHA1 of a file.
 */
public class FileVerifySha1Step extends FileVerifyHashStep {
    @DataBoundConstructor
    public FileVerifySha1Step(String file, String hash) throws Descriptor.FormException {
        super(file, hash, "SHA1");
    }

    @Extension
    public static class DescriptorImpl extends FileVerifyHashStep.DescriptorImpl {
        public DescriptorImpl() {
            super("Sha1");
        }
    }

}
