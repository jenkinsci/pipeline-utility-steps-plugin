package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Verify the MD5 of a file.
 */
public class FileVerifyMd5Step extends FileVerifyHashStep {
    @DataBoundConstructor
    public FileVerifyMd5Step(String file, String hash) throws Descriptor.FormException {
        super(file, hash, "MD5");
    }

    @Extension
    public static class DescriptorImpl extends FileVerifyHashStep.DescriptorImpl {
        public DescriptorImpl() {
            super("Md5");
        }
    }
}
