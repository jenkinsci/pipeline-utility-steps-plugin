package org.jenkinsci.plugins.pipeline.utility.steps.fs;

import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Compute the MD5 of a file.
 */
public class FileMd5Step extends FileHashStep {
    @DataBoundConstructor
    public FileMd5Step(String file) throws Descriptor.FormException {
        super(file, "MD5");
    }

    @Extension
    public static class DescriptorImpl extends FileHashStep.DescriptorImpl {
        public DescriptorImpl() {
            super("MD5");
        }
    }
}
